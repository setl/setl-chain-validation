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
package io.setl.bc.pychain.node.txpool;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.TxIdList;
import io.setl.bc.pychain.node.StateManager;
import io.setl.bc.pychain.node.TransactionPool.HasResult;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.tx.Txi;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * A series of transactions associated with an address. The series is represented as blocks of transactions with contiguous nonces.
 */
class Series {

  private final String address;

  private final int addressCode;

  private final SparseArray sparseArray = new SparseArray();

  private long minNonce = 0;


  Series(StateManager stateManager, String address) {
    this.address = address;
    this.addressCode = address.hashCode();

    Merkle<AddressEntry> addresses = stateManager.getState().getAssetBalances();
    AddressEntry entry = addresses.find(address);
    if (entry == null) {
      this.minNonce = 0;
    } else {
      this.minNonce = entry.getNonce();
    }
  }


  synchronized boolean addTx(Txi txi) {
    long nonce = txi.getNonce();
    if (nonce < minNonce) {
      // nonce is in past, so ignore
      return false;
    }

    return sparseArray.addTx(nonce, txi);
  }


  synchronized void bulkRemove(long max) {
    minNonce = Math.max(max + 1, minNonce);
    sparseArray.bulkRemove(max);
  }


  String getAddress() {
    return address;
  }


  int getAddressCode() {
    return addressCode;
  }


  synchronized List<Txi> getAllTXs() {
    ArrayList<Txi> list = new ArrayList<>();
    Deque<Range> ranges = sparseArray.getRanges();
    while (!ranges.isEmpty()) {
      Range r = ranges.removeFirst();
      for (Txi txi : r.txis) {
        if (txi != null) {
          if (txi instanceof ConfusedNonceTx) {
            txi = ((ConfusedNonceTx) txi).getPrimaryTx();
          }
          list.add(txi);
        }
      }
    }
    return list;
  }


  synchronized Txi getTx(long nonce, Hash hash) {
    return sparseArray.getTx(nonce, hash);
  }


  synchronized HasResult hasTx(long nonce, Hash hash) {
    if (nonce < minNonce) {
      return HasResult.REPLAY;
    }
    return sparseArray.getTx(nonce, hash) != null ? HasResult.PRESENT : HasResult.NOT_PRESENT;
  }


  synchronized boolean isEmpty() {
    return sparseArray.isEmpty();
  }


  synchronized TxIterator iterate(int maxTxPerBlock) {
    return new TxIterator(sparseArray, minNonce, maxTxPerBlock);
  }


  synchronized void matchTxs(List<Txi> transactions, Set<NonceAndHash> unmatched, TxIdList txIds) {
    long nonce = txIds.getFirstNonce();
    List<Hash> hashes = txIds.getHashes();
    AllTxIterator iterator = new AllTxIterator(sparseArray, nonce, nonce + hashes.size());
    Txi nextKnown = (iterator.hasNext()) ? iterator.next() : null;
    for (Hash hash : hashes) {
      if (nextKnown == null) {
        // We've run out of known TXs, so these are unmatched.
        unmatched.add(new NonceAndHash(nonce, hash));
        transactions.add(null);
      } else if (nextKnown.getNonce() == nonce) {
        if (hash.equals(Hash.fromHex(nextKnown.getHash()))) {
          // Nice and simple match
          transactions.add(nextKnown);
        } else {
          // Could be a confused nonce?
          if (nextKnown instanceof ConfusedNonceTx) {
            Txi m = ((ConfusedNonceTx) nextKnown).getTx(hash);
            if (m != null) {
              // matched TX :-)
              transactions.add(m);
            } else {
              // we have several TXs, but nonce of them match
              transactions.add(null);
              unmatched.add(new NonceAndHash(nonce, hash));
            }
          } else {
            // we have TX, but hash doesn't match
            transactions.add(null);
            unmatched.add(new NonceAndHash(nonce, hash));
          }
        }

        // move to next known TX
        nextKnown = (iterator.hasNext()) ? iterator.next() : null;
      } else {
        unmatched.add(new NonceAndHash(nonce, hash));
        transactions.add(null);
      }

      // increase nonce to match
      nonce++;
    }
  }
}
