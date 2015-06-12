package com.orange.oss.cloudfoundry.cscpi.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class Network {

    public String ip;
    public String netmask;
    public Map<String,String> cloud_properties;
    //public List<String> default;
    public String dns;
    public String gateway;

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
