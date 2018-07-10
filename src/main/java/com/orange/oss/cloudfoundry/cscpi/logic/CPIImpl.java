package com.orange.oss.cloudfoundry.cscpi.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.orange.oss.cloudfoundry.cscpi.config.CloudStackConfiguration;
import com.orange.oss.cloudfoundry.cscpi.domain.NetworkType;
import com.orange.oss.cloudfoundry.cscpi.domain.*;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.VMCreationFailedException;
import com.orange.oss.cloudfoundry.cscpi.webdav.WebdavServerAdapter;
import com.orange.oss.cloudfoundry.cspi.cloudstack.CacheableCloudstackConnector;
import com.orange.oss.cloudfoundry.cspi.cloudstack.NativeCloudstackConnector;
import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.*;
import org.jclouds.cloudstack.domain.Network;
import org.jclouds.cloudstack.domain.Template.Status;
import org.jclouds.cloudstack.domain.VirtualMachine.State;
import org.jclouds.cloudstack.domain.Volume.Type;
import org.jclouds.cloudstack.features.VolumeApi;
import org.jclouds.cloudstack.options.*;
import org.jclouds.cloudstack.predicates.JobComplete;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.util.Predicates2.retry;

/**
 * Implementation of the CPI API, translating to CloudStack jclouds API calls
 *
 * @see: http://www.programcreek.com/java-api-examples/index.php?api=org.jclouds.predicates.RetryablePredicate
 * @see:
 */
public class CPIImpl implements CPI {

    public static final String CPI_VM_PREFIX = "cpivm-";
    public static final String CPI_PERSISTENT_DISK_PREFIX = "cpi-disk-";
    public static final String CPI_EPHEMERAL_DISK_PREFIX = "cpi-ephemeral-disk-";

    //	private static final String CPI_OS_TYPE = "Other PV (64-bit)";
    private static final String HYPERVISOR = "XenServer";
    private static final String TEMPLATE_FORMAT = "VHD"; // QCOW2, RAW, and VHD.

    private static Logger logger = LoggerFactory.getLogger(CPIImpl.class);
    @Autowired
    UserDataGenerator userDataGenerator;
    @Autowired
    VmSettingGenerator vmSettingGenerator;
    @Autowired
    private CloudStackConfiguration cloudstackConfig;
    @Autowired
    private CloudStackApi api;
    @Autowired
    private WebdavServerAdapter webdav;

    @Autowired
    private BoshRegistryClient boshRegistry;

    @Autowired
    private NativeCloudstackConnector nativeCloudstackConnector;

    @Autowired
    private CacheableCloudstackConnector cacheableCloudstackConnector;


    private Predicate<String> jobComplete;

    /**
     * creates a vm.
     * take the stemcell_id as cloudstack template name.
     * create the vm on the correct network configuration
     * manual
     * vip
     * dynamic
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
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public String create_vm(String agent_id,
                            String stemcell_id,
                            ResourcePool resource_pool,
                            Networks networks,
                            List<String> disk_locality,
                            JsonNode env) throws VMCreationFailedException {

        String compute_offering = resource_pool.compute_offering;
        Assert.isTrue(compute_offering != null, "Must provide compute offering in vm ressource pool");

        String affinityGroup = resource_pool.affinity_group;
        if (affinityGroup != null) {
            logger.info("an affinity group {} has been specified for create_vm", affinityGroup);
        }


        //create ephemeral disk, read the disk size from properties, attach it to the vm.
        //NB: if base ROOT disk is large enough, bosh agent can use it to hold swap / ephemeral data. CPI forces an external vol for ephemeral
        //NB: begin with ephemeral disk to avoid vm leaks is disk creation is KO
        String ephemeralDiskServiceOfferingName = resource_pool.ephemeral_disk_offering;
        if (ephemeralDiskServiceOfferingName == null) {
            ephemeralDiskServiceOfferingName = this.cloudstackConfig.defaultEphemeralDiskOffering;
            logger.info("no ephemeral_disk_offering specified in cloud_properties. use global CPI default ephemeral disk offering {}", ephemeralDiskServiceOfferingName);
        }
        logger.debug("ephemeral disk offering is {}", ephemeralDiskServiceOfferingName);

        logger.info("now creating ephemeral disk");
        int ephemeralDiskSize = resource_pool.disk;
        String name = CPI_EPHEMERAL_DISK_PREFIX + UUID.randomUUID().toString();
        String ephemeralDiskName = this.diskCreate(name, ephemeralDiskSize, ephemeralDiskServiceOfferingName);

        String vmName = CPI_VM_PREFIX + UUID.randomUUID().toString();

        // cloudstack userdata generation for bootstrap
        String userData = this.userDataGenerator.userMetadata(vmName, networks);

        try {
            logger.info("now creating cloudstack vm");
            this.vmCreation(stemcell_id, compute_offering, networks, vmName, agent_id, userData, env);
        } catch (Throwable t) {
            logger.error("=> failed creating vm {}. cleaning ephemeral disk {}", vmName, ephemeralDiskName);
            try {
                this.delete_vm(vmName);
                logger.info("=> failed creating vm {}. cleaning vm is OK", vmName);
            } catch (Throwable tvm) {
                logger.error("vm clean after failed {} vm cleanup failed too with error {}", vmName, tvm.getMessage());
            }

            try {
                this.delete_disk(ephemeralDiskName);
                logger.info("=> failed creating vm {}. clean ephemeral disk {} is OK", vmName, ephemeralDiskName);
            } catch (Throwable tdisk) {
                logger.error("ephemeral disk {} clean after failed {} vm creation, disk clean failed too with error {}", ephemeralDiskName, vmName, tdisk.getMessage());
            }
            throw t;
        }

        //NOW attach the ephemeral disk to the vm (hot plug)
        //FIXME : placement constraint local disk offering / vm
        logger.info("now attaching ephemeral disk {} to cloudstack vm {}", ephemeralDiskName, vmName);
        this.diskAttachment(vmName, ephemeralDiskName);

        //FIXME: if attach fails, clean both vm and ephemeral disk ??


        return vmName;
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
     * create a vm from an existing template, stop it, create template from vm, delete work vm
     *
     * @param image_path
     * @param cloud_properties
     * @return
     * @throws CpiErrorException
     */
    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public String create_stemcell(String image_path,
                                  Map<String, Object> cloud_properties) throws CpiErrorException {
        logger.info("create_stemcell");

        //read cloud properties to get template information
        String stemcellName = cloud_properties.get("name").toString(); //,"bosh-cloudstack-xen-ubuntu-trusty-go_agent"
        String stemcellVersion = cloud_properties.get("version").toString();//,"3165"
        String stemcellInfrastructure = cloud_properties.get("infrastructure").toString();//,"cloudstack"
        String stemcellHypervisor = cloud_properties.get("hypervisor").toString();//,"xen"
        Integer stemcellDisk = (Integer) cloud_properties.get("disk");//,"3072"
        String stemcellDiskDFormat = cloud_properties.get("disk_format").toString();//,"raw"
        String stemcellContainerFormat = cloud_properties.get("container_format").toString();//,"bare"
        String stemcellOsType = cloud_properties.get("os_type").toString();//"linux"
        String stemcellOsDistro = cloud_properties.get("os_distro").toString();//"ubuntu"
        String stemcellArchitecture = cloud_properties.get("architecture").toString();//,"x86_64"
        String stemcellAutoDiskConfig = cloud_properties.get("auto_disk_config").toString();//,"true"

        String stemcellLightTemplate = (String) cloud_properties.get("light_template"); //bosh-stemcell-3033-po10.vhd.bz2

        logger.info("stemcell cloud_properties:\n stemcellName {}\n stemcellVersion {}\n stemcellInfrastructure  {}\n stemcellHypervisor  {}\n",
                stemcellName,
                stemcellVersion,
                stemcellInfrastructure,
                stemcellHypervisor);

        //checking property consistency
        Assert.isTrue(stemcellInfrastructure.equals("cloudstack"), "infrastructure " + stemcellInfrastructure + " is not supported by CPI");
        Assert.isTrue(stemcellArchitecture.equals("x86_64"), "architecture " + stemcellArchitecture + " is not supported by CPI");
        Assert.isTrue(stemcellHypervisor.equals("xen"), "hypervisor " + stemcellHypervisor + " is not supported by CPI");
        Assert.isTrue(stemcellOsType.equals("linux"), "OS type " + stemcellOsType + " is not supported by CPI");

        //detect light stemcell
        if (stemcellLightTemplate != null) {
            logger.info("a light_template attribute in cloud_properties. Infering Light Template from  {}", stemcellLightTemplate);
            logger.warn("USING LIGHT STEMCELL REFERENCE TO CREATE CLOUDSTAK TEMPLATE)");
            String stemcellId;
            try {
                stemcellId = mockTemplateGeneration(stemcellLightTemplate);
            } catch (VMCreationFailedException e) {
                throw new RuntimeException(e);
            }
            logger.info("done creating template {} from light_template attribute in cloud_properties  {}", stemcellId, stemcellLightTemplate);
            return stemcellId;
        }

        //TODO : template name limited to 32 chars, UUID is longer. use Random for now
        Random randomGenerator = new Random();
        String stemcellId = "cpitemplate-" + randomGenerator.nextInt(100000);

        logger.info("Starting to upload stemcell to webdav");

        Assert.isTrue(image_path != null, "create_stemcell: Image Path must not be Null");
        File f = new File(image_path);

        Assert.isTrue(f.exists(), "create_stemcell: Image Path does not exist :" + image_path);
        Assert.isTrue(f.isFile(), "create_stemcell: Image Path exist but is not a file :" + image_path);

        String webDavUrl = null;
        try {
            webDavUrl = this.webdav.pushFile(new FileInputStream(f), stemcellId + ".vhd.bz2");

        } catch (FileNotFoundException e) {
            logger.error("Unable to read file");
            throw new RuntimeException("Unable to read file", e);
        }
        logger.debug("template pushed to webdav, url {}", webDavUrl);

        OSType osType = this.cacheableCloudstackConnector.findOsType(cloudstackConfig.stemcell_os_type);

        TemplateMetadata templateMetadata = TemplateMetadata.builder()
                .name(stemcellId)
                .osTypeId(osType.getId())
                .displayText(stemcellId + " : cpi stemcell template") //TODO add the stemcell properties from MANIFEST
                .build();

        RegisterTemplateOptions options = RegisterTemplateOptions.Builder
                .bits(64)
                .isExtractable(true)
                .requiresHVM(cloudstackConfig.stemcell_requires_hvm);
        //.isPublic(false) //true is KO
        //.isFeatured(false)
        //.domainId(domainId)
        ;
        //TODO: get from cloud properties ie  from stemcell MANIFEST file ?
        Set<Template> registredTemplates = api.getTemplateApi().registerTemplate(templateMetadata, TEMPLATE_FORMAT, HYPERVISOR, webDavUrl, this.cacheableCloudstackConnector.findZoneId(), options);
        for (Template t : registredTemplates) {
            logger.debug("registred template " + t.toString());
        }

        this.waitForTemplateReady(stemcellId);

        logger.info("done registering cloudstack template for stemcell {}", stemcellId);

        //FIXME: purge template in webdav

        return stemcellId;
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void delete_stemcell(String stemcell_id) {
        logger.info("delete_stemcell {}", stemcell_id);

        //FIXME: assert stemcell_id template exists and is unique

        Set<Template> listTemplates = api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name(stemcell_id));
        Assert.isTrue(listTemplates.size() > 0, "Could not find any CloudStack Template matching stemcell id " + stemcell_id);
        Assert.isTrue(listTemplates.size() == 1, "Found multiple CloudStack templates matching stemcell_id " + stemcell_id);

        String csTemplateId = listTemplates.iterator().next().getId();

        String zoneId = this.cacheableCloudstackConnector.findZoneId();
        DeleteTemplateOptions options = DeleteTemplateOptions.Builder.zoneId(zoneId);
        AsyncCreateResponse asyncTemplateDeleteJob = api.getTemplateApi().deleteTemplate(csTemplateId, options);
        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(asyncTemplateDeleteJob.getJobId());

        //clean the webdav
        this.webdav.delete(stemcell_id + ".vhd.bz2"); //stemcell builder uses bzip2 to compress the vhd template

        logger.info("stemcell {} successfully deleted", stemcell_id);
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void delete_vm(String vm_id) throws CpiErrorException {
        logger.info("delete_vm");

        Set<VirtualMachine> vms = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id));

        if (vms.size() == 0) {
            logger.warn("Vm to delete does not exist {}. OK ...", vm_id);
            return;
        }

        Assert.isTrue(vms.size() == 1, "delete_vm : Found multiple VMs with name " + vm_id);

        VirtualMachine csVm = vms.iterator().next();
        String csVmId = csVm.getId();

        //disable any static nat on the vm (vm with vip ip)
        ListPublicIPAddressesOptions listPublicIpOptions = ListPublicIPAddressesOptions.Builder.allocatedOnly(true);
        for (PublicIPAddress publicIp : api.getAddressApi().listPublicIPAddresses(listPublicIpOptions)) {
            logger.debug("examining public ip {}", publicIp);
            if ((publicIp.getVirtualMachineId() != null) && (publicIp.getVirtualMachineId().equals(csVmId))) {
                logger.info("Found public ip adress {} associated to vm {}. => disabling static nat", publicIp.getIPAddress(), vm_id);
                //removing forwarding rules
                Set<IPForwardingRule> rules = api.getNATApi().listIPForwardingRules(ListIPForwardingRulesOptions.Builder.virtualMachineId(csVmId));
                for (IPForwardingRule rule : rules) {
                    logger.debug("removing forwarding rule {}", rule);
                    api.getNATApi().deleteIPForwardingRule(rule.getId());
                }
                api.getNATApi().disableStaticNATOnPublicIP(publicIp.getId());
                logger.info("Done disabling public ip adress {} static NAT to vm {}", publicIp.getIPAddress(), vm_id);
            }
        }

        //stop the vm
        String stopJobId = api.getVirtualMachineApi().stopVirtualMachine(csVmId);
        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(stopJobId);
        logger.info("vm {} stopped before destroying");

        //delete ephemeral disk !! Unmount then delete.
        Set<Volume> vols = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.type(Type.DATADISK).virtualMachineId(csVmId));

        if (vols.size() == 0) {
            logger.warn("No ephemeral disk found while deleting vm {}. Ignoring ...", vm_id);
            return;
        }

        if (vols.size() > 1) {
            logger.warn("Should only have a single data disk mounted (ephemeral disk) when deleting, found " + vols.size());
        }

        //iterate to identify the ephemeral disk
        for (Volume vol : vols) {
            if (!vol.getName().startsWith(CPI_EPHEMERAL_DISK_PREFIX)) {
                logger.warn("mounted disk is not ephemeral disk, ignoring. Name is " + vol.getName());
            } else {
                logger.warn("unmount and delete ephemeral disk " + vol.getName());
                //detach disk
                AsyncCreateResponse resp = api.getVolumeApi().detachVolume(vol.getId());

                jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
                jobComplete.apply(resp.getJobId());

                //delete disk
                api.getVolumeApi().deleteVolume(vol.getId());
            }
        }

        //destroy vm
        String jobId = api.getVirtualMachineApi().destroyVirtualMachine(csVmId);

        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(jobId);

        //if forceExpunge is set explicitly call cloudstack API to expunge (requires admin role)
        if (this.cloudstackConfig.forceVmExpunge) {
            logger.info("Force Expunge of vm {}", vm_id);
            Map<String, String> params = new HashMap<String, String>();
            params.put("id", csVmId);
            this.nativeCloudstackConnector.nativeCall("expungeVirtualMachine", params);
            logger.info("done Expunging of vm {}", vm_id);
        }

        //wait expunge delay
        try {
            logger.info("waiting expunge delay for vm {} delete", vm_id);
            Thread.sleep(this.cloudstackConfig.vmExpungeDelaySeconds * 1000);
            logger.info("DONE waiting expunge delay for vm {} delete", vm_id);
        } catch (InterruptedException e) {
            throw new CpiErrorException(e.getMessage(), e);
        }
        ;


        //remove  vm_id /settings from bosh registry. last step to avoid losing registry if delete vm fails
        logger.info("remove vm {} from registry", vm_id);
        this.boshRegistry.delete(vm_id);

        logger.info("deleted successfully vm {} and ephemeral disk ", vm_id);
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public boolean has_vm(String vm_id) {
        logger.info("has_vm ?");
        Set<VirtualMachine> listVirtualMachines = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id));
        Assert.isTrue(listVirtualMachines.size() <= 1, "INCONSISTENCY : multiple vms found for vm_id " + vm_id);
        if (listVirtualMachines.size() == 0) return false;
        return true;
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public boolean has_disk(String disk_id) {
        logger.info("has_disk ?");
        Set<Volume> vols = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK));
        Assert.isTrue(vols.size() <= 1, "INCONSISTENCY : multiple data volumes found for disk_id " + disk_id);

        if (vols.size() == 0) return false;

        logger.debug("disk {} found in cloudstack", disk_id);
        return true;
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void reboot_vm(String vm_id) {
        logger.info("reboot_vm");

        VirtualMachine vm = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next();
        String rebootJob = api.getVirtualMachineApi().rebootVirtualMachine(vm.getId());

        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(rebootJob);

        logger.info("done rebooting vm {}", vm_id);
    }

    /**
     * add metadata to the VM. CPI should not rely on the presence of specific ket
     */
    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void set_vm_metadata(String vm_id, Map<String, String> metadata) {
        logger.info("set vm metadata");
        VirtualMachine vm = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next();
        //set metadatas
        setMetadata(vm.getId(), metadata, Tag.ResourceType.USER_VM);
    }

    /**
     * add metadata to a disk. CPI should not rely on the presence of specific ket
     */
    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void set_disk_metadata(String disk_id, Map<String, String> metadata) {
        logger.info("set disk metadata");
        Volume volume = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK)).iterator().next();
        //set metadatas
        setMetadata(volume.getId(), metadata, Tag.ResourceType.VOLUME);
    }

    /**
     * Modify the VM network configuration
     * NB: throws NotSupported error for now. => The director will delete the VM and recreate a new One with the desired target conf
     *
     * @throws com.orange.oss.cloudfoundry.cscpi.exceptions.NotSupportedException
     */
    @Override
    public void configure_networks(String vm_id, Networks networks) throws com.orange.oss.cloudfoundry.cscpi.exceptions.NotSupportedException {
        logger.info("configure network");
        throw new com.orange.oss.cloudfoundry.cscpi.exceptions.NotSupportedException("CPI does not support modifying network yet");

    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public String create_disk(Integer size, Map<String, String> cloud_properties) {
        String diskOfferingName = cloud_properties.get("disk_offering");
        if (diskOfferingName == null) {
            diskOfferingName = this.cloudstackConfig.defaultDiskOffering;
            logger.info("no disk_offering attribute specified for disk creation. use  CPI global default disk offering: {}", diskOfferingName);
        }

        String name = CPI_PERSISTENT_DISK_PREFIX + UUID.randomUUID().toString();
        return this.diskCreate(name, size, diskOfferingName);

    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void delete_disk(String disk_id) {
        logger.info("delete_disk");


        //FIXME: check disk is detached

        Set<Volume> listVolumes = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK));
        if (listVolumes.size() == 0) {
            logger.warn("delete_disk {}. No disk exist with this name");
            return;
        }
        Assert.isTrue(listVolumes.size() == 1, "WARNING: found multiple disk with name " + disk_id);
        String csDiskId = listVolumes.iterator().next().getId();
        api.getVolumeApi().deleteVolume(csDiskId);
        logger.debug("disk {} has been deleted", disk_id);
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void attach_disk(String vm_id, String disk_id) {
        logger.info("attach disk");

        this.diskAttachment(vm_id, disk_id);

        //now update registry
        this.updatePersistentDisksInRegistry(vm_id);
        logger.info("==> attach disk updated in bosh registry");

    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public String snapshot_disk(String disk_id, Map<String, String> metadata) {
        logger.info("snapshot disk");
        //TODO: only for persistent disk
        String csDiskId = api.getVolumeApi().getVolume(disk_id).getId();
        AsyncCreateResponse async = api.getSnapshotApi().createSnapshot(csDiskId, CreateSnapshotOptions.Builder.domainId("domain"));

        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(async.getJobId());

        //FIXME
        return null;
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void delete_snapshot(String snapshot_id) {
        logger.info("delete snapshot");
        //TODO
    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public void detach_disk(String vm_id, String disk_id) {
        logger.info("detach disk");
        Volume csDisk = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK)).iterator().next();
        String csDiskId = csDisk.getId();

        //Dont fail if disk is already detached
        if (csDisk.getVirtualMachineId() == null) {
            logger.warn("CPI requests Detach volume {}, but disk not attached. Ignoring ...");
            //FIXME: should update registry anyway ?
            return;
        }

        AsyncCreateResponse resp = api.getVolumeApi().detachVolume(csDiskId);

        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(resp.getJobId());

        logger.info("==> detach disk successfull");

        this.updatePersistentDisksInRegistry(vm_id);

        logger.info("==> detach disk updated in bosh registry");

    }

    @Override
    @HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3600000"))
    public List<String> get_disks(String vm_id) {
        logger.info("get_disks");

        ArrayList<String> diskList = new ArrayList<String>();

        Map<String, PersistentDisk> disks = this.getPersistentDisks(vm_id);
        diskList.addAll(disks.keySet());
        return diskList;
    }

    @Override
    public List<String> stemcell_formats() {
        return new ArrayList<String>(Arrays.asList(new String[]{
                "cloudstack-vhdx",
        }));
    }

    @Override
    public ResourcePool calculate_vm_cloud_properties(VMParams vmParams) {
        ResourcePool resourcePool = new ResourcePool();
        resourcePool.compute_offering = findComputeOffering(vmParams);
        resourcePool.ephemeral_disk_offering = findEphemeralDiskOffering(vmParams);

        return resourcePool;
    }

    private String findEphemeralDiskOffering(VMParams vmParams) {
        Integer diskSize = vmParams.ephemeralDiskSize / 1024;
        List<DiskOffering> diskOfferings = api.getOfferingApi().listDiskOfferings().stream().collect(Collectors.toList());

        if (this.cloudstackConfig.calculateDiskTags.size() > 0) {
            diskOfferings = diskOfferings.stream()
                    .filter(o -> o.getTags().stream().filter(t -> this.cloudstackConfig.calculateDiskTags.contains(t)).toArray().length > 0)
                    .collect(Collectors.toList());
        }

        diskOfferings = diskOfferings.stream()
                .sorted(Comparator.comparingInt(DiskOffering::getDiskSize))
                .filter(o -> o.getDiskSize() >= diskSize)
                .collect(Collectors.toList());
        if (diskOfferings.size() == 0) {
            return api.getOfferingApi().listDiskOfferings().stream()
                    .sorted(Comparator.comparingInt(DiskOffering::getDiskSize)).findFirst().get().getName();
        }
        return diskOfferings.get(0).getName();
    }

    private String findComputeOffering(VMParams vmParams) {
        List<ServiceOffering> offers = api.getOfferingApi().listServiceOfferings().stream().collect(Collectors.toList());
        if (this.cloudstackConfig.calculateComputeTags.size() > 0) {
            offers = offers.stream()
                    .filter(o -> o.getTags().stream().filter(t -> this.cloudstackConfig.calculateComputeTags.contains(t)).toArray().length > 0)
                    .collect(Collectors.toList());
        }

        List<ServiceOffering> ramOffers = offers.stream().sorted(Comparator.comparingInt(ServiceOffering::getMemory))
                .filter(o -> o.getMemory() >= vmParams.ram).collect(Collectors.toList());

        if (ramOffers.size() == 0) {
            return offers.stream().sorted(Comparator.comparingInt(ServiceOffering::getMemory).reversed()).findFirst().get().getName();
        }

        List<ServiceOffering> finalOffers = ramOffers.stream().sorted(Comparator.comparingInt(ServiceOffering::getCpuNumber))
                .filter(o -> o.getCpuNumber() >= vmParams.cpu).collect(Collectors.toList());

        if (finalOffers.size() == 0) {
            return ramOffers.stream().sorted(Comparator.comparingInt(ServiceOffering::getCpuNumber).reversed()).findFirst().get().getName();
        }
        return finalOffers.get(0).getName();
    }

    /**
     * Cloudstack vm creation.
     *
     * @param stemcell_id
     * @param compute_offering
     * @param networks
     * @param vmName
     * @throws VMCreationFailedException
     */
    private void vmCreation(String stemcell_id, String compute_offering,
                            Networks networks, String vmName, String agent_id, String userData, JsonNode env) throws VMCreationFailedException {

        Template stemCellTemplate = this.cacheableCloudstackConnector.findStemcell(stemcell_id);
        String csTemplateId = stemCellTemplate.getId();
        logger.info("found cloudstack template {} matching name / stemcell_id {}", csTemplateId, stemcell_id);

        String csZoneId = this.cacheableCloudstackConnector.findZoneId();

        ServiceOffering so = this.cacheableCloudstackConnector.findComputeOffering(compute_offering);

        int manualAddress = 0;
        int dynamicAddress = 0;
        int vipAddress = 0;

        String manualIp = null;
        String vip = null;
        String network_name = null;

        //scan networks
        for (com.orange.oss.cloudfoundry.cscpi.domain.Network net : networks.networks.values()) {
            switch (net.type) {
                case NetworkType.manual:
                    manualAddress++;
                    network_name = net.cloud_properties.get("name");
                    manualIp = net.ip;
                    break;

                case NetworkType.dynamic:
                    dynamicAddress++;
                    network_name = net.cloud_properties.get("name");
                    break;
                case NetworkType.vip:
                    vipAddress++;
                    vip = net.ip;
                    break;

            }

        }

        Assert.isTrue(network_name != null, "no network name provided");

        Assert.isTrue(vipAddress < 2, "CPI supports at most 1 vip per vm");
        Assert.isTrue(dynamicAddress + manualAddress < 2, "CPI currently supports at most 1 NIC per vm");
        Assert.isTrue(dynamicAddress + manualAddress > 0, "CPI must define at list a dynamic or manual IP per vm");

        Assert.isTrue((vipAddress > 0 && vip != null) || (vipAddress == 0 && vip == null), "no public IP provided for vip network");
        Assert.isTrue((manualAddress > 0 && manualIp != null) || (manualAddress == 0 && manualIp == null), "no IP provided for manual/static network");


        Network network = this.cacheableCloudstackConnector.findNetwork(csZoneId, network_name);
        NetworkOffering networkOffering = this.cacheableCloudstackConnector.findNetworkOffering(csZoneId, network);

        logger.info("associated Network Offering is {}", networkOffering.getName());

        DeployVirtualMachineOptions options = null;

        if (manualAddress == 1) {
            logger.debug("static / manual ip vm creation. bosh director has chosen a specific IP");

            //check ip is available
            String vmUsingIp = this.vmWithIpExists(manualIp, network);
            Assert.isTrue(vmUsingIp == null, "The required IP " + manualIp + " is not available: used by vm " + vmUsingIp);

            options = DeployVirtualMachineOptions.Builder
                    .name(vmName)
                    .networkId(network.getId())
                    .userData(userData.getBytes())
                    .keyPair(cloudstackConfig.default_key_name)
                    //.dataDiskSize(dataDiskSize)
                    .ipOnDefaultNetwork(manualIp)
            ;
        } else //dynamic ip
        {
            logger.debug("dynamic ip vm creation. Let Cloudstack choose an IP");
            options = DeployVirtualMachineOptions.Builder
                    .name(vmName)
                    .networkId(network.getId())
                    .userData(userData.getBytes())
                    .keyPair(cloudstackConfig.default_key_name)
            //.dataDiskSize(dataDiskSize)
            ;
        }

        logger.info("Now launching VM {} creation !", vmName);
        try {
            AsyncCreateResponse job = api.getVirtualMachineApi().deployVirtualMachineInZone(csZoneId, so.getId(), csTemplateId, options);
            jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
            jobComplete.apply(job.getJobId());
        } catch (HttpResponseException hjce) {
            logger.info("Exception: {}", hjce.toString());

            HttpResponse response = hjce.getResponse();
            if (response != null) {
                int statusCode = response.getStatusCode();
                String message = response.getMessage();
                logger.error("Error while creating vm. Status code {}, Message : {} ", statusCode, message);
            }
            throw new VMCreationFailedException("Error while creating vm", hjce);
        }

        VirtualMachine vm = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vmName)).iterator().next();
        if (!vm.getState().equals(State.RUNNING)) {
            throw new RuntimeException("Not in expected running state:" + vm.getState());
        }

        //list NICS, check macs.
        NIC nic = vm.getNICs().iterator().next();
        logger.info("generated NIC : " + nic.toString());

        //FIXME: move bosh registry in create_vm (no need of registry for stemcell generation work vms)
        //populate bosh registry
        logger.info("add vm {} to registry", vmName);
        String settings = this.vmSettingGenerator.createsettingForVM(agent_id, vmName, vm, networks, env);
        this.boshRegistry.put(vmName, settings);

        //waiting create_delay
        try {
            logger.info("==> vm ok on cloudstack. Waiting create_delay for vm {} for {} seconds", vmName, this.cloudstackConfig.vmCreateDelaySeconds);
            Assert.isTrue(this.cloudstackConfig.vmCreateDelaySeconds < 1000 * 60 * 5, "create_delay configuration is excessive (more the 5 min wait time after cloustack vm creation");
            Thread.sleep(this.cloudstackConfig.vmCreateDelaySeconds * 1000);
            logger.info("DONE waiting create delay for vm {}", vmName);
        } catch (InterruptedException e) {
        }
        ;

        logger.info("vm creation completed, now running ! {}", vmName);

        /**
         * see this for cloudstack ip association
         * https://shankerbalan.net/blog/assign-static-nat-ip-address-using-cloudstack-api/
         */
        if (vipAddress == 1) {
            logger.warn("vip ip net yet fully implemented ");
            logger.info("adding vip {} to vm {}", vip, vmName);

            ListPublicIPAddressesOptions listPublicIpOptions = ListPublicIPAddressesOptions.Builder.IPAddress(vip);
            //check vip exists
            Assert.isTrue(api.getAddressApi().listPublicIPAddresses(listPublicIpOptions).size() == 1, "The required vip " + vip + "does not exist in cloudstack");
            String IPAddressId = api.getAddressApi().listPublicIPAddresses(listPublicIpOptions).iterator().next().getId(); //TODO: find public ip id ?.

            //check vip is avail? default is force


            logger.info("enable static nat for vip {} to vm {}", vip, vmName);
            //https://cloudstack.apache.org/api/apidocs-4.9/apis/enableStaticNat.html
            // on VPC, need to specify the networkid. not yet supported on jclouds
            //api.getNATApi().enableStaticNATForVirtualMachine(vm.getId(), IPAddressId);

            Map<String, String> apiParameters = new Hashtable<String, String>();
            apiParameters.put("virtualmachineid", vm.getId());
            apiParameters.put("ipaddressid", IPAddressId);
            apiParameters.put("networkid", network.getId());
            this.nativeCloudstackConnector.nativeCall("enableStaticNat", apiParameters);

            //FIXME: improve native connector exception handling for clean error message to bosh director

            //create nat forwarding rules. only possible if firewalling  feature is enabled on VPC
            //FIXME: add a property to activate firewalling (or inspect network offering firewall availability)

//			logger.info("adding nat forwarding rules for vip {} to vm {}",vip,vmName);
//			int startPort=1;
//			CreateIPForwardingRuleOptions natOptions=CreateIPForwardingRuleOptions.Builder.endPort(65535);//check
//
//			AsyncCreateResponse resp;
//			resp=api.getNATApi().createIPForwardingRule(IPAddressId, "TCP", startPort, natOptions);
//			jobComplete.apply(resp.getJobId());
//
//			resp=api.getNATApi().createIPForwardingRule(IPAddressId, "UDP", startPort, natOptions);
//			jobComplete.apply(resp.getJobId());

        }


    }

    /**
     * Wait for a registered template to be ready for instanciation (downloaded by cloudstack, and replicated in secondadry storage)
     *
     * @param stemcellId
     * @throws CpiErrorException
     */
    private void waitForTemplateReady(String stemcellId) throws CpiErrorException {
        //FIXME: wait for the template to be ready
        long startTime = System.currentTimeMillis();
        long timeoutTime = startTime + 1000 * this.cloudstackConfig.publishTemplateTimeoutMinutes * 60;

        boolean templateReady = false;
        boolean templateError = false;
        boolean timeout = false;
        while ((!timeout) && (!templateReady) && (!templateError)) {

            logger.info("polling template registration status for stemcell: {}", stemcellId);
            timeout = System.currentTimeMillis() > timeoutTime;
            Set<Template> matchingTemplates = api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name(stemcellId));
            Assert.isTrue(matchingTemplates.size() == 1, "found multiple templates matching stemcellid " + stemcellId);
            Template t = matchingTemplates.iterator().next();

            Status templateStatus = t.getStatus();
            logger.info("template status: {}", templateStatus);

            if (templateStatus != null) {
                switch (templateStatus) {
                    case DOWNLOADED:
                        templateReady = true;
                        break;
                    case DOWNLOAD_ERROR:
                        t.getStatus();
                        break;
                    case UPLOAD_ERROR:
                        templateError = true;
                        break;
                    case DOWNLOAD_IN_PROGRESS:
                        logger.info("DOWNLOAD_IN_PROGRESS: Cloudstack is downloading template from cpi");
                    default:
                        logger.warn("unknown template status {}" + templateStatus);
                }
            }

            //tempo before next polling
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

        }

        if (templateError) {
            logger.error("Error publishing template {} in cloudstack", stemcellId);
            throw new CpiErrorException("Error publishing template" + stemcellId);
        }
        if (timeout) {
            logger.error("Timeout publishing template {} in cloudstack", stemcellId);
            throw new CpiErrorException("Timeout publishing template" + stemcellId);
        }
    }

    /**
     * Mocktemplate generation : use existing template and copy it as another
     *
     * @return
     */
    private String mockTemplateGeneration(String existingTemplateName) throws VMCreationFailedException {

        //FIXME : should parameter the network offering
//		String network_name="DefaultIsolatedNetworkOfferingWithSourceNatService";


        //map stemcell to cloudstack template concept.
        String workVmName = "cpi-stemcell-work-" + UUID.randomUUID();
        logger.info("CREATING work vm {} for template generation", workVmName);

        //FIXME : temporay network config (dynamic)
        Networks fakeDirectorNetworks = new Networks();
        com.orange.oss.cloudfoundry.cscpi.domain.Network net = new com.orange.oss.cloudfoundry.cscpi.domain.Network();
        net.type = NetworkType.dynamic;
        net.cloud_properties.put("name", this.cloudstackConfig.lightStemcellNetworkName);
//		net.dns.add("10.234.50.180");
//		net.dns.add("10.234.71.124");

        fakeDirectorNetworks.networks.put("default", net);

        this.vmCreation(existingTemplateName, cloudstackConfig.light_stemcell_instance_type, fakeDirectorNetworks, workVmName, "fakeagent", "fakeuserdata", null);
        VirtualMachine m = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(workVmName)).iterator().next();

        logger.info("STOPPING work vm for template generation");
        String stopJob = api.getVirtualMachineApi().stopVirtualMachine(m.getId());
        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(stopJob);

        logger.info("Work vm stopped {}. now creating template from it its ROOT Volume", workVmName);


        Volume rootVolume = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.virtualMachineId(m.getId()).type(Type.ROOT)).iterator().next();

        //FIXME : template name limited to 32 chars, UUID is longer. use Random
        Random randomGenerator = new Random();
        String stemcellId = "cpitemplate-" + randomGenerator.nextInt(100000);

        //find correct os type (PVM 64 bits)
        OSType osType = null;
        for (OSType ost : api.getGuestOSApi().listOSTypes()) {
            if (ost.getDescription().equals(cloudstackConfig.stemcell_os_type)) osType = ost;
        }

        Assert.notNull(osType, "Unable to find OsType");

        TemplateMetadata templateMetadata = TemplateMetadata.builder()
                .name(stemcellId)
                .osTypeId(osType.getId())
                .volumeId(rootVolume.getId())
                .displayText("generated cpi stemcell template")
                .build();

        CreateTemplateOptions options = CreateTemplateOptions.Builder
                //.isPublic(true). public = true creates cross-zones templates?
                .isFeatured(true);

        AsyncCreateResponse asyncTemplateCreateJob = api.getTemplateApi().createTemplate(templateMetadata, options);
        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(asyncTemplateCreateJob.getJobId());

        logger.info("Template successfully created ! {} - {}", stemcellId);

        logger.info("now cleaning work vm {}", workVmName);

        String jobId = api.getVirtualMachineApi().destroyVirtualMachine(m.getId());
        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(jobId);

        logger.info("work vm cleaned work vm");

        return stemcellId;
    }

    /**
     * adapter method to cloudstack User Tag API
     *
     * @param resource_id  cloudstack resource id
     * @param metadata     map of tag name / value
     * @param resourceType cloudstack resource type to tag
     */
    private void setMetadata(String resource_id, Map<String, String> metadata,
                             Tag.ResourceType resourceType) {

        //NB: must merge with preexisting user tags. delete previous tag


        ListTagsOptions listTagOptions = ListTagsOptions.Builder.resourceId(resource_id).resourceType(resourceType);
        Set<Tag> existingTags = api.getTagApi().listTags(listTagOptions);

        if (existingTags.size() > 0) {
            //FIXME: merge change existing tags
            logger.warn("%s metadata already set on %s {}.Metadata change not yet implemented by CPI", resourceType.toString(), resourceType.toString());
            return;
        }

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        Map<String, String> tags = builder.putAll(metadata).build();

        logger.debug(">> adding tags %s to %s(%s)", tags, resourceType.toString(), resource_id);
        CreateTagsOptions tagOptions = CreateTagsOptions.Builder.resourceIds(resource_id).resourceType(resourceType).tags(tags);
        AsyncCreateResponse tagJob = api.getTagApi().createTags(tagOptions);

        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(tagJob.getJobId());

        logger.info("done settings metadata on %s/%s ", resourceType.toString(), resource_id);
    }

    /**
     * @param name             disk name
     * @param sizeMo           size
     * @param diskOfferingName disk offering name
     * @return
     */
    private String diskCreate(String name, int sizeMo, String diskOfferingName) {

        logger.info("create_disk {} on offering {}, size {} Mo", name, diskOfferingName, sizeMo);

        //find disk offering
        Set<DiskOffering> listDiskOfferings = api.getOfferingApi().listDiskOfferings(ListDiskOfferingsOptions.Builder.name(diskOfferingName));
        Assert.isTrue(listDiskOfferings.size() > 0, "Unknown Service Offering ! : " + diskOfferingName);
        DiskOffering csDiskOffering = listDiskOfferings.iterator().next();
        String diskOfferingId = csDiskOffering.getId();

        String zoneId = this.cacheableCloudstackConnector.findZoneId();

        AsyncCreateResponse resp = null;
        if (csDiskOffering.isCustomized()) {
            Assert.isTrue(sizeMo > 0, "Must specify a disk size for custom disk offering " + diskOfferingName);

            int sizeGo = (int) Math.ceil(sizeMo / 1024f);
            logger.info("creating disk with specified size (custom size offering): {} Mo => {} Go", sizeMo, sizeGo);
            resp = api.getVolumeApi().createVolumeFromCustomDiskOfferingInZone(name, diskOfferingId, zoneId, sizeGo);
        } else {
            Assert.isTrue(sizeMo <= csDiskOffering.getDiskSize() * 1024, "specified persistent disk size " + sizeMo + " too big for offering " + diskOfferingName + "=> " + csDiskOffering.getDiskSize() + "Go");
            logger.info("creating disk - ignoring specified size {} Mo for cloudstack volume creation (fixed by offering {}: {} Go )", sizeMo, diskOfferingName, csDiskOffering.getDiskSize());
            resp = api.getVolumeApi().createVolumeFromDiskOfferingInZone(name, diskOfferingId, zoneId);
        }

        jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
        jobComplete.apply(resp.getJobId());

        logger.info("disk {} successfully created ", name);

        return name;
    }

    /**
     * Cloudstack Attachement.
     * Used for persistent disks and ephemeral disk
     *
     * @param vm_id
     * @param disk_id
     */
    private void diskAttachment(String vm_id, String disk_id) {
        Set<Volume> volumes = api.getVolumeApi().listVolumes(ListVolumesOptions.Builder.name(disk_id).type(Type.DATADISK));
        Assert.isTrue(volumes.size() < 2, "attach_disk: Fatal, Found multiple volume with name  " + disk_id);
        Assert.isTrue(volumes.size() == 1, "attach_disk: Unable to find volume " + disk_id);

        Volume csDisk = volumes.iterator().next();
        Assert.isTrue(csDisk.getVmName() == null, "attach_disk: volume " + disk_id + " is already attached to vm " + csDisk.getVmName() + " . cant attach to vm" + vm_id);

        String csDiskId = csDisk.getId();

        Set<VirtualMachine> vms = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id));
        Assert.isTrue(vms.size() == 1, "attach_disk: Unable to find vm " + vm_id);
        String csVmId = vms.iterator().next().getId();

        //FIXME: with local disk, should check the host and vm host id match ?

        IllegalArgumentException lastException = null;

        int attachRetryCount = 100;
        for (int i = 0; i < attachRetryCount; i++) {
            try {
                VolumeApi vol = this.api.getVolumeApi();
                AsyncCreateResponse resp = vol.attachVolume(csDiskId, csVmId);
                // TODO: need to restart vm ?
                jobComplete = retry(new JobComplete(api), 1200, 3, 5, SECONDS);
                jobComplete.apply(resp.getJobId());
                logger.info("==> attach disk successfull");
                return;
            } catch (Throwable e) {
                lastException = new IllegalArgumentException("Unable to attach disk." + e.getMessage());
                logger.warn("Attach failed. " + e.getMessage() + "\nretrying");
                try {
                    Thread.sleep(10000); //10s tempo
                } catch (InterruptedException e1) {
                }

            }
        }
        throw lastException;

    }

    /**
     * get the persistent disk list from iaas. catch the mounted disk deviceid
     * Possible values for a Linux OS are:* 0 - /dev/xvda* 1 - /dev/xvdb* 2 - /dev/xvdc* 4 - /dev/xvde* 5 - /dev/xvdf* 6 - /dev/xvdg* 7 - /dev/xvdh* 8 - /dev/xvdi* 9 - /dev/xvdj
     * see https://cloudstack.apache.org/api/apidocs-4.6/user/attachVolume.html
     */
    private Map<String, PersistentDisk> getPersistentDisks(String vm_id) {
        VirtualMachine vm = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.name(vm_id)).iterator().next();

        VolumeApi vol = this.api.getVolumeApi();
        Set<Volume> vols = vol.listVolumes(ListVolumesOptions.Builder.virtualMachineId(vm.getId()));
        Map<String, PersistentDisk> attachedDisks = new HashMap<String, PersistentDisk>();

        Iterator<Volume> it = vols.iterator();
        while (it.hasNext()) {
            Volume v = it.next();
            //only DATA disk  - persistent disk. No ROOT disk,no ephemeral disk ?
            if ((v.getType() == Type.DATADISK) && (v.getName().startsWith(CPI_PERSISTENT_DISK_PREFIX))) {
                PersistentDisk dsk = new PersistentDisk();
                dsk.volumeId = v.getDeviceId();
                //calculate vol path letter /dev/xvdX
                int driveIndex = Integer.valueOf(v.getDeviceId());
                char drive = (char) ('a' + driveIndex);
                dsk.path = "/dev/xvd" + drive;

                attachedDisks.put(v.getName(), dsk);
            }
        }
        return attachedDisks;
    }

    /**
     * update the persistent disk in registry, from cloudstack iaas api
     */
    private void updatePersistentDisksInRegistry(String vm_id) {
        Map<String, PersistentDisk> disks = this.getPersistentDisks(vm_id);
        String previousSetting = this.boshRegistry.getRaw(vm_id);
        String newSetting = this.vmSettingGenerator.updateVmSettingForDisks(previousSetting, disks);
        this.boshRegistry.put(vm_id, newSetting);
    }

    /**
     * utility method to check ip conflict (scope is Zone)
     *
     * @param ip
     * @return Vm name using this ip
     */
    public String vmWithIpExists(String ip, Network network) {
        logger.debug("check vm exist with ip {}", ip);

        Set<VirtualMachine> listVirtualMachines = api.getVirtualMachineApi()
                .listVirtualMachines(ListVirtualMachinesOptions.Builder
                        .networkId(network.getId())
                        .zoneId(this.cacheableCloudstackConnector.findZoneId()));
        for (VirtualMachine vm : listVirtualMachines) {
            Set<NIC> nics = vm.getNICs();
            for (NIC nic : nics) {

                String vmIp = nic.getIPAddress();
                if ((vmIp != null) && (vmIp.equals(ip))) {
                    logger.warn("vm {} already uses ip {}", vm.getName(), ip);
                    URI isolationURI = nic.getIsolationURI();
                    return vm.getName();
                }
            }
        }
        return null;
    }

}
