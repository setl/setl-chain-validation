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
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AMOUNT;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSET_ID;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AUTHORISE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AUTH_ID;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CANCEL;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.COMMITMENT;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_SPECIFIC;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ENCUMBRANCES;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.INDEX;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.METADATA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PARAMETERS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PARAMETER_NAME;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PARTY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PARTY_IDENTIFIER;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PUBLIC_KEY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.RECEIVE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFUSED;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.SIGNATURE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.VALUE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__FUNCTION;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;
import static io.setl.util.Constants.I_ZERO;
import static io.setl.util.Convert.objectToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.setl.bc.pychain.common.ToObjectArray;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;

@SuppressWarnings("squid:CommentedOutCodeLine")
public class DvpUKCommitData implements IContractData {

  /*

   {
   'party'         : [                    # Provides 'party' identifier and signature. Not required if the commit relates only to 'authorise','parameters',
                     PartyIdentifier,     # If no public key is specified on Contract creation, an index should be specified and a public key provided.
                     PubKey,              # Public key for this party (or for Authoring Party if POA)
                     Signature            # Signature of contractAddress to verify vs PubKey above.
                     ],
   'commitment'    : <Commitment Data>,   # optional, see below. Requires the 'party' data to be present.
   'receive'       : <Receive Data>,      # optional, see below. Requires the 'party' data to be present.
   'authorise'     : <Authorise Data>,    # optional, see below
   'parameters'    : <Parameters Data>,   # optional, see below
   'encumbrances'  : <addEncumbrance Data># optional, see below
   'cancel'        : <Cancellation Data>  # optional. Note if 'cancel' is given, all other parameters are ignored.
   'autosign'      : Boolean              # Defaults to True. If True, the Wallet node will attempt to sign the contract with the authoring key
   'signforwallet' : Boolean              # Defaults to False. If True (and autosign is true), the Wallet node will attempt to sign the contract with any keys
                                            in the current wallet
   }

   Commitment Data :
   dvp_uk : <Optional>  Supplies Public Key and Signature Data. Address may be derived from the Public Key.
      [
         Internal representation :
         [
         Index,                 # If no public key is specified on Contract creation, an index should be specified and a public key provided.
         PublicKey,             # Address or Hex Public key. Will only be Address during Tx Creation when the autosigner should replace it with the Public Key.
         Signature              # Base64. Signature of [NameSpace, AssetID, Qty] by Address1
         ],

         When submitting Commitments to the wallet node, they need to be in the format :
         The Namespace, ClassID and Amount must be provided do that the Wallet node may create the signature. Internally they are dropped there-after.
         [
         Index,                 # If no public key is specified on Contract creation, an index should be specified and a public key provided.
         Namespace,             # Required by the Wallet node to generate the signature message.
         ClassID,               # Required by the Wallet node to generate the signature message.
         Amount,                # Required by the Wallet node to generate the signature message.
         PublicKey,             # (API Optional) Address or Hex Public key. Will only be Address during Tx Creation when the autosigner should replace it
                                  with the Public key.
                                  For PoA commitments this will be set to the Attorney Public Key.
         Signature              # (API Optional) Base64. Signature of [NameSpace, AssetID, Qty] by PublicKey
         ],
         ...
      ]

   Receive Data:
   dvp_uk : <Optional>  Allows the Party to supply Receipt addresses if not specified
      [
         [
         Index,                 # Base 0 index
         Address                #
         ],
         ...
      ]

   Authorise Data:
   dvp_uk : <Optional>  Allows the Party to supply Authorisation signatures
      [
         [
         PublicKey,             # Public key for authorisation. Alternatively, an Address may be specified, in which case it will be signed and substituted
                                # for the PubKey in the TX creation code, permissions and commit parameters (signforwallet and autosign) allowing.
                                # For POA, the Public Key & Signature must be for the Authoring Address. The public key given here will be saved to the
                                # 'PoaPublicKey' on the underlying contract authorisation. The Wallet node will automatically add as required.
         AuthID,                #
         Signature,             # (API Optional) Signature data. If the `PublicKey` value matches the TX Authoring address, then this signature will be
                                # generated as part of the TX creation code. For PoA, both the public key and the signature will be set to the authoring
                                # address credentials.
         Metadata,              # (API Optional) This data will be inserted into the Contract code on acceptance of this Authorisation.
         Refused,               # (API Optional) If true (default false), then this Authorisation is 'Refused'. Refused Authorisations will not count as
                                  signed. This value may be set during Commit TXs.
         ContractSpecific,      # (API Optional) (Int) 0 : Signature message does not include ContractAddress, 1 : It does. (1 is the default)
         ],
         ...
      ]

   Parameters Data:
      [
         [
         Parameter name,        # Parameter 'key' name to update
         Value,                 # Parameter value to insert.
         ContractSpecific,      # (API Optional) (Int) 0 : (default) Signature message does not include ContractAddress, 1 : It does.
         PublicKey,             # (API Optional) Public key for authorisation. Alternatively the TX Authoring address may be specified, or the Authoring
                                  Address will be inserted if left blank, in which case this parameter will be signed by the authoring PubKey in the TX
                                  creation code.
                                  For PoA commitments this will be set to the Attorney Public Key.
         Signature,             # (API Optional) Signature data. If the `PublicKey` value matches the TX Authoring address or is blank, then this signature
                                  will be generated as part of the TX creation code.
         ],
         ...
      ]

   addEncumbrance Data:
   dvp_uk : <Optional>  Allows the Party to supply addEncumrance signatures
      [
         [
         PublicKey,             # PublicKey may be the issuingAddress (of if omitted will be set to the Issuing Address), in which case it will automatically
                                  be replaced with the issuingPublicKey
         AssetID,               # full Asset ID  <Namespace>|<Class>
         Reference,             #
         Amount,                #
         Signature              #
         ],
         ...
      ]

   Cancellation Data:
      [
      PublicKey,             # Public key for cancellation. Alternatively, an Address may be specified, provided it is the TX Authoring address, in which case
                             # it will be signed and substituted for the PubKey in the TX creation code.
                             # for POA, this is the Authoring Public key.
      Signature              # Signature data. If the `PublicKey` value matches the TX Authoring address or is blank, then this signature will be generated as
                             # part of the TX creation code.
                             # For POA, Signature of Authoring Private Key, Cancellation will require POA Address to be a party to
                             # the contract.
      ]

   */



  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpCommitAuthorise implements ToObjectArray {

    public final String authId;

    public final String metadata;

    public final boolean refused;

    public final Integer specific;

    public String signature;

    private String address;

    private String publicKey;


    /**
     * DvpCommitAuthorise constructor.
     *
     * @param publicKey :
     * @param authId    :
     * @param signature :
     * @param metadata  :
     * @param refused   :
     */
    public DvpCommitAuthorise(String publicKey, String authId, String signature, String metadata, boolean refused, Integer specific) {

      this.publicKey = publicKey;
      this.authId = objectToString(authId);
      this.signature = signature;
      this.metadata = metadata;
      this.refused = refused;
      this.specific = specific;
      this.address = null;
    }


    /**
     * DvpCommitAuthorise class de-serialisation constructor.
     *
     * @param authoriseData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitAuthorise(MPWrappedArray authoriseData) {

      int dLen = (authoriseData != null ? authoriseData.size() : 0);

      this.publicKey = (dLen > 0 ? authoriseData.asString(0) : "");
      this.authId = (dLen > 1 ? objectToString(authoriseData.get(1)) : "");
      this.signature = (dLen > 2 ? authoriseData.asString(2) : "");
      this.metadata = (dLen > 3 ? authoriseData.asString(3) : "");
      this.refused = (dLen > 4 ? authoriseData.asBoolean(4) : false);
      this.specific = (dLen > 5 ? authoriseData.asInt(5) : 1);
      this.address = null;
    }


    /**
     * DvpCommitAuthorise class de-serialisation constructor.
     *
     * @param authoriseData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitAuthorise(Object[] authoriseData) {

      int dLen = (authoriseData != null ? authoriseData.length : 0);

      this.publicKey = (dLen > 0 ? authoriseData[0] : "").toString();

      if (dLen > 1) {
        this.authId = (authoriseData[1] == null ? "" : authoriseData[1].toString());
      } else {
        this.authId = "";
      }

      this.signature = (dLen > 2 ? authoriseData[2] : "").toString();
      this.metadata = (dLen > 3 ? authoriseData[3] : "").toString();
      this.refused = (Boolean) (dLen > 4 ? authoriseData[4] : Boolean.FALSE);

      if ((dLen > 5) && (authoriseData[5] != null)) {
        if (authoriseData[5] instanceof Number) {
          this.specific = ((Number) authoriseData[5]).intValue();
        } else {
          this.specific = Integer.valueOf(authoriseData[5].toString());
        }
      } else {
        this.specific = 1;
      }

      this.address = null;
    }


    /**
     * DvpCommitAuthorise class copy constructor.
     *
     * @param toCopy :
     */
    public DvpCommitAuthorise(DvpCommitAuthorise toCopy) {

      this.publicKey = toCopy.publicKey;
      this.authId = toCopy.authId;
      this.signature = toCopy.signature;
      this.metadata = toCopy.metadata;
      this.refused = toCopy.refused;
      this.specific = toCopy.specific;
      this.address = null;
    }


    @Override
    public Object[] encode(long index) {

      if (this.specific.equals(0)) {
        // backwards compatibility.
        return new Object[]{publicKey, authId, signature, metadata, refused, specific};
      } else {
        return new Object[]{publicKey, authId, signature, metadata, refused};
      }
    }


    /**
     * encodeJson.
     * <p>Return CommitAuthorise data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(PUBLIC_KEY, publicKey);
      rVal.put(AUTH_ID, authId);
      rVal.put(SIGNATURE, signature);
      rVal.put(METADATA, metadata);
      rVal.put(REFUSED, refused);
      rVal.put(CONTRACT_SPECIFIC, specific);

      return rVal;
    }


    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpCommitAuthorise)) {
        return false;
      }

      DvpCommitAuthorise theOther = (DvpCommitAuthorise) toCompare;

      if (!Objects.equals(authId, theOther.authId)) {
        return false;
      }

      if (!Objects.equals(publicKey, theOther.publicKey)) {
        return false;
      }

      if (!Objects.equals(signature, theOther.signature)) {
        return false;
      }

      if (!Objects.equals(metadata, theOther.metadata)) {
        return false;
      }

      if (!Objects.equals(specific, theOther.specific)) {
        return false;
      }

      return refused == theOther.refused;
    }


    /**
     * getAddress().
     *
     * @return :
     */
    public String getAddress() {
      // TODO : the address should have been stored, rather than trying to reconstruct it by guessing the encoding and type.
      if (address == null) {
        // If an EdDSA key, then the address could be Base-58 or Base-64. We have to trust the defaults.
        address = AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL);
      }
      return address;
    }


    // TODO - it is assumed that this public key could actually be an address. It should have a fixed data type.
    public String getPublicKey() {
      return publicKey;
    }


    @Override
    public int hashCode() {

      return ((publicKey == null ? 0 : publicKey.hashCode()) + (authId == null ? 0 : authId.hashCode())) % Integer.MAX_VALUE;

    }


    public boolean isContractSpecific() {

      return (!this.specific.equals(0));
    }


    public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
      this.address = null;
    }


    /**
     * stringToHashToSign.
     * <p>Return the String that represents the data content of this Authorisation that is used as the data to sign.</p>
     *
     * @param contractAddress the contract's address
     *
     * @return :
     */
    public String stringToHashToSign(String contractAddress) {

      if (this.specific.equals(0)) {
        return String.format("%s_%d", authId, (refused ? 1 : 0));
      } else {
        return String.format("%s%s_%d", contractAddress, authId, (refused ? 1 : 0));
      }
    }

  }



  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpCommitCancel implements ToObjectArray {
    // TODO - this can be an address or a public key. It should have a concrete type.
    public String publicKey;

    public String signature;


    /**
     * DvpCommitCancel constructor.
     *
     * @param publicKey :
     * @param signature :
     */
    public DvpCommitCancel(String publicKey, String signature) {

      this.publicKey = publicKey;
      this.signature = signature;
    }


    /**
     * DvpCommitCancel constructor from MPWrapped array.
     *
     * @param data :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitCancel(MPWrappedArray data) {

      int dLen = (data != null ? data.size() : 0);

      this.publicKey = (dLen > 0 ? data.asString(0) : "");
      this.signature = (dLen > 1 ? data.asString(1) : "");
    }


    /**
     * DvpCommitCancel constructor from Object array.
     *
     * @param data :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitCancel(Object[] data) {

      int dLen = (data != null ? data.length : 0);

      this.publicKey = (String) (dLen > 0 ? data[0] : "");
      this.signature = (String) (dLen > 1 ? data[1] : "");
    }


    /**
     * DvpCommitCancel, Copy constructor.
     *
     * @param toCopy :
     */
    public DvpCommitCancel(DvpCommitCancel toCopy) {

      this.publicKey = toCopy.publicKey;
      this.signature = toCopy.signature;

    }


    @Override
    public Object[] encode(long index) {

      return new Object[]{publicKey, signature};
    }


    /**
     * encodeJson.
     * <p>Return cancel data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(PUBLIC_KEY, publicKey);
      rVal.put(SIGNATURE, signature);

      return rVal;
    }


    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpCommitCancel)) {
        return false;
      }

      DvpCommitCancel theOther = (DvpCommitCancel) toCompare;

      if (!Objects.equals(publicKey, theOther.publicKey)) {
        return false;
      }

      return Objects.equals(signature, theOther.signature);
    }


    @Override
    public int hashCode() {

      return ((publicKey == null ? 0 : publicKey.hashCode()) + (signature == null ? 0 : signature.hashCode())) % Integer.MAX_VALUE;

    }


    /**
     * Get the message which will be signed for the transaction.
     *
     * @param contractAddress the contract's address
     *
     * @return the message to sign
     */
    public String stringToHashToSign(String contractAddress) {
      return "cancel_" + ((contractAddress != null) ? contractAddress : "");
    }

  }



  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpCommitEncumbrance implements ToObjectArray {

    public final Balance amount;

    public final String amountString;

    public final String assetID;

    public final String reference;

    public String signature;

    private String address;

    private String publicKey;


    /**
     * DvpCommitEncumbrance constructor.
     *
     * @param publicKey :
     * @param assetID   :
     * @param reference :
     * @param amount    :
     * @param signature :
     */
    public DvpCommitEncumbrance(String publicKey, String assetID, String reference, Number amount, String signature) {

      this.publicKey = publicKey;
      this.assetID = assetID;
      this.reference = reference;
      this.amount = new Balance(amount);
      this.amountString = null;
      this.signature = signature;
      this.address = null;
    }


    /**
     * DvpCommitEncumbrance constructor.
     *
     * @param publicKey    :
     * @param assetID      :
     * @param reference    :
     * @param amountString :
     * @param signature    :
     */
    public DvpCommitEncumbrance(String publicKey, String assetID, String reference, String amountString, String signature) {

      this.publicKey = publicKey;
      this.assetID = assetID;
      this.reference = reference;
      this.amount = null;
      this.amountString = amountString;
      this.signature = signature;
      this.address = null;
    }


    /**
     * DvpCommitEncumbrance constructor from MPWrappedArray array.
     *
     * @param partyData : [publicKey, assetID, reference, amount, signature, address]
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitEncumbrance(MPWrappedArray partyData) {
      this(partyData != null ? partyData.unwrap() : null);
    }


    /**
     * DvpCommitEncumbrance constructor from Object array.
     *
     * @param partyData : [publicKey, assetID, reference, amount, signature, address]
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitEncumbrance(Object[] partyData) {
      int dLen = (partyData != null ? partyData.length : 0);

      this.publicKey = (String) (dLen > 0 ? partyData[0] : "");
      this.assetID = (String) (dLen > 1 ? partyData[1] : "");
      this.reference = (String) (dLen > 2 ? partyData[2] : "");
      if (dLen > 3) {
        Object o = partyData[3];
        if (o instanceof String) {
          // could be an expression
          String s = (String) o;
          if (s.matches("-?[0-9]+")) {
            // it is an amount
            this.amount = new Balance(s);
            this.amountString = null;
          } else {
            // it is an expression
            this.amount = null;
            this.amountString = s;
          }
        } else {
          this.amount = new Balance(o);
          this.amountString = null;
        }
      } else {
        this.amount = BALANCE_ZERO;
        this.amountString = null;
      }
      this.signature = (String) (dLen > 4 ? partyData[4] : "");
      this.address = null;
    }


    /**
     * DvpCommitEncumbrance Copy constructor.
     *
     * @param toCopy :
     */
    public DvpCommitEncumbrance(DvpCommitEncumbrance toCopy) {

      this.publicKey = toCopy.publicKey;
      this.assetID = toCopy.assetID;
      this.reference = toCopy.reference;
      this.amount = toCopy.amount;
      this.amountString = toCopy.amountString;
      this.signature = toCopy.signature;
      this.address = null;
    }


    @Override
    public Object[] encode(long index) {
      if (amount != null) {
        return new Object[]{publicKey, assetID, reference, amount.getValue(), signature};
      }
      return new Object[]{publicKey, assetID, reference, amountString, signature};
    }


    /**
     * encodeJson.
     * <p>Return CommitEncumbrance data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(PUBLIC_KEY, publicKey);
      rVal.put(ASSET_ID, assetID);
      rVal.put(REFERENCE, reference);
      rVal.put(AMOUNT, amount.getValue());
      rVal.put(SIGNATURE, signature);

      return rVal;
    }


    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpCommitEncumbrance)) {
        return false;
      }

      DvpCommitEncumbrance theOther = (DvpCommitEncumbrance) toCompare;
      return
          Objects.equals(publicKey, theOther.publicKey)
              && Objects.equals(assetID, theOther.assetID)
              && Objects.equals(signature, theOther.signature)
              && Objects.equals(reference, theOther.reference)
              && Objects.equals(amount, theOther.amount)
              && Objects.equals(amountString, theOther.amountString);
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

      return ((publicKey == null ? 0 : publicKey.hashCode()) + (assetID == null ? 0 : assetID.hashCode())) % Integer.MAX_VALUE;

    }


    public Object[] objectToHashToSign(String contractAddress) {
      if (amount != null) {
        return new Object[]{contractAddress, assetID, reference, amount.getValue()};
      }
      return new Object[]{contractAddress, assetID, reference, amountString};

    }


    public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
      this.address = null;
    }

  }



  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpCommitParameter implements ToObjectArray {

    public final Integer contractSpecific;

    public final String parameterName;

    public String signature;

    private String address;

    private String publicKey;

    private Number valueNumber;

    private String valueString;


    /**
     * DvpCommitParameter constructor.
     *
     * @param parameterName    :
     * @param paramValue       :
     * @param contractSpecific :
     * @param publicKey        :
     * @param signature        :
     */
    public DvpCommitParameter(String parameterName, Object paramValue, Integer contractSpecific, String publicKey, String signature) {

      this.parameterName = parameterName;

      if (paramValue instanceof Number) {
        this.valueNumber = (new Balance(paramValue)).getValue();
        this.valueString = null;
      } else {
        this.setValue((String) paramValue);
      }

      this.contractSpecific = contractSpecific;
      this.publicKey = publicKey;
      this.signature = signature;
      this.address = null;
    }


    /**
     * DvpCommitParameter class de-serialisation constructor.
     *
     * @param paramData :
     */
    public DvpCommitParameter(MPWrappedArray paramData) {

      if (paramData != null) {

        int dLen = paramData.size();

        this.parameterName = (dLen > 0 ? paramData.asString(0) : "");

        if (dLen > 1) {
          if (paramData.get(1) instanceof Number) {
            this.valueNumber = (Number) paramData.get(1);
            this.valueString = null;
          } else {
            this.setValue(paramData.asString(1));
          }
        } else {
          this.valueNumber = I_ZERO;
          this.valueString = null;
        }

        this.contractSpecific = (dLen > 2 ? paramData.asInt(2) : 0);
        this.publicKey = (dLen > 3 ? paramData.asString(3) : "");
        this.signature = (dLen > 4 ? paramData.asString(4) : "");
        this.address = null;

      } else {

        this.parameterName = "";
        this.valueNumber = I_ZERO;
        this.valueString = null;
        this.contractSpecific = I_ZERO;
        this.publicKey = "";
        this.signature = "";
        this.address = null;
      }
    }


    /**
     * DvpCommitParameter class de-serialisation constructor.
     *
     * @param paramData :
     */
    public DvpCommitParameter(Object[] paramData) {

      if (paramData != null) {

        int dLen = paramData.length;

        this.parameterName = (String) (dLen > 0 ? paramData[0] : "");

        if (dLen > 1) {

          if (paramData[1] instanceof Number) {
            this.valueNumber = (Number) paramData[1];
            this.valueString = null;
          } else {
            this.setValue((String) paramData[1]);
          }
        } else {
          this.valueNumber = I_ZERO;
          this.valueString = null;
        }
        this.contractSpecific = (Integer) (dLen > 2 ? paramData[2] : 0);
        this.publicKey = (String) (dLen > 3 ? paramData[3] : "");
        this.signature = (String) (dLen > 4 ? paramData[4] : "");
        this.address = null;

      } else {

        this.parameterName = "";
        this.valueNumber = I_ZERO;
        this.valueString = null;
        this.contractSpecific = I_ZERO;
        this.publicKey = "";
        this.signature = "";
        this.address = null;
      }

    }


    /**
     * DvpCommitParameter class copy constructor.
     *
     * @param toCopy :
     */
    public DvpCommitParameter(DvpCommitParameter toCopy) {

      this.parameterName = toCopy.parameterName;
      this.valueNumber = toCopy.valueNumber;
      this.valueString = toCopy.valueString;
      this.contractSpecific = toCopy.contractSpecific;
      this.publicKey = toCopy.publicKey;
      this.signature = toCopy.signature;
      this.address = null;
    }


    @Override
    public Object[] encode(long index) {

      return new Object[]{parameterName, (valueNumber != null ? valueNumber : valueString), contractSpecific, publicKey, signature};
    }


    /**
     * encodeJson.
     * <p>Return CommitParameter data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(PARAMETER_NAME, parameterName);
      rVal.put(VALUE, (valueNumber != null ? valueNumber : valueString));
      rVal.put(CONTRACT_SPECIFIC, contractSpecific);
      rVal.put(PUBLIC_KEY, publicKey);
      rVal.put(SIGNATURE, signature);

      return rVal;

    }


    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpCommitParameter)) {
        return false;
      }

      DvpCommitParameter theOther = (DvpCommitParameter) toCompare;

      if (!Objects.equals(parameterName, theOther.parameterName)) {
        return false;
      }

      if ((!Objects.equals(valueNumber, theOther.valueNumber)) && ((valueNumber == null) || (!valueNumber.equals(theOther.valueNumber)))) {
        return false;
      }

      if (!Objects.equals(valueString, theOther.valueString)) {
        return false;
      }

      if (!Objects.equals(contractSpecific, theOther.contractSpecific)) {
        return false;
      }

      if (!Objects.equals(publicKey, theOther.publicKey)) {
        return false;
      }

      return Objects.equals(signature, theOther.signature);
    }


    /**
     * getAddress().
     *
     * @return :
     */
    public String getAddress() {

      if (address == null) {
        if (AddressUtil.verifyAddress(publicKey)) {
          return publicKey;
        }

        // If an EdDSA key, then the address could be Base-58 or Base-64. We have to trust the defaults.
        address = AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL);
      }
      return address;
    }


    public String getPublicKey() {
      return publicKey;
    }


    public Object getValue() {

      return (valueNumber == null ? valueString : valueNumber);
    }


    public Number getValueNumber() {
      return valueNumber;
    }


    public String getValueString() {
      return valueString;
    }


    @Override
    public int hashCode() {

      return (parameterName == null ? 0 : parameterName.hashCode());

    }


    public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
      this.address = null;
    }


    private void setValue(String value) {
      if (((value == null) || (value.isEmpty()))) {
        valueNumber = 0L;
        valueString = null;
        return;
      }

      for (int i = value.length() - 1; i >= 0; i--) {
        char ch = value.charAt(i);
        if ((ch < '0') || ('9' < ch)) {
          // definitely not a number
          valueNumber = null;
          valueString = value;
          return;
        }
      }

      try {
        // All numeric, so should be a number.
        valueNumber = (new Balance(value)).getValue();
        valueString = null;
      } catch (NumberFormatException e) {
        valueNumber = null;
        valueString = value;
      }
    }


    /**
     * DvpCommitParameter, return string message to sign for Create or Commit transactions.
     *
     * @param contractAddress :
     *
     * @return :
     */
    public String stringToHashToSign(String contractAddress) {

      if (contractSpecific == 0) {
        return String.format("%s%s", parameterName, getValue());
      } else {
        return String.format("%s%s%s", contractAddress, parameterName, getValue());
      }

    }

  }



  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpCommitParty implements ToObjectArray {

    public final String partyIdentifier;

    public String publicKey;

    public String signature;


    /**
     * DvpCommitParty class constructor.
     *
     * @param partyIdentifier :
     * @param publicKey       :
     * @param signature       :
     */
    public DvpCommitParty(String partyIdentifier, String publicKey, String signature) {

      this.partyIdentifier = partyIdentifier;
      this.publicKey = publicKey;
      this.signature = signature;
    }


    /**
     * DvpCommitParty class de-serialisation constructor.
     *
     * @param partyData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitParty(MPWrappedArray partyData) {

      int dLen = (partyData != null ? partyData.size() : 0);

      this.partyIdentifier = (dLen > 0 ? partyData.asString(0) : "");
      this.publicKey = (dLen > 1 ? partyData.asString(1) : "");
      this.signature = (dLen > 2 ? partyData.asString(2) : "");
    }


    /**
     * DvpCommitParty class de-serialisation constructor.
     *
     * @param partyData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitParty(Object[] partyData) {

      int dLen = (partyData != null ? partyData.length : 0);

      this.partyIdentifier = ((dLen > 0) && (partyData[0] != null) ? partyData[0].toString() : "");
      this.publicKey = ((dLen > 1) && (partyData[1] != null) ? partyData[1].toString() : "");
      this.signature = ((dLen > 2) && (partyData[2] != null) ? partyData[2].toString() : "");
    }


    /**
     * DvpCommitParty class copy constructor.
     *
     * @param toCopy :
     */
    public DvpCommitParty(DvpCommitParty toCopy) {

      this.partyIdentifier = toCopy.partyIdentifier;
      this.publicKey = toCopy.publicKey;
      this.signature = toCopy.signature;
    }


    @Override
    public Object[] encode(long index) {

      return new Object[]{partyIdentifier, publicKey, signature};
    }


    /**
     * encodeJson.
     * <p>Return CommitParty data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(PARTY_IDENTIFIER, partyIdentifier);
      rVal.put(PUBLIC_KEY, publicKey);
      rVal.put(SIGNATURE, signature);

      return rVal;

    }


    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpCommitParty)) {
        return false;
      }

      DvpCommitParty theOther = (DvpCommitParty) toCompare;

      if (!Objects.equals(partyIdentifier, theOther.partyIdentifier)) {
        return false;
      }

      if (!Objects.equals(publicKey, theOther.publicKey)) {
        return false;
      }

      return Objects.equals(signature, theOther.signature);
    }


    @Override
    public int hashCode() {

      return ((partyIdentifier == null ? 0 : partyIdentifier.hashCode()) + (publicKey == null ? 0 : publicKey.hashCode()) + (signature == null ? 0
          : signature.hashCode())) % Integer.MAX_VALUE;

    }

  }



  public static class DvpCommitReceive implements ToObjectArray {

    public final String address;

    public final Long index;


    /**
     * DvpCommitReceive class constructor.
     *
     * @param index   :
     * @param address :
     */
    public DvpCommitReceive(Long index, String address) {

      this.index = index;
      this.address = address;
    }


    /**
     * DvpCommitReceive de-serialisation constructor.
     *
     * @param paramData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitReceive(MPWrappedArray paramData) {

      int dLen = (paramData != null ? paramData.size() : 0);

      this.index = (dLen > 0 ? paramData.asLong(0) : -1);
      this.address = (dLen > 1 ? paramData.asString(1) : "");
    }


    /**
     * DvpCommitReceive de-serialisation constructor.
     *
     * @param paramData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpCommitReceive(Object[] paramData) {

      int dLen = (paramData != null ? paramData.length : 0);

      this.index = dLen > 0 ? ((Number) paramData[0]).longValue() : 0L;
      this.address = (String) (dLen > 1 ? paramData[1] : "");
    }


    /**
     * DvpCommitReceive class copy constructor.
     *
     * @param toCopy :
     */
    public DvpCommitReceive(DvpCommitReceive toCopy) {

      this.index = toCopy.index;
      this.address = toCopy.address;
    }


    @Override
    public Object[] encode(long unused) {

      return new Object[]{this.index, this.address};
    }


    /**
     * encodeJson.
     * <p>Return CommitReceive data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(INDEX, index);
      rVal.put(ADDRESS, address);

      return rVal;

    }


    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpCommitReceive)) {
        return false;
      }

      DvpCommitReceive theOther = (DvpCommitReceive) toCompare;

      if (!Objects.equals(index, theOther.index)) {
        return false;
      }

      return Objects.equals(address, theOther.address);
    }


    @Override
    public int hashCode() {

      return ((index == null ? 0 : index.hashCode()) + (address == null ? 0 : address.hashCode())) % Integer.MAX_VALUE;

    }

  }



  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpCommitment implements ToObjectArray {

    // [Index, Namespace, ClassID, Amount, PublicKey, Signature] When decoding API Data set
    // [Index, PublicKey, Signature] At all other times.

    public final Balance amount;

    public final String amountString;

    public final String classID;

    public final Long index;

    public final String namespace;

    public String signature;

    private String payAddress; // temporary working variable.

    private String publicKey;


    /**
     * DvpCommitment class constructor.
     *
     * @param index     :
     * @param publicKey :
     * @param signature :
     */
    public DvpCommitment(Long index, String publicKey, String signature) {

      this.index = (index == null ? Long.valueOf(0) : index);
      this.publicKey = publicKey;
      this.signature = signature;
      this.payAddress = null;
      this.namespace = "";
      this.classID = "";
      this.amount = BALANCE_ZERO;
      this.amountString = null;
    }


    /**
     * DvpCommitment class de-serialisation constructor.
     *
     * @param data :
     */
    public DvpCommitment(MPWrappedArray data) {

      if (data != null) {

        int size = data.size();

        if (size == 3) {
          this.index = (data.get(0) == null ? 0L : data.asLong(0));
          this.publicKey = data.asString(1);
          this.signature = data.asString(2);
          this.payAddress = null;
          this.namespace = "";
          this.classID = "";
          this.amount = BALANCE_ZERO;
          this.amountString = null;
          return;

        } else if (size >= 6) {
          this.index = (data.get(0) == null ? 0L : data.asLong(0));
          this.namespace = data.asString(1);
          this.classID = data.asString(2);

          if (data.get(3) instanceof Number) {
            this.amount = new Balance(data.get(3));
            this.amountString = null;
          } else {
            this.amountString = data.asString(3);
            this.amount = null;
          }

          this.publicKey = data.asString(4);
          this.signature = data.asString(5);
          this.payAddress = null;
          return;

        }

      }

      this.index = 0L;
      this.publicKey = "";
      this.signature = "";
      this.payAddress = null;
      this.namespace = "";
      this.classID = "";
      this.amount = BALANCE_ZERO;
      this.amountString = null;

    }


    /**
     * DvpCommitment class de-serialisation constructor.
     *
     * @param data :
     */
    public DvpCommitment(Object[] data) {

      if (data != null) {

        if (data.length == 3) {
          this.index = data[0] == null ? 0L : ((Number) data[0]).longValue();
          this.publicKey = (String) data[1];
          this.signature = (String) data[2];
          this.namespace = "";
          this.classID = "";
          this.amount = BALANCE_ZERO;
          this.amountString = null;
          this.payAddress = null;
          return;

        } else if (data.length == 6) {
          this.index = data[0] == null ? 0L : ((Number) data[0]).longValue();
          this.namespace = (String) data[1];
          this.classID = (String) data[2];

          if (data[3] instanceof Number) {
            this.amount = new Balance(data[3]);
            this.amountString = null;
          } else {
            this.amountString = (String) data[3];
            this.amount = null;
          }

          this.publicKey = (String) data[4];
          this.signature = (String) data[5];
          this.payAddress = null;
          return;
        }

      }

      this.index = 0L;
      this.publicKey = "";
      this.signature = "";
      this.payAddress = null;
      this.namespace = "";
      this.classID = "";
      this.amount = BALANCE_ZERO;
      this.amountString = null;

    }


    /**
     * DvpCommitment class copy constructor.
     *
     * @param toCopy :
     */
    public DvpCommitment(DvpCommitment toCopy) {

      this.index = toCopy.index;
      this.publicKey = toCopy.publicKey;
      this.signature = toCopy.signature;
      this.namespace = toCopy.namespace;
      this.classID = toCopy.classID;
      this.amount = toCopy.amount;
      this.amountString = toCopy.amountString;
      this.payAddress = null;
    }


    @Override
    public Object[] encode(long unused) {

      // The Namespace, class, amount are not encoded as they are only used in the API code
      // so that the API code may have all of the data required to generate the signature.
      // The [Namespace, class, amount] are not intrinsic to the Commitment.

      return new Object[]{index, publicKey, signature};
    }


    /**
     * encodeJson.
     * <p>Return Party Commitment data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(INDEX, index);
      rVal.put(PUBLIC_KEY, publicKey);
      rVal.put(SIGNATURE, signature);

      return rVal;
    }


    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpCommitment)) {
        return false;
      }

      DvpCommitment theOther = (DvpCommitment) toCompare;

      if (!Objects.equals(index, theOther.index)) {
        return false;
      }

      if (!Objects.equals(publicKey, theOther.publicKey)) {
        return false;
      }

      return Objects.equals(signature, theOther.signature);
    }


    /**
     * getPayAddress().
     *
     * @return :
     */
    public String getPayAddress() {
      // TODO : To recover the address we have to guess the address type and encoding. It would be safer if we stored the address that corresponds to the
      // public key.

      if (payAddress == null) {
        // If an EdDSA key, then the address could be Base-58 or Base-64. We have to trust the defaults.
        payAddress = AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL);
      }
      return payAddress;
    }


    public String getPublicKey() {
      return publicKey;
    }


    @Override
    public int hashCode() {

      return ((index == null ? 0 : index.hashCode()) + (publicKey == null ? 0 : publicKey.hashCode()) + (signature == null ? 0
          : signature.hashCode())) % Integer.MAX_VALUE;

    }


    /**
     * Get the objects that will be combined to create the signature.
     *
     * @param contractAddress the contract's address
     *
     * @return the collected objects
     */
    public Object[] objectToHashToSign(String contractAddress) {
      return new Object[]{contractAddress, namespace, classID, (amountString != null ? amountString : amount.getValue())};
    }


    public void setPublicKey(String publicKey) {
      this.publicKey = publicKey;
      payAddress = null;
    }

  }



  private List<DvpCommitAuthorise> authorise;

  private DvpCommitCancel cancel;

  private List<DvpCommitment> commitment;

  private List<DvpCommitEncumbrance> encumbrances;

  private List<DvpCommitParameter> parameters;

  private DvpCommitParty party;

  private List<DvpCommitReceive> receive;


  public DvpUKCommitData(Object[] commitData) {

    this(new MPWrappedMap<>(commitData));
  }


  public DvpUKCommitData(Map<String, Object> sourceMap) {

    this(new MPWrappedMap<>(sourceMap));
  }


  /**
   * DvpUKCommitData Constructor from MPWrappedMap.
   * Map may contain `party`, `commitment`, `receive`, `authorise`, `parameters`, `encumbrances` or `cancel` sections.
   *
   * @param asMapValue :
   */
  public DvpUKCommitData(MPWrappedMap<String, Object> asMapValue) {

    asMapValue.iterate((k, v) -> {

      switch (k.toLowerCase()) {

        case PARTY:
          setParty(v);
          break;

        case COMMITMENT:
          setCommitments(v);
          break;

        case RECEIVE:
          setReceive(v);
          break;

        case AUTHORISE:
          setAuthorise(v);
          break;

        case PARAMETERS:
          setParameters(v);
          break;

        case ENCUMBRANCES:
          setEncumbrance(v);
          break;

        case CANCEL:
          if (v instanceof Object[]) {
            this.cancel = new DvpCommitCancel((Object[]) v);
          } else if (v instanceof List) {
            this.cancel = new DvpCommitCancel(((List) v).toArray());
          } else if (v instanceof DvpCommitCancel) {
            this.cancel = new DvpCommitCancel((DvpCommitCancel) v);
          }

          break;

        default:
          break;

      }

    });

  }


  /**
   * DvpUKCommitData Copy Constructor.
   *
   * @param toCopy :
   */
  public DvpUKCommitData(DvpUKCommitData toCopy) {

    if (toCopy == null) {
      return;
    }

    if (toCopy.party != null) {
      this.party = new DvpCommitParty(toCopy.party);
    }

    if (toCopy.commitment != null) {
      this.commitment = new ArrayList<>();
      toCopy.commitment.forEach(thisItem -> this.commitment.add(new DvpCommitment(thisItem)));
    }

    if (toCopy.receive != null) {
      this.receive = new ArrayList<>();
      toCopy.receive.forEach(thisItem -> this.receive.add(new DvpCommitReceive(thisItem)));
    }

    if (toCopy.authorise != null) {
      this.authorise = new ArrayList<>();
      toCopy.authorise.forEach(thisItem -> this.authorise.add(new DvpCommitAuthorise(thisItem)));
    }

    if (toCopy.parameters != null) {
      this.parameters = new ArrayList<>();
      toCopy.parameters.forEach(thisItem -> this.parameters.add(new DvpCommitParameter(thisItem)));
    }

    if (toCopy.encumbrances != null) {
      this.encumbrances = new ArrayList<>();
      toCopy.encumbrances.forEach(thisItem -> this.encumbrances.add(new DvpCommitEncumbrance(thisItem)));
    }

    if (toCopy.cancel != null) {
      this.cancel = new DvpCommitCancel(toCopy.cancel);
    }

  }


  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    if ((party != null) && ((party.publicKey != null) && (party.publicKey.length() > 0))) {
      if (AddressUtil.verifyAddress(party.publicKey)) {
        rVal.add(party.publicKey);
      } else if (AddressUtil.verifyPublicKey(party.publicKey)) {
        rVal.add(AddressUtil.publicKeyToAddress(party.publicKey, AddressType.NORMAL));
      }
    }

    if (commitment != null) {
      for (DvpCommitment thisitem : commitment) {
        if ((thisitem.payAddress != null) && (thisitem.payAddress.length() > 0)) {
          if (AddressUtil.verifyAddress(thisitem.payAddress)) {
            rVal.add(thisitem.payAddress);
          } else if (AddressUtil.verifyPublicKey(thisitem.payAddress)) {
            rVal.add(AddressUtil.publicKeyToAddress(thisitem.payAddress, AddressType.NORMAL));
          }
        }
      }
    }

    if (receive != null) {
      for (DvpCommitReceive thisitem : receive) {
        if ((thisitem.address != null) && (thisitem.address.length() > 0)) {
          if (AddressUtil.verifyAddress(thisitem.address)) {
            rVal.add(thisitem.address);
          } else if (AddressUtil.verifyPublicKey(thisitem.address)) {
            rVal.add(AddressUtil.publicKeyToAddress(thisitem.address, AddressType.NORMAL));
          }
        }
      }
    }

    if (authorise != null) {
      for (DvpCommitAuthorise thisitem : authorise) {
        if ((thisitem.getAddress() != null) && (thisitem.getAddress().length() > 0)) {
          if (AddressUtil.verifyAddress(thisitem.getAddress())) {
            rVal.add(thisitem.getAddress());
          } else if (AddressUtil.verifyPublicKey(thisitem.getAddress())) {
            rVal.add(AddressUtil.publicKeyToAddress(thisitem.getAddress(), AddressType.NORMAL));
          }
        }
      }
    }

    if (parameters != null) {
      for (DvpCommitParameter thisitem : parameters) {
        if ((thisitem.getAddress() != null) && (thisitem.getAddress().length() > 0)) {
          if (AddressUtil.verifyAddress(thisitem.getAddress())) {
            rVal.add(thisitem.getAddress());
          } else if (AddressUtil.verifyPublicKey(thisitem.getAddress())) {
            rVal.add(AddressUtil.publicKeyToAddress(thisitem.getAddress(), AddressType.NORMAL));
          }
        }
      }
    }

    return rVal;
  }


  @Override
  public IContractData copy() {

    return new DvpUKCommitData(this);
  }


  private List<DvpCommitAuthorise> decodeAuthorise(Object[] paramData) {

    return decodeAuthorise(new MPWrappedArrayImpl(paramData));
  }


  @SuppressWarnings("squid:S1168")
  private List<DvpCommitAuthorise> decodeAuthorise(MPWrappedArray paramData) {

    if (paramData == null) {
      return null;
    }

    ArrayList<DvpCommitAuthorise> newList = new ArrayList<>();

    for (int index = 0, l = paramData.size(); index < l; index++) {
      newList.add(new DvpCommitAuthorise(paramData.asWrapped(index)));
    }

    return newList;
  }


  private List<DvpCommitment> decodeCommitments(Object[] commitment) {

    return decodeCommitments(new MPWrappedArrayImpl(commitment));
  }


  @SuppressWarnings("squid:S1168")
  private List<DvpCommitment> decodeCommitments(MPWrappedArray commitments) {

    if (commitments == null) {
      return null;
    }

    ArrayList<DvpCommitment> theseParties = new ArrayList<>();

    for (int index = 0, l = commitments.size(); index < l; index++) {
      theseParties.add(new DvpCommitment(commitments.asWrapped(index)));
    }

    return theseParties;

  }


  private List<DvpCommitEncumbrance> decodeEncumbrance(Object[] paramData) {

    return decodeEncumbrance(new MPWrappedArrayImpl(paramData));
  }


  @SuppressWarnings("squid:S1168")
  private List<DvpCommitEncumbrance> decodeEncumbrance(MPWrappedArray paramData) {

    if (paramData == null) {
      return null;
    }

    ArrayList<DvpCommitEncumbrance> newList = new ArrayList<>();

    for (int index = 0, l = paramData.size(); index < l; index++) {
      newList.add(new DvpCommitEncumbrance(paramData.asWrapped(index)));
    }

    return newList;
  }


  private List<DvpCommitParameter> decodeParameter(Object[] paramData) {

    return decodeParameter(new MPWrappedArrayImpl(paramData));
  }


  @SuppressWarnings("squid:S1168") // Allow  null return.
  private List<DvpCommitParameter> decodeParameter(MPWrappedArray paramData) {

    if (paramData == null) {
      return null;
    }

    ArrayList<DvpCommitParameter> newList = new ArrayList<>();

    for (int index = 0, l = paramData.size(); index < l; index++) {
      newList.add(new DvpCommitParameter(paramData.asWrapped(index)));
    }

    return newList;
  }


  private List<DvpCommitReceive> decodeReceive(Object[] paramData) {

    return decodeReceive(new MPWrappedArrayImpl(paramData));
  }


  @SuppressWarnings("squid:S1168")
  private List<DvpCommitReceive> decodeReceive(MPWrappedArray paramData) {

    if (paramData == null) {
      return null;
    }

    ArrayList<DvpCommitReceive> newList = new ArrayList<>();

    for (int index = 0, l = paramData.size(); index < l; index++) {
      newList.add(new DvpCommitReceive(paramData.asWrapped(index)));
    }

    return newList;
  }


  @Override
  public MPWrappedMap<String, Object> encode() {

    Map<String, Object> rVal = encodeToMap();

    return new MPWrappedMap<>(rVal);

  }


  @SuppressWarnings("squid:S1168")
  private Object[] encodeAuthorise() {

    if (this.authorise == null) {
      return null;
    }

    Object[] rVal = new Object[this.authorise.size()];

    final int[] index = {0};

    this.authorise.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;

  }


  /**
   * encodeAuthoriseJson.
   * <p>Return authorise data as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeAuthoriseJson() {

    JSONArray rVal = new JSONArray();

    this.authorise.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;

  }


  @SuppressWarnings("squid:S1168")
  private Object[] encodeCommitments() {

    if (this.commitment == null) {
      return null;
    }

    Object[] rVal = new Object[this.commitment.size()];

    final int[] index = {0};

    this.commitment.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;

  }


  /**
   * encodeCommitmentsJson.
   * <p>Return Commitments data as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeCommitmentsJson() {

    JSONArray rVal = new JSONArray();

    this.commitment.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;

  }


  /**
   * encodeEncumbrances.
   * Serialise Encumbrances.
   *
   * @return :
   */
  @SuppressWarnings("squid:S1168")
  private Object[] encodeEncumbrances() {

    if (this.encumbrances == null) {
      return null;
    }

    Object[] rVal = new Object[this.encumbrances.size()];

    final int[] index = {0};

    this.encumbrances.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;

  }


  /**
   * encodeEncumbrancesJson.
   * <p>Return Encumbrances Commitment data as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeEncumbrancesJson() {

    JSONArray rVal = new JSONArray();

    this.encumbrances.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;

  }


  @SuppressWarnings("squid:S1168")
  private Object[] encodeParameters() {

    if (this.parameters == null) {
      return null;
    }

    Object[] rVal = new Object[this.parameters.size()];

    final int[] index = {0};

    this.parameters.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;

  }


  /**
   * encodeParametersJson.
   * <p>Return Parameters Commitment data as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeParametersJson() {

    JSONArray rVal = new JSONArray();

    this.parameters.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;

  }


  @SuppressWarnings("squid:S1168")
  private Object[] encodeReceive() {

    if (this.receive == null) {
      return null;
    }

    Object[] rVal = new Object[this.receive.size()];

    final int[] index = {0};

    this.receive.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;

  }


  /**
   * encodeReceiveJson.
   * <p>Return receive data as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeReceiveJson() {

    JSONArray rVal = new JSONArray();

    this.receive.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;

  }


  @Override
  public Map<String, Object> encodeToMap() {

    TreeMap<String, Object> rVal = new TreeMap<>();

    if ((this.cancel != null) && (this.cancel.publicKey.length() > 0) && (this.cancel.signature.length() > 0)) {
      rVal.put(CANCEL, cancel.encode(0));
    } else {

      if (this.party != null) {
        rVal.put(PARTY, this.party.encode(0));
      }

      if (this.commitment != null) {
        rVal.put(COMMITMENT, encodeCommitments());
      }

      if (this.receive != null) {
        rVal.put(RECEIVE, encodeReceive());
      }

      if (this.authorise != null) {
        rVal.put(AUTHORISE, encodeAuthorise());
      }

      if (this.parameters != null) {
        rVal.put(PARAMETERS, encodeParameters());
      }

      if (this.encumbrances != null) {
        rVal.put(ENCUMBRANCES, encodeEncumbrances());
      }
    }

    rVal.put(__FUNCTION, CONTRACT_NAME_DVP_UK_COMMIT);

    return rVal;
  }


  @Override
  public Map<String, Object> encodeToMapForTxParameter() {

    return encodeToMap();
  }


  @Override
  public boolean equals(Object toCompare) {

    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof DvpUKCommitData)) {
      return false;
    }

    DvpUKCommitData theOther = (DvpUKCommitData) toCompare;

    // Party
    if ((party != theOther.party) && ((party == null) || (!party.equals(theOther.party)))) {
      return false;
    }

    if ((commitment != theOther.commitment) && ((commitment == null) || (!commitment.equals(theOther.commitment)))) {
      return false;
    }

    if ((receive != theOther.receive) && ((receive == null) || (!receive.equals(theOther.receive)))) {
      return false;
    }

    if ((authorise != theOther.authorise) && ((authorise == null) || (!authorise.equals(theOther.authorise)))) {
      return false;
    }

    if ((parameters != theOther.parameters) && ((parameters == null) || (!parameters.equals(theOther.parameters)))) {
      return false;
    }

    if ((encumbrances != theOther.encumbrances) && ((encumbrances == null) || (!encumbrances.equals(theOther.encumbrances)))) {
      return false;
    }

    return (cancel == theOther.cancel) || ((cancel != null) && (cancel.equals(theOther.cancel)));
  }


  public List<DvpCommitAuthorise> getAuthorise() {

    return authorise;
  }


  public DvpCommitCancel getCancel() {

    return cancel;
  }


  public List<DvpCommitment> getCommitment() {

    return commitment;
  }


  @Override
  public String getContractType() {

    return CONTRACT_NAME_DVP_UK_COMMIT;
  }


  public List<DvpCommitEncumbrance> getEncumbrances() {

    return encumbrances;
  }


  public List<DvpCommitParameter> getParameters() {

    return parameters;
  }


  public DvpCommitParty getParty() {

    return party;
  }


  public List<DvpCommitReceive> getReceive() {

    return receive;
  }


  @Override
  public String get__function() {

    return getContractType();
  }


  @Override
  public int hashCode() {

    if (party != null) {
      if (party.partyIdentifier != null) {
        return party.partyIdentifier.hashCode();
      }
      if (party.publicKey != null) {
        return party.publicKey.hashCode();
      }

      return party.hashCode();
    }

    if (cancel != null) {
      return cancel.hashCode();
    }

    return 0;
  }


  /**
   * setAuthorise.
   * Sign or Refute Contract Authorisation.
   *
   * @param paramData :
   */
  public void setAuthorise(Object paramData) {

    if (paramData != null) {
      if (paramData instanceof MPWrappedArray) {
        this.authorise = decodeAuthorise((MPWrappedArray) paramData);
      } else if (paramData instanceof Object[]) {
        this.authorise = decodeAuthorise((Object[]) paramData);
      } else if (paramData instanceof List) {
        this.authorise = decodeAuthorise(((List) paramData).toArray());
      } else if (paramData instanceof DvpCommitAuthorise) {
        if (this.authorise == null) {
          this.authorise = new ArrayList<>();
        }
        this.authorise.add((DvpCommitAuthorise) paramData);
      }
    } else {
      this.authorise = null;
    }
  }


  public void setCancel(DvpCommitCancel cancel) {

    this.cancel = cancel;
  }


  /**
   * setCommitment.
   * set signature for a Party payment.
   *
   * @param commitment :
   */
  public void setCommitment(DvpCommitment commitment) {

    if (commitment == null) {
      return;
    }

    if (this.commitment == null) {
      this.commitment = new ArrayList<>();
    }

    this.commitment.add(commitment);
  }


  /**
   * setCommitments.
   * Supply party signature for this DVP Contract.
   *
   * @param commitment :
   */
  public void setCommitments(Object commitment) {

    if (commitment != null) {
      if (commitment instanceof MPWrappedArray) {
        this.commitment = decodeCommitments((MPWrappedArray) commitment);
      } else if (commitment instanceof Object[]) {
        this.commitment = decodeCommitments((Object[]) commitment);
      } else if (commitment instanceof List) {
        this.commitment = decodeCommitments(((List) commitment).toArray());
      }
    } else {
      this.commitment = null;
    }
  }


  /**
   * setEncumbrance.
   * Supply signatures for Encumbrances to be added on execution of the DVP Contract.
   *
   * @param encumbrance :
   */
  public void setEncumbrance(DvpCommitEncumbrance encumbrance) {

    if (encumbrance == null) {
      return;
    }

    if (this.encumbrances == null) {
      this.encumbrances = new ArrayList<>();
    }

    this.encumbrances.add(encumbrance);
  }


  /**
   * setEncumbrance().
   *
   * @param paramData :
   */
  public void setEncumbrance(Object paramData) {

    if (paramData != null) {
      if (paramData instanceof MPWrappedArray) {
        this.encumbrances = decodeEncumbrance((MPWrappedArray) paramData);
      } else if (paramData instanceof Object[]) {
        this.encumbrances = decodeEncumbrance((Object[]) paramData);
      } else if (paramData instanceof List) {
        this.encumbrances = decodeEncumbrance(((List) paramData).toArray());
      }
    } else {
      this.encumbrances = null;
    }
  }


  @Override
  public long setNextTimeEvent(long updateTime, boolean forceToUpdateTime) {
    // No Time Events on Commit data

    return 0L;
  }


  /**
   * setParameter.
   * Supply signature and data for Contract parameter.
   *
   * @param parameter :
   */
  public void setParameter(DvpCommitParameter parameter) {

    if (parameter == null) {
      return;
    }

    if (this.parameters == null) {
      this.parameters = new ArrayList<>();
    }

    this.parameters.add(parameter);
  }


  /**
   * setParameter.
   * Supply signature and data for Contract parameter(s).
   *
   * @param paramData :
   */
  public void setParameters(Object paramData) {

    if (paramData != null) {
      if (paramData instanceof MPWrappedArray) {
        this.parameters = decodeParameter((MPWrappedArray) paramData);
      } else if (paramData instanceof Object[]) {
        this.parameters = decodeParameter((Object[]) paramData);
      } else if (paramData instanceof List) {
        this.parameters = decodeParameter(((List) paramData).toArray());
      }
    } else {
      this.parameters = null;
    }
  }


  /**
   * setParty.
   * Set Party Signature.
   *
   * @param thisParty :
   */
  public void setParty(DvpCommitParty thisParty) {

    if (thisParty == null) {
      this.party = null;
      return;
    }

    this.party = new DvpCommitParty(thisParty);
  }


  /**
   * setParty.
   * Set Party Signature.
   *
   * @param thisParty :
   */
  public void setParty(Object thisParty) {

    if (thisParty != null) {

      if (thisParty instanceof DvpCommitParty) {
        this.party = new DvpCommitParty((DvpCommitParty) thisParty);
      } else if (thisParty instanceof MPWrappedArray) {
        this.party = new DvpCommitParty((MPWrappedArray) thisParty);
      } else if (thisParty instanceof Object[]) {
        this.party = new DvpCommitParty((Object[]) thisParty);
      } else if (thisParty instanceof List) {
        this.party = new DvpCommitParty(((List) thisParty).toArray());
      } else {
        this.party = null;
      }

    } else {
      this.party = null;
    }

  }


  /**
   * setReceive.
   * Sign and/or set Receipt details for a Party Receipt.
   *
   * @param paramData :
   */
  public void setReceive(Object paramData) {

    if (paramData != null) {
      if (paramData instanceof MPWrappedArray) {
        this.receive = decodeReceive((MPWrappedArray) paramData);
      } else if (paramData instanceof Object[]) {
        this.receive = decodeReceive((Object[]) paramData);
      } else if (paramData instanceof List) {
        this.receive = decodeReceive(((List) paramData).toArray());
      } else if (paramData instanceof DvpCommitReceive) {
        if (this.receive == null) {
          this.receive = new ArrayList<>();
        }
        this.receive.add((DvpCommitReceive) paramData);
      }
    } else {
      this.receive = null;
    }
  }


  @Override
  public JSONObject toJSON() {

    JSONObject rVal = new JSONObject(true);

    if (this.party != null) {
      rVal.put(PARTY, this.party.encodeJson());
    }

    if (this.commitment != null) {
      rVal.put(COMMITMENT, encodeCommitmentsJson());
    }

    if (this.receive != null) {
      rVal.put(RECEIVE, encodeReceiveJson());
    }

    if (this.authorise != null) {
      rVal.put(AUTHORISE, encodeAuthoriseJson());
    }

    if (this.parameters != null) {
      rVal.put(PARAMETERS, encodeParametersJson());
    }

    if (this.encumbrances != null) {
      rVal.put(ENCUMBRANCES, encodeEncumbrancesJson());
    }

    if (this.cancel != null) {
      rVal.put(CANCEL, cancel.encodeJson());
    }

    rVal.put(__FUNCTION, CONTRACT_NAME_DVP_UK_COMMIT);

    return rVal;
  }

}
