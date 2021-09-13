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
package io.setl.bc.pychain;

import java.util.Collection;
import java.util.Set;

@Deprecated
public interface AddressToNamespaceMapper {

  /**
   * Add multiple namespaces to an address.
   *
   * @param address    the address
   * @param namespaces the namespaces
   */
  void add(String address, Collection<String> namespaces);

  /**
   * AddressToNamespacesMapper : append(). Add Address / Namespace to the Mapper.
   *
   * @param address   :
   * @param namespace :
   */
  void addNamespace(String address, String namespace);

  /**
   * Delete a namespace.
   *
   * @param namespace the namespace to delete
   */
  void delete(String namespace);

  /**
   * getAllNamespaces.
   *
   * @return : List of All Namespaces.
   */
  Set<String> getAllNamespaces();

  /**
   * Get the namespaces owned by the specified address.
   *
   * @param address the owner's address
   *
   * @return the visible namespaces
   */
  Set<String> getNamespaces(String address);
}
