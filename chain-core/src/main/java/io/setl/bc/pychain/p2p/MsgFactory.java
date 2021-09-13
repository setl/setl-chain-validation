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
package io.setl.bc.pychain.p2p;

import io.setl.bc.pychain.p2p.message.BlockIndexMessage;
import io.setl.bc.pychain.p2p.message.FragmentMessage;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.UUID;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.p2p.message.BlockCommitted;
import io.setl.bc.pychain.p2p.message.BlockFinalized;
import io.setl.bc.pychain.p2p.message.BlockRequest;
import io.setl.bc.pychain.p2p.message.CheckOrigin;
import io.setl.bc.pychain.p2p.message.CloseRequest;
import io.setl.bc.pychain.p2p.message.EmptyProposal;
import io.setl.bc.pychain.p2p.message.Encodable;
import io.setl.bc.pychain.p2p.message.ItemRequest;
import io.setl.bc.pychain.p2p.message.ListenPort;
import io.setl.bc.pychain.p2p.message.Message;
import io.setl.bc.pychain.p2p.message.PeerRecord;
import io.setl.bc.pychain.p2p.message.PeerRecord.Record;
import io.setl.bc.pychain.p2p.message.PeerRequest;
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
import io.setl.common.CommonPy;
import io.setl.common.CommonPy.ItemType;
import io.setl.common.CommonPy.P2PType;
import io.setl.common.NearlyConstant;

import org.springframework.stereotype.Component;

/**
 * Create P2P messages.
 */
@Component
public class MsgFactory {


  /**
   * Create a BLOCK_COMMITTED message.
   *
   * @param chainId     the chain's ID
   * @param blockNumber the block's number
   * @param blockHash   the block's hash
   *
   * @return the message
   */
  public Object[] blockCommitted(int chainId, int blockNumber, Hash blockHash) {
    Message msg = new BlockCommitted(chainId, blockNumber, blockHash);
    return msg.encode();
  }


  /**
   * Create a BLOCK_FINALIZED message.
   *
   * @param block the block
   *
   * @return the message
   */
  public Object[] blockFinalized(Block block) {
    Message msg = new BlockFinalized(block);
    return msg.encode();
  }


  /**
   * Create a BLOCK_REQUEST message.
   *
   * @param chainId              the chain's ID
   * @param firstHeight          the height of the first requested block
   * @param additionalBlockCount the number of additional block's desired
   *
   * @return the message
   */
  public Object[] blockRequest(int chainId, int firstHeight, int additionalBlockCount) {
    Message msg = new BlockRequest(chainId, firstHeight, additionalBlockCount);
    return msg.encode();
  }


  /**
   * Create a CHECK_ORIGIN message.
   *
   * @param chainId              the chain's ID
   * @param uniqueNodeIdentifier the unique node identifier
   *
   * @return the message
   */
  public Object[] checkOrigin(int chainId, String uniqueNodeIdentifier, CommonPy.NodeType nodeType) {
    Message msg = new CheckOrigin(chainId, uniqueNodeIdentifier, nodeType);
    return msg.encode();
  }


  /**
   * Create a message instance from an encoded message.
   *
   * @param message the encoded message
   *
   * @return the message object
   */
  public Message create(MPWrappedArray message) {
    // Replace constants with their constant representation.
    Object[] array = message.unwrap();
    NearlyConstant.fixed(array);

    /* 0  chainId, */
    /* 1  P2PType. */

        P2PType type = P2PType.get(message.asInt(1));
    if (type == null) {
      return new UnknownMessage(message);
    }
    switch (type) {
      case BLOCK_COMMITTED:
        return new BlockCommitted(message);
      case BLOCK_FINALIZED:
        return new BlockFinalized(message);
      case BLOCK_REQUEST:
        return new BlockRequest(message);
      case CHECK_ORIGIN:
        return new CheckOrigin(message);
      case CLOSE_REQUEST:
        return new CloseRequest(message);
      case EMPTY_PROPOSAL:
        return new EmptyProposal(message);
      case ITEM_REQUEST:
        return new ItemRequest(message);
      case LISTEN_PORT:
        return new ListenPort(message);
      case PEER_REQUEST:
        return new PeerRequest(message);
      case PEER_RECORD:
        return new PeerRecord(message);
      case PREPARING_PROPOSAL:
        return new PreparingProposal(message);
      case PROPOSAL:
        return new ProposalMessage(message);
      case PROPOSED_TXS:
        return new ProposedTransactions(message);
      case SIGNATURE:
        return new SignatureMessage(message);
      case STATE_REQUEST:
        return new StateRequest(message);
      case STATE_RESPONSE:
        return new StateResponse(message);
      case TX_PACKAGE_RESPONSE:
        return new TxPackageResponse(message);
      case TX_PACKAGE_FORWARD:
        return new TxPackage(message);
      case TX_PACKAGE_ORIGINAL:
        return new TxPackage(message);
      case VOTE:
        return new VoteMessage(message);
      case MESSAGE_FRAGMENT:
        return new FragmentMessage(message);
      case BLOCK_INDEX:
        return new BlockIndexMessage(message);
      default:
        return new UnknownMessage(message);
    }
  }


  /**
   * Create an EMPTY_PROPOSAL message (signed).
   *
   * @param chainId          the chain's ID
   * @param currentStateHash the current state's hash
   * @param height           the current chain height
   * @param timestamp        the timestamp
   * @param nodeName         this node's name
   * @param pubkey           this node's public key
   * @param privateKey       this node's private key
   *
   * @return the encoded message
   */
  public Object[] emptyProposal(int chainId, Hash currentStateHash, int height, long timestamp, String nodeName, String pubkey, PrivateKey privateKey) {
    EmptyProposal emptyProposal = new EmptyProposal(chainId, currentStateHash, height, timestamp, nodeName, pubkey);
    emptyProposal.sign(privateKey);
    return emptyProposal.encode();
  }


  /**
   * Create an ITEM_REQUEST message.
   *
   * @param chainId the chain's ID
   * @param item    the item's type
   * @param itemID  the item's ID
   *
   * @return the message
   */
  public Object[] itemRequest(int chainId, ItemType item, Object itemID) {
    ItemRequest msg = new ItemRequest(chainId, item, itemID);
    return msg.encode();
  }


  /**
   * Create a LISTEN_PORT message.
   *
   * @param chainId    the chain's ID
   * @param listenPort the listen port
   * @param nodeName   the node's name
   *
   * @return the message
   */
  public Object[] listenPort(int chainId, int listenPort, String nodeName) {
    Message msg = new ListenPort(chainId, listenPort, nodeName);
    return msg.encode();
  }


  /**
   * Create a PEER_REQUEST message.
   *
   * @param chainId the chain's ID
   *
   * @return the message
   */
  public Object[] peerRequest(int chainId) {
    Message msg = new PeerRequest(chainId);
    return msg.encode();
  }


  /**
   * Create a PEER_RECORD message.
   *
   * @param chainId     the chain's ID
   * @param peerRecords - arrays of peers in format [[host,port,node-type,uuid],...]
   */
  public Object[] peerRequestResponse(int chainId, Collection<Record> peerRecords) {
    PeerRecord msg = new PeerRecord(chainId, peerRecords);
    return msg.encode();
  }


  /**
   * Create a PREPARING_PROPOSAL message.
   *
   * @param chainId          the chain's ID
   * @param timestamp        the time stamp
   * @param pubkey           the proposer's public key
   * @param height           the current chain height
   * @param currentBlockHash the current block hash
   *
   * @return the message
   */
  public Object[] preparingProposal(int chainId, long timestamp, String pubkey, int height, Hash currentBlockHash, PrivateKey privateKey) {
    PreparingProposal msg = new PreparingProposal(chainId, timestamp, pubkey, height, currentBlockHash);
    msg.sign(privateKey);
    return msg.encode();
  }


  /**
   * Create a PROPOSAL message.
   *
   * @param chainId   the chain's ID
   * @param stateHash the state's hash
   * @param height    the chain's height
   * @param timestamp the time stamp
   * @param block     the block being proposed
   * @param nodeName  the proposer's name
   *
   * @return the message
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public Object[] proposal(
      int chainId,
      int height,
      Hash stateHash,
      long timestamp,
      Block block,
      String nodeName,
      String publicKey,
      PrivateKey privateKey
  ) {
    ProposalMessage message = new ProposalMessage(chainId, height, stateHash, timestamp, block, nodeName, publicKey);
    message.sign(privateKey);
    return message.encode();
  }


  /**
   * Create a SIGNATURE message.
   *
   * @param chainId    the chain's ID
   * @param height     the chain's height
   * @param proposalId the proposal's ID
   * @param blockHash  the block's hash
   * @param votes      the supporting votes
   * @param xChainId   the cross-chain's ID
   * @param xcHash     the cross-chain hash
   * @param publicKey  the sender's public key
   * @param privateKey the sender's private key, used for signing
   *
   * @return the message
   */
  public Object[] signature(
      int chainId,
      int height,
      UUID proposalId,
      Hash blockHash,
      Collection<VoteMessage> votes,
      String xChainId,
      Hash xcHash,
      String publicKey,
      PrivateKey privateKey
  ) {
    SignatureMessage signature = new SignatureMessage(chainId, height, proposalId, blockHash, votes, xChainId, xcHash, publicKey);
    signature.sign(privateKey);
    return signature.encode();
  }


  /**
   * Create a STATE_REQUEST message.
   *
   * @param chainId the chain's ID
   *
   * @return the message
   */
  public Object[] stateRequest(int chainId) {
    StateRequest stateRequest = new StateRequest(chainId);
    return stateRequest.encode();
  }


  /**
   * Create a STATE_RESPONSE message.
   *
   * @param chainId       the chain's ID
   * @param height        the chain's height
   * @param lastStateHash the hash of the last state
   * @param lastBlockHash the hash of the last block
   *
   * @return the message
   */
  public Object[] stateResponse(int chainId, int height, Hash lastStateHash, Hash lastBlockHash, Long votingPower) {
    return stateResponse(chainId, height, new Encodable[0], lastStateHash, lastBlockHash, votingPower);
  }


  /**
   * Create a STATE_RESPONSE message.
   *
   * @param chainId       the chain's ID
   * @param height        the chains' height
   * @param xchainDetails the cross chain details
   * @param lastStateHash the last state hash
   * @param lastBlockHash the last block hash
   *
   * @return the message
   */
  public Object[] stateResponse(int chainId, int height, Encodable[] xchainDetails, Hash lastStateHash, Hash lastBlockHash, Long votingPower) {
    StateResponse stateResponse = new StateResponse(chainId, height, xchainDetails, lastStateHash, lastBlockHash, votingPower);
    return stateResponse.encode();
  }


  public Object[] txPackageForward(int chainId, Object[][] encodedTx) {
    TxPackage message = new TxPackage(P2PType.TX_PACKAGE_FORWARD, chainId, encodedTx);
    return message.encode();
  }


  /**
   * Create a TX_PACKAGE_FORWARD message.
   *
   * @param va the package
   *
   * @return the message
   */
  public Object[] txPackageForwardFromOriginal(MPWrappedArray va) {
    if (va == null) {
      return null;
    }

    TxPackage message = new TxPackage(P2PType.TX_PACKAGE_FORWARD, va);
    return message.encode();
  }


  public Object[] txPackageOriginal(int chainId, Object[][] encodedTx) {
    TxPackage message = new TxPackage(P2PType.TX_PACKAGE_ORIGINAL, chainId, encodedTx);
    return message.encode();
  }


  public Object[] txPackageResponse(int chainId, UUID uuid, Object[][] encodedTx) {
    TxPackageResponse message = new TxPackageResponse(chainId, uuid, encodedTx);
    return message.encode();
  }


  /**
   * Create a VOTE message.
   *
   * @param chainId    the chain's ID
   * @param proposalId the proposal's ID
   * @param blockHash  the block's hash
   * @param height     the chain's height
   * @param timestamp  the time stamp
   * @param pubkey     the signer's public key
   *
   * @return the message
   */
  public Object[] vote(int chainId, UUID proposalId, Hash blockHash, int height, long timestamp, String pubkey, PrivateKey privateKey) {
    VoteMessage message = new VoteMessage(chainId, proposalId, timestamp, blockHash, height, timestamp, pubkey);
    message.sign(privateKey);
    return message.encode();
  }

}
