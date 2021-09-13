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
package io.setl.bc.pychain.serialise;

import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.entry.HashWithType;

/**
 * Something that is stored in a content-addressable store.
 *
 * @author Simon Greatrix on 2019-05-31.
 */
public class Content {

  /** The serialized data. */
  private final byte[] data;

  /** The digest type used to derive the hash. */
  private final int digestType;

  /** The hash derived from the data. */
  private final Hash key;


  /**
   * Combine data and a digest into a Content instance.
   *
   * @param digest the digest
   * @param data   the data
   */
  public Content(Digest digest, byte[] data) {
    this(digest, data, data);
  }


  /**
   * Combine data and a digest into a Content instance.
   *
   * @param digest   the digest
   * @param hashData the data to use with the digest.
   * @param data     the data
   */
  public Content(Digest digest, byte[] hashData, byte[] data) {
    this.data = data.clone();
    this.key = digest.digest(hashData);
    this.digestType = digest.getType();
  }


  /**
   * Get the serialized data.
   *
   * @return the data
   */
  public byte[] getData() {
    return data;
  }


  /**
   * Get the digest type used to derive the hash.
   *
   * @return the digest type
   */
  public int getDigestType() {
    return digestType;
  }


  /**
   * Get the typed hash of the data.
   *
   * @return the typed hash
   */
  public HashWithType getHash() {
    return new HashWithType(key, digestType);
  }


  /**
   * Get the hash of the data.
   *
   * @return the hash
   */
  public Hash getKey() {
    return key;
  }
}
