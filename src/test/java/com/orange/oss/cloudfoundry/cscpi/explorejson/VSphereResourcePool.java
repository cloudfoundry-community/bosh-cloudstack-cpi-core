package com.orange.oss.cloudfoundry.cscpi.explorejson;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */
public class VSphereResourcePool {

    public String ram;
    public String disk;
    public int cpu;

    public VSphereResourcePool() {
    }

    public VSphereResourcePool(String ram, String disk, int cpu) {
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
