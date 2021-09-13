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
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceDeleteTx extends AbstractTx implements HasNamespace {

  public static final TxType TXT = TxType.DELETE_NAMESPACE;

  private static final Logger logger = LoggerFactory.getLogger(NamespaceDeleteTx.class);


  /**
   * Accept an Object[] and return a NamespaceDeleteTx.
   *
   * @param encodedTx : The input MPWrappedArray object
   *
   * @return : The returned NamespaceDeleteTx object
   */
  public static NamespaceDeleteTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return a NamespaceDeleteTx.
   *
   * @param encodedTX : The input MPWrappedArray object
   *
   * @return : The returned NamespaceDeleteTx object
   */
  public static NamespaceDeleteTx decodeTX(MPWrappedArray encodedTX) {
    int chainId = encodedTX.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(encodedTX.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      return null;
    }
    String hash = encodedTX.asString(TxGeneralFields.TX_HASH);
    long nonce = encodedTX.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = encodedTX.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = encodedTX.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = encodedTX.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = encodedTX.asLong(8);
    String poa = encodedTX.asString(9);
    String nameSpace = encodedTX.asString(10);
    String metadata = encodedTX.asString(11);
    String signature = encodedTX.asString(12);
    int height = encodedTX.asInt(13);
    return new NamespaceDeleteTx(chainId, hash, nonce, updated, fromPubKey, fromAddress, nameSpace, metadata, signature, height, poa, timestamp);
    // thisTX.priority = -20
  }


  protected String metadata;

  protected String nameSpace;


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain chainID
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param nameSpace   : Txn namespace
   * @param metadata    : Txn metadata
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public NamespaceDeleteTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure superclass constructor is called.
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.nameSpace = (nameSpace == null ? "" : nameSpace);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
  }


  /**
   * NamespaceDeleteTx Copy Constructor.
   *
   * @param toCopy :
   */
  public NamespaceDeleteTx(NamespaceDeleteTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.nameSpace = toCopy.getNameSpace();
    this.metadata = toCopy.getMetadata();
    this.height = toCopy.getHeight();
  }


  /**
   * Return associated addresses for this Transaction.
   *
   * @return :
   */
  @Override
  public Set<String> addresses() {
    Set<String> rVal = new TreeSet<>();
    rVal.add(this.fromAddress);
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.add(chainId);
    hashList.add(TXT.getId());
    hashList.add(nonce);
    hashList.add(fromPubKey);
    hashList.add(fromAddress);
    hashList.add(timestamp);
    hashList.add(powerOfAttorney);
    hashList.add(nameSpace);
    hashList.add(metadata);
    return hashList;
  }


  /**
   * Return this transation as an Object [].
   *
   * @return :   Object []
   */
  @Override
  public Object[] encodeTx() {
    return new Object[]{
        this.chainId,
        this.int1,
        TXT.getId(),
        this.hash,
        this.nonce,
        this.updated,
        this.fromPubKey,
        this.fromAddress,
        this.timestamp,
        this.powerOfAttorney,
        this.nameSpace,
        this.metadata,
        this.signature,
        this.height
    };
  }


  @Override
  public String getMetadata() {
    return metadata;
  }


  public String getNameSpace() {
    return nameSpace;
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  @Override
  public int getToChainId() {
    return this.chainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public String toString() {
    return String.format("%s nameSpace:%s from:%s", super.toString(), nameSpace, fromAddress);
  }
}
