package com.orange.oss.cloudfoundry.cscpi.exceptions;

public class DiskNotFoundException extends CPIException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3250507780678796137L;

	public DiskNotFoundException(String msg,Throwable e){
		super(msg, e);
	}
	
	@Override
	public String toString(){
		return "Bosh::Clouds::DiskNotFound";
	}
}
