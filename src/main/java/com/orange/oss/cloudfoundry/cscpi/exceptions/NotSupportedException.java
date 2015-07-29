package com.orange.oss.cloudfoundry.cscpi.exceptions;

public class NotSupportedException extends CPIException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3250507780678796137L;

	public NotSupportedException(String msg,Throwable e){
		super(msg, e);
	}
	
	public NotSupportedException(String msg){
		super(msg);
	}
	
	
	
	@Override
	public String toString(){
		return "Bosh::Clouds::NotSupported";
	}
}
