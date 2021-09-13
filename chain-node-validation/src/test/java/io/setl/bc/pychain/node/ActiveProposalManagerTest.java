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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.eventbus.EventBus;
import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.MissingTxIds;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.block.ProposedTxList.Builder;
import io.setl.bc.pychain.block.TxIdList;
import io.setl.bc.pychain.event.ProposalUpdateEvent;
import io.setl.bc.pychain.p2p.message.ProposedTransactions;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import io.setl.bc.pychain.p2p.message.VoteMessage;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.TransactionProcessor;
import io.setl.bc.pychain.tx.create.NullTX;
import io.setl.common.Balance;
import io.setl.common.Hex;
import io.setl.common.MutableBoolean;
import io.setl.crypto.KeyGen;
import io.setl.util.Priorities;
import io.setl.util.PriorityExecutor;
import io.setl.util.RuntimeInterruptedException;
import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by Simon Greatrix
 */
public class ActiveProposalManagerTest {

  private static Hash STATE_HASH = Hash.fromHex("00112233445566778899aabbccddeeff");

  ActiveProposalManager apm;

  EventBus eventBus;

  KeyPair keyPair1 = KeyGen.generateKeyPair();

  KeyPair keyPair2 = KeyGen.generateKeyPair();

  PeerAddress peerAddress;

  PriorityExecutor priorityExecutor;

  UUID proposalId = UUID.randomUUID();

  KeyPair proposerKeyPair = KeyGen.generateKeyPair();

  State state;

  StateManager stateManager;

  StateSnapshot stateSnapshot;

  long timestamp = System.currentTimeMillis();

  TransactionProcessor transactionProcessor;

  private ArrayList<Object> events = new ArrayList<>();


  @Test
  public void addCompleteProposal() {
    ProposedTransactions message;

    Builder builder = new Builder();
    for (int i = 0; i < 5; i++) {
      builder.add(makeTx(i));
    }
    ProposedTxList txList = builder.build();

    message = new ProposedTransactions(1, 5, STATE_HASH, 1000, proposalId, txList.asIdList(), "proposerNode",
        Hex.encode(proposerKeyPair.getPublic().getEncoded()));

    TransactionPool txPool = mock(TransactionPool.class);
    apm.addProposedTxs(peerAddress, message, txList, txPool);

    ProposedTransactions actual = apm.getProposal(proposalId);

    // Should have kicked off processing of the transactions
    verify(priorityExecutor).submit(eq(Priorities.PROPOSAL), any(Runnable.class));

    // Should have kept the proposal message
    assertSame(message, actual);
  }


  @Test
  public void addIncompleteProposal() {
    ProposedTransactions message;

    Builder builder = new Builder();
    for (int i = 0; i < 5; i++) {
      builder.add(makeTx(i));
    }
    ProposedTxList txList = builder.build();

    message = new ProposedTransactions(1, 5, STATE_HASH, 1000, proposalId, txList.asIdList(), "proposerNode",
        Hex.encode(proposerKeyPair.getPublic().getEncoded()));

    // Detect the transactions as missing
    TransactionPool txPool = mock(TransactionPool.class);
    doAnswer(
        iom -> {
          Set<NonceAndHash> missingTxIds = iom.getArgument(1);
          TxIdList txIdList = iom.getArgument(2);
          int i = 0;
          for (Hash hash : txIdList.getHashes()) {
            missingTxIds.add(new NonceAndHash(txIdList.getFirstNonce() + i, hash));
          }
          return null;
        }
    ).when(txPool).matchTxs(any(), any(), any());

    // set the transaction request handler
    BiConsumer<PeerAddress, MissingTxIds> handler = mock(BiConsumer.class);
    apm.setTransactionRequestHandler(handler);

    apm.addProposedTxs(peerAddress, message, null, txPool);

    ProposedTransactions actual = apm.getProposal(proposalId);

    // Should not have kicked off processing of the transactions
    verify(priorityExecutor, never()).submit(eq(Priorities.PROPOSAL), any(Runnable.class));

    // Should have requested transactions
    verify(handler).accept(any(), any());

    // Should have kept the proposal message
    assertSame(message, actual);
  }


  @Test
  public void addProposalAndVote() {
    apm.reset(new Balance(200), new Balance(300), 5);
    addCompleteProposal();
    finishAsync();

    Block block = apm.getBlock(proposalId);
    assertNotNull(block);

    VoteMessage vm = new VoteMessage(1, proposalId, 0, block.getBlockHash(), 5, 0, Hex.encode(proposerKeyPair.getPublic().getEncoded()));
    apm.addVote(vm);

    assertEquals(2, events.size());
    ProposalUpdateEvent event = (ProposalUpdateEvent) events.get(1);

    // Proposal should have 33% vote now
    assertEquals(33.0, event.getVotePercentage(), 1.0);

    // second vote triggers signature
    ConsensusVoteEventHandler handler = mock(ConsensusVoteEventHandler.class);
    apm.setVoteRequirementMetHandler(handler);
    vm = new VoteMessage(1, proposalId, 0, block.getBlockHash(), 5, 0, Hex.encode(keyPair1.getPublic().getEncoded()));
    apm.addVote(vm);

    assertEquals(3, events.size());
    event = (ProposalUpdateEvent) events.get(1);

    // Proposal should have 33% vote now
    assertEquals(33.0, event.getVotePercentage(), 1.0);
    verify(handler).accept(eq(proposalId),any(),any(),any(),anyLong(),anyInt());
  }



  @Test
  public void addSignature() {
    // need to add a proposal to create the block, then we reset to actually do the test
    apm.reset(new Balance(200), new Balance(300), 5);

    addCompleteProposal();
    finishAsync();

    Block block = apm.getBlock(proposalId);
    apm.reset(new Balance(200), new Balance(300), 5);
    events.clear();

    VoteMessage vm1 = new VoteMessage(1, proposalId, 0, block.getBlockHash(), 5, 0, Hex.encode(proposerKeyPair.getPublic().getEncoded()));
    vm1.sign(proposerKeyPair.getPrivate());
    VoteMessage vm2 = new VoteMessage(1, proposalId, 0, block.getBlockHash(), 5, 0, Hex.encode(keyPair1.getPublic().getEncoded()));
    vm2.sign(keyPair1.getPrivate());
    List<VoteMessage> votes = new ArrayList<>();
    votes.add(vm1);
    votes.add(vm2);
    Hash xcHash = new DefaultHashableHashComputer().computeHash(block.getXChainHashableObject());
    SignatureMessage sm = new SignatureMessage(1,5,proposalId,block.getBlockHash(),votes,"XC",xcHash,
        Hex.encode(keyPair1.getPublic().getEncoded()));
    apm.addSignature(sm);


  }

  @Test
  public void addProposalTwice() {
    ProposedTransactions message;

    Builder builder = new Builder();
    for (int i = 0; i < 5; i++) {
      builder.add(makeTx(i));
    }
    ProposedTxList txList = builder.build();

    message = new ProposedTransactions(1, 5, STATE_HASH, 1000, proposalId, txList.asIdList(), "proposerNode",
        Hex.encode(proposerKeyPair.getPublic().getEncoded()));

    TransactionPool txPool = mock(TransactionPool.class);
    apm.addProposedTxs(peerAddress, message, txList, txPool);

    ProposedTransactions actual = apm.getProposal(proposalId);

    // Create a second proposal message
    ProposedTransactions other = new ProposedTransactions(1, 5, STATE_HASH, 1000, proposalId, txList.asIdList(), "otherNode",
        Hex.encode(proposerKeyPair.getPublic().getEncoded()));
    apm.addProposedTxs(peerAddress, message, null, txPool);

    // Should have kept the proposal message
    assertSame(message, actual);
  }


  @Before
  public void before() throws IOException, StateSnapshotCorruptedException {
    priorityExecutor = spy(new PriorityExecutor(1));

    transactionProcessor = mock(TransactionProcessor.class);
    when(transactionProcessor.processTransactions(anyInt(), any(), any(), any(), anyLong())).thenReturn(true);

    stateManager = mock(StateManager.class);

    eventBus = mock(EventBus.class);
    doAnswer(iom -> {
      events.add(iom.getArgument(0));
      return null;
    }).when(eventBus).post(any());

    apm = new ActiveProposalManager(priorityExecutor, eventBus, stateManager, transactionProcessor);

    state = mock(State.class);
    stateSnapshot = mock(StateSnapshot.class);
    when(state.createSnapshot()).thenReturn(stateSnapshot);
    when(state.getXChainSignNodes()).thenReturn(Collections.emptyNavigableMap());
    when(state.getLoadedHash()).thenReturn(new Hash("Loaded Hash".getBytes(UTF_8)));
    when(state.getBlockHash()).thenReturn(new Hash("Block Hash".getBytes(UTF_8)));
    when(state.getHeight()).thenReturn(5);

    when(stateManager.getState()).thenReturn(state);
    when(stateManager.getVotingPower(any())).thenReturn(new Balance(100L));
    peerAddress = mock(PeerAddress.class);
    when(peerAddress.getAddressString()).thenReturn("proposer");
  }


  private void finishAsync() {
    // Since the priority executor has only one thread, we just need to add a low priority job and wait for it to finish.
    final MutableBoolean isDone = new MutableBoolean(false);
    priorityExecutor.submit(0, () -> {
      synchronized (isDone) {
        isDone.set(true);
        isDone.notifyAll();
      }
    });

    try {
      synchronized (isDone) {
        while (!isDone.get()) {
          isDone.wait();
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }


  private Txi makeTx(long nonce) {
    NullTX creator = new NullTX();
    creator.setAddress("myAddress");
    creator.setNonce(nonce);
    creator.setTimestamp(timestamp++);
    return creator.create();
  }
}
