package com.orange.oss.cloudfoundry.cspi.cloudstack;

import java.util.Set;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.Network;
import org.jclouds.cloudstack.domain.NetworkOffering;
import org.jclouds.cloudstack.domain.OSType;
import org.jclouds.cloudstack.domain.ServiceOffering;
import org.jclouds.cloudstack.domain.SshKeyPair;
import org.jclouds.cloudstack.domain.Template;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.options.ListNetworkOfferingsOptions;
import org.jclouds.cloudstack.options.ListNetworksOptions;
import org.jclouds.cloudstack.options.ListSSHKeyPairsOptions;
import org.jclouds.cloudstack.options.ListServiceOfferingsOptions;
import org.jclouds.cloudstack.options.ListTemplatesOptions;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.Assert;

import com.orange.oss.cloudfoundry.cscpi.config.CloudStackConfiguration;



public class CacheableCloudstackConnector {

	private static Logger logger=LoggerFactory.getLogger(CacheableCloudstackConnector.class.getName());
	
	
	@Autowired
	private CloudStackConfiguration cloudstackConfig;
	
	@Autowired
	private CloudStackApi api;

	@Cacheable("networking-offering")	
	public NetworkOffering findNetworkOffering(String csZoneId, Network network) {
		Set<NetworkOffering> listNetworkOfferings = api.getOfferingApi().listNetworkOfferings(ListNetworkOfferingsOptions.Builder.zoneId(csZoneId).id(network.getNetworkOfferingId()));		
		NetworkOffering networkOffering=listNetworkOfferings.iterator().next();

		//Requirements, check the provided network, must have a correct prerequisite in offering
		// service offering need dhcp (for dhcp bootstrap, and getting vrouter ip, used for metadata access)
		// 					need metadata service for userData
		//					need dns ?
		Set<SshKeyPair> keyPairs=api.getSSHKeyPairApi().listSSHKeyPairs(ListSSHKeyPairsOptions.Builder.name(cloudstackConfig.default_key_name));

		//check keypair existence cloudstackConfig.default_key_name (for clear error message)
		Assert.isTrue(keyPairs.size()>0,"ERROR: no keypair "+ cloudstackConfig.default_key_name +" found in cloudstack. create one");
		return networkOffering;
	}

	@Cacheable("networking")
	public Network findNetwork(String csZoneId, String network_name) {
		//find the network with the provided name
		Network network=null;
		
		Set<Network> listNetworks = api.getNetworkApi().listNetworks(ListNetworksOptions.Builder.zoneId(csZoneId));
		for (Network n:listNetworks){
			if (n.getName().equals(network_name)){
				network=n;
			}
		}
		Assert.notNull(network,"Could not find network "+network_name);
		return network;
	}

	@Cacheable("service-offering")
	public ServiceOffering findComputeOffering(String compute_offering) {
		//find compute offering
		Set<ServiceOffering> computeOfferings = api.getOfferingApi().listServiceOfferings(ListServiceOfferingsOptions.Builder.name(compute_offering));
		
		Assert.isTrue(computeOfferings.size()>0, "Unable to find compute offering "+compute_offering);
		ServiceOffering so=computeOfferings.iterator().next();
		return so;
	}

	@Cacheable("stemcell")
	public Template findStemcell(String stemcell_id) {
		Set<Template> matchingTemplates=api.getTemplateApi().listTemplates(ListTemplatesOptions.Builder.name(stemcell_id));
		Assert.isTrue(matchingTemplates.size()==1,"Did not find a single template with name "+stemcell_id);
		Template stemCellTemplate=matchingTemplates.iterator().next();
		return stemCellTemplate;
	}
	
	/**
	 * utility to retrieve cloudstack zoneId
	 * 
	 * @return
	 */
	@Cacheable("zone")
	public String findZoneId() {
		// TODO: select the exact zone if multiple available
		ListZonesOptions zoneOptions = ListZonesOptions.Builder.available(true);
		Set<Zone> zones = api.getZoneApi().listZones(zoneOptions);
		Assert.notEmpty(zones, "No Zone available");
		String zoneId = "";
		Iterator<Zone> it = zones.iterator();
		while(it.hasNext())
		{
			Zone zone = it.next();
			zoneId = zone.getId();
			if(zone.getName().equals(this.cloudstackConfig.default_zone))
				return zoneId;
		}
		Assert.isTrue(false,
				"Zone not found " + this.cloudstackConfig.default_zone);
		return zoneId;
	}
	
	/**
	 * find cloudstack os type by name
	 * @param targetStemcellOsType
	 * @return
	 */
	@Cacheable("os-type")	
	public OSType findOsType(String targetStemcellOsType) {
		OSType osType=null;		
		//FIXME: find correct os type, as defined by CPI (default is PVM 64 bits)		
		for (OSType ost:api.getGuestOSApi().listOSTypes()){
			if (ost.getDescription().equals(targetStemcellOsType)) osType=ost;
		}
		Assert.notNull(osType, "Unable to find OsType");
		return osType;
	}
	
}
