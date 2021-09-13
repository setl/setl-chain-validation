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

import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.msgpack.MPWrappedArray;

/**
 * State entry recording that an asset is locked.
 *
 * @author Simon Greatrix on 30/12/2018.
 */
public class LockedAsset implements MEntry {

  /**
   * Type of asset lock.
   */
  public enum Type {
    NO_LOCK(0),
    FULL(1);

    private static final Type[] FOR_CODES;


    @JsonCreator
    public static Type forCode(int code) {
      return FOR_CODES[code];
    }


    static {
      Type[] values = values();
      FOR_CODES = new Type[values.length];
      for (Type t : values) {
        FOR_CODES[t.code] = t;
      }
    }

    private final int code;


    Type(int code) {
      this.code = code;
    }


    @JsonValue
    public int code() {
      return code;
    }
  }



  /**
   * Decoder for deserializing locked asset instances that were persisted using the encode method.
   */
  public static class Decoder implements EntryDecoder<LockedAsset> {

    @Override
    public LockedAsset decode(MPWrappedArray va) {
      LockedAsset asset = new LockedAsset(va.asString(1), Type.forCode(va.asInt(2)));
      if (va.size() > 3) {
        asset.setBlockUpdateHeight(va.asLong(3));
      }
      return asset;
    }

  }



  private final String assetId;

  private Type type;

  private long updateHeight = -1;


  /**
   * New instance.
   *
   * @param assetId the asset's ID
   * @param type    the lock type
   */
  @JsonCreator
  public LockedAsset(
      @JsonProperty("key") String assetId,
      @JsonProperty("type") Type type
  ) {
    this.assetId = assetId;
    this.type = type;
  }


  /**
   * New instance.
   *
   * @param assetId      the asset's ID
   * @param type         the lock type
   * @param updateHeight last block change height
   */
  public LockedAsset(
      String assetId,
      Type type,
      long updateHeight
  ) {
    this.assetId = assetId;
    this.type = type;
    this.updateHeight = Math.max(-1L, updateHeight);
  }


  /**
   * New instance.
   *
   * @param toCopy the original to copy
   */
  public LockedAsset(LockedAsset toCopy) {
    assetId = toCopy.assetId;
    type = toCopy.type;
    updateHeight = toCopy.updateHeight;
  }


  /**
   * New instance from encoded form.
   *
   * @param unpacker unpacker of encoded form
   */
  public LockedAsset(MessageUnpacker unpacker) throws IOException {
    byte version = unpacker.unpackByte();
    if (version != 0) {
      throw new IllegalArgumentException("Unrecognised encoding version: " + version);
    }
    assetId = unpacker.unpackString();
    type = Type.forCode(unpacker.unpackInt());
    updateHeight = unpacker.unpackLong();
  }


  @Override
  public LockedAsset copy() {
    return new LockedAsset(this);
  }


  @Override
  @Deprecated(since = "28-11-2019")
  public Object[] encode(long index) {
    if (updateHeight < 0) {
      return new Object[]{0, assetId, type.code()};
    }
    return new Object[]{0, assetId, type.code(), updateHeight};
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LockedAsset)) {
      return false;
    }
    LockedAsset that = (LockedAsset) o;
    return assetId.equals(that.assetId) && getType() == that.getType();
  }


  @JsonProperty("blockUpdateHeight")
  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  @Nonnull
  @Override
  @JsonProperty("key")
  public String getKey() {
    return assetId;
  }


  @JsonProperty("type")
  public Type getType() {
    return type;
  }


  @Override
  public int hashCode() {
    return Objects.hash(assetId, getType());
  }


  @JsonProperty("blockUpdateHeight")
  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }


  /**
   * Set the type of lock applied to the asset.
   *
   * @param newType      the lock type
   * @param updateHeight the height at which the type of lock was set.
   */
  public void setType(Type newType, long updateHeight) {
    if (type != newType) {
      type = newType;
      setBlockUpdateHeight(updateHeight);
    }
  }


  @Override
  public String toString() {
    return "LockedAsset{" + "assetId='" + assetId + '\'' + ", type=" + type + '}';
  }

}
