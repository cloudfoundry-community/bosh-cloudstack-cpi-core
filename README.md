[![Build Status](https://travis-ci.org/cloudfoundry-community/bosh-cloudstack-cpi-core.png)](https://travis-ci.org/cloudfoundry-community/bosh-cloudstack-cpi-core)
[![Docker Automated build](https://img.shields.io/docker/automated/jrottenberg/ffmpeg.svg)](https://hub.docker.com/r/orangecloudfoundry/bosh-cloudstack-cpi-core/builds/)

# bosh-cloudstack-cpi-core
bosh external cloustack cpi implementation (spring boot, included in bosh-cloudstack-cpi-release)

The cpi-core is a single spring boot jar with tomcat embedded.
Includes Spring Boot Actuator (eg: can use /health, /trace, /metrics endpoints), Hystrix and Zipkin Sleuth instrumentation.



For bosh inception, the cpi is also provided as a [docker image](https://hub.docker.com/r/orangecloudfoundry/bosh-cloudstack-cpi-core/), with can be launched with the provided launchCpi.sh script.
An application.yml and logback.xml must be provided, and the cpi-launched before bosh-init deploy.



HowTo Bootstrap
---------------

```bash
docker run -ti --name cpi --rm -v /full/path/to/application-override.yml:/application-override.yml -p 8080:8080  orangecloudfoundry/bosh-cloudstack-cpi-core:latest
```


see [bosh-cloudstack-cpi-release](https://github.com/cloudfoundry-community/bosh-cloudstack-cpi-release) for doc and issues.
