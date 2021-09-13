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

import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.ProposedTxList.TxList;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.p2p.message.Encodable;
import io.setl.bc.pychain.state.tx.Txi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Simon Greatrix on 2019-04-30.
 */
public class ProposedTxIds implements Encodable {


  /**
   * Restore a proposed TX list from its encoded form.
   *
   * @param encoded the encoded form
   *
   * @return the list of proposed transactions
   */
  public static ProposedTxIds decode(Object[] encoded) {
    if (!Long.valueOf(1).equals(encoded[0])) {
      throw new IllegalArgumentException("Unknown encoding version: " + encoded[0]);
    }

    ProposedTxIds list = new ProposedTxIds();
    Object[] allLists = (Object[]) encoded[1];
    for (Object o : allLists) {
      TxIdList txIdList = new TxIdList((Object[]) o);
      list.transactions.put(txIdList.getAddress(), txIdList);
    }
    return list;
  }


  final Map<String, TxIdList> transactions = new TreeMap<>();


  ProposedTxIds() {
    // do nothing
  }


  ProposedTxIds(ProposedTxList list) {
    for (Map.Entry<String, TxList> e : list.transactions.entrySet()) {
      transactions.put(e.getKey(), new TxIdList(e.getValue()));
    }
  }


  /**
   * Convert this list of transaction IDs into a list of actual transactions.
   *
   * @param pool      the pool to draw transactions from
   * @param unmatched the unmatched transactions, which will be populated by this call
   *
   * @return the list of transactions
   */
  public ProposedTxList asTxList(TransactionPool pool, MissingTxIds unmatched) {
    ProposedTxList output = new ProposedTxList();
    Set<NonceAndHash> missing = new HashSet<>();
    for (TxIdList list : transactions.values()) {
      List<Txi> transactions = new ArrayList<>(list.getHashes().size());

      pool.matchTxs(transactions, missing, list);
      if (!missing.isEmpty()) {
        unmatched.addAll(list.getAddress(), missing);
        missing = new HashSet<>();
      }

      TxList txList = new TxList(list.getAddress(), list.getFirstNonce(), transactions);
      output.transactions.put(list.getAddress(), txList);
    }
    return output;
  }


  @Override
  public Object[] encode() {
    Object[] encoded = new Object[transactions.size()];
    int i = 0;
    for (TxIdList t : transactions.values()) {
      encoded[i++] = t.encode();
    }
    return new Object[]{1, encoded};
  }
}
