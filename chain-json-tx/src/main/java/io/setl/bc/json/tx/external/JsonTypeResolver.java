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
package io.setl.bc.json.tx.external;

import java.util.Arrays;

import io.setl.bc.pychain.tx.create.BaseTransactionTypeResolver;

/**
 * @author Simon Greatrix on 23/01/2020.
 */
public class JsonTypeResolver {

  /**
   * Add the specified types to the Jackson type resolver for BaseTransaction instances.
   */
  public static void addTypes() {
    BaseTransactionTypeResolver.addTypes(Arrays.asList(
        CreateAcl.class,
        DeleteAcl.class,
        UpdateAcl.class,
        CreateAclRole.class,
        DeleteAclRole.class,
        UpdateAclRole.class,
        CreateDataNamespace.class,
        UpdateDataNamespace.class,
        DeleteDataNamespace.class,
        CreateDataDocument.class,
        CreateValidator.class,
        DeleteValidator.class,
        UpdateDataDocument.class,
        UpdateDataDocumentDescription.class,
        DeleteDataDocument.class
    ));
  }

}
