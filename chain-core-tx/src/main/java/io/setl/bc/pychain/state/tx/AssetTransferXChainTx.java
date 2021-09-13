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
import io.setl.bc.pychain.state.tx.helper.HasToAddress;
import io.setl.common.Balance;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AssetTransferXChainTx extends AssetTransferTx implements HasNamespace, HasClassId, HasToAddress, HasAmount, HasProtocol {

  public static final TxType TXT = TxType.TRANSFER_ASSET_X_CHAIN;

  private static final Logger logger = LoggerFactory.getLogger(AssetTransferXChainTx.class);


  /**
   * Take AssetIssueTx and copy construct with a null signature.
   *
   * @param toCopy : The transaction to be stripped of signature
   *
   * @return : The signature-less cloned transaction
   */
  public static AssetTransferXChainTx cloneWithoutSignature(AssetTransferXChainTx toCopy) {
    return new AssetTransferXChainTx(
        toCopy.chainId,
        toCopy.getHash(),
        toCopy.nonce,
        toCopy.updated,
        toCopy.fromPubKey,
        toCopy.fromAddress,
        toCopy.nameSpace,
        toCopy.classId,
        toCopy.toChainId,
        toCopy.toAddress,
        toCopy.amount,
        null,
        toCopy.protocol,
        toCopy.metadata,
        toCopy.height,
        toCopy.powerOfAttorney,
        toCopy.timestamp
    );
  }


  /**
   * Accept an Object[] and return a AssetTransferXChainTx.
   *
   * @param encodedTx : The input Object[]
   *
   * @return : The returned AssetTransferXChainTx object
   */
  public static AssetTransferXChainTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept MPWrappedArray argument and create AssetTransferXChainTx from it.
   *
   * @param txData : The input MPWrappedArray
   *
   * @return : The constructed AssetTransferXChainTx object
   */
  public static AssetTransferXChainTx decodeTX(MPWrappedArray txData) {
    // 2300,
    // 4,
    // 128,
    // 469bf945400391172faa09ee745b249ab33eceb6e9d2bd87a1ad192f0e7aeb41,
    // 1,
    // true,
    // d86a55ad6162d54286b5693ae0a32bbe4af4d5f4507acfb19adc0f20c0701292,
    // 12zwvhcfidrsASxuPCLosdabTsw6K4mQ3u,
    // 1496331340,
    // ,
    // ANDY_ISSUER,
    // ANDY_INSTRUMENT,
    // 2300,
    // 1CBFKweAmACWo8KyY9WmYfWNcFNhB4vtbX,
    // 1,
    // rggU1uOIkuDDYF8+vBHWIt2OpKkf8n4oabqYCgAN+NVTRwvRj3kAxCGOoiik9FgKWksORdROAjq7VUwQUc6MAw,
    // ,
    // oA==,90]
    int chainId = txData.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(txData.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);
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
    int toChainId = txData.asInt(12);
    String toAddress = txData.asString(13);
    Number amount = (new Balance(txData.get(14))).getValue();
    String signature = txData.asString(15);
    String protocol = txData.asString(16);
    String metadata = txData.asString(17);
    int height = txData.size() > 18 ? txData.asInt(18) : -1;
    return new AssetTransferXChainTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        toChainId,
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


  protected int toChainId;


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain ID
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated true/false
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param nameSpace   : Txn namespace
   * @param classId     : Txn classID
   * @param toChainId   : Txn target blockchain ID
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
  public AssetTransferXChainTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String nameSpace,
      String classId,
      int toChainId,
      String toAddress,
      Number amount,
      String signature,
      String protocol,
      String metadata,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure that super class constructor is called
    //
    super(
        chainId,
        4,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        nameSpace,
        classId,
        toAddress,
        amount,
        signature,
        protocol,
        metadata,
        height,
        poa,
        timestamp
    );
    this.toChainId = toChainId;
  }


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public AssetTransferXChainTx(AssetTransferXChainTx toCopy) {
    super(toCopy);
    this.toChainId = toCopy.getToChainId();
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
    hashList.add(toChainId);
    hashList.add(toAddress);
    hashList.add(nameSpace);
    hashList.add(classId);
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
        this.toChainId,
        this.toAddress,
        this.amount,
        this.signature,
        this.protocol,
        this.metadata,
        this.height
    };
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
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
    return String.format("%s nameSpace:%s classId:%s from:%s to:%s %s", super.toString(), nameSpace, classId, fromAddress, toAddress, amount.toString());
  }
}
