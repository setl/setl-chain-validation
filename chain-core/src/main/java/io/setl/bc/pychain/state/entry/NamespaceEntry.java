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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ImmutableValue;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.serialise.SafeMsgPackable;

/**
 * Entry in state for an asset namespace.
 */
public class NamespaceEntry implements MEntry, SafeMsgPackable {

  /**
   * Definition of an asset class.
   */
  public static class Asset implements SafeMsgPackable {

    final String assetId;

    final String metadata;


    @JsonCreator
    public Asset(
        @JsonProperty("assetId") String assetId,
        @JsonProperty("metadata") String metadata
    ) {
      this.assetId = assetId;
      this.metadata = metadata;
    }


    /**
     * New instance from encoded form.
     *
     * @param unpacker unpacker of encoded form
     */
    public Asset(MessageUnpacker unpacker) throws IOException {
      int version = unpacker.unpackByte();
      if (version != 0) {
        throw new IllegalArgumentException("Unrecognised encoding version: " + version);
      }
      assetId = unpacker.unpackString();
      ImmutableValue value = unpacker.unpackValue();
      if (value.isStringValue()) {
        metadata = value.asStringValue().asString();
      } else {
        metadata = null;
      }
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Asset)) {
        return false;
      }
      Asset asset = (Asset) o;
      return getAssetId().equals(asset.getAssetId()) && Objects.equals(getMetadata(), asset.getMetadata());
    }


    public String getAssetId() {
      return assetId;
    }


    public String getMetadata() {
      return metadata;
    }


    @Override
    public int hashCode() {
      return Objects.hash(getAssetId(), getMetadata());
    }


    @Override
    public void pack(MessagePacker p) throws IOException {
      p.packByte((byte) 0);
      p.packString(assetId);
      if (metadata != null) {
        p.packString(metadata);
      } else {
        p.packNil();
      }
    }


    @Override
    public String toString() {
      return "Asset{"
          + "assetId='" + assetId + '\''
          + ", metadata='" + metadata + '\''
          + '}';
    }

  }



  /**
   * Decoder for namespace instances that were serialized using the encode method.
   */
  public static class Decoder implements EntryDecoder<NamespaceEntry> {

    @Override
    public NamespaceEntry decode(MPWrappedArray va) {
      /*
      [
      Index,
      Name,
      Data  [
              [
                Name,
                Address,
                ClassMap,
                Metadata
              ],
              null,
              updateHeight // Only there if >= 0
      ]
       */

      // int id = va.asInt(0)
      String name = va.asString(1);

      MPWrappedArray data = va.asWrapped(2);

      MPWrappedArray nsData = data.asWrapped(0);
      String metadata = nsData.size() > 3 ? nsData.asString(3) : null;

      NamespaceEntry rVal = new NamespaceEntry(name, nsData.asString(1), nsData.<String, Object[]>asWrappedMap(2), metadata);

      if (data.size() > 2) {
        rVal.updateHeight = data.asLong(2);
      }

      return rVal;
    }

  }



  private final Map<String, Asset> classes = new TreeMap<>();

  private final String name;

  private String metadata;

  private String txFromAddress;

  private long updateHeight = -1;


  /**
   * NamespaceEntry Constructor.
   *
   * @param namespace   :
   * @param fromAddress :
   * @param classMap    :
   * @param metadata    :
   */
  public NamespaceEntry(String namespace, String fromAddress, MPWrappedMap<String, Object[]> classMap, String metadata) {
    name = namespace;
    txFromAddress = fromAddress;
    this.metadata = (metadata == null ? "" : metadata);

    if (classMap != null) {
      classMap.iterate((className, classData) -> classes.put(className, new Asset((String) classData[0], (String) classData[1])));
    }
  }


  /**
   * New instance.
   *
   * @param name     the name of the new namespace
   * @param address  the controlling address of the new namespace
   * @param metadata metadata associated with the new namespace
   */
  public NamespaceEntry(String name, String address, String metadata) {
    this.name = name;
    txFromAddress = address;
    this.metadata = (metadata == null ? "" : metadata);
  }


  /**
   * New instance.
   *
   * @param name     the name of the new namespace
   * @param address  the controlling address of the new namespace
   * @param classes  the classes within the new namespace
   * @param metadata metadata associated with the new namespace
   */
  @JsonCreator
  public NamespaceEntry(
      @JsonProperty("name") String name,
      @JsonProperty("address") String address,
      @JsonProperty("classes") Map<String, Asset> classes,
      @JsonProperty("metadata") String metadata
  ) {
    this.name = name;
    txFromAddress = address;
    this.metadata = (metadata == null ? "" : metadata);
    if (classes != null) {
      this.classes.putAll(classes);
    }
  }


  /**
   * New instance from encoded form.
   *
   * @param unpacker unpacker of encoded form
   */
  public NamespaceEntry(MessageUnpacker unpacker) throws IOException {
    int version = unpacker.unpackByte();
    if (version < 0 || version > 1) {
      throw new IllegalArgumentException("Unrecognised version encoding: " + version);
    }
    name = unpacker.unpackString();
    txFromAddress = unpacker.unpackString();
    metadata = unpacker.unpackString();
    int s = unpacker.unpackMapHeader();
    for (int i = 0; i < s; i++) {
      String key = unpacker.unpackString();
      Asset value = new Asset(unpacker);
      classes.put(key, value);
    }
    if (version >= 1) {
      updateHeight = unpacker.unpackLong();
    }
  }


  /**
   * NamespaceEntry Constructor.
   *
   * @param toCopy :
   */
  public NamespaceEntry(NamespaceEntry toCopy) {
    name = toCopy.name;
    txFromAddress = toCopy.txFromAddress;
    metadata = toCopy.metadata;
    classes.putAll(toCopy.classes);
    updateHeight = toCopy.updateHeight;
  }


  /**
   * Test if this namespace contains an asset of the specified name.
   *
   * @param name the asset's name
   *
   * @return true if the asset is present
   */
  public boolean containsAsset(String name) {
    return classes.containsKey(name);
  }


  @Override
  public NamespaceEntry copy() {
    // Direct copy is faster.
    return new NamespaceEntry(this);
  }


  /**
   * encode().
   *
   * @param index :
   *
   * @return :
   */
  @Override
  @Deprecated(since = "28-11-2019")
  public Object[] encode(long index) {
    TreeMap<String, Object[]> encodedClasses = new TreeMap<>();
    classes.forEach((k, v) -> encodedClasses.put(k, new Object[]{v.getAssetId(), v.getMetadata()}));
    MPWrappedMap<String, Object[]> wrappedMap = new MPWrappedMap<>(encodedClasses);

    Object[] nsData;

    if (updateHeight < 0L) {
      nsData = new Object[]{
          new Object[]{
              name,
              txFromAddress,
              wrappedMap,
              metadata
          },
          null
      };
    } else {
      nsData = new Object[]{
          new Object[]{
              name,
              txFromAddress,
              wrappedMap,
              metadata
          },
          null,
          updateHeight
      };

    }

    return new Object[]{
        Math.max(0, index),
        name,
        nsData
    };
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NamespaceEntry that = (NamespaceEntry) o;
    return Objects.equals(name, that.name)
        && Objects.equals(txFromAddress, that.txFromAddress)
        && Objects.equals(metadata, that.metadata)
        && Objects.equals(classes, that.classes);
  }


  public String getAddress() {
    return txFromAddress;
  }


  /**
   * Get the names of all the assets currently contained in this namespace.
   *
   * @return the asset names
   */
  @JsonIgnore
  public SortedSet<String> getAllAssetNames() {
    return new TreeSet<>(classes.keySet());
  }


  /**
   * Get the named asset from this namespace.
   *
   * @param name the asset's name
   *
   * @return the asset, or null if it does not exist
   */
  @Nullable
  public Asset getAsset(String name) {
    return classes.get(name);
  }


  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  /**
   * getClassMetadata.
   * <p>Return Metadata associated with the given class.
   * Note that this is (Always?) a String, but is returned as an Object just in case.</p>
   *
   * @param className : ClassId
   *
   * @return : Object - Metadata.
   */
  public String getClassMetadata(String className) {
    Asset thisEntry = classes.get(className);
    return (thisEntry != null) ? thisEntry.getMetadata() : null;
  }


  /**
   * Get an immutable copy of the asset's in this namespace.
   *
   * @return an immutable copy
   */
  public Map<String, Asset> getClasses() {
    return Map.copyOf(classes);
  }


  @JsonProperty("name")
  @Nonnull
  @Override
  public String getKey() {
    return name;
  }


  public String getMetadata() {
    return metadata;
  }


  @Override
  public int hashCode() {
    return Objects.hash(metadata, name, txFromAddress);
  }


  /**
   * Test if this namespace is empty.
   *
   * @return true if this namespace contains no assets.
   */
  @JsonIgnore
  public boolean isEmpty() {
    return classes.isEmpty();
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    p.packByte((byte) 1); // version
    p.packString(name);
    p.packString(txFromAddress);
    p.packString(metadata);
    p.packMapHeader(classes.size());
    for (Entry<String, Asset> e : classes.entrySet()) {
      p.packString(e.getKey());
      e.getValue().pack(p);
    }
    p.packLong(updateHeight);
  }


  /**
   * Remove an asset from this namespace.
   *
   * @param name the asset's name
   *
   * @return true if it existed
   */
  public boolean removeAsset(String name) {
    return classes.remove(name) != null;
  }


  public void setAddress(@Nonnull String newAddress) {
    // The owning address is a required field.
    Objects.requireNonNull(newAddress);
    txFromAddress = newAddress;
  }


  public void setAsset(@Nonnull Asset newAsset) {
    classes.put(newAsset.getAssetId(), newAsset);
  }


  @JsonProperty("updateHeight")
  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }


  public void setMetadata(String metadata) {
    this.metadata = (metadata == null ? "" : metadata);
  }


  /**
   * Get the number of assets in this namespace.
   *
   * @return the number of assets
   */
  @JsonIgnore
  public int size() {
    return classes.size();
  }


  @Override
  public String toString() {
    return String.format("%s from:%s classes:%s", name, txFromAddress, classes);
  }

}
