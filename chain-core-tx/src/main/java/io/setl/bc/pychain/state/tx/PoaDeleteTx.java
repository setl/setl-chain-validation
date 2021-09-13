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
import io.setl.bc.pychain.state.tx.helper.HasProtocol;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoaDeleteTx extends AbstractTx implements HasProtocol {

  public static final TxType TXT = TxType.REVOKE_POA;

  private static final Logger logger = LoggerFactory.getLogger(PoaDeleteTx.class);


  /**
   * Accept an Object[] and return a PoaDeleteTx.
   *
   * @param encodedTx : The input Object[]
   *
   * @return : The returned PoaDeleteTx object
   */
  public static PoaDeleteTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a NewConstractTx.
   *
   * @param contractData : The input MPWrappedArray object
   *
   * @return : The constructed PoaDeleteTx object
   */
  public static PoaDeleteTx decodeTX(MPWrappedArray contractData) {

    logger.info("New PoaDeleteTx:{}", contractData);

    int chainId = contractData.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(contractData.asInt(TxGeneralFields.TX_TXTYPE));

    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      return null;
    }

    String hash = contractData.asString(TxGeneralFields.TX_HASH);
    long nonce = contractData.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = contractData.asBoolean(TxGeneralFields.TX_UPDATED);

    String fromPubKey = contractData.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = contractData.asString(TxGeneralFields.TX_FROM_ADDR);
    String issuingAddress = contractData.asString(8);

    long timestamp = contractData.asLong(9);
    String poa = contractData.asString(10);
    String reference = contractData.asString(11);
    String protocol = contractData.asString(12);
    String metadata = contractData.asString(13);
    String signature = contractData.asString(14);
    int height = contractData.asInt(15);

    return new PoaDeleteTx(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        issuingAddress,
        reference,
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp);

  }

  private String issuingAddress;

  private String metadata;

  private String protocol;

  private String reference;

  /**
   * PoaDeleteTx(). Copy constructor.
   *
   * @param toCopy : PoaDeleteTx
   */
  public PoaDeleteTx(PoaDeleteTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.height = toCopy.height;
    this.protocol = toCopy.protocol;
    this.metadata = toCopy.metadata;
    this.reference = toCopy.reference;
    this.issuingAddress = toCopy.issuingAddress;
  }


  /**
   * PoaDeleteTx Constructor.
   *
   * @param chainId        :
   * @param hash           :
   * @param nonce          :
   * @param updated        :
   * @param fromPubKey     :
   * @param fromAddress    :
   * @param issuingAddress :
   * @param reference      :
   * @param protocol       :
   * @param metadata       :
   * @param signature      :
   * @param height         :
   * @param poa            :
   * @param timestamp      :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaDeleteTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String issuingAddress,
      String reference,
      String protocol,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp) {

    //  ensure super class is invoked
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

    this.height = height;
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.reference = (reference == null ? "" : reference);
    this.issuingAddress = issuingAddress;
  }


  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    rVal.add(this.fromAddress);
    if ((this.issuingAddress != null) && (this.issuingAddress.length() > 0)) {
      rVal.add(this.issuingAddress);
    }

    return rVal;

  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {

    hashList.addAll(new Object[]{
        chainId,
        TXT.getId(),
        nonce,
        fromPubKey,
        fromAddress,
        issuingAddress,
        timestamp,
        powerOfAttorney,
        reference,
        protocol,
        metadata
    });

    return hashList;

  }


  /**
   * Return this transaction as an Object [].
   *
   * @return :   Object []
   */
  @Override
  public Object[] encodeTx() {

    PoaDeleteTx tx = this;

    return new Object[]{tx.chainId,
        tx.int1,
        TXT.getId(),
        tx.hash,
        tx.nonce,
        tx.updated,
        tx.fromPubKey,
        tx.fromAddress,
        tx.issuingAddress,
        tx.timestamp,
        tx.powerOfAttorney,
        tx.reference,
        tx.protocol,
        tx.metadata,
        tx.signature,
        tx.height};
  }


  public String getIssuingAddress() {

    return this.issuingAddress;
  }


  @Override
  public String getMetadata() {

    return metadata;
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


  @Override
  public int getToChainId() {

    return this.chainId;
  }


  @Override
  public TxType getTxType() {

    return TXT;

  }

}
