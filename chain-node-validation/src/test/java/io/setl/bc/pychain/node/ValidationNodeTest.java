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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

import com.google.common.eventbus.EventBus;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.Encodable;
import io.setl.bc.pychain.p2p.message.PreparingProposal;
import io.setl.bc.pychain.p2p.message.StateRequest;
import io.setl.bc.pychain.p2p.message.StateResponse;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.validator.ValidatorHandler;
import io.setl.common.Balance;
import io.setl.common.CommonPy.P2PType;
import io.setl.util.PerformanceDrivenPriorityExecutor;
import io.setl.utils.TimeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Resource;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;




/**
 * Created by wojciechcichon on 21/06/2017.
 */
public class ValidationNodeTest {

  private static final Hash BLOCK_HASH = Hash.fromHex("89abcdef");

  private static final int CHAIN = 10;

  private static final int HEIGHT = 5;

  private static final String PUBKEY = "000102";

  private static final int SIGN_SIZE = 1;

  private static final List<String> SORTED_SIGNER_KEYS = new ArrayList<>();

  private static final Hash STATE_HASH = Hash.fromHex("01234567");

  private static final long TIMESTAMP = TimeUtil.unixTime();


  @Mock
  private ActiveProposalManager activeProposalManager;

  private Optional<BuildProperties> buildProperties = Optional.empty();

  private DBStore dbStore;

  @Mock
  private Environment environment;

  @Mock
  private EventBus eventBus;

  @Mock
  private KeyRepository keyRepository;

  @Mock
  private NodeConfiguration nodeConfiguration;

  @Mock
  private NodeStatus nodeStatus;

  @Mock
  private PeerManager peerManager;

  @Mock
  private PerformanceDrivenPriorityExecutor priorityExecutor;

  @Mock
  private StateManager stateManager;

  @Mock
  private TransactionVerifier transactionVerifier;

  // Testing instance, mocked `resource` should be injected here
  @Mock
  private TransactionPool txPool;

  @Resource
  private ValidationNode validationNode;

  @Mock
  private ValidatorHandler validatorHandler;

  private BlockManager blockManager = new BlockManager(
      nodeConfiguration, null, null, stateManager, peerManager, dbStore, validatorHandler, priorityExecutor, new MsgFactory());

  @Mock
  private MeterRegistry meterRegistry;

  @Test
  public void eventPreparingProposalReceived() throws Exception {
    validationNode.onApplicationEvent(null);
    StateDetail stateDetail = Mockito.mock(StateDetail.class);
    Mockito.when(stateDetail.getBlockHash()).thenReturn(BLOCK_HASH);
    Mockito.when(stateDetail.getStateHash()).thenReturn(STATE_HASH);
    Mockito.when(stateDetail.getSignSize()).thenReturn(SIGN_SIZE);
    Mockito.when(stateDetail.getHeight()).thenReturn(HEIGHT);
    Mockito.when(stateManager.getSortedSignerKeys()).thenReturn(SORTED_SIGNER_KEYS);
    Mockito.when(stateManager.getCurrentStateDetail()).thenReturn(stateDetail);
    Mockito.when(nodeConfiguration.getChainId()).thenReturn(CHAIN);
    

//    return new Object[]{chainId, P2PType.PREPARING_PROPOSAL.getId(),
//        new Object[]{timestamp, pubkey, height, currentBlockHash}};

    PeerAddress peerAddress = Mockito.mock(PeerAddress.class);
    PreparingProposal preparingProposal = new PreparingProposal(CHAIN, TIMESTAMP, PUBKEY, HEIGHT, BLOCK_HASH);

    validationNode.eventReceived(peerAddress, preparingProposal);

    Mockito.verify(activeProposalManager, Mockito.times(1)).setExpectedProposer(eq(SORTED_SIGNER_KEYS.get(0)), anyLong());
  }


  @Test
  public void eventStateRequestReceived() throws Exception {

    StateDetail stateDetail = Mockito.mock(StateDetail.class);

    Mockito.when(stateManager.getCurrentStateDetail()).thenReturn(stateDetail);
    Mockito.when(stateDetail.getStateHash()).thenReturn(STATE_HASH);
    Mockito.when(stateDetail.getHeight()).thenReturn(HEIGHT);
    Mockito.when(stateDetail.getBlockHash()).thenReturn(BLOCK_HASH);

    Mockito.when(stateManager.getState()).thenReturn(Mockito.mock(State.class));
    Mockito.when(nodeConfiguration.getChainId()).thenReturn(CHAIN);

    PeerAddress peerAddress = Mockito.mock(PeerAddress.class);
    StateRequest stateRequest = new StateRequest(4);
    validationNode.eventReceived(peerAddress, stateRequest);
    Mockito.verify(peerManager, Mockito.times(1)).send(Mockito.any(PeerAddress.class), Mockito.any(Object[].class));

  }


  @Test
  public void eventStateResponseReceived() throws Exception {

    StateDetail stateDetail = Mockito.mock(StateDetail.class);
    Mockito.when(stateDetail.getBlockHash()).thenReturn(BLOCK_HASH);
    Mockito.when(stateDetail.getStateHash()).thenReturn(STATE_HASH);
    Mockito.when(stateDetail.getHeight()).thenReturn(HEIGHT);
    Mockito.when(stateManager.getCurrentStateDetail()).thenReturn(stateDetail);
    Mockito.when(nodeConfiguration.getChainId()).thenReturn(CHAIN);
    Mockito.when(stateManager.getTotalRequiredVotingPower()).thenReturn(new Balance(51L));

    StateResponse stateResponse = new StateResponse(CHAIN, HEIGHT + 1, new Encodable[0], STATE_HASH, BLOCK_HASH, 33L);

    PeerAddress peerAddress = Mockito.mock(PeerAddress.class);
    validationNode.eventReceived(peerAddress, stateResponse);

    Mockito.verify(peerManager, Mockito.times(1)).send(Mockito.any(PeerAddress.class), Mockito.any(Object[].class));

  }


  @Test
  public void getExpectedProposerPubkey() throws Exception {

  }


  @Test
  public void sendSignature() throws Exception {

  }


  @Test
  public void sendVote() throws Exception {

  }


  /**
   * Setup.
   *
   * @throws Exception exception.
   */
  @Before
  public void setUp() throws Exception {

    SORTED_SIGNER_KEYS.add("AA");
    // Initialize mocks created above
    dbStore = Mockito.mock(DBStore.class);
    Mockito.when(dbStore.getHeight()).thenReturn(1);
    Mockito.when(dbStore.getStateHash(1)).thenReturn("ABCD");
    MockitoAnnotations.initMocks(this);

    eventBus = Mockito.mock(EventBus.class);

    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

        ((Runnable) invocationOnMock.getArgument(0)).run();
        return null;
      }
    }).when(priorityExecutor).execute((Mockito.any(Runnable.class)));

    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

        ((Runnable) invocationOnMock.getArgument(1)).run();
        return null;
      }
    }).when(priorityExecutor).submit(anyInt(), (Mockito.any(Runnable.class)));
    // Change behaviour of `resource`

    validationNode = new ValidationNode(buildProperties, environment, null, null, null, peerManager, stateManager, txPool,
        dbStore, keyRepository, nodeConfiguration, priorityExecutor, null, null, null, activeProposalManager,
        transactionVerifier, "name", null, eventBus, nodeStatus, validatorHandler, blockManager, meterRegistry
    );

//    Mockito.when(txPool.addTx(Mockito.any(PeerAddress.class), Mockito.any(MPWrappedArrayImpl.class))).thenReturn(true);
  }


  @Test
  public void transactionReceived() throws Exception {

    PeerAddress peerAddress = Mockito.mock(PeerAddress.class);
    TxPackage txPackage = new TxPackage(P2PType.TX_PACKAGE_ORIGINAL, 27, new Object[0][0]);
    validationNode.transactionReceived(peerAddress, txPackage);
//    Mockito.verify(transactionVerifier, Mockito.times(1)).verifyTxAndAddToPool(Mockito.any(PeerAddress.class), Mockito.any(MPWrappedArray.class));
//    Mockito.verify(txPool, Mockito.times(1)).addTx(Mockito.any(PeerAddress.class), Mockito.any(Txi.class));
  }

}
