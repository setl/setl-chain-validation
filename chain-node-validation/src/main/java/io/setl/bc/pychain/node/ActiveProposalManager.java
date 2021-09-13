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

import static io.setl.bc.logging.LoggingConstants.MARKER_CONSENSUS;
import static io.setl.common.Balance.BALANCE_ZERO;

import com.google.common.eventbus.EventBus;
import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.BlockVerifier;
import io.setl.bc.pychain.block.MissingTxIds;
import io.setl.bc.pychain.block.ProposedTxIds;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.block.ProposedTxList.BlockTx;
import io.setl.bc.pychain.event.ProposalUpdateEvent;
import io.setl.bc.pychain.event.ProposalUpdateEvent.UpdateType;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.p2p.message.ProposedTransactions;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import io.setl.bc.pychain.p2p.message.VoteMessage;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.tx.TransactionProcessor;
import io.setl.common.Balance;
import io.setl.common.Pair;
import io.setl.util.Priorities;
import io.setl.util.PriorityExecutor;
import io.setl.util.PriorityExecutor.TaskContext;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Manage any active proposals. Note: all public methods are synchronized. This implementation is explicitly single threaded.
 *
 * @author aanten
 */
@Component
public class ActiveProposalManager {

  private static final Logger logger = LoggerFactory.getLogger(ActiveProposalManager.class);



  /**
   * A single proposal.
   */
  private class Proposal {

    /** The chain height for this. */
    private final int height;

    /** Signatures received for this. */
    private final Map<String, SignatureMessage> signatures = new HashMap<>();

    /** The time we became aware of this proposal and started working on it. (For logging and performance analysis) */
    private final long startTime;

    /** The timestamp associated with the block. */
    private final long timestamp;

    /** Unique ID of this proposal. */
    private final UUID uuid;

    /** Votes received for this proposal. */
    private final Map<String, VoteMessage> votes = new HashMap<>();

    /** Has this proposal been committed?. */
    boolean isCommitted = false;

    /** Transactions that were missing from the pool when the original list arrived. */
    MissingTxIds missingTxIds;

    /**
     * The original message, which if a message is lost we may need to resend. Under what circumstances would a message go missing? Do we actually need
     * this?
     */
    ProposedTransactions originalMessage;

    /** The IDs of the transactions that should be in this proposal's block. */
    ProposedTxIds proposedTxIds;

    /** The actual transactions that should be in this proposals block, where known. */
    ProposedTxList proposedTxList;

    /** The proposer of this proposal. */
    String proposer;

    /** Flag to identify recursive calls. */
    boolean signatureRequirementCheckedInThisCall;

    /** The block produced by this proposal. */
    private Block block;

    /** The hash of the block produced by this proposal. */
    private Hash blockHash;

    /** The snapshot created by applying this proposal to state. */
    private StateSnapshot snapshot;

    /** The total voting power of all signatures received for this. */
    private Balance totalSignature = BALANCE_ZERO;

    /** The total voting power of all votes received for this. */
    private Balance totalVote = BALANCE_ZERO;


    private Proposal(UUID uuid, int height, long timestamp) {
      this.height = height;
      this.uuid = uuid;
      this.timestamp = timestamp;
      this.startTime = System.currentTimeMillis();
    }


    boolean addSignature(String pubkey, SignatureMessage signature, Balance weight) {
      if (blockHash != null && !blockHash.equals(signature.getBlockHash())) {
        logger.warn(MARKER_CONSENSUS, "Discarding signature for proposal {} for different block {}", uuid, blockHash);
        return false;
      }

      if (signatures.put(pubkey, signature) == null) {
        totalSignature = totalSignature.add(weight);
        return true;
      }
      return false;
    }


    private boolean addVote(String pubkey, VoteMessage message, Balance percent) {
      if (blockHash != null && !blockHash.equals(message.getBlockHash())) {
        logger.warn(MARKER_CONSENSUS, "Discarding vote for proposal {} for different block {}", uuid, blockHash);
        return false;
      }

      if (votes.putIfAbsent(pubkey, message) == null) {
        totalVote = totalVote.add(percent);
        return true;
      }
      return false;
    }


    private void clearVotes() {
      if (totalSignature.equalTo(BALANCE_ZERO)) {
        votes.clear();
        totalVote = BALANCE_ZERO;
        return;
      }

      logger.warn(MARKER_CONSENSUS, "Clearing votes after signature(s) have been received");
      Iterator<VoteMessage> iterator = votes.values().iterator();
      while (iterator.hasNext()) {
        VoteMessage vm = iterator.next();
        if (vm.getVoteTimestamp() != VoteMessage.NO_EXPIRY) {
          Balance voteWeighting = stateManager.getVotingPower(vm.getPublicKey());
          totalVote = totalVote.subtract(voteWeighting);
          iterator.remove();
        }
      }
    }


    /** On commit, clear out all large memory structures. The main use for a historic proposal is to resend signatures if someone out there is still voting. */
    void commit() {
      isCommitted = true;

      block = null;
      originalMessage = null;
      proposedTxIds = null;
      proposedTxList = null;
      snapshot = null;
    }


    Block getBlock() {
      return block;
    }


    Hash getBlockHash() {
      return blockHash;
    }


    int getHeight() {
      return height;
    }


    MissingTxIds getMissingTxIds() {
      return missingTxIds;
    }


    public ProposedTxIds getProposedTxIds() {
      return proposedTxIds;
    }


    ProposedTxList getProposedTxList() {
      return proposedTxList;
    }


    double getSignaturePercentage() {

      if (getMaximumVotingPower().equalTo(BALANCE_ZERO)) {
        return 1.0;
      }

      return (new BigDecimal(totalSignature.multiplyBy(100L).bigintValue())).divide(new BigDecimal(getMaximumVotingPower().bigintValue()), RoundingMode.HALF_UP)
          .doubleValue();
    }


    long getTimestamp() {
      return timestamp;
    }


    int getTxCount() {
      // If we just have votes and signatures, we do not know the transaction count
      return (proposedTxList != null) ? proposedTxList.size() : -1;
    }


    UUID getUuid() {
      return uuid;
    }


    double getVotePercentage() {
      if (getMaximumVotingPower().equalTo(BALANCE_ZERO)) {
        return 1.0;
      }

      return (new BigDecimal(totalVote.multiplyBy(100L).bigintValue())).divide(new BigDecimal(getMaximumVotingPower().bigintValue()), RoundingMode.HALF_UP)
          .doubleValue();
    }


    Balance getVotingPower() {
      return totalVote;
    }


    void setBlock(Block block) {
      this.block = block;
      this.blockHash = block.getBlockHash();
      logger.info(MARKER_CONSENSUS, "Proposal {} creates block {}", uuid, blockHash);

      // remove any votes for a different block
      Iterator<VoteMessage> i1 = votes.values().iterator();
      while (i1.hasNext()) {
        VoteMessage vm = i1.next();
        if (!vm.getBlockHash().equals(blockHash)) {
          logger.warn(MARKER_CONSENSUS, "Discarding vote from {} for different block {}", vm.getPublicKey(), vm.getBlockHash());
          Balance voteWeighting = stateManager.getVotingPower(vm.getPublicKey());
          totalVote = totalVote.subtract(voteWeighting);
          i1.remove();
        }
      }

      Iterator<SignatureMessage> i2 = signatures.values().iterator();
      while (i2.hasNext()) {
        SignatureMessage vm = i2.next();
        if (!vm.getBlockHash().equals(blockHash)) {
          logger.warn(MARKER_CONSENSUS, "Discarding signature from {} for different block {}", vm.getPublicKey(), vm.getBlockHash());
          Balance voteWeighting = stateManager.getVotingPower(vm.getPublicKey());
          totalSignature = totalSignature.subtract(voteWeighting);
          i2.remove();
        }
      }
    }


    void setMissingTxIds(MissingTxIds missingTxIds) {
      this.missingTxIds = missingTxIds;
    }


    void setProposedTxIds(ProposedTxIds proposedTxIds) {
      this.proposedTxIds = proposedTxIds;
    }


    void setProposedTxList(ProposedTxList proposedTxList) {
      this.proposedTxList = proposedTxList;
    }


    boolean signatureRequirementMet() {
      return totalSignature.greaterThanEqualTo(getTotalVotingPowerRequired());
    }


    private boolean voteRequirementMet() {
      return totalVote.greaterThanEqualTo(getTotalVotingPowerRequired());
    }
  }



  private final EventBus eventBus;

  /**
   * Proposals for future heights. We should only see these when the local node is falling behind.
   */
  private final Map<Integer, Map<UUID, Proposal>> futureProposals = new HashMap<>();

  /** Votes which are beyond current window. */
  private final List<VoteMessage> futureVotes = new ArrayList<>();

  private final Set<String> handledProposalSignatures = new HashSet<>();

  private final DefaultHashableHashComputer hashComputer = new DefaultHashableHashComputer();

  private final Pair<UUID, Proposal> lastProposalInPlay = new Pair<>(null, null);

  /** Proposals at the current height. */
  private final Map<UUID, Proposal> proposalsInPlay = new HashMap<>();

  private final StateManager stateManager;

  private final TransactionProcessor transactionProcessor;

  private long currentProposalCycle;

  private long exclusivityExpiry;

  @Value("${proposal.exclusivity.seconds:20}")
  private int exclusivitySeconds;

  private String expectedProposer;

  private Balance maximumVotingPower;

  private PriorityExecutor priorityExecutor;

  private ConsensusEventHandler proposalChosenHandler;

  private Consumer<UUID> proposalRequestHandler;

  private UUID selectedProposalId = null;

  private ConsensusEventHandler signatureRequirementMetHandler;

  private int stateHeight;

  private Balance totalVotingPowerRequired;

  private BiConsumer<PeerAddress, MissingTxIds> transactionRequestHandler;

  private ConsensusVoteEventHandler voteRequirementMetHandler;


  /**
   * ActiveProposalManager constructor.
   *
   * @param priorityExecutor     :
   * @param eventBus             :
   * @param stateManager         :
   * @param transactionProcessor :
   */
  @Autowired
  public ActiveProposalManager(PriorityExecutor priorityExecutor, EventBus eventBus, StateManager stateManager, TransactionProcessor transactionProcessor) {
    this.priorityExecutor = priorityExecutor;
    this.eventBus = eventBus;
    this.stateManager = stateManager;
    this.transactionProcessor = transactionProcessor;
  }


  /**
   * On receipt of a list of proposed transactions, the proposal cycle kicks off.
   *
   * @param message the message including the proposed transactions
   */
  synchronized void addProposedTxs(PeerAddress proposer, ProposedTransactions message, ProposedTxList txList, TransactionPool txPool) {
    UUID uuid = message.getUuid();
    int messageHeight = message.getStateHeight();
    Proposal proposal;
    if (messageHeight == stateHeight) {
      proposal = proposalsInPlay.computeIfAbsent(uuid, u -> new Proposal(uuid, message.getStateHeight(), message.getTimestamp()));
    } else if (messageHeight < stateHeight) {
      logger.warn(MARKER_CONSENSUS, "Received proposal {} from {} for height {} when height is already {}. Ignoring",
          uuid, proposer, messageHeight, stateHeight);
      return;
    } else {
      logger.warn(MARKER_CONSENSUS, "Received proposal {} from {} for future height {}. Current height is {}.", uuid, proposer, messageHeight, stateHeight);
      Map<UUID, Proposal> map = futureProposals.computeIfAbsent(messageHeight, h -> new HashMap<>());
      proposal = map.computeIfAbsent(uuid, u -> new Proposal(uuid, message.getStateHeight(), message.getTimestamp()));
    }

    if (proposal.originalMessage != null) {
      logger.warn(MARKER_CONSENSUS, "Already have proposed transactions for {} from {}. Ignoring.", uuid, proposal.proposer);
      return;
    }

    proposal.proposer = message.getProposerHostName();
    proposal.originalMessage = message;
    ProposedTxIds txIdList = message.getProposed();
    MissingTxIds unmatched = new MissingTxIds(uuid);
    if (txList == null) {
      txList = txIdList.asTxList(txPool, unmatched);
    }

    proposal.setProposedTxIds(txIdList);
    proposal.setProposedTxList(txList);
    proposal.setMissingTxIds(unmatched);

    // If we have all the transactions, we can process them now.
    if (unmatched.isEmpty()) {
      if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
        logger.info(MARKER_CONSENSUS, "Transactions for proposal {}: All {} TX are known", uuid, txList.size());
      }
      priorityExecutor.submit(Priorities.PROPOSAL, () -> processTransactions(proposal));
    } else {
      if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
        logger.info(MARKER_CONSENSUS, "Transactions for proposal {}: Missing {} out of {} TX", uuid, unmatched.size(), txList.size());
      }
      transactionRequestHandler.accept(proposer, unmatched);
    }
  }


  /**
   * addSignature.
   *
   * @param signature : the verified signature message
   *
   * @return :
   */
  synchronized boolean addSignature(SignatureMessage signature) {

    Proposal activeProposal;
    UUID proposalId = signature.getProposalId();
    long timestamp = signature.getVotes().get(0).getProposalTimestamp();
    Balance weight = stateManager.getVotingPower(signature.getSignature().getPublicKey());

    int messageMeight = signature.getHeight();

    if (messageMeight < stateHeight) {
      if (proposalId.equals(lastProposalInPlay.left())) {
        // Update 'Old' proposal with this vote for reporting.
        activeProposal = lastProposalInPlay.right();

        if ((activeProposal != null)
            && (activeProposal.getBlock() != null)
            && (activeProposal.addSignature(signature.getSignature().getPublicKey(), signature, weight))
        ) {
          sendProposalUpdate(activeProposal, UpdateType.SIGNATURE);
        }
      } else {
        // Someone is *way* behind
        logger.warn(MARKER_CONSENSUS, "Signature received for historic proposal {} at chain height {}. Current height is {}.", proposalId,
            messageMeight, stateHeight);
      }

      return false;
    } else if (messageMeight > stateHeight) {
      // Signature at future height
      Map<UUID, Proposal> map = futureProposals.computeIfAbsent(messageMeight, h -> new HashMap<>());
      activeProposal = map.computeIfAbsent(proposalId, u -> new Proposal(proposalId, messageMeight, timestamp));
    } else {
      // Signature at current height.
      activeProposal = proposalsInPlay.computeIfAbsent(proposalId, u -> new Proposal(proposalId, messageMeight, timestamp));
    }

    if (activeProposal.getProposedTxIds() == null) {
      logger.info(MARKER_CONSENSUS, "Signature received before proposal has been added:{} from {}", proposalId, signature.getSignature().getPublicKey());
      requestProposal(proposalId);
    }

    // Add all the votes that were used to justify the signature.
    for (VoteMessage vm : signature.getVotes()) {
      if (vm.verifySignature() && proposalId.equals(vm.getProposalId())) {
        VoteMessage old = activeProposal.votes.get(vm.getPublicKey());
        if (old != null) {
          // Previous vote for same proposal from same public key, make it immortal now it supports a signature.
          old.clearTimestamp();
        } else {
          // New vote supporting a signature. Make it immortal and add it.
          vm.clearTimestamp();
          addVote(vm);
        }
      }
    }

    String pubkey = signature.getSignature().getPublicKey();

    if (!activeProposal.votes.containsKey(pubkey)) {
      logger.warn(MARKER_CONSENSUS, "Signature received without vote - for proposal {} from {}", proposalId, pubkey);
    }

    if (activeProposal.addSignature(pubkey, signature, weight)) {

      if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
        logger.info(MARKER_CONSENSUS, "Signature added {}/{} for proposal {} from {}", activeProposal.signatures.size(), activeProposal.votes.size(),
            proposalId, pubkey);
      }

      activeProposal.signatureRequirementCheckedInThisCall = true;

      if (activeProposal.getBlock() != null && activeProposal.signatureRequirementMet()) {
        expectedProposer = null;
        exclusivityExpiry = 0;
        signatureRequirementMetHandler.accept(proposalId, activeProposal.getBlock().getBlockHash(), currentProposalCycle, activeProposal.getHeight(),
            activeProposal.getTimestamp());
      }

      if (activeProposal.getBlock() != null) {
        sendProposalUpdate(activeProposal, UpdateType.SIGNATURE);
      }

      return true;
    } else {
      logger.info(MARKER_CONSENSUS, "Signature not added (already present):{} from {}", proposalId, pubkey);
    }

    return false;
  }


  /**
   * addVote : Add vote to proposal.
   *
   * @param voteMessage the vote
   */
  synchronized boolean addVote(VoteMessage voteMessage) {
    Balance voteWeighting = stateManager.getVotingPower(voteMessage.getPublicKey());
    int voteHeight = voteMessage.getHeight();
    UUID proposalId = voteMessage.getProposalId();
    String pubkey = voteMessage.getPublicKey();
    long voteTimestamp = voteMessage.getVoteTimestamp();

    Proposal activeProposal;

    if (voteHeight < stateHeight) {
      if (logger.isWarnEnabled(MARKER_CONSENSUS)) {
        logger.warn(MARKER_CONSENSUS, "Vote received for historic proposal {} at height {}. Expecting {}", proposalId, voteHeight, stateHeight);
      }

      if (proposalId.equals(lastProposalInPlay.left())) {
        // Update 'Old' proposal with this vote.

        activeProposal = lastProposalInPlay.right();

        if ((activeProposal != null) && (activeProposal.getBlock() != null) && (activeProposal.addVote(pubkey, voteMessage, voteWeighting))) {
          sendProposalUpdate(activeProposal, UpdateType.VOTE);
        }
      }

      return false;
    } else if (voteHeight > stateHeight) {
      if (logger.isWarnEnabled(MARKER_CONSENSUS)) {
        logger.warn(MARKER_CONSENSUS, "Vote received for future proposal {} at height {}. Expecting {}", proposalId, voteHeight, stateHeight);
      }
      Map<UUID, Proposal> map = futureProposals.computeIfAbsent(voteHeight, h -> new HashMap<>());
      activeProposal = map.computeIfAbsent(proposalId, u -> new Proposal(proposalId, voteHeight, voteMessage.getProposalTimestamp()));
    } else {
      activeProposal = proposalsInPlay.computeIfAbsent(proposalId, u -> new Proposal(proposalId, voteHeight, voteMessage.getProposalTimestamp()));
    }

    if (voteTimestamp != currentProposalCycle && voteTimestamp != VoteMessage.NO_EXPIRY) {
      if (logger.isWarnEnabled(MARKER_CONSENSUS)) {
        if (voteTimestamp > currentProposalCycle) {
          //Store future votes until next window
          futureVotes.add(voteMessage);
          logger.warn(MARKER_CONSENSUS, "addVote:Postponed future vote:{}@{}", proposalId, voteTimestamp);
        } else {
          logger.warn(MARKER_CONSENSUS, "addVote:Refused out of date vote:{}@{}", proposalId, voteTimestamp);
        }
      }

      return false;
    }

    if (activeProposal.getProposedTxIds() == null) {
      logger.info(MARKER_CONSENSUS, "Vote received before proposal has been added:{} from {}", proposalId, pubkey);
      requestProposal(proposalId);
    }

    if (activeProposal.addVote(pubkey, voteMessage, voteWeighting)) {
      logger.info(MARKER_CONSENSUS, "Vote ADDED for proposal:{} @{} from {}", proposalId, voteTimestamp, pubkey);

      Hash xChainHash = Hash.NULL_HASH;
      if (activeProposal.getBlock() != null) {
        // X-Chain details are not de-hydrated, so no need to re-hydrate the block
        xChainHash = hashComputer.computeHash(activeProposal.getBlock().getXChainHashableObject());
      }

      if (activeProposal.getBlock() != null && activeProposal.signatureRequirementMet()) {

        if (!isProposalSelected(proposalId)) {
          logger.warn(MARKER_CONSENSUS, "Selected proposal is {}, but proposal {} has required signatures.", selectedProposalId, proposalId);
        }
        logger.info(MARKER_CONSENSUS, "Required voting power met by signatures {}/{} - proceeding {}", activeProposal.totalSignature,
            getTotalVotingPowerRequired(), proposalId);
        voteRequirementMetHandler.accept(proposalId, activeProposal.getBlock().getBlockHash(),
            activeProposal.votes.values(), xChainHash, currentProposalCycle, activeProposal.getHeight());
        return true;
      }

      if (activeProposal.getBlock() != null && activeProposal.voteRequirementMet()) {
        if (!isProposalSelected(proposalId)) {

          logger.error(MARKER_CONSENSUS, "Required voting power met {}/{} - NOT proceeding {} - selected proposal is {}", activeProposal.totalVote,
              getTotalVotingPowerRequired(), proposalId, selectedProposalId);
          return true;
        }

        logger.info(MARKER_CONSENSUS, "Required voting power met {}/{} - proceeding {}", activeProposal.totalVote, getTotalVotingPowerRequired(), proposalId);
        voteRequirementMetHandler.accept(proposalId, activeProposal.getBlock().getBlockHash(),
            activeProposal.votes.values(), xChainHash, currentProposalCycle, activeProposal.getHeight());

      } else {
        logger.debug(MARKER_CONSENSUS, "Received voting power {} for {}", activeProposal.totalVote, proposalId);
      }

      if (activeProposal.getBlock() != null) {
        sendProposalUpdate(activeProposal, UpdateType.VOTE);
      }

      return true;
    }
    logger.warn(MARKER_CONSENSUS, "Vote NOT added for proposal:{} @{} from {}", proposalId, voteHeight, pubkey);

    return false;
  }


  private void clearExistingVotesAndReselect() {
    selectedProposalId = null;
    proposalsInPlay.values().forEach(Proposal::clearVotes);

    List<Proposal> p = proposalsInPlay.values().stream().filter(x -> x.getProposedTxIds() != null).collect(Collectors.toList());
    if (p.isEmpty()) {
      logger.info(MARKER_CONSENSUS, "clearExistingVotesAndReselect:No filtered proposals to clear");
      return;
    }
    p.sort(Comparator.comparing(Proposal::getVotingPower).reversed().thenComparing(Proposal::getTxCount).reversed().thenComparing(Proposal::getUuid));
    Proposal proposal = p.get(0);
    selectedProposalId = proposal.getUuid();
    if (proposal.getBlockHash() != null) {
      proposalChosenHandler.accept(selectedProposalId, proposal.getBlockHash(), currentProposalCycle, proposal.getHeight(), proposal.getTimestamp());
    }
  }


  /**
   * Attempt to complete the handling of proposals.
   */
  public synchronized void endHandleProposal() {

  }


  /**
   * getBlock : Return block from given active proposal.
   *
   * @param proposalId the proposal's identifier
   *
   * @return the block, if known
   */
  synchronized Block getBlock(UUID proposalId) {

    Proposal proposal = proposalsInPlay.get(proposalId);

    if (proposal == null) {
      logger.error(MARKER_CONSENSUS, "No matching proposal for {}", proposalId);
      throw new IllegalArgumentException("getBlock:no matching proposal ID");
    }

    int signatureCount = proposal.signatures.size();

    if (proposal.getBlock() == null) {
      logger.error(MARKER_CONSENSUS, "Block for proposal {} is not yet determined", proposalId);
      throw new IllegalStateException("getBlock:proposal is not yet ready");
    }

    Block block = proposal.getBlock();

    //Add received signatures to the block
    if (signatureCount > 0) {
      Object[] signatures = new Object[signatureCount];
      Object[] xcSignatures = new Object[signatureCount];
      int index = 0;
      for (SignatureMessage sig : proposal.signatures.values()) {
        if (index >= signatureCount) {
          logger.error(MARKER_CONSENSUS, "Signature count changed from {} to {}", signatureCount, index);
          throw new IllegalStateException("Unexpected signature change");
        }
        signatures[index] = sig.getSignature().encode();
        xcSignatures[index] = sig.getXChainSignature().encode();
        index++;
      }
      MPWrappedArray wrappedSignatures = new MPWrappedArrayImpl(signatures);
      MPWrappedArray wrappedXCSignatures = new MPWrappedArrayImpl(xcSignatures);
      block.setSigList(wrappedSignatures);
      block.setSigListXChain(wrappedXCSignatures);
    }

    return block;
  }


  synchronized String getExpectedProposer() {
    return expectedProposer;
  }


  private synchronized Balance getMaximumVotingPower() {
    return (maximumVotingPower != null) ? maximumVotingPower : BALANCE_ZERO;
  }


  synchronized ProposedTransactions getProposal(UUID uuid) {
    Proposal proposal = proposalsInPlay.get(uuid);
    if (proposal == null) {
      for (Map<UUID, Proposal> map : futureProposals.values()) {
        proposal = map.get(uuid);
        if (proposal != null) {
          break;
        }
      }
    }
    if (proposal == null || proposal.originalMessage == null) {
      return null;
    }
    return proposal.originalMessage;
  }


  synchronized String getProposalProposer(UUID uuid) {
    Proposal proposal = proposalsInPlay.get(uuid);
    if (proposal == null) {
      for (Map<UUID, Proposal> map : futureProposals.values()) {
        proposal = map.get(uuid);
        if (proposal != null) {
          break;
        }
      }
    }
    if (proposal == null || proposal.originalMessage == null) {
      return null;
    }
    return proposal.proposer;
  }


  /**
   * Get the snapshot that resulted from applying a proposal to state.
   *
   * @param proposalId the proposal's ID
   *
   * @return the snapshot, if known
   */
  synchronized StateSnapshot getSnapshot(UUID proposalId) {
    Proposal proposal = proposalsInPlay.get(proposalId);
    if (proposal == null) {
      for (Map<UUID, Proposal> map : futureProposals.values()) {
        proposal = map.get(proposalId);
        if (proposal != null) {
          break;
        }
      }
    }
    if (proposal == null) {
      throw new IllegalArgumentException("getSnapshot: no matching proposal hash");
    }

    return proposal.snapshot;
  }


  private synchronized Balance getTotalVotingPowerRequired() {
    return (totalVotingPowerRequired != null) ? totalVotingPowerRequired : BALANCE_ZERO;
  }


  private boolean hasProposal(UUID proposalId) {
    Proposal proposal = proposalsInPlay.get(proposalId);
    if (proposal == null) {
      for (Map<UUID, Proposal> map : futureProposals.values()) {
        proposal = map.get(proposalId);
        if (proposal != null) {
          break;
        }
      }
    }
    return proposal != null && proposal.getProposedTxIds() != null;
  }


  synchronized boolean isProposalInProgress() {
    return !proposalsInPlay.isEmpty();
  }


  private boolean isProposalSelected(UUID proposalId) {
    return proposalId.equals(selectedProposalId);
  }


  private synchronized void newProposalInPlay(State state, StateSnapshot snapshot, Proposal proposal) {
    UUID proposalId = proposal.getUuid();
    proposal.snapshot = snapshot;

    // Get the transactions for the block
    TaskContext taskContext = priorityExecutor.getTaskContext(Priorities.PROPOSAL);
    boolean isGetXC = !state.getXChainSignNodes().isEmpty();
    BlockTx encodedTx = proposal.getProposedTxList().getBlockTransactions(taskContext, isGetXC, snapshot.getChainId(), proposal.getHeight());

    // Create the block
    Block block = new Block(
        snapshot.getChainId(),
        proposal.getHeight(),
        state.getLoadedHash(),
        state.getBlockHash(),
        encodedTx.getEncodedTx(),
        proposal.getTimestamp(),
        proposal.proposer,
        encodedTx.getXChainTx(),
        state.pendingEventTimeAddresses(proposal.getTimestamp()).toArray(),
        snapshot.getContractEventsEncoded(),
        snapshot.getEffectiveTxListEncoded());

    Hash blockHash = new BlockVerifier().computeHash(block);
    block.setBlockHash(blockHash);

    proposal.setBlock(block);

    if (logger.isDebugEnabled(MARKER_CONSENSUS)) {
      logger.debug(MARKER_CONSENSUS, "Adding proposal:{} now in play:{}", proposalId, proposalsInPlay.size());
    }

    // NB: Do not reverse the order of these checks. 'selectProposal' has side effects we require.
    if (selectProposal(proposal.uuid) && proposalChosenHandler != null) {

      //Work around for non-voting validation node where proposal arrives AFTER signature.
      proposal.signatureRequirementCheckedInThisCall = false;

      proposalChosenHandler.accept(proposalId, blockHash, currentProposalCycle, proposal.getHeight(), proposal.getTimestamp());

      if ((!proposal.signatureRequirementCheckedInThisCall)
          && (proposal.getBlock() != null && proposal.signatureRequirementMet())) {
        expectedProposer = null;
        exclusivityExpiry = 0;
        signatureRequirementMetHandler.accept(proposalId, blockHash, currentProposalCycle, proposal.getHeight(), proposal.getTimestamp());
      }
    }

    sendProposalUpdate(proposal, UpdateType.NEW_PROPOSAL);
  }


  /**
   * This method is deliberately not synchronized. If the block's transactions need processing, it should not be done whilst holding this manager's lock.
   *
   * @param proposal the proposal
   */
  private void processTransactions(Proposal proposal) {
    try {
      State state = stateManager.getState();
      StateSnapshot snapshot = stateManager.getState().createSnapshot();
      if (proposal.getHeight() != state.getHeight()) {
        logger.error("Cannot apply proposal {} at height {} to state at height {}", proposal.getUuid(), proposal.getHeight(), state.getHeight());
        throw new IllegalStateException("Proposal at wrong height");
      }

      logger.info(MARKER_CONSENSUS, "Applying proposal transactions for proposal {}. {}ms elapsed", proposal.getUuid(),
          System.currentTimeMillis() - proposal.startTime);

      if (transactionProcessor.processTransactions(Block.CURRENT_VERSION, priorityExecutor, snapshot, proposal.proposedTxList, proposal.getTimestamp())) {
        logger.info(MARKER_CONSENSUS, "Transactions successfully applied for proposal {}. {}ms elapsed", proposal.getUuid(),
            System.currentTimeMillis() - proposal.startTime);

        // Synchronized prior to updating the proposal's status
        newProposalInPlay(state, snapshot, proposal);
      } else {
        logger.info(MARKER_CONSENSUS, "Block contains an invalid transaction for proposal {}. {}ms elapsed", proposal.getUuid(),
            System.currentTimeMillis() - proposal.startTime);
      }
    } catch (StateSnapshotCorruptedException e) {
      logger.error(MARKER_CONSENSUS, "State for proposal {} has become corrupt", proposal.getUuid(), e);
    }
  }


  private void requestProposal(UUID proposalId) {
    // As TCP/IP is reliable, the only time we can miss a proposal message is when we have just started.
    if (proposalRequestHandler != null) {
      proposalRequestHandler.accept(proposalId);
    }
  }


  /**
   * reset ActiveProposalManager.
   *
   * @param totalVotingPowerRequired : Int : Voting threshold.
   */
  synchronized void reset(Balance totalVotingPowerRequired, Balance maximumVotingPower, int stateHeight) {
    if (selectedProposalId != null) {

      Proposal activeProposal = proposalsInPlay.get(selectedProposalId);

      if (activeProposal != null) {
        sendProposalUpdate(activeProposal, UpdateType.COMMITTED);

        // Keep last proposal for reporting purposes
        lastProposalInPlay.set(selectedProposalId, activeProposal);
      }
    }

    if (!proposalsInPlay.isEmpty() && logger.isInfoEnabled(MARKER_CONSENSUS)) {
      logger.info(MARKER_CONSENSUS, "Reset with {} proposals in play: {}", proposalsInPlay.size(), proposalsInPlay.keySet());
    }
    proposalsInPlay.clear();
    if (futureProposals.containsKey(stateHeight)) {
      proposalsInPlay.putAll(futureProposals.remove(stateHeight));
      for (Proposal p : proposalsInPlay.values()) {
        if (p.getMissingTxIds() != null && p.getMissingTxIds().isEmpty()) {
          logger.info(MARKER_CONSENSUS, "Processing transactions for proposal {} at new height {}", p.getUuid(), stateHeight);
          priorityExecutor.submit(Priorities.PROPOSAL, () -> processTransactions(p));
        }
      }
    }
    expectedProposer = null;
    this.totalVotingPowerRequired = (totalVotingPowerRequired == null ? BALANCE_ZERO : totalVotingPowerRequired);
    this.maximumVotingPower = (maximumVotingPower == null) ? BALANCE_ZERO : maximumVotingPower;
    this.selectedProposalId = null;
    this.stateHeight = stateHeight;
    this.handledProposalSignatures.clear();
    logger.info(MARKER_CONSENSUS, "Reset - ready for proposal:Required power:{}, new stateHeight:{}", this.totalVotingPowerRequired.getValue(), stateHeight);
  }


  private boolean selectProposal(UUID proposalId) {
    if (selectedProposalId == null) {
      selectedProposalId = proposalId;
      logger.info(MARKER_CONSENSUS, "Proposal {} selected for interval {}", proposalId, currentProposalCycle);
      return true;
    } else if (selectedProposalId.equals(proposalId)) {
      logger.info(MARKER_CONSENSUS, "Proposal {} RE-selected for interval {}", proposalId, currentProposalCycle);
      return true;
    } else {
      logger.info(MARKER_CONSENSUS, "Proposal {} NOT selected for interval {} - {} already selected", proposalId, currentProposalCycle, selectedProposalId);
    }
    return false;
  }


  /**
   * sendProposalUpdate.
   * <p>
   *
   * </p>
   *
   * @param activeProposal :
   * @param updateType     :
   */
  private void sendProposalUpdate(Proposal activeProposal, UpdateType updateType) {
    SignatureMessage[] signatures = activeProposal.signatures.values().toArray(new SignatureMessage[0]);
    eventBus.post(new ProposalUpdateEvent(activeProposal.getVotePercentage(),
        activeProposal.getSignaturePercentage(), signatures, updateType, activeProposal.getUuid()));
  }


  synchronized void setExpectedProposer(String expectedProposer, long timestamp) {
    String oldProposer = this.expectedProposer;
    this.expectedProposer = expectedProposer;
    if (expectedProposer != null) {
      exclusivityExpiry = timestamp + exclusivitySeconds;
      logger.info(MARKER_CONSENSUS, "Proposer {} has exclusivity until {}", expectedProposer, exclusivityExpiry);
    } else {
      exclusivityExpiry = 0;
      logger.info(MARKER_CONSENSUS, "Proposer {}'s exclusivity has been released", oldProposer);
    }
  }


  synchronized void setProposalChosenHandler(ConsensusEventHandler proposalChosenHandler) {
    this.proposalChosenHandler = proposalChosenHandler;
  }


  synchronized void setProposalRequestHandler(Consumer<UUID> proposalRequestHandler) {
    this.proposalRequestHandler = proposalRequestHandler;
  }


  synchronized void setSignatureRequirementMetHandler(ConsensusEventHandler signatureRequirementMetHandler) {
    this.signatureRequirementMetHandler = signatureRequirementMetHandler;
  }


  synchronized void setTransactionRequestHandler(BiConsumer<PeerAddress, MissingTxIds> transactionRequestHandler) {
    this.transactionRequestHandler = transactionRequestHandler;
  }


  synchronized void setVoteRequirementMetHandler(ConsensusVoteEventHandler voteRequirementMetHandler) {
    this.voteRequirementMetHandler = voteRequirementMetHandler;
  }


  /**
   * Start the process of creating a proposal and voting upon it.
   *
   * @param now the current time
   *
   * @return true if a new proposal cycle was initiated
   */
  synchronized boolean startProposalCycle(long now) {
    if (currentProposalCycle >= now) {
      logger.debug(MARKER_CONSENSUS, "Proposal cycle {} already started, ignoring cycle {}", currentProposalCycle, now);
      return false;
    }
    final boolean ret;
    currentProposalCycle = now;
    int proposalCount = proposalsInPlay.size();
    if (proposalCount > 0) {
      if (logger.isWarnEnabled(MARKER_CONSENSUS)) {
        logger.warn(MARKER_CONSENSUS, "Skipping proposal cycle, {} proposals in play", proposalCount);
      }
      clearExistingVotesAndReselect();
      ret = false;
    } else {
      // Only start the cycle if the exclusivity period has expired.
      ret = now > exclusivityExpiry;
      if (ret) {
        exclusivityExpiry = 0;
        expectedProposer = null;
      } else {
        logger.warn(MARKER_CONSENSUS, "Skipping proposal cycle as proposer {} has exclusivity for {} seconds", expectedProposer, exclusivityExpiry - now);
      }
    }

    //Reprocess any future votes
    if (!futureVotes.isEmpty()) {
      futureVotes.forEach(fv -> {
        if (fv.getVoteTimestamp() > currentProposalCycle) {
          logger.info(MARKER_CONSENSUS, "Discarding future vote at {} for {}", fv.getVoteTimestamp(), fv.getBlockHash());
        } else {
          // NB: addVote can add to futureVotes, which can cause a concurrent modification
          logger.info(MARKER_CONSENSUS, "Reprocessing future vote for:{}", fv.getBlockHash());
          addVote(fv);
        }
      });

      //Make sure futureVotes is clear - including any that have been added during reprocess.
      futureVotes.clear();
    }
    return ret;
  }


  /**
   * Check if a proposal still has missing transactions after receiving a transaction package that should have completed it. If there are no longer missing
   * transactions, apply them to a state snapshot.
   *
   * @param proposalId the proposal's ID
   * @param pool       the transaction pool
   */
  synchronized void updateProposalWithTx(UUID proposalId, TransactionPool pool) {
    Proposal proposal = proposalsInPlay.get(proposalId);
    if (proposal == null) {
      logger.error(MARKER_CONSENSUS, "Missing transactions received for proposal {}, but the proposal was not found.", proposalId);
      return;
    }

    if (!proposal.getMissingTxIds().update(priorityExecutor.getTaskContext(Priorities.PROPOSAL), proposal.getProposedTxList(), pool)) {
      logger.error(MARKER_CONSENSUS, "Missing transactions received for proposal {} did not complete proposal", proposalId);
      return;
    }

    priorityExecutor.submit(Priorities.PROPOSAL, () -> processTransactions(proposal));
  }
}
