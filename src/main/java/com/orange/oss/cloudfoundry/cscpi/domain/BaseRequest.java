package com.orange.oss.cloudfoundry.cscpi.domain;

import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

/**
 * Jackson mapping class for cpi request leverages advance Jackson Mapping see
 * http://www.baeldung.com/jackson-annotations
 * 
 * @author pierre
 *
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "method")
@JsonSubTypes({ 
		@JsonSubTypes.Type(value = DetachDiskRequest.class, name = "detach_disk"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "attach_disk"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "create_disk"),
		@JsonSubTypes.Type(value = DeleteDiskRequest.class, name = "delete_disk"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "get_disks"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "has_disk"),		
		@JsonSubTypes.Type(value = CreateVMRequest.class, name = "create_vm"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "delete_vm"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "reboot_vm"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "has_vm"),		
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "current_vm_id"),		
		@JsonSubTypes.Type(value = SetVMMetadataRequest.class, name = "set_vm_metadata"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "snapshot_disk"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "delete_snapshot"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "configure_network"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "create_stemcell"),
		@JsonSubTypes.Type(value = AttachDiskRequest.class, name = "delete_stemcell"),		

})
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseRequest {
	public String method;
	public Map<String,String> context;
	
	
	
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
