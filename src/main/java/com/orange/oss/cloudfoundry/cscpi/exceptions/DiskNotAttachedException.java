package com.orange.oss.cloudfoundry.cscpi.exceptions;

public class DiskNotAttachedException extends CPIException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3250507780678796137L;

	public DiskNotAttachedException(String msg,Throwable e){
		super(msg, e);
	}
	
	@Override
	public String toString(){
		return "Bosh::Clouds::DiskNotAttached";
	}
}
