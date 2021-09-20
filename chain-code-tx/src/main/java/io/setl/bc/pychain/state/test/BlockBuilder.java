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
    
    Here is my wonderful comment
 
</notice> */
package io.setl.bc.pychain.state.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.tx.TxFromList;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.create.BaseTransaction;
import io.setl.common.MutableInt;
import io.setl.common.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A builder for blocks.
 *
 * @author Simon Greatrix on 08/01/2020.
 */
public class BlockBuilder {

  /**
   * Decode event data from their block format.
   *
   * @param block the block
   *
   * @return the event data
   */
  public static SortedSet<EventData> decodeContractEvents(Block block) {
    MPWrappedArray raw = block.getContractEvents();
    if (raw == null) {
      return Collections.emptySortedSet();
    }
    TreeSet<EventData> data = new TreeSet<>();
    int s = raw.size();
    for (int i = 0; i < s; i++) {
      data.add(new EventData(raw.asObjectArray(i)));
    }
    return data;
  }


  /**
   * Decode time events from their block format.
   *
   * @param block the block
   *
   * @return the events
   */
  public static SortedSet<String> decodeTimeEvents(Block block) {
    MPWrappedArray raw = block.getTimeEvents();
    if (raw == null) {
      return Collections.emptySortedSet();
    }
    TreeSet<String> data = new TreeSet<>();
    int s = raw.size();
    for (int i = 0; i < s; i++) {
      data.add(raw.asString(i));
    }
    return data;
  }


  /**
   * Decode a transaction list from their block format.
   *
   * @param block the block
   *
   * @return the transactions
   */
  public static List<Txi> decodeTxList(Block block) {
    List<MPWrappedArray> raw = block.getTxList();
    if (raw == null) {
      return Collections.emptyList();
    }
    ArrayList<Txi> txis = new ArrayList<>(raw.size());
    raw.forEach(mp -> txis.add(TxFromList.txFromList(mp)));
    return txis;
  }


  /**
   * Decode cross chain transaction IDs from their block format.
   *
   * @param block the block
   *
   * @return the IDs
   */
  public static SortedSet<XCTxId> decodeXChainTxs(Block block) {
    List<Pair<String, Integer>> pairs = block.getXChainTxs();
    if (pairs == null) {
      return Collections.emptySortedSet();
    }
    TreeSet<XCTxId> ids = new TreeSet<>();
    pairs.forEach(p -> ids.add(new XCTxId(p.right(), Hash.fromHex(p.left()))));
    return ids;
  }


  /**
   * Create a block builder from a state, snapshot and timestamp. This leaves the transaction list, the node name, and the cross-chain transaction IDs unset.
   *
   * @return the builder
   */
  public static BlockBuilder from(State state, StateSnapshot snapshot, long timeStamp) {
    BlockBuilder bb = from(state, timeStamp);
    bb.setContractEvents(snapshot.getContractEvents());
    bb.effectiveTxList = snapshot.getEffectiveTXList();
    return bb;
  }


  /**
   * Create a block builder starting from a given state.
   *
   * @param state the starting state
   *
   * @return the builder
   */
  public static BlockBuilder from(State state, long timeStamp) {
    BlockBuilder bb = new BlockBuilder();
    bb.timeStamp = timeStamp;
    bb.chainId = state.getChainId();
    bb.height = state.getHeight() + 1;
    bb.previousBlockHash = state.getBlockHash();
    bb.baseStateHash = state.getLoadedHash();
    bb.timeEvents = state.pendingEventTimeAddresses(timeStamp);
    return bb;
  }


  private static List<BaseTransaction> toBaseTransaction(List<Txi> txis) {
    if (txis == null) {
      return Collections.emptyList();
    }
    ArrayList<BaseTransaction> bases = new ArrayList<>(txis.size());
    txis.forEach(txi -> bases.add(BaseTransaction.getRepresentation(txi)));
    return bases;
  }


  public class TxListBuilder {

    /** TXs for regular or effective transactions?. */
    private final boolean isRegular;

    private List<Txi> txis = new ArrayList<>();


    TxListBuilder(boolean isRegular) {
      this.isRegular = isRegular;
    }


    public TxListBuilder add(Txi txi) {
      txis.add(txi);
      return this;
    }


    public TxListBuilder add(MPWrappedArray wrappedArray) {
      txis.add(TxFromList.txFromList(wrappedArray));
      return this;
    }


    public TxListBuilder add(BaseTransaction txi) {
      add(txi.create());
      return this;
    }


    public TxListBuilder addEncoded(List<MPWrappedArray> encoded) {
      encoded.forEach(this::add);
      return this;
    }


    public TxListBuilder addTransactions(List<BaseTransaction> bases) {
      bases.forEach(this::add);
      return this;
    }


    public TxListBuilder addTxi(List<Txi> newTxis) {
      txis.addAll(newTxis);
      return this;
    }


    /**
     * Finish building this list of transactions and return to building the block.
     *
     * @return the block builder
     */
    public BlockBuilder finish() {
      if (isRegular) {
        BlockBuilder.this.txList = txis;
      } else {
        BlockBuilder.this.effectiveTxList = txis;
      }
      return BlockBuilder.this;
    }
  }



  private Hash baseStateHash = Hash.NULL_HASH;

  private int chainId;

  private SortedSet<EventData> contractEvents = Collections.emptySortedSet();

  private List<Txi> effectiveTxList = Collections.emptyList();

  private int height;

  private String nodeName;

  private Hash previousBlockHash = Hash.NULL_HASH;

  private SortedSet<String> timeEvents = Collections.emptySortedSet();

  private long timeStamp;

  private List<Txi> txList = Collections.emptyList();

  private SortedSet<XCTxId> xChainTxs = Collections.emptySortedSet();


  public BlockBuilder() {
    // do nothing
  }


  /**
   * New instance capturing the initial values from an existing block.
   *
   * @param block the block
   */
  public BlockBuilder(Block block) {
    baseStateHash = block.getBaseStateHash();
    chainId = block.getChainId();
    height = block.getHeight();
    nodeName = block.getNodeName();
    previousBlockHash = block.getPreviousBlockHash();
    timeStamp = block.getTimeStamp();

    // Decode from encoded form
    contractEvents = decodeContractEvents(block);
    effectiveTxList = Arrays.asList(block.getEffectiveTransactions());
    timeEvents = decodeTimeEvents(block);
    txList = decodeTxList(block);
    xChainTxs = decodeXChainTxs(block);
  }


  /**
   * Build the block as defined in this.
   *
   * @return the block
   */
  public Block build() {
    Block block = new Block(
        chainId,
        height,
        baseStateHash,
        previousBlockHash,
        encodeTxList(),
        timeStamp,
        nodeName,
        encodeXChainTxs(),
        timeEvents.toArray(),
        encodeContractEvents(),
        encodeEffectiveTxList()
    );
    block.setBlockHash();
    return block;
  }


  private Object[] encodeContractEvents() {
    int s = contractEvents.size();
    Object[][] output = new Object[s][];
    MutableInt i = new MutableInt(-1);
    contractEvents.forEach(ed -> output[i.increment()] = ed.encode());
    return output;
  }


  private Object[] encodeEffectiveTxList() {
    if (effectiveTxList == null) {
      return new Object[0];
    }
    Object[] rVal = new Object[effectiveTxList.size()];
    final MutableInt i = new MutableInt(-1);
    effectiveTxList.forEach(thisTx -> rVal[i.increment()] = thisTx.encodeTx());
    return rVal;
  }


  private List<MPWrappedArray> encodeTxList() {
    ArrayList<MPWrappedArray> wrapped = new ArrayList<>(txList.size());
    txList.forEach(txi -> wrapped.add(new MPWrappedArrayImpl(txi.encodeTx())));
    return wrapped;
  }


  private List<Pair<String, Integer>> encodeXChainTxs() {
    ArrayList<Pair<String, Integer>> pairs = new ArrayList<>(xChainTxs.size());
    xChainTxs.forEach(id -> pairs.add(new Pair<>(id.getHash().toHexString(), id.getChainId())));
    return pairs;
  }


  public Hash getBaseStateHash() {
    return baseStateHash;
  }


  public int getChainId() {
    return chainId;
  }


  public Collection<EventData> getContractEvents() {
    return contractEvents;
  }


  public Set<XCTxId> getCrossChainTxs() {
    return xChainTxs;
  }


  public List<BaseTransaction> getEffectiveTxList() {
    return toBaseTransaction(effectiveTxList);
  }


  @JsonIgnore
  public List<Txi> getEffectiveTxiList() {
    return effectiveTxList;
  }


  public int getHeight() {
    return height;
  }


  public String getNodeName() {
    return nodeName;
  }


  public Hash getPreviousBlockHash() {
    return previousBlockHash;
  }


  public Set<String> getTimeEvents() {
    return timeEvents;
  }


  public long getTimeStamp() {
    return timeStamp;
  }


  public List<BaseTransaction> getTxList() {
    return toBaseTransaction(txList);
  }


  @JsonIgnore
  public List<Txi> getTxiList() {
    return txList;
  }


  public void setBaseStateHash(Hash baseStateHash) {
    this.baseStateHash = baseStateHash != null ? baseStateHash : Hash.NULL_HASH;
  }


  public void setChainId(int chainId) {
    this.chainId = chainId;
  }


  /**
   * Set the contract events in the block.
   *
   * @param contractEvents the contract events
   */
  public void setContractEvents(Collection<EventData> contractEvents) {
    TreeSet<EventData> events = new TreeSet<>();
    events.addAll(contractEvents);
    this.contractEvents = events;
  }


  public void setCrossChainTxs(Set<XCTxId> xChainTxs) {
    this.xChainTxs = new TreeSet<>(xChainTxs);
  }


  /**
   * Set the effective transaction list in the block.
   *
   * @param newTxList the transaction list
   */
  public void setEffectiveTxList(List<BaseTransaction> newTxList) {
    if (newTxList == null) {
      effectiveTxList = Collections.emptyList();
      return;
    }
    effectiveTxList = new ArrayList<>(newTxList.size());
    newTxList.forEach(txi -> effectiveTxList.add(txi.create()));
  }


  /**
   * Set the effective transaction list in the block.
   *
   * @param newTxis the transaction list
   */
  @JsonIgnore
  public void setEffectiveTxiList(List<Txi> newTxis) {
    if (newTxis == null) {
      effectiveTxList = Collections.emptyList();
      return;
    }
    effectiveTxList = newTxis;
  }


  public void setHeight(int height) {
    this.height = height;
  }


  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }


  public void setPreviousBlockHash(Hash previousBlockHash) {
    this.previousBlockHash = previousBlockHash != null ? previousBlockHash : Hash.NULL_HASH;
  }


  /**
   * Sets the address's of objects that have experienced time events in this block.
   *
   * @param timeEvents the addresses
   */
  public void setTimeEvents(Set<String> timeEvents) {
    this.timeEvents = new TreeSet<>(timeEvents);
  }


  public void setTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
  }


  /**
   * Set the transaction list in the block.
   *
   * @param newTxList the transaction list
   */
  public void setTxList(List<BaseTransaction> newTxList) {
    if (newTxList == null) {
      txList = Collections.emptyList();
      return;
    }
    txList = new ArrayList<>(newTxList.size());
    newTxList.forEach(txi -> txList.add(txi.create()));
  }


  /**
   * Set the transaction list in the block.
   *
   * @param newTxiList the transaction list
   */
  @JsonIgnore
  public void setTxiList(List<Txi> newTxiList) {
    if (newTxiList == null) {
      txList = Collections.emptyList();
      return;
    }
    txList = newTxiList;
  }


  public BlockBuilder withBaseStateHash(Hash hash) {
    setBaseStateHash(hash);
    return this;
  }


  public BlockBuilder withChainId(int chainId) {
    this.chainId = chainId;
    return this;
  }


  public BlockBuilder withContractEvents(Collection<EventData> data) {
    setContractEvents(data);
    return this;
  }


  /**
   * Add cross chain transactions. Each pair provides a transaction hash and the chain ID.
   *
   * @param xChainTxs the cross chain transactions
   *
   * @return this
   */
  public BlockBuilder withCrossChainTxs(Set<XCTxId> xChainTxs) {
    setCrossChainTxs(xChainTxs);
    return this;
  }


  public TxListBuilder withEffectiveTxList() {
    return new TxListBuilder(false);
  }


  public BlockBuilder withEffectiveTxList(List<BaseTransaction> txis) {
    setEffectiveTxList(txis);
    return this;
  }


  public BlockBuilder withEffectiveTxiList(List<Txi> txis) {
    setEffectiveTxiList(txis);
    return this;
  }


  public BlockBuilder withHeight(int height) {
    this.height = height;
    return this;
  }


  public BlockBuilder withNodeName(String nodeName) {
    this.nodeName = nodeName;
    return this;
  }


  public BlockBuilder withPreviousBlockHash(Hash previousBlockHash) {
    setPreviousBlockHash(previousBlockHash);
    return this;
  }


  /**
   * Add the set of the addresses for all objects that have an associated events.
   *
   * @param addresses the set of addresses
   *
   * @return this
   */
  public BlockBuilder withTimeEvents(Set<String> addresses) {
    setTimeEvents(addresses);
    return this;
  }


  public BlockBuilder withTimeStamp(long timeStamp) {
    this.timeStamp = timeStamp;
    return this;
  }


  public TxListBuilder withTxList() {
    return new TxListBuilder(true);
  }


  public BlockBuilder withTxList(List<BaseTransaction> txis) {
    setTxList(txis);
    return this;
  }


  public BlockBuilder withTxiList(List<Txi> txis) {
    setTxiList(txis);
    return this;
  }
}
