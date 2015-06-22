package com.orange.oss.cloudfoundry.cscpi.exceptions;

public class VMCreationFailedException extends CPIException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3250507780678796137L;

	public VMCreationFailedException(String msg,Throwable e){
		super(msg, e);
	}
	
	@Override
	public String toString(){
		return "Bosh::Clouds::VMCreationFailed";
	}
}
