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
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoaAddTx extends AbstractTx implements HasProtocol {

  public static final TxType TXT = TxType.GRANT_POA;

  private static final Logger logger = LoggerFactory.getLogger(PoaAddTx.class);


  /**
   * Accept an Object[] and return a PoaAddTx.
   *
   * @param encodedTx : The input Object[]
   *
   * @return : The returned PoaAddTx object
   */
  public static PoaAddTx decodeTX(Object[] encodedTx) {

    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return it as a PoaAddTx.
   *
   * @param contractData : The input MPWrappedArray object
   *
   * @return : The constructed PoaAddTx object
   */
  public static PoaAddTx decodeTX(MPWrappedArray contractData) {

    logger.info("New PoaAddTx:{}", contractData);

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

    long timestamp = contractData.asLong(8);
    String poa = contractData.asString(9);
    String reference = contractData.asString(10);
    String attorneyAddress = contractData.asString(11);
    long startDate = contractData.asLong(12);
    long endDate = contractData.asLong(13);

    Object[] permissionData = contractData.asObjectArray(14);
    String protocol = contractData.asString(15);
    String metadata = contractData.asString(16);
    String signature = contractData.asString(17);
    int height = contractData.asInt(18);

    return new PoaAddTx(chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        reference,
        attorneyAddress,
        startDate,
        endDate,
        permissionData,
        protocol,
        metadata,
        signature,
        height,
        poa,
        timestamp);

  }


  /**
   * verifyItemData.
   * Verify given array of Serialised PoaItems.
   *
   * @param testItemData :
   *
   * @return :
   */
  public static boolean verifyItemData(Object[] testItemData) {

    if ((testItemData == null) || (testItemData.length == 0)) {
      return true;
    }

    for (int index = 0, l = testItemData.length; index < l; index++) {
      if (!PoaItem.verifyItemData((Object[]) (testItemData[index]))) {
        return false;
      }
    }

    return true;
  }

  private String attorneyAddress;

  private long endDate;

  private Object[] itemData;

  private String metadata;

  private ArrayList<PoaItem> poaItems;

  private String protocol;

  private String reference;

  private long startDate;

  /**
   * PoaAddTx(). Copy constructor.
   *
   * @param toCopy : PoaAddTx
   */
  public PoaAddTx(PoaAddTx toCopy) {

    //  ensure superclass constructor is called.
    //
    super(toCopy);

    this.height = toCopy.height;
    this.protocol = toCopy.protocol;
    this.metadata = toCopy.metadata;
    this.reference = toCopy.reference;
    this.attorneyAddress = toCopy.attorneyAddress;
    this.startDate = toCopy.startDate;
    this.endDate = toCopy.endDate;

    if (toCopy.itemData != null) {
      this.itemData = new Object[toCopy.itemData.length];

      for (int index = 0, l = toCopy.itemData.length; index < l; index++) {
        if (toCopy.itemData[index] != null) {
          PoaItem thisItem = new PoaItem((Object[]) (toCopy.itemData[index]));
          this.itemData[index] = thisItem.encode();
        } else {
          this.itemData[index] = null;
        }
      }
    } else {
      this.itemData = null;
    }

    this.poaItems = null;

  }


  /**
   * PoaAddTx Constructor.
   *
   * @param chainId            :
   * @param hash               :
   * @param nonce              :
   * @param updated            :
   * @param fromPubKey         :
   * @param fromAddress        :
   * @param reference          :
   * @param attorneyAddress    :
   * @param startDate          :
   * @param endDate            :
   * @param permissionItemData :
   * @param protocol           :
   * @param metadata           :
   * @param signature          :
   * @param height             :
   * @param poa                :
   * @param timestamp          :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public PoaAddTx(int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      String reference,
      String attorneyAddress,
      long startDate,
      long endDate,
      Object[] permissionItemData,
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
    this.attorneyAddress = attorneyAddress;
    this.startDate = (Math.min(startDate, endDate));
    this.endDate = (Math.max(startDate, endDate));
    this.itemData = (permissionItemData == null ? null : permissionItemData.clone());
    this.poaItems = null;
  }


  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    rVal.add(this.fromAddress);
    rVal.add(this.attorneyAddress);

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
        timestamp,
        powerOfAttorney,
        reference,
        attorneyAddress,
        startDate,
        endDate,
        itemData,
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

    PoaAddTx tx = this;

    return new Object[]{tx.chainId,
        tx.int1,
        TXT.getId(),
        tx.hash,
        tx.nonce,
        tx.updated,
        tx.fromPubKey,
        tx.fromAddress,
        tx.timestamp,
        tx.powerOfAttorney,
        tx.reference,
        tx.attorneyAddress,
        tx.startDate,
        tx.endDate,
        tx.itemData,
        tx.protocol,
        tx.metadata,
        tx.signature,
        tx.height};
  }


  public String getAttorneyAddress() {

    return attorneyAddress;
  }


  public long getEndDate() {

    return endDate;
  }


  @Override
  public String getMetadata() {

    return metadata;
  }


  /**
   * getPoaItems().
   *
   * @return : List of PoaItem
   */
  public List<PoaItem> getPoaItems() {

    if (poaItems == null) {

      poaItems = new ArrayList<>();

      if (itemData != null) {
        for (int index = 0, l = itemData.length; index < l; index++) {
          poaItems.add(new PoaItem((Object[]) (itemData[index])));
        }
      }
    }

    return poaItems;
  }


  @Override
  public String getPoaReference() {

    return reference;
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


  public long getStartDate() {

    return startDate;
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
