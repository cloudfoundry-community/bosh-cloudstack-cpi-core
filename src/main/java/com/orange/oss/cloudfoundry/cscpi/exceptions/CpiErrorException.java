package com.orange.oss.cloudfoundry.cscpi.exceptions;

public class CpiErrorException extends CPIException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3250507780678796137L;

	public CpiErrorException(String msg,Throwable e){
		super(msg, e);
	}
	
	public CpiErrorException(String msg){
		super(msg);
	}
	
	
	@Override
	public String toString(){
		return "Bosh::Clouds::CpiError";
	}
}
