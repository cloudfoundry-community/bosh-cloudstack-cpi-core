package com.orange.oss.cloudfoundry.cscpi.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Base JSON mapping for CPI response to CPI
 * @author pierre
 *
 */
@JsonAutoDetect
@JsonPropertyOrder({"result", "error","log"})
public class CPIResponse {

 public static class  CmdError{
	 public String type="Bosh::Clouds::CpiError";
	 public String message;
	 @JsonProperty(value="ok_to_retry")
	 boolean okToRetry=false;
 }
	
	
  public List <Object> result=new ArrayList<Object>();
	
  public CmdError error=null;
	
  public String log="";
  
  
  public boolean equals(Object obj) {
      return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public int hashCode() {
      return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
      return ToStringBuilder.reflectionToString(this);
  }
  
}
