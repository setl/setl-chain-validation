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
package io.setl.bc.pychain.block;

import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.state.tx.Txi;
import org.msgpack.core.MessagePacker;

/**
 * Identification of a transaction by its nonce address, nonce, and hash.
 *
 * <p>The nonce address and nonce work as co-ordinates, indicating where the transaction belong in the process. The hash is a unique identifier of the
 * transaction, which sadly cannot be used to locate it.</p>
 *
 * <p>As a bad actor could submit multiple transactions with the same nonce address and nonce, the hash ensures a perfect match.</p>
 */
public class TxId implements MsgPackable {

  private final String address;

  private final String hash;

  private final long nonce;


  /**
   * New instance.
   *
   * @param address address
   * @param nonce   nonce
   * @param hash    hash
   */
  public TxId(String address, long nonce, String hash) {
    this.address = address;
    this.nonce = nonce;
    this.hash = hash;
  }


  /**
   * New instance.
   *
   * @param packed packed representation
   */
  public TxId(Object[] packed) {
    this.address = (String) packed[0];
    this.nonce = (Long) packed[1];
    this.hash = (String) packed[2];

  }


  /**
   * New instance.
   *
   * @param txi the transaction
   */
  public TxId(Txi txi) {
    this.address = txi.getNonceAddress();
    this.nonce = txi.getNonce();
    this.hash = txi.getHash();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TxId)) {
      return false;
    }
    TxId txId = (TxId) o;
    return nonce == txId.nonce && address.equals(txId.address) && hash.equals(txId.hash);
  }


  public String getAddress() {
    return address;
  }


  public String getHash() {
    return hash;
  }


  public long getNonce() {
    return nonce;
  }


  @Override
  public int hashCode() {
    return address.hashCode() ^ (int) nonce;
  }


  @Override
  public void pack(MessagePacker p) throws Exception {
    p.packArrayHeader(3);
    p.packString(address);
    p.packLong(nonce);
    p.packString(hash);
  }


  public String toString() {
    return String.format("TxId{%s:%d}", address, nonce);
  }
}
