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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Thin wrapper around an message pack style SignatureMessage.
 * Created by aanten on 04/07/2017.
 */
public class SignatureMessage implements Message {

  public class SignatureDetail implements Encodable {

    @Override
    public Object[] encode() {
      return new Object[]{getSignature(), getPublicKey(), getChainId()};
    }


    public int getChainId() {
      return SignatureMessage.this.getChainId();
    }


    public String getPublicKey() {
      return message.asString(8);
    }


    public String getSignature() {
      return Base64.encode(message.asByte(9));
    }


    private void sign(PrivateKey privateKey) {
      byte[] binary = toSign();
      MessageSigner signer = MessageSignerFactory.get();
      byte[] signature = signer.createSignature(binary, privateKey);
      message.unwrap()[9] = signature;
    }


    private byte[] toSign() {
      // Local chain signature is on the block hash
      return message.asByte(4);
    }


    /**
     * Verify the signature on this message.
     *
     * @return true if the signature is valid
     */
    public boolean verifySignature() {
      byte[] binary = toSign();
      PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(getPublicKey()));
      byte[] signature = message.asByte(9);
      MessageVerifier verifier = MessageVerifierFactory.get();
      return verifier.verifySignature(binary, publicKey, signature);
    }
  }



  public class XCSignatureDetail implements Encodable {

    @Override
    public Object[] encode() {
      return new Object[]{getSignature(), getPublicKey(), getXChainId()};
    }


    public String getPublicKey() {
      return message.asString(8);
    }


    public String getSignature() {
      return Base64.encode(message.asByte(10));
    }


    public String getXChainId() {
      return message.asString(6);
    }


    private void sign(PrivateKey privateKey) {
      byte[] binary = toSign();
      MessageSigner signer = MessageSignerFactory.get();
      byte[] signature = signer.createSignature(binary, privateKey);
      message.unwrap()[10] = signature;
    }


    private byte[] toSign() {
      // X-Chain signature is on X-Chain hash.
      return message.asByte(7);
    }


    /**
     * Verify the signature on this message.
     *
     * @return true if the signature is valid
     */
    public boolean verifySignature() {
      byte[] binary = toSign();
      PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(getPublicKey()));
      byte[] signature = message.asByte(10);
      MessageVerifier verifier = MessageVerifierFactory.get();
      return verifier.verifySignature(binary, publicKey, signature);
    }
  }



  private final MPWrappedArray message;

  private List<VoteMessage> voteMessages = null;


  public SignatureMessage(MPWrappedArray message) {
    this.message = message;
  }


  /**
   * New instance.
   *
   * @param chainId    chain ID
   * @param height     height
   * @param proposalId proposal's ID
   * @param blockhash  block's hash
   * @param votes      votes supporting signature
   * @param xChainId   cross chain ID
   * @param xcHash     cross chain hash
   * @param publicKey  signer's public key
   */
  public SignatureMessage(int chainId, int height, UUID proposalId, Hash blockhash, Collection<VoteMessage> votes, String xChainId, Hash xcHash,
      String publicKey) {
    Object[] votesArray = new Object[votes.size()];
    int i = 0;
    for (VoteMessage vm : votes) {
      votesArray[i] = vm.encode();
      i++;
    }
    message = new MPWrappedArrayImpl(
        new Object[]{
            /* 0 */ chainId,
            /* 1 */ P2PType.SIGNATURE.getId(),
            /* 2 */ height,
            /* 3 */ UUIDEncoder.encode(proposalId),
            /* 4 */ blockhash.get(),
            /* 5 */ votesArray,
            /* 6 */ xChainId,
            /* 7 */ xcHash.get(),
            /* 8 */ publicKey,
            /* 9 (signature) */ null,
            /* 10 (XC signature) */ null});
    voteMessages = new ArrayList<>(votes);
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
    if (!(o instanceof SignatureMessage)) {
      return false;
    }
    SignatureMessage that = (SignatureMessage) o;
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
    return message.asInt(2);
  }


  public UUID getProposalId() {
    return UUIDEncoder.decode(message.asByte(3));
  }


  public String getPublicKey() {
    return message.asString(8);
  }


  /**
   * Get the details of the signature.
   *
   * @return the signature details
   */
  public SignatureDetail getSignature() {
    return new SignatureDetail();
  }


  @Override
  public P2PType getType() {
    return P2PType.SIGNATURE;
  }


  /**
   * Get the votes supporting the signature.
   *
   * @return the supporting votes
   */
  public List<VoteMessage> getVotes() {
    if (voteMessages == null) {
      MPWrappedArray allVotes = message.asWrapped(5);
      ArrayList<VoteMessage> votes = new ArrayList<>(allVotes.size());
      for (int i = 0; i < allVotes.size(); i++) {
        votes.add(new VoteMessage(allVotes.asWrapped(i)));
      }
      voteMessages = votes;
    }
    return voteMessages;
  }


  public Hash getXChainHash() {
    return new Hash(message.asByte(7));
  }


  /**
   * Get the cross chain signature.
   *
   * @return the cross chain signature
   */
  public XCSignatureDetail getXChainSignature() {
    return new XCSignatureDetail();
  }


  @Override
  public int hashCode() {
    return Arrays.hashCode(message.unwrap());
  }


  public void sign(PrivateKey privateKey) {
    new SignatureDetail().sign(privateKey);
    new XCSignatureDetail().sign(privateKey);
  }


  public boolean verifySignature() {
    return new SignatureDetail().verifySignature() && new XCSignatureDetail().verifySignature();
  }

  @Override
  public String toString() {

    return String.format("SIGNATURE - Chain : %d, Height : %d, BlockHash : %s, Proposal : %s, Key : %s",
        getChainId(),
        getHeight(),
        getBlockHash().toB64(),
        getProposalId(),
        java.util.Base64.getEncoder().encodeToString(Hex.decode(getPublicKey()))
    );
  }
}
