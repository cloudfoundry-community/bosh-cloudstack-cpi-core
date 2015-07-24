package com.orange.oss.cloudfoundry.cscpi.domain;

import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class SetVMMetadataRequest extends BaseRequest {


	public static class SetVMMetadataRequestArguments {
		public String vmId;
		public Map<String,String> metadatas;
	}
	
	public SetVMMetadataRequestArguments arguments;
	
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
