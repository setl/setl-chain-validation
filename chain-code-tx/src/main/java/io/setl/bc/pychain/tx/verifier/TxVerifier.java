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

import io.setl.bc.pychain.state.tx.Txi;


/**Transaction hashing, signing and verification
 * Created by aanten on 13/06/2017.
 */
public interface TxVerifier {

  /**
   *  Verify that the current hash for the transaction matches a newly computed hash.
   * @param tx  : Transaction
   * @return    : true/false verification result
   */
  boolean verifyCurrentHash(Txi tx);

  /**
   *  Verify that the signature of the transactions current hash is valid for its public key. Does NOT recompute hash
   * @param tx  : Transaction
   * @return    : true/false verification result
   */
  boolean verifySignature(Txi tx);
  
}
