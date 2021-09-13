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
package io.setl.bc.json.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.setl.bc.json.tx.external.JsonTypeResolver;
import io.setl.bc.json.tx.internal.JsonTxFromList;
import io.setl.bc.json.tx.state.JsonUpdateState;
import io.setl.bc.json.validation.ObjectValidator;
import io.setl.bc.json.validation.ValidatorFactory;
import io.setl.bc.pychain.serialise.MsgPack;
import io.setl.bc.pychain.state.tx.TxFromList;
import io.setl.bc.pychain.tx.UpdateState;
import io.setl.json.jackson.JsonModule;

/**
 * Spring configuration for the JSON module.
 *
 * @author Simon Greatrix on 13/02/2020.
 */
@Configuration
@ComponentScan(basePackages = {"io.setl.bc.json.factory"})
public class JsonSpringConfiguration implements InitializingBean {

  @Override
  public void afterPropertiesSet() {
    TxFromList.registerDecoder(new JsonTxFromList());
    UpdateState.registerStateUpdater(new JsonUpdateState());
    JsonTypeResolver.addTypes();
    MsgPack.setObjectMapper(MsgPack.copy().registerModule(new JsonModule()));

    System.out.println("\nJSON Support is enabled.\n");
  }


  @Autowired
  public void setJavaValidator(ObjectMapper objectMapper, javax.validation.Validator javaValidator) {
    ValidatorFactory.addSupplier("java", () -> new ObjectValidator(objectMapper, javaValidator));
  }

}
