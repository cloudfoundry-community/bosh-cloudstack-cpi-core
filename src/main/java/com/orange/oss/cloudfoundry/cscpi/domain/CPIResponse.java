package com.orange.oss.cloudfoundry.cscpi.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

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
