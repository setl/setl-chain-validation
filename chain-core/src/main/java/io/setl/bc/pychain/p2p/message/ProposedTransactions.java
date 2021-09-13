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
package io.setl.bc.pychain.p2p.message;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.block.ProposedTxIds;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.serialise.UUIDEncoder;
import io.setl.common.CommonPy.P2PType;
import io.setl.common.Hex;
import io.setl.crypto.KeyGen;
import io.setl.crypto.MessageSigner;
import io.setl.crypto.MessageSignerFactory;
import io.setl.crypto.MessageVerifier;
import io.setl.crypto.MessageVerifierFactory;
import io.setl.utils.Base64;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

/**
 * @author Simon Greatrix on 2019-04-30.
 */
public class ProposedTransactions implements Message {

  private final MPWrappedArray message;

  private ProposedTxIds txList;


  /**
   * New instance.
   *
   * @param chainId        chainID
   * @param height         height
   * @param stateHash      state's hash
   * @param timestamp      timestamp
   * @param uuid           ID assigned to the proposal
   * @param txList         list of transactions in the proposal
   * @param nodeName       the proposing node's name
   * @param proposerPubKey the proposer's public key
   */
  @SuppressWarnings("squid:S00107") // More than 7 constructor parameters
  public ProposedTransactions(int chainId,
      int height,
      Hash stateHash,
      long timestamp,
      UUID uuid,
      ProposedTxIds txList,
      String nodeName,
      String proposerPubKey) {
    this.txList = txList;
    message = new MPWrappedArrayImpl(new Object[]{
        /* 0 */ chainId,
        /* 1 */ P2PType.PROPOSED_TXS.getId(),
        /* 2 */ height,
        /* 3 */ stateHash.get(),
        /* 4 */ timestamp,
        /* 5 */ UUIDEncoder.encode(uuid),
        /* 6 */ txList.encode(),
        /* 7 */ nodeName,
        /* 8 */ proposerPubKey,
        /* 9 */ null});
  }


  public ProposedTransactions(MPWrappedArray message) {
    this.message = message;
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  /**
   * getChainID
   * Return Chain ID associated with the Proposal.
   *
   * @return : int - Chain ID
   */
  @Override
  public int getChainId() {
    return message.asInt(0);
  }


  /**
   * getBlock.
   * Returns Block object embedded in this Proposal.
   *
   * @return : Block block.
   */
  public ProposedTxIds getProposed() {
    if (txList == null) {
      txList = ProposedTxIds.decode(message.asObjectArray(6));
    }
    return txList;
  }


  /**
   * getProposerHostName.
   * Return name of the host machine that created this Proposal.
   *
   * @return : String HostHane.
   */
  public String getProposerHostName() {
    return message.asString(7);
  }


  public String getPublicKey() {
    return message.asString(8);
  }


  public String getSignature() {
    return Base64.encode(message.asByte(9));
  }


  /**
   * Get the StateHash of the state upon which this proposal builds.
   *
   * @return : String StateHash.
   */
  public Hash getStateHash() {
    return new Hash(message.asByte(3));
  }


  /**
   * Get the state height upon which this Proposed Block builds.
   *
   * @return : int Height.
   */
  public int getStateHeight() {
    return message.asInt(2);
  }


  /**
   * Returns the timestamp associated with this Proposal.
   * Timestamps are expressed in UTC (Unix epoch seconds).
   *
   * @return : long - Timestamp.
   */
  public long getTimestamp() {
    return message.asLong(4);
  }


  @Override
  public P2PType getType() {
    return P2PType.PROPOSED_TXS;
  }


  public UUID getUuid() {
    return UUIDEncoder.decode(message.asByte(5));
  }


  /**
   * Sign this message.
   *
   * @param privateKey the private key
   */
  public void sign(PrivateKey privateKey) {
    Object[] input = message.unwrap().clone();
    input[9] = null;
    byte[] binary = MsgPackUtil.pack(input);
    MessageSigner signer = MessageSignerFactory.get();
    byte[] signature = signer.createSignature(binary, privateKey);
    message.unwrap()[9] = signature;
  }


  /**
   * Verify the signature on this message.
   *
   * @return true if the signature is valid
   */
  public boolean verifySignature() {
    Object[] input = message.unwrap().clone();
    input[9] = null;
    byte[] binary = MsgPackUtil.pack(input);
    PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(getPublicKey()));
    byte[] signature = message.asByte(9);
    MessageVerifier verifier = MessageVerifierFactory.get();
    return verifier.verifySignature(binary, publicKey, signature);
  }

  @Override
  public String toString() {

    return String.format("PROPOSED_TXS - Chain : %d, StateHeight : %d, State : %s, Node : %s, Key : %s, Timestamp : %d ",
        getChainId(),
        getStateHeight(),
        getStateHash().toB64(),
        getProposerHostName(),
        getPublicKey(),
        getTimestamp()
    );
  }
}
