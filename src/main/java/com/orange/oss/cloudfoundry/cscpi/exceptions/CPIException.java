package com.orange.oss.cloudfoundry.cscpi.exceptions;

public abstract class CPIException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6620218281008600091L;

	
	public CPIException(Throwable e){
		super(e);
	}

	public CPIException(String msg){
		super(msg);
	}

	
	public CPIException(String msg,Throwable e){
		super(msg,e);
	}
	
	
}
