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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.NewContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * ContractEntry().
 * Class to represent a Contract-Entry data leaf.
 * <p>
 * Uses the IContractData interface and specific implementations such as DvpUkContractData to represent specific flavours
 * of contract data.
 * </p>
 */
public class ContractEntry implements MEntry {

  public static class Decoder implements EntryDecoder<ContractEntry> {

    /**
     * decode.
     * Decode MsgPack item array to ContractEntry instance.
     *
     * @param merkleItem : MsgPack Data
     *
     * @return : ContractEntry instance
     */
    @Override
    public ContractEntry decode(MPWrappedArray merkleItem) {
      return new ContractEntry(merkleItem);
    }
  }



  Map<String, Object> dictionary;

  private String contractAddress;

  private IContractData contractData;

  private Long index;

  private long updateHeight = -1;


  protected ContractEntry() {
    index = -1L;
    contractAddress = "";
    dictionary = new TreeMap<>();
  }


  /**
   * ContractEntry().
   *
   * @param contractAddress :
   * @param asMapValue      :
   */
  public ContractEntry(Long index, String contractAddress, MPWrappedMap<String, Object> asMapValue) {
    this();

    this.index = index;
    this.contractAddress = contractAddress;

    if (asMapValue != null) {
      asMapValue.iterate((k, v) -> dictionary.put(k, v));
    }
  }


  public ContractEntry(String contractAddress, MPWrappedMap<String, Object> asMapValue) {
    this(-1L, contractAddress, asMapValue);
  }


  @JsonCreator
  public ContractEntry(
      @JsonProperty("address") String contractAddress,
      @JsonProperty("contract") Map<String, Object> asMapValue
  ) {
    this(-1L, contractAddress, new MPWrappedMap<>(asMapValue));
  }


  public ContractEntry(Object[] merkleItem) {
    this(new MPWrappedArrayImpl(merkleItem));
  }


  /**
   * ContractEntry().
   * ContractEntry constructor (MPWrappedArray)
   *
   * @param merkleItem : MPWrappedArray (as read from state).
   */
  public ContractEntry(MPWrappedArray merkleItem) {
    this();

    if ((merkleItem != null) && (merkleItem.size() > 2)) {
      this.index = merkleItem.asLong(0);
      this.contractAddress = merkleItem.asString(1);

      if (merkleItem.get(2) != null) {
        merkleItem.asWrapped(2).<String, Object>asWrappedMap(0).iterate((k, v) -> dictionary.put(k, v));
      }

      if (merkleItem.size() > 3) {
        this.updateHeight = merkleItem.asLong(3);
      }

    }
  }


  /**
   * Create a copy of an existing contract entry.
   *
   * @param toCopy the instance to copy
   */
  public ContractEntry(ContractEntry toCopy) {

    // TODO Lazy?
    this(toCopy == null ? new Object[]{} : toCopy.encode(toCopy.index));

  }


  @Override
  public ContractEntry copy() {
    return new ContractEntry(this);
  }


  @Override
  public Object[] encode(long index) {

    ContractEntry a = this;
    Object[] r;
    // Need to wrap any treemaps to MPWrappedMap, so that decode works when serialised with Java
    // serialisation - eg H2 database

    MPWrappedMap<String, Object> encodedData;

    if (contractData != null) {
      encodedData = contractData.encode();
    } else {
      // If this file was read from a saved Python state, the saved data may well not be sorted. If it was written by Java, then it will be.

      if ((dictionary != null) && (dictionary.containsKey("__function"))) {
        encodedData = getContractData().encode();
      } else {
        encodedData = new MPWrappedMap<>(dictionary != null ? dictionary : Collections.emptyMap());
      }
    }

    if (this.updateHeight < 0) {
      r = new Object[]{
          (index < 0 ? this.index : index),
          a.contractAddress,
          new Object[]{encodedData, null}
      };
    } else {
      r = new Object[]{
          (index < 0 ? this.index : index),
          a.contractAddress,
          new Object[]{encodedData, null},
          this.updateHeight
      };
    }

    return r;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContractEntry that = (ContractEntry) o;
    return Objects.equals(index, that.index)
        && Objects.equals(contractAddress, that.contractAddress)
        && Objects.equals(getContractData(), that.getContractData());
  }


  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  @JsonProperty("address")
  public String getContractAddress() {
    return this.getKey();
  }


  /**
   * getContractData().
   * <p>Return Contract Data in Object format.</p>
   *
   * @return : IContractData
   */
  @JsonIgnore
  public IContractData getContractData() {
    if (this.contractData != null) {
      return this.contractData;
    }

    this.contractData = NewContractTx.getContractDataFromDictionary(this.dictionary);

    return this.contractData;
  }


  /**
   * getFunction().
   * <p>Return contract function.</p>
   *
   * @return :
   */
  @JsonIgnore
  public String getFunction() {
    if (this.contractData != null) {
      return this.contractData.get__function();
    }

    return (String) dictionary.getOrDefault("__function", "");
  }


  @JsonIgnore
  @Override
  public String getKey() {
    return contractAddress;
  }


  /**
   * Get the raw contract data. The data is not modifiable.
   *
   * @return the raw contract data
   */
  @JsonProperty("contract")
  public Map<String, Object> getRawContractData() {
    if (contractData != null) {
      return Collections.unmodifiableMap(contractData.encode());
    }
    return Collections.unmodifiableMap(dictionary);
  }


  @Override
  public int hashCode() {
    return Objects.hash(dictionary, contractAddress, index);
  }


  @JsonProperty("updateHeight")
  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }

}
