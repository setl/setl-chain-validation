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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import io.setl.bc.pychain.BlockReader;
import io.setl.bc.pychain.BlockWriter;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.BlockRequest;
import io.setl.bc.pychain.peer.PeerAddress;
import io.setl.bc.pychain.peer.PeerManager;
import io.setl.bc.pychain.validator.ValidatorHandler;
import io.setl.util.PriorityExecutor;


@RunWith(SpringRunner.class)
public class BlockManagerTest {

  @Mock
  private NodeConfiguration nodeConfiguration;

  @Mock
  private BlockReader blockReader;

  @Mock
  private BlockWriter blockWriter;

  @Mock
  private StateManager stateManager;

  @Mock
  private PeerManager peerManager;

  @Mock
  private DBStore dbStore;

  @Mock
  private ValidatorHandler validatorHandler;

  @Mock
  private PriorityExecutor priorityExecutor;

  @Mock
  private MsgFactory msgFactory;

  @Mock
  private Block block;

  @Mock
  private Hash hash;

  @Mock
  private BlockRequest blockRequest;

  @Mock
  private PeerAddress peerAddress;

  @Mock
  private StateDetail stateDetail;

  private BlockManager blockManager;

  private final int chainId = 32;
  private final int blockHeight = 2;
  private final long blockTimestamp = 45667L;
  private final String hashHex = "I might just be a hex string";
  private final int stateHeight = 2;


  @Before
  public void setup() {
    blockManager = new BlockManager(
        nodeConfiguration, blockReader, blockWriter, stateManager, peerManager, dbStore, validatorHandler, priorityExecutor, msgFactory);
    when(block.getHeight()).thenReturn(blockHeight);
    when(block.getTimeStamp()).thenReturn(blockTimestamp);
    when(hash.toHexString()).thenReturn(hashHex);
    when(nodeConfiguration.getChainId()).thenReturn(chainId);
    when(blockRequest.getChainId()).thenReturn(chainId);
    when(stateManager.getCurrentStateDetail()).thenReturn(stateDetail);
    when(stateDetail.getHeight()).thenReturn(stateHeight);
  }


  @Test
  public void testPersistBlock() throws Exception {
    blockManager.persistBlock(block, hash);
    verify(dbStore).setBlockHash(blockHeight, hashHex, blockTimestamp);
    verify(blockWriter).writeBlock(block, hash);
    verify(validatorHandler).setHeight(blockHeight);
  }


  @Test
  public void handleBlockRequestIncorrectChainId() {
    when(blockRequest.getChainId()).thenReturn(chainId - 1);

    blockManager.handleBlockRequest(blockRequest, peerAddress);

    verify(stateManager, times(0)).getCurrentStateDetail();
  }


  @Test
  public void handleBlockRequestRequestHeightLessThanZero() {
    when(blockRequest.getFirstHeight()).thenReturn(stateHeight + 1);

    blockManager.handleBlockRequest(blockRequest, peerAddress);

    verify(stateManager, times(1)).getCurrentStateDetail();
    verify(blockRequest, times(0)).getAdditionalBlockCount();
  }


  @Test
  public void handleBlockRequestRequestHeightGreaterThanCurrentStateHeight() {
    when(blockRequest.getFirstHeight()).thenReturn(-1);

    blockManager.handleBlockRequest(blockRequest, peerAddress);

    verify(stateManager, times(1)).getCurrentStateDetail();
    verify(blockRequest, times(0)).getAdditionalBlockCount();
  }


  @Test
  public void handleBlockRequestRequest() throws Exception {
    Block readBlock = mock(Block.class);
    Hash readHash = mock(Hash.class);
    when(blockRequest.getFirstHeight()).thenReturn(stateHeight - 1);
    when(blockRequest.getAdditionalBlockCount()).thenReturn(1);
    when(stateManager.getBlockHash(stateHeight - 1)).thenReturn(readHash);
    when(blockReader.readBlock(readHash)).thenReturn(readBlock);

    Object[] finalisedBlock = new Object[]{"boo"};
    Object[] finalisedReadBlock = new Object[]{"hello"};
    when(msgFactory.blockFinalized(block)).thenReturn(finalisedBlock);
    when(msgFactory.blockFinalized(readBlock)).thenReturn(finalisedReadBlock);

    blockManager.persistBlockAsync(block, hash);

    blockManager.handleBlockRequest(blockRequest, peerAddress);

    verify(peerManager, times(1)).send(peerAddress, finalisedReadBlock);
    verify(peerManager, times(1)).send(peerAddress, finalisedBlock);
    verifyNoMoreInteractions(peerManager);
  }

}
