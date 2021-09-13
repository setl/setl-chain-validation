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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.BLOCK_SIZE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CLASSID;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.NAMESPACE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PUBLIC_KEY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.SIGNATURE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.setl.bc.pychain.common.ToObjectArray;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.Balance;

@JsonInclude(Include.NON_NULL)
public class NominateAsset implements ToObjectArray {

  /*
       output : [ [
              namespace,
              classid,
              blocksize,
              address,
              reference,
              publickey,
              signature
              ], ...
   */


  /**
   * Utility function to clone (Deep Copy) an AssetList().
   *
   * @param sourceList :
   *
   * @return :
   */
  public static List<NominateAsset> cloneAssetList(List<NominateAsset> sourceList) {

    if (sourceList == null) {
      return new ArrayList<>(0);
    }

    ArrayList<NominateAsset> rVal = new ArrayList<>(sourceList.size());

    for (NominateAsset thisAsset : sourceList) {
      if (thisAsset != null) {
        rVal.add(new NominateAsset(thisAsset));
      }
    }

    return rVal;
  }


  /**
   * Utility function to decode an AssetList().
   *
   * @param sourceList :
   *
   * @return :
   */
  public static List<NominateAsset> decodeAssetList(Object sourceList) {

    if (sourceList == null) {
      return new ArrayList<>();
    }

    ArrayList<NominateAsset> rVal;

    if (sourceList instanceof NominateAsset) {
      rVal = new ArrayList<>(1);
      rVal.add((NominateAsset) sourceList);
      return rVal;
    }

    if (sourceList instanceof List) {
      rVal = new ArrayList<>(((List) sourceList).size());
      for (Object thisItem : (List) sourceList) {
        rVal.add(new NominateAsset(thisItem));
      }
      return rVal;
    }

    if (sourceList instanceof Object[]) {
      rVal = new ArrayList<>(((Object[]) sourceList).length);
      for (Object thisItem : (Object[]) sourceList) {
        rVal.add(new NominateAsset(thisItem));
      }
      return rVal;
    }

    return new ArrayList<>();
  }


  /**
   * Utility function to encode an AssetList().
   *
   * @param sourceList :
   *
   * @return : List of NominateAsset as Object[].
   */
  @SuppressWarnings("squid:S1168") // I intend to return a null, not an empty array (Encoding space & backwards compatability).
  public static Object[] encodeAssetList(List<NominateAsset> sourceList) {

    if (sourceList == null) {
      return null;
    }

    Object[] rVal = new Object[sourceList.size()];
    int i = 0;

    for (NominateAsset thisAsset : sourceList) {
      rVal[i++] = ((thisAsset != null) ? thisAsset.encode(0) : null);
    }

    return rVal;
  }


  /**
   * encodeAssetListToJSON.
   * <p>Return Asset List data as JSONArray</p>
   *
   * @return :
   */
  public static JSONArray encodeAssetListToJSON(List<NominateAsset> sourceList) {

    JSONArray rVal = new JSONArray();

    for (NominateAsset thisAsset : sourceList) {
      rVal.add((thisAsset != null) ? thisAsset.encodeJson() : null);
    }

    return rVal;
  }


  @JsonIgnore
  public final Balance blocksize;

  @Schema(description = "The ID of the asset")
  @JsonProperty("assetId")
  public final String classid;

  @Schema(description = "The namespace that contains the asset")
  @JsonProperty("namespace")
  public final String namespace;

  @Schema(description = "A reference")
  @JsonProperty("reference")
  public final String reference;

  @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
  @JsonIgnore
  public boolean isIssuer = false; // Transitory value, not persisted or hashed.

  @SuppressWarnings("squid:ClassVariableVisibilityCheckClass")
  @Schema(description = "The public key that signed this")
  @JsonProperty("publicKey")
  public String publickey = null;

  @SuppressWarnings("squid:ClassVariableVisibilityCheckClass")
  @Schema(description = "The signature on this")
  @JsonProperty("signature")
  public String signature = null;

  // It is decided that there is no coding benefit, but a small performance hit, to be had from having getters / setters on these values.

  private String address = null;

  private String fullAssetID;


  /**
   * NominateAsset constructor.
   *
   * @param namespace :
   * @param classid   :
   * @param blocksize :
   */
  public NominateAsset(
      String namespace,
      String classid,
      Number blocksize
  ) {

    this.namespace = (namespace == null ? "" : namespace);
    this.classid = (classid == null ? "" : classid);
    this.blocksize = new Balance(blocksize == null ? 0L : blocksize);
    this.reference = null;
  }


  /**
   * NominateAsset constructor.
   *
   * @param namespace :
   * @param classid   :
   * @param blocksize :
   * @param address   :
   * @param reference :
   * @param publickey :
   * @param signature :
   */
  @JsonCreator
  public NominateAsset(
      @JsonProperty("namespace") String namespace,
      @JsonProperty("assetId") String classid,
      @JsonProperty("amount") Number blocksize,
      @JsonProperty("address") String address,
      @JsonProperty("reference") String reference,
      @JsonProperty("publicKey") String publickey,
      @JsonProperty("signature") String signature
  ) {
    this.namespace = (namespace == null ? "" : namespace);
    this.classid = (classid == null ? "" : classid);
    this.blocksize = new Balance(blocksize == null ? 0L : blocksize);
    this.address = address;
    this.reference = reference;
    this.publickey = publickey;
    this.signature = signature;
  }


  /**
   * NominateAsset constructor.
   * <p>
   * Handles both copy constructor behavior and de-serialisation.
   * </p>
   *
   * @param data :
   */
  @SuppressWarnings({"squid:S3776"}) // This is not overly complex !
  public NominateAsset(Object data) {

    // normal 'copy' constructor.

    if (data instanceof NominateAsset) {
      NominateAsset toCopy = (NominateAsset) data;

      this.namespace = toCopy.namespace;
      this.classid = toCopy.classid;
      this.blocksize = toCopy.blocksize;
      this.address = toCopy.address;
      this.reference = toCopy.reference;
      this.publickey = toCopy.publickey;
      this.signature = toCopy.signature;

      return;
    }

    MPWrappedArray arrayData = null;

    // Normalise likely de-serialisation parameter to a MPWrapped array.

    if (data instanceof MPWrappedArray) {
      arrayData = (MPWrappedArray) data;
    } else if (data instanceof Object[]) {
      arrayData = new MPWrappedArrayImpl((Object[]) data);
    } else if (data instanceof List) {
      arrayData = new MPWrappedArrayImpl(((List) data).toArray());
    }

    // De-Serialise :

    if (arrayData != null) {

      int dLen = arrayData.size();

      this.namespace = (((dLen > 0) && (arrayData.get(0) != null)) ? arrayData.get(0).toString() : "");
      this.classid = (((dLen > 1) && (arrayData.get(1) != null)) ? arrayData.get(1).toString() : "");
      this.blocksize = (((dLen > 2) && (arrayData.get(2) != null)) ? new Balance(arrayData.get(2)) : Balance.BALANCE_ZERO);

      this.address = (((dLen > 3) && (arrayData.get(3) != null)) ? arrayData.get(3).toString() : null);
      this.reference = (((dLen > 4) && (arrayData.get(4) != null)) ? arrayData.get(4).toString() : null);
      this.publickey = (((dLen > 5) && (arrayData.get(5) != null)) ? arrayData.get(5).toString() : null);
      this.signature = (((dLen > 6) && (arrayData.get(6) != null)) ? arrayData.get(6).toString() : null);

    } else {
      // Default values for class constructor.

      this.reference = "";
      this.namespace = "";
      this.classid = "";
      this.blocksize = Balance.BALANCE_ZERO;
    }
  }


  @Override
  public Object[] encode(long unused) {

    return new Object[]{
        this.namespace,
        this.classid,
        this.blocksize.getValue(),
        this.address,
        this.reference,
        this.publickey,
        this.signature,
    };
  }


  /**
   * encodeJson.
   * <p>Return DVP PayItem data as JSONObject</p>
   *
   * @return :
   */
  public JSONObject encodeJson() {

    JSONObject rVal = new JSONObject(true);

    rVal.put(NAMESPACE, this.namespace);
    rVal.put(CLASSID, this.classid);
    rVal.put(BLOCK_SIZE, this.blocksize);

    if (this.address != null) {
      rVal.put(ADDRESS, this.address);
    }

    if (this.reference != null) {
      rVal.put(REFERENCE, this.reference);
    }

    if (this.publickey != null) {
      rVal.put(PUBLIC_KEY, this.publickey);
    }

    if (this.signature != null) {
      rVal.put(SIGNATURE, this.signature);
    }

    return rVal;
  }


  @SuppressWarnings({"squid:S1126", "squid:S3776"})
  @Override
  public boolean equals(Object toCompare) {

    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof NominateAsset)) {
      return false;
    }

    NominateAsset theOther = (NominateAsset) toCompare;

    if ((!Objects.equals(namespace, theOther.namespace)) && ((namespace == null) || (!namespace.equals(theOther.namespace)))) {
      return false;
    }

    if ((!Objects.equals(classid, theOther.classid)) && ((classid == null) || (!classid.equals(theOther.classid)))) {
      return false;
    }

    if (this.blocksize != null) {
      if (!this.blocksize.equalTo(theOther.blocksize)) {
        return false;
      }
    } else {
      // this.blocksize == null
      if (theOther.blocksize != null) {
        return false;
      }
    }

    if ((!Objects.equals(address, theOther.address)) && ((address == null) || (!address.equals(theOther.address)))) {
      return false;
    }

    if ((!Objects.equals(reference, theOther.reference)) && ((reference == null) || (!reference.equals(theOther.reference)))) {
      return false;
    }

    if ((!Objects.equals(publickey, theOther.publickey)) && ((publickey == null) || (!publickey.equals(theOther.publickey)))) {
      return false;
    }

    if ((!Objects.equals(signature, theOther.signature)) && ((signature == null) || (!signature.equals(theOther.signature)))) {
      return false;
    }

    return true;
  }


  public String getAddress() {
    return address;
  }


  public Balance getAmount() {
    return blocksize;
  }


  /**
   * getFullAssetID().
   * <p>Return 'full' asset ID for thid Pay Item, the Asset ID is cached for minor performance reasons.</p>
   *
   * @return :
   */
  @JsonIgnore
  public String getFullAssetID() {

    if (fullAssetID == null) {
      fullAssetID = namespace + "|" + classid;
    }

    return fullAssetID;
  }


  @Override
  public int hashCode() {

    return getFullAssetID().hashCode();

  }


  /**
   * objectToHashToSign().
   *
   * @param contractAddress :
   *
   * @return :
   */
  public Object[] objectToHashToSign(String contractAddress) {

    return new Object[]{
        contractAddress,
        (address == null ? "" : address),
        getFullAssetID(),
        (reference == null ? "" : reference)
    };
  }

} // NominateAsset
