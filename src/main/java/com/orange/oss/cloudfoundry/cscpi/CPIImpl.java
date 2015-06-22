package com.orange.oss.cloudfoundry.cscpi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.*;
import org.jclouds.cloudstack.domain.VirtualMachine.State;
import org.jclouds.cloudstack.features.VolumeApi;
import org.jclouds.cloudstack.options.CreateSnapshotOptions;
import org.jclouds.cloudstack.options.DeployVirtualMachineOptions;
import org.jclouds.cloudstack.options.ListDiskOfferingsOptions;
import org.jclouds.cloudstack.options.ListServiceOfferingsOptions;
import org.jclouds.cloudstack.options.ListTemplatesOptions;
import org.jclouds.cloudstack.options.ListVirtualMachinesOptions;
import org.jclouds.cloudstack.options.ListVolumesOptions;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.jclouds.cloudstack.predicates.JobComplete;
import org.jclouds.cloudstack.strategy.BlockUntilJobCompletesAndReturnResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.util.Predicates2.retry;

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

		
		//TODO: find csTemplateId from name when template generation is OK
		String csTemplateId=stemcell_id;
        
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
		

		AsyncCreateResponse job = api.getVirtualMachineApi().deployVirtualMachineInZone(zoneId, so.getId(), csTemplateId, options);
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(job.getJobId());
		
//		AsyncJob<VirtualMachine> jobWithResult = api.getAsyncJobApi().<VirtualMachine> getAsyncJob(job.getId());
//		if (jobWithResult.getError() != null) {
//			throw new RuntimeException("Failed with:" + jobWithResult.getError());
//		}
		
		
		VirtualMachine vm = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vmName)).iterator().next();
		if (! vm.getState().equals(State.RUNNING)) {
			throw new RuntimeException("Not in expectedrunning:" + vm.getState());
		}

		logger.info("vm creation completed, now running ! {}");

        return vmName;
    }


	@Override
	public void initialize(Map<String, String> options) {
		logger.info("",options);
		
	}

	@Override
	public String current_vm_id() {
		logger.info("current_vm_id");
		//FIXME : strange API ? must keep state in CPI with latest changed / created vm ??
		return null;
	}

	@Override
	public String create_stemcell(String image_path,
			Map<String, String> cloud_properties) {
		logger.info("create_stemcell");
		
		//map stemcell to cloudstack template concept.
		
		//FIXME: change with template generation, for now use existing centos template
		String stemcellId="cpitemplate-"+UUID.randomUUID();
		
		String csTemplateId=api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name("CentOS 5.6(64-bit) no GUI (XenServer)")).iterator().next().getId();
		
		return csTemplateId;
	}

	@Override
	public void delete_stemcell(String stemcell_id) {
		logger.info("delete_stemcell");
		
	}

	@Override
	public void delete_vm(String vm_id) {
		logger.info("delete_vm");
		
		//FIXME : check vm is existing
		String csVmId=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next().getId();
		api.getVirtualMachineApi().destroyVirtualMachine(csVmId);
		
		logger.info("deleted successfully vm {}",vm_id);
	}

	@Override
	public boolean has_vm(String vm_id) {
		logger.info("has_vm ?");
		VirtualMachine vm=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name("vm_id")).iterator().next();

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
		
		//FIXME see disk offering (cloud properties specificy?)
		
		String name="cpidisk-"+UUID.randomUUID().toString();
		
		//find disk offering
		
		//TODO: Custom disk offering possible, but cant delete vol ?
		
		
		//String diskOfferingName = "Small";
		String diskOfferingName = "Custom";
		String diskOfferingId=api.getOfferingApi().listDiskOfferings(ListDiskOfferingsOptions.Builder.name(diskOfferingName)).iterator().next().getId();
		//FIXME assert a single offering found
		
		
		String zoneId=this.findZoneId();
		api.getVolumeApi().createVolumeFromCustomDiskOfferingInZone(name, diskOfferingId, zoneId, size);
		
		
		return name;

	}

	@Override
	public void delete_disk(String disk_id) {
		logger.info("delete_disk");
		
		String csDiskId=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id)).iterator().next().getId();
		api.getVolumeApi().deleteVolume(csDiskId);
	}

	@Override
	public void attach_disk(String vm_id, String disk_id) {
		logger.info("attach disk");
		String csDiskId=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id)).iterator().next().getId();
		String csVmId=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next().getId();
		
		VolumeApi vol = this.api.getVolumeApi();
		AsyncCreateResponse resp=vol.attachVolume(csDiskId, csVmId);
		//TODO: wait for attachment ? need to restart vm ?
		
		logger.info("==> detach disk successfull");
	}

	@Override
	public String snapshot_disk(String disk_id, Map<String, String> metadata) {
		logger.info("snapshot disk");
		
		String csDiskId=api.getVolumeApi().getVolume(disk_id).getId();
		AsyncCreateResponse async = api.getSnapshotApi().createSnapshot(csDiskId,CreateSnapshotOptions.Builder.domainId("domain"));
		//FIXME
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
		String csDiskId=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id)).iterator().next().getId();		
		AsyncCreateResponse resp=api.getVolumeApi().detachVolume(csDiskId);
		logger.info("==> detach disk successfull");
		
	}

	@Override
	public List<String> get_disks(String vm_id) {
		logger.info("get_disks");

		VirtualMachine vm=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next();		

		VolumeApi vol = this.api.getVolumeApi();
		Set<Volume> vols=vol.listVolumes(ListVolumesOptions.Builder.virtualMachineId(vm.getId()));

		ArrayList<String> disks = new ArrayList<String>();
		Iterator<Volume> it=vols.iterator();
		while (it.hasNext()){
			Volume v=it.next();
			String disk_id=v.getName();
			disks.add(disk_id);
		}
		return disks;
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
