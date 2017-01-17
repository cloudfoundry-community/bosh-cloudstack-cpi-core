#!/bin/sh
APPLICATION_YML_LOCATION=$PWD/config
docker run   -p 8080:8080 -e LOGGING_CONFIG=/config/logback.xml -e SPRING_CONFIG_LOCATION=/config/application.yml -v $APPLICATION_YML_LOCATION:/config orangecloudfoundry/bosh-cloudstack-cpi-core
