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

import com.google.common.base.Strings;
import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.state.tx.CommitContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.NominateCommitData.AssetIn;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.crypto.MessageSigner;
import io.setl.crypto.MessageSignerFactory;
import java.util.List;

/**
 * @author Simon Greatrix on 26/02/2018.
 */
public class Nominate {

  /**
   * Sign a nominate commitment.
   *
   * @param tx                  the transaction
   * @param commitData          the commitment data
   * @param addressToKeysMapper address to key pair mapper
   */
  public static void sign(CommitContractInterface tx, NominateCommitData commitData, AddressToKeysMapper addressToKeysMapper) {

    if (tx.isPOA()) {
      /* Nominates are not signed for PoA, they will only be effective for the PoA Address and the authority is taken
       * from the TX signature. */
      return;
    }

    List<AssetIn> assetsIn = commitData.getAssetsIn();

    if (assetsIn == null) {
      return;
    }

    MessageSigner messageSigner = MessageSignerFactory.get();

    // TODO : Cope with ContractAddressArray - For Nominate
    String contractAddress = tx.getContractAddress().get(0);

    String authoringAddress = tx.getAuthoringAddress();
    long nonce = tx.getNonce();

    for (AssetIn assetIn : assetsIn) {

      String publicKey = assetIn.getPublicKey();
      if (Strings.isNullOrEmpty(publicKey)) {
        continue;
      }

      // Public key may be an address
      WalletAddress walletAddress;

      if (AddressUtil.verifyAddress(publicKey)) {
        // it is an address
        walletAddress = addressToKeysMapper.getKeyPair(publicKey);

        if (walletAddress != null) {
          assetIn.setPublicKey(walletAddress.getHexPublicKey());
        }

      } else {
        // it is a public key
        walletAddress = addressToKeysMapper.getKeyPair(AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL));
      }

      if (walletAddress != null) {

        String sigMessage = assetIn.stringToHashToSign(contractAddress, authoringAddress, nonce);
        assetIn.signature = messageSigner.createSignatureB64(sigMessage, walletAddress.getPrivateKey());
      }
    }
  }

}
