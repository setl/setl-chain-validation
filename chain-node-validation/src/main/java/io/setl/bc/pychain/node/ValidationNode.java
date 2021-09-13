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
import static io.setl.bc.logging.LoggingConstants.MARKER_FATAL_ERROR;
import static io.setl.bc.logging.LoggingConstants.MARKER_MESSAGING;
import static io.setl.bc.logging.LoggingConstants.MARKER_PERFORMANCE;
import static io.setl.bc.logging.LoggingConstants.MARKER_PROCESSED;
import static io.setl.bc.logging.LoggingConstants.MARKER_STATE;
import static io.setl.bc.logging.LoggingConstants.MARKER_STORAGE;

import io.setl.bc.pychain.p2p.message.BlockIndexMessage;
import io.setl.common.CommonPy.IndexStatus;
import java.io.IOException;
import java.security.PrivateKey;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.eventbus.EventBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.setl.bc.performance.PerformanceEventListener;
import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.BlockWriter;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.block.BlockVerifier;
import io.setl.bc.pychain.block.MissingTxIds;
import io.setl.bc.pychain.block.MissingTxIds.NonceAndHash;
import io.setl.bc.pychain.block.ProposedTxList;
import io.setl.bc.pychain.consensus.ProposerElection;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.event.StateRequestEvent;
import io.setl.bc.pychain.event.TransactionListenerInternal;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.BlockCommitted;
import io.setl.bc.pychain.p2p.message.BlockFinalized;
import io.setl.bc.pychain.p2p.message.BlockRequest;
import io.setl.bc.pychain.p2p.message.EmptyProposal;
import io.setl.bc.pychain.p2p.message.ItemRequest;
import io.setl.bc.pychain.p2p.message.Message;
import io.setl.bc.pychain.p2p.message.PreparingProposal;
import io.setl.bc.pychain.p2p.message.ProposalMessage;
import io.setl.bc.pychain.p2p.message.ProposedTransactions;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import io.setl.bc.pychain.p2p.message.StateRequest;
import io.setl.bc.pychain.p2p.message.StateResponse;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.p2p.message.TxPackageResponse;
import io.setl.bc.pychain.p2p.message.UnknownMessage;
import io.setl.bc.pychain.p2p.message.VoteMessage;
import io.setl.bc.pychain.peer.BlockChainListener;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.peer.TransactionListener;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.TransactionProcessor;
import io.setl.bc.pychain.validator.ValidatorHandler;
import io.setl.bc.serialise.UUIDEncoder;
import io.setl.common.Balance;
import io.setl.common.CommonPy.ItemType;
import io.setl.common.CommonPy.P2PType;
import io.setl.crypto.MessageSigner;
import io.setl.crypto.MessageVerifier;
import io.setl.crypto.provider.SetlProvider;
import io.setl.util.ParallelTask;
import io.setl.util.PerformanceDrivenPriorityExecutor;
import io.setl.util.Priorities;
import io.setl.util.TimeBasedUuid;
import io.setl.utils.TimeUtil;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Validation node spring implementation.
 *
 * @author aanten
 */
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class
})
@Component
public class ValidationNode implements TransactionListener, TransactionListenerInternal, BlockChainListener, ApplicationListener<ApplicationReadyEvent>,
                                       HealthIndicator {

  private static final Logger logger = LoggerFactory.getLogger(ValidationNode.class);

  private static final int MAX_BLOCK_REQUEST_COUNT = 1;

  private static final int SIGNATURE_CHECK_KEEP_COUNT = 3;

  private static final int CONSENSUS_FAILED_EXIT_CODE = -1;


  /**
   * Bootstrap the spring application.
   */
  public static void main(String[] args) {

    //Pass parameter in system property to logging system
    SimpleCommandLinePropertySource ps = new SimpleCommandLinePropertySource(args);

    String logfilename = ps.getProperty("logfilename");

    System.setProperty("logfilename", logfilename == null ? "vnode" : logfilename);

    // Bootstrap spring
    new SpringApplicationBuilder(SpringConfiguration.class).headless(true).run(args);
  }


  private final ActiveProposalManager activeProposalManager;

  private final BlockReader blockReader;

  private final BlockWriter blockWriter;

  private final BuildProperties buildProperties;

  private final NodeConfiguration config;

  private final DBStore dbStore;

  private final EventBus eventBus;

  private final KeyRepository keyRepository;

  //Note messageSigner and messageVerifier resolve to single object
  private final MessageSigner messageSigner;

  private final MessageVerifier messageVerifier;

  private final MsgFactory msgFactory = new MsgFactory();

  private final String nodeName;

  private final NodeStatus nodeStatus;

  private final PeerManager peerManager;

  private final PerformanceDrivenPriorityExecutor priorityExecutor;

  private final ProposalValidator proposalValidator;

  private final DefaultSignatureMessageVerifier signatureMessageVerifier;

  private final StateManager stateManager;

  private final TransactionProcessor transactionProcessor;

  private final TransactionVerifier transactionVerifier;

  private final TransactionPool txPool;

  private final ValidatorHandler validatorHandler;

  private final BlockManager blockManager;

  @Autowired
  private ApplicationContext appContext;

  @Value("${vnode.setting.auto-adjust-block-limit}")
  private boolean autoAdjustBlockLimit;

  @Value("${chainid}")
  private int chainId;

  @Value("${vnode.setting.masterperiod}")
  private int consensusInterval;

  /**
   * Should we respond to item requests for proposals?. It was almost certainly sent, so the requesting node will get it soon anyway, and sending it more than
   * once will eat up processing time and bandwidth.
   */
  @Value("${enableRequestProposal:false}")
  private boolean enableRequestProposal = false;

  //TODO - threading should be reviewed. Possibly too many race conditions to be successfully handled in a completely free threaded way. Review.
  @Value("${vnode.setting.max-tx-per-block}")
  private int maxTxPerBlock;

  private boolean noStart;

  // TODO:Refactor nodeState to be useful
  private NodeState nodeState = NodeState.Init;

  //Map for enforcing single signed hash per height
  private Map<Integer, Hash> signatureHeightMap = new HashMap<>();

  @Value("${useFullBlocks:false}")
  private boolean useFullBlocks = false;

  @Value("${p2p.stateresponsefromblockindex:false}")
  private boolean stateresponsefromblockindex = false;

  private boolean txPoolAboveMaximum;

  private boolean txPoolBelowMinimum;

  private boolean executorAboveMaximum;

  private boolean executorBelowMinimum;

  private final Object txFlowLock = new Object();
  
  private Long proposalTime = 0L;

  private final AtomicLong proposalInterval = new AtomicLong(0);
  
  private final AtomicInteger blockSize = new AtomicInteger(0);

  private final AtomicLong blockElapsed = new AtomicLong(0);
  

  /**
   * Constructor.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public ValidationNode(
      Optional<BuildProperties> buildProperties,
      Environment env,
      ProposalValidator proposalValidator,
      MessageVerifier messageVerifier,
      MessageSigner messageSigner,
      PeerManager peerManager,
      StateManager stateManager,
      TransactionPool txPool,
      DBStore dbStore,
      KeyRepository keyRepository,
      NodeConfiguration config,
      PerformanceDrivenPriorityExecutor priorityExecutor,
      DefaultSignatureMessageVerifier signatureMessageVerifier,
      BlockWriter blockWriter,
      BlockReader blockReader,
      ActiveProposalManager activeProposalManager,
      TransactionVerifier transactionVerifier,
      @Value("${nodename}") String nodeName,
      TransactionProcessor transactionProcessor,
      EventBus eventBus,
      NodeStatus nodeStatus,
      ValidatorHandler validatorHandler,
      BlockManager blockManager,
      MeterRegistry meterRegistry
  ) {
    this.buildProperties = buildProperties.orElse(null);
    this.proposalValidator = proposalValidator;
    this.messageVerifier = messageVerifier;
    this.messageSigner = messageSigner;
    this.peerManager = peerManager;
    this.stateManager = stateManager;
    this.txPool = txPool;
    this.dbStore = dbStore;
    this.keyRepository = keyRepository;
    this.config = config;
    this.priorityExecutor = priorityExecutor;
    this.signatureMessageVerifier = signatureMessageVerifier;
    this.blockWriter = blockWriter;
    this.blockReader = blockReader;
    this.activeProposalManager = activeProposalManager;
    this.nodeName = nodeName;
    this.transactionVerifier = transactionVerifier;
    this.transactionProcessor = transactionProcessor;
    this.eventBus = eventBus;
    this.nodeStatus = nodeStatus;
    this.validatorHandler = validatorHandler;
    this.blockManager = blockManager;

    SetlProvider.install();

    activeProposalManager.setTransactionRequestHandler(this::requestTransactions);
    activeProposalManager.setProposalChosenHandler(this::sendVotes);
    activeProposalManager.setVoteRequirementMetHandler((uuid, hash, votes, xcHash, h, ts) -> sendSignatures(uuid, hash, votes, xcHash));
    activeProposalManager.setSignatureRequirementMetHandler((uuid, hash, t, h, ts) -> commit(uuid, hash, ts));
    activeProposalManager.setProposalRequestHandler(this::requestProposal);

    // Performance tuning
    txPool.addListener(new PerformanceEventListener() {

      @Override
      public void belowMinimumEvent() {
        synchronized (txFlowLock) {
          txPoolBelowMinimum = true;
          txPoolAboveMaximum = false;
          ValidationNode.this.setTxFlow();
        }
      }


      @Override
      public void aboveMaximumEvent() {
        synchronized (txFlowLock) {
          txPoolBelowMinimum = false;
          txPoolAboveMaximum = true;
          ValidationNode.this.setTxFlow();
        }
      }

    });

    priorityExecutor.addListener(new PerformanceEventListener() {

      @Override
      public void belowMinimumEvent() {
        synchronized (txFlowLock) {
          executorBelowMinimum = true;
          executorAboveMaximum = false;
          ValidationNode.this.setTxFlow();
        }
      }


      @Override
      public void aboveMaximumEvent() {
        synchronized (txFlowLock) {
          executorBelowMinimum = false;
          executorAboveMaximum = true;
          ValidationNode.this.setTxFlow();
        }
      }

    });

    meterRegistry.gauge("proposal_interval", proposalInterval);
    meterRegistry.gauge("block_size", blockSize);
    meterRegistry.gauge("block_elapsed", blockElapsed);

    this.noStart = Boolean.parseBoolean(env.getProperty("nostart"));
  }


  /**
   * Process all transactions within block against state snapshot.
   *
   * @return true if, and only if for ALL transactions the following conditions are met:- transaction is marked as updated, and can be applied to state without
   *     error. transaction is marked as not updated, and cannot be applied to state without error.
   */
  private boolean applyTransactionsAndValidateProposalBlock(Block block, StateSnapshot stateSnapshot) {
    try {
      Txi[] tx = block.getTransactions();
      return transactionProcessor.processTransactions(stateSnapshot, tx, block.getTimeStamp(), true);
    } catch (StateSnapshotCorruptedException e) {
      throw new ConsensusFailedException(e);
    }
  }


  private boolean checkForBadPublicKey(String publicKey) {
    if (stateManager.getSortedSignerKeys().contains(publicKey)) {
      logger.trace("Public key {} is a valid signer key", publicKey);
      return false;
    }
    logger.warn("Public key {} is not recognised as a signer key", publicKey);
    return true;
  }


  /**
   * Commit block, persist state and reset ready for next proposal.
   */
  private void commit(final UUID proposalId, final Hash blockHash, final long timestamp) {
    // Proposal has votes and signatures. Commit block and state
    Block block;
    long elapsed;
    synchronized (activeProposalManager) {

      logger.info(MARKER_CONSENSUS, "Begin commit of proposal {} for block {}", proposalId, blockHash);

      block = activeProposalManager.getBlock(proposalId);

      // Basic validation.
      if (block == null || block.getHeight() != stateManager.getCurrentStateDetail().getHeight()) {
        logger.warn(MARKER_CONSENSUS, "Proposal:{} has been abandoned as height has been modified by another proposal.", blockHash);
        return;
      }

      // Get snapshot related to this proposal and finalise

      StateSnapshot snapshot = activeProposalManager.getSnapshot(proposalId);

      final int newStateHeight;

      try {
        logger.debug(MARKER_CONSENSUS, "  commit,Finalising : {}", blockHash);
        newStateHeight = finaliseTransactionsAndIncrementState(block, snapshot);
      } catch (Exception e1) {
        logger.error(MARKER_FATAL_ERROR, "  ValidationNode:commit", e1);
        initiateShutdown(CONSENSUS_FAILED_EXIT_CODE);
        throw new ConsensusFailedException(e1);
      }

      //

      logger.debug(MARKER_CONSENSUS, "  commit,State persisted:{}", blockHash);

      // Save Block

      blockManager.persistBlockAsync(block, blockHash);

      // persist to Block topic message queue (if PeerManager supports).
      // Only persist blocks in normal consensus operation, not block catchup process.
      // Block catchup is handled by the 'handleBlockFinalized' handler, so does not call into the peerManager to persist.

      boolean iDidPropose = nodeName.equals(activeProposalManager.getProposalProposer(proposalId));
      peerManager.persistBlock(block, block.getChainId(), block.getHeight(), iDidPropose);

      // Clean tx pool
      txPool.bulkRemove(block);

      logger.debug(MARKER_CONSENSUS, "  commit,Pool cleaned:{}", blockHash);

      // Reset, and set possibly new total voting power
      activeProposalManager.reset(stateManager.getTotalRequiredVotingPower(), stateManager.getTotalVotingPower(), newStateHeight);
      stateManager.reset();

      //Send block committed message to peers
      peerManager.broadcast(new BlockCommitted(config.getChainId(), stateManager.getCurrentStateDetail().getBlockHeight(), blockHash));

      // logging
      elapsed = getElapsed(block.getTimeStamp());
      if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
        logger.info(MARKER_CONSENSUS, "  commit, Block:{}, txsize:{}, elapsed:{}", blockHash, block.getTransactionCount(), elapsed);
      }

      if (logger.isWarnEnabled(MARKER_PERFORMANCE)) {
        logger.warn(
            MARKER_PERFORMANCE,
            "timestamp:{}, block:{}, txsize:{}, elapsed:{}",
            System.currentTimeMillis(),
            blockHash,
            block.getTransactionCount(),
            elapsed
        );
      }

      //

      if (!proposalTime.equals(0L)) {
        proposalInterval.set(System.currentTimeMillis() - proposalTime);
      }
      proposalTime = System.currentTimeMillis();
      
      blockSize.set(block.getTransactionCount());
      blockElapsed.set(elapsed);
    }

    if (autoAdjustBlockLimit) {
      int txSize = block.getTransactionCount();
      double txPerConsensus = (double) txSize * consensusInterval * 1000 / elapsed;
      logger.info(MARKER_CONSENSUS, "Estimated max TX per consensus: {}", txPerConsensus);

      // Maximum TX per block has a 5% safety margin
      double safeMaxTxPerBlock = 0.95 * txPerConsensus;

      // Update max TX per block appropriately.
      maxTxPerBlock = (int) (0.95 * maxTxPerBlock + 0.05 * safeMaxTxPerBlock);
      logger.info(MARKER_CONSENSUS, "Updated max TX per block to {}", maxTxPerBlock);
    }
    peerManager.consensusFinished();

    if (logger.isDebugEnabled(MARKER_PROCESSED)) {
      for (String hash : block.getTxHashes()) {
        logger.debug(MARKER_PROCESSED, "Processed Tx - hash: {}", hash);
      }
    }

    // Check the schedule in-case we over ran the consensus period.
    proposalSchedule();
  }


  /**
   * Create a proposal if necessary, and broadcast.
   */
  @SuppressWarnings("squid:S135")//Loops should not contain more than a single "break" or "continue" statement
  private void createProposal(final String pubkey, PrivateKey privkey, final long timestamp) {
    //Broadcast a preparing proposal message.
    StateDetail csd = stateManager.getCurrentStateDetail();
    int height = csd.getHeight();
    PreparingProposal prepMsg = new PreparingProposal(config.getChainId(), timestamp, pubkey, height, csd.getBlockHash());
    prepMsg.sign(privkey);
    peerManager.broadcast(prepMsg);
    peerManager.consensusStarted();

    if (config.getProposalDisabled()) {
      logger.warn(MARKER_CONSENSUS, "Create proposal disabled");
    }

    boolean pendingEventsToProcess = stateManager.getState().anyPendingEventTime(timestamp);

    // Exit if there are no pending Transactions and no Contract Time Events due.
    if ((!txPool.hasAvailableTransactions()) && (!pendingEventsToProcess)) {
      logger.info(MARKER_CONSENSUS, "Proposal : No Available Transactions, No events. TxPool size = {} ", txPool.getAvailableTransactionCount());

      // An empty proposal notifies the other nodes that there is nothing to do and that they can end this proposers exclusivity period.
      sendEmptyProposal(pubkey, privkey, timestamp, csd);
      peerManager.consensusFinished();
      return;
    }

    ProposedTxList proposedTxList = txPool.createProposal(maxTxPerBlock);

    // Exit if no Events or Transactions...
    if (!pendingEventsToProcess && proposedTxList.isEmpty()) {
      logger.warn(MARKER_CONSENSUS, "Filtered transaction list empty - No proposal");

      // An empty proposal notifies the other nodes that there is nothing to do and that they can end this proposers exclusivity period.
      sendEmptyProposal(pubkey, privkey, timestamp, csd);
      peerManager.consensusFinished();
      return;
    }

    // Announce the transactions we will be processing
    UUID uuid = TimeBasedUuid.create();
    if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
      logger.info(MARKER_CONSENSUS, "Proceeding with proposal {} with {} TX out of {}", uuid, proposedTxList.size(), txPool.getAvailableTransactionCount());
    }
    ProposedTransactions message = new ProposedTransactions(config.getChainId(), height, csd.getStateHash(), timestamp,
        uuid, proposedTxList.asIdList(), nodeName, pubkey
    );
    message.sign(privkey);
    activeProposalManager.addProposedTxs(null, message, proposedTxList, txPool);
    peerManager.broadcast(message);
  }


  @Override
  public void eventReceived(PeerAddress addr, Message message) {
    //Marshal to own thread, then handle. Almost all the messages we care about are related to proposals.
    priorityExecutor.submit(Priorities.PROPOSAL, () -> handleBlockchainEvent(addr, message));
  }


  private int finaliseTransactionsAndIncrementState(final Block block, final StateSnapshot snapshot) throws
                                                                                                     IOException,
                                                                                                     StateSnapshotCorruptedException {
    if (!transactionProcessor.postProcessTransactions(snapshot, block, block.getTimeStamp())) {
      logger.error(MARKER_CONSENSUS, "Post process transactions failed");
      throw new ConsensusFailedException("postProcess:Failed");
    }

    transactionProcessor.removeProcessedTimeEvents(snapshot, block, block.getTimeStamp());

    snapshot.commit();
    State newState = snapshot.finalizeBlock(block);
    stateManager.setState(newState);
    return newState.getHeight();
  }


  private int getElapsed(long timeStamp) {
    return (int) (System.currentTimeMillis() - timeStamp * 1000);
  }


  /**
   * Get the public key of the expected proposer.
   */
  private String getExpectedProposerPubkey(final long interval, final int height, final Hash blockhash, final int signSize) {
    int sel = ProposerElection.selectProposer(blockhash, interval, height, signSize);
    List<String> snlist = stateManager.getSortedSignerKeys();
    return snlist.get(sel);
  }


  //TODO - When multiple blocks requested, although they arrive in order, once 2 or more are received, the order threads are woken is indeterminate,
  //       resulting in incorrect processing order.
  // Note : handleStateResponse only requests One block at a time.

  private void handleBlockFinalized(BlockFinalized blockFinalized, PeerAddress peerAddress) {
    // Result of block request
    // Validate block and signatures, then apply if > 50% of required signatures and
    // transactions validate

    synchronized (activeProposalManager) {
      logger.info(MARKER_CONSENSUS, "handleBlockFinalized, start.");

      Block block = blockFinalized.getBlock();

      StateDetail csd = stateManager.getCurrentStateDetail();

      if (block.getChainId() != csd.getChainId()) {
        logger.error(MARKER_CONSENSUS, "handleBlockFinalized:Wrong chain id :{}, required:{}", block.getChainId(),
            csd.getChainId()
        );
        return;
      }

      if (block.getHeight() != csd.getHeight()) {
        logger.error(MARKER_CONSENSUS, "handleBlockFinalized:Wrong height:{}, required:{}", block.getHeight(),
            csd.getHeight()
        );
        return;
      }

      if (!csd.getStateHash().equals(block.getBaseStateHash())) {
        logger.error(MARKER_CONSENSUS, "handleBlockFinalized:Wrong state hash:{}, required:{}", block.getBaseStateHash(),
            csd.getStateHash()
        );
        return;
      }

      logger.info(MARKER_CONSENSUS, "handleBlockFinalized, block height:{}", block.getHeight());

      Hash blockHash;
      try {
        blockHash = new BlockVerifier().computeHash(block);
        logger.info(MARKER_CONSENSUS, "handleBlockFinalized, block hash:{}", blockHash);
      } catch (Exception e) {
        logger.error(MARKER_CONSENSUS, "handleBlockFinalized:", e);
        return;
      }

      if (!signatureMessageVerifier.verifyBlockSignatureList(block.getSigList(), blockHash, config.getChainId(), stateManager.getTotalRequiredVotingPower(),
          stateManager::getVotingPower
      )) {
        logger.error(MARKER_CONSENSUS, "handleBlockFinalized,Insufficient signature coverage: {}", blockHash);
        return;
      }

      //TODO - Further validation of block

      final int newStateHeight;
      State state = stateManager.getState();
      StateSnapshot snapshot = state.createSnapshot();

      if (applyTransactionsAndValidateProposalBlock(block, snapshot)) {
        logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized:tx's valid : {}", blockHash);

        try {
          logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized,Finalising Block : {}", blockHash);
          newStateHeight = finaliseTransactionsAndIncrementState(block, snapshot);
          logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized, New Height {}", newStateHeight);
        } catch (Exception e1) {
          logger.error(MARKER_CONSENSUS, "  handleBlockFinalized:finaliseTransactionsAndIncrementState", e1);
          throw new ConsensusFailedException(e1);
        }

        logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized,Persisting Block : {}", blockHash);
        blockManager.persistBlock(block, blockHash);

        // Trigger a tidy of the BlockIndex (may be necessary if we are catching up a lot of blocks).
        // Do not want 'iDidPropose' parameter of this call to 'persistBlock' to be true as this could trigger a publication of blocks
        // to the 'blocks' topic at a time when this node should not be doing so.
        peerManager.persistBlock(null, block.getChainId(), block.getHeight(), false);

        logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized, Cleaning pool:{}", blockHash);
        txPool.bulkRemove(block);

        // Reset, and set possibly new total voting power
        logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized,Reset ProposalMgr");
        activeProposalManager.reset(stateManager.getTotalRequiredVotingPower(), stateManager.getTotalVotingPower(), newStateHeight);

        //Reset and persist state
        logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized,Reset StateManager");
        stateManager.reset();

        //Request next block
        logger.debug(MARKER_CONSENSUS, "  handleBlockFinalized,Request Next block");

        // Notify the Peer manager that block has been handled (please send the next one). If this returns false, then
        // this is a classic Mesh network and traditional peer-request will be made.

        if (!peerManager.getBlock(block.getChainId(),  newStateHeight)) {

          peerManager.send(peerAddress, msgFactory.blockRequest(config.getChainId(),
              stateManager.getCurrentStateDetail().getHeight(), 0
          ));

        }

        logger.info(MARKER_CONSENSUS, "  handleBlockFinalized completed : {}", blockHash);
      } else {
        logger.error(MARKER_CONSENSUS, "  handleBlockFinalized:Failed to process transactions");
      }

    } //  synchronized

  }


  /**
   * Handle incoming blockchain events.
   */
  private void handleBlockchainEvent(PeerAddress peerAddress, Message eventMessage) {

    try {
      P2PType messageType = eventMessage.getType();
      if (messageType == P2PType.UNKNOWN) {
        logger.error(MARKER_MESSAGING, "Unrecognised message type {}", ((UnknownMessage) eventMessage).getTypeId());
        return;
      }

      switch (messageType) {
        case PREPARING_PROPOSAL:
          if (nodeState == NodeState.Run) {
            handlePreparingProposal((PreparingProposal) eventMessage);
          } else {
            if (logger.isErrorEnabled(MARKER_CONSENSUS)) {
              logger.error(MARKER_CONSENSUS, "Invalid state {}, expecting {}", nodeState.name(), NodeState.Run.name());
            }
          }
          break;
        case EMPTY_PROPOSAL:
          // Proposer acknowledges their turn, but has nothing to do.
          handleEmptyProposal((EmptyProposal) eventMessage);
          break;
        case PROPOSAL:
          handleProposal(peerAddress, (ProposalMessage) eventMessage);
          break;
        case PROPOSED_TXS:
          handleProposedTransactions(peerAddress, (ProposedTransactions) eventMessage);
          break;
        case VOTE:
          handleVote((VoteMessage) eventMessage);
          break;
        case SIGNATURE:
          handleSignature((SignatureMessage) eventMessage);
          break;
        case BLOCK_FINALIZED:
          handleBlockFinalized((BlockFinalized) eventMessage, peerAddress);
          break;
        case STATE_REQUEST:
          // Respond to state request with current state, unless we are broadcasting state anyway.
          if (!peerManager.doStateBroadcast()) {
            priorityExecutor.submit(Priorities.NETWORK_WRITE, () -> handleStateRequest(peerAddress));
          }
          break;
        case STATE_RESPONSE:
          handleStateResponse((StateResponse) eventMessage, peerAddress);
          break;
        case BLOCK_INDEX:
          // Want to send `fake` state update messages based on BLOCK_INDEX
          // Could be used trigger a B/C node to run a 'catchup' operation if this was necessary.
          // Disabled by default.
          if (stateresponsefromblockindex) {
            BlockIndexMessage bm = (BlockIndexMessage)eventMessage;

            if (bm.getStatus().equals(IndexStatus.SUCCESS)) {
              handleStateResponse(new StateResponse(bm.getChainId(), bm.getBlockHeight() + 1, null, null, bm.getBlockHash(), null), null);
            }
          }
          break;
        case BLOCK_REQUEST:
          priorityExecutor.submit(Priorities.NETWORK_WRITE, () -> blockManager.handleBlockRequest((BlockRequest) eventMessage, peerAddress));
          break;
        case ITEM_REQUEST:
          handleItemRequest((ItemRequest) eventMessage, peerAddress);
          break;
        case TX_PACKAGE_RESPONSE:
          handleTxPackageResponse((TxPackageResponse) eventMessage, peerAddress);
          break;
        default:
          if (logger.isDebugEnabled(MARKER_MESSAGING)) {
            logger.debug(MARKER_MESSAGING, "{}:Ignored", messageType.name());
          }
          break;
      }

    } catch (Exception e) {
      logger.error(MARKER_MESSAGING, "Error in ValidationNode:handleBlockchainEvent", e);
    }

  }


  private void handleEmptyProposal(EmptyProposal eventMessage) {
    // Empty proposal does not go through consensus, but has the effect of releasing a proposers exclusivity period.

    if (logger.isInfoEnabled(MARKER_MESSAGING)) {
      logger.info(MARKER_MESSAGING, "Empty proposal from {}", eventMessage.getNodeName());
    }
    if (eventMessage.verifySignature() && eventMessage.getPublicKey().equals(activeProposalManager.getExpectedProposer())) {
      activeProposalManager.setExpectedProposer(null, 0);
      peerManager.consensusFinished();
    }
  }


  private void handleItemRequest(ItemRequest eventMessage, PeerAddress peerAddress) {
    logger.info(MARKER_MESSAGING, "Item request from {} for {}", peerAddress, eventMessage.getItemType());
    ItemType itemRequested = eventMessage.getItemType();
    if (ItemType.PROPOSAL == itemRequested) {
      UUID uuid = UUIDEncoder.decode((byte[]) eventMessage.getId());
      if (!enableRequestProposal) {
        logger.info(MARKER_MESSAGING, "ITEM_REQUEST for proposal {} ignored, as disabled", uuid);
        return;
      }
      ProposedTransactions proposal = activeProposalManager.getProposal(uuid);
      if (proposal != null) {
        peerManager.send(peerAddress, proposal);
        //Expensive operation - warn level
        logger.warn(MARKER_MESSAGING, "ITEM_REQUEST:Sent proposal:{} to {}", uuid, peerAddress);
        return;
      }
      logger.warn(MARKER_MESSAGING, "Could not Send ITEM_REQUEST proposal:{}", uuid);
      return;
    }

    if (ItemType.PROPVOTES == itemRequested) {
      logger.info(MARKER_MESSAGING, "PROPVOTES_REQUEST IGNORED:{}", eventMessage);
      return;
    }

    if (ItemType.TRANSACTIONS == itemRequested) {
      MissingTxIds missingTxIds = new MissingTxIds(new MPWrappedArrayImpl((Object[]) eventMessage.getId()));
      String[] addresses = missingTxIds.getAddresses();
      final Object[][] encodedTx = new Object[missingTxIds.size()][];
      AtomicInteger index = new AtomicInteger(0);
      AtomicBoolean allGood = new AtomicBoolean(true);
      logger.info(MARKER_MESSAGING, "ITEM_REQUEST:{} transactions requested for {} addresses", missingTxIds.size(), addresses.length);

      ParallelTask.process(priorityExecutor.getTaskContext(Priorities.PROPOSAL), 1, addresses.length, i -> {
        if (!allGood.get()) {
          return;
        }
        String address = addresses[i];
        Set<NonceAndHash> nhs = missingTxIds.getMissing(address);
        for (NonceAndHash nh : nhs) {
          Txi txi = txPool.getTx(address, nh.getNonce(), nh.getHash());
          if (txi == null) {
            // If we don't have all the transactions, we return an empty array to indicate we cannot fulfil the request. This could be because consensus
            // has already committed to the block containing the transaction, so it is gone from the pool, or it could be because the block was invalid.
            logger.warn(MARKER_MESSAGING, "Unable to provide transaction {}. Returning failure indicator.", nh);
            allGood.set(false);
            return;
          }
          encodedTx[index.getAndIncrement()] = txi.encodeTx();
        }
      });

      if (allGood.get()) {
        peerManager.send(peerAddress, msgFactory.txPackageResponse(chainId, missingTxIds.getUuid(), encodedTx));
      } else {
        peerManager.send(peerAddress, msgFactory.txPackageResponse(chainId, missingTxIds.getUuid(), new Object[0][]));
      }
      return;
    }

    logger.info(MARKER_MESSAGING, "ITEM_REQUEST IGNORED:{}", eventMessage);
  }


  /**
   * Handle a preparing proposal message from another peer.
   *
   * @param message The message
   */
  private void handlePreparingProposal(PreparingProposal message) {

    StateDetail st0 = stateManager.getCurrentStateDetail();

    long interval = message.getTimestamp();

    if (st0.getHeight() != message.getHeight()) {
      logger.error(MARKER_CONSENSUS, "Preparing proposal:Invalid height:{}", message.getHeight());
      return;
    }

    if (st0.getBlockHash().equals(message.getBlockHash())) {
      String proposer = getExpectedProposerPubkey(interval, st0.getHeight(), st0.getBlockHash(), st0.getSignSize());
      activeProposalManager.setExpectedProposer(proposer, interval);
      if (proposer.equals(message.getPublicKey())) {
        logger.info(MARKER_CONSENSUS, "Preparing proposal: Ok by {}", proposer);
        peerManager.consensusStarted();
      } else {
        if (logger.isErrorEnabled(MARKER_CONSENSUS)) {
          logger.error(MARKER_CONSENSUS, "Preparing proposal:Invalid proposer {} (expecting {})",
              message.getPublicKey(), proposer
          );
        }
      }

    } else {
      if (logger.isErrorEnabled(MARKER_CONSENSUS)) {
        logger.error(MARKER_CONSENSUS, "Preparing proposal:Invalid blockhash Local:{} Incoming:{} from:{}", st0.getBlockHash(), message.getBlockHash(),
            message.getPublicKey()
        );
      }
    }
  }


  /**
   * Handle a received proposal message.
   */
  private void handleProposal(PeerAddress proposer, final ProposalMessage proposalMessage) {
    logger.error(MARKER_CONSENSUS, "PROPOSAL message received from {}, but this message type is no longer in use.", proposer);
  }


  private void handleProposedTransactions(PeerAddress peerAddress, ProposedTransactions eventMessage) {
    if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
      logger.info(MARKER_CONSENSUS, "Received proposed transactions for proposal {} from {}", eventMessage.getUuid(), peerAddress);
    }
    if (checkForBadPublicKey(eventMessage.getPublicKey())) {
      logger.warn(MARKER_CONSENSUS, "Received proposed transactions from invalid address {}", peerAddress);
      return;
    }
    if (!eventMessage.verifySignature()) {
      logger.warn(MARKER_CONSENSUS, "Proposed transactions for {} from {} have invalid signature", eventMessage.getUuid(), peerAddress);
    }
    activeProposalManager.addProposedTxs(peerAddress, eventMessage, null, txPool);
  }


  /**
   * Handle received, or locally generated signature message.
   */
  private void handleSignature(final SignatureMessage va) {
    if (logger.isDebugEnabled(MARKER_CONSENSUS)) {
      logger.debug(MARKER_CONSENSUS, "SIGNATURE:height={} proposal={} from:{}", va.getHeight(), va.getBlockHash(), va.getPublicKey());
    }

    if (checkForBadPublicKey(va.getPublicKey())) {
      return;
    }

    // Validate votes signature
    boolean verified = va.verifySignature();
    if (verified) {
      if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
        logger.info(MARKER_CONSENSUS, "SIGNATURE:Verified OK:{} from:{}", va.getBlockHash(), va.getPublicKey());
      }
      activeProposalManager.addSignature(va);
    } else {
      if (logger.isErrorEnabled(MARKER_CONSENSUS)) {
        logger.error(MARKER_CONSENSUS, "SIGNATURE:Verify failed:{} from:{}", va.getBlockHash(), va.getPublicKey());
      }
    }
  }


  /**
   * Reply to state request.
   *
   * @param addr The address requiring a reply.
   */
  private void handleStateRequest(PeerAddress addr) {

    StateDetail csd = stateManager.getCurrentStateDetail();
    State state = stateManager.getState();

    final XChainDetails[] xchainDetails;

    if (state.getXChainSignNodes() == null || state.getXChainSignNodes().isEmpty()) {
      xchainDetails = new XChainDetails[0];
    } else {
      Collection<XChainDetails> signingNodes = state.getXChainSignNodes().values();
      xchainDetails = signingNodes.toArray(new XChainDetails[0]);
    }

    // Only the nodes with private keys accessible via the public keys can be counted for voting power
    Long totalVotingPower = stateManager.getSortedSignerKeys().stream()
        .filter(pk -> keyRepository.getSignNodePrivateKeySource().getPrivateKey(pk) != null)
        .map(stateManager::getVotingPower)
        .mapToLong(Balance::longValue)
        .sum();
    
    peerManager.send(addr, msgFactory.stateResponse(config.getChainId(), csd.getHeight(), xchainDetails, csd.getStateHash(), csd.getBlockHash(), totalVotingPower));
  }


  /**
   * Handle a state response message from another peer.
   *
   * @param message The state response payload
   * @param addr    the source of the state
   */
  private void handleStateResponse(StateResponse message, PeerAddress addr) {

    // Get height from peer State Response
    int messageHeight = message.getHeight();

    // Inform UpdateSocketHandler of this event.
    eventBus.post(new StateRequestEvent(TimeUtil.unixTimeDouble(), (addr != null ? addr.getAddressString() : ""), messageHeight, TimeUtil.unixTimeDouble() + 30.0,
        message.getChainId(), message.getLastBlockHash().toHexString()
    ));

    // Process response.
    synchronized (activeProposalManager) {

      // Get details of current state
      StateDetail currentStateDetail = stateManager.getCurrentStateDetail();

      long currentTimestamp = System.currentTimeMillis();
      
      Long ownVotingPower = keyRepository.getOwnedPublicKeys().stream()
          .map(stateManager::getVotingPower)
          .mapToLong(Balance::longValue)
          .sum();

      // Make sure that this node's height status is up to date and that peer details are fresh and updated.
      nodeStatus.dropStalePeers(currentTimestamp);
      nodeStatus.setHeight(currentStateDetail.getHeight());

      if (addr != null) {
        nodeStatus.addPeerDetails(addr.getAddressString(), messageHeight, currentTimestamp);
      }

      nodeStatus.setLastStatusTime(currentTimestamp);

      if (addr != null) {
        nodeStatus.recordVotingPower(addr.getAddressString(), currentTimestamp, message.getVotingPower());
      }

      nodeStatus.setTotalRequiredVotingPower(stateManager.getTotalRequiredVotingPower().longValue());
      nodeStatus.setOwnVotingPower(ownVotingPower);

      // How many blocks behind are we?
      int behind = messageHeight - currentStateDetail.getHeight();

      if (behind != 0) {
        // We are behind (or ahead)

        if (activeProposalManager.isProposalInProgress()) {

          if (behind == 1) {
            // Probably, just a lag in committing the new proposal...

            logger.info(MARKER_CONSENSUS, "State is one block behind and proposal is in progress");
            return;

          } else if (behind > 1) {
            // Badly behind. Abandon proposal : We will need to do a block catchup.

            logger.info(MARKER_CONSENSUS, "State is {} blocks behind. Abandoning in progress proposal.", behind);
            activeProposalManager.reset(stateManager.getTotalRequiredVotingPower(), stateManager.getTotalVotingPower(), currentStateDetail.getHeight());

          } else {
            // We are ahead of the other node.
            logger.error(MARKER_CONSENSUS, "State is ahead and proposal is in progress.");
            return;
          }

        } // isProposalInProgress

        logger.warn(MARKER_CONSENSUS, "Network height:{} local height:{}", messageHeight, currentStateDetail.getHeight());

        if (behind > 0) {
          // At this point, we are behind and either, there is no proposal, or we are behind by more than One block.
          // We will request the missing Block(s)

          // How many blocks ?
          int blockCount = messageHeight - currentStateDetail.getHeight();

          // Apply maximum request size
          // Gets one at a time as it requests the next block in the block-handled code.
          if (blockCount > MAX_BLOCK_REQUEST_COUNT) {
            blockCount = MAX_BLOCK_REQUEST_COUNT;
          }

          logger.info(MARKER_CONSENSUS, "Requesting {} of {} blocks", behind, blockCount);

          // Request the blocks from the network.

          // Try to request blocks from the Peer Manager.
          // If this return 'True', then the PeerManager supports a single request to get all Blocks
          // from a given height (e.g. Kafka). Traditional Mesh network does not support this.

          if (!peerManager.getBlock(config.getChainId(), currentStateDetail.getHeight())) {

            // Use traditional request form :

            peerManager.send(addr, msgFactory.blockRequest(config.getChainId(),
                currentStateDetail.getHeight(), blockCount - 1
            ));

          }

        }
      } else {
        // We are not behind (or are ahead)

        // Check if state is up to date
        Hash messageLastStateHash = message.getLastStateHash();
        Hash messageLastBlockHash = message.getLastBlockHash();

        // Validate message Blockhash
        if (messageLastBlockHash != null) {
          if (currentStateDetail.getBlockHash().equals(messageLastBlockHash)) {
            logger.debug(MARKER_CONSENSUS, "Network blockhash ok: {}", messageLastBlockHash);
          } else {
            logger.error(MARKER_CONSENSUS, "Network blockhash {}!=local block hash{}", messageLastBlockHash, currentStateDetail.getBlockHash()
            );
          }
        }

        // Validate message Statehash
        if (messageLastStateHash != null) {
          if (currentStateDetail.getStateHash().equals(messageLastStateHash)) {
            logger.debug(MARKER_CONSENSUS, "Network statehash ok: {}", messageLastStateHash);
          } else {
            // Someone's state hash is incorrect !
            logger.error(MARKER_CONSENSUS, "Network statehash {}!=local state hash{}", messageLastStateHash, currentStateDetail.getStateHash()
            );
            //TODO - handle this - rewind or flag terminal exception?
          }
        }
      }
    }


  }


  private void handleTxPackageResponse(TxPackageResponse eventMessage, PeerAddress peerAddress) {
    logger.info("Received {} TX for proposal {} from {}", eventMessage.size(), eventMessage.getUuid(), peerAddress);
    UUID proposalId = eventMessage.getUuid();

    transactionVerifier.verifyTxsAndAddToPool(peerAddress, eventMessage, proposalId).thenAccept(b -> {
      activeProposalManager.updateProposalWithTx(proposalId, txPool);
    });
  }


  /**
   * Handle received vote message.
   *
   * @param va The vote
   */
  private void handleVote(final VoteMessage va) {
    if (logger.isDebugEnabled(MARKER_CONSENSUS)) {
      logger.debug(MARKER_CONSENSUS, "handleVote:height={} proposal={} from:{}", va.getHeight(), va.getBlockHash(), va.getPublicKey());
    }

    if (checkForBadPublicKey(va.getPublicKey())) {
      return;
    }

    // Validate votes signature
    boolean verified = va.verifySignature();
    if (verified) {
      if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
        logger.info(MARKER_CONSENSUS, "VOTE:Verified OK:{} from:{}", va.getBlockHash(), va.getPublicKey());
      }
      activeProposalManager.addVote(va);
    } else {
      if (logger.isErrorEnabled(MARKER_CONSENSUS)) {
        logger.error(MARKER_CONSENSUS, "VOTE:Verify failed:{} from:{}", va.getBlockHash(), va.getPublicKey());
      }
    }
  }


  @Override
  public Health health() {
    nodeStatus.setLastVerificationResult(validatorHandler.getIsValidated().get());
    nodeStatus.setLastVerificationTime(validatorHandler.getResultTime().get());
    
    Builder builder = Health.status(nodeStatus.isNodeUp(false) ? Status.UP : Status.DOWN);
    builder.withDetail("nodeStatus", nodeStatus);
    return builder.build();
  }


  public void initiateShutdown(int exitCode) {
    stopValidationNode();
    SpringApplication.exit(appContext, () -> exitCode);
    System.exit(exitCode);
  }


  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    // Generated using the FIGlet "Doom" font.
    System.out.println("\n\n\n"
        + " _____ _____ _____ _         _   _       _ _     _       _   _               _   _           _\n"
        + "/  ___|  ___|_   _| |    _  | | | |     | (_)   | |     | | (_)             | \\ | |         | |\n"
        + "\\ `--.| |__   | | | |   (_) | | | | __ _| |_  __| | __ _| |_ _  ___  _ __   |  \\| | ___   __| | ___\n"
        + " `--. \\  __|  | | | |       | | | |/ _` | | |/ _` |/ _` | __| |/ _ \\| '_ \\  | . ` |/ _ \\ / _` |/ _ \\\n"
        + "/\\__/ / |___  | | | |_____  \\ \\_/ / (_| | | | (_| | (_| | |_| | (_) | | | | | |\\  | (_) | (_| |  __/\n"
        + "\\____/\\____/  \\_/ \\_____(_)  \\___/ \\__,_|_|_|\\__,_|\\__,_|\\__|_|\\___/|_| |_| \\_| \\_/\\___/ \\__,_|\\___|\n"
        + "                                                                                                    "
        + "\n\n\n");
    if (buildProperties != null) {
      System.out.println("Version: " + buildProperties.getVersion() + ", built at " + DateTimeFormatter.ISO_INSTANT.format(buildProperties.getTime()) + "\n");
    }

    try {
      startValidationNode();
    } catch (DBStoreException e) {
      logger.error("Failed to start validation node", e);
      SpringApplication.exit(appContext, () -> 1);
    }
  }


  /**
   * Proposal check - Schedule every 5th second.
   */
  @Scheduled(cron = "${vnode.setting.mastercron}")
  private void proposalSchedule() {
    // This can be called multiple times, if a call is missed due to threads being busy.
    long now = ProposerElection.curInterval(TimeUtil.unixTime(), consensusInterval);
    priorityExecutor.submit(Priorities.PROPOSAL, () -> proposalScheduleImpl(now));
  }


  private synchronized void proposalScheduleImpl(long now) {
    if (!nodeState.isRunning()) {
      return;
    }

    // Determine interval
    if (!activeProposalManager.startProposalCycle(now)) {
      logger.warn(MARKER_CONSENSUS, "Proposal cycle could not start:{}", now);
      return;
    }

    // Determine which node should be proposing
    StateDetail st0 = stateManager.getCurrentStateDetail();
    int height = st0.getHeight();
    int sel = ProposerElection.selectProposer(st0.getBlockHash(), now, height, st0.getSignSize());

    // Determine if this node has access to private key for the proposing node
    List<String> signNodeList = stateManager.getSortedSignerKeys();
    String pubkey = signNodeList.get(sel);

    PrivateKey privkey = keyRepository.getSignNodePrivateKeySource().getPrivateKey(pubkey);

    if (logger.isInfoEnabled(MARKER_CONSENSUS)) {
      long pendingTime = stateManager.getState().nextPendingEventTime();
      boolean pendingEventsToProcess = stateManager.getState().anyPendingEventTime(pendingTime); // pendingEventTimeAddresses

      if (pendingEventsToProcess) {
        logger.info(MARKER_CONSENSUS, "  My State : Height {}, {} TxPool size = {}, Peers = {}, Events = {} at {}.  ", height,
            ((privkey != null) ? "LOCAL,  " : "REMOTE,"),
            txPool.getAvailableTransactionCount(),
            peerManager.getActiveConnectionSnapshot().size(),
            stateManager.getState().pendingEventTimeAddresses(pendingTime).size(), TimeUtil.unixTimeToString(pendingTime)
        );
      } else {
        logger.info(MARKER_CONSENSUS, "  My State : Height {}, {} TxPool size = {}, Peers = {}. Events = None.  ", height, ((privkey != null) ? "LOCAL,  " :
                "REMOTE,"),
            txPool.getAvailableTransactionCount(),
            peerManager.getActiveConnectionSnapshot().size()
        );
      }
    }

    // If access to proposers private key, then create proposal
    if (privkey != null) {
      logger.info(MARKER_CONSENSUS, "Proposal due LOCAL height:{} by pubkey:{}", height, pubkey);

      // Sync on the proposal manager as we do not want to be creating a proposal whilst another is processing.
      synchronized (activeProposalManager) {
        createProposal(pubkey, privkey, now);
      }
      logger.info(MARKER_CONSENSUS, "Proposal created LOCAL height:{} by pubkey:{}", height, pubkey);
      return;
    }

    logger.info(MARKER_CONSENSUS, "Proposal due REMOTE height:{} by pubkey:{}", height, pubkey);
  }


  /**
   * Track signatures sent against height, disallowing signing different hashes at a given height.
   *
   * @param height    The height wishing to be signed
   * @param blockHash The hash wishing to be signed
   *
   * @return true if this hash can be signed
   */
  private boolean registerSignatureSent(int height, Hash blockHash) {

    synchronized (activeProposalManager) {
      //Maintain map size
      signatureHeightMap.remove(height - SIGNATURE_CHECK_KEEP_COUNT);
      Hash current = signatureHeightMap.putIfAbsent(height, blockHash);
      if (current == null || current.equals(blockHash)) {
        return true;
      }
      logger.error(MARKER_CONSENSUS, "Error:Abandoned signature process - Already signed {}", current);
      return false;
    }
  }


  @SuppressWarnings("squid:S1181") // I do want to catch and log all failures
  @Scheduled(cron = "${vnode.setting.peer-request-cron}")
  private void requestPeerStatus() {
    try {
      // Why cron a State request, when we could just cron a state response ? NPP - 20/04/21
      // doStateBroadcast() added to PeerManager Interface for backwards compatibility with
      // previous versions, KafkaPeerManager returns true.

      if (peerManager.doStateBroadcast()) {
        // This can trigger before state is loaded, so check for state before triggering the StateResponse (indirectly).
        if (stateManager.getState() != null) {
          // Send State response.
          handleStateRequest(null);
        }
      } else {
        peerManager.broadcast(new StateRequest(config.getChainId()));
      }

    } catch (Throwable thrown) {
      logger.error(MARKER_MESSAGING, "Failed to request peer status", thrown);
    }
  }


  private void requestProposal(UUID proposalId) {
    logger.info(MARKER_CONSENSUS, "Requesting proposal {}", proposalId);
    Message msg = new ItemRequest(config.getChainId(), ItemType.PROPOSAL, UUIDEncoder.encode(proposalId));
    peerManager.broadcast(msg);
  }


  private void requestTransactions(PeerAddress address, MissingTxIds txIds) {
    ItemRequest itemRequest = new ItemRequest(chainId, ItemType.TRANSACTIONS, txIds);
    peerManager.send(address, itemRequest);
  }


  /**
   * Send a message saying that the proposal was empty, with no transactions and no events.
   *
   * @param pubkey    the public key
   * @param privkey   the signing key
   * @param timestamp the timestamp
   * @param csd       the current state
   */
  private void sendEmptyProposal(final String pubkey, final PrivateKey privkey, final long timestamp, StateDetail csd) {

    EmptyProposal emptyMsg = new EmptyProposal(config.getChainId(), csd.getStateHash(), csd.getHeight(), timestamp, nodeName, pubkey);
    emptyMsg.sign(privkey);
    peerManager.broadcast(emptyMsg);

    // Trigger a check of cached Blocks to persist (since this was another proposal cycle, even though there was nothing to do)
    // if 'iDidPropose' is true, then this will trigger a check of the 'Blocks to persist' cache (which we do want at this point - it's why we are calling).
    peerManager.persistBlock(null, config.getChainId(), csd.getHeight(), true);

    //
    if (!proposalTime.equals(0L)) {
      proposalInterval.set(System.currentTimeMillis() - proposalTime);
    }

    proposalTime = System.currentTimeMillis();
    blockSize.set(0);
    blockElapsed.set(0);
  }


  /**
   * Create a signature message and send.
   */
  private void sendSignature(
      final UUID proposalId, final Hash proposalHash, Collection<VoteMessage> votes, Hash xcHash, final String pubkey,
      final int height
  ) {
    PrivateKey privkey = keyRepository.getSignNodePrivateKeySource().getPrivateKey(pubkey);
    SignatureMessage message = new SignatureMessage(config.getChainId(), height, proposalId, proposalHash, votes, "XC", xcHash, pubkey);
    message.sign(privkey);

    logger.info(MARKER_CONSENSUS, "Sending Signature:{} using local pubkey:{}", proposalHash, pubkey);
    peerManager.broadcast(message);
    // Make sure signature is counted, and handled appropriately
    handleSignature(message);
  }


  private void sendSignatures(final UUID proposalId, final Hash blockHash, Collection<VoteMessage> votes, Hash xcHash) {

    StateDetail csd = stateManager.getCurrentStateDetail();
    int height = csd.getHeight();

    //Can this hash be signed at this height? We can only sign once at each height!
    if (registerSignatureSent(height, blockHash)) {
      keyRepository.getOwnedPublicKeys().forEach(ownedpubkey -> sendSignature(proposalId, blockHash, votes, xcHash, ownedpubkey, height));
    }
  }


  /**
   * Create a vote message and send.
   */
  private void sendVote(
      final UUID proposalId, final long proposalTimestamp, final Hash blockHash, final long voteTimestamp, final int height,
      final String pubkey
  ) {

    PrivateKey privkey = keyRepository.getSignNodePrivateKeySource().getPrivateKey(pubkey);
    VoteMessage voteMessage = new VoteMessage(config.getChainId(), proposalId, proposalTimestamp, blockHash, height, voteTimestamp, pubkey);
    voteMessage.sign(privkey);
    logger.info(MARKER_CONSENSUS, "Sending vote:{} using local pubkey:{}", blockHash, pubkey);
    peerManager.broadcast(voteMessage);

    // Make sure our vote is counted, and handled appropriately
    activeProposalManager.addVote(voteMessage);
  }


  private void sendVotes(final UUID proposalId, final Hash blockHash, final long voteTimestamp, final int height, final long proposalTimestamp) {
    keyRepository.getOwnedPublicKeys().forEach(pk -> sendVote(proposalId, proposalTimestamp, blockHash, voteTimestamp, height, pk));
  }


  /**
   * Initialise the node, and start all required services.
   */
  private synchronized void startValidationNode() throws DBStoreException {
    if (noStart) {
      logger.warn("NOSTART parameter specified - validation node NOT started");
      return;
    }
    if (nodeState.isRunning()) {
      return;
    }

    logger.info("Starting validation node");

    // Initialise state at last persisted hash
    // NPP : Re-Code Init to work down the State height, ignoring the error generated when a hash has been recorded, but the state is unloadable.

    int height = dbStore.getHeight();
    String hash;

    while (true) {
      hash = dbStore.getStateHash(height);

      if (hash != null) {
        try {
          stateManager.init(hash);
          break;
        } catch (Exception e) { // Needs to be Exception, not ConsensusFailedException.
          logger.error("Failed to init State, Height {}, Hash {}", height, hash);
          logger.error("Error details : ", e);
        }
      }

      if (height <= 0) {
        throw new AssertionError("Could not get root hash for height. Server cannot start.");
      }

      logger.error(MARKER_STORAGE, "No hash stored for height:{} - REWIND", height);
      height--;
    }

    // Reset the proposal manager with required voting power
    activeProposalManager.reset(stateManager.getTotalRequiredVotingPower(), stateManager.getTotalVotingPower(), height);

    // Initialise network
    peerManager.init(config.getChainId(), config.getUniqueNodeIdentifier(), true);

    //Provide health with node identifier
    nodeStatus.setUniqueNodeIdentifier(config.getUniqueNodeIdentifier());

    // Listen for transactions
    peerManager.addTransactionListener(this);

    // Listen for all other blockchain messages
    peerManager.addListener(this);

    peerManager.start();
    nodeState = NodeState.Run;

    keyRepository.updateOwnedPublicKeys();
    validatorHandler.setHeight(height);
    validatorHandler.start();

    logger.info("Started validation node");
  }


  private synchronized void stopValidationNode() {
    nodeState = NodeState.Stopped;
    logger.info("Current node state: {}", nodeState);

    try {
      dbStore.close();
      logger.info("DB store closed.");
    } catch (DBStoreException e) {
      logger.error("Could not close DB store.", e);
    }

    activeProposalManager.endHandleProposal();
    logger.info("Active Proposal Manager stopped handling proposals.");

    peerManager.stop();
    logger.info("Peer manager stopped.");

    keyRepository.updateOwnedPublicKeys();
    logger.info("Updated owned public keys.");

    validatorHandler.stop();
    priorityExecutor.shutdown();
  }


  @Override
  public void transactionReceived(final PeerAddress addr, final TxPackage va) {
    // Add to the current transaction pool if not already present
    if (logger.isTraceEnabled(MARKER_MESSAGING)) {
      logger.trace(MARKER_MESSAGING, "Transactions received:{}", va.size());
    }

    // Add transactions to pool
    transactionVerifier.verifyTxsAndAddToPool(addr, va, null);
  }


  public void transactionReceivedInternal(final TxPackage va) {
    peerManager.broadcast(new TxPackage(P2PType.TX_PACKAGE_FORWARD, va));
    transactionReceived(null, va);
  }


  private void setTxFlow() {
    if (txPoolBelowMinimum && executorBelowMinimum) {
      peerManager.resume();
    }

    if (txPoolAboveMaximum || executorAboveMaximum) {
      peerManager.pause();
    }
  }

}
