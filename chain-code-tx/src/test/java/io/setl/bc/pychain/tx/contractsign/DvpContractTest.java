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

import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Test;

public class DvpContractTest extends BaseTestClass {

  // Helper function
  static class Mapper implements AddressToKeysMapper {

    Set<String> addresses = new HashSet<>();

    Map<String, WalletAddress> wallets = new HashMap<>();


    Mapper(WalletAddress wa) {
      addresses.add(wa.getAddress());
      wallets.put(wa.getAddress(), wa);
    }


    @Override
    public void addAddress(String newAddr, KeyPair keyPair) {
      // do nothing
    }


    @Override
    public Set<String> getAddresses() {
      return addresses;
    }


    @Override
    public WalletAddress getKeyPair(String address) {
      if (!addresses.contains(address)) {
        return null;
      }
      return wallets.get(address);
    }


    @Nullable
    @Override
    public byte[] getPrivateKey(String address) {
      return getKeyPair(address).getPrivateKeyBytes();
    }


    @Override
    public String getPublicKey(String addr) {
      return getKeyPair(addr).getHexPublicKey();
    }
  }


  @Test
  public void sign() throws Exception {

    //
    int id = 42;
    KeyPair pair = getStandardKeyPair(0);
    WalletAddress walletAddress1 = new WalletAddress(id, pair, AddressType.NORMAL);

    final AddressToKeysMapper mapper = new Mapper(walletAddress1);

    String contractAddress = "contractAddress";
    String sigMessage;
    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    // Build Contract Data

    DvpUkContractData contractData = new DvpUkContractData((DvpUkContractData) null);

    contractData.setParties(new ArrayList<>());

    // partyIdentifier, sigAddress, publicKey, signature, mustSign
    contractData.getParties().add(new DvpParty("1", walletAddress1.getAddress(), "", "", true));

    // new DvpPayItem(address, namespace, classId, amount, publicKey, signature, isIssuance, metadata, encumbrance)
    contractData.getParties().get(0).payList.add(new DvpPayItem(walletAddress1.getAddress(), "NS", "class", 24680, "", "", false, "", ""));

    // assetID, reference, amount, publicKey, signature
    contractData.addAddEncumbrance(new DvpAddEncumbrance("NS|class", "encRef1", 1000, walletAddress1.getAddress(), ""));

    // key, new DvpParameter(address, value, calculatedIndex, contractspecific, calculationOnly, signature)
    contractData.addParameter("p1", new DvpParameter(walletAddress1.getAddress(), "12345", 0, 0, 0, ""));

    // Check signatures are missing and bad.

    assertTrue(contractData.getParties().get(0).signature.isEmpty());
    sigMessage = contractAddress;
    assertFalse(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), contractData.getParties().get(0).signature));

    contractData.getParties().get(0).payList.forEach(dvpPayItem -> {
      assertTrue(dvpPayItem.signature.isEmpty());
      assertFalse(verifier.verifySignature(computeHash(dvpPayItem.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(),
          dvpPayItem.signature));

    });

    contractData.getParameters().forEach((key, parameter) -> {
      assertTrue(parameter.getSignature().isEmpty());
      assertFalse(verifier.verifySignature(parameter.stringToHashToSign(key, contractAddress), walletAddress1.getHexPublicKey(), parameter.getSignature()));
    });

    contractData.getAddencumbrances().forEach(encumbrance -> {
      assertTrue(encumbrance.getSignature().isEmpty());
      assertFalse(
          verifier.verifySignature(computeHash(encumbrance.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(), encumbrance.getSignature()));
    });

    // Set Signatures
    io.setl.bc.pychain.tx.contractsign.DvpContract.sign(mapper, contractData, contractAddress, false, "");

    // Check signatures

    assertFalse(contractData.getParties().get(0).signature.isEmpty());
    sigMessage = contractAddress;
    assertTrue(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), contractData.getParties().get(0).signature));

    contractData.getParties().get(0).payList.forEach(dvpPayItem -> {
      assertFalse(dvpPayItem.signature.isEmpty());
      assertTrue(verifier.verifySignature(computeHash(dvpPayItem.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(),
          dvpPayItem.signature));

    });

    contractData.getParameters().forEach((key, parameter) -> {
      assertFalse(parameter.getSignature().isEmpty());
      assertTrue(verifier.verifySignature(parameter.stringToHashToSign(key, contractAddress), walletAddress1.getHexPublicKey(), parameter.getSignature()));
    });

    contractData.getAddencumbrances().forEach(encumbrance -> {
      assertFalse(encumbrance.getSignature().isEmpty());
      assertTrue(
          verifier.verifySignature(computeHash(encumbrance.objectToHashToSign(contractAddress)), walletAddress1.getHexPublicKey(), encumbrance.getSignature()));
    });

    // Exercise the null tests

    contractData = new DvpUkContractData((DvpUkContractData) null);
    io.setl.bc.pychain.tx.contractsign.DvpContract.sign(mapper, contractData, contractAddress, false, "");

  }
}