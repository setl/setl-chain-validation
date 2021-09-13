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
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferToManyTx extends AbstractTx implements HasNamespace, HasClassId, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.TRANSFER_ASSET_TO_MANY;

  private static final Logger logger = LoggerFactory.getLogger(TransferToManyTx.class);


  /**
   * Accept an Object[] and return a TransferToManyTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned TransferToManyTx object
   */
  public static TransferToManyTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a TranserToManyTx.
   *
   * @param txData : The input MPWrappedArray object
   *
   * @return : The returned TransferToManyTx object
   */
  public static TransferToManyTx decodeTX(MPWrappedArray txData) {
    // [SETL_NETWORK, 0x04, TxType, hash, Nonce, Updated, frompub, fromAddr, namespace, classid,
    // amount, toChainid, [[toAddress, amount], ...], sig, protocol, metadata]
    logger.info("CommitToContractTx:{}", txData);
    int chainId = txData.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(txData.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupported:{}", txt);
      return null;
    }
    String hash = txData.asString(TxGeneralFields.TX_HASH);
    long nonce = txData.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = txData.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = txData.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = txData.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = txData.asLong(8);
    String poa = txData.asString(9);
    String nameSpace = txData.asString(10);
    String classId = txData.asString(11);
    Number amount = (new Balance(txData.get(12))).getValue();
    int toChainId = txData.asInt(13);
    Object[] addresses = (Object[]) txData.get(14);
    String signature = txData.asString(15);
    String protocol = txData.asString(16);
    String metadata = txData.asString(17);
    int height = txData.asInt(18);
    return new TransferToManyTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        toChainId,
        addresses,
        amount,
        signature,
        protocol,
        metadata,
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

  protected Object[][] toAddresses;

  protected int toChainId;


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public TransferToManyTx(TransferToManyTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.nameSpace = toCopy.getNameSpace();
    this.classId = toCopy.getClassId();
    this.amount = toCopy.getAmount();
    this.metadata = toCopy.getMetadata();
    this.protocol = toCopy.getProtocol();
    this.height = toCopy.getHeight();
    this.toChainId = toCopy.getToChainId();
    Object[] fromAddr = toCopy.getToAddresses();
    this.toAddresses = new Object[fromAddr.length][];
    for (int index = 0, l = fromAddr.length; index < l; index++) {
      this.toAddresses[index] = Arrays.copyOf((Object[]) fromAddr[index], 2);
    }
  }


  /**
   * Constructor.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public TransferToManyTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      int toChainId,
      Object[] addresses,
      Number amount,
      String signature,
      String protocol,
      String metadata,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure super class constructor is called
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.nameSpace = (nameSpace == null ? "" : nameSpace);
    this.classId = (classId == null ? "" : classId);
    this.amount = (new Balance(amount)).getValue();
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
    this.toChainId = toChainId;
    if (addresses == null) {
      this.toAddresses = new Object[0][];
    } else {
      this.toAddresses = new Object[addresses.length][];
      for (int index = 0, l = addresses.length; index < l; index++) {
        this.toAddresses[index] = Arrays.copyOf((Object[]) addresses[index], 2);
      }
    }
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
    if ((this.toAddresses != null) && (this.toAddresses.length > 0)) {
      for (int index = 0, l = toAddresses.length; index < l; index++) {
        rVal.add((String) ((Object[]) toAddresses[index])[0]);
      }
    }
    return rVal;
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hashList) {
    /*
      [
      thisTX.basechain,
      thisTX.txtype,
      thisTX.nonce,
      thisTX.frompub,
      thisTX.fromaddr,
      thisTX.objecttime,
      thisTX.poa,
      thisTX.namespace,
      thisTX.classid,
      thisTX.amount,
      thisTX.tochain,
      thisTX.txlist,
      thisTX.protocol,
      thisTX.metadata
      ]
     */
    hashList.addAll(
        new Object[]{
            chainId,
            TXT.getId(),
            nonce,
            fromPubKey,
            fromAddress,
            timestamp,
            powerOfAttorney,
            nameSpace,
            classId,
            amount,
            toChainId,
            toAddresses,
            protocol,
            metadata
        });
    return hashList;
  }


  /**
   * encodeTx().
   * <p>Encode TransferToManyTx as Object[] for Serialisation or Persistence</p>
   *
   * @return : Object[]
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
        this.nameSpace,
        this.classId,
        this.amount,
        this.toChainId,
        this.toAddresses,
        this.signature,
        this.protocol,
        this.metadata,
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


  public Object[][] getToAddresses() {
    return toAddresses != null ? toAddresses.clone() : null;
  }


  @Override
  public int getToChainId() {
    return toChainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }


  @Override
  public String toString() {
    return String.format(
        "%s nameSpace:%s classId:%s from:%s to:%s %s",
        super.toString(),
        nameSpace,
        classId,
        fromAddress,
        Arrays.toString(toAddresses),
        amount.toString()
    );
  }
}
