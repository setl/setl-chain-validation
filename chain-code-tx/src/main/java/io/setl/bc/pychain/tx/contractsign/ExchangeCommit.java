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

import com.google.common.base.Strings;
import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.crypto.MessageSigner;
import io.setl.crypto.MessageSignerFactory;
import java.util.List;

public class ExchangeCommit {

  /**
   * Sign DVP contract data.
   *
   * @param addressToKeysMapper mapper for key-pairs for addresses
   * @param commitData        the contract data
   * @param contractAddress     Related contract address.
   * @param isPoA               Is this a PoA Tx, affects signing if it is.
   * @param authoringAddress    Only used is `isPoA`, specifies Authoring address by whom all signatures are made.
   */
  public static void sign(
      AddressToKeysMapper addressToKeysMapper,
      ExchangeCommitData commitData,
      String contractAddress,
      boolean isPoA,
      String authoringAddress) {

    if ((addressToKeysMapper == null) || (commitData == null) || (contractAddress == null)) {
      return;
    }

    MessageSigner signer = MessageSignerFactory.get();

    signOutputs(signer, addressToKeysMapper, commitData, contractAddress, isPoA, authoringAddress);

  }


  private static void signOutputs(
      MessageSigner signer,
      AddressToKeysMapper addressToKeysMapper,
      ExchangeCommitData contractData,
      String contractAddress,
      boolean isPoA,
      String authoringAddress) {

    List<NominateAsset> outputs = contractData.getAssetsIn();
    if (outputs == null) {
      return;
    }

    WalletAddress poaKeyPair = (isPoA ? addressToKeysMapper.getKeyPair(authoringAddress) : null); // Small performance improvement.

    outputs.forEach(thisOut -> {

      WalletAddress keyPair = null;

      // If Address is given and Signature is not....

      if ((!Strings.isNullOrEmpty(thisOut.getAddress())) && (Strings.isNullOrEmpty(thisOut.signature))) {

        if (isPoA) {
          // Sign it with Authoring Address.

          keyPair = poaKeyPair;

        } else {
          // Not PoA
          // Try to sign with an available address.

          if (!thisOut.getAddress().equalsIgnoreCase(authoringAddress)) {
            // Sign it.

            keyPair = addressToKeysMapper.getKeyPair(thisOut.getAddress());
          }
        }

        if (keyPair != null) {

          thisOut.publickey = keyPair.getHexPublicKey();
          thisOut.signature = signer.createSignatureB64(computeHash(thisOut.objectToHashToSign(contractAddress)), keyPair.getPrivateKey());
        }
      }

    });

  }

}
