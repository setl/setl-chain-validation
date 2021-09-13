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
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BondTx extends AbstractTx implements HasAmount {

  public static final TxType TXT = TxType.GRANT_VOTING_POWER;

  private static final Logger logger = LoggerFactory.getLogger(BondTx.class);


  /**
   * Take BondTx and copy costruct with a null signature.
   *
   * @param txn : The transaction to be stripped of signature
   *
   * @return : The signature-less cloned transaction
   */
  public static BondTx cloneWithoutSignature(BondTx txn) {
    return new BondTx(
        txn.chainId,
        txn.int1,
        txn.getHash(),
        txn.nonce,
        txn.updated,
        txn.fromPubKey,
        txn.fromAddress,
        txn.toSignodePubKey,
        txn.returnAddress,
        txn.amount,
        null,
        txn.height,
        txn.powerOfAttorney,
        txn.timestamp
    );
  }


  /**
   * Accept an Object[] and return a BondTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned BondTx object
   */
  public static BondTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept MPWrappedArray argument and create BondTx from it.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The constructed BondTx object
   */
  public static BondTx decodeTX(MPWrappedArray vat) {
    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = vat.asInt(1);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.info("Unsupportted:{}", txt);
      // /NAMESPACE_REGISTER
      // ASSET_CLASS_REGISTER
      // ASSET_ISSUE
      return null;
    }
    String hash = vat.asString(TxGeneralFields.TX_HASH);
    long nonce = vat.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = vat.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = vat.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = vat.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = vat.asLong(8);
    String poa = vat.asString(9);
    String toPub = vat.asString(10);
    String returnAddress = vat.asString(11);
    Number amount = (new Balance(vat.get(12))).getValue();
    String signature = vat.asString(13);
    int height = -1;
    if (vat.size() > 14) {
      height = vat.asInt(14);
    }
    return new BondTx(chainId, int1, hash, nonce, updated, fromPubKey, fromAddress, toPub, returnAddress, amount, signature, height, poa, timestamp);
  }


  private Number amount;

  private String returnAddress;

  private String toSignodePubKey;


  /**
   * Constructor.
   *
   * @param chainId         : Txn ID on the blockchain
   * @param int1            : Txn int1
   * @param hash            : Txn hash
   * @param nonce           : Txn nonce
   * @param updated         : Txn updated true or false
   * @param fromPubKey      : Txn from Public Key
   * @param fromAddress     : Txn from address
   * @param toSignodePubKey : Txn Signode Public Key
   * @param returnAddress   : Txn classID
   * @param amount          : Txn amount
   * @param signature       : Txn signature
   * @param height          : Txn height
   * @param poa             : Txn Power of Attorney
   * @param timestamp       : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public BondTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String toSignodePubKey,
      String returnAddress,
      Number amount,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure super object constructor is called
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.toSignodePubKey = toSignodePubKey;
    this.returnAddress = returnAddress;
    this.amount = (new Balance(amount)).getValue();
    this.height = height;
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public BondTx(BondTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.toSignodePubKey = toCopy.getToSignodePubKey();
    this.returnAddress = toCopy.getReturnAddress();
    this.amount = toCopy.getAmount();
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
    hashList.addAll(new Object[]{chainId, TXT.getId(), nonce, fromPubKey, fromAddress, timestamp, powerOfAttorney, toSignodePubKey, returnAddress, amount});
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
        this.toSignodePubKey,
        this.returnAddress,
        this.amount,
        this.signature,
        this.height
    };
  }


  public Number getAmount() {
    return amount;
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  public String getReturnAddress() {
    return returnAddress;
  }


  @Override
  public int getToChainId() {
    return this.chainId;
  }


  public String getToSignodePubKey() {
    return toSignodePubKey;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public String toString() {
    return String
        .format("%s toSignodePubKey:%s returnAddress:%s from:%s , %s", super.toString(), toSignodePubKey, returnAddress, fromAddress, amount.toString());
  }
}
