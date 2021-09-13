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
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.ObjectArrayReader;
import io.setl.common.TypeSafeMap;

/**
 * Base class for all transactions. A transaction mutates state.
 *
 * @author aanten
 */
public abstract class AbstractTx implements Txi {

  protected static final int REQUIRED_ENCODED_FIELDS = 11;

  protected final String powerOfAttorney;

  protected int chainId;

  protected String fromAddress;

  protected String fromPubKey;

  protected String hash;

  protected int height;

  protected int int1;

  protected long nonce;

  protected String signature;

  protected long timestamp;

  protected boolean updated;


  /**
   * Constructor.
   *
   * @param chainId         : Txn ID on the block chain
   * @param hash            : Txn hash
   * @param nonce           : Txn nonce
   * @param updated         : Txn updated
   * @param fromAddress     : Txn from address
   * @param fromPubKey      : Txn from public key
   * @param signature       : Txn signature
   * @param powerOfAttorney : Txn Power of Attorney
   * @param timestamp       : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public AbstractTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromAddress,
      String fromPubKey,
      String signature,
      String powerOfAttorney,
      long timestamp
  ) {
    //  load object attributes
    //
    this.chainId = chainId;
    this.int1 = 4;
    this.hash = hash;
    this.nonce = nonce;
    this.powerOfAttorney = (powerOfAttorney == null ? "" : powerOfAttorney);
    this.timestamp = timestamp;
    // Updated == state was UPDATED. I.E. false = bad transaction, which did not apply state change
    // (if any - !memo)
    this.updated = updated;
    this.fromAddress = (fromAddress == null ? "" : fromAddress);
    this.fromPubKey = (fromPubKey == null ? "" : fromPubKey);
    this.signature = (signature == null ? "" : signature);
  }


  /**
   * AbstractTx Class.
   * <p>Copy constructor.</p>
   *
   * @param toCopy :
   */
  public AbstractTx(AbstractTx toCopy) {
    //  load object attributes
    //
    this.chainId = toCopy.getChainId();
    this.int1 = 4;
    this.hash = toCopy.getHash();
    this.nonce = toCopy.getNonce();
    this.powerOfAttorney = toCopy.getPowerOfAttorney();
    this.timestamp = toCopy.getTimestamp();
    // Updated == state was UPDATED. I.E. false = bad transaction, which did not apply state change
    // (if any - !memo)
    this.updated = toCopy.isGood();
    this.fromAddress = toCopy.getFromAddress();
    this.fromPubKey = toCopy.getFromPublicKey();
    this.signature = toCopy.getSignature();
  }


  /**
   * Create an instance from the encoded form, assuming the traditional encoding positions are used.
   *
   * @param encoded the encoded form
   */
  protected AbstractTx(MPWrappedArray encoded) {
    chainId = encoded.asInt(TxGeneralFields.TX_CHAIN);
    int1 = encoded.asInt(1);
    hash = encoded.asString(TxGeneralFields.TX_HASH);
    nonce = encoded.asLong(TxGeneralFields.TX_NONCE);
    updated = encoded.asBoolean(TxGeneralFields.TX_UPDATED);
    fromPubKey = encoded.asString(TxGeneralFields.TX_FROM_PUB);
    fromAddress = encoded.asString(TxGeneralFields.TX_FROM_ADDR);
    timestamp = encoded.asLong(8);
    powerOfAttorney = encoded.asString(9);
    signature = encoded.asString(encoded.size() - 1);
  }


  /**
   * Create an instance from the encoded form, assuming the traditional encoding positions are used.
   *
   * @param reader the encoded form
   */
  protected AbstractTx(ObjectArrayReader reader) {
    chainId = reader.getInt();
    int1 = reader.getInt();
    reader.skip(); // Skip the TX type, as we have to just trust it.
    hash = reader.getString();
    nonce = reader.getLong();
    updated = reader.getBoolean();
    fromPubKey = reader.getString();
    fromAddress = reader.getString();
    timestamp = reader.getLong();
    powerOfAttorney = reader.getString();
    signature = reader.getValue(TypeSafeMap::asString, reader.remaining() - 1);
  }


  public int getChainId() {
    return chainId;
  }


  @Override
  public String getFromAddress() {
    return fromAddress;
  }


  @Override
  public String getFromPublicKey() {
    return fromPubKey;
  }


  public String getHash() {
    return hash;
  }


  public int getHeight() {
    return height;
  }


  @Override
  public long getNonce() {
    return nonce;
  }


  /**
   * getNonceAddress().
   * <p>
   * The default for almost all transactions is to use the fromAddress, but to allow for this to be overridden ...
   * </p>
   *
   * @return String : Address
   */
  @Override
  public String getNonceAddress() {
    return fromAddress;
  }


  public String getPoaAddress() {
    return fromAddress;
  }


  public String getPoaReference() {
    return "";
  }


  public String getPowerOfAttorney() {
    return powerOfAttorney;
  }


  @Override
  public String getSignature() {
    return signature;
  }


  @Override
  public long getTimestamp() {
    return timestamp;
  }


  @Override
  public int getToChainId() {
    // Normally the same thing
    return getChainId();
  }


  @Override
  public boolean isGood() {
    return updated;
  }


  public boolean isPOA() {
    return false;
  }


  protected int requiredEncodingSize() {
    return REQUIRED_ENCODED_FIELDS;
  }


  public void setHash(String hash) {
    this.hash = (hash == null ? "" : hash);
  }


  public void setHeight(int height) {
    this.height = height;
  }


  @Override
  public void setSignature(String sig) {
    this.signature = (sig == null ? "" : sig);
  }


  /**
   * setTimestamp().
   * <p>Allow the transaction timestamp to be independently set.
   * This is to enable test scripts to re-create test transaction hashes.
   * NOT intended for production use.
   * </p>
   *
   * @param timestamp :  long UTC Unix epoch time (seconds).
   */
  public void setTimestamp(long timestamp) {
    // Test purposes only.
    this.timestamp = timestamp;
  }


  @Override
  public void setUpdated(boolean r) {
    this.updated = r;
  }


  /**
   * Store the standard fields in their traditional places in the encoded form.
   *
   * @param encoded array of at least size 11. TX specific data should be at locations 10 onwards.
   *
   * @return first usable entry in array
   */
  protected int startEncode(Object[] encoded) {
    encoded[TxGeneralFields.TX_CHAIN] = chainId;
    encoded[1] = 4;
    encoded[TxGeneralFields.TX_TXTYPE] = getTxType().getId();
    encoded[TxGeneralFields.TX_HASH] = hash;
    encoded[TxGeneralFields.TX_NONCE] = nonce;
    encoded[TxGeneralFields.TX_UPDATED] = updated;
    encoded[TxGeneralFields.TX_FROM_PUB] = fromPubKey;
    encoded[TxGeneralFields.TX_FROM_ADDR] = fromAddress;
    encoded[8] = timestamp;
    encoded[9] = powerOfAttorney;
    // encoded 10+ for TX specific data
    // final entry for the signature
    encoded[encoded.length - 1] = signature;

    // first usable entry in array
    return 10;
  }


  /**
   * Start the hash accumulation with the standard fields.
   *
   * @param hashList the accumulator
   */
  protected void startHash(HashAccumulator hashList) {
    hashList.add(chainId);
    hashList.add(getTxType().getId());
    hashList.add(nonce);
    hashList.add(fromPubKey);
    hashList.add(fromAddress);
    hashList.add(timestamp);
    hashList.add(powerOfAttorney);
  }


  @Override
  public String toString() {
    return getTxType().getLongName() + ":" + isGood();
  }

}
