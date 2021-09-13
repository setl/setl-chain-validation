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
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddXChainTx extends AbstractTx {

  public static final TxType TXT = TxType.ADD_X_CHAIN;

  private static final Logger logger = LoggerFactory.getLogger(AddXChainTx.class);


  /**
   * Accept an Object[] and return a AddXChainTx.
   *
   * @param encodedTx : The input  Object[]
   *
   * @return : The returned AddXChainTx object
   */
  public static AddXChainTx decodeTX(Object[] encodedTx) {
    return decodeTX(new MPWrappedArrayImpl(encodedTx));
  }


  /**
   * Accept an MPWrappedArray object and return a AddXChainTx.
   *
   * @param vat : The input MPWrappedArray object
   *
   * @return : The returned AddXChainTx object
   */
  public static AddXChainTx decodeTX(MPWrappedArray vat) {
    int chainId = vat.asInt(TxGeneralFields.TX_CHAIN);
    TxType txt = TxType.get(vat.asInt(TxGeneralFields.TX_TXTYPE));
    if (txt != TXT) {
      logger.error("Unsupportted:{}", txt);
      return null;
    }
    String hash = vat.asString(TxGeneralFields.TX_HASH);
    long nonce = vat.asLong(TxGeneralFields.TX_NONCE);
    boolean updated = vat.asBoolean(TxGeneralFields.TX_UPDATED);
    String fromPubKey = vat.asString(TxGeneralFields.TX_FROM_PUB);
    String fromAddress = vat.asString(TxGeneralFields.TX_FROM_ADDR);
    long timestamp = vat.asLong(8);
    String poa = vat.asString(9);
    int newChainId = vat.asInt(10);
    int newBlockHeight = vat.asInt(11);
    Long newChainParams = vat.asLong(12);
    List<Object[]> newChainSignodes = new ArrayList<>();
    Object[] tempArray = vat.asObjectArray(13);
    if ((tempArray != null) && (tempArray.length > 0)) {
      for (int index = 0, l = tempArray.length; index < l; index++) {
        newChainSignodes.add((Object[]) tempArray[index]);
      }
    }
    String signature = vat.asString(14);
    int height = (vat.size() > 15 ? vat.asInt(15) : -1);
    return new AddXChainTx(
        chainId,
        hash,
        nonce,
        updated,
        fromPubKey,
        fromAddress,
        newChainId,
        newBlockHeight,
        newChainParams,
        newChainSignodes,
        signature,
        height,
        poa,
        timestamp
    );
  }


  private int newBlockHeight;

  private int newChainId;

  private Long newChainParams;

  private List<Object[]> newChainSignodes;


  /**
   * AddXChainTx.
   * <p>Copy constructor.</p>\
   *
   * @param toCopy : As it says on the tin...
   */
  public AddXChainTx(AddXChainTx toCopy) {
    super(toCopy);
    this.newChainId = toCopy.getNewChainId();
    this.newBlockHeight = toCopy.getNewBlockHeight();
    this.newChainParams = toCopy.getNewChainParams();
    this.height = toCopy.getHeight();
    ArrayList<Object[]> copySignodes = new ArrayList<>();
    if (toCopy.getNewChainSignodes() != null) {
      toCopy.getNewChainSignodes().forEach(thisSignode -> {
        if (thisSignode.length > 1) {
          copySignodes.add(new Object[]{thisSignode[0], thisSignode[1]});
        }
      });
    }
    this.newChainSignodes = copySignodes;
  }


  /**
   * AddXChainTx.
   * <p></p>
   *
   * @param chainId          : Chain to submit Tx to.
   * @param hash             :
   * @param nonce            : Tx Nonce.
   * @param updated          : Updated Status.
   * @param fromPubKey       : Authoring Public Key
   * @param fromAddress      : Authoring Address
   * @param newChainId       : Chain to associate with.
   * @param newBlockHeight   : Block Height on the other chain.
   * @param newChainParams   : Parameters to apply to his association.
   * @param newChainSignodes : Signing Information for the other chain, at the given height.
   * @param signature        : Signature for this transaction.
   * @param height           : Not Used.
   * @param poa              : POA, if applicable.
   * @param timestamp        : TX Timestamp.
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public AddXChainTx(
      int chainId,
      String hash,
      long nonce,
      boolean updated,
      String fromPubKey,
      String fromAddress,
      int newChainId,
      int newBlockHeight,
      long newChainParams,
      List<Object[]> newChainSignodes,
      String signature,
      int height,
      String poa,
      long timestamp
  ) {
    //  ensure superclass constructor is called.
    //
    super(chainId, hash, nonce, updated, fromAddress, fromPubKey, signature, poa, timestamp);
    this.newChainId = newChainId;
    this.newBlockHeight = newBlockHeight;
    this.newChainParams = newChainParams;
    this.newChainSignodes = (newChainSignodes != null ? newChainSignodes : new ArrayList<>());
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
    // [thisTX.basechain, thisTX.txtype, thisTX.nonce, thisTX.frompub, thisTX.fromaddr, thisTX.objecttime, thisTX.poa, thisTX.xchainid, thisTX.blockheight,
    // thisTX.chainparameters, thisTX.signodes], sort_keys=True)).hexdigest()
    hashList.addAll(new Object[]{
        chainId,
        TXT.getId(),
        nonce,
        fromPubKey,
        fromAddress,
        timestamp,
        powerOfAttorney,
        newChainId,
        newBlockHeight,
        newChainParams,
        getNewChainSignodesAsArray()
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
        this.timestamp,
        this.powerOfAttorney,
        this.newChainId,
        this.newBlockHeight,
        this.newChainParams,
        getNewChainSignodesAsArray(),
        this.signature,
        this.height
    };
  }


  public int getNewBlockHeight() {
    return newBlockHeight;
  }


  public int getNewChainId() {
    return newChainId;
  }


  public Long getNewChainParams() {
    return newChainParams;
  }


  /**
   * getNewChainSignodes.
   * <p> Return Signode data associated with the xChain to add.</p>
   *
   * @return : List of Object[] - [["PbKey", StakeQty], ...]
   */
  public List<Object[]> getNewChainSignodes() {
    return newChainSignodes;
  }


  /**
   * getNewChainSignodesAsArray.
   * <p> Return Signode data associated with the xChain to add.</p>
   *
   * @return : Object[] - [["PbKey", StakeQty], ...]
   */
  @Nonnull
  public Object[] getNewChainSignodesAsArray() {
    if (newChainSignodes != null) {
      return newChainSignodes.<Object[]>toArray();
    }
    return new Object[0];
  }


  @Override
  public int getPriority() {
    return TXT.getPriority();
  }


  @Override
  public int getToChainId() {
    return this.newChainId;
  }


  @Override
  public TxType getTxType() {
    return TXT;
  }
}
