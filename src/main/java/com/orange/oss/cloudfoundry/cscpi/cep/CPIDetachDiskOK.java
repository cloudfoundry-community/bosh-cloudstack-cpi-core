package com.orange.oss.cloudfoundry.cscpi.cep;

public class CPIDetachDiskOK extends CPIEvent {

	public CPIDetachDiskOK(String vmId, String diskId) {
		super(vmId, diskId);
		this.command="detach_disk";		
	}

}
