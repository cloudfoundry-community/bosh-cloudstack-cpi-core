package com.orange.oss.cloudfoundry.cscpi.cep;


/**
 * CPI events
 * @author poblin
 *
 */
public class CPIEvent {

	private String command;
	private String vmId = null;
	private String diskId = null;

	public CPIEvent(String command, String vmId, String diskId) {
		this.command = command;
		this.vmId = vmId;
		this.diskId = diskId;
	}

	public String getCommand() {
		return command;
	}

	public String getVmId() {
		return vmId;
	}

	public String getDiskId() {
		return diskId;
	}

}
