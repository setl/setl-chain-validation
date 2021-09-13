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
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteXChainTx extends AbstractTx {

  public static final TxType TXT = TxType.REMOVE_X_CHAIN;

  private static final Logger logger = LoggerFactory.getLogger(DeleteXChainTx.class);


  /**
   * Accept an Object[] and return a DeleteXChainTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned DeleteXChainTx object
   */
  public static DeleteXChainTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return a DeleteXChainTx.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The returned DeleteXChainTx object
   */
  public static DeleteXChainTx decodeTX(MPWrappedArray vat) {
    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);
      return null;
    }
    String hash = vat.asString(TxGeneralFields.TX_HASH);
    long nonce = vat.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = vat.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = vat.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = vat.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = vat.asLong(8);
    String poa = vat.asString(9);
    int xChainId = vat.asInt(10);
    String signature = vat.asString(11);
    String metadata = vat.asString(12);
    int height = vat.asInt(13);
    return new DeleteXChainTx(chainId, hash, nonce, updated, fromPubKey, fromAddress, xChainId, signature, height, poa, metadata, timestamp);
  }


  private String metadata;

  private int xChainId;


  /**
   * DeleteXChainTx copy constructor.
   *
   * @param toCopy ; DeleteXChainTx top copy.
   */
  public DeleteXChainTx(DeleteXChainTx toCopy) {
    super(toCopy);
    this.xChainId = toCopy.getXChainId();
    this.height = toCopy.getHeight();
    this.metadata = toCopy.getMetadata();
  }


  /**
   * DeleteXChainTx constructor.
   * <p></p>
   *
   * @param chainId     : Blockchain to which this TX will be submitted
   * @param hash        : Tx Hash
   * @param nonce       : Tx Nonce
   * @param updated     : 'Updated' flag. Post consensuc, indicated the successful application to state.
   * @param fromPubKey  : Authoring Public Key
   * @param fromAddress : Authoring Address
   * @param xChainId    : Chain ID to remove association for.
   * @param signature   : Tx Signature
   * @param height      : Unused.
   * @param poa         : Power of Attourney info.
   * @param metadata    : Arbitrary Metadata.
   * @param timestamp   : Tx Timestamp ; UTC Unix Epoch, seconds.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public DeleteXChainTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      int xChainId,
      String signature,
      int height,
      String poa,
      String metadata,
      long timestamp
  ) {
    //  ensure superclass constructor is called.
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.xChainId = xChainId;
    this.height = height;
    this.metadata = (metadata == null ? "" : metadata);
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
    // [thisTX.basechain, thisTX.txtype, thisTX.nonce, thisTX.frompub, thisTX.fromaddr, thisTX.objecttime, thisTX.poa,
    // thisTX.xchainid, thisTX.blockheight, thisTX.chainparameters, thisTX.signodes], sort_keys=True)).hexdigest()
    hashList.addAll(new Object[]{chainId, TXT.getId(), nonce, fromPubKey, fromAddress, timestamp, powerOfAttorney, xChainId, metadata});
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
        this.xChainId,
        this.signature,
        this.metadata,
        this.height
    };
  }


  @Override
  public String getMetadata() {
    return metadata;
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  @Override
  public int getToChainId() {
    return this.xChainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  public int getXChainId() {
    return xChainId;
  }
}
