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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import java.util.Objects;

/**
 * A tuple combining a Hash and an identifier for the algorithm used to generate the hash.
 *
 * @author Simon Greatrix on 2019-06-07.
 */
// Serialize as a two element array of [ hash, type ].
@JsonFormat(shape = Shape.ARRAY)
@JsonPropertyOrder(alphabetic = true)
public class HashWithType {

  private Hash hash;

  private int type;


  public HashWithType() {
    setHash(Hash.NULL_HASH);
    setType(0);
  }


  public HashWithType(Hash hash, int type) {
    this.setHash(hash);
    this.setType(type);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HashWithType)) {
      return false;
    }
    HashWithType that = (HashWithType) o;
    return getType() == that.getType() && Objects.equals(getHash(), that.getHash());
  }


  public Hash getHash() {
    return hash;
  }


  public int getType() {
    return type;
  }


  @Override
  public int hashCode() {
    return Objects.hash(getHash(), getType());
  }


  public void setHash(Hash hash) {
    this.hash = hash;
  }


  public void setType(int type) {
    this.type = type;
  }


  @Override
  public String toString() {
    return String.format("HashWithType{hash=%s, type=%s}", getHash(), Digest.name(getType()));
  }
}
