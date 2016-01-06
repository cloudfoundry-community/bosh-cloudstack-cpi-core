package com.orange.oss.cloudfoundry.cscpi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PersistentDisk {
	public String path;
	@JsonProperty(value="volume_id")
	public String volumeId;
}
