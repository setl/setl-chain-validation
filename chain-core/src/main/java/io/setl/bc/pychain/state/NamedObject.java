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
package io.setl.bc.pychain.state;

import io.setl.bc.pychain.state.entry.MEntry;
import java.util.Objects;

/**
 * A named object which is part of a change to state.
 *
 * @param <DataType> the type of the named object
 */
public class NamedObject<DataType extends MEntry> implements Comparable<NamedObject<?>> {

  /**
   * Is the item new to state?.
   */
  final boolean isNew;

  /** The name of this item in state. */
  final String name;

  /** Is the item deleted?. */
  boolean isDeleted;

  /** The value. */
  DataType object;

  /** The sequence number. */
  int sequence;


  /**
   * An existing entry which is to be inserted, updated, or deleted.
   *
   * @param sequence       the change number
   * @param objectName     the object's name
   * @param objectInstance the object
   */
  protected NamedObject(int sequence, boolean isNew, String objectName, DataType objectInstance) {
    this.isNew = isNew;
    this.name = objectName;
    this.isDeleted = false;
    this.object = objectInstance;
    this.sequence = sequence;
  }


  private NamedObject(int sequence, boolean isDeleted, NamedObject<DataType> original) {
    isNew = original.isNew;
    this.name = original.name;
    this.isDeleted = isDeleted;
    @SuppressWarnings("unchecked")
    DataType value = (DataType) original.object.copy();
    object = value;
    this.sequence = sequence;
  }


  @Override
  public int compareTo(NamedObject<?> otherInstance) {
    // Natural order should be by increasing sequence.
    return Integer.compare(this.sequence, otherInstance.sequence);
  }


  protected NamedObject<DataType> copy(int newSequence, boolean newIsDeleted) {
    return new NamedObject<>(newSequence, newIsDeleted, this);
  }


  @Override
  public boolean equals(Object otherInstance) {
    if (otherInstance == this) {
      return true;
    }
    if (otherInstance == null) {
      return false;
    }
    if (this.getClass() != otherInstance.getClass()) {
      return false;
    }
    NamedObject<?> other = (NamedObject<?>) otherInstance;
    return Objects.equals(name, other.name) && (this.compareTo(other) == 0);
  }


  @Override
  public int hashCode() {
    return name.hashCode() ^ (31 * Integer.hashCode(sequence));
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("NamedObject{");
    sb.append("name=").append(name);
    sb.append(", sequence=").append(sequence);
    sb.append(", isNew=").append(isNew);
    sb.append(", isDeleted=").append(isDeleted);
    sb.append(", object=").append(object);
    sb.append('}');
    return sb.toString();
  }
}

