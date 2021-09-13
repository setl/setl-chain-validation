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
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nullable;

public class PoaEntry implements MEntry {

  /*

  A POA Entry may be either a 'Header' entry or a 'Detail' entry.

  There will be a 'Header' entry recorded against an Address and any number of 'Detail' entries each of which holds
  the details of a particular POA. The key of a 'Detail' entry is <Address>|<Reference> so that the Reference need
  only be unique by Address.

  The Header entry maintains a map of POAs that relate to this Address with effective Start and End dates. The header
  entry exists for two purposes :

    1) To quickly establish if a POA exists, though individually it would be just as quick to look it up, if multiple POAs
       are used from a single address, there will be caching benefits.
    2) To record what POAs exist against a particular address. If a Header is not maintained, then the only way to know what
       POAs exist would be to iterate the entire POA tree : Not a good thing !

  A 'Detail' POA Entry contains an instance of the PoaDetail class (state.tx.poadataclasses) which indicates to whom a POA
  has been granted and what Start / End times would apply, together with a list of PoaItem objects each of which which individually
  describe a permission being granted, that is, a TX Type, Amount and Identifier (Namespace, Class, AsstID). Note that an
  individual POA may specify multiple items, for example if you wish to grant Issue and Transfer permissions then this is
  two distinct items.
  The POA Item class contains methods for consuming POAs and UpdateStateUtils contains methods for checking permissions and
  tidying entries.

   */
  public static class Decoder implements EntryDecoder<PoaEntry> {

    /**
     * decode().
     *
     * @param merkleItem :
     *
     * @return :
     */
    @Override
    public PoaEntry decode(MPWrappedArray merkleItem) {
      return new PoaEntry(merkleItem);

    }
  }



  protected MPWrappedArray addressPoasArray;

  long index;

  String itemKey;

  long updateHeight = -1;

  private PoaDetail poaDetail;

  private SortedMap<String, PoaHeader> poaMap;


  /**
   * PoaEntry Basic Constructor.
   *
   * @param index   :
   * @param itemKey :
   */
  public PoaEntry(long index, String itemKey) {
    this.index = index;
    this.itemKey = itemKey;
  }


  /**
   * New instance.
   * @param key the POA's identifier
   * @param detail the POA's detail structure, if any
   * @param headers the POA's headers, if any
   */
  @JsonCreator
  public PoaEntry(
      @JsonProperty("key") String key,
      @JsonProperty("detail") PoaDetail detail,
      @JsonProperty("headers") Collection<PoaHeader> headers
  ) {
    index = 0;
    itemKey = key;
    poaDetail = detail;
    if (headers != null) {
      poaMap = new TreeMap<>();
      for (PoaHeader h : headers) {
        poaMap.put(h.reference, h);
      }
    }
  }


  /**
   * PoaEntry de-Serialise constructor.
   *
   * @param merkleItem : MPWrappedArray [index, keyName, [PoaMap], [PoaDetail]]
   */
  public PoaEntry(MPWrappedArray merkleItem) {
    addressPoasArray = null;
    poaMap = null;
    poaDetail = null;
    itemKey = "";

    if (merkleItem == null) {
      return;
    }

    int thisSize = merkleItem.size();

    if (thisSize >= 2) {
      this.index = merkleItem.asLong(0);
      this.itemKey = merkleItem.asString(1);
    }

    if (thisSize >= 4) {
      Object thisPoaMap = merkleItem.get(2);
      Object thisPoaDetail = merkleItem.get(3);

      if (thisPoaMap != null) {
        this.addressPoasArray = new MPWrappedArrayImpl((Object[]) thisPoaMap);
      }
      if (thisPoaDetail != null) {
        this.poaDetail = new PoaDetail(new MPWrappedArrayImpl((Object[]) thisPoaDetail));
      }
    }

    if (thisSize >= 5) {
      this.updateHeight = merkleItem.asLong(4);
    }

  }


  /**
   * PoaEntry, Copy Constructor.
   *
   * @param toCopy :
   */
  public PoaEntry(PoaEntry toCopy) {
    if (toCopy == null) {
      this.index = -1;
      this.itemKey = "";

      return;
    }

    updateHeight = toCopy.updateHeight;
    index = toCopy.index;
    itemKey = toCopy.itemKey;
    addressPoasArray = toCopy.addressPoasArray;
    poaMap = null;
    if (toCopy.poaMap != null) {
      poaMap = new TreeMap<>();
      toCopy.poaMap.forEach((key, value) -> poaMap.put(key, new PoaHeader(value)));
    }

    poaDetail = null;
    if (toCopy.poaDetail != null) {
      poaDetail = new PoaDetail(toCopy.poaDetail);
    }
  }


  @Override
  public PoaEntry copy() {
    return new PoaEntry(this);
  }


  @Deprecated
  @Override
  public Object[] encode(long index) {
    Object[] rVal;

    // Encode updateHeight if it is set.
    if (this.updateHeight < 0L) {
      rVal = new Object[4];
    } else {
      rVal = new Object[5];
      rVal[4] = this.updateHeight;
    }

    rVal[0] = index;
    rVal[1] = itemKey;

    if (poaMap != null) {
      rVal[2] = poaMapEncode(poaMap);
    } else {
      rVal[2] = addressPoasArray;
    }

    if (poaDetail != null) {
      rVal[3] = poaDetail.encode();
    } else {
      rVal[3] = null;
    }

    return rVal;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PoaEntry poaEntry = (PoaEntry) o;
    return index == poaEntry.index
        && Objects.equals(itemKey, poaEntry.itemKey)
        && Objects.equals(poaDetail, poaEntry.poaDetail)
        && Objects.equals(getPoaMap(), poaEntry.getPoaMap());
  }


  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  public String getFullReference(String thisRef) {
    return itemKey + "|" + thisRef;
  }


  @JsonProperty("headers")
  public Collection<PoaHeader> getHeaders() {
    return Collections.unmodifiableCollection(getPoaMap().values());
  }


  @Override
  public String getKey() {
    return itemKey;
  }


  @JsonProperty("detail")
  public PoaDetail getPoaDetail() {
    return poaDetail;
  }


  private SortedMap<String, PoaHeader> getPoaMap() {
    if (poaMap == null) {
      poaMap = new TreeMap<>();

      if (addressPoasArray != null) {
        for (int mapIndex = 0, l = addressPoasArray.size(); mapIndex < l; mapIndex++) {
          Object[] thisPoa = addressPoasArray.asObjectArray(mapIndex);

          if (thisPoa.length == 2) {
            poaMap.put((String) (thisPoa[0]), new PoaHeader((Object[]) (thisPoa[1])));
          }
        }

        addressPoasArray = null;
      }
    }

    return poaMap;
  }


  /**
   * getReference().
   * <p>Return PoaHeader relating to given PoA Reference.</p>
   * <p>A PoaEntry entry may represent EITHER the address 'header' record or a PoaDetail record.
   * If poaDetail is set then this is not a Header record and References all return null.</p>
   *
   * @param checkRef :
   *
   * @return :
   */
  public PoaHeader getReference(String checkRef) {
    if ((checkRef != null) && (poaDetail == null)) {
      if (poaMap == null) {
        getPoaMap();
      }

      return (poaMap.get(checkRef));
    }

    return null;
  }


  /**
   * getReferenceCount.
   *
   * @return :
   */
  @JsonIgnore
  public int getReferenceCount() {
    if (poaDetail != null) {
      return 0;
    }

    if (poaMap == null) {
      getPoaMap();
    }
    return poaMap.size();
  }


  /**
   * hasReference.
   * <p>If this entry is an Address entry, check that the given reference is already used.</p>
   *
   * @param checkRef :
   *
   * @return :
   */
  public boolean hasReference(String checkRef) {
    if ((checkRef != null) && (poaDetail == null)) {
      if ((poaMap == null) && (addressPoasArray != null)) {
        getPoaMap();
      }

      if (poaMap != null) {
        return (poaMap.containsKey(checkRef));
      }
    }

    return false;
  }


  @Override
  public int hashCode() {
    return Objects.hash(index, itemKey, poaDetail, getPoaMap());
  }


  @Nullable
  private Object[] poaMapEncode(SortedMap<String, PoaHeader> thisPoaMap) {
    if (thisPoaMap == null) {
      return null;
    }

    int mapSize = thisPoaMap.size();
    Object[] rVal = new Object[mapSize];

    if (mapSize > 0) {
      final int[] mapIndex = {0};

      thisPoaMap.forEach((String reference, PoaHeader thisHeader) -> rVal[mapIndex[0]++] = new Object[]{reference, thisHeader.encode()});
    }

    return rVal;
  }


  /**
   * removeReference().
   * <p>Remove PoaHeader relating to given PoA Reference, return true if the reference exists.</p>
   * <p>A PoaEntry entry may represent EITHER the address 'header' record or a PoaDetail record.
   * If poaDetail is set or this reference does not exist then return false.</p>
   *
   * @param checkRef :
   *
   * @return : boolean, Success.
   */
  public boolean removeReference(String checkRef) {
    if ((checkRef != null) && (poaDetail == null)) {
      if (poaMap == null) {
        getPoaMap();
      }

      poaMap.remove(checkRef);
      return true;
    }

    return false;
  }


  @JsonProperty("updateHeight")
  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }


  /**
   * setDetail().
   * <p>For 'Detail' Entries, set PoaDetails.</p>
   *
   * @param reference       :
   * @param issuerAddress   :
   * @param attorneyAddress :
   * @param startTime       :
   * @param endTime         :
   * @param items           :
   */
  public void setDetail(String reference, String issuerAddress, String attorneyAddress, long startTime, long endTime, PoaItem[] items) {
    this.poaDetail = new PoaDetail(reference, issuerAddress, attorneyAddress, startTime, endTime, items);

  }


  /**
   * setDetail().
   * <p>For 'Detail' Entries, set PoaDetails.</p>
   *
   * @param reference       :
   * @param issuerAddress   :
   * @param attorneyAddress :
   * @param startTime       :
   * @param endTime         :
   * @param items           :
   */
  public void setDetail(String reference, String issuerAddress, String attorneyAddress, long startTime, long endTime, List<PoaItem> items) {

    this.poaDetail = new PoaDetail(reference, issuerAddress, attorneyAddress, startTime, endTime, items);

  }


  /**
   * setReference().
   * <p>Set PoaHeader with given Reference and effective dates.</p>
   *
   * @param thisRef   :
   * @param startDate :
   * @param endDate   :
   */
  public PoaHeader setReference(String thisRef, long startDate, long endDate) {
    if ((thisRef != null) && (poaDetail == null)) {
      if (poaMap == null) {
        if (addressPoasArray != null) {
          getPoaMap();
        } else {
          poaMap = new TreeMap<>();
        }
      }

      poaMap.put(thisRef, new PoaHeader(thisRef, "", startDate, endDate));
      return poaMap.get(thisRef);
    }

    return null;
  }
}
