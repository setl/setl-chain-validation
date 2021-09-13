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
package io.setl.bc.pychain.p2p.message;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.P2PType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Simon Greatrix on 2019-03-26.
 */
public class TxPackage implements Message {

  protected final int chainId;

  protected final ArrayList<MPWrappedArray> encodedTxs = new ArrayList<>();

  protected final P2PType type;


  /**
   * New instance from its encoded form.
   *
   * @param message the encoded form
   */
  public TxPackage(MPWrappedArray message) {
    chainId = message.asInt(0);
    this.type = P2PType.get(message.asInt(1));

    Object[] txs = message.asObjectArray(2);
    encodedTxs.ensureCapacity(txs.length);
    for (Object o : txs) {
      if (o instanceof Object[]) {
        encodedTxs.add(new MPWrappedArrayImpl((Object[]) o));
      } else if (o instanceof MPWrappedArray) {
        encodedTxs.add((MPWrappedArray) o);
      } else {
        encodedTxs.add(new MPWrappedArrayImpl(((Collection<?>) o).toArray()));
      }
    }
  }


  /**
   * New instance, based upon an original message.
   *
   * @param type     the new message type
   * @param original the original message
   */
  public TxPackage(P2PType type, MPWrappedArray original) {
    chainId = original.asInt(0);
    this.type = type;

    Object[] txs = original.asObjectArray(2);
    encodedTxs.ensureCapacity(txs.length);
    for (Object o : txs) {
      if (o instanceof Object[]) {
        encodedTxs.add(new MPWrappedArrayImpl((Object[]) o));
      } else if (o instanceof MPWrappedArray) {
        encodedTxs.add((MPWrappedArray) o);
      } else {
        encodedTxs.add(new MPWrappedArrayImpl(((Collection<?>) o).toArray()));
      }
    }
  }


  /**
   * New instance.
   *
   * @param type      message type
   * @param chainId   chain's ID
   * @param encodedTx encoded transactions
   */
  public TxPackage(P2PType type, int chainId, Object[] encodedTx) {
    this.chainId = chainId;
    this.type = type;

    encodedTxs.ensureCapacity(encodedTx.length);
    for (Object o : encodedTx) {
      encodedTxs.add(new MPWrappedArrayImpl((Object[]) o));
    }
  }


  /**
   * New instance from original.
   *
   * @param type     new message type
   * @param original original message
   */
  public TxPackage(P2PType type, TxPackage original) {
    this.chainId = original.getChainId();
    this.type = type;
    encodedTxs.addAll(original.encodedTxs);
  }


  /**
   * New instance from encoded form.
   *
   * @param type    the message type
   * @param chainId the chain ID
   * @param txList  the encoded transactions
   */
  public TxPackage(P2PType type, int chainId, List<MPWrappedArray> txList) {
    this.chainId = chainId;
    this.type = type;
    encodedTxs.addAll(txList);
  }


  @Override
  public Object[] encode() {
    return new Object[]{chainId, type.getId(), encodedTxs.toArray()};
  }


  @Override
  public int getChainId() {
    return chainId;
  }


  public List<MPWrappedArray> getEncodedTxs() {
    return Collections.unmodifiableList(encodedTxs);
  }


  @Override
  public P2PType getType() {
    return type;
  }


  /**
   * Merge another message into this.
   *
   * @param other the other message
   */
  public void merge(TxPackage other) {
    if (chainId != other.chainId) {
      throw new IllegalArgumentException("Invalid chain. Expected " + chainId + ", got " + other.getChainId());
    }
    encodedTxs.addAll(other.encodedTxs);
  }


  public int size() {
    return encodedTxs.size();
  }
}
