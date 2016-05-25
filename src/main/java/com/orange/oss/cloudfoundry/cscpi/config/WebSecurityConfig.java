package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class WebSecurityConfig {


    @Value("${cpi.core.user}")
    private String coreUser;
    @Value("${cpi.core.password}")
    private String corePassword;
	
	
    @Value("${cpi.registry.user}")
    private String registryUser;
    @Value("${cpi.registry.password}")
    private String registryPassword;

    
    
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(coreUser).password(corePassword).roles("CORE");
        auth.inMemoryAuthentication().withUser(registryUser).password(registryPassword).roles("REGISTRY");
    }


    @Configuration
    @Order(1)
    public static class ServiceBrokerSecurity extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/cpi/**")
                    .authorizeRequests()
                    .anyRequest()
                    .hasRole("CORE")
                    .and()
                    .httpBasic()
                    .and()
                    .csrf().disable();
        }
    }
    
    @Configuration
    @Order(2)
    public static class RegistrySecurity extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/instances/**")
                    .authorizeRequests()
                    .anyRequest()
                    .hasRole("REGISTRY")
                    .and()
                    .httpBasic()
                    .and()
                    .csrf().disable();
        }
    }
    
}
