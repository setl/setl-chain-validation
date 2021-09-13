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
package io.setl.validation;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Enable the aggregation of validation messages so that we can specify relevant messages in the appropriate modules.
 *
 * @author Simon Greatrix on 14/01/2021.
 */
@Configuration
public class SpringValidation {

  @Bean
  public LocalValidatorFactoryBean validator() {
    // This is the standard Hibernate validator configuration with aggregations of user validation messages enabled.
    // User validation messages are put in the ValidationMessages.properties bundle.
    PlatformResourceBundleLocator resourceBundleLocator = new PlatformResourceBundleLocator(
        ResourceBundleMessageInterpolator.USER_VALIDATION_MESSAGES, null, true);
    LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
    factoryBean.setMessageInterpolator(new ResourceBundleMessageInterpolator(resourceBundleLocator));
    return factoryBean;
  }
}
