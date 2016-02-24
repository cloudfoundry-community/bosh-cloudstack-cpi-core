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

    @Value("${broker.username:user}")
    private String brokerUsername;
    @Value("${broker.password:password}")
    private String brokerPassword;

    @Value("${admin.username:admin}")
    private String adminUsername;
    @Value("${admin.password:password}")
    private String adminPassword;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(brokerUsername).password(brokerPassword).roles("API");
        auth.inMemoryAuthentication().withUser(adminUsername).password(adminPassword).roles("REGISTRY");
    }


    @Configuration
    @Order(1)
    public static class ServiceBrokerSecurity extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/cpi/**")
                    .authorizeRequests()
                    .anyRequest();
//                    .hasRole("API")
//                    .and()
//                    .httpBasic()
//                    .and()
//                    .csrf().disable();
        }
    }
    
//    @Configuration
//    @Order(2)
    public static class RegistrySecurity extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/registry/**")
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
