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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.state.tx.helper.HasNamespace;
import io.setl.bc.pychain.state.tx.helper.HasProtocol;
import io.setl.bc.pychain.state.tx.helper.HasToAddress;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;

public class PoaNamespaceTransferTx extends NamespaceTransferTx implements HasNamespace, HasToAddress, HasProtocol {

  public static final TxType TXT = TxType.POA_TRANSFER_NAMESPACE;

  private static final Logger logger = LoggerFactory.getLogger(PoaNamespaceTransferTx.class);


  /**
   * Accept an Object[] and return a PoaNamespaceTransferTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned PoaNamespaceTransferTx object
   */
  public static PoaNamespaceTransferTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return a PoaNamespaceTransferTx.
   *
   * @param encodedTX : The input MPWrappedArray object
   *
   * @return : The returned PoaNamespaceTransferTx object
   */
  public static PoaNamespaceTransferTx decodeTX(MPWrappedArray encodedTX) {

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
    String nameSpace = encodedTX.asString(12);
    String protocol = encodedTX.asString(13);
    String toAddress = encodedTX.asString(14);
    String signature = encodedTX.asString(15);
    int height = encodedTX.asInt(16);

    return new PoaNamespaceTransferTx(
        chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        nameSpace,
        protocol,
        toAddress,
        signature,
        height,
        poa,
        timestamp
    );

    // thisTX.priority = -20

  }


  private String poaAddress;

  private String poaReference;

  private String protocol;


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public PoaNamespaceTransferTx(PoaNamespaceTransferTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.poaAddress = toCopy.getPoaAddress();
    this.poaReference = toCopy.getPoaReference();
    this.protocol = toCopy.getProtocol();
  }


  /**
   * Constructor.
   *
   * @param chainId      : Txn blockchain chainID
   * @param hash         : Txn hash
   * @param nonce        : Txn nonce
   * @param updated      : Txn updated
   * @param fromPubKey   : Txn from Public Key
   * @param fromAddress  : Txn from address
   * @param poaAddress   : Effective Address
   * @param poaReference : poa Reference
   * @param nameSpace    : Txn namespace
   * @param protocol     : Txn protocol
   * @param toAddress    : Txn toAddress
   * @param signature    : Txn signature
   * @param height       : Txn height
   * @param poa          : Txn Power of Attorney
   * @param timestamp    : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaNamespaceTransferTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String poaAddress,
      String poaReference,
      String nameSpace,
      String protocol,
      String toAddress,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {

    //  ensure superclass constructor is called.
    //
    super(
        chainId,
        int1,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        toAddress,
        signature,
        height,
        poa,
        timestamp
    );

    this.poaAddress = (poaAddress == null ? "" : poaAddress);
    this.poaReference = (poaReference == null ? "" : poaReference);
    this.protocol = (protocol == null ? "" : protocol);
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
        nameSpace,
        protocol,
        toAddress
    });

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
        this.poaAddress,
        this.poaReference,
        this.timestamp,
        this.powerOfAttorney,
        this.nameSpace,
        this.protocol,
        this.toAddress,
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


  @Override
  public String getProtocol() {
    return protocol;
  }


  public String getToAddress() {
    return toAddress;
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
