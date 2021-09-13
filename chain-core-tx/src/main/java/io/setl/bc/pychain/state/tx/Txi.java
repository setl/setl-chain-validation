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

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Common properties of a transaction.
 */
public interface Txi {

  /**
   * Get all the addresses involved in this transaction.
   *
   * @return all the addresses
   */
  Set<String> addresses();

  /**
   * Accept the provided hash accumulator as a visitor to allow it to accumulate a hash.
   *
   * @param hacc the accumulator
   *
   * @return the provided accumulator
   */
  HashAccumulator buildHash(HashAccumulator hacc);

  /**
   * Encode this transaction as a standard list of objects suitable for signing or persistent storage.
   *
   * @return a list of objects
   */
  Object[] encodeTx();

  /**
   * The address that created this transaction.
   *
   * @return the address
   */
  String getFromAddress();

  /**
   * Get the public key used to sign this transaction.
   *
   * @return the public key
   */
  String getFromPublicKey();

  /**
   * Get the hash of this transaction.
   *
   * @return the hash
   */
  String getHash();

  /**
   * Get the height at which this transaction was applied to the block chain.
   *
   * @return the height.
   */
  int getHeight();

  /**
   * Get the metadata associated with this transaction. Transactions that do not support metadata return null.
   *
   * @return the metadata
   */
  @Nullable
  default String getMetadata() {
    return null;
  }

  /**
   * Get the number-used-once which was applied to this transaction.
   *
   * @return the nonce
   */
  long getNonce();

  /**
   * Get the address that provided the signing nonce. This will almost always be the from address. Please see the documentation of specific transactions for
   * any exceptions.
   *
   * @return the address that provided the signing nonce
   */
  String getNonceAddress();

  /**
   * Get the processing priority for this transaction. Lower numbers are processed first.
   *
   * @return the priority.
   */
  default int getPriority() {
    return getTxType().getPriority();
  }

  /**
   * Get the signature of this transaction. An unsigned transaction has the empty string as its signature.
   *
   * @return the signature
   */
  String getSignature();

  /**
   * Get the timestamp of this transaction, in seconds since the epoch.
   *
   * @return the timestamp
   */
  long getTimestamp();

  /**
   * Get the ID of the chain to which this transaction is applied.
   *
   * @return the chain's ID
   */
  int getToChainId();

  /**
   * This transaction's type.
   *
   * @return the type
   */
  TxType getTxType();

  /**
   * Was this transaction successfully applied to state?.
   *
   * @return true if transaction was applied
   */
  boolean isGood();

  /** Is this TX a PoA transaction?. */
  boolean isPOA();

  /**
   * Set whether this transaction has been successfully applied to state.
   *
   * @param isGood true if this transaction has been applied to state.
   */
  default void setGood(boolean isGood) {
    setUpdated(isGood);
  }


  /**
   * Set the block height for this transaction.
   *
   * @param height the block height
   */
  void setHeight(int height);

  /**
   * Set the signature of this transaction.
   *
   * @param sig the signature
   */
  void setSignature(String sig);

  /**
   * Set whether this transaction has been applied to state. (Note: this is equivalent to setGood(boolean))
   *
   * @param isApplied true if this transaction has been applied to state
   */
  void setUpdated(boolean isApplied);
}
