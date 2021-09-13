/* <notice>
 
    SETL Blockchain
    Copyright (C) 2021 SETL Ltd
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License, version 3, as
    published by the Free Software Foundation.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
 
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 
</notice> */
package io.setl.bc.pychain.node;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private static final String ROLE_MONITOR = "MONITOR";

  @Value("${health.user:}")
  private String user;

  @Value("${health.password:}")
  private String password;

  @Value("${management.endpoint.health.enabled:false}")
  private boolean healthEnabled;


  @SuppressFBWarnings("SPRING_CSRF_PROTECTION_DISABLED")
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    if (healthEnabled) {
      http.requestMatcher(EndpointRequest.toAnyEndpoint()).authorizeRequests()
          .anyRequest().hasRole(ROLE_MONITOR)
          .and()
          .httpBasic();
    } else {
      //Open access to JWT API
      http.csrf().disable();
    }
  }


  @Autowired
  protected void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    if (!user.isEmpty() && !password.isEmpty()) {
      auth.inMemoryAuthentication()
          .passwordEncoder(NoOpPasswordEncoder.getInstance())
          .withUser(user)
          .password(password)
          .roles(ROLE_MONITOR);
    }
  }
}
