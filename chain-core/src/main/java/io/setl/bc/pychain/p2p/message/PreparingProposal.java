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
public class PreparingProposal implements Message {

  private final MPWrappedArray message;


  public PreparingProposal(MPWrappedArray message) {
    this.message = message;
  }


  public PreparingProposal(int chainId, long timestamp, String pubkey, int height, Hash currentBlockHash) {
    message = new MPWrappedArrayImpl(new Object[]{chainId, P2PType.PREPARING_PROPOSAL.getId(), timestamp, height, currentBlockHash.get(), pubkey, null});
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  public Hash getBlockHash() {
    return new Hash(message.asByte(4));
  }


  @Override
  public int getChainId() {
    return message.asInt(0);
  }


  public int getHeight() {
    return message.asInt(3);
  }


  public String getPublicKey() {
    return message.asString(5);
  }


  public long getTimestamp() {
    return message.asLong(2);
  }


  @Override
  public P2PType getType() {
    return P2PType.PREPARING_PROPOSAL;
  }


  /**
   * Sign this message.
   *
   * @param privateKey the private key
   */
  public void sign(PrivateKey privateKey) {
    Object[] input = message.unwrap().clone();
    input[6] = null;
    byte[] binary = MsgPackUtil.pack(input);
    MessageSigner signer = MessageSignerFactory.get();
    byte[] signature = signer.createSignature(binary, privateKey);
    message.unwrap()[6] = signature;
  }


  /**
   * Verify the signature on this message.
   *
   * @return true if the signature is valid
   */
  public boolean verifySignature() {
    Object[] input = message.unwrap().clone();
    input[6] = null;
    byte[] binary = MsgPackUtil.pack(input);
    PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(getPublicKey()));
    byte[] signature = message.asByte(6);
    MessageVerifier verifier = MessageVerifierFactory.get();
    return verifier.verifySignature(binary, publicKey, signature);
  }

  @Override
  public String toString() {

    return String.format("PREPARING_PROPOSAL - Chain : %d, Height : %d, BlockHash : %s, Key : %s, Timestamp : %d ",
        getChainId(),
        getHeight(),
        getBlockHash().toB64(),
        getPublicKey(),
        getTimestamp()
    );
  }
}
