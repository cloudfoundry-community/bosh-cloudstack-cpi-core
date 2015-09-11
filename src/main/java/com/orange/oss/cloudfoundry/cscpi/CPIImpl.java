package com.orange.oss.cloudfoundry.cscpi;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.util.Predicates2.retry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.AsyncCreateResponse;
import org.jclouds.cloudstack.domain.DiskOffering;
import org.jclouds.cloudstack.domain.NIC;
import org.jclouds.cloudstack.domain.Network;
import org.jclouds.cloudstack.domain.NetworkOffering;
import org.jclouds.cloudstack.domain.OSType;
import org.jclouds.cloudstack.domain.ServiceOffering;
import org.jclouds.cloudstack.domain.Tag;
import org.jclouds.cloudstack.domain.Template;
import org.jclouds.cloudstack.domain.Template.Status;
import org.jclouds.cloudstack.domain.TemplateMetadata;
import org.jclouds.cloudstack.domain.VirtualMachine;
import org.jclouds.cloudstack.domain.VirtualMachine.State;
import org.jclouds.cloudstack.domain.Volume;
import org.jclouds.cloudstack.domain.Volume.Type;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.features.VolumeApi;
import org.jclouds.cloudstack.options.CreateSnapshotOptions;
import org.jclouds.cloudstack.options.CreateTagsOptions;
import org.jclouds.cloudstack.options.CreateTemplateOptions;
import org.jclouds.cloudstack.options.DeleteTemplateOptions;
import org.jclouds.cloudstack.options.DeployVirtualMachineOptions;
import org.jclouds.cloudstack.options.ListDiskOfferingsOptions;
import org.jclouds.cloudstack.options.ListNetworkOfferingsOptions;
import org.jclouds.cloudstack.options.ListNetworksOptions;
import org.jclouds.cloudstack.options.ListServiceOfferingsOptions;
import org.jclouds.cloudstack.options.ListTagsOptions;
import org.jclouds.cloudstack.options.ListTemplatesOptions;
import org.jclouds.cloudstack.options.ListVirtualMachinesOptions;
import org.jclouds.cloudstack.options.ListVolumesOptions;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.jclouds.cloudstack.options.RegisterTemplateOptions;
import org.jclouds.cloudstack.predicates.JobComplete;
import org.jclouds.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.orange.oss.cloudfoundry.cscpi.boshregistry.BoshRegistryClient;
import com.orange.oss.cloudfoundry.cscpi.config.CloudStackConfiguration;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CPIException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.VMCreationFailedException;
import com.orange.oss.cloudfoundry.cscpi.webdav.WebdavServerAdapter;

/**
 * Implementation of the CPI API, translating to CloudStack jclouds API calls
 * 
 * 
 * @see: http://www.programcreek.com/java-api-examples/index.php?api=org.jclouds.predicates.RetryablePredicate
 * @see:
 * 
 */
public class CPIImpl implements CPI{

	private static final String CPI_OS_TYPE = "Other PV (64-bit)";

	private static final String CPI_VM_PREFIX = "cpivm-";

	private static final String CPI_PERSISTENT_DISK_PREFIX = "cpi-disk-";

	private static final String CPI_EPHEMERAL_DISK_PREFIX = "cpi-ephemeral-disk-";

	private static Logger logger=LoggerFactory.getLogger(CPIImpl.class);
	
	@Autowired
	private CloudStackConfiguration cloudstackConfig;
	
	@Autowired
	private CloudStackApi api;
	
	@Autowired
	UserDataGenerator userDataGenerator;
	
	@Autowired
	VmSettingGenerator vmSettingGenerator;
	
	@Autowired
	private WebdavServerAdapter webdav;
	
	@Autowired
	private BoshRegistryClient boshRegistry;

	
	private  Predicate<String> jobComplete;
	
	/**
	 * creates a vm.
	 * take the stemcell_id as cloudstack template name.
	 * create the vm on the correct network configuration
	 * 	static
	 * 	vip
	 * 	floating
	 * create an "ephemeral disk" and attach it the the vm
	 * 
	 * @param agent_id
	 * @param stemcell_id
	 * @param resource_pool
	 * @param networks
	 * @param disk_locality
	 * @param env
	 * @return
	 * @throws VMCreationFailedException 
	 */
    public String create_vm(String agent_id,
                            String stemcell_id,
                            ResourcePool resource_pool,
                            Networks networks,
                            List<String> disk_locality,
                            Map<String,String> env) throws VMCreationFailedException {
        
        String compute_offering=resource_pool.compute_offering;
        Assert.isTrue(compute_offering!=null,"Must provide compute offering in vm ressource pool");
        
        String affinityGroup=resource_pool.affinity_group;
        if (affinityGroup!=null) {
        	logger.info("an affinity group {} has been specified for create_vm",affinityGroup);
        }
        
        String vmName=CPI_VM_PREFIX+UUID.randomUUID().toString();
		
		logger.info("now creating cloudstack vm");
        //cloudstack userdata generation for bootstrap
        String userData=this.userDataGenerator.userMetadata(vmName,networks);

		this.vmCreation(stemcell_id, compute_offering, networks, vmName,agent_id,userData);

		
		//create ephemeral disk, read the disk size from properties, attach it to the vm.
		//NB: if base ROOT disk is large enough, bosh agent can use it to hold swap / ephemeral data. CPI forces an external vol for ephemeral
	    String ephemeralDiskServiceOfferingName=resource_pool.ephemeral_disk_offering;
	    if (ephemeralDiskServiceOfferingName==null) {
	    	ephemeralDiskServiceOfferingName=this.cloudstackConfig.defaultEphemeralDiskOffering;
	    	logger.info("no ephemeral_disk_offering specified in cloud_properties. use global CPI default ephemeral disk offering {}",ephemeralDiskServiceOfferingName);
	    }
	    logger.debug("ephemeral disk offering is {}",ephemeralDiskServiceOfferingName);

	    logger.info("now creating ephemeral disk");
	    int ephemeralDiskSize=resource_pool.disk/1024; //cloudstack size api is Go
		String name=CPI_EPHEMERAL_DISK_PREFIX+UUID.randomUUID().toString();
		String ephemeralDiskName=this.diskCreate(name,ephemeralDiskSize,ephemeralDiskServiceOfferingName);
		
		//NOW attache the ephemeral disk to the vm (need reboot ?)
		//FIXME : placement constraint local disk offering / vm
		logger.info("now attaching ephemeral disk {} to cloudstack vm {}",ephemeralDiskName,vmName);		
		this.diskAttachment(vmName, ephemeralDiskName);
		
		//FIXME: if attach fails, clean both vm and ephemeral disk ??
		
		
		
		//FIXME: registry feeding in vmCreation method. refactor here ?
		
        return vmName;
    }




    /**
     * Cloudstack vm creation.
     * @param stemcell_id
     * @param compute_offering
     * @param networks
     * @param vmName
     * @throws VMCreationFailedException 
     */
	private void vmCreation(String stemcell_id, String compute_offering,
			Networks networks, String vmName,String agent_id,String userData) throws VMCreationFailedException {
	
		
		
		Set<Template> matchingTemplates=api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name(stemcell_id));
		Assert.isTrue(matchingTemplates.size()==1,"Did not find a single template with name "+stemcell_id);
		Template stemCellTemplate=matchingTemplates.iterator().next();
		
		
		String csTemplateId=stemCellTemplate.getId();
		logger.info("found cloudstack template {} matching name / stemcell_id {}",csTemplateId,stemcell_id );
        
		String csZoneId = findZoneId();
		
		//find compute offering
		Set<ServiceOffering> computeOfferings = api.getOfferingApi().listServiceOfferings(ListServiceOfferingsOptions.Builder.name(compute_offering));
		
		Assert.isTrue(computeOfferings.size()>0, "Unable to find compute offering "+compute_offering);
		ServiceOffering so=computeOfferings.iterator().next();
		

		//parse network from cloud_properties
		Assert.isTrue(networks.networks.size()==1, "CPI currenly only support 1 network / nic per VM");
		String directorNetworkName=networks.networks.keySet().iterator().next();
		//NB: directorName must be usefull for vm provisioning ?
		
		com.orange.oss.cloudfoundry.cscpi.domain.Network directorNetwork=networks.networks.values().iterator().next();
		
		String network_name=directorNetwork.cloud_properties.get("name");
		
		
		//find the network with the provided name
		Network network=null;		
		Set<Network> listNetworks = api.getNetworkApi().listNetworks(ListNetworksOptions.Builder.zoneId(csZoneId));
		for (Network n:listNetworks){
			if (n.getName().equals(network_name)){
				network=n;
			}
		}
		Assert.notNull(network,"Could not find network "+network_name);


		Set<NetworkOffering> listNetworkOfferings = api.getOfferingApi().listNetworkOfferings(ListNetworkOfferingsOptions.Builder.zoneId(csZoneId).id(network.getNetworkOfferingId()));		
		NetworkOffering networkOffering=listNetworkOfferings.iterator().next();

		//Requirements, check the provided network, must have a correct prerequisite in offering
		// service offering need dhcp (for dhcp bootstrap, and getting vrouter ip, used for metadata access)
		// 					need metadata service for userData
		//					need dns ?
		
		logger.info("associated Network Offering is {}", networkOffering.getName());
		
        
        NetworkType netType=directorNetwork.type;
        DeployVirtualMachineOptions options=null;
        switch (netType) 
        {
		case vip:
        case dynamic:
        	logger.debug("dynamic ip vm creation");
			options=DeployVirtualMachineOptions.Builder
			.name(vmName)
			.networkId(network.getId())
			.userData(userData.getBytes())
			//.dataDiskSize(dataDiskSize)
			;
			break;
		case manual:
        	logger.debug("static / manual ip vm creation");
        	
        	//check ip is available
        	Assert.isTrue(!this.vmWithIpExists(directorNetwork.ip), "The required IP "+directorNetwork.ip +" is not available");
        	
			options=DeployVirtualMachineOptions.Builder
			.name(vmName)
			.networkId(network.getId())
			.userData(userData.getBytes())
			//.dataDiskSize(dataDiskSize)
			.ipOnDefaultNetwork(directorNetwork.ip)
			;
			break;
			
        }
		
        logger.info("Now launching VM {} creation !",vmName);
        try {
        	AsyncCreateResponse job = api.getVirtualMachineApi().deployVirtualMachineInZone(csZoneId, so.getId(), csTemplateId, options);
        	jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        	jobComplete.apply(job.getJobId());
        } catch (HttpResponseException hjce){
        	int statusCode=hjce.getResponse().getStatusCode();
        	String message=hjce.getResponse().getMessage();
        	logger.error("Error while creating vm. Status code {}, Message : {} ",statusCode,message);
        	throw new VMCreationFailedException("Error while creating vm",hjce);
        }
		
		VirtualMachine vm = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vmName)).iterator().next();
		if (! vm.getState().equals(State.RUNNING)) {
			throw new RuntimeException("Not in expected running state:" + vm.getState());
		}
		
		//list NICS, check macs.
		NIC nic=vm.getNICs().iterator().next();
		logger.info("generated NIC : "+nic.toString());

		
		
		//FIXME: move bosh registry in create_vm (no need of registry for stemcell generation work vms)
		//populate bosh registry
		logger.info("add vm {} to registry", vmName );
		String settings=this.vmSettingGenerator.createsettingForVM(agent_id,vmName,vm,networks);
		this.boshRegistry.put(vmName, settings);
		
		
		logger.info("vm creation completed, now running ! {}");
	}


	@Override
	@Deprecated
	public String current_vm_id() {
		logger.info("current_vm_id");
		//FIXME : deprecated API
		//must keep state in CPI with latest changed / created vm ?? Or find current vm running cpi ? by IP / hostname ?
		// ==> use local vm meta data server to identify.
		// see http://cloudstack-administration.readthedocs.org/en/latest/api.html#user-data-and-meta-data
		return null;
	}

	
	
	/**
	 * 
	 * first try : create a vm from an existing template, stop it, create template from vm, delete work vm
	 * 
	 * @param image_path
	 * @param cloud_properties
	 * @return
	 * @throws CpiErrorException 
	 */
	@Override
	public String create_stemcell(String image_path,
			Map<String, String> cloud_properties) throws CpiErrorException {
		logger.info("create_stemcell");
		
		
		//mock mode enables tests with an existing template (copied as a new template)
		
		if (this.cloudstackConfig.mockCreateStemcell){
			logger.warn("USING MOCK STEMCELL TRANSFORMATION TO CLOUDSTAK TEMPLATE)");
			String stemcellId;
			try {
				stemcellId = mockTemplateGeneration();
			} catch (VMCreationFailedException e) {
				throw new RuntimeException(e);
			}
			return stemcellId;

		}

		//TODO : template name limited to 32 chars, UUID is longer. use Random for now
		Random randomGenerator=new Random();
		String stemcellId="cpitemplate-"+randomGenerator.nextInt(100000);

		logger.info("Starting to upload stemcell to webdav");
		
		Assert.isTrue(image_path!=null,"create_stemcell: Image Path must not be Null");
		File f=new File(image_path);
		
		Assert.isTrue(f.exists(), "create_stemcell: Image Path does not exist :"+image_path);
		Assert.isTrue(f.isFile(), "create_stemcell: Image Path exist but is not a file :"+image_path);
		
		String webDavUrl=null;
		try {
			webDavUrl=this.webdav.pushFile(new FileInputStream(f), stemcellId+".vhd");
			
		} catch (FileNotFoundException e) {
			logger.error("Unable to read file");
			throw new RuntimeException("Unable to read file",e);
		}
		logger.debug("template pushed to webdav, url {}",webDavUrl);

		//FIXME: find correct os type (PVM 64 bits)
		OSType osType=null;
		for (OSType ost:api.getGuestOSApi().listOSTypes()){
			if (ost.getDescription().equals(CPI_OS_TYPE)) osType=ost;
		}
		
		Assert.notNull(osType, "Unable to find OsType");
		
		TemplateMetadata templateMetadata=TemplateMetadata.builder()
				.name(stemcellId)
				.osTypeId(osType.getId())
				.displayText(stemcellId+" : cpi stemcell template")
				.build();
		
		RegisterTemplateOptions options=RegisterTemplateOptions.Builder
				.bits(64)
				.isExtractable(true)				
				//.isPublic(false) //true is KO
				//.isFeatured(false)
				//.domainId(domainId) 
				;
		//TODO: get from cloud properties ie  from stemcell MANIFEST file ?
		String hypervisor="XenServer";
		String format="VHD"; // QCOW2, RAW, and VHD.
		Set<Template> registredTemplates = api.getTemplateApi().registerTemplate(templateMetadata, format, hypervisor, webDavUrl, findZoneId(), options);
		for (Template t: registredTemplates){
			logger.debug("registred template "+t.toString());
		}
		
		this.waitForTemplateReady(stemcellId);
		
		logger.info("done registering cloudstack template for stemcell {}",stemcellId);
		return stemcellId;
	}




	/**
	 * Wait for a registered template to be ready for instanciation (downloaded by cloudstack, and replicated in secondadry storage)
	 * 
	 * @param stemcellId
	 * @throws CpiErrorException 
	 */
	private void waitForTemplateReady(String stemcellId) throws CpiErrorException {
		//FIXME: wait for the template to be ready
		long startTime=System.currentTimeMillis();
		long timeoutTime=startTime+1000*60*5; //5 min wait for template publication
		
		boolean templateReady=false;
		boolean templateError=false;
		boolean timeout=false;
		while ((!timeout)&&(!templateReady) &&(!templateError)){
			timeout=System.currentTimeMillis()> timeoutTime;
			Set<Template> matchingTemplates=api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name(stemcellId));
			Assert.isTrue(matchingTemplates.size()==1,"found multiple templates matching stemcellid "+stemcellId);
			Template t=matchingTemplates.iterator().next();
			Status templateStatus=t.getStatus();
				
			switch (templateStatus) {
				case DOWNLOADED : templateReady=true; break;
				case DOWNLOAD_ERROR: t.getStatus();break;
				case UPLOAD_ERROR: templateError=true;break;
				default: 
					logger.warn("unknown template status {}"+templateStatus);
			}
		}
		
		if (templateError){
			logger.error("Error publishing template {} in cloudstack",stemcellId);
			throw new CpiErrorException("Error publishing template" +stemcellId);
			}
		if (timeout){
			logger.error("Timeout publishing template {} in cloudstack",stemcellId);
			throw new CpiErrorException("Timeout publishing template" +stemcellId);
			}
		
	}

	/**
	 * Mocktemplate generation : use existing template and copy it as another
	 * 
	 * @return
	 */
	private String mockTemplateGeneration() throws VMCreationFailedException {
		//String instance_type="Ultra Tiny";
		String instance_type="CO1 - Small STD";
		
		//FIXME : should parameter the network offering
		String network_name="DefaultIsolatedNetworkOfferingWithSourceNatService";	
		
		
		logger.info("CREATING work vm for template generation");
		//map stemcell to cloudstack template concept.
		String workVmName="cpi-stemcell-work-"+UUID.randomUUID();
		
		
		//FIXME : temporay network config (dynamic)
		Networks fakeDirectorNetworks=new Networks();
		com.orange.oss.cloudfoundry.cscpi.domain.Network net=new com.orange.oss.cloudfoundry.cscpi.domain.Network();
		net.type=NetworkType.dynamic;
		net.cloud_properties.put("name", "3112 - preprod - back");
		net.dns.add("10.234.50.180");
		net.dns.add("10.234.71.124");
		
		fakeDirectorNetworks.networks.put("default",net);
		
		
		this.vmCreation(cloudstackConfig.existingTemplateName, instance_type, fakeDirectorNetworks, workVmName,"fakeagent","fakeuserdata");
		VirtualMachine m=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(workVmName)).iterator().next();
		
		logger.info("STOPPING work vm for template generation");
		String stopJob=api.getVirtualMachineApi().stopVirtualMachine(m.getId());
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(stopJob);
		
		logger.info("Work vm stopped. now creating template from it its ROOT Volume");
		
		
		Volume rootVolume=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.virtualMachineId(m.getId()).type(Type.ROOT)).iterator().next();
		//hopefully, fist volume is ROOT ?

		
		//FIXME : template name limited to 32 chars, UUID is longer. use Random
		Random randomGenerator=new Random();
		String stemcellId="cpitemplate-"+randomGenerator.nextInt(100000);
		
		//find correct os type (PVM 64 bits)
		OSType osType=null;
		for (OSType ost:api.getGuestOSApi().listOSTypes()){
			if (ost.getDescription().equals(CPI_OS_TYPE)) osType=ost;
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
		
		String jobId=api.getVirtualMachineApi().destroyVirtualMachine(m.getId());
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(jobId);
		
		logger.info("work vm cleaned work vm");
		
		return stemcellId;
	}

	


	@Override
	public void delete_stemcell(String stemcell_id) {
		logger.info("delete_stemcell");
		
		//FIXME: assert stemcell_id template exists and is unique
		
		Set<Template> listTemplates = api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name(stemcell_id));
		Assert.isTrue(listTemplates.size()>0,"Could not find any CloudStack Template matching stemcell id "+stemcell_id);
		Assert.isTrue(listTemplates.size()==1,"Found multiple CloudStack templates matching stemcell_id "+stemcell_id);		
		
		String csTemplateId=listTemplates.iterator().next().getId();
		
		String zoneId=findZoneId();
		DeleteTemplateOptions options=DeleteTemplateOptions.Builder.zoneId(zoneId);
		AsyncCreateResponse asyncTemplateDeleteJob =api.getTemplateApi().deleteTemplate(csTemplateId, options);
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(asyncTemplateDeleteJob.getJobId());
		
		logger.info("stemcell {} successfully deleted",stemcell_id);
	}

	@Override
	public void delete_vm(String vm_id) throws CpiErrorException {
		logger.info("delete_vm");
		
		Set<VirtualMachine> vms = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id));
		
		if (vms.size()==0) {
			logger.warn("Vm to delete does not exist {}. OK ...",vm_id);
			return;
		}
		
		Assert.isTrue(vms.size()==1, "delete_vm : Found multiple VMs with name "+vm_id);		
		
		VirtualMachine csVm = vms.iterator().next();
		String csVmId=csVm.getId();
		
		//stop the vm
		String stopJobId=api.getVirtualMachineApi().stopVirtualMachine(csVmId);
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(stopJobId);
		logger.info("vm {} stopped before destroying");

		
		//remove NIC (free the IP before expunge delay
		NIC nic=csVm.getNICs().iterator().next();
		logger.info("NIC to delete : {}",nic.toString());
		//TODO: can use jclouds to remove the nic?

		
		String jobId=api.getVirtualMachineApi().destroyVirtualMachine(csVmId);
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(jobId);
		
		//FIXME : should force expunge VM (bosh usually recreates a vm shortly, expunge is necessary to avoid ip / vols reuse conflicts).
		
		
		//remove  vm_id /settings from bosh registry		
		logger.info("remove vm {} from registry", vm_id );
		this.boshRegistry.delete(vm_id);
		
		
		//delete ephemeral disk !! Unmount then delete.
		Set<Volume> vols=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.type(Type.DATADISK).virtualMachineId(csVmId));

		if (vols.size()==0){
			logger.warn("No ephemeral disk found while deleting vm {}. Ignoring ...",vm_id);
			return;
		} 
		Assert.isTrue(vols.size()==1,"Should have a single data disk mounted (ephemeral disk), found "+vols.size());
		Volume ephemeralVol=vols.iterator().next();
		Assert.isTrue(ephemeralVol.getName().startsWith(CPI_EPHEMERAL_DISK_PREFIX),"mounted disk is not ephemeral disk. Name is "+ephemeralVol.getName());
		
		//detach disk
		AsyncCreateResponse resp=api.getVolumeApi().detachVolume(ephemeralVol.getId());
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(resp.getJobId());
		
		//delete disk
		api.getVolumeApi().deleteVolume(ephemeralVol.getId());
		
		
		//wait expunge delay
		try {
			logger.info("waiting expunge delay for vm {} delete",vm_id);
			Thread.sleep(this.cloudstackConfig.vmExpungeDelaySeconds*1000);
			logger.info("DONE waiting expunge delay for vm {} delete",vm_id);
		} catch (InterruptedException e) {
			throw new CpiErrorException(e.getMessage(),e); 
			};
		
		logger.info("deleted successfully vm {} and ephemeral disk {}",vm_id,ephemeralVol.getName());
	}

	@Override
	public boolean has_vm(String vm_id) {
		logger.info("has_vm ?");
		Set<VirtualMachine> listVirtualMachines = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id));
		Assert.isTrue(listVirtualMachines.size() <=1, "INCONSISTENCY : multiple vms found for vm_id "+vm_id);		
		if (listVirtualMachines.size()==0) return false;
		return true;
		
	}
	
	
	

	@Override
	public boolean has_disk(String disk_id) {
		logger.info("has_disk ?");
		Set<Volume> vols=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK));
		Assert.isTrue(vols.size() <=1, "INCONSISTENCY : multiple data volumes found for disk_id "+disk_id);
		
		if (vols.size()==0) return false;
		
		logger.debug("disk {} found in cloudstack", disk_id);
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

	/**
	 * add metadata to the VM. CPI should not rely on the presence of specific ket
	 */
	@Override
	public void set_vm_metadata(String vm_id, Map<String, String> metadata) {
		logger.info("set vm metadata");
		VirtualMachine vm=api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next();
		
		//set metadatas
		setVmMetada(vm_id, metadata, vm);
		
	}


	/**
	 * 
	 * adapter method to cloudstack User Tag API
	 * @param vm_id cloudstack vm id
	 * @param metadata map of tag name / value
	 * @param vm cloudstack VirtualMachine
	 */
	private void setVmMetada(String vm_id, Map<String, String> metadata,
			VirtualMachine vm) {
		
		//NB: must merge with preexisting user tags. delete previous tag
		
		
		ListTagsOptions listTagOptions=ListTagsOptions.Builder.resourceId(vm.getId()).resourceType(Tag.ResourceType.USER_VM);
		Set<Tag> existingTags=api.getTagApi().listTags(listTagOptions);
		
		if (existingTags.size()>0) {
			//FIXME: merge change existing tags
			logger.warn("VM metadata already set on vm {}.Metadata change not yet implemented by CPI");
			return;
		}
		
		ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();		
		Map<String, String> tags = builder.putAll(metadata).build();
		
		logger.debug(">> adding tags %s to virtualmachine(%s)", tags, vm.getId());
		CreateTagsOptions tagOptions = CreateTagsOptions.Builder.resourceIds(vm.getId()).resourceType(Tag.ResourceType.USER_VM).tags(tags);
		AsyncCreateResponse tagJob = api.getTagApi().createTags(tagOptions);				
		 		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(tagJob.getJobId());
		
		logger.info("done settings metadata on vm ",vm_id);
	}

	/**
	 * Modify the VM network configuration
	 * NB: throws NotSupported error for now. => The director will delete the VM and recreate a new One with the desired target conf
	 * @throws com.orange.oss.cloudfoundry.cscpi.exceptions.NotSupportedException 
	 */
	@Override
	public void configure_networks(String vm_id, Networks networks) throws com.orange.oss.cloudfoundry.cscpi.exceptions.NotSupportedException {
		logger.info("configure network");
		throw new com.orange.oss.cloudfoundry.cscpi.exceptions.NotSupportedException("CPI does not support modifying network yet");
		
	}
	

	@Override
	public String create_disk(Integer size, Map<String, String> cloud_properties) {
		String diskOfferingName=cloud_properties.get("disk_offering");
//		Assert.isTrue(diskOfferingName!=null, "no disk_offering attribute specified for disk creation !");
		if (diskOfferingName==null){
			diskOfferingName=this.cloudstackConfig.defaultDiskOffering;
			logger.info("no disk_offering attribute specified for disk creation. use global CPI default disk offering: {}",diskOfferingName);
		}
		
		String name=CPI_PERSISTENT_DISK_PREFIX+UUID.randomUUID().toString();
		return this.diskCreate(name,size,diskOfferingName);

	}

	/**
	 * @param diskOfferingName
	 * @return
	 */
	private String diskCreate(String name,int size,String diskOfferingName) {

		logger.info("create_disk {} on offering {}",name,diskOfferingName);
		
		//find disk offering
		Set<DiskOffering> listDiskOfferings = api.getOfferingApi().listDiskOfferings(ListDiskOfferingsOptions.Builder.name(diskOfferingName));
		Assert.isTrue(listDiskOfferings.size()>0, "Unknown Service Offering ! : "+diskOfferingName);
		DiskOffering csDiskOffering = listDiskOfferings.iterator().next();
		String diskOfferingId=csDiskOffering.getId();
		
		String zoneId=this.findZoneId();
		
		AsyncCreateResponse resp=null;
		if (csDiskOffering.isCustomized()){
			logger.info("creating disk with specified size (custom size offering)");			
			resp=api.getVolumeApi().createVolumeFromCustomDiskOfferingInZone(name, diskOfferingId, zoneId, size);			
			
		} else {
			logger.info("creating disk -  ignoring specified size {} (fixed by offering : {} )",size,csDiskOffering.getDiskSize());
			resp=api.getVolumeApi().createVolumeFromDiskOfferingInZone(name, diskOfferingId, zoneId);

		}
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(resp.getJobId());
		
		logger.info("disk {} successfully created ",name);
		
		return name;
	}

	@Override
	public void delete_disk(String disk_id) {
		logger.info("delete_disk");
		
		//FIXME; check disk exists
		
		String csDiskId=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK)).iterator().next().getId();
		api.getVolumeApi().deleteVolume(csDiskId);
	}

	@Override
	public void attach_disk(String vm_id, String disk_id) {
		logger.info("attach disk");
		
		this.diskAttachment(vm_id, disk_id);
		
		//now update registry
		String previousSetting=this.boshRegistry.getRaw(vm_id);
		String newSetting=this.vmSettingGenerator.updateVmSettingForAttachDisk(previousSetting, disk_id);
		this.boshRegistry.put(vm_id, newSetting);
		logger.info("==> attach disk updated in bosh registry");
		
	}



	/**
	 * Cloudstack Attachement.
	 * Used for persistent disks and ephemeral disk
	 * @param vm_id
	 * @param disk_id
	 */
	private void diskAttachment(String vm_id, String disk_id) {
		//FIXME; check disk exists
		//FIXME: check vm exists
		Set<Volume> volumes = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK));
		Assert.isTrue(volumes.size()<2,"attach_disk: Fatal, Found multiple volume with name  "+disk_id);
		Assert.isTrue(volumes.size()==1,"attach_disk: Unable to find volume "+disk_id);
		String csDiskId=volumes.iterator().next().getId();
		
		
		Set<VirtualMachine> vms = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id));
		Assert.isTrue(vms.size()==1, "attach_disk: Unable to find vm "+vm_id);
		String csVmId=vms.iterator().next().getId();
		
		//FIXME: with local disk, should check the host and vm host id match ?
		
		
		VolumeApi vol = this.api.getVolumeApi();
		AsyncCreateResponse resp=vol.attachVolume(csDiskId, csVmId);
		//TODO:  need to restart vm ?
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(resp.getJobId());
		
		logger.info("==> attach disk successfull");
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
		String csDiskId=api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK)).iterator().next().getId();		
		AsyncCreateResponse resp=api.getVolumeApi().detachVolume(csDiskId);
		
		jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
		jobComplete.apply(resp.getJobId());
		
		logger.info("==> detach disk successfull");
		
		//now update registry
		String previousSetting=this.boshRegistry.getRaw(vm_id);
		String newSetting=this.vmSettingGenerator.updateVmSettingForDetachDisk(previousSetting, disk_id);
		this.boshRegistry.put(vm_id, newSetting);
		logger.info("==> attach disk updated in bosh registry");
		
		
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
			//only DATA disk  - persistent disk. No ROOT disk,no ephemeral disk ?			
			if ((v.getType()==Type.DATADISK) && (v.getName().startsWith(CPI_PERSISTENT_DISK_PREFIX))){
				String disk_id=v.getName();
				disks.add(disk_id);
			}
		}
		return disks;
	}

  
	/**
	 * utility to retrieve cloudstack zoneId
	 * @return
	 */
	private String findZoneId() {
		//TODO: select the exact zone if multiple available
        ListZonesOptions zoneOptions=ListZonesOptions.Builder.available(true);
		Set<Zone> zones = api.getZoneApi().listZones(zoneOptions);
		Assert.notEmpty(zones, "No Zone available");
		Zone zone=zones.iterator().next();
		String zoneId = zone.getId();
		
		Assert.isTrue(zone.getName().equals(this.cloudstackConfig.default_zone));
		
		return zoneId;
	}

	/**
	 * utility method to check ip conflict
	 * @param ip
	 * @return
	 */
	public boolean vmWithIpExists(String ip) {
		logger.debug("check vm exist with ip {}",ip);
		Set<VirtualMachine> listVirtualMachines = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.zoneId(this.findZoneId()));
		boolean ipExists=false;
		for (VirtualMachine vm:listVirtualMachines){
			String vmIp=vm.getIPAddress();
			if ((vmIp!=null)&& (vmIp.equals(ip))){
				logger.warn("vm {} already uses ip {}",vm.getName(),ip);
				ipExists=true;
			}
		}
		return ipExists;
	}
	
	
    
}
