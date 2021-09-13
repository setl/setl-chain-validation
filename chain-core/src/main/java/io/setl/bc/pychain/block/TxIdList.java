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

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.ProposedTxList.TxList;
import io.setl.bc.pychain.p2p.message.Encodable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A list of consecutive TXs for a specific address.
 *
 * @author Simon Greatrix on 2019-04-30.
 */
public class TxIdList implements Encodable {

  private final String address;

  private final long firstNonce;

  private final ArrayList<Hash> hashes = new ArrayList<>();


  TxIdList(TxList txList) {
    address = txList.address;
    firstNonce = txList.firstNonce;
    hashes.ensureCapacity(txList.txis.size());
    for (TransactionForProcessing txi : txList.txis) {
      hashes.add(Hash.fromHex(txi.getHash()));
    }
  }


  /**
   * New instance from the encoded form.
   *
   * @param encoded the encoded form
   */
  public TxIdList(Object[] encoded) {
    address = (String) encoded[0];
    firstNonce = (Long) encoded[1];
    Object[] h = (Object[]) encoded[2];
    hashes.ensureCapacity(h.length);
    for (Object o : h) {
      hashes.add(new Hash((byte[]) o));
    }
  }


  @Override
  public Object[] encode() {
    return new Object[]{address, firstNonce, hashes.toArray()};
  }


  public String getAddress() {
    return address;
  }


  public long getFirstNonce() {
    return firstNonce;
  }


  public List<Hash> getHashes() {
    return Collections.unmodifiableList(hashes);
  }
}
