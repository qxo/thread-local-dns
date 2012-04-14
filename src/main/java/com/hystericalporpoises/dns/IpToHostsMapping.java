package com.hystericalporpoises.dns;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.AutoProperty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@AutoProperty
@Immutable
public final class IpToHostsMapping {

  private String ipAddress;

  private List<String> hosts = Lists.newArrayList();

  public IpToHostsMapping() {}

  public IpToHostsMapping(String ipAddress, List<String> hosts) {
    this.ipAddress = ipAddress;
    this.hosts = hosts;
  }

  public final String getIpAddress() { return this.ipAddress; }

  public final List<String> getHosts() { return ImmutableList.copyOf(hosts); }

  @Override public boolean equals(Object o) {
    return Pojomatic.equals(this, o);
  }

  @Override public int hashCode() {
    return Pojomatic.hashCode(this);
  }

  @Override public String toString() {
    return Pojomatic.toString(this);
  }

}
