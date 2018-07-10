package com.orange.oss.cloudfoundry.cscpi.config;


import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


@Configuration
public class CloudStackConfiguration {

    private final static Logger logger = LoggerFactory.getLogger(CloudStackConfiguration.class.getName());


    @Value("${cloudstack.endpoint}")
    public String endpoint;

    @Value("${cloudstack.api_key}")
    public String api_key;

    @Value("${cloudstack.secret_access_key}")
    public String secret_access_key;

    @Value("${cloudstack.default_key_name}")
    public String default_key_name;

    @Value("${cloudstack.private_key}")
    public String private_key;


    @Value("${cloudstack.proxy_host}")
    public String proxy_host;

    @Value("${cloudstack.proxy_port}")
    public String proxy_port;

    @Value("${cloudstack.proxy_user}")
    public String proxy_user;

    @Value("${cloudstack.proxy_password}")
    public String proxy_password;

    @Value("${cloudstack.state_timeout}")
    public int state_timeout;

    @Value("${cloudstack.state_timeout_volume}")
    public int state_timeout_volume;

    @Value("${cloudstack.stemcell_public_visibility}")
    public boolean stemcell_public_visibility;

    @Value("${cloudstack.stemcell_publish_timeout}")
    public int publishTemplateTimeoutMinutes;

    @Value("${cloudstack.stemcell_requires_hvm}")
    public boolean stemcell_requires_hvm;


    @Value("${cloudstack.stemcell_os_type}")
    public String stemcell_os_type;


    @Value("${cloudstack.default_zone}")
    public String default_zone;


    @Value("${cpi.vm_create_delay}")
    public int vmCreateDelaySeconds;


    @Value("${cpi.vm_expunge_delay}")
    public int vmExpungeDelaySeconds;

    @Value("${cpi.force_expunge}")
    public boolean forceVmExpunge;


    @Value("${cpi.default_disk_offering}")
    public String defaultDiskOffering;

    @Value("${cpi.default_ephemeral_disk_offering}")
    public String defaultEphemeralDiskOffering;

    @Value("${cpi.lightstemcell.instance_type}")
    public String light_stemcell_instance_type;//"CO1 - Small STD";

    @Value("${cpi.lightstemcell.network_name}")
    public String lightStemcellNetworkName; //"3112 - preprod - back";

    public List<String> calculateDiskTags;
    public List<String> calculateComputeTags;

    @Value("${cpi.calculate_vm_cloud_properties.disk.tags:#{null}}")
    private String calculateDiskTagsRaw;

    @Value("${cpi.calculate_vm_cloud_properties.compute.tags:#{null}}")
    private String calculateComputeTagsRaw;

    @Bean
    public CloudStackApi cloudStackAdapter() {

        String username = this.api_key;
        String password = this.secret_access_key;

        logger.debug("cloudstack adapter. endpoint {} \n username {} \n", endpoint, username);

        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

        logger.debug("initialize jclouds compute API");
        String provider = "cloudstack";

        logger.debug("logging as {}", username);

        Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
        overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");

        //see https://raw.githubusercontent.com/abayer/cloudcat/master/src/groovy/cloudstack/reporting/JCloudsConnection.groovy
        overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, "5000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "VirtualMachineClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "TemplateClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "GlobalHostClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "HostClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "GlobalAlertClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "AlertClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "GlobalAccountClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "AccountClient", "360000");

        if (proxy_host.length() > 0) {
            logger.info("using proxy {}:{} with user {}", proxy_host, proxy_port, proxy_user);

            overrides.setProperty(Constants.PROPERTY_PROXY_HOST, proxy_host);
            overrides.setProperty(Constants.PROPERTY_PROXY_PORT, proxy_port);
            overrides.setProperty(Constants.PROPERTY_PROXY_USER, proxy_user);
            overrides.setProperty(Constants.PROPERTY_PROXY_PASSWORD,
                    proxy_password);
        }

        overrides.setProperty("jclouds.retries-delay-start", "1000");


        CloudStackApi api = ContextBuilder.newBuilder(provider)
                .endpoint(endpoint)
                .credentials(username, password)
                .modules(modules)
                .overrides(overrides)
                .buildApi(CloudStackApi.class);


        return api;

    }

    @PostConstruct
    public void setCalculateTags() {
        calculateDiskTags = calculateDiskTagsRaw == null ? Collections.emptyList() : Arrays.asList(calculateDiskTagsRaw.split(","));
        calculateComputeTags = calculateComputeTagsRaw == null ? Collections.emptyList() : Arrays.asList(calculateComputeTagsRaw.split(","));
    }

}
