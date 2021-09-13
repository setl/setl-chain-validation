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
package io.setl.bc.pychain.state.tx.contractdataclasses;

import static io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset.cloneAssetList;
import static io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset.decodeAssetList;
import static io.setl.bc.pychain.state.tx.contractdataclasses.NominateAsset.encodeAssetList;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSETS_IN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.METADATA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PROTOCOL;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.TO_ADDR;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__FUNCTION;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE_COMMIT;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExchangeCommitData implements IContractData {

  @SuppressWarnings("squid:CommentedOutCodeLine")
  /*
  'function' = exchange_commit :

     {
     'contractaddress'    :          # Contract Address to which the
     
     'inputs'  : [[
                   namespace,        # NS & Class define the asset to be supplied
                   classid,          #
                   Amount,           #
                   address,          # (API Optional) Address from which to take the asset. May be blank or omitted, in which case the Tx Effective address
                                     # is used.
                   reference,        # (API Optional) PoA reference to use for this Output. Ignored if this is not a PoA.
                   PublicKey,        # (API Optional) If Blank, then Commit Tx Effective (Authoring or PoA) address is assumed
                                     # If is set to Effective Address or Public key, then no signature is required, as the Commit Tx itself is signed.
                                       If Asset is to be taken from a different addres, then FromAddress (or Public Key) may be specified in the API. The
                                       Wallet node will replace with Public key and sign if it can.
                   Signature         # (API Optional) [contractaddress|AssetAddress|Amount|AuthoringAddress|Tx Nonce].
                                       Signature only required if FromAddress != Tx Authoring address.
                   ], ...
                  ],
     'toaddr'             :          # (API Optional) destination address. If specified then this is where the contract 'Output' assets will be delivered.
                                     # If not specified, then assets will be delivered to the Commitment Tx (Author or PoA, if relevant) Address.
     'protocol'           : "",      # (API Optional) User Data
     'metadata'           : ""       # (API Optional) User Data
     'autosign'           : Boolean  # (API Optional) Defaults to True. If True, the Wallet node will attempt to sign the commitment with the authoring key
     'signforwallet'      : Boolean  # (API Optional) Defaults to False. If True (and autosign is true), the Wallet node will attempt to sign the contract
                                     #                with any keys
     }
     
     For PoA, the Attorney address needs PoAs from the payment Address for
     Tx Type COMMIT_TO_CONTRACT and the relevant assets covering the respective
     input quantities.
  */

  private static final Logger logger = LoggerFactory.getLogger(ExchangeCommitData.class);

  // It is decided that there is no coding benefit, but a small performance hit, to be had from having getters / setters on these values.

  @SuppressWarnings("squid:ClassVariableVisibilityCheckClass")
  public String contractAddress;

  @SuppressWarnings("squid:ClassVariableVisibilityCheckClass")
  public String metadata;

  @SuppressWarnings("squid:ClassVariableVisibilityCheckClass")
  public String protocol;

  @SuppressWarnings("squid:ClassVariableVisibilityCheckClass")
  public String toaddress;

  private List<NominateAsset> assetsIn;


  /**
   * ExchangeCommitData constructor.
   *
   * @param contractAddress :
   * @param assetsIn        :
   * @param toaddress       :
   * @param protocol        :
   * @param metadata        :
   */
  public ExchangeCommitData(
      String contractAddress,
      Object assetsIn,
      String toaddress,
      String protocol,
      String metadata
  ) {

    this.contractAddress = contractAddress;
    this.toaddress = toaddress;
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);

    this.assetsIn = decodeAssetList(assetsIn);

  }


  /**
   * ExchangeCommitData constructor.
   *
   * @param toCopy :
   */
  public ExchangeCommitData(ExchangeCommitData toCopy) {

    if (toCopy == null) {
      return;
    }

    this.contractAddress = toCopy.contractAddress;
    this.toaddress = toCopy.toaddress;
    this.protocol = toCopy.protocol;
    this.metadata = toCopy.metadata;

    this.assetsIn = cloneAssetList(toCopy.assetsIn);

  }


  /**
   * ExchangeCommitData constructor.
   *
   * @param sourceMap :
   */
  public ExchangeCommitData(Map<String, Object> sourceMap) {

    contractAddress = (String) sourceMap.getOrDefault(__ADDRESS, "");

    this.assetsIn = decodeAssetList(sourceMap.get(ASSETS_IN));

    this.toaddress = (String) sourceMap.getOrDefault(TO_ADDR, "");
    this.protocol = (String) sourceMap.getOrDefault(PROTOCOL, "");
    this.metadata = (String) sourceMap.getOrDefault(METADATA, "");

  }


  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    if ((this.toaddress != null) && (!this.toaddress.isEmpty())) {
      rVal.add(toaddress);
    }

    if ((this.assetsIn != null) && (!this.assetsIn.isEmpty())) {
      for (NominateAsset thisAsset : this.assetsIn) {
        if ((thisAsset != null) && (thisAsset.getAddress() != null) && (!thisAsset.getAddress().isEmpty())) {
          rVal.add(thisAsset.getAddress());
        }
      }
    }

    return rVal;

  }

  @SuppressWarnings({"squid:S1126", "squid:S3776"})
  @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
  @Override
  public boolean equals(Object toCompare) {

    // squid:S3776 : If this is too cognitively complex, find another job.

    // Something does not equal Nothing
    if (toCompare == null) {
      return false;
    }

    // Dis-similar objects are not equal.
    if (!(toCompare instanceof ExchangeCommitData)) {
      return false;
    }

    ExchangeCommitData theOther = (ExchangeCommitData) toCompare;

    // ContractAddress not match ?
    if ((!Objects.equals(contractAddress, theOther.contractAddress)) && ((contractAddress == null) || (!contractAddress
        .equalsIgnoreCase(theOther.contractAddress)))) {
      return false;
    }

    // toAddress not match ?
    if ((!Objects.equals(toaddress, theOther.toaddress)) && ((toaddress == null) || (!toaddress
        .equalsIgnoreCase(theOther.toaddress)))) {
      return false;
    }

    // Protocol not match ?
    if ((!Objects.equals(protocol, theOther.protocol)) && ((protocol == null) || (!protocol.equalsIgnoreCase(theOther.protocol)))) {
      return false;
    }

    // Metadata not match ?
    if ((!Objects.equals(metadata, theOther.metadata)) && ((metadata == null) || (!metadata.equalsIgnoreCase(theOther.metadata)))) {
      return false;
    }

    // Assets not match ?
    // squid:S1126 : This does not just return here in case the number of checks increases in the future,
    if ((assetsIn != theOther.assetsIn) && ((assetsIn == null) || (!assetsIn.equals(theOther.assetsIn)))) {
      return false;
    }

    // OK then...
    return true;
  }


  @Override
  public int hashCode() {

    return (this.toaddress.hashCode() + this.contractAddress.hashCode()) % Integer.MAX_VALUE;

  }

  @Override
  public IContractData copy() {
    return new ExchangeCommitData(this);

  }


  /**
   * implements IContractData encode().
   * <p>
   * Return Object array in correct format for serialisation (Persisting and Hashing).
   * </p>
   *
   * @return : MPWrappedMap{String, Object}
   */
  @Override
  public MPWrappedMap<String, Object> encode() {

    Map<String, Object> rVal = encodeToMap();

    return new MPWrappedMap<>(rVal);
  }


  /**
   * implements IContractData encode().
   * <p>
   * Return Object array in correct format for serialisation (Persisting and Hashing).
   * </p>
   *
   * @return : MPWrappedMap{String, Object}
   */
  @Override
  public Map<String, Object> encodeToMap() {

    TreeMap<String, Object> rVal = new TreeMap<>();

    if (this.contractAddress != null) {
      rVal.put(__ADDRESS, this.contractAddress);
    }

    if (this.protocol != null) {
      rVal.put(PROTOCOL, this.protocol);
    }

    if (this.toaddress != null) {
      rVal.put(TO_ADDR, this.toaddress);
    }

    if (this.metadata != null) {
      rVal.put(METADATA, this.metadata);
    }

    if (this.assetsIn != null) {
      rVal.put(ASSETS_IN, encodeAssetList(this.assetsIn));
    }

    rVal.put(__FUNCTION, CONTRACT_NAME_EXCHANGE_COMMIT);

    return rVal;
  }


  @Override
  public Map<String, Object> encodeToMapForTxParameter() {

    return encodeToMap();
  }


  public List<NominateAsset> getAssetsIn() {

    return assetsIn;
  }


  @Override
  public String getContractType() {

    return CONTRACT_NAME_EXCHANGE_COMMIT;
  }


  @Override
  public String get__function() {

    return getContractType();
  }


  /**
   * setAssetsIn.
   * <p>Set 'AssetIn' details from the given Object which may be a MPWrappedArray or an Object Array</p>
   *
   * @param paramData :
   */
  public void setAssetsIn(Object paramData) {

    this.assetsIn = decodeAssetList(paramData);

  }


  @Override
  public long setNextTimeEvent(long updateTime, boolean forceToUpdateTime) {
    // No Time Events on Commit data

    return 0L;
  }


  @Override
  public JSONObject toJSON() {
    JSONObject rVal = new JSONObject(true);

    rVal.put(__FUNCTION, get__function());
    rVal.put(__ADDRESS, contractAddress);

    rVal.put(ASSETS_IN, encodeAssetList(this.assetsIn));

    if (protocol != null) {
      rVal.put(PROTOCOL, protocol);
    }

    if ((toaddress != null) && (toaddress.length() > 0)) {
      rVal.put(TO_ADDR, toaddress);
    }

    if (metadata != null) {
      rVal.put(METADATA, metadata);
    }

    return rVal;
  }

}
