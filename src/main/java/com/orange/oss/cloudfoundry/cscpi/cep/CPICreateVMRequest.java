package com.orange.oss.cloudfoundry.cscpi.cep;

public class CPICreateVMRequest extends CPIEvent {

	public CPICreateVMRequest(String vmId, String diskId) {
		super(vmId, diskId);
		this.command="create_vm";		
	}

}
