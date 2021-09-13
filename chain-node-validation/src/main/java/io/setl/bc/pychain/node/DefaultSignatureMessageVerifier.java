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

import static io.setl.common.Balance.BALANCE_ZERO;

import java.security.PublicKey;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import io.setl.common.Balance;
import io.setl.common.Hex;
import io.setl.crypto.KeyGen;
import io.setl.crypto.MessageVerifier;

/**
 * Verify blockchain p2p signature messages - pychain compatible.
 *
 * @author aanten
 */
public class DefaultSignatureMessageVerifier {

  private static final Logger logger =
      LoggerFactory.getLogger(DefaultSignatureMessageVerifier.class);

  @Autowired
  private MessageVerifier messageVerifier;


  /**
   * verifyBlockSignatureList.
   *
   * @param sigList                  :
   * @param blockHash                :
   * @param chainId                  :
   * @param totalVotingPowerRequired :
   * @param getVotingPower           :
   *
   * @return :
   */
  public boolean verifyBlockSignatureList(
      MPWrappedArray sigList, Hash blockHash, int chainId, Balance totalVotingPowerRequired,
      Function<String, Balance> getVotingPower
  ) {

    Set<String> processedPublicKeys = new HashSet<>();
    Balance processedVotingPower = BALANCE_ZERO;

    for (int i = 0; i < sigList.size(); i++) {

      MPWrappedArray signatureDetails = sigList.asWrapped(i);
      String publicKeyHex = signatureDetails.asString(1);
      PublicKey publicKey = KeyGen.getPublicKey(Hex.decode(publicKeyHex));

      if (processedPublicKeys.contains(publicKeyHex)) {
        logger.warn("Signing public key already processed {}", publicKeyHex);
        continue;
      }

      String sigstr = signatureDetails.asString(0);

      if (!messageVerifier.verifySignature(blockHash.get(), publicKey, Base64.getDecoder().decode(sigstr))) {
        return false;
      }

      processedPublicKeys.add(publicKeyHex);
      processedVotingPower = processedVotingPower.add(getVotingPower.apply(publicKeyHex));

      if (processedVotingPower.greaterThan(totalVotingPowerRequired)) {
        return true;
      }
    }

    return false;
  }


  /**
   * Verify the signature in the net encoded message.
   *
   * @param signatureMessage The encoded message.
   * @param csd              The state detail.
   * @param cb               The consumer of the validated message - ONLY called upon a valid signature.
   */
  public void verifySignature(final SignatureMessage signatureMessage, final StateDetail csd, final Consumer<SignatureMessage> cb) {
    // Extract Signature message components
    int height = signatureMessage.getHeight();
    Hash blockHash = signatureMessage.getBlockHash();
    String pubkey = signatureMessage.getSignature().getPublicKey();

    if (height != csd.getHeight()) {
      if (logger.isWarnEnabled()) {
        logger.warn("Signature:{} from:{} Expecting height {} got {}", blockHash, pubkey, csd.getHeight(), height);
      }
      return;
    }

    boolean verified = signatureMessage.verifySignature();

    // If verified, check also the cross chain signature
    if (verified) {
      cb.accept(signatureMessage);
    } else {
      logger.error("SIGNATURE:Verify failed");
    }
  }

}
