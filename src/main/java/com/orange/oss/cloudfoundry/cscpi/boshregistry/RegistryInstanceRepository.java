package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import org.springframework.data.repository.CrudRepository;


/**
 * Spring Data JPA interface to access DB
 * @author pierre
 *
 */
public interface RegistryInstanceRepository extends CrudRepository<RegistryInstance, String> { 

}
