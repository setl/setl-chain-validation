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
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Thin wrapper around an message pack style VoteMessage.
 * Created by aanten on 04/07/2017.
 */
public class VoteMessage implements Message {

  /** Magic value used instead of timestamp to indicate the vote will never expire as it has been used to support a signature. */
  public static final long NO_EXPIRY = -0x5ea1ed;

  private final MPWrappedArray message;


  public VoteMessage(MPWrappedArray message) {
    this.message = message;
  }


  /**
   * New instance.
   *
   * @param chainId           the chain ID
   * @param proposalId        the proposal's ID
   * @param proposalTimestamp the timestamp associated with the proposal
   * @param blockHash         the block's hash
   * @param height            the chain height
   * @param voteTimestamp     the timestamp
   * @param pubkey            the voter's public key
   */
  public VoteMessage(int chainId, UUID proposalId, long proposalTimestamp, Hash blockHash, int height, long voteTimestamp, String pubkey) {
    message = new MPWrappedArrayImpl(new Object[]{
        /* 0 */ chainId,
        /* 1 */ P2PType.VOTE.getId(),
        /* 2 */ UUIDEncoder.encode(proposalId),
        /* 3 */ proposalTimestamp,
        /* 4 */ blockHash.get(),
        /* 5 */ height,
        /* 6 */ voteTimestamp,
        /* 7 */ pubkey,
        /* 8 */ null});
  }


  public void clearTimestamp() {
    message.unwrap()[6] = NO_EXPIRY;
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VoteMessage)) {
      return false;
    }
    VoteMessage that = (VoteMessage) o;
    return Arrays.equals(message.unwrap(), that.message.unwrap());
  }


  public Hash getBlockHash() {
    return new Hash(message.asByte(4));
  }


  @Override
  public int getChainId() {
    return message.asInt(0);
  }


  public int getHeight() {
    return message.asInt(5);
  }


  public UUID getProposalId() {
    return UUIDEncoder.decode(message.asByte(2));
  }


  public long getProposalTimestamp() {
    return message.asLong(3);
  }


  public String getPublicKey() {
    return message.asString(7);
  }


  @Override
  public P2PType getType() {
    return P2PType.VOTE;
  }


  public long getVoteTimestamp() {
    return message.asLong(6);
  }


  @Override
  public int hashCode() {
    return Arrays.hashCode(message.unwrap());
  }


  /**
   * Sign this message.
   *
   * @param privateKey the private key
   */
  public void sign(PrivateKey privateKey) {
    byte[] binary = toSign();
    MessageSigner signer = MessageSignerFactory.get();
    byte[] signature = signer.createSignature(binary, privateKey);
    message.unwrap()[8] = signature;
  }


  private byte[] toSign() {
    Object[] input = message.unwrap().clone();
    input[6] = null; // remove vote timestamp, as votes used to support signatures never expire
    input[8] = null; // remove signature
    return MsgPackUtil.pack(input);
  }


  /**
   * Verify the signature on this message.
   *
   * @return true if the signature is valid
   */
  public boolean verifySignature() {
    byte[] binary = toSign();
    PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(getPublicKey()));
    byte[] signature = message.asByte(8);
    MessageVerifier verifier = MessageVerifierFactory.get();
    return verifier.verifySignature(binary, publicKey, signature);
  }

  @Override
  public String toString() {

    return String.format("VOTE - Chain : %d, Height : %d, BlockHash : %s, Proposal : %s, Key : %s, Vote Timestamp : %d ",
        getChainId(),
        getHeight(),
        getBlockHash().toB64(),
        getProposalId(),
        Base64.getEncoder().encodeToString(Hex.decode(getPublicKey())),
        getVoteTimestamp()
    );
  }
}
