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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoaAddressDeleteTx extends AddressDeleteTx implements HasProtocol {

  public static final TxType TXT = TxType.POA_DELETE_ADDRESS;

  private static final Logger logger = LoggerFactory.getLogger(PoaAddressDeleteTx.class);

  /**
   * Constructor.
   *
   * @param chainId      : Txn ID on the block chain
   * @param int1         : Txn int1 value
   * @param hash         : Txn hash
   * @param nonce        : Txn nonce
   * @param updated      : Txn updated
   * @param fromPubKey   : Txn from public key
   * @param fromAddress  : Txn from address
   * @param poaAddress   : Effective Address
   * @param poaReference : poa Reference
   * @param protocol     : Txn protocol
   * @param metadata     : Txn metadata
   * @param signature    : Txn signature
   * @param height       : Txn height
   * @param poa          : Txn Power of Attorney
   * @param timestamp    : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaAddressDeleteTx(int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String protocol,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp) {

    //  ensure super class object construction
    //
    super(chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );

    //  load object attributes
    //
    this.poaAddress = (poaAddress == null ? "" : poaAddress);
    this.poaReference = (poaReference == null ? "" : poaReference);
  }

  /**
   * Accept an Object[] and return a PoaAssetClassRegisterTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned PoaAssetClassRegisterTx object
   */
  public static PoaAddressDeleteTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));

  }

  /**
   * Decode a transaction presented as a MPWrappedArray object.
   *
   * @param encodedTX :   The encodedTX object to be decoded
   *
   * @return :      An PoaAssetClassRegisterTx object
   */
  public static PoaAddressDeleteTx decodeTX(MPWrappedArray encodedTX) {

    int chainId = encodedTX.asInt(TxGeneralFields.TX_CHAIN);
    int int1 = encodedTX.asInt(1);
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
    String poaAddress = encodedTX.asString(8);
    String poaReference = encodedTX.asString(9);

    long timestamp = encodedTX.asLong(10);
    String poa = encodedTX.asString(11);
    String protocol = encodedTX.asString(12);
    String metadata = encodedTX.asString(13);
    String signature = encodedTX.asString(14);
    int height = encodedTX.asInt(15);

    return new PoaAddressDeleteTx(chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp
    );

    // thisTX.priority = -20

  }


  private String poaAddress;

  private String poaReference;

  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public PoaAddressDeleteTx(PoaAddressDeleteTx toCopy) {

    //  ensure super class object construction
    //
    super(toCopy);

    //  load object attributes
    //
    this.poaAddress = toCopy.getPoaAddress();
    this.poaReference = toCopy.getPoaReference();
  }


  /**
   * Return associated addresses for this Transaction.
   *
   * @return :
   */
  @Override
  public Set<String> addresses() {

    Set<String> rVal = super.addresses();

    if ((this.poaAddress != null) && (!this.poaAddress.isEmpty())) {
      rVal.add(this.poaAddress);
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
        poaAddress,
        poaReference,
        timestamp,
        powerOfAttorney,
        protocol,
        metadata
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
        this.poaAddress,
        this.poaReference,
        this.timestamp,
        this.powerOfAttorney,
        this.protocol,
        this.metadata,
        this.signature,
        this.height
    };

  }

  @Override
  public String getPoaAddress() {

    return poaAddress;
  }


  @Override
  public String getPoaReference() {

    return poaReference;
  }


  @Override
  public int getPriority() {

    return TXT.getPriority();

  }


  public String getProtocol() {

    return protocol;
  }


  @Override
  public TxType getTxType() {

    return TXT;
  }


  @Override
  public boolean isPOA() {
    return true;
  }

}
