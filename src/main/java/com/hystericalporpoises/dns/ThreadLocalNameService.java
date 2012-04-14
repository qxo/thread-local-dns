package com.hystericalporpoises.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.xbill.DNS.spi.DNSJavaNameService;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class ThreadLocalNameService extends DNSJavaNameService {

  private static Logger LOGGER = Logger.getLogger(ThreadLocalNameService.class);

  static {
    LOGGER.info("Thread Local DNS Name Service loaded");
  }

  private InheritableThreadLocal<LoadingCache<String, InetAddress[]>> dnsCache = new InheritableThreadLocal<LoadingCache<String,InetAddress[]>>() {
    @Override
    protected LoadingCache<String, InetAddress[]> initialValue() {
      return CacheBuilder.newBuilder().build(new CacheLoader<String, InetAddress[]>() {

        @Override
        public InetAddress[] load(String key) throws Exception {
          LOGGER.debug("Looking up " + key);
          if (OverrideNameServiceManager.hasIpForHost(key)) {
            String ipAddress = OverrideNameServiceManager.getIpForHost(key);
            LOGGER.debug("Found thread local override for " + key + " of " + ipAddress);
            return convertToInetAddress(ipAddress);
          }
          else if (HostsFileResolver.hasOverride(key)) {
            String ipAddress = HostsFileResolver.getOverride(key);
            LOGGER.debug("Found hosts entry for " + key + " of " + ipAddress);
            return convertToInetAddress(ipAddress);
          }
          else {
            LOGGER.debug("No override found for " + key + ", performing normal resolution");
            return normalLookup(key);
          }
        }

        private InetAddress[] convertToInetAddress(String ipAddress) throws UnknownHostException {
          byte[] ipAsBytes = TextToNumeric.convert(ipAddress);
          InetAddress[] address = new InetAddress[1];
          address[0] = InetAddress.getByAddress(ipAsBytes);
          return address;
        }

      });
    }
  };


  @VisibleForTesting
  static boolean isLocal(InetAddress ip) {
    return ip.isLinkLocalAddress() || ip.isLoopbackAddress() || ip.isSiteLocalAddress();
  }

  public ThreadLocalNameService() {
    super();
  }


  @Override
  public String getHostByAddr(byte[] in) throws UnknownHostException {
    return super.getHostByAddr(in);
  }

  /**
   * Lookup override
   * @throws UnknownHostException for null hostname
   */
  @VisibleForTesting
  static byte[] getOverride(String hostname) throws UnknownHostException {
    LOGGER.debug("Looking up " + hostname);
    if (hostname == null) {
      throw new UnknownHostException("Null host string");
    }

    String val = null;
    if (OverrideNameServiceManager.hasIpForHost(hostname)) {
      val = OverrideNameServiceManager.getIpForHost(hostname);
    }
    else if (HostsFileResolver.hasOverride(hostname)) {
      val = HostsFileResolver.getOverride(hostname);
    }
    return TextToNumeric.convert(val);
  }

  /**
   * Return the address that corresponds with the given hostname
   * @param hostname
   * @throws UnknownHostException
   */
  public static InetAddress getByName(String hostname) throws UnknownHostException {
    byte val[] = getOverride(hostname);
    if (val == null) {
      return InetAddress.getByName(hostname);
    }
    return InetAddress.getByAddress(val);
  }

  private InetAddress[] normalLookup(String hostname) throws UnknownHostException {
    return super.lookupAllHostAddr(hostname);
  }


  @Override
  public InetAddress[] lookupAllHostAddr(String hostname) throws UnknownHostException {
    try {
      return dnsCache.get().get(hostname);
    }
    catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

}
