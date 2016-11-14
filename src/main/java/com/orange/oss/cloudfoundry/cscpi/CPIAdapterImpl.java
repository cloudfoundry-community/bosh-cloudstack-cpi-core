package com.orange.oss.cloudfoundry.cscpi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse.CmdError;
import com.orange.oss.cloudfoundry.cscpi.domain.Network;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CPIException;
import com.orange.oss.cloudfoundry.cscpi.logic.CPI;


public class CPIAdapterImpl implements CPIAdapter {


	public enum CPIMethod {
		delete_stemcell,
		create_stemcell,
		has_disk,
		has_vm,
		delete_vm,
		set_vm_metadata,
		reboot_vm,
		create_vm,
		detach_disk,
		attach_disk,
		delete_disk,
		create_disk
	}
	
	private static final String DELETE_STEMCELL = "delete_stemcell";
	private static final String CREATE_STEMCELL = "create_stemcell";
	private static final String HAS_DISK = "has_disk";
	private static final String HAS_VM = "has_vm";
	private static final String DELETE_VM = "delete_vm";
	private static final String SET_VM_METADATA = "set_vm_metadata";
	private static final String REBOOT_VM = "reboot_vm";
	private static final String CREATE_VM = "create_vm";
	private static final String DETACH_DISK = "detach_disk";
	private static final String ATTACH_DISK = "attach_disk";
	private static final String DELETE_DISK = "delete_disk";
	private static final String CREATE_DISK = "create_disk";
	private static Logger logger = LoggerFactory.getLogger(CPIAdapterImpl.class.getName());

	@Autowired
	private CPI cpi;


	@Override
	public CPIResponse execute(JsonNode json) {

		
		ObjectMapper mapper=new ObjectMapper();
		//prepare response
		CPIResponse response = new CPIResponse();
		
		try {

			String method = json.get("method").asText();
			logger.info("method : {}", method);
			
			Iterator<JsonNode> args=json.get("arguments").elements();


			if (method.equals(CREATE_DISK)) {
				Integer size=args.next().asInt();
				Map<String, String> cloud_properties=mapper.convertValue(args.next(), HashMap.class);
				String diskId=this.cpi.create_disk(size,cloud_properties);
				response.result.add(diskId);

			} else if (method.equals(DELETE_DISK)) {
				String disk_id=args.next().asText();
				this.cpi.delete_disk(disk_id);


			} else if (method.equals(ATTACH_DISK)) {
				String vm_id=args.next().asText();
				String disk_id=args.next().asText();;
				this.cpi.attach_disk(vm_id, disk_id);

			} else if (method.equals(DETACH_DISK)) {
				String vm_id=args.next().asText();
				String disk_id=args.next().asText();
				this.cpi.detach_disk(vm_id, disk_id);

			} else if (method.equals(CREATE_VM)) {
				String agent_id=args.next().asText();
				String stemcell_id=args.next().asText();;
				ResourcePool resource_pool=this.parseResourcePool(args.next());
				Networks networks=this.parseNetwork(args.next());
				
				List<String> disk_locality=mapper.convertValue(args.next(), List.class);
				//TODO: log and parse if a disk hint is provided by director
				//see: https://github.com/cloudfoundry/bosh/issues/945
				
				
				JsonNode env=args.next();

				String vmId=this.cpi.create_vm(agent_id, stemcell_id, resource_pool, networks, disk_locality, env);
				response.result.add(vmId);
				
				
			} else if (method.equals(REBOOT_VM)) {
				String vm_id=args.next().asText();
				this.cpi.reboot_vm(vm_id);

			} else if (method.equals(SET_VM_METADATA)) {
				String vm_id=args.next().asText();
				Map<String, String> metadata=mapper.convertValue(args.next(), HashMap.class);
				this.cpi.set_vm_metadata(vm_id, metadata);
				
			} else if (method.equals(DELETE_VM)) {
				String vm_id=args.next().asText();
				this.cpi.delete_vm(vm_id);
			} else if (method.equals(HAS_VM)) {
				String vm_id=args.next().asText();
				boolean hasVm=this.cpi.has_vm(vm_id);
				response.result.add(new Boolean(hasVm));

			}  else if (method.equals(HAS_DISK)) {
				String disk_id=args.next().asText();
				boolean hasDisk=this.cpi.has_disk(disk_id);
				response.result.add(new Boolean(hasDisk));
			
			} else if (method.equals(CREATE_STEMCELL)) {
				String image_path=args.next().asText();				
				Map<String, Object> cloud_properties=mapper.convertValue(args.next(), HashMap.class);
				
				String stemcell=this.cpi.create_stemcell(image_path, cloud_properties);
				response.result.add(stemcell);

			} else if (method.equals(DELETE_STEMCELL)) {
				String stemcell_id=args.next().asText();				
				this.cpi.delete_stemcell(stemcell_id);
			} else if (method.equals("configure_networks")) {
				String vm_id=args.next().asText();
				Networks networks=this.parseNetwork(args.next());				
				this.cpi.configure_networks(vm_id, networks);
			} 
			
			else
				throw new IllegalArgumentException("Unknown method :" + method);

			
			return response;

		}
		 
		catch (CPIException e) {
		 logger.error("Caught Exception {}, converted to CPI response.", e);
			CmdError err=new CmdError();

			err.type=e.toString();
			err.message=e.toString() + "\n" + e.getMessage() + "\n" + e.getCause();
			response.error=err;
			
			Writer result = new StringWriter();
		    PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
		    response.log=result.toString();
			
			return response;
		
		 }

		catch (Exception e) {
			logger.error("Caught Exception {}, converted to CPI response.", e);
			CmdError err=new CmdError();
			err.message=e.toString() + "\n" + e.getMessage() + "\n" + e.getCause();
			response.error=err;
			
			Writer result = new StringWriter();
		    PrintWriter printWriter = new PrintWriter(result);
		    e.printStackTrace(printWriter);
		    response.log=result.toString();
			
			return response;
		}

	}
	
	
	
	/**
	 * Utility to parse JSON resource pool
	 * @param resource_pool
	 * @return
	 */
	private ResourcePool parseResourcePool(JsonNode resource_pool) {
    	ObjectMapper mapper=new ObjectMapper();
    	ResourcePool rp=mapper.convertValue(resource_pool, ResourcePool.class);
    	return rp;
	}
	
	/**
	 * Utility to parse JSON network list
	 * @param networks
	 * @return
	 */
    private Networks parseNetwork(JsonNode networks) {
    	ObjectMapper mapper=new ObjectMapper();
    	
		Networks nets = new Networks();
		Iterator<Entry<String, JsonNode>> it = networks.fields();
		while (it.hasNext()) {

			Entry<String, JsonNode> entry = it.next();
			String networkName = entry.getKey();
			JsonNode node = entry.getValue();

			nets.networks.put(networkName,
					mapper.convertValue(node, Network.class));
		}
    	
		//TODO: at least 1 network
		
    	//consistency check
    	for (Network n:nets.networks.values()){

    		
    		
    		//FIXME: compilation vm create_vm do not provide network type. assume manual defaut
    		switch (n.type){
			case manual:
				Assert.notNull(n.ip,"must provide ip  with manual (static) network");
				Assert.notNull(n.gateway,"must provide gateway  with manual (static) network");
				Assert.notNull(n.netmask,"must provide netmask with manual (static) network");
				
				Assert.notNull(n.cloud_properties.get("name"),"A name for the target network is required in cloud_properties");				
				break;

    		case vip:
				Assert.isNull(n.gateway,"must not provide ip / gateway / netmask with dynamic/vip network");
				Assert.isNull(n.netmask,"must not provide ip / gateway / netmask with dynamic/vip network");
				break;
    			
    			
    		case dynamic:
				//Assert.isNull(n.ip,"must not provide ip / gateway / netmask with dynamic/vip network");
				//Assert.isNull(n.gateway,"must not provide ip / gateway / netmask with dynamic/vip network");
				//Assert.isNull(n.netmask,"must not provide ip / gateway / netmask with dynamic/vip network");
				Assert.notNull(n.cloud_properties.get("name"),"A name for the target network is required in cloud_properties");				
				break;
    		default:
    			logger.warn("network type not specified for {}",n.toString());
    		}
    	}

    	

		return nets;
	}
	
}
