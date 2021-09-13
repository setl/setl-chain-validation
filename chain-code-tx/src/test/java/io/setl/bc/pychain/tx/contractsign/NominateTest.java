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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.state.tx.CommitToContractTx;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData.AssetIn;
import io.setl.bc.pychain.tx.contractsign.DvpContractTest.Mapper;
import io.setl.bc.pychain.tx.updatestate.BaseTestClass;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import java.security.KeyPair;
import org.junit.Test;

public class NominateTest extends BaseTestClass {

  @Test
  public void sign() throws Exception {
    int id = 42;
    // Simple Key details.
    KeyPair pair = getStandardKeyPair(0);

    WalletAddress walletAddress1 = new WalletAddress(id, pair, AddressType.NORMAL);

    AddressToKeysMapper mapper = new Mapper(walletAddress1);
    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    String contractAddress = "contractAddress";

    // Build Nominate

    // namespace, assetClass, protocol, metadata, contractAddress
    NominateCommitData nominate = new NominateCommitData("NS", "class", "protocol", "", contractAddress);
    nominate.setAssetIn(new AssetIn(2123, walletAddress1.getHexPublicKey(), ""));

    CommitToContractTx tx = new CommitToContractTx(
        0,
        "",
        0,
        false,
        walletAddress1.getHexPublicKey(),
        walletAddress1.getAddress(),
        contractAddress,
        nominate.encode(),
        "",
        0,
        "",
        0
    );

    // Not Set.

    for (AssetIn assetIn : nominate.getAssetsIn()) {
      assertTrue(assetIn.signature.isEmpty());

      String sigMessage = assetIn.stringToHashToSign(contractAddress, walletAddress1.getAddress(), 0);
      assertFalse(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), assetIn.signature));

    }

    // Sign

    io.setl.bc.pychain.tx.contractsign.Nominate.sign(tx, nominate, mapper);

    // All set.

    for (AssetIn assetIn : nominate.getAssetsIn()) {
      assertFalse(assetIn.signature.isEmpty());

      String sigMessage = assetIn.stringToHashToSign(contractAddress, walletAddress1.getAddress(), 0);
      assertTrue(verifier.verifySignature(sigMessage, walletAddress1.getHexPublicKey(), assetIn.signature));

    }

    // Coverage

    nominate = new NominateCommitData("NS", "class", "protocol", "", contractAddress);
    io.setl.bc.pychain.tx.contractsign.Nominate.sign(tx, nominate, mapper);
  }

}