package com.orange.oss.cloudfoundry.cscpi;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.AsyncCreateResponse;
import org.jclouds.cloudstack.domain.ServiceOffering;
import org.jclouds.cloudstack.domain.VirtualMachine;
import org.jclouds.cloudstack.domain.Volume;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.features.VolumeApi;
import org.jclouds.cloudstack.options.DeployVirtualMachineOptions;
import org.jclouds.cloudstack.options.ListDiskOfferingsOptions;
import org.jclouds.cloudstack.options.ListServiceOfferingsOptions;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.jclouds.cloudstack.predicates.JobComplete;
import org.jclouds.cloudstack.predicates.VirtualMachineRunning;
import org.jclouds.cloudstack.strategy.BlockUntilJobCompletesAndReturnResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;

/**
 * Implementation of the CPI API, translating to CloudStack jclouds API calls
 * 
 * 
 * @see: http://www.programcreek.com/java-api-examples/index.php?api=org.jclouds.predicates.RetryablePredicate
 * @see:
 * 
 */
public class CPIImpl implements CPI{

	private static Logger logger=LoggerFactory.getLogger(CPIImpl.class);
	

	
	@Value("${cloudstack.state_timeout}")	
	int state_timeout;

	@Value("${cloudstack.state_timeout_volume}")	
	int state_timeout_volume;

	@Value("${cloudstack.stemcell_public_visibility}")	
	boolean stemcell_public_visibility;

	@Value("${cloudstack.default_zone}")	
	String  default_zone;

	
	
	protected Predicate<String> jobComplete;

	@Autowired
	private CloudStackApi api;

	
	
	/**
	 * creates a vm
	 * 
	 * @param agent_id
	 * @param stemcell_id
	 * @param resource_pool
	 * @param networks
	 * @param disk_locality
	 * @param env
	 * @return
	 */
    public String create_vm(String agent_id,
                            String stemcell_id,
                            JsonNode resource_pool,
                            JsonNode networks,
                            List<String> disk_locality,
                            Map<String,String> env) {

        ObjectMapper mapper = new ObjectMapper();

//        
//        Template template = api.getVirtualMachineApi().templateBuilder()
//        	    .osFamily(OsFamily.UBUNTU)
//        	    .minRam(2048)
//        	    .options(inboundPorts(22, 80))
//        	    .build();
		

        String vmName="cpivm-"+UUID.randomUUID().toString();
		String serviceOfferingName="Ultra Tiny";
		String templateId=stemcell_id;
        
		String zoneId = findZoneId();
		
		
		
		//find offering
		Set<ServiceOffering> s = api.getOfferingApi().listServiceOfferings(ListServiceOfferingsOptions.Builder.name(serviceOfferingName));
		//FIXME assert a single offering
		ServiceOffering so=s.iterator().next();

		//set options
        long dataDiskSize=100;
        String userData="testdata=zzz";
        
		DeployVirtualMachineOptions options=DeployVirtualMachineOptions.Builder
        			.userData(userData.getBytes())
        			.dataDiskSize(dataDiskSize)
        			.name(vmName);
		
		
		AsyncCreateResponse job = api.getVirtualMachineApi().deployVirtualMachineInZone(zoneId, so.getId(), templateId, options);
		
		jobComplete = new JobComplete(api);
		
		
		BlockUntilJobCompletesAndReturnResult blockUntilJobCompletesAndReturnResult=new BlockUntilJobCompletesAndReturnResult(this.api,jobComplete);
		VirtualMachine vm = blockUntilJobCompletesAndReturnResult.<VirtualMachine>apply(job);
        
        return vm.getId();
    }


	@Override
	public void initialize(Map<String, String> options) {
		logger.info("",options);
		
	}

	@Override
	public String current_vm_id() {
		logger.info("current_vm_id");
		return null;
	}

	@Override
	public String create_stemcell(String image_path,
			Map<String, String> cloud_properties) {
		logger.info("current_stemcell");
		return null;
	}

	@Override
	public void delete_stemcell(String stemcell_id) {
		logger.info("delete_stemcell");
		
	}

	@Override
	public void delete_vm(String vm_id) {
		logger.info("delete_vm");
		
	}

	@Override
	public boolean has_vm(String vm_id) {
		logger.info("has_vm ?");
		
		VirtualMachine vm = api.getVirtualMachineApi().getVirtualMachine(vm_id);
		if (vm==null) return false;
		return true;
		
	}

	@Override
	public boolean has_disk(String disk_id) {
		logger.info("has_disk ?");
		
		Volume vol = api.getVolumeApi().getVolume(disk_id);
		if (vol==null) return false;
		return true;
	}

	@Override
	public void reboot_vm(String vm_id) {
		logger.info("reboot_vm");
		api.getVirtualMachineApi().rebootVirtualMachine(vm_id);

		
	}

	@Override
	public void set_vm_metadata(String vm_id, Map<String, String> metadata) {
		logger.info("set vm metadata");
		
		
	}

	@Override
	public void configure_networks(String vm_id, JsonNode networks) {
		logger.info("configure network");
		
	}
	
	
	

	@Override
	public String create_disk(Integer size, Map<String, String> cloud_properties) {
		logger.info("create_disk");
		
		//generate random disk id
		
		//FIXME see disk offering (cloud properties specificy?)
		
		String name="cpidisk-"+UUID.randomUUID().toString();
		
		//find disk offering
		
		//TODO: Custom disk offering possible, but cant delete vol ?
		String diskOfferingId=api.getOfferingApi().listDiskOfferings(ListDiskOfferingsOptions.Builder.name("Small")).iterator().next().getId();
		//FIXME assert a single offering found
		
		
		String zoneId=this.findZoneId();
		api.getVolumeApi().createVolumeFromCustomDiskOfferingInZone(name, diskOfferingId, zoneId, size);
		
		return name;

	}

	@Override
	public void delete_disk(String disk_id) {
		logger.info("delete_disk");
		api.getVolumeApi().deleteVolume(disk_id);
		
	}

	@Override
	public void attach_disk(String vm_id, String disk_id) {
		logger.info("attach disk");
		VolumeApi vol = this.api.getVolumeApi();
		AsyncCreateResponse resp=vol.attachVolume(disk_id, vm_id);
		
		
	}

	@Override
	public String snapshot_disk(String disk_id, Map<String, String> metadata) {
		logger.info("snapshot disk");
		//TODO
		return null;
	}

	@Override
	public void delete_snapshot(String snapshot_id) {
		logger.info("delete snapshot");
		//TODO
	}

	@Override
	public void detach_disk(String vm_id, String disk_id) {
		logger.info("detach disk");
		VolumeApi vol = this.api.getVolumeApi();
		AsyncCreateResponse resp=vol.detachVolume(disk_id);
		
		
	}

	@Override
	public List<String> get_disks(String vm_id) {
		logger.info("get_disks");
		VirtualMachine vm=api.getVirtualMachineApi().getVirtualMachine(vm_id);
		
		 VolumeApi vol = this.api.getVolumeApi();
		 Set<Volume> vols=vol.listVolumes();
		 //FIXME : fix search volume (option to target a single vm)
		return null;
	}
    
  
	/**
	 * utility to retrieve cloudstack zoneId
	 * @return
	 */
	private String findZoneId() {
		//find zone
        ListZonesOptions zoneOptions=ListZonesOptions.Builder.available(true);
		Set<Zone> zones = api.getZoneApi().listZones(zoneOptions);
		//FIXME: assert a single zone matching
		Zone zone=zones.iterator().next();
		String zoneId = zone.getId();
		return zoneId;
	}

    
    
}
