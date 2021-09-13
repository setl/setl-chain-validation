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
package io.setl.bc.pychain.node;

import java.util.List;
import java.util.Set;

import io.setl.bc.performance.PerformanceEventHandler;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.block.TxIdList;
import io.setl.bc.pychain.state.tx.Txi;

/**
 * Provide a pool of transactions.
 *
 * @author aanten, Simon Greatrix
 */
public interface TransactionPool extends PerformanceEventHandler {

  enum HasResult {
    PRESENT,
    REPLAY,
    NOT_PRESENT
  }

  /**
   * Add a transaction to the pool.
   *
   * @param va The encoded transaction.
   */
  void addTx(Txi va);

  /**
   * Remove all the transactions that were part of a block.
   */
  void bulkRemove(Block block);

  /**
   * Clear all transactions from the pool.
   */
  void clear();

  /**
   * Create a list of transactions to propose in the next block.
   *
   * @param maxTxPerBlock the approximate maximum number of transactions to include
   *
   * @return the selected transactions
   */
  ProposedTxList createProposal(int maxTxPerBlock);

  /**
   * Get all transactions associated with a specified issuing address.
   *
   * @param nonceAddress the issuing address
   *
   * @return the transactions
   */
  List<Txi> getAllTx(String nonceAddress);

  /**
   * Count of transactions in pool.
   *
   * @return count of available transactions.
   */
  int getAvailableTransactionCount();

  /**
   * Get the specified transaction.
   *
   * @param nonceAddress the transaction's issuing address
   * @param nonce        the transaction's nonce
   * @param hash         the transaction's hash
   *
   * @return the transaction
   */
  Txi getTx(String nonceAddress, long nonce, Hash hash);

  /**
   * Check if any transactions are in the pool.
   *
   * @return true if transactions are present.
   */
  boolean hasAvailableTransactions();

  /**
   * Does this pool contain the specified transaction?.
   *
   * @param nonceAddress the transaction's issuing address
   * @param nonce        the transaction's nonce
   * @param hash         the transaction's hash
   *
   * @return check result
   */
  HasResult hasTx(String nonceAddress, long nonce, Hash hash);

  /**
   * Match the transactions from a proposal.
   *
   * @param transactions the matched transactions that are present in this pool.
   * @param missing      the transactions that were not matched
   * @param txIds        the transactions to find
   */
  void matchTxs(List<Txi> transactions, Set<NonceAndHash> missing, TxIdList txIds);

}
