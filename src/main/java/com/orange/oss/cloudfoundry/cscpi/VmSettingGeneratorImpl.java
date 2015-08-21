package com.orange.oss.cloudfoundry.cscpi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jclouds.cloudstack.domain.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;


/**
 * generates setting yml description to be exposed in bosh registry
 * 
 * see https://github.com/cloudfoundry/bosh-agent/blob/master/settings/settings.go
 * 
 * NB: yaml not json
 * @author poblin
 *
 */
public class VmSettingGeneratorImpl implements VmSettingGenerator {

	
	private static Logger logger=LoggerFactory.getLogger(VmSettingGeneratorImpl.class.getName());
	
	
	public static class RegistryReponse {
		public String status="ok";
		public String settings;
	}
	
	
	public static class Setting{
		public String agent_id;
		public BlobStore blobstore=new BlobStore();
		public Disks disks=new Disks();
		public Map<String, String> env=new HashMap<String, String>();
		//public NetworksSetting networks=new NetworksSetting();
		public Map<String,Network> networks;
		public List<String> ntp=new ArrayList<String>();
		public String mbus="https://mbus:mbus-password@0.0.0.0:6868";
		public VM vm=new VM();
		public String trusted_certs;
	}
	
	
	public static class BlobStore {
		public String provider="local"; //dav
		public Map<String,String> options=new HashMap<String, String>();
	}
	
//	public static class BlobStoreOptions {
//        String endpoint="http://10.203.6.105:25250";
//        String user="agent";
//        String password="agent-password";
//        String blobstore_path="/var/vcap/micro_bosh/data/cache";  
// 	}
	
	public static class Disks {
		String system="/dev/xvda";
		String ephemeral="";
		Map<String,String> persistent=new HashMap<String, String>();
	}
	
	
	public static class Env {
		//FIXME: describe env properly
		//public Map<String, String> options=new HashMap<String, String>();
	}
	
	
	public static class VM {
		String name;
	}
	
	
	@Override
	public String settingFor(String agent,String vmName, VirtualMachine vm, Networks networks) {
		Setting settingObject=new Setting();
		settingObject.agent_id=agent;
		
		//blobstore
		settingObject.blobstore.options.put("endpoint","http://10.203.6.105:25250");
		settingObject.blobstore.options.put("user","agent");
		settingObject.blobstore.options.put("password","agent-password");
		settingObject.blobstore.options.put("blobstore_path","/var/vcap/micro_bosh/data/cache");
		
		
		//env
		
		
		//networks
		settingObject.networks=networks.networks;
		
		
		//ntp
		
		//mbus url
		
		
		//vm
		settingObject.vm.name=vmName;
		
		//serialize to json
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);		
		String setting;
		try {
			setting = mapper.writeValueAsString(settingObject);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cant serialize JSON userData", e);
		}
		

		//the global json setting is wrapped in a 2 field json structure
		
		RegistryReponse response=new RegistryReponse();
		response.settings=setting;
		String resp=null;

		try {
			resp = mapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cant serialize JSON userData", e);
		}

		logger.info("generated vm setting : \n{}",resp);
		
		return resp;
	}

}
