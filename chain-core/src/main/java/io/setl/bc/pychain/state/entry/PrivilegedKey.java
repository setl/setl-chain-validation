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

import java.security.PublicKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.setl.bc.pychain.HashedMap;
import io.setl.bc.pychain.serialise.MsgPack;
import io.setl.bc.pychain.state.monolithic.EncodedHashedMap;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Hex;

/**
 * @author Simon Greatrix on 30/12/2018.
 */
// Property "updateHeight" was previously "blockUpdateHeight"
@JsonFormat(shape = Shape.ARRAY)
@JsonPropertyOrder({"address", "updateHeight", "expiry", "hexPublicKey", "name", "permissions"})
public class PrivilegedKey {

  public static class Encoded extends EncodedHashedMap<String, PrivilegedKey, Object[]> {

    public Encoded() {
      super(pk -> MsgPack.convert(pk, Object[].class));
    }


    /**
     * New instance.
     *
     * @param map original map
     */
    public Encoded(Map<String, Object[]> map) {
      super(
          pk -> MsgPack.convert(pk, Object[].class),
          oa -> MsgPack.convert(oa, PrivilegedKey.class),
          map
      );
    }

  }



  public static class Store extends HashedMap<String, PrivilegedKey> {

    public Store() {
      super();
    }


    @JsonCreator
    public Store(Map<String, PrivilegedKey> map) {
      super(map);
    }

  }



  private final String address;

  private final long expiry;

  private final String hexPublicKey;

  private final String name;

  private final SortedSet<String> permissions;

  private final long updateHeight;


  /**
   * New instance.
   *
   * @param address      the address associated with this
   * @param expiry       when this expires
   * @param hexPublicKey the public key in hexadecimal
   * @param name         the name of this public key
   * @param permissions  the permissions granted to this public key
   * @param updateHeight the height at which this key was last changed
   */
  @JsonCreator
  public PrivilegedKey(
      @JsonProperty("address") String address,
      @JsonProperty("expiry") long expiry,
      @JsonProperty("hexPublicKey") String hexPublicKey,
      @JsonProperty("name") String name,
      @JsonProperty("permissions") Collection<String> permissions,
      @JsonProperty("updateHeight") long updateHeight
  ) {
    this.address = address;
    this.expiry = expiry;
    this.hexPublicKey = hexPublicKey;
    this.name = name;
    this.permissions = new TreeSet<>(permissions);
    this.updateHeight = updateHeight;
  }


  /**
   * New instance without permissions.
   *
   * @param name         the key's name
   * @param expiry       the key's expiry
   * @param publicKey    the key's public key
   * @param updateHeight the change height
   */
  public PrivilegedKey(String name, long expiry, PublicKey publicKey, long updateHeight) {
    this.name = name;
    this.expiry = expiry;
    address = AddressUtil.publicKeyToAddress(publicKey, AddressType.PRIVILEGED);
    hexPublicKey = Hex.encode(publicKey.getEncoded());
    this.permissions = new TreeSet<>();
    this.updateHeight = updateHeight;
  }


  /**
   * New instance as a copy of another instance.
   *
   * @param privilegedKey the other instance
   * @param updateHeight  the change height
   */
  public PrivilegedKey(PrivilegedKey privilegedKey, long updateHeight) {
    address = privilegedKey.address;
    expiry = privilegedKey.expiry;
    hexPublicKey = privilegedKey.hexPublicKey;
    name = privilegedKey.name;
    permissions = new TreeSet<>(privilegedKey.permissions);
    this.updateHeight = Math.max(privilegedKey.updateHeight, updateHeight);
  }


  /**
   * Create a new instance adding the specified permission.
   *
   * @param value        the permission to add
   * @param updateHeight the height of the change
   *
   * @return the new instance
   */
  public PrivilegedKey addPermission(String value, long updateHeight) {
    if (!permissions.contains(value)) {
      PrivilegedKey copy = new PrivilegedKey(this, updateHeight);
      copy.permissions.add(value);
      return copy;
    }
    return this;
  }


  /**
   * Create a new instance with a permission deleted from the set of granted permissions.
   *
   * @param value        the permission to delete
   * @param updateHeight the block change height
   *
   * @return the new instance
   */
  public PrivilegedKey deletePermission(String value, long updateHeight) {
    if (permissions.contains(value)) {
      PrivilegedKey copy = new PrivilegedKey(this, updateHeight);
      copy.permissions.remove(value);
      return copy;
    }
    return this;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PrivilegedKey)) {
      return false;
    }
    PrivilegedKey that = (PrivilegedKey) o;
    return getExpiry() == that.getExpiry()
        && getAddress().equals(that.getAddress())
        && getHexPublicKey().equals(that.getHexPublicKey())
        && getName().equals(that.getName())
        && getPermissions().equals(that.getPermissions());
  }


  @JsonProperty
  public String getAddress() {
    return address;
  }


  @JsonProperty("updateHeight")
  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  @JsonProperty
  public long getExpiry() {
    return expiry;
  }


  @JsonProperty
  public String getHexPublicKey() {
    return hexPublicKey;
  }


  @JsonIgnore
  @Nonnull
  public String getKey() {
    return getAddress();
  }


  @JsonProperty
  public String getName() {
    return name;
  }


  /**
   * Get the permissions granted to this privileged key.
   *
   * @return the permissions
   */
  @JsonProperty
  public SortedSet<String> getPermissions() {
    // Expired addresses have no permissions
    if (Instant.now().getEpochSecond() > expiry) {
      return Collections.emptySortedSet();
    }
    return Collections.unmodifiableSortedSet(permissions);
  }


  public boolean hasPermission(String permission) {
    return getPermissions().contains(permission);
  }


  @Override
  public int hashCode() {
    return Objects.hash(getAddress(), getExpiry(), getHexPublicKey(), getName(), getPermissions());
  }


  /**
   * Create a new instance with the given public key.
   *
   * @param newExpiry    the new expiry date
   * @param publicKey    the new public key
   * @param updateHeight the change height
   *
   * @return the new instance
   */
  public PrivilegedKey setKey(long newExpiry, PublicKey publicKey, long updateHeight) {
    String newAddress = AddressUtil.publicKeyToAddress(publicKey, AddressType.PRIVILEGED);
    String newPublicKey = Hex.encode(publicKey.getEncoded());
    return new PrivilegedKey(
        newAddress,
        newExpiry,
        newPublicKey,
        name,
        permissions,
        updateHeight
    );
  }


  @Override
  public String toString() {
    return String.format(
        "PrivilegedKey(address='%s', expiry=%s, hexPublicKey='%s', name='%s', permissions=%s, updateHeight=%s)",
        this.address, this.expiry, this.hexPublicKey, this.name, this.permissions, this.updateHeight
    );
  }

}
