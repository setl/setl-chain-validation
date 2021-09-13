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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_DEFAULT_AUTHORISE_BY_ADDRESS;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;
import static io.setl.common.CommonPy.VersionConstants.VERSION_SET_ADDRESS_TIME;
import static io.setl.common.CommonPy.VersionConstants.VERSION_SET_FULL_ADDRESS;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.simple.JSONObject;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.serialise.SafeMsgPackable;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import io.setl.util.CollectionUtils;

/**
 * An entry in state for an address with its asset balances.
 */
public class AddressEntry implements MEntry, SafeMsgPackable {

  /**
   * Decoder for address entries persisted using the encode method.
   */
  public static class Decoder implements EntryDecoder<AddressEntry> {

    /**
     * AddressEntry : Decode MsgPack Array representing Merkle Item in Address tree.
     * [Index, Address, [{Holdings Map}, [Nonces], addressPermissions, permissionedTXs, MetaData, updateTime]]
     *
     * @param merkleItem : Item to decode
     *
     * @return : decoded AddressEntry object.
     */
    @Override
    public AddressEntry decode(MPWrappedArray merkleItem) {
      String address = merkleItem.asString(1);
      MPWrappedArray addressDataArray = merkleItem.asWrapped(2);
      MPWrappedMap<String, Object> addressHoldings = addressDataArray.asWrappedMap(0);

      AddressEntry addressEntry;

      if (!addressDataArray.isNull(1)) {

        MPWrappedArray addressNonces = addressDataArray.asWrapped(1);
        long nonce = addressNonces.asLong(0);
        long highPriorityNonce = addressNonces.asLong(1);
        long lowPriorityNonce = addressNonces.asLong(2);
        addressEntry = new AddressEntry(address, nonce, highPriorityNonce, lowPriorityNonce);

      } else {

        addressEntry = new AddressEntry(address);

      }

      if (addressHoldings != null) {

        addressEntry.makeClassBalance();

        addressHoldings.iterate((assetID, assetHolding) -> {
          addressEntry.classBalance.put(assetID, new Balance(assetHolding));
        });
      }

      if (addressDataArray.size() > 6) {
        addressEntry.addressPermissions = (addressDataArray.isNull(2) ? 0L : addressDataArray.asLong(2));
        Object[] txs = addressDataArray.asObjectArray(3);
        if (txs != null) {
          addressEntry.authorisedTx = IntStream.of(CollectionUtils.toIntArray(txs, true, true))
              .mapToObj(TxType::get)
              .filter(Objects::nonNull)
              .collect(
                  () -> EnumSet.noneOf(TxType.class),
                  (set, value) -> set.add(value),
                  (s1, s2) -> s1.addAll(s2)
              );
        }
        addressEntry.addressMetadata = addressDataArray.asString(4);
        addressEntry.updateTime = (addressDataArray.isNull(5) ? null : addressDataArray.asLong(5));
        addressEntry.setBlockUpdateHeight(addressDataArray.isNull(6) ? -1L : addressDataArray.asLong(6));

      } else {
        addressEntry.addressPermissions = 0L;
        addressEntry.authorisedTx = null;
        addressEntry.addressMetadata = null;
        addressEntry.updateTime = null;
      }

      return addressEntry;
    }

  }


  /**
   * addDefaultAddressEntry().
   * <p>Helper function to add a default AddressEntry.
   * This helps to ensure consistency when adding this data from multiple places in code.</p>
   * <p>note that as of version 'VERSION_SET_FULL_ADDRESS' (4) all addresses are established in complete form. i.e. No null nonces or balances.</p>
   *
   * @param assetBalances : Asset Balances List.
   * @param address       :
   * @param nullBalance   : Optional, have the entry added with a 'null' Asset balance entry. (Python compatability).
   *
   * @return :
   */
  private static AddressEntry addAddressEntry(MutableMerkle<AddressEntry> assetBalances, String address, boolean nullBalance, int version) {
    AddressEntry addressEntry = new AddressEntry(address, 0L, 0L, 0L);

    if ((!nullBalance) || (version >= VERSION_SET_FULL_ADDRESS)) {
      addressEntry.makeClassBalance();
    }

    if (version >= VERSION_SET_ADDRESS_TIME) {
      addressEntry.setUpdateTime(0L);
    }

    assetBalances.add(addressEntry);

    return addressEntry;
  }


  /**
   * addBalanceOnlyAddressEntry.
   *
   * @param assetBalances :
   * @param address       :
   *
   * @return :
   */
  @SuppressFBWarnings("UC_USELESS_CONDITION") /* VERSION_SET_ADDRESS_TIME test is considered useless by FindBugs, I disagree. NPP 28/08/2018 */
  public static AddressEntry addBalanceOnlyAddressEntry(MutableMerkle<AddressEntry> assetBalances, String address, int version) {
    if (version >= VERSION_SET_FULL_ADDRESS) {
      return addAddressEntry(assetBalances, address, false, version);
    }

    AddressEntry addressEntry = new AddressEntry(address);

    addressEntry.makeClassBalance();

    if (version >= VERSION_SET_ADDRESS_TIME) {
      addressEntry.setUpdateTime(0L);
    }
    assetBalances.add(addressEntry);

    return addressEntry;
  }


  /**
   * addDefaultAddressEntry().
   * <p>Helper function to add a default AddressEntry.
   * This helps to ensure consistency when adding this data from multiple places in code.</p>
   *
   * @param assetBalances : Asset Balances List.
   * @param address       :
   *
   * @return :
   */
  public static AddressEntry addDefaultAddressEntry(MutableMerkle<AddressEntry> assetBalances, String address, int version) {

    return addAddressEntry(assetBalances, address, false, version);
  }


  /**
   * addDefaultAddressEntry().
   * <p>Helper function to add a default AddressEntry for nonce recording purposes.
   * This helps to ensure consistency when adding this data from multiple places in code.</p>
   *
   * @param assetBalances : Asset Balances List.
   * @param address       :
   *
   * @return :
   */
  public static AddressEntry addDefaultNonceEntry(MutableMerkle<AddressEntry> assetBalances, String address, int version) {

    return addAddressEntry(assetBalances, address, true, version);
  }


  private final String address;

  private String addressMetadata;

  private long addressPermissions;

  private EnumSet<TxType> authorisedTx;

  private SortedMap<String, Balance> classBalance;

  private long highPriNonce;

  private long lowPriNonce;

  private long nonce;

  private boolean nonceUnset;

  private long updateHeight = -1;

  private Long updateTime = null;


  /**
   * AddressEntry constructor.
   */
  public AddressEntry() {
    address = "";

    makeClassBalance();

    nonceUnset = true;
    addressPermissions = 0L;
    authorisedTx = null;
    addressMetadata = null;
    updateTime = null;
  }


  /**
   * AddressEntry constructor.
   *
   * @param address :
   */
  public AddressEntry(String address) {
    this.address = (address == null ? "" : address);
    nonceUnset = true;
    addressPermissions = 0L;
    authorisedTx = null;
    addressMetadata = null;
    updateTime = null;
  }


  /**
   * AddressEntry constructor.
   *
   * @param address      :
   * @param nonce        :
   * @param highPriNonce :
   * @param lowPriNonce  :
   */
  public AddressEntry(String address, long nonce, long highPriNonce, long lowPriNonce) {
    this.address = (address == null ? "" : address);
    this.nonce = nonce;
    this.highPriNonce = highPriNonce;
    this.lowPriNonce = lowPriNonce;
    nonceUnset = false;
    addressPermissions = 0L;
    authorisedTx = null;
    addressMetadata = null;
    updateTime = null;
  }


  /**
   * New instance.
   *
   * @param address the address
   * @param balance the balance
   */
  @JsonCreator
  public AddressEntry(
      @JsonProperty("address") String address,
      @JsonProperty("balance") Map<String, Balance> balance
  ) {
    this.address = (address == null ? "" : address);
    if (balance != null) {
      makeClassBalance().putAll(balance);
    }
  }


  /**
   * New instance.
   *
   * @param toCopy original to copy
   */
  public AddressEntry(AddressEntry toCopy) {
    address = toCopy.address;
    nonce = toCopy.nonce;
    highPriNonce = toCopy.highPriNonce;
    lowPriNonce = toCopy.lowPriNonce;
    nonceUnset = toCopy.nonceUnset;
    addressPermissions = toCopy.addressPermissions;
    authorisedTx = (toCopy.authorisedTx == null ? null : toCopy.authorisedTx.clone());
    addressMetadata = toCopy.addressMetadata;
    updateTime = toCopy.updateTime;
    updateHeight = toCopy.updateHeight;

    if (toCopy.classBalance != null) {
      makeClassBalance();
      classBalance.putAll(toCopy.classBalance);
    }
  }


  /**
   * New instance from the encoded form.
   *
   * @param unpacker unpacker of the encoded form
   */
  public AddressEntry(MessageUnpacker unpacker) throws IOException {
    int version = unpacker.unpackByte();
    if (version < 0 || version > 1) {
      throw new IllegalArgumentException("Unrecognised encoding version: " + version);
    }
    int flag = unpacker.unpackInt();

    address = unpacker.unpackString();
    if ((flag & 1) != 0) {
      addressMetadata = unpacker.unpackString();
    } else {
      addressMetadata = null;
    }

    addressPermissions = unpacker.unpackLong();

    if ((flag & 2) != 0) {
      Map<String, Balance> map = makeClassBalance();
      int s = unpacker.unpackMapHeader();
      for (int i = 0; i < s; i++) {
        String key = unpacker.unpackString();
        Balance value = new Balance(unpacker);
        map.put(key, value);
      }
    } else {
      classBalance = null;
    }

    if ((flag & 4) != 0) {
      nonce = unpacker.unpackLong();
      lowPriNonce = unpacker.unpackLong();
      highPriNonce = unpacker.unpackLong();
      nonceUnset = false;
    } else {
      nonce = 0;
      lowPriNonce = 0;
      highPriNonce = 0;
      nonceUnset = true;
    }

    if ((flag & 8) != 0) {
      int s = unpacker.unpackArrayHeader();
      EnumSet<TxType> tmp = EnumSet.noneOf(TxType.class);
      for (int i = 0; i < s; i++) {
        tmp.add(TxType.get(unpacker.unpackInt()));
      }
      setAuthorisedTx(tmp);
    } else {
      authorisedTx = null;
    }

    if ((flag & 16) != 0) {
      updateTime = unpacker.unpackLong();
    } else {
      updateTime = null;
    }

    if (version > 0) {
      updateHeight = unpacker.unpackLong();
    }
  }


  /**
   * Verify if the address has been granted the permission to execute the specified transaction. All transactions are denied if the address does not have the
   * AP_TX_LIST permission.
   *
   * @param txID the transaction
   *
   * @return true if the address has been granted this transaction
   */
  public boolean canUseTx(TxType txID) {
    return txID != null && (addressPermissions & AP_TX_LIST) != 0 && authorisedTx.contains(txID);
  }


  @Override
  public AddressEntry copy() {
    return new AddressEntry(this);
  }


  @Override
  @Deprecated(since = "28-11-2019")
  public Object[] encode(long index) {

    Object[] rVal;

    if ((authorisedTx == null)
        && (addressMetadata == null)
        && (addressPermissions == 0L)
        && (updateTime == null)
        && (updateHeight < 0L)
    ) {
      rVal = new Object[]{
          Math.max(0, index),
          address,
          new Object[]{
              (classBalance == null ? null : new MPWrappedMap<>(classBalance)),
              (nonceUnset ? null : new Object[]{nonce, highPriNonce, lowPriNonce})
          }
      };
    } else {
      rVal = new Object[]{
          Math.max(0, index),
          address,
          new Object[]{
              (classBalance == null ? null : new MPWrappedMap<>(classBalance)),
              (nonceUnset ? null : new Object[]{nonce, highPriNonce, lowPriNonce}),
              addressPermissions,
              authorisedTx,
              addressMetadata,
              updateTime,
              updateHeight
          }
      };
    }

    return rVal;
  }


  /**
   * equals().
   *
   * @param toCompare :
   *
   * @return :
   */
  @Override
  public boolean equals(Object toCompare) {

    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof AddressEntry)) {
      return false;
    }

    AddressEntry theOther = (AddressEntry) toCompare;

    if (!address.equals(theOther.address)) {
      return false;
    }

    if (nonce != theOther.nonce) {
      return false;
    }

    if (highPriNonce != theOther.highPriNonce) {
      return false;
    }

    if (lowPriNonce != theOther.lowPriNonce) {
      return false;
    }

    if (nonceUnset != theOther.nonceUnset) {
      return false;
    }

    if (classBalance == null) {
      if (theOther.classBalance != null) {
        return false;
      }
    } else {
      if ((theOther.classBalance == null) || (!classBalance.equals(theOther.classBalance))) {
        return false;
      }
    }

    if (addressPermissions != theOther.addressPermissions) {
      return false;
    }

    if (addressMetadata == null) {
      if (theOther.addressMetadata != null) {
        return false;
      }
    } else {
      if (!addressMetadata.equals(theOther.addressMetadata)) {
        return false;
      }
    }

    if (updateTime == null) {
      return theOther.updateTime == null;
    } else {
      return updateTime.equals(theOther.updateTime);
    }
  }


  @Nonnull
  public String getAddress() {
    return getKey();
  }


  /**
   * getAddressMetadata.
   * <p>addressMetadata getter.</p>
   *
   * @return : (String) Address Metadata
   */
  public String getAddressMetadata() {
    return addressMetadata;
  }


  /**
   * getAddressPermissions().
   * <p>Return Address permissions bitmap, including any System Default permissions.</p>
   *
   * @return :
   */
  @SuppressFBWarnings("INT_VACUOUS_BIT_OPERATION") // Although ORing with 0 is pointless, we want to record that this method includes the default permission.
  public long getAddressPermissions() {
    return addressPermissions | AP_DEFAULT_AUTHORISE_BY_ADDRESS;
  }


  /**
   * getAssetBalance.
   *
   * @param fullAssetID :
   *
   * @return :
   */
  @Nonnull
  public Balance getAssetBalance(String fullAssetID) {
    if (classBalance != null) {
      return classBalance.getOrDefault(fullAssetID, Balance.BALANCE_ZERO);
    }
    return Balance.BALANCE_ZERO;
  }


  /**
   * Get the transaction type that this address has been explicitly authorised to use.
   *
   * <p>This set of authorisations is only applicable if the address also has the AP_TX_LIST permission and state is configured to use address level
   * transaction authorisation.
   *
   * @return the set of transactions types
   */
  public Set<TxType> getAuthorisedTx() {
    return authorisedTx != null ? Collections.unmodifiableSet(EnumSet.copyOf(authorisedTx)) : null;
  }


  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  @JsonProperty("balance")
  @Nullable
  public Map<String, Balance> getClassBalance() {
    if (classBalance != null) {
      return Collections.unmodifiableMap(classBalance);
    }
    return null;
  }


  public long getHighPriorityNonce() {
    return highPriNonce;
  }


  @Nonnull
  @Override
  @JsonIgnore
  public String getKey() {
    return address;
  }


  public long getLowPriorityNonce() {
    return lowPriNonce;
  }


  public long getNonce() {
    return nonce;
  }


  /**
   * getUpdateTime.
   * <p>Address Update time getter. In more recent B/C versions, the address update time is updated as part of the Nonce increment. This
   * is done so that Address Entries (if empty) may be deleted after a period of inactivity.
   * </p>
   *
   * @return : (String) Address Time Stamp
   */
  @Nullable
  public Long getUpdateTime() {
    return updateTime;
  }


  @Override
  public int hashCode() {
    return address.hashCode();
  }


  @JsonIgnore
  public boolean isNonceUnset() {
    return nonceUnset;
  }


  /**
   * makeClassBalance(). Utility method to create standard Address Balance map.
   *
   * @return :
   */
  @Nonnull
  public Map<String, Balance> makeClassBalance() {
    classBalance = new TreeMap<>();
    return classBalance;
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    p.packByte((byte) 1); // The encoding version

    // Flags indicating which fields need special treatment
    int flag = (addressMetadata == null ? 0 : 1) | (classBalance == null ? 0 : 2) | (nonceUnset ? 0 : 4) | (authorisedTx == null ? 0 : 8)
        | (updateTime == null ? 0 : 16);
    p.packInt(flag);

    p.packString(address);
    if (addressMetadata != null) {
      p.packString(addressMetadata);
    }
    p.packLong(addressPermissions);

    if (classBalance != null) {
      // NB: output order will be sorted, because the iterator over the entry set of a tree map is sorted.
      p.packMapHeader(classBalance.size());
      for (Entry<String, Balance> e : classBalance.entrySet()) {
        p.packString(e.getKey());
        e.getValue().pack(p);
      }
    }

    if (!nonceUnset) {
      p.packLong(nonce);
      p.packLong(lowPriNonce);
      p.packLong(highPriNonce);
    }

    if (authorisedTx != null) {
      // NB: The array is sorted and distinct on load.
      p.packArrayHeader(authorisedTx.size());
      for (TxType tx : authorisedTx) {
        p.packInt(tx.getId());
      }
    }

    if (updateTime != null) {
      p.packLong(updateTime);
    }

    p.packLong(updateHeight);
  }


  /**
   * setAddressMetadata(). Address Metadata setter.
   *
   * @param addressMetadata :
   */
  public void setAddressMetadata(String addressMetadata) {
    this.addressMetadata = addressMetadata;
  }


  public void setAddressPermissions(long addressPermissions) {
    this.addressPermissions = addressPermissions;
  }


  /**
   * Set the amount of an asset held by this address.
   *
   * @param fullAssetID the asset's full ID
   * @param newBalance  the new balance
   */
  public void setAssetBalance(@Nonnull String fullAssetID, @Nonnull Balance newBalance) {
    if (classBalance == null) {
      makeClassBalance();
    }
    if (newBalance.equalZero()) {
      classBalance.remove(fullAssetID);
    } else {
      classBalance.put(fullAssetID, newBalance);
    }
  }


  public void setAuthorisedTx(Set<TxType> newAuthorisations) {
    if (newAuthorisations != null) {
      authorisedTx = EnumSet.copyOf(newAuthorisations);
    } else {
      authorisedTx = null;
    }
  }


  @JsonProperty("updateHeight")
  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }


  public void setHighPriorityNonce(long nonce) {
    highPriNonce = Long.max(nonce, highPriNonce);
  }


  public void setLowPriorityNonce(long nonce) {
    lowPriNonce = Long.max(nonce, lowPriNonce);
  }


  /**
   * setNonce.
   * <p>Set TX Nonce of an AddressEntry Object</p>
   *
   * @param nonce :
   */
  public void setNonce(long nonce) {
    nonceUnset = false;
    this.nonce = Long.max(nonce, this.nonce);
  }


  /**
   * setUpdateTime(). Update Time setter.
   *
   * @param updateTime :
   */
  public void setUpdateTime(Long updateTime) {
    if (this.updateTime == null) {
      this.updateTime = updateTime;
    } else {
      if (updateTime != null) {
        this.updateTime = Long.max(this.updateTime, updateTime);
      }
    }
  }


  /**
   * Create a JSON representation of this.
   *
   * @return JSON representation of this
   */
  public JSONObject toJSON() {
    // This must include all data returned by the "encode" method.
    JSONObject jsonObject = new JSONObject(true);
    jsonObject.put("address", address);
    jsonObject.put("balances", classBalance);
    if (nonceUnset) {
      jsonObject.put("nonce", null);
      jsonObject.put("noncelowpriority", null);
      jsonObject.put("noncehighpriority", null);
    } else {
      jsonObject.put("nonce", nonce);
      jsonObject.put("noncelowpriority", lowPriNonce);
      jsonObject.put("noncehighpriority", highPriNonce);
    }
    jsonObject.put("addresspermissions", addressPermissions);
    jsonObject.put("permissionedtxs", authorisedTx);
    jsonObject.put("addressmetadata", addressMetadata);
    jsonObject.put("updatetime", updateTime);

    return jsonObject;
  }


  @Override
  public String toString() {
    return String.format("address:%s nonce:%d classBalance:%s", address, nonce,
        classBalance
    );
  }

}
