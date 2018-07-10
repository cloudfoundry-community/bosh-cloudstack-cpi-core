package com.orange.oss.cloudfoundry.cscpi.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Copyright (C) 2018 Orange
 * <p>
 * This software is distributed under the terms and conditions of the 'Apache-2.0'
 * license which can be found in the file 'LICENSE' in this package distribution
 * or at 'https://opensource.org/licenses/Apache-2.0'.
 * <p>
 * Author: Arthur Halet
 * Date: 09/07/2018
 */
@JsonAutoDetect
public class VMParams {
    public int cpu;
    public int ram;
    @JsonProperty("ephemeral_disk_size")
    public int ephemeralDiskSize;
}
