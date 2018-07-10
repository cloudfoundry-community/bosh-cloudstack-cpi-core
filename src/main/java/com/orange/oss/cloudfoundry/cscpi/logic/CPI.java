package com.orange.oss.cloudfoundry.cscpi.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;
import com.orange.oss.cloudfoundry.cscpi.domain.ResourcePool;
import com.orange.oss.cloudfoundry.cscpi.domain.VMParams;
import com.orange.oss.cloudfoundry.cscpi.exceptions.CpiErrorException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.NotSupportedException;
import com.orange.oss.cloudfoundry.cscpi.exceptions.VMCreationFailedException;

import java.util.List;
import java.util.Map;

/**
 * CPI - Cloud Provider Interface, used for interfacing with various IaaS APIs.
 * Key terms:
 * Stemcell: template used for creating VMs (shouldn't be powered on)
 * VM:       VM created from a stemcell with custom settings (networking and resources)
 * Disk:     volume that can be attached and detached from the VMs,
 * never attached to more than a single VM at one time
 *
 * @see https://github.com/cloudfoundry/bosh/wiki/BOSH-CPI-API-v1-%28WIP%29
 */
public interface CPI {


    /**
     * ##
     * # Creates a VM - creates (and powers on) a VM from a stemcell with the proper resources
     * # and on the specified network. When disk locality is present the VM will be placed near
     * # the provided disk so it won't have to move when the disk is attached later.
     * #
     * # Sample networking config:
     * #  {"network_a" =>
     * #    {
     * #      "netmask"          => "255.255.248.0",
     * #      "ip"               => "172.30.41.40",
     * #      "gateway"          => "172.30.40.1",
     * #      "dns"              => ["172.30.22.153", "172.30.22.154"],
     * #      "cloud_properties" => {"name" => "VLAN444"}
     * #    }
     * #  }
     * #
     * # Sample resource pool config (CPI specific):
     * #  {
     * #    "ram"  => 512,
     * #    "disk" => 512,
     * #    "cpu"  => 1
     * #  }
     * # or similar for EC2:
     * #  {"name" => "m1.small"}
     * #
     * # @param [String] agent_id UUID for the agent that will be used later on by the director
     * #                 to locate and talk to the agent
     * # @param [String] stemcell stemcell id that was once returned by {#create_stemcell}
     * # @param [Hash] resource_pool cloud specific properties describing the resources needed
     * #               for this VM
     * # @param [Hash] networks list of networks and their settings needed for this VM
     * # @param [optional, String, Array] disk_locality disk id(s) if known of the disk(s) that will be
     * #                                    attached to this vm
     * # @param [optional, Hash] env environment that will be passed to this vm
     * # @return [String] opaque id later used by {#configure_networks}, {#attach_disk},
     * #                  {#detach_disk}, and {#delete_vm}
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
    String create_vm(String agent_id, String stemcell_id,
                     ResourcePool resource_pool, Networks networks,
                     List<String> disk_locality, JsonNode env) throws VMCreationFailedException;


    /**
     * Get the vm_id of this host
     *
     * @return [String] opaque id later used by other methods of the CPI
     */
    @Deprecated
    String current_vm_id();


    /**
     * Creates a stemcell
     *
     * @param [String] image_path path to an opaque blob containing the stemcell image
     * @param [Hash]   cloud_properties properties required for creating this template
     *                 specific to a CPI
     * @return [String] opaque id later used by {#create_vm} and {#delete_stemcell}
     * @throws CpiErrorException
     */
    String create_stemcell(String image_path, Map<String, Object> cloud_properties) throws CpiErrorException;


    /**
     * Deletes a stemcell
     *
     * @param [String] stemcell stemcell id that was once returned by {#create_stemcell}
     * @return [void]
     */
    void delete_stemcell(String stemcell_id);


    /**
     * ##
     * # Deletes a VM
     * #
     * # @param [String] vm vm id that was once returned by {#create_vm}
     * # @return [void]
     * def delete_vm(vm_id)
     * not_implemented(:delete_vm)
     * end
     *
     * @throws CpiErrorException
     **/
    void delete_vm(String vm_id) throws CpiErrorException;

    /**
     * ##
     * # Checks if a VM exists
     * #
     * # @param [String] vm vm id that was once returned by {#create_vm}
     * # @return [Boolean] True if the vm exists
     * def has_vm?(vm_id)
     * not_implemented(:has_vm?)
     * end
     */

    boolean has_vm(String vm_id);


    /**
     * ##
     * # Checks if a disk exists
     * #
     * # @param [String] disk disk_id that was once returned by {#create_disk}
     * # @return [Boolean] True if the disk exists
     * def has_disk?(disk_id)
     * not_implemented(:has_disk?)
     * end
     */

    boolean has_disk(String disk_id);


    /**
     * ##
     * # Reboots a VM
     * #
     * # @param [String] vm vm id that was once returned by {#create_vm}
     * # @param [Optional, Hash] CPI specific options (e.g hard/soft reboot)
     * # @return [void]
     * def reboot_vm(vm_id)
     * not_implemented(:reboot_vm)
     * end
     */

    void reboot_vm(String vm_id);


    /**
     * ##
     * # Set metadata for a VM
     * #
     * # Optional. Implement to provide more information for the IaaS.
     * #
     * # @param [String] vm vm id that was once returned by {#create_vm}
     * # @param [Hash] metadata metadata key/value pairs
     * # @return [void]
     * def set_vm_metadata(vm, metadata)
     * not_implemented(:set_vm_metadata)
     * end
     */
    void set_vm_metadata(String vm_id, Map<String, String> metadata);

    /**
     * ##
     * # Set metadata for a disk
     * #
     * # Optional. Implement to provide more information for the IaaS.
     * #
     * # @param [String] disk id id that was once returned by {#create_vm}
     * # @param [Hash] metadata metadata key/value pairs
     * # @return [void]
     * def set_disk_metadata(disk_id, metadata)
     * end
     */
    void set_disk_metadata(String disk_id, Map<String, String> metadata);

    /**
     * # Configures networking an existing VM.
     * #
     * # @param [String] vm vm id that was once returned by {#create_vm}
     * # @param [Hash] networks list of networks and their settings needed for this VM,
     * #               same as the networks argument in {#create_vm}
     * # @return [void]
     * def configure_networks(vm_id, networks)
     * not_implemented(:configure_networks)
     * end
     *
     * @throws NotSupportedException
     */
    void configure_networks(String vm_id, Networks networks) throws NotSupportedException;


    /**
     * ##
     * # Creates a disk (possibly lazily) that will be attached later to a VM. When
     * # VM locality is specified the disk will be placed near the VM so it won't have to move
     * # when it's attached later.
     * #
     * # @param [Integer] size disk size in MB
     * # @param [Hash] cloud_properties properties required for creating this disk
     * #               specific to a CPI
     * # @param [optional, String] vm_locality vm id if known of the VM that this disk will
     * #                           be attached to
     * # @return [String] opaque id later used by {#attach_disk}, {#detach_disk}, and {#delete_disk}
     * def create_disk(size, cloud_properties, vm_locality = nil)
     * not_implemented(:create_disk)
     * end
     */
    String create_disk(Integer size, Map<String, String> cloud_properties);


    /**
     * ##
     * # Deletes a disk
     * # Will raise an exception if the disk is attached to a VM
     * #
     * # @param [String] disk disk id that was once returned by {#create_disk}
     * # @return [void]
     * def delete_disk(disk_id)
     * not_implemented(:delete_disk)
     * end
     */
    void delete_disk(String disk_id);


    /**
     * # Attaches a disk
     * # @param [String] vm vm id that was once returned by {#create_vm}
     * # @param [String] disk disk id that was once returned by {#create_disk}
     * # @return [void]
     * def attach_disk(vm_id, disk_id)
     * not_implemented(:attach_disk)
     * end
     */
    void attach_disk(String vm_id, String disk_id);


    /**
     * # Take snapshot of disk
     * # @param [String] disk_id disk id of the disk to take the snapshot of
     * # @param [Hash] metadata metadata key/value pairs
     * # @return [String] snapshot id
     * def snapshot_disk(disk_id, metadata={})
     * not_implemented(:snapshot_disk)
     * end
     */
    String snapshot_disk(String disk_id, Map<String, String> metadata);


    /**
     * # Delete a disk snapshot
     * # @param [String] snapshot_id snapshot id to delete
     * # @return [void]
     * def delete_snapshot(snapshot_id)
     * not_implemented(:delete_snapshot)
     * end
     */
    void delete_snapshot(String snapshot_id);


    /**
     * # Detaches a disk
     * # @param [String] vm vm id that was once returned by {#create_vm}
     * # @param [String] disk disk id that was once returned by {#create_disk}
     * # @return [void]
     * def detach_disk(vm_id, disk_id)
     * not_implemented(:detach_disk)
     * end
     */
    void detach_disk(String vm_id, String disk_id);


    /**
     * # List the attached disks of the VM.
     * # @param [String] vm_id is the CPI-standard vm_id (eg, returned from current_vm_id)
     * # @return [array[String]] list of opaque disk_ids that can be used with the
     * # other disk-related methods on the CPI
     * def get_disks(vm_id)
     * not_implemented(:get_disks)
     * end
     */
    List<String> get_disks(String vm_id);


    /**
     * Give back all supported stemcell format by this cpi
     *
     * @return List<String>
     */
    List<String> stemcell_formats();

    /**
     * Returns a hash that can be used as VM cloud_properties when calling create_vm; it describes the IaaS instance type closest to the arguments passed.
     *
     * @param vmParams - e.g.: VM with the parameters { "cpu": 1, "ram": 512, "ephemeral_disk_size": 1024 }
     * @return ResourcePool
     */
    ResourcePool calculate_vm_cloud_properties(VMParams vmParams);
}
