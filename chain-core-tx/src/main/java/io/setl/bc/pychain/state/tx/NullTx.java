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

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;

public class NullTx extends AbstractTx {

  public static final TxType TXT = TxType.DO_NOTHING;

  private static final Logger logger = LoggerFactory.getLogger(NullTx.class);


  /**
   * Accept an Object[] and return a AssetIssueTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned NullTx object
   */
  public static NullTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a NullTx.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The constructed NullTx object
   */
  public static NullTx decodeTX(MPWrappedArray vat) {

    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = vat.asInt(1);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));

    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);
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
    String signature = vat.asString(10);

    int height = -1;

    if (vat.size() > 11) {
      height = vat.asInt(11);
    }

    return new NullTx(
        chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        signature,
        height,
        poa,
        timestamp
    );

    // thisTX.priority = -20

  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public NullTx(NullTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.height = toCopy.getHeight();
  }


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain chainID
   * @param int1        : Txn int1
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public NullTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {

    //  ensure superclass constructor is invoked
    //
    super(
        chainId,
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

    return hashList;
  }


  /**
   * Return this transation as an Object [].
   *
   * @return :   Object []
   */
  @Override
  public Object[] encodeTx() {

    NullTx tx = this;

    return new Object[]{
        tx.chainId,
        tx.int1,
        TXT.getId(),
        tx.hash,
        tx.nonce,
        tx.updated,
        tx.fromPubKey,
        tx.fromAddress,
        tx.timestamp,
        tx.powerOfAttorney,
        tx.signature,
        tx.height
    };
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

}
