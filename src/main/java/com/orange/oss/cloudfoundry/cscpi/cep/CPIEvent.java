package com.orange.oss.cloudfoundry.cscpi.cep;


/**
 * CPI events
 * @author poblin
 *
 */
public abstract class CPIEvent {

	protected String command;
	private String vmId = null;
	private String diskId = null;

	public CPIEvent( String vmId, String diskId) {
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
