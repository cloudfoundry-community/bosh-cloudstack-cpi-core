package com.orange.oss.cloudfoundry.cscpi.cep;

public class CPIDeleteVmOK extends CPIEvent {

	public CPIDeleteVmOK(String vmId, String diskId) {
		super(vmId, diskId);
		this.command="delete_vm";		
	}

}
