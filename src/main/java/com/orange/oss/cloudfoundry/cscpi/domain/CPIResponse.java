package com.orange.oss.cloudfoundry.cscpi.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;


/**
 * Base JSON mapping for CPI response to CPI
 * @author pierre
 *
 */
@JsonAutoDetect
public class CPIResponse {

  public List <String> result=new ArrayList<String>();
	
  public String error=null;
	
  public String log="";
}
