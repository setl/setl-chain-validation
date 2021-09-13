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

public class NamespaceRegisterTx extends AbstractTx implements HasNamespace {

  public static final TxType TXT = TxType.REGISTER_NAMESPACE;

  private static final Logger logger = LoggerFactory.getLogger(NamespaceRegisterTx.class);


  /**
   * Accept an Object[] and return a NamespaceRegisterTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned NamespaceRegisterTx object
   */
  public static NamespaceRegisterTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return a NamespaceRegisterTx.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The returned NamespaceRegisterTx object
   */
  public static NamespaceRegisterTx decodeTX(MPWrappedArray vat) {
    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      return null;
    }
    String hash = vat.asString(TxGeneralFields.TX_HASH);
    long nonce = vat.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = vat.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = vat.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = vat.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = vat.asLong(8);
    String poa = vat.asString(9);
    String nameSpace = vat.asString(10);
    String metadata = vat.asString(11);
    String signature = vat.asString(12);
    int height = vat.asInt(13);
    return new NamespaceRegisterTx(chainId, hash, nonce, updated, fromPubKey, fromAddress, nameSpace, metadata, signature, height, poa, timestamp);
    // thisTX.priority = -20
  }


  protected String metadata;

  protected String nameSpace;


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public NamespaceRegisterTx(NamespaceRegisterTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.nameSpace = toCopy.getNameSpace();
    this.metadata = toCopy.getMetadata();
    this.height = toCopy.getHeight();
  }


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
  public NamespaceRegisterTx(
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
