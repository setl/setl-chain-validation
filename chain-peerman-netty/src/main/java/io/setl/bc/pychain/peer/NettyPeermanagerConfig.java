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
package io.setl.bc.pychain.peer;

import io.setl.common.CommonPy.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyPeermanagerConfig {

  private static final Logger logger = LoggerFactory.getLogger(NettyPeermanagerConfig.class);


  @Bean
  public NodeType nodeType(@Value("${nodetype:-1}") int nodetype) {
    NodeType nodeType = NodeType.byId(nodetype);
    logger.warn("Peer mesh node type is {}", nodeType);
    return nodeType;
  }

}
