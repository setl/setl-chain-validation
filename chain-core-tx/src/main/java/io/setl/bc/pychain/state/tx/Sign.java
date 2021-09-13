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
package io.setl.bc.pychain.state.tx;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;

import io.setl.bc.pychain.state.tx.AbstractTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.crypto.MessageSignerFactory;
import io.setl.utils.Base64;
import io.setl.utils.ByteUtil;
import java.security.PrivateKey;

/**
 * Sign.
 */
public class Sign {
  
  
  private static String ensureHash(Txi thisTX) {
    // Check Hash exists.
    String txHash = thisTX.getHash();
    
    if (txHash == null) {
      txHash = computeHash(thisTX);
      ((AbstractTx) thisTX).setHash(txHash);
    }
    return txHash;
  }
  
  
  /**
   * Compute the transactions hash (if not set), sign and store.
   *
   * @param privateKey : Key to be used in hashing algorithm
   * @param thisTX     : Transaction to be hashed
   * @deprecated Pass in the actual private key instead.
   */
  @Deprecated
  public static void signTransaction(Txi thisTX, byte[] privateKey) {
    
    // Private key given ?
    if (privateKey == null) {
      throw new NullPointerException("privateKey is null");
    }
    
    // Hash
    try {
      // Check Hash exists.
      String txHash = ensureHash(thisTX);
  
      // Derive signature.
      String sig = Base64.encode(MessageSignerFactory.get().createSignature(txHash.getBytes(ByteUtil.BINCHARSET), privateKey));
      
      // Update TX.
      thisTX.setSignature(sig);
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Compute the transactions hash (if not set), sign and store.
   *
   * @param privateKey : Key to be used in hashing algorithm
   * @param thisTX     : Transaction to be hashed
   */
  public static void signTransaction(Txi thisTX, PrivateKey privateKey) {
    
    // Private key given ?
    if (privateKey == null) {
      throw new NullPointerException("privateKey is null");
    }
    
    // Hash
    try {
      // Check Hash exists.
      String txHash = ensureHash(thisTX);
      
      // Derive signature.
      byte[] signature = MessageSignerFactory.get().createSignature(txHash.getBytes(ByteUtil.BINCHARSET), privateKey);
      String sig = Base64.encode(signature);
      
      // Update TX.
      thisTX.setSignature(sig);
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * <p>signMessage. Return a signature of the given message by the given private key.</p>
   * @param thisMessage : Byte[] message to sign
   * @param privateKey  : PrivateKey with which to sign
   * @return : B64 encoded signature
   */
  public static String signMessage(byte[] thisMessage, PrivateKey privateKey) {
    
    // Private key given ?
    if (privateKey == null) {
      throw new NullPointerException("privateKey is null");
    }
    
    try {
      byte[] signature = MessageSignerFactory.get().createSignature(thisMessage, privateKey);
      return Base64.encode(signature);
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
}
