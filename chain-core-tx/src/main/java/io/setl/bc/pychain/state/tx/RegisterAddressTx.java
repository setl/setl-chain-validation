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
import io.setl.bc.pychain.state.tx.helper.HasToAddress;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterAddressTx extends AbstractTx implements HasToAddress {

  public static final TxType TXT = TxType.REGISTER_ADDRESS;

  private static final Logger logger = LoggerFactory.getLogger(RegisterAddressTx.class);


  /**
   * Accept an Object[] and return a RegisterAddressTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned RegisterAddressTx object
   */
  public static RegisterAddressTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a RegisterAddressTx.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The returned RegisterAddressTx object
   */
  public static RegisterAddressTx decodeTX(MPWrappedArray vat) {
    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = vat.asInt(1);
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
    String toAddress = vat.asString(10);
    String metadata = vat.asString(11);
    String signature = vat.asString(12);
    int height = vat.asInt(13);

    return new RegisterAddressTx(chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        toAddress,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );
  }


  private final String toAddress;

  private String metadata;


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public RegisterAddressTx(RegisterAddressTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.toAddress = toCopy.getToAddress();
    this.metadata = toCopy.getMetadata();
    this.height = toCopy.getHeight();
  }


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain chainID
   * @param int1        : Txn int1
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated true/false
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param toAddress   : Txn to address
   * @param metadata    : Txn metadata
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public RegisterAddressTx(int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String toAddress,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp) {
    //  ensure the super class in invoked
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

    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
    this.toAddress = toAddress;
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
  @SuppressWarnings(value = "unchecked")
  public HashAccumulator buildHash(HashAccumulator hashList) {
    hashList.add(chainId);
    hashList.add(TXT.getId());
    hashList.add(nonce);
    hashList.add(fromPubKey);
    hashList.add(fromAddress);
    hashList.add(timestamp);
    hashList.add(powerOfAttorney);
    hashList.add(fromAddress); // in original code it is toAdress field which is set to fromaddress
    hashList.add(metadata);

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
        this.toAddress,
        this.metadata,
        this.signature,
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
    return String.format("%s from:%s to:%s", super.toString(), fromAddress, toAddress);
  }
}
