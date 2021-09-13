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
import io.setl.bc.pychain.state.tx.helper.HasProtocol;
import io.setl.bc.pychain.state.tx.helper.HasToAddress;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnBondTx extends AbstractTx implements HasAmount, HasToAddress, HasProtocol {

  public static final TxType TXT = TxType.REVOKE_VOTING_POWER;

  private static final Logger logger = LoggerFactory.getLogger(UnBondTx.class);


  /**
   * Take UnBondTx and copy costruct with a null signature.
   *
   * @param txn : The transaction to be stripped of signature
   *
   * @return : The signature-less cloned transaction
   */
  public static UnBondTx cloneWithoutSignature(UnBondTx txn) {
    return new UnBondTx(txn.chainId,
        txn.int1,
        txn.getHash(),
        txn.nonce,
        txn.updated,
        txn.fromPubKey,
        txn.fromAddress,
        txn.toAddress,
        txn.amount,
        null,
        txn.protocol,
        txn.metadata,
        txn.height,
        txn.powerOfAttorney,
        txn.timestamp
    );
  }


  /**
   * Accept an Object[] and return a UnBondTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned UnBondTx object
   */
  public static UnBondTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept MPWrappedArray argument and create UnBondTx from it.
   *
   * @param txData : The input MPWrappedArray object
   *
   * @return : The constructed UnBondTx object
   */
  public static UnBondTx decodeTX(MPWrappedArray txData) {
    int chainId = txData.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = txData.asInt(1);
    TxType txt = TxType.get(txData.asInt(TxGeneralFields.TX_TXTYPE));

    if (txt != TXT) {
      logger.info("Unsupported:{}", txt);
      return null;
    }

    String hash = txData.asString(TxGeneralFields.TX_HASH);
    long nonce = txData.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = txData.asBoolean(TxGeneralFields.TX_UPDATED);

    String fromPubKey = txData.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = txData.asString(TxGeneralFields.TX_FROM_ADDR);

    long timestamp = txData.asLong(8);
    String poa = txData.asString(9);
    String toAddress = txData.asString(10);
    Number amount = (new Balance(txData.get(11))).getValue();
    String signature = txData.asString(12);
    String protocol = txData.asString(13);
    String metadata = txData.asString(14);
    int height = txData.asInt(15);

    return new UnBondTx(chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        toAddress,
        amount,
        signature,
        protocol,
        metadata,
        height,
        poa,
        timestamp
    );
  }

  protected String metadata;

  protected String protocol;

  protected String toAddress;

  private Number amount;


  /**
   * Constructor.
   *
   * @param chainId     : Txn ID on the blockchain
   * @param int1        : Txn int1
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated true or false
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param toAddress   : Txn to address
   * @param amount      : Txn amount
   * @param signature   : Txn signature
   * @param protocol    : Txn protocol
   * @param metadata    : Txn metadata
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public UnBondTx(int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String toAddress,
      Number amount,
      String signature,
      String protocol,
      String metadata,
      int height,
      String poa,
      long timestamp) {
    //  ensure super object constructor is called
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

    this.amount = (new Balance(amount)).getValue();
    this.toAddress = toAddress;
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public UnBondTx(UnBondTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.amount = toCopy.getAmount();
    this.toAddress = toCopy.getToAddress();
    this.metadata = toCopy.getMetadata();
    this.protocol = toCopy.getProtocol();
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
    if ((this.toAddress != null) && (this.toAddress.length() > 0)) {
      rVal.add(this.toAddress);
    }

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
    hashList.add(toAddress);
    hashList.add(amount);
    hashList.add(protocol);
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
        this.toAddress,
        this.amount,
        this.signature,
        this.protocol,
        this.metadata,
        this.height
    };
  }


  public Number getAmount() {
    return amount;
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
  public String getProtocol() {
    return protocol;
  }


  @Override
  public String getToAddress() {
    return toAddress;
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
    return String.format("%s from:%s , %s", super.toString(), fromAddress, amount.toString());
  }
}
