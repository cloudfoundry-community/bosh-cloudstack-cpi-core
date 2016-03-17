package com.orange.oss.cloudfoundry.cscpi.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;



@JsonIgnoreProperties(ignoreUnknown=true)
public class Network {
	    public NetworkType type=NetworkType.manual;//default is manual ?
		public String ip;
	    public String netmask;
	    public Map<String,String> cloud_properties=new HashMap<String, String>();

	    
	    //FIXME parse default services ?
	    //public List<String> default=new ArrayList<String>();
	    
	    public List<String> dns=new ArrayList<String>();
	    public String gateway;

	    //properties added for Setting generation (added by CPI for bosh-agent, not from manifest)
	    public String mac;
	    public boolean use_dhcp;
	    public boolean resolved;
	    
	    //set this network default gateway AND OR default dns
	    @JsonProperty(value="default")
	    public List<String> default_prop=new ArrayList<String>();
	    
	    
	    
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
