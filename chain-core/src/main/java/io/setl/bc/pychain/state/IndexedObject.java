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

public class IndexedObject<DataType extends MEntry> implements Comparable<IndexedObject<?>> {

  /** The index of this item in state. */
  long index;

  /** Has this entry been deleted?. */
  boolean isDeleted = false;

  /** The value. */
  DataType object;

  /** The sequence number. */
  int sequence;


  /**
   * An existing entry which is to be cached, updated, or deleted.
   *
   * @param sequence       the change number
   * @param objectIndex    the object's location
   * @param objectInstance the object
   */
  protected IndexedObject(int sequence, long objectIndex, DataType objectInstance) {
    this.index = objectIndex;
    this.object = objectInstance;
    this.sequence = sequence;
  }


  /**
   * A new entry.
   *
   * @param sequence       the change number
   * @param objectInstance the new object
   */
  protected IndexedObject(int sequence, DataType objectInstance) {
    this.index = -1;
    this.object = objectInstance;
    this.sequence = sequence;
  }


  @Override
  public int compareTo(IndexedObject<?> otherInstance) {
    // Natural order should be by increasing sequence.
    return Integer.compare(this.sequence, otherInstance.sequence);
  }


  protected IndexedObject<DataType> copy(int newSequence) {
    @SuppressWarnings("unchecked")
    DataType value = (DataType) object.copy();
    return new IndexedObject<>(newSequence, index, value);
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
    return (this.compareTo((IndexedObject<?>) otherInstance) == 0);
  }


  @Override
  public int hashCode() {
    return Long.hashCode(index) ^ (31 * Integer.hashCode(sequence));
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("IndexedObject{");
    sb.append("index=").append(index);
    sb.append(", sequence=").append(sequence);
    sb.append(", isDeleted=").append(isDeleted);
    sb.append(", object=").append(object);
    sb.append('}');
    return sb.toString();
  }
}

