package com.theotherian.dns;

import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

/**
 * Initializes the thread local settings for your application. Make sure one of this type's initialize methods
 * is the first thing called in your application, or at least the first thing called before DNS is
 * initialized.<br>
 * If you try to initialize the thread local DNS settings after DNS has initialized, the settings
 * will not be detected.
 * @author isimpson
 */
public final class ThreadLocalDns {

  private static final Logger LOGGER = Logger.getLogger(ThreadLocalDns.class);

  private static volatile boolean initialized = false;

  private static final ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadLocalDnsFactory());

  /**
   * Run the provided context inside of an thread local DNS configuration that is inheritable so
   * that all threads spawned in the context have the same configuration.
   * @param configuration
   * @param context
   */
  public static void executeContext(final ThreadLocalDnsConfiguration configuration,
    final ThreadLocalDnsContext context) {

    threadPool.submit(new Runnable() {

      @Override
      public void run() {
        try {
          OverrideNameService nameService = new OverrideNameService(configuration.getMappings());
          OverrideNameServiceManager.initializeForThread(nameService);
          OverrideNameServiceManager.initializeCache();
          nameService.validate();
          context.execute();
        } catch (Throwable t) {
          LOGGER.fatal("Context failed to start", t);
        }
      }

    });
  }


  /**
   * Initialize all system properties required for overriding DNS.
   * @throws RuntimeException if you try to initialize twice
   */
  public static void initialize() {
    initialize(DnsConfigurationBuilder.newBuilder().build());
  }

  /**
   * Initialize all system properties required for overriding DNS, and set a configuration for the main
   * thread and all children of the main thread not created by the
   * {@link #executeContext(ThreadLocalDnsConfiguration, ThreadLocalDnsContext)} method, which accepts its own
   * configuration
   * @param configuration
   */
  public static void initialize(ThreadLocalDnsConfiguration configuration) {
    if (!initialized) {
      LOGGER.info("Initializing thread local DNS settings");
      ThreadLocalDnsDescriptor descriptor = new ThreadLocalDnsDescriptor();
      String provider = descriptor.getType() + "," + descriptor.getProviderName();
      System.setProperty("sun.net.spi.nameservice.provider.1", provider);
      Security.setProperty("networkaddress.cache.ttl", "0");
      OverrideNameService nameService = new OverrideNameService(configuration.getMappings());
      OverrideNameServiceManager.initializeForThread(nameService);
      initialized = true;
    }
    else {
      throw new RuntimeException("You can't initialize DNS twice in an application");
    }
  }

  private static class ThreadLocalDnsFactory implements ThreadFactory {

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
      return new Thread(r, "ThreadLocalDnsWorker-" + counter.getAndIncrement());
    }

  }

}
