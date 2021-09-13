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
package io.setl.bc.pychain.state.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.serialise.MsgPack;
import io.setl.bc.pychain.state.HashStore;
import io.setl.bc.pychain.state.entry.HashWithType;

/**
 * @author Simon Greatrix on 2019-05-21.
 */
abstract class Entry {

  protected static byte[] encode(Entry e) {
    ObjectWriter writer = MsgPack.writer();
    try {
      return writer.writeValueAsBytes(e);
    } catch (JsonProcessingException ex) {
      throw new InternalError("Unable to serialize structure entry: " + e.getClass(), ex);
    }
  }


  HashWithType myHash;


  HashWithType computeHash(int type) {
    if (myHash != null && myHash.getType() == type) {
      return myHash;
    }
    myHash = new HashWithType(Digest.create(type).digest(encode(this)), type);
    return myHash;
  }


  @JsonIgnore
  public HashWithType getHash() {
    return myHash;
  }


  void save(HashStore store) {
    byte[] encoded = encode(this);
    myHash = new HashWithType(store.insert(encoded), Digest.TYPE_SHA_512_256);
  }


  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new InternalError("Unable to serialize structure entry: " + getClass(), e);
    }
  }


  public boolean verify() {
    if (myHash == null) {
      return true;
    }
    Hash actual = Digest.create(myHash.getType()).digest(encode(this));
    return myHash.getHash().equals(actual);
  }
}
