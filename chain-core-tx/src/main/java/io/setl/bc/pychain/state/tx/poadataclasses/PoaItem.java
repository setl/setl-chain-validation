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
import static io.setl.common.StringUtils.matchString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Simon Greatrix on 25/09/2019.
 */
@Schema(description = "Part of a PoA authorisation which describes how much of some assets may be manipulated by which transactions.")
public class PoaItem {

  /**
   * verifyItemData().
   * <p>Verify that the format of the given Object[] represents a valid permissionItems data set.</p>
   *
   * @param data : Object[] : [(Int)TxType, (Number)Amount, [(String)AssetID, ...]]
   *
   * @return : boolean success.
   */
  public static boolean verifyItemData(Object[] data) {

    try {
      if ((data == null) || (data.length != 3)) {
        return false;
      }

      if (!(data[0] instanceof Integer)) {
        return false;
      }

      if (!(data[1] instanceof Number)) {
        return false;
      }

      if ((data[2] != null) && (data[2] instanceof Object[])) {
        Object[] theseAssets = (Object[]) (data[2]);

        for (Object assetID : theseAssets) {
          if ((assetID == null) || (!(assetID instanceof String))) {
            return false;
          }
        }

      } else {
        return false;
      }

    } catch (RuntimeException e) {
      return false;
    }

    return true;
  }


  @Schema(name = "amount", description = "The amount of asset covered by this PoA item.", required = true, type = "integer")
  protected Balance amount;

  @Schema(name = "assets", description = "The assets that may be manipulated via this PoA item.", required = true)
  protected Set<String> assets;

  @Schema(name = "txType", description = "The transaction type covered by this PoA item.", required = true)
  protected TxType txType;


  /**
   * Copy Constructor().
   *
   * @param toCopy :
   */
  public PoaItem(PoaItem toCopy) {

    txType = toCopy.txType;
    amount = toCopy.amount;

    if (toCopy.assets == null) {
      assets = null;
    } else {
      assets = new TreeSet<>(toCopy.assets);
    }
  }


  /**
   * Decode constructor().
   *
   * @param data ; Object[] - [txType, amount, [Asset ID, ...]]
   */
  public PoaItem(Object[] data) {
    if ((data != null) && (data.length == 3)) {
      txType = TxType.get(((Number) data[0]).intValue());
      amount = new Balance((Number) data[1]);
      assets = new TreeSet<>();

      if (data[2] instanceof Object[]) {
        Object[] theseAssets = (Object[]) (data[2]);
        assets = new TreeSet<>();
        for (Object assetID : theseAssets) {
          assets.add(assetID instanceof String ? (String) assetID : "");
        }
      }
    }
  }


  /**
   * Create a new PoaItem from its constituent parts.
   *
   * @param txType the transaction type
   * @param amount the amount
   * @param assets the assets involved.
   */
  @JsonCreator
  public PoaItem(
      @JsonProperty("txType")
          TxType txType,
      @JsonProperty("amount")
          Number amount,
      @JsonProperty("assets")
          Collection<String> assets) {
    this.txType = txType;
    this.amount = new Balance(amount);
    this.assets = new TreeSet<>(assets);
  }


  /**
   * consume()
   * <p>Consume a quantity of a POA Item.</p>
   *
   * @param toUse :
   *
   * @return :
   */
  public Balance consume(Number toUse) {
    if (toUse instanceof Balance) {
      return consume(((Balance) toUse).getValue());
    }

    boolean positive = false;

    if (toUse instanceof BigInteger) {
      if (((BigInteger) toUse).compareTo(BigInteger.ZERO) > 0) {
        positive = true;
      }
    } else if (toUse instanceof BigDecimal) {
      if (((BigDecimal) toUse).compareTo(BigDecimal.ZERO) > 0) {
        positive = true;
      }
    } else if (toUse.longValue() > 0) {
      positive = true;
    }

    if (positive) {
      // if (toUse > this.amount) {
      if (this.amount.compareTo(toUse) <= 0) {
        this.amount = BALANCE_ZERO;
      } else {
        this.amount = this.amount.subtract(toUse);
      }
    }

    return this.amount;
  }


  public boolean consumed() {
    return (this.amount.compareTo(0L) == 0);
  }


  /**
   * encode().
   * <p>Serialise PoaItem.</p>
   *
   * @return : Object[].
   */
  public Object[] encode() {
    return new Object[]{txType.getId(), amount.getValue(), assets.toArray()};
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

    if (!(toCompare instanceof PoaItem)) {
      return false;
    }

    PoaItem theOther = (PoaItem) toCompare;

    if (this.txType != theOther.txType) {
      return false;
    }

    if (this.amount.compareTo(theOther.amount) != 0) {
      return false;
    }

    return Objects.equals(assets, theOther.assets);
  }


  @JsonProperty("amount")
  public Balance getAmount() {
    return amount;
  }


  /**
   * getAssets.
   * <p>Return shallow copy of assets listed in this poa permission item.</p>
   *
   * @return :
   */
  @JsonProperty("assets")
  public Set<String> getAssets() {
    return (assets == null ? null : Collections.unmodifiableSet(assets));
  }


  @JsonProperty("txType")
  public TxType getTxType() {
    return txType;
  }


  @Override
  public int hashCode() {
    int hc = txType.getId();
    hc = hc * 31 + amount.intValue();
    hc = hc * 31 + assets.hashCode();
    return hc;
  }


  /**
   * matchAsset.
   * <p>Return true if any asset listed in this poaItem matches the given string.</p>
   *
   * @param toMatch :
   *
   * @return :
   */
  public boolean matchAsset(String toMatch) {
    if ((toMatch == null) || (this.assets == null)) {
      return false;
    }

    for (String thisAsset : this.assets) {
      if ((thisAsset != null) && matchString(toMatch, thisAsset)) {
        return true;
      }
    }

    return false;
  }
}
