package com.orange.oss.cloudfoundry.cscpi.boshregistry;


import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is a simple rest controller implementing bosh registry
 * Easier to embed as part of the CPI (bosh registry has plugins for cloudstack / openstack, and will be deprecated by bosh team)
 * 
 * Uses a simple in memory db
 * 	Table : registry_instances
 * 	id (string, pk)
 * 	settings (string)
 * 
 * see 
 * @author poblin
 *
 */

@RestController
@RequestMapping("/instances")
public class BoshRegistryRestControler {
	
	private static Logger logger=LoggerFactory.getLogger(BoshRegistryRestControler.class.getName());
	
	@Autowired
	RegistryInstanceRepository repository;
	
	
	
	public static class RegistryReponse {
		public String status="ok";
		public String settings;
	}
	
	
	
	
	/**
	http://10.234.228.154:8080/instances/cpivm-ffce8e11-4496-494f-802d-a6df17285b5a/settings
	**/
	
	@Transactional
	@RequestMapping(method=RequestMethod.GET,value = "/{vm_id}/settings",produces="application/json")
	@ResponseBody
	public String getSettingForVmId(@PathVariable String vm_id) {
		logger.info("registry setting request for {}",vm_id);
		
		
		RegistryInstance instance=repository.findOne(vm_id);
		if (instance==null){
			logger.warn("instance not found with vm_id {}",vm_id);
			throw new InstanceNotFoundException();
		}
		String settings=instance.getSettings();
		
		
		
		//the global json setting is wrapped in a 2 field json structure
		
		RegistryReponse response=new RegistryReponse();
		response.settings=settings;
		String resp=null;

		try {
			ObjectMapper mapper = new ObjectMapper();
			resp = mapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cant serialize JSON registry reponse", e);
		}
		
		logger.debug("found settings for vm {}\n{}",vm_id,settings);
		
		return resp;
	}

	
	
	@Transactional
	@RequestMapping(method=RequestMethod.GET,value = "/{vm_id}/rawsettings",produces="application/json")
	@ResponseBody
	public String getRawSettingForVmId(@PathVariable String vm_id) {
		logger.info("registry : give raw setting  for {}",vm_id);
		
		
		RegistryInstance instance=repository.findOne(vm_id);
		if (instance==null){
			logger.warn("instance not found with vm_id {}",vm_id);
			throw new InstanceNotFoundException();
		}
		String settings=instance.getSettings();
		
		logger.debug("found settings for vm {}\n{}",vm_id,settings);
		
		return settings;
	}
	
	
	@Transactional
	@RequestMapping(method=RequestMethod.POST,value = "/{vm_id}")
	public void setSettingForVmId(@PathVariable String vm_id,@RequestBody String settings) {
		logger.info("set registry setting request for {} with setting :\n{}",vm_id,settings);
		
		RegistryInstance instance=repository.findOne(vm_id);
		if (instance==null){
			logger.info("create new instance with vm_id {}",vm_id);
			RegistryInstance registryInstance=new RegistryInstance(vm_id,settings);
			this.repository.save(registryInstance);
		} else
		{logger.info("update existing registry instance with vm_id {}",vm_id);
			instance.setSettings(settings);
			//transaction commit with flush the new value
		}
		
		
		
	}

	@Transactional
	@RequestMapping(method=RequestMethod.DELETE,value = "/{vm_id}")
	@ResponseBody
	public void deleteSettingForVmId(@PathVariable String vm_id) {
		logger.info("registry setting delete for {}",vm_id);
		
		RegistryInstance instance=repository.findOne(vm_id);
		if (instance==null){
			logger.warn("instance not found with vm_id {}. cant delete ...",vm_id);
			//throw new InstanceNotFoundException(); //FIXME: should we fail or just warn ?
		} else {
			repository.delete(instance);
			logger.info("deleted settings for vm {}\n{}",vm_id);
			
		}
		
	}
	
}
