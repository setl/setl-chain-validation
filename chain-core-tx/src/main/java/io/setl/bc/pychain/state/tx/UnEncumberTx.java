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
import io.setl.bc.pychain.state.tx.helper.HasAmount;
import io.setl.bc.pychain.state.tx.helper.HasClassId;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.bc.pychain.state.tx.helper.HasProtocol;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UnEncumberTx extends AbstractTx implements HasNamespace, HasClassId, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.UNENCUMBER_ASSET;

  private static final Logger logger = LoggerFactory.getLogger(UnEncumberTx.class);


  /**
   * Accept an Object[] and return a UnEncumberTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned UnEncumberTx object
   */
  public static UnEncumberTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Decode a transaction presented as a MPWrappedArray object.
   *
   * @param encodedTX :   The encodedTX object to be decoded
   *
   * @return :      An UnEncumberTx object
   */
  public static UnEncumberTx decodeTX(MPWrappedArray encodedTX) {

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
    Number amount = (new Balance(encodedTX.get(13))).getValue();
    String reference = encodedTX.asString(14);
    String protocol = encodedTX.asString(15);
    String metadata = encodedTX.asString(16);
    String signature = encodedTX.asString(17);
    int height = encodedTX.asInt(18);

    return new UnEncumberTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        subjectAddress,
        reference,
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


  protected Number amount;

  protected String classId;

  protected String metadata;

  protected String nameSpace;

  protected String protocol;

  protected String reference;

  protected String subjectaddress;


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
   * @param reference      : Encumbrance Reference
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
  public UnEncumberTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String subjectaddress,
      String reference,
      String nameSpace,
      String classId,
      Number amount,
      String protocol,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp) {

    //  ensure super class object construction
    //
    super(chainId,
        hash,
        nonce,
        updated,
        fromAddress,
        fromPubKey,
        signature,
        poa,
        timestamp
    );

    this.nameSpace = (nameSpace == null ? "" : nameSpace);
    this.classId = (classId == null ? "" : classId);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
    this.subjectaddress = (subjectaddress == null ? "" : subjectaddress);
    this.amount = (new Balance(amount)).getValue();
    this.protocol = (protocol == null ? "" : protocol);
    this.reference = (reference == null ? "" : reference);


  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public UnEncumberTx(UnEncumberTx toCopy) {
    //  ensure super class object construction
    //
    super(toCopy);

    //  load object attributes
    //
    this.nameSpace = toCopy.getNameSpace();
    this.classId = toCopy.getClassId();
    this.metadata = toCopy.getMetadata();
    this.height = toCopy.getHeight();
    this.subjectaddress = toCopy.getSubjectaddress();
    this.amount = toCopy.getAmount();
    this.protocol = toCopy.getProtocol();
    this.reference = toCopy.getReference();

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

    if ((this.subjectaddress != null) && (!this.subjectaddress.isEmpty())) {
      rVal.add(this.subjectaddress);
    }

    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.addAll(new Object[]{
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
        this.reference,
        this.protocol,
        this.metadata,
    });

    return hashList;
  }


  @Override
  public Object[] encodeTx() {

    return new Object[]{this.chainId,
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
        this.reference,
        this.protocol,
        this.metadata,
        this.signature,
        this.height
    };
  }


  public Number getAmount() {
    return amount;
  }


  public String getClassId() {
    return classId;
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


  public String getProtocol() {
    return protocol;
  }


  public String getReference() {
    return reference;
  }


  public String getSubjectaddress() {
    return subjectaddress;
  }


  @Override
  public int getToChainId() {
    return this.chainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  /**
   * isHoldingLockTx. Is this Encumbrance a holding lock rather than a standard encumbrance.
   *
   * @return :
   */
  public boolean isHoldingLockTx() {
    return false;
  }


  @Override
  public String toString() {
    return String.format("%s nameSpace:%s classId:%s", super.toString(), nameSpace, classId);
  }

}
