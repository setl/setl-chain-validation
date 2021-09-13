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

import static com.google.common.base.Strings.isNullOrEmpty;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.crypto.MessageSigner;
import io.setl.crypto.MessageSignerFactory;

/**
 * @author Simon Greatrix on 26/02/2018.
 */
public class DvpContract {

  /**
   * Sign DVP contract data.
   *
   * @param addressToKeysMapper mapper for key-pairs for addresses
   * @param contractData        the contract data
   * @param contractAddress     Related contract address.
   * @param isPoA               Is this a PoA Tx, affects signing if it is.
   * @param authoringAddress    Only used is `isPoA`, specifies Authoring address by whom all signatures are made.
   */
  public static void sign(
      Function<String, WalletAddress> addressToKeysMapper,
      DvpUkContractData contractData,
      String contractAddress,
      boolean isPoA,
      String authoringAddress
  ) {

    if ((addressToKeysMapper == null) || (contractData == null) || (contractAddress == null)) {
      return;
    }

    MessageSigner signer = MessageSignerFactory.get();

    signParties(signer, addressToKeysMapper, contractData, contractAddress, isPoA, authoringAddress);

    //  Auto sign parameters
    signParameters(signer, addressToKeysMapper, contractData, contractAddress, isPoA, authoringAddress);

    // Auto sign encumbrances.
    signEncumbrances(signer, addressToKeysMapper, contractData, contractAddress, isPoA, authoringAddress);
  }


  private static void signEncumbrances(
      MessageSigner signer,
      Function<String, WalletAddress> addressToKeysMapper,
      DvpUkContractData contractData,
      String contractAddress,
      boolean isPoA,
      String authoringAddress
  ) {
    /*
    Non PoA : Signature matches AddEncumbrance Public Key
    PoA : Public key = PoA Address (The entity making the payment that granted the PoA) the Signature matches the Tx Authoring Public Key.
      */

    List<DvpAddEncumbrance> encumbrances = contractData.getAddencumbrances();
    if (encumbrances == null) {
      return;
    }

    encumbrances.forEach(encumbrance -> {

      if (isNullOrEmpty(encumbrance.getSignature())) {

        WalletAddress keyPair;

        if (!isPoA) {
          // Sign with all available addresses :

          keyPair = (AddressUtil.verifyAddress(encumbrance.getPublicKey())
              ? addressToKeysMapper.apply(encumbrance.getPublicKey()) :
              addressToKeysMapper.apply(AddressUtil.publicKeyToAddress(encumbrance.getPublicKey(), AddressType.NORMAL)));
        } else {
          // is PoA, Sign with the authoring address only.

          if (isNullOrEmpty(authoringAddress)) {
            return;
          }

          keyPair = addressToKeysMapper.apply(authoringAddress);
        }

        if (keyPair != null) {
          String sigMessage = computeHash(encumbrance.objectToHashToSign(contractAddress));

          if (!isPoA) {
            // encumbrance public key remains as the Encumbering address is isPoA
            encumbrance.setPublicKey(keyPair.getHexPublicKey());
          }

          encumbrance.setSignature(signer.createSignatureB64(sigMessage, keyPair.getPrivateKey()));
        }
      }
    });
  }


  private static void signParameters(
      MessageSigner signer,
      Function<String, WalletAddress> addressToKeysMapper,
      DvpUkContractData contractData,
      String contractAddress,
      boolean isPoA,
      String authoringAddress
  ) {

    Map<String, DvpParameter> parameters = contractData.getParameters();
    if (parameters == null) {
      return;
    }

    parameters.forEach((key, parameter) -> {
      // Sign if no signature, not for calculation-only, and value is specified.

      if (isNullOrEmpty(parameter.getSignature()) && (parameter.calculationOnly == 0) && parameter.isSpecified()) {

        for (String address : parameter.getAddresses()) {

          WalletAddress keyPair;

          if (!isPoA) {
            // Sign with all available addresses :

            keyPair = AddressUtil.verifyAddress(address)
                ? addressToKeysMapper.apply(address) :
                addressToKeysMapper.apply(AddressUtil.publicKeyToAddress(address, AddressType.NORMAL));

          } else {
            // is PoA, Sign with the authoring address only.

            if (isNullOrEmpty(authoringAddress)) {
              return;
            }

            keyPair = addressToKeysMapper.apply(authoringAddress);
          }

          if (keyPair != null) {
            String sigMessage = parameter.stringToHashToSign(key, contractAddress);

            if (!isPoA) {
              // If isPoA, Parameter Address (Public Key) remains as the PoA Address. For PoA, the Signing Public key is saved
              // from the transaction itself.

              parameter.setAddress(keyPair.getHexPublicKey());
            }

            parameter.setSignature(signer.createSignatureB64(sigMessage, keyPair.getPrivateKey()));
            break;
          }
        }
      }
    });
  }


  private static void signParties(
      MessageSigner signer,
      Function<String, WalletAddress> addressToKeysMapper,
      DvpUkContractData contractData,
      String contractAddress,
      boolean isPoA,
      String authoringAddress
  ) {

    /*
     Non-Poa : Party   : sigAddress, sigPublicKey and Signature all relate to each other.
               Payment : payAddress, payPublicKey and Signature all relate to each other.

     Poa     : Party   : sigAddress is the Underlying party (grantor of PoA), sigPublicKey and Signature relate to the Attorney.
               Payment : payAddress is the Underlying party (grantor of PoA) that will actually pay,
                         payPublicKey and Signature relate to the Attorney.

      */

    List<DvpParty> dvpParties = contractData.getParties();
    if (dvpParties == null) {
      return;
    }

    dvpParties.stream().filter(party -> !party.payList.isEmpty()).forEach(party -> {
      // Sign for Party
      if ((party.sigAddress.length() > 0) && isNullOrEmpty(party.signature)) {
        WalletAddress keyPair = null;

        if (!isPoA) {
          keyPair = addressToKeysMapper.apply(party.sigAddress);
        } else {
          keyPair = addressToKeysMapper.apply(authoringAddress);
        }

        if (keyPair != null) {
          party.publicKey = keyPair.getHexPublicKey(); // OK for a PoA too.
          party.signature = signer.createSignatureB64(contractAddress, keyPair.getPrivateKey());
        }
      }

      // Sign for commitments - everything which has an address, does not have a signature and for which we can get a key pair.
      party.payList.stream().filter(payment -> ((payment.address.length() > 0) && isNullOrEmpty(payment.signature))).forEach(payment -> {

        WalletAddress keyPair;

        if (!isPoA) {
          keyPair = addressToKeysMapper.apply(payment.address);
        } else {
          keyPair = addressToKeysMapper.apply(authoringAddress);
        }

        if (keyPair != null) {
          String sigMessage = computeHash(payment.objectToHashToSign(contractAddress));

          payment.publicKey = keyPair.getHexPublicKey(); // OK for a PoA too.
          payment.signature = signer.createSignatureB64(sigMessage, keyPair.getPrivateKey());
        }
      });
    });
  }

}
