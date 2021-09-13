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
package io.setl.bc.json.factory;

import org.springframework.stereotype.Component;

import io.setl.bc.json.data.acl.AclRole;
import io.setl.common.ObjectArrayReader;

/**
 * @author Simon Greatrix on 22/01/2020.
 */
@Component
public class AclRoleFactory extends BaseDatumFactory<AclRole> {

  @Override
  protected AclRole construct(ObjectArrayReader reader) {
    return new AclRole(reader);
  }


  @Override
  public Class<AclRole> getType() {
    return AclRole.class;
  }

}
