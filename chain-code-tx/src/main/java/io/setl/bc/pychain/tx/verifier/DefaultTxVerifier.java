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
package io.setl.bc.pychain.tx.verifier;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;

import io.setl.bc.pychain.DefaultHashableHashComputer;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.HashableObjectArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.TxFromList;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.XChainTxPackageTx;
import io.setl.crypto.MessageVerifierFactory;
import io.setl.util.PairSerializer;
import io.setl.utils.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Class DefaultTxVerifier.
 */
public class DefaultTxVerifier implements TxVerifier {

  DefaultHashableHashComputer defaultHashableHashComputer = new DefaultHashableHashComputer();

  /**
   * Verify that the signature of the transactions current hash is valid for its public key. Does not recompute hash
   *
   * @param thisTX : Transaction
   * @return :       true/false verification result
   */
  @Override
  public boolean verifySignature(Txi thisTX) {

    boolean verified;

    if (thisTX.getSignature() == null) {
      return false;
    }
    try {

      switch (thisTX.getTxType()) {

        case X_CHAIN_TX_PACKAGE:
          XChainTxPackageTx chainTx = (XChainTxPackageTx) thisTX;
          Object[] xchainSign = chainTx.getXChainSigs();

          HashableObjectArray hashable = () -> new Object[]{
              chainTx.getFromChainID(),
              Integer.valueOf(chainTx.getVersion()),
              chainTx.getNonce(),
              Hash.fromHex(chainTx.getBasehash()),
              PairSerializer.serialize(chainTx.getTxHashList()),
              chainTx.getSignodes()};

          Hash hash = defaultHashableHashComputer.computeHash(hashable);

          for (Object object : xchainSign) {

            Object[] sign = (Object[]) object;

            verified = MessageVerifierFactory.get().verifySignature(
                hash.get(),
                sign[1].toString(),
                Base64.decode(sign[0].toString())
            );

            if (!verified) {
              return false;
            }
          }

//          [cwZy+oI9dLAANp5NP2JqBEtffzQTAXSMTz4qAu5KS39C+Ar6IiSoRFoeOqBCCHbkyGFjcbXqW6tk9svP1yQiCw,
//           aa3e712659287614ee1567fb3d99ca3ca4644a7cbbe3d19f74cf7143fc54e649, XC]

          Object[] transactions = chainTx.getTxList();
          for (Object encodedTx : transactions) {
            AbstractTx transaction = TxFromList.txFromList(new MPWrappedArrayImpl((Object[]) encodedTx));
            if (!verifySignature(transaction)) {
              return false;
            }
          }
          verified = true;
          break;

        case DO_DIVIDEND: {
          throw new RuntimeException("DIVIDEND not Implimented");
        }

        default: {

          verified = MessageVerifierFactory.get().verifySignature(
              thisTX.getHash().getBytes(StandardCharsets.UTF_8),
              thisTX.getFromPublicKey(),
              Base64.decode(thisTX.getSignature())
          );

        } // default.

      } // Switch

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return verified;
  }


  /**
   * Report whether the current hash is correct.
   *
   * @param tx : Transaction
   * @return : true/false verification result
   */
  @Override
  public boolean verifyCurrentHash(Txi tx) {

    return computeHash(tx).equals(tx.getHash());
  }


}
