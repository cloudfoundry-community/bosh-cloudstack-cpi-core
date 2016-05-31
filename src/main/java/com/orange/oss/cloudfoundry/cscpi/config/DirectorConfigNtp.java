package com.orange.oss.cloudfoundry.cscpi.config;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="cpi")
public class DirectorConfigNtp {

	@NotNull
	private final List<String>ntp=new ArrayList<>();

	public List<String> getNtp() {
		return ntp;
	}
	
}
