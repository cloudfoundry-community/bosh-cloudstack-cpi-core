[![Build Status](https://travis-ci.org/cloudfoundry-community/bosh-cloudstack-cpi-core.png)](https://travis-ci.org/cloudfoundry-community/bosh-cloudstack-cpi-core)

# bosh-cloudstack-cpi-core
bosh external cloustack cpi implementation (spring boot, included in bosh-cloudstack-cpi-release)

The cpi-core is a single spring boot jar with tomcat embedded.
Includes Spring Boot Actuator (eg: can use /health, /trace, /metrics endpoints)

The cpi json execution command must be sent (POST) to /cpi


# TODO
leverage cloudstack simulator
http://sebgoa.blogspot.fr/2015/04/running-cloudstack-simulator-in-docker.html
http://stackoverflow.com/questions/26098531/can-travis-ci-run-docker
