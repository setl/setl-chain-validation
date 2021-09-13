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
import java.util.function.Function;

import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitAuthorise;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitCancel;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParty;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.crypto.MessageSigner;
import io.setl.crypto.MessageSignerFactory;

/**
 * @author Simon Greatrix on 26/02/2018.
 */
public class DvpCommit {

  /**
   * Sign all the parts of a a DVP Commitment.
   *
   * <p>Only a single contract address is supplied, even if multiple ContractAddresses exist on a commitment. This is because
   * all of the signatures (except Parameter) include the contract address and will not work if multiple addresses are used and
   * the parameter signature does not include the contract address if it is not marked as contract specific. </p>
   *
   * @param contractData    the DVP contract commitment
   * @param contractAddress the address of the contract
   * @param walletAddress   Default signing address (Authoring Address)
   * @param addressMapper   Mapper to get additional signing addresses if required.
   */
  // public static void sign(DvpUKCommitData contractData, String contractAddress, WalletAddress walletAddress) {
  public static void sign(
      Function<String, WalletAddress> addressMapper,
      DvpUKCommitData contractData,
      String contractAddress,
      WalletAddress walletAddress, // Authoring Address
      boolean isPoA
  ) {

    MessageSigner signer = MessageSignerFactory.get();

    // Check Cancel Data
    DvpCommitCancel cancel = contractData.getCancel();
    if ((cancel != null) && (cancel.publicKey != null)) {
      signCancel(signer, cancel, contractAddress, walletAddress);
      return;
    }

    // Not cancelling, so process the party data.
    signParty(signer, contractData, contractAddress, walletAddress, addressMapper, isPoA);

    // Authorise
    signAuthorise(signer, contractData, contractAddress, walletAddress, addressMapper, isPoA);

    // Parameter data
    signParameters(signer, contractData, contractAddress, walletAddress, addressMapper, isPoA);

    // Add Encumbrance
    signEncumbrance(signer, contractData, contractAddress, walletAddress, addressMapper, isPoA);


  }


  private static void signAuthorise(
      MessageSigner signer,
      DvpUKCommitData contractData,
      String contractAddress,
      WalletAddress walletAddress,
      Function<String, WalletAddress> addressMapper,
      boolean isPoA
  ) {

    List<DvpCommitAuthorise> authorisations = contractData.getAuthorise();
    if (authorisations == null) {
      return;
    }

    authorisations.forEach(authorise -> {

      if (isNullOrEmpty(authorise.signature)) {
        WalletAddress thisAddress = null;

        if (isPoA) {
          // PoA can ONLY sign with the Authoring address.
          thisAddress = walletAddress;
        } else if (isNullOrEmpty(authorise.getPublicKey())) {
          // No address nor public key specified, so sign with the committing address
          thisAddress = walletAddress;
        } else if (authorise.getPublicKey().equals(walletAddress.getAddress())) {
          // Matches on address
          thisAddress = walletAddress;
        } else if (authorise.getPublicKey().equalsIgnoreCase(walletAddress.getHexPublicKey())) {
          // Matches on public key
          thisAddress = walletAddress;
        } else if (AddressUtil.verifyAddress(authorise.getPublicKey())) {
          // Different address so try the mapper
          if (addressMapper != null) {
            thisAddress = addressMapper.apply(authorise.getPublicKey());
          }
        } else if (addressMapper != null && AddressUtil.verifyPublicKey(authorise.getPublicKey())) {
          // Different public key so try the mapper
          if (addressMapper != null) {
            thisAddress = addressMapper.apply(AddressUtil.publicKeyToAddress(authorise.getPublicKey(), AddressType.NORMAL));
          }
        }

        // If Address OK'd, then sign.

        if (thisAddress != null) {
          authorise.setPublicKey(thisAddress.getHexPublicKey());
          authorise.signature = signer.createSignatureB64(authorise.stringToHashToSign(contractAddress), thisAddress.getPrivateKey());
        }

      }

    });
  }


  private static void signCancel(MessageSigner signer, DvpCommitCancel cancel, String contractAddress, WalletAddress walletAddress) {

    if (isNullOrEmpty(cancel.signature)) {

      if (isNullOrEmpty(cancel.publicKey)
          || cancel.publicKey.equals(walletAddress.getAddress())
          || cancel.publicKey.equalsIgnoreCase(walletAddress.getHexPublicKey())) {

        // Set signature
        String sigMessage = cancel.stringToHashToSign(contractAddress);

        cancel.signature = signer.createSignatureB64(sigMessage, walletAddress.getPrivateKey());
        cancel.publicKey = walletAddress.getHexPublicKey();
      }
    }
  }


  private static void signEncumbrance(
      MessageSigner signer,
      DvpUKCommitData contractData,
      String contractAddress,
      WalletAddress walletAddress,
      Function<String, WalletAddress> addressMapper,
      boolean isPoA
  ) {

    List<DvpCommitEncumbrance> encumbrances = contractData.getEncumbrances();
    if (encumbrances == null) {
      return;
    }

    contractData.getEncumbrances().forEach(encumbrance -> {

      if (isNullOrEmpty(encumbrance.signature)) {
        WalletAddress thisAddress = null;

        if (isPoA) {
          // PoA can ONLY sign with the Authoring address.
          thisAddress = walletAddress;
        } else if (isNullOrEmpty(encumbrance.getPublicKey())) {
          // no address nor public key specified
          thisAddress = walletAddress;
        } else if (encumbrance.getPublicKey().equals(walletAddress.getAddress())) {
          // matches on address
          thisAddress = walletAddress;
        } else if (encumbrance.getPublicKey().equalsIgnoreCase(walletAddress.getHexPublicKey())) {
          // matched on hex
          thisAddress = walletAddress;
        } else if (AddressUtil.verifyAddress(encumbrance.getPublicKey())) {
          // different address, try mapper
          if (addressMapper != null) {
            thisAddress = addressMapper.apply(encumbrance.getPublicKey());
          }
        } else if (AddressUtil.verifyPublicKey(encumbrance.getPublicKey())) {
          // different public key, try mapper
          if (addressMapper != null) {
            thisAddress = addressMapper.apply(AddressUtil.publicKeyToAddress(encumbrance.getPublicKey(), AddressType.NORMAL));
          }
        }

        // If Address OK'd, then sign.

        if (thisAddress != null) {
          encumbrance.setPublicKey(thisAddress.getHexPublicKey());
          encumbrance.signature = signer.createSignatureB64(
              computeHash(encumbrance.objectToHashToSign(contractAddress)),
              thisAddress.getPrivateKey()
          );

        }

      }

    });
  }


  private static void signParameters(
      MessageSigner signer,
      DvpUKCommitData contractData,
      String contractAddress,
      WalletAddress walletAddress,
      Function<String, WalletAddress> addressMapper,
      boolean isPoA
  ) {

    List<DvpCommitParameter> parameters = contractData.getParameters();
    if (parameters == null) {
      return;
    }

    // TODO: Should we verify that the committed parameter value is not null? Is committing to a "null" allowed?

    parameters.forEach(param -> {

      if (isNullOrEmpty(param.signature)) {
        WalletAddress thisAddress = null;

        if (isPoA) {
          // PoA can ONLY sign with the Authoring address.
          thisAddress = walletAddress;
        } else if (isNullOrEmpty(param.getPublicKey())) {
          thisAddress = walletAddress;
        } else if (param.getPublicKey().equalsIgnoreCase(walletAddress.getAddress())) {
          thisAddress = walletAddress;
        } else if (param.getPublicKey().equalsIgnoreCase(walletAddress.getHexPublicKey())) {
          thisAddress = walletAddress;
        } else if (AddressUtil.verifyAddress(param.getPublicKey())) {
          if (addressMapper != null) {
            thisAddress = addressMapper.apply(param.getPublicKey());
          }
        } else if (AddressUtil.verifyPublicKey(param.getPublicKey())) {
          if (addressMapper != null) {
            thisAddress = addressMapper.apply(AddressUtil.publicKeyToAddress(param.getPublicKey(), AddressType.NORMAL));
          }
        }

        // If Address OK'd, then sign.

        if (thisAddress != null) {
          param.setPublicKey(thisAddress.getHexPublicKey());
          param.signature = signer.createSignatureB64(param.stringToHashToSign(contractAddress), thisAddress.getPrivateKey());
        }

      }
    });
  }


  private static void signParty(
      MessageSigner signer,
      DvpUKCommitData contractData,
      String contractAddress,
      WalletAddress walletAddress,
      Function<String, WalletAddress> addressMapper,
      boolean isPoA
  ) {

    DvpCommitParty party = contractData.getParty();
    if (party == null) {
      return;
    }

    if (isNullOrEmpty(party.signature)) {

      // Sign for Party

      if (isPoA) {
        // If PoA

        party.publicKey = walletAddress.getHexPublicKey();
        party.signature = signer.createSignatureB64(contractAddress, walletAddress.getPrivateKey());

      } else if (isNullOrEmpty(party.publicKey)
          || (party.publicKey.equals(walletAddress.getAddress()))
          || (party.publicKey.equals(walletAddress.getHexPublicKey()))) {

        // Not PoA, but is Authoring Address
        party.publicKey = walletAddress.getHexPublicKey();
        party.signature = signer.createSignatureB64(contractAddress, walletAddress.getPrivateKey());

      } else {
        // Not PoA here

        WalletAddress keyPair = null;
        if (AddressUtil.verifyAddress(party.publicKey)) {
          keyPair = addressMapper.apply(party.publicKey);
        } else if (AddressUtil.verifyPublicKey(party.publicKey)) {
          keyPair = addressMapper.apply(AddressUtil.publicKeyToAddress(party.publicKey, AddressType.NORMAL));
        }

        if (keyPair != null) {
          party.publicKey = keyPair.getHexPublicKey();
          party.signature = signer.createSignatureB64(contractAddress, keyPair.getPrivateKey());
        }
      }
    }

    if (contractData.getCommitment() != null) {

      contractData.getCommitment().forEach(paymentCommit -> {

        if ((paymentCommit != null) && (paymentCommit.index >= 0)) {

          // Try to sign if signature is missing :

          if (isNullOrEmpty(paymentCommit.signature)) {

            // Does this Payment match the default address ?

            if (isNullOrEmpty(paymentCommit.getPublicKey())
                || (paymentCommit.getPublicKey().equalsIgnoreCase(walletAddress.getAddress()))
                || (paymentCommit.getPublicKey().equalsIgnoreCase(walletAddress.getHexPublicKey()))
                || isPoA) {

              // Sign with default address if Empty, Matches or PoA
              // PoA can ONLY sign with the Authoring Address.

              paymentCommit.setPublicKey(walletAddress.getHexPublicKey());
              paymentCommit.signature = signer.createSignatureB64(
                  computeHash(paymentCommit.objectToHashToSign(contractAddress)),
                  walletAddress.getPrivateKey()
              );

            } else {

              // Not PoA
              // Try to get the correct address based on the payment public key being an address or a public key.

              if (addressMapper != null) {

                if (AddressUtil.verifyAddress(paymentCommit.getPublicKey())) {

                  WalletAddress thisAddress = addressMapper.apply(paymentCommit.getPublicKey());

                  if (thisAddress != null) {
                    paymentCommit.setPublicKey(thisAddress.getHexPublicKey());
                    paymentCommit.signature = signer.createSignatureB64(
                        computeHash(paymentCommit.objectToHashToSign(contractAddress)),
                        thisAddress.getPrivateKey()
                    );
                  }

                } else if (AddressUtil.verifyPublicKey(paymentCommit.getPublicKey())) {

                  WalletAddress thisAddress = addressMapper.apply(AddressUtil.publicKeyToAddress(paymentCommit.getPublicKey(), AddressType.NORMAL));

                  if (thisAddress != null) {
                    paymentCommit.setPublicKey(thisAddress.getHexPublicKey());
                    paymentCommit.signature = signer.createSignatureB64(
                        computeHash(paymentCommit.objectToHashToSign(contractAddress)),
                        thisAddress.getPrivateKey()
                    );
                  }

                }

              } // addressMapper not null

            } // else

          } // No Signature

        } // paymentCommit
      });
    }

  }

}

