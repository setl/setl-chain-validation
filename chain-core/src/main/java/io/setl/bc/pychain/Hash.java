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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.common.Base64Hex;
import io.setl.common.Hex;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Base64;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.json.simple.JSONAware;
import org.json.simple.JSONValue;
import org.msgpack.core.MessagePacker;

public class Hash implements Serializable, MsgPackable, Comparable<Hash>, JSONAware {

  public static final Hash NULL_HASH = new Hash((byte[]) null);

  /**
   * Unset value (Cast off).
   */
  private static final int UNSET = 0xca570ff;

  private static final long serialVersionUID = 4512309956636207405L;


  /**
   * Get a Hash from its hexadecimal format.
   *
   * @param hexString the hex string
   *
   * @return the new hash, or null if the input was null
   */
  public static Hash fromHex(String hexString) {
    if (hexString == null) {
      return null;
    }
    return new Hash(Hex.decode(hexString));
  }


  /**
   * Get the hexadecimal representation of a hash.
   *
   * @param hash the hash
   *
   * @return the hexadecimal, or null if the input was null
   */
  public static String toHex(Hash hash) {
    return (hash != null) ? hash.toHexString() : null;
  }


  /**
   * The hash value itself.
   */
  protected final byte[] value;

  /**
   * The hash code for the hash value. The hash code is set lazily.
   */
  private int hashCode = UNSET;

  /** A lazily set hexadecimal representation of this hash. */
  private String hexValue;


  /**
   * Hash Constructor.
   */
  @JsonCreator
  public Hash(byte[] hash) {
    if (hash == null) {
      this.value = null;
      return;
    }
    this.value = hash.clone();
  }


  /**
   * Copy constructor.
   *
   * @param hash original
   */
  protected Hash(Hash hash) {
    this.value = hash.value;
    this.hashCode = hash.hashCode;
    this.hexValue = hash.hexValue;
  }


  @Override
  public int compareTo(@Nonnull Hash o) {
    if (value == null) {
      return o.value == null ? 0 : 1;
    }
    if (o.value == null) {
      return -1;
    }

    return Arrays.compareUnsigned(value, o.value);
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    Hash other = (Hash) obj;
    if (value == null) {
      return other.value == null;
    }
    if (other.value == null) {
      return false;
    }
    return Arrays.equals(value, other.value);
  }


  public boolean equalsHex(String hash) {
    return Hex.equals(hash, value);
  }


  /**
   * get(). return value.
   *
   * @return : byte[] value.
   */
  @Nullable
  @JsonValue
  public byte[] get() {
    if (value == null) {
      return null;
    }
    return value.clone();
  }


  /**
   * Get the specified byte of the hash.
   *
   * @param i the index of the required byte
   *
   * @return the byte
   */
  public byte get(int i) {
    return value[i];
  }


  @Override
  public int hashCode() {
    if (hashCode == UNSET) {
      hashCode = Arrays.hashCode(value);
      if (hashCode == UNSET) {
        hashCode *= 13;
      }
    }
    return hashCode;
  }


  public boolean isNull() {
    return value == null;
  }


  @Override
  public void pack(MessagePacker messagePacker) throws IOException {
    if (value == null) {
      // store as a zero-length array
      messagePacker.packNil();
      return;
    }
    messagePacker.packBinaryHeader(value.length).writePayload(value);
  }


  /**
   * Generate the Base-64 representation of this hash.
   *
   * @return this hash in base-64
   */
  public String toB64() {
    if (value == null) {
      return "";
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }


  /**
   * Generate the Base-64-hex representation of this hash. The Base-64-Hex representation uses the same alphabet as the URL safe Base-64, but retains the
   * natural ordering.
   */
  public String toB64Hex() {
    if (value == null) {
      return "";
    }
    return Base64Hex.encodeToString(value);
  }


  /**
   * Get this hash's bytes in hexadecimal format.
   *
   * @return a hexadecimal representation of this hash's byte value
   */
  public String toHexString() {
    if (value == null) {
      return "";
    }
    if (hexValue == null) {
      hexValue = Hex.encode(value);
    }
    return hexValue;
  }


  @Override
  public String toJSONString() {
    return JSONValue.toJSONString(this.toHexString());
  }


  @Override
  public String toString() {
    if (value == null) {
      return "HASH=NULL_TX";
    }
    return Hex.encode(value);
  }
}
