package com.orange.oss.cloudfoundry.cscpi;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.util.Predicates2.retry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.AsyncCreateResponse;
import org.jclouds.cloudstack.domain.Network;
import org.jclouds.cloudstack.domain.NetworkOffering;
import org.jclouds.cloudstack.domain.OSType;
import org.jclouds.cloudstack.domain.ServiceOffering;
import org.jclouds.cloudstack.domain.Template;
import org.jclouds.cloudstack.domain.TemplateMetadata;
import org.jclouds.cloudstack.domain.VirtualMachine;
import org.jclouds.cloudstack.domain.VirtualMachine.State;
import org.jclouds.cloudstack.domain.Volume;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.features.VolumeApi;
import org.jclouds.cloudstack.options.CreateSnapshotOptions;
import org.jclouds.cloudstack.options.CreateTemplateOptions;
import org.jclouds.cloudstack.options.DeployVirtualMachineOptions;
import org.jclouds.cloudstack.options.ListDiskOfferingsOptions;
import org.jclouds.cloudstack.options.ListNetworkOfferingsOptions;
import org.jclouds.cloudstack.options.ListNetworksOptions;
import org.jclouds.cloudstack.options.ListOSTypesOptions;
import org.jclouds.cloudstack.options.ListServiceOfferingsOptions;
import org.jclouds.cloudstack.options.ListTemplatesOptions;
import org.jclouds.cloudstack.options.ListVirtualMachinesOptions;
import org.jclouds.cloudstack.options.ListVolumesOptions;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.jclouds.cloudstack.predicates.JobComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

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

        //TODO parse from resource pool
		//String instance_type="Ultra Tiny";
		String instance_type="CO1 - Small STD";
		
		
		//TODO parse from networks
		
		//String network_name="elpaaso-network";
		//String network_name="DefaultIsolatedNetworkOfferingWithSourceNat";
		String network_name="DefaultIsolatedNetworkOffering";
		

        String vmName="cpivm-"+UUID.randomUUID().toString();
		
		this.vmCreation(stemcell_id, instance_type, network_name, vmName);

        return vmName;
    }


	private void vmCreation(String stemcell_id, String instance_type,
			String network_name, String vmName) {
		
		Set<Template> matchingTemplates=api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name(stemcell_id));
		
		Template stemCellTemplate=matchingTemplates.iterator().next();
		String csTemplateId=stemCellTemplate.getId();
		logger.info("found cloudstack template {} matching name / stemcell_id {}",csTemplateId,stemcell_id );
        
		String csZoneId = findZoneId();
		
		//find compute offering
		Set<ServiceOffering> s = api.getOfferingApi().listServiceOfferings(ListServiceOfferingsOptions.Builder.name(instance_type));
		//FIXME assert a single offering
		ServiceOffering so=s.iterator().next();
		
		
		//find network offering
		//TODO: endusers choose offering or indicate precise network id ??
		Set<NetworkOffering> listNetworkOfferings = api.getOfferingApi().listNetworkOfferings(ListNetworkOfferingsOptions.Builder.zoneId(csZoneId).name(network_name));		
		NetworkOffering networkOffering=listNetworkOfferings.iterator().next();
		
		Network network=api.getNetworkApi().listNetworks(ListNetworksOptions.Builder.zoneId(csZoneId)).iterator().next();

		//set options
        long dataDiskSize=100;
        String userData="testdata=zzz";
        
		DeployVirtualMachineOptions options=DeployVirtualMachineOptions.Builder
					.name(vmName)
					.networkId(network.getId())
					.userData(userData.getBytes())
        			.dataDiskSize(dataDiskSize)
        			;
		

		AsyncCreateResponse job = api.getVirtualMachineApi().deployVirtualMachineInZone(csZoneId, so.getId(), csTemplateId, options);
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(job.getJobId());
		
		VirtualMachine vm = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vmName)).iterator().next();
		if (! vm.getState().equals(State.RUNNING)) {
			throw new RuntimeException("Not in expectedrunning:" + vm.getState());
		}

		logger.info("vm creation completed, now running ! {}");
	}


	@Override
	public void initialize(Map<String, String> options) {
		logger.info("",options);
		
	}

	@Override
	public String current_vm_id() {
		logger.info("current_vm_id");
		//FIXME : strange API ? must keep state in CPI with latest changed / created vm ?? Or find current vm running cpi ? by IP / hostname ?
		return null;
	}

	
	
	/**
	 * 
	 * first try : create a vm from an existing template, stop it, create template from vm, delete work vm
	 * 
	 * @param image_path
	 * @param cloud_properties
	 * @return
	 */
	@Override
	public String create_stemcell(String image_path,
			Map<String, String> cloud_properties) {
		logger.info("create_stemcell");
		
		
		//FIXME: change with template generation, for now use existing cloustack template
		String existingTemplateName="Ubuntu Trusty amd64 [2015-06-01]"; //ubuntu precise template
		
		
		
		//String instance_type="Ultra Tiny";
		String instance_type="CO1 - Small STD";
		
		//FIXME : should parameter the network offering
		//String network_name="DefaultIsolatedNetworkOfferingWithSourceNat";
		String network_name="DefaultIsolatedNetworkOfferingWithSourceNatService";	
		
		
		logger.info("CREATING work vm for template generation");
		//map stemcell to cloudstack template concept.
		String workVmName="cpi-stemcell-work-"+UUID.randomUUID();
		this.vmCreation(existingTemplateName, instance_type, network_name, workVmName);
		VirtualMachine m=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(workVmName)).iterator().next();
		
		logger.info("STOPPING work vm for template generation");
		String stopJob=api.getVirtualMachineApi().stopVirtualMachine(m.getId());
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(stopJob);
		
		logger.info("Work vm stopped. now creating template from it its ROOT Volume");
		
		
		Volume rootVolume=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.virtualMachineId(m.getId())).iterator().next(); //hopefully, fist volume is ROOT ?

		
		//FIXME : template name limited to 32 chars, UUID is longer. use Random
		Random randomGenerator=new Random();
		String stemcellId="cpitemplate-"+randomGenerator.nextInt(1000);
		
		//FIXME: find correct os type (PVM 64 bits)
		OSType osType=null;
		for (OSType ost:api.getGuestOSApi().listOSTypes()){
			if (ost.getDescription().equals("Other PV (64-bit)")) osType=ost;
		}
		
		Assert.notNull(osType, "Unable to find OsType");
		
		TemplateMetadata templateMetadata=TemplateMetadata.builder()
				.name(stemcellId)
				.osTypeId(osType.getId())
				.volumeId(rootVolume.getId())
				.displayText("generated cpi stemcell template")
				.build();
		
		CreateTemplateOptions options=CreateTemplateOptions.Builder
				.isPublic(true)
				.isFeatured(true);
		
		AsyncCreateResponse asyncTemplateCreateJob =api.getTemplateApi().createTemplate(templateMetadata, options);
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(asyncTemplateCreateJob.getJobId());

		logger.info("Template successfully created ! {} - {}",stemcellId);
		
		logger.info("now cleaning work vm");
		this.delete_vm(workVmName);
		
		return stemcellId;
	}

	


	@Override
	public void delete_stemcell(String stemcell_id) {
		logger.info("delete_stemcell");
		
		//FIXME : implement
		
	}

	@Override
	public void delete_vm(String vm_id) {
		logger.info("delete_vm");
		
		//FIXME : check vm is existing
		String csVmId=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next().getId();
		String jobId=api.getVirtualMachineApi().destroyVirtualMachine(csVmId);
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(jobId);

		
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
		
		VirtualMachine vm=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next();
		String rebootJob=api.getVirtualMachineApi().rebootVirtualMachine(vm.getId());
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(rebootJob);
		
		logger.info("done rebooting vm {}",vm_id);
		
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
		
		//TODO: Custom disk offering possible, custom size ? how to manage size ?
		//String diskOfferingName = "Custom";
		String diskOfferingName = "DO1 - Small STD";
		String diskOfferingId=api.getOfferingApi().listDiskOfferings(ListDiskOfferingsOptions.Builder.name(diskOfferingName)).iterator().next().getId();
		//FIXME assert a single offering found
		
		
		String zoneId=this.findZoneId();
		//api.getVolumeApi().createVolumeFromCustomDiskOfferingInZone(diskOfferingName, diskOfferingId, zoneId, size);
		AsyncCreateResponse resp=api.getVolumeApi().createVolumeFromDiskOfferingInZone(name, diskOfferingId, zoneId);
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(resp.getJobId());
		
		logger.info("disk {} successfully created ",name);
		
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
		//TODO:  need to restart vm ?
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(resp.getJobId());

		
		logger.info("==> detach disk successfull");
	}

	@Override
	public String snapshot_disk(String disk_id, Map<String, String> metadata) {
		logger.info("snapshot disk");
		
		String csDiskId=api.getVolumeApi().getVolume(disk_id).getId();
		AsyncCreateResponse async = api.getSnapshotApi().createSnapshot(csDiskId,CreateSnapshotOptions.Builder.domainId("domain"));
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(async.getJobId());
		
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
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(resp.getJobId());
		
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
