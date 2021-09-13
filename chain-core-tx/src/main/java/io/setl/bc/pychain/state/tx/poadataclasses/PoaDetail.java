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
package io.setl.bc.pychain.state.tx.poadataclasses;

import static io.setl.common.Balance.BALANCE_ZERO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PoaDetail {
  
  /*
  Reference,
  Issuer Address,
  Attorney Address,
  Start Date,
  End Date,
  [[
    txType,
    amount,
    [assetid, ...]
   ], ...
  ]
  
   */


  private String attorneyAddress;

  private long endTime = 0L;

  private String issuerAddress;

  private ArrayList<PoaItem> poaItems;

  private String reference;

  private long startTime = 0L;


  /**
   * Decode() Constructor.
   *
   * @param merkleItem :
   */
  public PoaDetail(MPWrappedArray merkleItem) {
    if ((merkleItem != null) && (merkleItem.size() >= 6)) {
      reference = merkleItem.asString(0);
      issuerAddress = merkleItem.asString(1);
      attorneyAddress = merkleItem.asString(2);
      startTime = merkleItem.asLong(3);
      endTime = merkleItem.asLong(4);

      MPWrappedArray theseItems = merkleItem.asWrapped(5);

      poaItems = new ArrayList<>();

      if (theseItems != null) {
        for (int index = 0, l = theseItems.size(); index < l; index++) {
          if (theseItems.asObjectArray(index) != null) {
            poaItems.add(new PoaItem(theseItems.asObjectArray(index)));
          }
        }
      }
    }
  }


  /**
   * PoaDetail Copy Constructor.
   *
   * @param toCopy :
   */
  public PoaDetail(PoaDetail toCopy) {
    if (toCopy != null) {
      reference = toCopy.reference;
      issuerAddress = toCopy.issuerAddress;
      attorneyAddress = toCopy.attorneyAddress;
      startTime = toCopy.startTime;
      endTime = toCopy.endTime;

      poaItems = null;
      if (toCopy.poaItems != null) {
        poaItems = new ArrayList<>();

        toCopy.poaItems.forEach(thisItem -> {
          if (thisItem != null) {
            poaItems.add(new PoaItem(thisItem));
          }
        });
      }
    }
  }


  /**
   * PoaDetail constructor.
   *
   * @param reference       : Unique PoA reference (per issuer address)
   * @param issuerAddress   : Issuer Address. i.e. Address granting PoA.
   * @param attorneyAddress : Address being granted permissions.
   * @param startTime       : Effective start time.
   * @param endTime         : Effective end time.
   * @param items           : PoaItem[], Array of PoaItems detailing permissions granted.
   */
  public PoaDetail(String reference, String issuerAddress, String attorneyAddress, long startTime, long endTime, PoaItem[] items) {
    this.reference = reference;
    this.issuerAddress = issuerAddress;
    this.attorneyAddress = attorneyAddress;
    this.startTime = startTime;
    this.endTime = endTime;
    this.poaItems = new ArrayList<>();

    if (items != null) {
      for (PoaItem thisItem : items) {
        if (thisItem != null) {
          this.poaItems.add(new PoaItem(thisItem));
        }
      }
    }
  }


  /**
   * PoaDetail constructor.
   *
   * @param reference       : Unique PoA reference (per issuer address)
   * @param issuerAddress   : Issuer Address. i.e. Address granting PoA.
   * @param attorneyAddress : Address being granted permissions.
   * @param startTime       : Effective start time.
   * @param endTime         : Effective end time.
   * @param items           : List[PoaItem], List of PoaItems detailing permissions granted.
   */
  @JsonCreator
  public PoaDetail(
      @JsonProperty("reference")
          String reference,
      @JsonProperty("issuer")
          String issuerAddress,
      @JsonProperty("attorney")
          String attorneyAddress,
      @JsonProperty("startTime")
          long startTime,
      @JsonProperty("endTime")
          long endTime,
      @JsonProperty("items")
          List<PoaItem> items) {
    this.reference = reference;
    this.issuerAddress = issuerAddress;
    this.attorneyAddress = attorneyAddress;
    this.startTime = startTime;
    this.endTime = endTime;
    this.poaItems = new ArrayList<>();

    if (items != null) {
      for (PoaItem thisItem : items) {
        if (thisItem != null) {
          this.poaItems.add(new PoaItem(thisItem));
        }
      }
    }
  }


  /**
   * encode().
   *
   * @return :
   */
  public Object[] encode() {
    Object[] rval = new Object[6];

    rval[0] = reference;
    rval[1] = issuerAddress;
    rval[2] = attorneyAddress;
    rval[3] = startTime;
    rval[4] = endTime;
    rval[5] = null;

    if (poaItems != null) {
      Object[] items = new Object[poaItems.size()];

      for (int index = 0, l = poaItems.size(); index < l; index++) {
        items[index] = poaItems.get(index).encode();
      }

      rval[5] = items;
    }

    return rval;
  }


  /**
   * equals().
   * <p>Deep equality comparator.</p>
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

    if (!(toCompare instanceof PoaDetail)) {
      return false;
    }

    PoaDetail theOther = (PoaDetail) toCompare;

    if (!Objects.equals(reference, theOther.reference)) {
      return false;
    }

    if (!Objects.equals(issuerAddress, theOther.issuerAddress)) {
      return false;
    }

    if (!Objects.equals(attorneyAddress, theOther.attorneyAddress)) {
      return false;
    }

    if (this.startTime != theOther.startTime) {
      return false;
    }

    if (this.endTime != theOther.endTime) {
      return false;
    }

    return Objects.equals(poaItems,theOther.poaItems);
  }


  @JsonProperty("attorney")
  @Schema(name = "attorney", description = "Address of the attorney who can use this PoA.", required = true)
  public String getAttorneyAddress() {
    return attorneyAddress;
  }


  @JsonProperty("endTime")
  @Schema(name = "endTime", description = "The time at which this PoA expires (seconds since the epoch)", required = true)
  public long getEndTime() {
    return endTime;
  }


  @JsonProperty("issuer")
  @Schema(name = "issuer", description = "The address that issued this PoA.", required = true)
  public String getIssuerAddress() {
    return issuerAddress;
  }


  /**
   * getItem.
   * <p>Return poa permission item matching given TxId, or null.</p>
   *
   * @param txID :
   *
   * @return :
   */
  public List<PoaItem> getItem(TxType txID) {
    if ((this.poaItems != null) && (!this.poaItems.isEmpty())) {

      ArrayList<PoaItem> rVal = new ArrayList<>(this.poaItems.size());

      for (PoaItem thisItem : poaItems) {
        if (thisItem.txType == txID) {
          rVal.add(thisItem);
        }
      }

      if (!rVal.isEmpty()) {
        return rVal;
      }
    }

    return null;
  }


  @JsonProperty("items")
  @Schema(name = "items", description = "The items that constitute this PoA.", required = true)
  public List<PoaItem> getItems() {
    if (this.poaItems == null) {
      return Collections.emptyList();
    }

    return this.poaItems.stream().map(PoaItem::new).collect(Collectors.toList());
  }


  @JsonProperty("reference")
  @Schema(name = "reference", description = "The unique reference for this PoA as assigned by the issuer", required = true)
  public String getReference() {
    return reference;
  }


  @JsonProperty("startTime")
  @Schema(name = "startTime", description = "The time after which this PoA can be used (seconds since the epoch).", required = true)
  public long getStartTime() {
    return startTime;
  }


  /**
   * getSumAmounts().
   * <p>Return sum of PoaItem Amounts. Used to determine that a POA is fully consumed.</p>
   *
   * @return :
   */
  @Schema(hidden = true)
  @JsonIgnore
  public Balance getSumAmounts() {

    Balance rVal = BALANCE_ZERO;

    for (PoaItem thisItem : poaItems) {
      if (thisItem != null) {
        rVal = rVal.add(thisItem.amount.compareTo(BigInteger.ZERO) > 0 ? thisItem.amount : 0);
      }
    }

    return rVal;
  }


  @Override
  public int hashCode() {

    if (reference != null) {
      return reference.hashCode();
    }
    if (issuerAddress != null) {
      return issuerAddress.hashCode();
    }
    return 0;
  }

}
