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
package io.setl.bc.pychain.tx.contractsign;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitAuthorise;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitCancel;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitment;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.security.KeyPair;
import org.junit.Test;

public class DvpCommitTest extends BaseTestClass {

  @Test
  public void sign() throws Exception {
    //
    int id = 42;
    KeyPair pair = getStandardKeyPair(0);
    WalletAddress walletAddress1 = new WalletAddress(id, pair, AddressType.NORMAL);

    // Build DvPCommitment

    String partyIdentifier = "1";

    DvpUKCommitData commitData = new DvpUKCommitData(new Object[]{});

    commitData.setParty(new DvpCommitParty(partyIdentifier, "", "")); // ID, publicKey, signature
    commitData.setCommitment(new DvpCommitment(1L, "", "")); // index, publicKey, signature
    commitData.setParameter(new DvpCommitParameter("param1", 12345, 0, "", "")); // paramName, paramValue, contractSpecific, publicKey, signature
    commitData.setParameter(new DvpCommitParameter("param2", "NAV * 42", 1, "", ""));
    commitData.setEncumbrance(new DvpCommitEncumbrance("", "NS|Class", "ref1", 42, "")); // publicKey, assetID, reference, amount, signature
    commitData.setAuthorise(new DvpCommitAuthorise("", "auth1", "", "meta", false, 0)); // publicKey, authID, signature, metadata, refused

    String contractAddress = "contractAddress";

    // Check signatures are missing and bad.

    MessageSignerVerifier verifier = MessageVerifierFactory.get();
    String sigMessage;

    assertTrue(commitData.getParty().signature.isEmpty());
    sigMessage = contractAddress;
    assertFalse(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), commitData.getParty().signature));

    commitData.getAuthorise().forEach(authorise -> {
      assertTrue(authorise.signature.isEmpty());

      assertFalse(verifier.verifySignature(authorise.stringToHashToSign(contractAddress), walletAddress1.getHexPublicKey(), authorise.signature));
    });

    commitData.getEncumbrances().forEach(encumbrance -> {
      assertTrue(encumbrance.signature.isEmpty());
      assertFalse(
          verifier.verifySignature(computeHash(encumbrance.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(), encumbrance.signature));
    });

    commitData.getParameters().forEach(parameter -> {
      assertTrue(parameter.signature.isEmpty());
      assertFalse(verifier.verifySignature(parameter.stringToHashToSign(contractAddress), walletAddress1.getHexPublicKey(), parameter.signature));
    });

    commitData.getCommitment().forEach(commitment -> {
      assertTrue(commitment.signature.isEmpty());
      assertFalse(
          verifier.verifySignature(computeHash(commitment.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(), commitment.signature));
    });

    // Set Signatures

    io.setl.bc.pychain.tx.contractsign.DvpCommit.sign(null, commitData, contractAddress, walletAddress1, false);

    // Check signatures are present and good.

    assertFalse(commitData.getParty().signature.isEmpty());
    sigMessage = contractAddress;
    assertTrue(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), commitData.getParty().signature));

    commitData.getAuthorise().forEach(authorise -> {
      assertFalse(authorise.signature.isEmpty());

      assertTrue(verifier.verifySignature(authorise.stringToHashToSign(contractAddress), walletAddress1.getHexPublicKey(), authorise.signature));
    });

    commitData.getEncumbrances().forEach(encumbrance -> {
      assertFalse(encumbrance.signature.isEmpty());
      assertTrue(
          verifier.verifySignature(computeHash(encumbrance.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(), encumbrance.signature));
    });

    commitData.getParameters().forEach(parameter -> {
      assertFalse(parameter.signature.isEmpty());
      assertTrue(verifier.verifySignature(parameter.stringToHashToSign(contractAddress), walletAddress1.getHexPublicKey(), parameter.signature));
    });

    commitData.getCommitment().forEach(commitment -> {
      assertFalse(commitment.signature.isEmpty());
      assertTrue(verifier.verifySignature(computeHash(commitment.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(), commitment.signature));
    });

    // Now to test Cancel. Note PublicKey or Address is required.

    commitData.setCancel(new DvpCommitCancel(walletAddress1.getAddress(), "")); // publicKey, signature

    // Test missing Sigs.

    assertTrue(commitData.getCancel().signature.isEmpty());
    sigMessage = commitData.getCancel().stringToHashToSign(contractAddress);
    assertFalse(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), commitData.getCancel().signature));

    // Set Signatures

    io.setl.bc.pychain.tx.contractsign.DvpCommit.sign(null, commitData, contractAddress, walletAddress1, false);

    // Test good sigs.

    assertFalse(commitData.getCancel().signature.isEmpty());
    sigMessage = commitData.getCancel().stringToHashToSign(contractAddress);
    ;
    assertTrue(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), commitData.getCancel().signature));

    // Exercise the null tests

    commitData = new DvpUKCommitData(new Object[]{});
    io.setl.bc.pychain.tx.contractsign.DvpCommit.sign(null, commitData, contractAddress, walletAddress1, false);
    // No errors...

  }

}