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
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.util.MsgPackUtil;
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

/**
 * Thin wrapper around an message pack style proposal.
 * Created by aanten on 28/06/2017.
 */
public class ProposalMessage implements Message {

  private final MPWrappedArray message;

  private Block block;


  /**
   * Proposal.
   * Constructor, takes MPWrappedArray 'List' of Proposal information.
   */
  public ProposalMessage(final MPWrappedArray message) {
    this.message = message;
  }


  /**
   * New instance.
   *
   * @param chainId        the block chain ID
   * @param height         the current block height
   * @param proposerPubKey the proposer's public key
   * @param timestamp      the time of the proposal
   * @param block          the block being proposed
   * @param nodeName       the node making the proposal
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public ProposalMessage(int chainId,
      int height,
      Hash stateHash,
      long timestamp,
      Block block,
      String nodeName,
      String proposerPubKey
  ) {
    message = new MPWrappedArrayImpl(new Object[]{chainId, P2PType.PROPOSAL.getId(), height, stateHash.get(), timestamp,
        block.encode(), nodeName, proposerPubKey, null});
    this.block = block;
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  /**
   * getBlock.
   * Returns Block object embedded in this Proposal.
   *
   * @return : Block block.
   */
  public Block getBlock() {
    return block;
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
   * getProposerHostName.
   * Return name of the host machine that created this Proposal.
   *
   * @return : String HostHane.
   */
  public String getProposerHostName() {
    return message.asString(6);
  }


  public String getPublicKey() {
    return message.asString(7);
  }


  public String getSignature() {
    return Base64.encode(message.asByte(8));
  }


  /**
   * getStateHash.
   * Get the StateHash of the state upon which this proposal builds.
   *
   * @return : String StateHash.
   */
  public Hash getStateHash() {
    return new Hash(message.asByte(3));
  }


  /**
   * getStateHeight.
   * Get the stat height upon which this Proposed Block builds.
   *
   * @return : int Height.
   */
  public int getStateHeight() {
    return message.asInt(2);
  }


  /**
   * getTimestamp.
   * returns the timestamp associated with this Proposal.
   * Timestamps are expressed in UTC (Unix epoch seconds).
   *
   * @return : long - Timestamp.
   */
  public long getTimestamp() {
    return message.asLong(4);
  }


  @Override
  public P2PType getType() {
    return P2PType.PROPOSAL;
  }


  /**
   * Sign this message.
   *
   * @param privateKey the private key
   */
  public void sign(PrivateKey privateKey) {
    Object[] input = message.unwrap().clone();
    input[8] = null;
    byte[] binary = MsgPackUtil.pack(input);
    MessageSigner signer = MessageSignerFactory.get();
    byte[] signature = signer.createSignature(binary, privateKey);
    message.unwrap()[8] = signature;
  }


  /**
   * Verify the signature on this message.
   *
   * @return true if the signature is valid
   */
  public boolean verifySignature() {
    Object[] input = message.unwrap().clone();
    input[8] = null;
    byte[] binary = MsgPackUtil.pack(input);
    PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(getPublicKey()));
    byte[] signature = message.asByte(8);
    MessageVerifier verifier = MessageVerifierFactory.get();
    return verifier.verifySignature(binary, publicKey, signature);
  }

  @Override
  public String toString() {

    return String.format("PROPOSAL - Chain : %d, BlockHeight : %d, State : %s, Node : %s, Key : %s, Timestamp : %d ",
        getChainId(),
        getBlock().getHeight(),
        getStateHash().toB64(),
        getProposerHostName(),
        getPublicKey(),
        getTimestamp()
    );
  }
}
