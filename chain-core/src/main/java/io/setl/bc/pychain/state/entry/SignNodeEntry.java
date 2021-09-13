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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.serialise.SafeMsgPackable;
import io.setl.common.Balance;


/**
 * State entry for a consensus signing node.
 */
public class SignNodeEntry implements MEntry, SafeMsgPackable {

  /**
   * Decoder for sign node entries that were persisted using the encode method.
   */
  public static class Decoder implements EntryDecoder<SignNodeEntry> {

    @Override
    public SignNodeEntry decode(MPWrappedArray va) {

      if (va == null) {
        return null;
      }
      if (va.size() < 3) {
        return null;
      }

      MPWrappedArray sigNodeEntry = va.asWrapped(2);

      if ((sigNodeEntry == null) || (sigNodeEntry.isEmpty())) {
        return null;
      }

      MPWrappedArray sigNodeData = sigNodeEntry.asWrapped(0);
      if (sigNodeData.size() < 6) {
        return null;
      }

      SignNodeEntry rVal;

      // Three extra integers after vaa.asInt(2) - Not Used.

      rVal = new SignNodeEntry(va.asString(1), sigNodeData.asString(1), new Balance(sigNodeData.get(2)), sigNodeData.asInt(3),
          sigNodeData.asInt(4), sigNodeData.asInt(5)
      );

      if (sigNodeEntry.size() > 2) {
        rVal.updateHeight = Math.max(-1L, sigNodeEntry.asLong(2));
      }

      return rVal;
    }

  }

  private final int unused1;

  private final int unused2;

  private Balance balance;

  private String hexPublicKey;

  private String returnAddressWhenUnbonded;

  private long signNodeNonce;

  private long updateHeight = -1;


  /**
   * SignNodeEntry Constructor.
   *
   * @param hexPublicKey              :
   * @param returnAddressWhenUnbonded :
   * @param balance                   :
   * @param signNodeNonce             :
   */
  @JsonCreator
  public SignNodeEntry(
      @JsonProperty("publicKey") String hexPublicKey,
      @JsonProperty("returnAddress") String returnAddressWhenUnbonded,
      @JsonProperty("balance") Number balance,
      @JsonProperty("nonce") long signNodeNonce
  ) {
    this.hexPublicKey = hexPublicKey;
    this.returnAddressWhenUnbonded = returnAddressWhenUnbonded;
    this.balance = new Balance(balance);
    this.signNodeNonce = signNodeNonce;
    unused1 = 0;
    unused2 = 0;
  }


  /**
   * SignNodeEntry Constructor.
   *
   * @param hexPublicKey              :
   * @param returnAddressWhenUnbonded :
   * @param balance                   :
   * @param signNodeNonce             :
   * @param unused1                   :  Unused.
   * @param unused2                   :  Unused.
   */
  public SignNodeEntry(
      String hexPublicKey, String returnAddressWhenUnbonded, Number balance, long signNodeNonce,
      int unused1, int unused2
  ) {
    this.hexPublicKey = hexPublicKey;
    this.returnAddressWhenUnbonded = returnAddressWhenUnbonded;
    this.balance = new Balance(balance);
    this.signNodeNonce = signNodeNonce;
    this.unused1 = unused1;
    this.unused2 = unused2;
  }


  /**
   * SignNodeEntry deserializer.
   *
   * @param unpacker the source of the new SignNodeEntry
   */
  public SignNodeEntry(MessageUnpacker unpacker) throws IOException {
    byte version = unpacker.unpackByte();
    if (version < 0 || version > 1) {
      throw new IllegalArgumentException("Unrecognised encoding version:" + version);
    }
    hexPublicKey = unpacker.unpackString();
    returnAddressWhenUnbonded = unpacker.unpackString();
    balance = new Balance(unpacker);
    signNodeNonce = unpacker.unpackLong();
    unused1 = unpacker.unpackInt();
    unused2 = unpacker.unpackInt();
    if (version >= 1) {
      updateHeight = unpacker.unpackLong();
    }
  }


  /**
   * New instance.
   *
   * @param original instance to copy
   */
  public SignNodeEntry(SignNodeEntry original) {
    balance = original.balance;
    hexPublicKey = original.hexPublicKey;
    returnAddressWhenUnbonded = original.returnAddressWhenUnbonded;
    signNodeNonce = original.signNodeNonce;
    updateHeight = original.updateHeight;

    unused1 = original.unused1;
    unused2 = original.unused2;
  }


  @Override
  public SignNodeEntry copy() {
    return new SignNodeEntry(this);
  }


  /**
   * decrement Signode balance.
   *
   * @param balance :
   */
  public void decrementBalance(Number balance) {
    if ((new Balance(balance)).greaterThanZero()) {
      this.balance = this.balance.subtract(Balance.min(this.balance, new Balance(balance)));
    }
  }


  @Override
  @Deprecated(since = "28-11-2019")
  public Object[] encode(long index) {
    SignNodeEntry a = this;
    if (updateHeight < 0) {

      return new Object[]{
          index,
          a.hexPublicKey,
          new Object[]{
              new Object[]{a.hexPublicKey, a.returnAddressWhenUnbonded, a.balance.getValue(), signNodeNonce, unused1, unused2},
              null
          }
      };
    } else {
      return new Object[]{
          index,
          a.hexPublicKey,
          new Object[]{
              new Object[]{a.hexPublicKey, a.returnAddressWhenUnbonded, a.balance.getValue(), signNodeNonce, unused1, unused2},
              null,
              updateHeight
          }
      };
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SignNodeEntry that = (SignNodeEntry) o;
    return signNodeNonce == that.signNodeNonce
        && Objects.equals(returnAddressWhenUnbonded, that.returnAddressWhenUnbonded)
        && Objects.equals(balance, that.balance)
        && Objects.equals(hexPublicKey, that.hexPublicKey);
  }


  @JsonProperty("balance")
  public Balance getBalance() {
    return balance;
  }


  @JsonProperty("updateHeight")
  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  public String getHexPublicKey() {
    return hexPublicKey;
  }


  @JsonProperty("publicKey")
  @Override
  public String getKey() {
    return getHexPublicKey();
  }


  public long getNonce() {
    return signNodeNonce;
  }


  @JsonProperty("returnAddress")
  public String getReturnAddress() {
    return returnAddressWhenUnbonded;
  }


  @Override
  public int hashCode() {
    return Objects.hash(returnAddressWhenUnbonded, balance, signNodeNonce, hexPublicKey);
  }


  /**
   * Increment Signode balance.
   *
   * @param balance :
   */
  public void incrementBalance(Number balance) {
    if ((new Balance(balance)).greaterThanZero()) {
      this.balance = this.balance.add(balance);
    }
  }


  public void incrementNonce() {
    signNodeNonce += 1;
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    p.packByte((byte) 1); // version
    p.packString(hexPublicKey);
    p.packString(returnAddressWhenUnbonded);
    balance.pack(p);
    p.packLong(signNodeNonce);
    p.packInt(unused1);
    p.packInt(unused2);
    p.packLong(updateHeight);
  }


  public void setBalance(Number balance) {
    this.balance = new Balance(balance);
  }


  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }


  public void setHexPublicKey(String hexPublicKey) {
    this.hexPublicKey = hexPublicKey;
  }


  public void setReturnAddress(String returnAddressWhenUnbonded) {
    this.returnAddressWhenUnbonded = returnAddressWhenUnbonded;
  }

}
