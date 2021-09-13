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

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.utils.Base64;
import io.setl.utils.ByteUtil;

public class MemoTx extends AbstractTx {

  public static final TxType TXT = TxType.CREATE_MEMO;

  private static final Logger logger = LoggerFactory.getLogger(MemoTx.class);


  /**
   * Accept an Object[] and return a MemoTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned MemoTx object
   */
  public static MemoTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a MemoTx.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The returned MemoTx object
   */
  public static MemoTx decodeTX(MPWrappedArray vat) {
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
    // The next field might be a binary payload, or the textual metadata
    byte[] payload = null;
    int offset = 0;
    Object datum = vat.get(10);
    if (datum instanceof byte[]) {
      payload = (byte[]) datum;
      offset = 1;
    }
    String metadata = vat.asString(10 + offset);
    String signature = vat.asString(11 + offset);
    int height = -1;
    if (vat.size() > 12 + offset) {
      height = vat.asInt(12 + offset);
    }
    return new MemoTx(chainId, int1, hash, nonce, updated, fromPubKey, fromAddress, payload, metadata, signature, height, poa, timestamp);
  }


  private String metadata;

  private byte[] payload;


  /**
   * Copy Constructor.
   *
   * @param toCopy :
   */
  public MemoTx(MemoTx toCopy) {
    //  ensure superclass constructor is called.
    //
    super(toCopy);
    this.payload = toCopy.getPayload();
    this.metadata = toCopy.getMetadata();
    this.height = toCopy.getHeight();
  }


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain ID
   * @param int1        : Txn int1
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param metadata    : Txn metadata
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public MemoTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    this(chainId, int1, hash, nonce, updated, fromPubKey, fromAddress, null, metadata, signature, height, poa, timestamp);
  }


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain ID
   * @param int1        : Txn int1
   * @param hash        : Txn hash
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param payload     : Txn payload
   * @param metadata    : Txn metadata
   * @param signature   : Txn signature
   * @param height      : Txn height
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public MemoTx(
      int chainId,
      int int1,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      byte[] payload,
      String metadata,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure that super class constructor is called
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.payload = payload != null ? payload.clone() : null;
    this.metadata = (metadata == null ? "" : metadata);
    this.height = height;
  }


  /**
   * Constructor.
   *
   * @param chainId     : Txn blockchain chainID
   * @param int1        : Txn int1
   * @param nonce       : Txn nonce
   * @param updated     : Txn updated
   * @param fromPubKey  : Txn from Public Key
   * @param fromAddress : Txn from address
   * @param metadata    : Txn metadata
   * @param poa         : Txn Power of Attorney
   * @param timestamp   : Txn timestamp
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public MemoTx(int chainId, int int1, long nonce, boolean updated, String fromPubKey, String fromAddress, String metadata, String poa, long timestamp) {
    this(chainId, int1, null, nonce, updated, fromPubKey, fromAddress, metadata, null,         //  signature
        -1,           //  height
        poa, timestamp
    );
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
    if (payload != null) {
      hashList.add(Base64.encode(payload));
    }
    hashList.add(metadata);
    return hashList;
  }


  /**
   * Return this transaction object as an Object[] type.
   *
   * @return : This object expressed as type Object[]
   */
  @Override
  public Object[] encodeTx() {
    if (payload != null) {
      return new Object[]{
          chainId,
          int1,
          TXT.getId(),
          hash,
          nonce,
          updated,
          fromPubKey,
          fromAddress,
          timestamp,
          powerOfAttorney,
          payload,
          metadata,
          signature,
          height
      };
    }

    return new Object[]{
        chainId,
        int1,
        TXT.getId(),
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        timestamp,
        powerOfAttorney,
        metadata,
        signature,
        height
    };
  }


  @Override
  public String getMetadata() {
    return metadata;
  }


  public byte[] getPayload() {
    return payload != null ? payload.clone() : null;
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


  @Override
  public String toString() {
    if (metadata == null) {
      return "null metadata";
    }
    return String.format("metadata length:%d", metadata.length());
  }


  /**
   * toStringLongFormat().
   * <p>Return metadata in UnEncoded and Encoded format. Debug purposes.</p>
   *
   * @return :
   */
  public String toStringLongFormat() {
    if (metadata == null) {
      return "null metadata";
    }
    try {
      return String.format("metadata:%s raw:%s", new String(Base64.decode(metadata), ByteUtil.BINCHARSET), metadata);
    } catch (IOException e) {
      return "Exception : " + e.getMessage();
    }
  }

}
