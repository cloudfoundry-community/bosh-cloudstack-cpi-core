package com.orange.oss.cloudfoundry.cscpi.exceptions;

public class VMNotFoundException extends CPIException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3250507780678796137L;

	public VMNotFoundException(String msg,Throwable e){
		super(msg, e);
	}
	
	@Override
	public String toString(){
		return "Bosh::Clouds::VMNotFound";
	}
}
