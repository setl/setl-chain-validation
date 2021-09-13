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
package io.setl.rest.util;

import io.setl.bc.pychain.AddressToNamespaceMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Deprecated
public class DefaultAddressToNamespacesMapper implements AddressToNamespaceMapper {

  private final Map<String, Set<String>> addressNamespacesMap = new HashMap<>();


  @Override
  public void add(String address, Collection<String> namespaces) {
    Set<String> current = addressNamespacesMap.computeIfAbsent(address, a -> new TreeSet<>());
    current.addAll(namespaces);
  }


  /**
   * AddressToNamespacesMapper : addNamespace(). Add Address / Namespace to the Mapper.
   *
   * @param address   :
   * @param namespace :
   */
  @Override
  public void addNamespace(String address, String namespace) {
    Set<String> namespaces = addressNamespacesMap.computeIfAbsent(address, a -> new TreeSet<>());
    namespaces.add(namespace);
  }


  @Override
  public void delete(String namespace) {
    addressNamespacesMap.values().forEach(set -> set.remove(namespace));
  }


  /**
   * getAllNamespaces.
   *
   * @return : List of All Namespaces.
   */
  @Override
  public Set<String> getAllNamespaces() {
    Set<String> allNamespaces = new TreeSet<>();
    addressNamespacesMap.forEach((address, namespaces) -> allNamespaces.addAll(namespaces));
    return allNamespaces;
  }


  @Override
  public Set<String> getNamespaces(String address) {
    return addressNamespacesMap.getOrDefault(address, new TreeSet<>());
  }
}
