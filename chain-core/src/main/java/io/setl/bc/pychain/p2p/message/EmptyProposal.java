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
import io.setl.common.CommonPy.P2PType;
import io.setl.common.Hex;
import io.setl.crypto.KeyGen;
import io.setl.crypto.MessageSigner;
import io.setl.crypto.MessageSignerFactory;
import io.setl.crypto.MessageVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author Simon Greatrix on 2019-03-26.
 */
public class EmptyProposal implements Message {

  private final MPWrappedArray message;


  public EmptyProposal(MPWrappedArray message) {
    this.message = message;
  }


  /**
   * New instance.
   *
   * @param chainId          the chain ID
   * @param currentStateHash the current state's hash
   * @param height           the height
   * @param timestamp        the timestamp
   * @param nodeName         the proposing node's name
   * @param publicKey        the proposing node's public key
   */
  public EmptyProposal(int chainId, Hash currentStateHash, int height, long timestamp, String nodeName, String publicKey) {
    message = new MPWrappedArrayImpl(new Object[]{
        /* 0 */ chainId,
        /* 1 */ P2PType.EMPTY_PROPOSAL.getId(),
        /* 2 */ currentStateHash.get(),
        /* 3 */ height,
        /* 4 */ timestamp,
        /* 5 */ nodeName,
        /* 6 */ publicKey,
        /* 7 */ null});
  }


  public Object[] encode() {
    return message.unwrap();
  }


  public int getChainId() {
    return message.asInt(0);
  }


  public int getHeight() {
    return message.asInt(3);
  }


  public String getNodeName() {
    return message.asString(5);
  }


  public String getPublicKey() {
    return message.asString(6);
  }


  public Hash getStateHash() {
    return new Hash(message.asByte(2));
  }


  public long getTimestamp() {
    return message.asLong(4);
  }


  public P2PType getType() {
    return P2PType.EMPTY_PROPOSAL;
  }


  /**
   * Sign this message.
   *
   * @param privateKey the private key
   */
  public void sign(PrivateKey privateKey) {
    Object[] input = message.unwrap().clone();
    input[7] = null;
    byte[] binary = MsgPackUtil.pack(input);
    MessageSigner signer = MessageSignerFactory.get();
    byte[] signature = signer.createSignature(binary, privateKey);
    message.unwrap()[7] = signature;
  }


  /**
   * Verify the signature on this message.
   *
   * @return true if the signature is valid
   */
  public boolean verifySignature() {
    Object[] input = message.unwrap().clone();
    input[7] = null;
    byte[] binary = MsgPackUtil.pack(input);
    PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(getPublicKey()));
    byte[] signature = message.asByte(7);
    MessageVerifier verifier = MessageVerifierFactory.get();
    return verifier.verifySignature(binary, publicKey, signature);
  }

  @Override
  public String toString() {

    return String.format("Chain : %d, Height : %d, State : %s, Node : %s, Key : %s, Timestamp : %d ",
        getChainId(),
        getHeight(),
        getStateHash().toB64(),
        getNodeName(),
        getPublicKey(),
        getTimestamp()
    );
  }
}
