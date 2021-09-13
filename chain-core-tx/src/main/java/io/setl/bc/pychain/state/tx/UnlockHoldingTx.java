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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockHoldingTx extends UnEncumberTx {

  public static final TxType TXT = TxType.UNLOCK_ASSET_HOLDING;

  private static final Logger logger = LoggerFactory.getLogger(UnlockHoldingTx.class);


  /**
   * Accept an Object[] and return a UnEncumberTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned UnEncumberTx object
   */
  public static UnlockHoldingTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Decode a transaction presented as a MPWrappedArray object.
   *
   * @param encodedTX :   The encodedTX object to be decoded
   *
   * @return :      An UnEncumberTx object
   */
  public static UnlockHoldingTx decodeTX(MPWrappedArray encodedTX) {
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
    String subjectAddress = encodedTX.asString(10);
    String nameSpace = encodedTX.asString(11);
    String classId = encodedTX.asString(12);
    Long amount = encodedTX.asLong(13);
    String protocol = encodedTX.asString(14);
    String metadata = encodedTX.asString(15);
    String signature = encodedTX.asString(16);
    int height = encodedTX.asInt(17);
    return new UnlockHoldingTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        subjectAddress,
        nameSpace,
        classId,
        amount,
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
  }


  /**
   * UnEncumberTx.
   *
   * @param chainId        :
   * @param hash           :
   * @param nonce          :
   * @param updated        :
   * @param fromPubKey     :
   * @param fromAddress    : Address to which the encumbrance refers
   * @param subjectaddress : (For future use)
   * @param nameSpace      : Asset Namespace
   * @param classId        : Asset Class
   * @param amount         : Quantity to encumber
   * @param protocol       : Info Only (string)
   * @param metadata       : Info Only (string)
   * @param signature      :
   * @param poa            :
   * @param timestamp      :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public UnlockHoldingTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String subjectaddress,
      String nameSpace,
      String classId,
      Number amount,
      String protocol,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure super class object construction
    //
    super(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        subjectaddress,
        "",
        nameSpace,
        classId,
        amount,
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public UnlockHoldingTx(UnEncumberTx toCopy) {
    //  ensure super class object construction
    //
    super(toCopy);
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.addAll(
        new Object[]{
            this.chainId,
            TXT.getId(),
            this.nonce,
            this.fromPubKey,
            this.fromAddress,
            this.timestamp,
            this.powerOfAttorney,
            this.subjectaddress,
            this.nameSpace,
            this.classId,
            this.amount,
            this.protocol,
            this.metadata,
        });
    return hashList;
  }


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
        this.subjectaddress,
        this.nameSpace,
        this.classId,
        this.amount,
        this.protocol,
        this.metadata,
        this.signature,
        this.height
    };
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  @Override
  public String getSubjectaddress() {
    return subjectaddress;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public boolean isHoldingLockTx() {
    return true;
  }
}
