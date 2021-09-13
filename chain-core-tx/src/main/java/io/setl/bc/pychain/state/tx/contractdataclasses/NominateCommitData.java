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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.AMOUNT;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSETS_IN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSET_CLASS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSET_IN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CLASS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.METADATA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.NAMESPACE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PROTOCOL;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PUBLIC_KEY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.SIGNATURE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__FUNCTION;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_NOMINATE;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.pychain.common.ToObjectArray;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class NominateCommitData implements IContractData {

  @SuppressWarnings("squid:CommentedOutCodeLine")
  /*
  'function' = nominate : // Java

     {
     'contractaddress'    :          #
     'Namespace',         :          #
     'Class',             :          #
     
     'assetin'  : [[
                   Amount,           #
                   PublicKey,        # If Blank, then Commit Tx Effective (Authoring or PoA) address is assumed
                                     # If is set to Effective Address or Public key, then no signature is required, as the Commit Tx itself is signed.
                                       If Asset is to be taken from a different addres, then FromAddress (or Public Key) may be specified in the API. The
                                       Wallet node will replace with Public key and sign if it can.
                   Signature         # [contractaddress|AssetAddress|Amount|AuthoringAddress|Tx Nonce].
                                       Signature only required if FromAddress != Tx Authoring address.
                   ], ...
                  ]
     'protocol'           : "",      # User Data
     'metadata'           : ""
     'autosign'           : Boolean  # Defaults to True. If True, the Wallet node will attempt to sign the contract with the authoring key
     'signforwallet'      : Boolean  # Defaults to False. If True (and autosign is true), the Wallet node will attempt to sign the contract with any keys
     }
  */
  private static final Logger logger = LoggerFactory.getLogger(NominateCommitData.class);



  public static class AssetIn implements ToObjectArray {

    public final Balance amount;

    public String signature;

    private String address;

    private String publicKey;


    /**
     * AssetIn Constructor.
     *
     * @param amount    :
     * @param publicKey :
     * @param signature :
     */
    @JsonCreator
    public AssetIn(
        @JsonProperty("amount") Number amount,
        @JsonProperty("publicKey") String publicKey,
        @JsonProperty("signature") String signature
    ) {
      this.amount = new Balance(amount);
      this.publicKey = publicKey;
      this.signature = signature;
      this.address = null;
    }


    /**
     * AssetIn Constructor.
     *
     * @param partyData :
     */
    public AssetIn(MPWrappedArray partyData) {

      this.amount = new Balance(partyData.get(0));
      this.publicKey = partyData.asString(1);
      this.signature = partyData.asString(2);
      this.address = null;
    }


    /**
     * AssetIn Constructor.
     *
     * @param partyData :
     */
    public AssetIn(Object[] partyData) {

      this.amount = new Balance(partyData[0]);
      this.publicKey = (String) partyData[1];
      this.signature = (String) partyData[2];
      this.address = null;
    }


    /**
     * AssetIn Copy Constructor.
     *
     * @param toCopy :
     */
    public AssetIn(AssetIn toCopy) {

      this.amount = toCopy.amount;
      this.publicKey = toCopy.publicKey;
      this.signature = toCopy.signature;
      this.address = null;
    }


    @Override
    public Object[] encode(long index) {

      return new Object[]{amount.getValue(), publicKey, signature};
    }


    /**
     * encodeJson.
     * <p>Encode AssetIn data as JSONObject.</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(AMOUNT, amount.getValue());
      rVal.put(PUBLIC_KEY, publicKey);
      rVal.put(SIGNATURE, signature);

      return rVal;

    }


    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof AssetIn)) {
        return false;
      }

      AssetIn theOther = (AssetIn) toCompare;

      if ((!Objects.equals(amount, theOther.amount)) && ((amount == null) || (!amount.equalTo(theOther.amount)))) {
        return false;
      }

      if ((!Objects.equals(publicKey, theOther.publicKey)) && ((publicKey == null) || (!publicKey.equals(theOther.publicKey)))) {
        return false;
      }

      return (Objects.equals(signature, theOther.signature)) || ((signature != null) && (signature.equals(theOther.signature)));
    }


    /**
     * DvpCommitEncumbrance, getAddress.
     * <p>Return public key as Address, cached.</p>
     *
     * @return :
     */
    public String getAddress() {

      if (address == null) {
        // If an EdDSA key, then the address could be Base-58 or Base-64. We have to trust the defaults.
        address = AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL);
      }
      return address;
    }


    public String getPublicKey() {
      return publicKey;
    }


    @Override
    public int hashCode() {

      return ((amount == null ? 0 : amount.getValue().hashCode()) + (publicKey == null ? 0 : publicKey.hashCode()) + (signature == null ? 0
          : signature.hashCode())) % Integer.MAX_VALUE;

    }


    public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
      this.address = null;
    }


    /**
     * Get the string that should be hashed and signed for this commitment.
     *
     * @param contractAddress  the contract's address
     * @param authoringAddress the author of this commitment
     * @param nonce            a nonce
     *
     * @return the string to process
     */
    public String stringToHashToSign(String contractAddress, String authoringAddress, Number nonce) {

      return String.format("%s|%s|%s|%s|%d",
          contractAddress,
          this.getAddress(),
          (amount != null ? amount.toString() : "Missing"),
          authoringAddress,
          (nonce != null ? nonce.longValue() : 0));

    }

  }



  public String contractAddress;

  public String metadata;

  public String protocol;

  private String assetclass;

  private String assetid;

  private List<AssetIn> assetsIn;

  private String namespace;


  /**
   * NominateCommitData constructor.
   *
   * @param namespace       :
   * @param assetclass      :
   * @param protocol        :
   * @param metadata        :
   * @param contractAddress :
   */
  public NominateCommitData(String namespace,
      String assetclass,
      String protocol,
      String metadata,
      String contractAddress
  ) {

    this.namespace = (namespace == null ? "" : namespace);
    this.assetclass = (assetclass == null ? "" : assetclass);
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.contractAddress = (contractAddress == null ? "" : contractAddress);
    assetid = null;
    assetsIn = null;
  }


  public NominateCommitData(Object[] commitData) {

    this(new MPWrappedMap<>(commitData));
  }


  /**
   * NominateCommitData Constructor.
   *
   * @param sourceMap :
   */
  public NominateCommitData(Map<String, Object> sourceMap) {

    this(new MPWrappedMap<>(sourceMap));

  }


  /**
   * NominateCommitData Constructor.
   *
   * @param asMapValue :
   */
  public NominateCommitData(MPWrappedMap<String, Object> asMapValue) {

    contractAddress = null;

    asMapValue.iterate((k, v) -> {

      switch (k.toLowerCase()) {

        case __ADDRESS:
          contractAddress = (v == null ? "" : (String) v);
          break;

        case CONTRACT_ADDRESS:
          if (contractAddress == null) {
            contractAddress = (v == null ? "" : (String) v);
          }
          break;

        case NAMESPACE:
          namespace = (v == null ? "" : (String) v);
          break;

        case CLASS:
        case ASSET_CLASS:
          assetclass = (v == null ? "" : (String) v);
          break;

        case PROTOCOL:
          protocol = (String) v;
          break;

        case METADATA:
          metadata = (String) v;
          break;

        case ASSET_IN:
        case ASSETS_IN:
          setAssetsIn(v);
          break;

        default:
          break;

      }
    });

    if (this.namespace == null) {
      this.namespace = "";
    }

    if (this.assetclass == null) {
      this.assetclass = "";
    }

    if (this.contractAddress == null) {
      this.contractAddress = "";
    }

    assetid = null;

  }


  /**
   * NominateCommitData Copy Constructor.
   *
   * @param toCopy :
   */
  public NominateCommitData(NominateCommitData toCopy) {

    if (toCopy == null) {
      return;
    }

    contractAddress = toCopy.contractAddress;
    namespace = toCopy.getNamespace();
    assetclass = toCopy.getAssetclass();
    protocol = toCopy.protocol;
    metadata = toCopy.metadata;

    if (toCopy.assetsIn != null) {
      this.assetsIn = new ArrayList<>();

      toCopy.assetsIn.forEach(thisItem -> {
            this.assetsIn.add(new AssetIn(thisItem));
          }
      );
    }

    assetid = null;

  }


  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    if (this.assetsIn != null) {
      for (AssetIn thisIn : this.assetsIn) {
        if (thisIn.address != null) {
          rVal.add(thisIn.address);
        }
      }
    }

    return rVal;
  }


  @Override
  public IContractData copy() {

    return new NominateCommitData(this);
  }


  private List<AssetIn> decodeAssetsIn(Object[] paramData) {

    return decodeAssetsIn(new MPWrappedArrayImpl(paramData));
  }


  @SuppressWarnings("squid:S1168")
  private List<AssetIn> decodeAssetsIn(MPWrappedArray paramData) {

    if ((paramData == null) || (paramData.size() == 0)) {
      return null;
    }

    ArrayList<AssetIn> newList = new ArrayList<>();

    for (int index = 0, l = paramData.size(); index < l; index++) {
      newList.add(new AssetIn(paramData.asWrapped(index)));
    }

    return newList;
  }


  @Override
  public MPWrappedMap<String, Object> encode() {

    Map<String, Object> rVal = encodeToMap();

    return new MPWrappedMap<>(rVal);

  }


  @SuppressWarnings("squid:S1168") // Return null
  private Object[] encodeAssetsIn() {

    if (this.assetsIn == null) {
      return null;
    }

    Object[] rVal = new Object[this.assetsIn.size()];

    final int[] index = {0};

    this.assetsIn.forEach(detail -> {
      rVal[index[0]++] = detail.encode(0);
    });

    return rVal;
  }


  private JSONArray encodeAssetsInJson() {

    JSONArray rVal = new JSONArray();

    this.assetsIn.forEach(detail -> {
      rVal.add(detail.encodeJson());
    });

    return rVal;

  }


  @Override
  public Map<String, Object> encodeToMap() {

    TreeMap<String, Object> rVal = new TreeMap<>();

    if (this.contractAddress != null) {
      rVal.put(CONTRACT_ADDRESS, this.contractAddress);
    }

    if (this.namespace != null) {
      rVal.put(NAMESPACE, this.namespace);
    }

    if (this.assetclass != null) {
      rVal.put(ASSET_CLASS, this.assetclass);
    }

    if (this.protocol != null) {
      rVal.put(PROTOCOL, this.protocol);
    }

    if (this.metadata != null) {
      rVal.put(METADATA, this.metadata);
    }

    if (this.assetsIn != null) {
      rVal.put(ASSETS_IN, encodeAssetsIn());
    }

    rVal.put(__FUNCTION, CONTRACT_NAME_NOMINATE);

    return rVal;
  }


  @Override
  public Map<String, Object> encodeToMapForTxParameter() {

    return encodeToMap();
  }


  @Override
  @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
  @SuppressWarnings("squid:S3776")
  public boolean equals(Object toCompare) {

    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof NominateCommitData)) {
      return false;
    }

    NominateCommitData theOther = (NominateCommitData) toCompare;

    if ((!Objects.equals(contractAddress, theOther.contractAddress)) && ((contractAddress == null) || (!contractAddress
        .equalsIgnoreCase(theOther.contractAddress)))) {
      return false;
    }

    if ((!Objects.equals(namespace, theOther.getNamespace())) && ((namespace == null) || (!namespace.equalsIgnoreCase(theOther.getNamespace())))) {
      return false;
    }

    if ((!Objects.equals(assetclass, theOther.getAssetclass())) && ((assetclass == null) || (!assetclass.equalsIgnoreCase(theOther.getAssetclass())))) {
      return false;
    }

    if ((!Objects.equals(protocol, theOther.protocol)) && ((protocol == null) || (!protocol.equalsIgnoreCase(theOther.protocol)))) {
      return false;
    }

    if ((!Objects.equals(metadata, theOther.metadata)) && ((metadata == null) || (!metadata.equalsIgnoreCase(theOther.metadata)))) {
      return false;
    }

    return (assetsIn == theOther.assetsIn) || ((assetsIn != null) && (assetsIn.equals(theOther.assetsIn)));
  }


  /**
   * getAssetID.
   *
   * @return : Performance wrapper for getting combined AssetID.
   */
  public String getAssetID() {

    if (assetid == null) {
      assetid = (namespace == null ? "" : namespace) + "|" + (assetclass == null ? "" : assetclass);
    }

    return assetid;
  }


  public String getAssetclass() {

    return assetclass;
  }


  public List<AssetIn> getAssetsIn() {

    return assetsIn;
  }


  @Override
  public String getContractType() {

    return CONTRACT_NAME_NOMINATE;
  }


  public String getNamespace() {

    return namespace;
  }


  @Override
  public String get__function() {

    return getContractType();
  }


  @Override
  public int hashCode() {

    return (
        (this.contractAddress != null ? this.contractAddress.hashCode() : 0)
            + (this.namespace != null ? this.namespace.hashCode() : 0)
            + (this.assetclass != null ? this.assetclass.hashCode() : 0)) % Integer.MAX_VALUE;
  }


  /**
   * setAssetIn.
   * <p>Add the given AssetIn Object to this CommintData Object.</p>
   *
   * @param parameter :
   */
  public void setAssetIn(AssetIn parameter) {

    if (parameter == null) {
      return;
    }

    if (this.assetsIn == null) {
      this.assetsIn = new ArrayList<>();
    }

    this.assetsIn.add(parameter);
  }


  public void setAssetclass(String assetclass) {

    this.assetclass = assetclass;
  }


  /**
   * setAssetsIn.
   * <p>Set 'AssetIn' details from the given Object which may be a MPWrappedArray or an Object Array</p>
   *
   * @param paramData :
   */
  public void setAssetsIn(Object paramData) {

    if (paramData != null) {
      if (paramData instanceof MPWrappedArray) {
        this.assetsIn = decodeAssetsIn((MPWrappedArray) paramData);
      } else if (paramData instanceof Object[]) {
        this.assetsIn = decodeAssetsIn((Object[]) paramData);
      } else if (paramData instanceof List) {
        this.assetsIn = decodeAssetsIn(((List) paramData).toArray());
      }
    } else {
      this.assetsIn = null;
    }
  }


  public void setNamespace(String namespace) {

    this.namespace = (namespace == null ? "" : namespace);
  }


  @Override
  public long setNextTimeEvent(long updateTime, boolean forceToUpdateTime) {
    // No Time Events on Commit data

    return 0L;
  }


  @Override
  public JSONObject toJSON() {

    JSONObject rVal = new JSONObject(true);

    if (this.contractAddress != null) {
      rVal.put(CONTRACT_ADDRESS, this.contractAddress);
    }

    if (this.namespace != null) {
      rVal.put(NAMESPACE, this.namespace);
    }

    if (this.assetclass != null) {
      rVal.put(ASSET_CLASS, this.assetclass);
    }

    if (this.protocol != null) {
      rVal.put(PROTOCOL, this.protocol);
    }

    if (this.metadata != null) {
      rVal.put(METADATA, this.metadata);
    }

    if (this.assetsIn != null) {
      rVal.put(ASSETS_IN, encodeAssetsInJson());
    }

    rVal.put(__FUNCTION, CONTRACT_NAME_NOMINATE);

    return rVal;
  }


}