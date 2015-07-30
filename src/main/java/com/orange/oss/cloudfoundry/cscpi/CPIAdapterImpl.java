package com.orange.oss.cloudfoundry.cscpi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse;

public class CPIAdapterImpl implements CPIAdapter {

	private static Logger logger = LoggerFactory.getLogger(CPIAdapterImpl.class.getName());

	@Autowired
	private CPI cpi;


	@Override
	public CPIResponse execute(JsonNode json) {

		//prepare response
		CPIResponse response = new CPIResponse();
		
		try {

			String method = json.get("method").asText();
			logger.info("method : {}", method);

			String arguments = json.get("arguments").asText();
			logger.info("arguments : {}", arguments);

			String context = json.get("context").asText();
			logger.info("context : {}", context);
			
			Iterator<JsonNode> args=json.get("arguments").elements();
			
			

			if (method.equals("create_disk")) {
				Integer size=args.next().asInt();
				Map<String, String> cloud_properties=new HashMap<String, String>();
				String diskId=this.cpi.create_disk(size,cloud_properties);
				response.result.add(diskId);

			} else if (method.equals("delete_disk")) {
				String disk_id=args.next().asText();
				this.cpi.delete_disk(disk_id);


			} else if (method.equals("attach_disk")) {
				String vm_id=args.next().asText();
				String disk_id=args.next().asText();;
				this.cpi.attach_disk(vm_id, disk_id);

			} else if (method.equals("detach_disk")) {
				String vm_id=args.next().asText();
				String disk_id=args.next().asText();
				this.cpi.detach_disk(vm_id, disk_id);

			} else if (method.equals("create_vm")) {

				//FIXME: TODO
				String agent_id=args.next().asText();;
				String stemcell_id=args.next().asText();;
				JsonNode resource_pool=args.next();
				JsonNode networks=args.next();
				
				List<String> disk_locality=new ArrayList<String>();				
				Map<String, String> env=new HashMap<String, String>();

				String vmId=this.cpi.create_vm(agent_id, stemcell_id, resource_pool, networks, disk_locality, env);
				response.result.add(vmId);
				
				
			} else if (method.equals("reboot_vm")) {
				String vm_id=args.next().asText();
				this.cpi.reboot_vm(vm_id);

			} else if (method.equals("set_vm_metadata")) {
				String vm_id=args.next().asText();
				Map<String, String> metadata=null;
				//TODO: parse map
				JsonNode jsonMap=args.next();
				
				Iterator<JsonNode> it=jsonMap.elements();
				while (it.hasNext()){
					JsonNode keyValue=it.next();
					String keyValueString=keyValue.toString();
					
				}
				
				this.cpi.set_vm_metadata(vm_id, metadata);
				
			} else if (method.equals("delete_vm")) {
				String vm_id=args.next().asText();
				this.cpi.delete_vm(vm_id);

			} else if (method.equals("create_stemcell")) {
				String image_path=args.next().asText();				
				Map<String, String> cloud_properties=new HashMap<String, String>();
				//TODO: map params
				String stemcell=this.cpi.create_stemcell(image_path, cloud_properties);

			} else
				throw new IllegalArgumentException("Unknown method :" + method);

			
			return response;

		}
//		 catch (CPIException e) {
//		 logger.error("Caught Exception {}, converted to CPI response.", e);
//		 CPIResponse response = new CPIResponse();
//		 response.error = e.toString() + "\n" + e.getMessage() + "\n"
//		 + e.getCause();
//		 return response;
//		
//		 }

		catch (Exception e) {
			logger.error("Caught Exception {}, converted to CPI response.", e);
			response.error = e.toString() + "\n" + e.getMessage() + "\n" + e.getCause();
			return response;
		}

	}
}
