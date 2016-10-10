package com.orange.oss.cloudfoundry.cscpi.logic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jclouds.cloudstack.domain.VirtualMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.config.DirectorConfig;
import com.orange.oss.cloudfoundry.cscpi.config.DirectorConfigNtp;
import com.orange.oss.cloudfoundry.cscpi.domain.Env;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.PersistentDisk;
/**
 * generates setting.json description to be exposed in bosh registry. Manages
 * setting.json update with attach / detach persistent disk (add disk_id
 * reference, and linux fs path /vokume id)
 * 
 * see
 * https://github.com/cloudfoundry/bosh-agent/blob/master/settings/settings.go
 * 
 * NB: yaml not json
 * 
 * @author poblin
 *
 */
public class VmSettingGeneratorImpl implements VmSettingGenerator {
	
	/**
	 * Director level configuration
	 */
	@Autowired
	DirectorConfig directorConfig;
	
	@Autowired
	DirectorConfigNtp ntpConfig;
	

	private static Logger logger = LoggerFactory
			.getLogger(VmSettingGeneratorImpl.class.getName());

	public static class Setting {
		public String agent_id;
		public BlobStore blobstore = new BlobStore();
		public Disks disks = new Disks();
		public Env env = null;
		public Map<String, Network> networks;
		public List<String> ntp = new ArrayList<String>();
		public String mbus = "https://mbus:mbus-password@0.0.0.0:6868";
		public VM vm = new VM();
		public String trusted_certs;
	}

	public static class BlobStore {
		public String provider = "local"; // dav
		public Map<String, String> options = new HashMap<String, String>();
	}


	public static class Disks {
		String system = "/dev/xvda";
		String ephemeral = "/dev/sdb";
		// use sdb due to parsing issue with /dev/xvdb see
		// https://github.com/cloudfoundry/bosh-agent/blob/master/infrastructure/devicepathresolver/mapped_device_path_resolver.go
		// (line 45)
		//fixed by https://github.com/cloudfoundry/bosh-agent/commit/565f89329bcf9416d1d2771f618c6b1b804ecc93
		Map<String, PersistentDisk> persistent = new HashMap<String, PersistentDisk>();
	}



	public static class VM {
		String name;
	}

	@Override
	public String createsettingForVM(String agent, String vmName,
			VirtualMachine vm, Networks networks,Env env) {
		Setting settingObject = new Setting();
		settingObject.agent_id = agent;

		// blobstore
		
		//FIXME: check blobstore options from director env (application.yml templated with deployment props)
		settingObject.blobstore.provider=directorConfig.blobstore_provider;
		settingObject.blobstore.options.put("endpoint", "http://"+directorConfig.address+":"+directorConfig.port);
		settingObject.blobstore.options.put("user", directorConfig.user);
		settingObject.blobstore.options.put("password", directorConfig.password);
		settingObject.blobstore.options.put("blobstore_path", directorConfig.path);

		// env
		settingObject.env=env;

		// networks
		settingObject.networks = networks.networks;
		
		// ntp
		settingObject.ntp=ntpConfig.getNtp();

		// mbus url
		settingObject.mbus=directorConfig.mbus;
				
		// vm
		settingObject.vm.name = vmName;
		
		// set mac adress (required?)
		if (vm!=null) {
			logger.debug("setting mac address in setting");
			String macAddress=vm.getNICs().iterator().next().getMacAddress();
			settingObject.networks.values().iterator().next().mac=macAddress;
			
		
		//FIXME only support single NIC
		}
		//director level configuration for cloudstack see https://github.com/cloudfoundry/bosh/issues/911
		settingObject.networks.values().iterator().next().use_dhcp=directorConfig.use_dhcp;
		
		
		//set default: [gateway,dns], hardcoded as long as single NIC support
		settingObject.networks.values().iterator().next().default_prop.add("gateway");
		settingObject.networks.values().iterator().next().default_prop.add("dns");		
		

		//Keep track if network was resolved via DHCP
		//Add Resolved flag to network to indicate that it was resolved via
		//DHCP so that on subsequent checks it continues to use DHCP.
		//Dynamic network always is using DHCP. Manual network will use DHCP only if
		//IP or Netmask are not provided required for static configuration.

		settingObject.networks.values().iterator().next().resolved=false; //Check ?
		

		// serialize to json
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		String setting;
		try {
			setting = mapper.writeValueAsString(settingObject);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cant serialize JSON userData",
					e);
		}

		logger.info("generated vm setting : \n{}", setting);

		return setting;
	}

	@Override
	public String updateVmSettingForDisks(String previousSetting,
			Map<String,PersistentDisk> newDisks) {
		// parse setting

		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);		
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		try {
			Setting setting = mapper.readValue(previousSetting, Setting.class);
			//rewrite the persistent disks list
			setting.disks.persistent=newDisks;

			// new setting
			String newSetting = mapper.writeValueAsString(setting);
			
			logger.info("generated updated vm setting : \n{}", newSetting);			
			return newSetting;

		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Cant deserialize JSON setting for attach \n"+previousSetting, e);
		}

	}

}
