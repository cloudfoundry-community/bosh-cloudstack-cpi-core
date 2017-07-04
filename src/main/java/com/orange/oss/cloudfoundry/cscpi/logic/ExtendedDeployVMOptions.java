package com.orange.oss.cloudfoundry.cscpi.logic;

import static com.google.common.base.Preconditions.checkArgument;

import org.jclouds.cloudstack.options.DeployVirtualMachineOptions;

import com.google.common.collect.ImmutableSet;

public class ExtendedDeployVMOptions extends DeployVirtualMachineOptions {

	@Override
	public ExtendedDeployVMOptions name(String name) {
		return ExtendedDeployVMOptions.class.cast(super.name(name));
	}

	@Override
	public ExtendedDeployVMOptions networkId(String networkId) {
		return ExtendedDeployVMOptions.class.cast(super.networkId(networkId));
	}

	@Override
	public ExtendedDeployVMOptions userData(byte[] unencodedData) {
		return ExtendedDeployVMOptions.class.cast(super.userData(unencodedData));
	}

	@Override
	public ExtendedDeployVMOptions keyPair(String keyPair) {
		return ExtendedDeployVMOptions.class.cast(super.keyPair(keyPair));
	}

	@Override
	public ExtendedDeployVMOptions ipOnDefaultNetwork(String ipOnDefaultNetwork) {
		return ExtendedDeployVMOptions.class.cast(super.ipOnDefaultNetwork(ipOnDefaultNetwork));
	}

	public ExtendedDeployVMOptions affinityGroupNames(String affinitygroupnames) {
		checkArgument(!queryParameters.containsKey("affinitygroupids"), "Mutually exclusive with affinitygroupids");
		this.queryParameters.replaceValues("affinitygroupnames", ImmutableSet.of(affinitygroupnames));
		return this;
	}

	public ExtendedDeployVMOptions affinityGroupIds(String affinitygroupids) {
		checkArgument(!queryParameters.containsKey("affinitygroupnames"), "Mutually exclusive with affinitygroupnames");
		this.queryParameters.replaceValues("affinitygroupids", ImmutableSet.of(affinitygroupids));
		return this;
	}

}
