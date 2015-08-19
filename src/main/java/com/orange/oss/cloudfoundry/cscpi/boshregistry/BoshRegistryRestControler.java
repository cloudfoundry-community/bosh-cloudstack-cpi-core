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
		
		logger.info("found settings for vm {}\n{}",vm_id,settings);
		
		return settings;
	}
	
	@RequestMapping(method=RequestMethod.POST,value = "/{vm_id}")
	public void setSettingForVmId(@PathVariable String vm_id,@RequestBody String settings) {
		logger.info("set registry setting request for {} with setting :\n{}",vm_id,settings);
		RegistryInstance registryInstance=new RegistryInstance(vm_id,settings);
		this.repository.save(registryInstance);
	}


	@RequestMapping(method=RequestMethod.DELETE,value = "/{vm_id}")
	@ResponseBody
	public void deleteSettingForVmId(@PathVariable String vm_id) {
		logger.info("registry setting delete for {}",vm_id);
		
		
		RegistryInstance instance=repository.findOne(vm_id);
		if (instance==null){
			logger.warn("instance not found with vm_id {}. cant delete ...",vm_id);
			throw new InstanceNotFoundException(); //FIXME: should we fail or just warn ?
		}
		
		repository.delete(instance);
		
		logger.info("deleted settings for vm {}\n{}",vm_id);
	}
	
	
	
}
