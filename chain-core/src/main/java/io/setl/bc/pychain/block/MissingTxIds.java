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
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.node.TransactionPool;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.serialise.UUIDEncoder;
import io.setl.util.ParallelTask;
import io.setl.util.PriorityExecutor.TaskContext;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.msgpack.core.MessagePacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Simon Greatrix on 2019-04-30.
 */
public class MissingTxIds implements MsgPackable {

  private static Logger logger = LoggerFactory.getLogger(MissingTxIds.class);



  public static class NonceAndHash {

    private final Hash hash;

    private final long nonce;


    public NonceAndHash(long nonce, Hash hash) {
      this.nonce = nonce;
      this.hash = hash;
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      NonceAndHash id = (NonceAndHash) o;

      if (nonce != id.nonce) {
        return false;
      }
      return hash.equals(id.hash);
    }


    public Hash getHash() {
      return hash;
    }


    public long getNonce() {
      return nonce;
    }


    @Override
    public int hashCode() {
      int result = (int) (nonce ^ (nonce >>> 32));
      result = 31 * result + hash.hashCode();
      return result;
    }


    @Override
    public String toString() {
      return new StringJoiner(", ", NonceAndHash.class.getSimpleName() + "[", "]")
          .add("hash='" + hash + "'")
          .add("nonce=" + nonce)
          .toString();
    }
  }



  private final Map<String, Set<NonceAndHash>> missing = new HashMap<>();

  private final AtomicInteger size = new AtomicInteger(0);

  private final UUID uuid;


  public MissingTxIds(UUID uuid) {
    this.uuid = uuid;
  }


  /**
   * New instance from the encoded form.
   *
   * @param encoded the encoded form
   */
  public MissingTxIds(MPWrappedArray encoded) {
    if (encoded.asInt(0) != 1) {
      throw new IllegalArgumentException("Unknown encoding version: " + encoded.unwrap()[0]);
    }

    uuid = UUIDEncoder.decode(encoded.asByte(1));

    MPWrappedMap<String, Object[]> map = encoded.asWrappedMap(2);
    for (Entry<String, Object[]> e : map.entrySet()) {
      HashSet<NonceAndHash> set = new HashSet<>();

      Object[] oa = e.getValue();
      for (Object o : oa) {
        Object[] oa2 = (Object[]) o;
        NonceAndHash nh = new NonceAndHash(((Number) oa2[0]).longValue(), new Hash((byte[]) oa2[1]));
        set.add(nh);
      }
      missing.put(e.getKey(), set);
      size.addAndGet(set.size());
    }
  }


  /**
   * Add a transaction's IDs to his set of missing transaction IDs.
   *
   * @param address the transaction's source address
   * @param nonce   the transaction's source nonce
   * @param hash    the transaction's hash
   */
  public void add(String address, long nonce, Hash hash) {
    NonceAndHash id = new NonceAndHash(nonce, hash);
    Set<NonceAndHash> set;
    synchronized (missing) {
      set = missing.computeIfAbsent(address, a -> new HashSet<>());
    }
    synchronized (set) {
      if (set.add(id)) {
        size.addAndGet(1);
      }
    }
  }


  /**
   * Add all the transaction's in the collection to this set of missing transaction IDs.
   *
   * @param address the source address for all the transactions
   * @param ids     the transactions nonce and hashes
   */
  public void addAll(String address, Collection<NonceAndHash> ids) {
    if (ids.isEmpty()) {
      return;
    }

    Set<NonceAndHash> set;
    synchronized (missing) {
      set = missing.computeIfAbsent(address, a -> new HashSet<>());
    }
    synchronized (set) {
      int s = set.size();
      set.addAll(ids);
      size.addAndGet(set.size() - s);
    }
  }


  /**
   * Get all the addresses that have missing transactions in this set.
   *
   * @return the list of all addresses
   */
  public String[] getAddresses() {
    synchronized (missing) {
      return missing.keySet().toArray(new String[0]);
    }
  }


  /**
   * Get the missing transaction identifiers for a specific hash.
   *
   * @param address the
   */
  public Set<NonceAndHash> getMissing(String address) {
    synchronized (missing) {
      Set<NonceAndHash> set = missing.get(address);
      return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }
  }


  public UUID getUuid() {
    return uuid;
  }


  public boolean isEmpty() {
    return size.get() == 0;
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    synchronized (missing) {
      p.packArrayHeader(3);
      p.packInt(1);

      UUIDEncoder.pack(p, uuid);

      p.packMapHeader(missing.size());
      for (Entry<String, Set<NonceAndHash>> e : missing.entrySet()) {
        p.packString(e.getKey());
        Set<NonceAndHash> set = e.getValue();
        synchronized (set) {
          p.packArrayHeader(set.size());
          for (NonceAndHash id : set) {
            p.packArrayHeader(2);
            p.packLong(id.getNonce());
            id.hash.pack(p);
          }
        }
      }
    }
  }


  public int size() {
    return size.get();
  }


  /**
   * Update the proposed TX list, supplying the transactions that were listed as missing in this.
   *
   * @param taskContext the task context for multi-threading
   * @param txList      the TX list to update
   * @param pool        the pool to draw transactions from
   *
   * @return true if all transactions are matched.
   */
  public boolean update(TaskContext taskContext, ProposedTxList txList, TransactionPool pool) {
    String[] addresses = getAddresses();
    AtomicBoolean allGood = new AtomicBoolean(true);
    ParallelTask.process(taskContext, 1, addresses.length, i -> {
      String address = addresses[i];
      TxList list = txList.transactions.get(address);
      Set<NonceAndHash> nhs;
      synchronized (missing) {
        nhs = missing.get(address);
      }
      if (nhs == null) {
        return;
      }
      boolean isEmpty;
      synchronized (nhs) {
        Iterator<NonceAndHash> iterator = nhs.iterator();
        while (iterator.hasNext()) {
          NonceAndHash nh = iterator.next();
          Txi txi = pool.getTx(address, nh.getNonce(), nh.getHash());
          if (txi == null) {
            logger.error(/* MARKER_CONSENSUS, */ "Transaction {}:{} {} has not validated into pool.", address, nh.getNonce(), nh.getHash());
            allGood.set(false);
          } else {
            size.decrementAndGet();
            iterator.remove();
            list.add(txi);
          }
        }
        isEmpty = nhs.isEmpty();
      }
      if (isEmpty) {
        synchronized (missing) {
          missing.remove(address);
        }
      }
    });

    return allGood.get();
  }
}
