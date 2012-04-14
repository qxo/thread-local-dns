package com.hystericalporpoises.dns;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Test;

public class ThreadLocalNameServiceTest extends BaseTest {

  @Test
  public void isLocal() throws Exception {
    // site local
    InetAddress host = InetAddress.getLocalHost();
    assertTrue(ThreadLocalNameService.isLocal(host));

    // loopback
    host = InetAddress.getByAddress(new byte[]{127,0,0,1});
    assertTrue(ThreadLocalNameService.isLocal(host));

    // link local - equivalent of 169.254.0.0
    host = InetAddress.getByAddress(new byte[]{-87,-2,0,0});
    assertTrue(ThreadLocalNameService.isLocal(host));

    host = InetAddress.getByName("www.amazon.com");
    assertFalse(ThreadLocalNameService.isLocal(host));
  }

  @Test(expected = UnknownHostException.class)
  public void getOverrideFailsNull() throws Exception {
    ThreadLocalNameService.getOverride(null);
  }

  @Test
  public void resolve() throws Exception {
    byte[] ipAddress = TextToNumeric.convert("192.168.1.1");
    assertArrayEquals(ipAddress, InetAddress.getByName("www.google.com").getAddress());
  }

}
