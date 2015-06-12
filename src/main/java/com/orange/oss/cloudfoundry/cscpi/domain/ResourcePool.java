package com.orange.oss.cloudfoundry.cscpi.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */
public class ResourcePool {

    public String ram;
    public String disk;
    public int cpu;

    public ResourcePool() {
    }

    public ResourcePool(String ram, String disk, int cpu) {
        this.ram = ram;
        this.disk = disk;
        this.cpu = cpu;
    }

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
