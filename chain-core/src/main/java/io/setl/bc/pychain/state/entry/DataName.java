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
package io.setl.bc.pychain.state.entry;

import java.util.function.Function;

import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.state.HashStore;

/**
 * The name of a datum in state.
 *
 * @author Simon Greatrix on 2019-12-20.
 */
public class DataName<X extends NamedDatum<X>> {


  private final Function<HashStore, X> creator;

  private final Function<HashStore, ContentFactory<X>> factory;

  private final String name;

  private final Class<X> type;


  /**
   * New instance.
   *
   * @param name     the datum's name
   * @param factory  factory for accessing persisted data
   * @param creator  creator for new instances of the datum
   * @param register if true, register this as a re-usable global name
   */
  public DataName(
      String name,
      Class<X> type,
      Function<HashStore, ContentFactory<X>> factory,
      Function<HashStore, X> creator,
      boolean register
  ) {
    this.name = name;
    this.type = type;
    this.factory = factory;
    this.creator = creator;
    if (register) {
      DataNameManager.register(this);
    }
  }


  public X createInitial(HashStore hashStore) {
    return creator.apply(hashStore);
  }


  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DataName<?>)) {
      return false;
    }
    DataName<?> other = (DataName<?>) o;
    return name.equals(other.name) && type.equals(other.type);
  }


  public ContentFactory<X> getFactory(HashStore store) {
    return factory.apply(store);
  }


  public String getName() {
    return name;
  }


  public Class<X> getType() {
    return type;
  }


  @Override
  public int hashCode() {
    return name.hashCode();
  }


  @Override
  public String toString() {
    return "[" + name + ":" + type + "]";
  }

}
