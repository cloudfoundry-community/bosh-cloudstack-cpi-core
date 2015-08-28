package com.orange.oss.cloudfoundry.cscpi;

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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.config.DirectorConfig;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;

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

	private static Logger logger = LoggerFactory
			.getLogger(VmSettingGeneratorImpl.class.getName());

	public static class Setting {
		public String agent_id;
		public BlobStore blobstore = new BlobStore();
		public Disks disks = new Disks();
		public Map<String, String> env = new HashMap<String, String>();
		// public NetworksSetting networks=new NetworksSetting();
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
		Map<String, PersistentDisk> persistent = new HashMap<String, PersistentDisk>();
	}

	public static class PersistentDisk {
		String path;
		@JsonProperty(value="volume_id")
		String volumeId;
	}

	public static class Env {
		// FIXME: describe env properly
		// public Map<String, String> options=new HashMap<String, String>();
	}

	public static class VM {
		String name;
	}

	@Override
	public String createsettingForVM(String agent, String vmName,
			VirtualMachine vm, Networks networks) {
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

		// networks
		settingObject.networks = networks.networks;

		// ntp
		
		//FIXME parse from directorCondif and set ntp server list
			

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
	public String updateVmSettingForAttachDisk(String previousSetting,
			String disk_id) {
		// parse setting

		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);		
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		try {
			Setting setting = mapper.readValue(previousSetting, Setting.class);

			Map<String, PersistentDisk> disks = setting.disks.persistent;
			if (disks.get(disk_id) != null) {
				logger.warn(
						"Trying to add a disk {} to setting {}, but disk already in setting ! => Ignoring",
						disk_id, previousSetting);
				return previousSetting;

			}
			// add disk to persistent structure (if not existing). calculate
			// unique device path.
			// FIXME : now use device. a is root, b is ephemeral, start from c.
			// Might night better algorithm, using cloudstack disk attachement info
			if (disks.size() != 0) {
				throw new IllegalArgumentException(
						"CPI not yet able to managed more than 1 persistent disk");
			}

			PersistentDisk newDisk = new PersistentDisk();
			newDisk.path = "/dev/sdc"; // FIXME : booooo. hardcoded in bosh agent, here too.
			newDisk.volumeId="3";
			disks.put(disk_id, newDisk);

			// new setting
			String newSetting = mapper.writeValueAsString(setting);
			return newSetting;

		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Cant deserialize JSON setting for attach \n"+previousSetting, e);
		}

	}

	@Override
	public String updateVmSettingForDetachDisk(String previousSetting,
			String disk_id) {

		// parse setting

		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);		
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		try {
			Setting setting = mapper.readValue(previousSetting, Setting.class);

			Map<String, PersistentDisk> disks = setting.disks.persistent;
			if (disks.get(disk_id) == null) {
				logger.warn(
						"Trying to remove a disk {} to setting {}, but disk not defined in setting ! => Ignoring",
						disk_id, previousSetting);
				return previousSetting;

			}
			// remove disk from persistent structure. keep existing
			// disk path (should not dynamically change ??)
			// Might night better algorithm, using cloudstack disk attachement
			if (disks.size() >1) {
				throw new IllegalArgumentException(
						"CPI not yet able to managed more than 1 persistent disk");
			}
					
			disks.remove(disk_id);

			// new setting
			String newSetting = mapper.writeValueAsString(setting);
			return newSetting;

		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Cant deserialize JSON setting for detach", e);
		}



	}

}
