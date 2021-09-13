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


import static java.lang.System.arraycopy;

import static io.setl.bc.pychain.state.tx.helper.TxParameters.ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ADD_ENCUMBRANCES;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ADMINS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AMOUNT;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSET_ID;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AUTHORISATIONS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AUTHORISATION_ID;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AUTO_SIGN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.BENEFICIARIES;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CALCULATED_INDEX;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CALCULATION_ONLY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CLASSID;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_SPECIFIC;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.DELAY_ON_COMPLETE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ENCUMBRANCE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ENCUMBRANCE_NAME;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.EVENTS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.EXPIRY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ISSUANCE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ISSUING_ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.METADATA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.MUST_SIGN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.NAMESPACE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PARAMETERS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PARTIES;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PARTY_IDENTIFIER;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PAY_LIST;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.POA_PUBLIC_KEY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PROTOCOL;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PUBLIC_KEY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.RECEIVE_LIST;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFERENCE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.REFUSED;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.SIGNATURE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.SIG_ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.START_DATE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.USE_CREATOR_ENCUMBRANCE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.VALUE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__CANCELTIME;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__COMPLETED;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__STATUS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__TIMEEVENT;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.DVP_DELETE_DELAY_ON_COMPLETE;
import static io.setl.util.Convert.objectToLong;
import static io.setl.util.Convert.objectToString;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.common.ToObjectArray;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.helper.TxParameters;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Balance;
import io.setl.common.TypeSafeMap;

// The cyclomatic complexity of methods should not exceed a defined threshold.
// 'Empty arrays and collections should be returned instead of null' : Needs to be this way.
// naming convention : Allow the 'legacy' get__XXX names.
@SuppressWarnings({"squid:S00100", "squid:S1168", "squid:MethodCyclomaticComplexity"})
@SuppressFBWarnings("RC_REF_COMPARISON")
public class DvpUkContractData implements IContractData {


  private static final long CLASS_DELETE_DELAY_ON_COMPLETE = DVP_DELETE_DELAY_ON_COMPLETE;



  @SuppressWarnings("squid:CommentedOutCodeLine")
  /*

  Contract Data :

    'function' = dvp_uk :

         {
         'contractfunction' : 'dvp_uk',
         'parties'       : [
              partyCount,
              [                     # party details :
                 PartyIdentifier,
                 SigAddress,        # Party Address.
                 [                  # pay list, may be an empty list.
                    [
                    Address,        # Address from which payment will be taken.
                    NameSpace,      # Asset Issuer
                    AssetID,        # Asset Class
                    Qty1,           # Amount
                    Public Key,     # (API Optional) Public Key for this Address. HEX.
                    Signature,      # (API Optional) Base64. Signature of [NameSpace, AssetID, Qty] by Address1
                                    # For PoA, the 'Public Key' and 'Signature' will relate to the Attorney, the Address to the PoA Grantor.
                    Issuance,       # (API Optional) (Bool) Issue new asset to fulfil this payment. Only works if this Payment Address (which MUST be specified
                                    #        at contract creation) is the Asset Issuance Address, otherwise causes a validation error.
                                    #        Will use an encumbrance, like other payments, when this payment / party is not signed.
                    MetaData        # (API Optional) (String) Metadata related to this payment. Will be copied through to 'Effective' Transactions.
                    Encumbrance     # (API Optional) (String) A specific encumbrance to be used for this payment. This will override the general Contract
                                      value, though encumbrances must still be enabled in the 'encumbrance' section.
                    ],
                    ...
                 ],
                 [                  # receive list, may be an empty list.
                    [
                    Address2,       # Address to which payment will be made
                    NameSpace1,     # Asset Issuer
                    AssetID1,       # Asset Class
                    Qty1],          # Amount
                 ...
                 ],
                 Public Key,        # (API Optional) Public key of this party. If not specified, may be provided by a DVP_Commit transaction.
                 Signature,         # (API Optional) Signature of contractAddress by SigAddress. Usually empty string, provided by a DVP_Commit transaction.
                                    # For PoA, the 'Public Key' and 'Signature' will relate to the Attorney, the SigAddress to the PoA Grantor.
                 MustSign           # (API Optional) Boolean. (default = false) If True, then party must sign, otherwise 'receipt-only' parties are not
                                      required to sign and Encumbrances will be used without signature.
              ],
              ...
           ],
         'authorisations': [                        # A list (may be empty) of additional commitments required to validate the contract
                              [
                              publickey,            # HEX. public key, 64 chars. Alternatively you may specify an Address, in which case 'Signature' may not
                                                      be supplied, but must be provided by a Commit transaction. This item always relates to the Authorising
                                                      party, i.e. never the PoA Attorney.
                              AuthorisationID       # (May be empty string),
                              Signature,            # (API Optional) Base64. Signature of the conatenation of the ContractAddress and the AuthorisationID.
                                                      Usually empty string, provided by subsequent commitment.
                                                      For PoA, this signature relates to the Attorney public key which will be taken from the Transaction
                                                      and saved in PoaPublicKey.
                              MetaData,             # (API Optional) User data. May be specified. Will be overwritten by MetaData from Commit if provided.
                              Refused               # (API Optional) If true, then this Authorisation is 'Refused'. Refused Authorisations will not count as
                                                      signed. This value may be set during Commit TXs.
                              ContractSpecific,     # (API Optional) (Int) 0 : Signature message does not include ContractAddress, 1 : (default) It does.
                                                      This allows an authorisation to, optionally, be sent to multiple contracts if required.
                              PoaPublicKey          # Internal Use. do not specify. Used to record PoaPublic key when PoAs have been used to sign.
                              ],
                              ...
                           ],
         'parameters'    : {                        # (Optional). 'parameters' are values that may be substituted into payment / receipt lines
                           'key' : [                # 'key' is the variable name used in payment / receipt lines, we recommend that all keys are specified
                                                      in lower case. Keys are not case sensitive when value substitution takes place, but are case sensitive
                                                      for signing or commitment purposes. If two keys are given which resolve to the same lower-case value,
                                                      then one will arbitrarily get precedence.
                                   Address,         # Address or Public Key that may update this variable via a 'Commit' transaction.
                                                      This item always relates to the Authorising party, i.e. never the Attorney.
                                   Value,           # value to insert, empty string ('') indicates a not-yet-set value.
                                   CalculatedIndex, # 'Values' will be evaluated before use in 'CalculatedIndex' order. This allows a calculation sequence
                                                      to be defined.
                                   ContractSpecific,# (API Optional) (Int) 0 : (default) Signature message does not include ContractAddress, 1 : It does. This
                                                      allows a parameter value to, optionally, be sent to multiple contracts is required.
                                   CalculationOnly, # (API Optional) (Int) 0 : (default) Normal - Requires signature etc. 1 : Is only present as an
                                                      intermediate value to be evaluated in consensus thus can not be changed and does not require signature.
                                   Signature        # (API Optional) Signature of '[ContractAddress|]key|Value' by 'Public Key'. 'ContractAddress|' is
                                                      included in the message if 'ContractSpecific' != 0
                                                      For PoA, this signature relates to the Attorney public key which will be taken from the Transaction
                                                      and saved in PoaPublicKey.
                                   PoaPublicKey     # (API Optional) Internal Use. do not specify. Used to record PoaPublic key when PoAs have been used to sign
                                   ],
                           ...
                           },
         'addencumbrances' :[                       # (Optional). Encumbrances to be put in place at the time of contract execution.
                               [
                                  publickey,        # Address or Public Key that may update this variable via a 'Commit' transaction. If a signature is
                                                      provided, then this needs to be a public key, unless the signature was provided by POA.
                                                      This item always relates to the Authorising party, i.e. never the Attorney.
                                  fullassetid,      # Asset to which this encumbrance relates
                                  reference,        # Reference for this encumbrance, will be set to the contract address if omitted.
                                  amount,           # Amount of this Encumbrance
                                  [beneficiaries],  # Beneficiaries.  Those addresses that may exercise an encumbrance with Start and End Times. End time
                                                      of 0 indicates unending. Format [[Address, StartUTC_Secs, EndUTC_Secs], ...]
                                  [administrators], # Administrators. Those addresses that may cancel (Amend as a future enhancement ?) an encumbrance with
                                                      Start and End Times. End time of 0 indicates unending.
                                                      Format [[Address, StartUTC_Secs, EndUTC_Secs], ...]
                                  Signature,        # (API Optional) Signature of
                                                      sha256(ujson.dumps([contractAddress, fullAssetID, reference, amount], sort_keys=True)).hexdigest()
                                                      If provided by POA, this Signature will have matched the Authoring Public key.
                                  PoaPublicKey      # (API Optional) Internal Use. do not specify. Used to record PoaPublic key when PoAs have been used to sign

                               ],
                               ...
                            ],
         'events'        : [                  # List of events that can occur.
                              'commit'
                              'expiry'
                           ],
         'startdate'     : nnn,               # (Optional) long(UTC Unix Epoch time, seconds) indicating the earliest time at which a DVP may execute.
                                                It may, of course, be signed or commited to before this time.
         'expiry'        : nnn,               # long(UTC Unix Epoch time, seconds)
         'encumbrance'   : [Use Creator Encumbrance (Bool), EncumbranceName (String) defaults to Contract Address],
                                              # (Optional) If included and 'Use Creator Encumbrance' is True, then Unsigned Parties are allowed
                                                (unless marked MustSign) and unsigned payments will be fulfilled if there is an encumbrance to the address
                                                that creates the contract (not the contract address) with the specified EncumbranceName.
                                              # Encumbrances can either be enduring or specific to a contract. To allow encumbrances specific to a
                                                contract, if 'EncumbranceName' is empty, then the contract address is used as the EncumbranceName.
                                              # Enduring encumbrances are consumed as unsigned payments are made against them.
                                              # Contract specific encumbrances (i.e. those that have the contract address as reference) are consumed as they
                                              # are used and deleted in any case when the contract exercises or expires.
         'protocol'      : "",                # (API Optional) User Data
         'metadata'      : "",                # (API Optional)
         'autosign'      : Boolean            # (API Optional) Defaults to True. If True, the Wallet node will attempt to sign the contract with the AUTHORING
                                                key (Note : only the Authoring key, See also below.).
         'signforwallet' : Boolean            # (API Optional) Defaults to False. If True (and autosign is true), the node will attempt to sign the contract
                                                with ANY appropriate key in the current wallet. (Current wallet is the one being used to author this
                                                transaction).
         }

         Parties must be signed, except when :
           1) There is an appropriate Encumbrance in place, or
           2) There are no Payments associated with the party. If the Party property `MustSign` is true, then the Party must sign regardless of this.

         Encumbrances may be specified towards the contract author in which case the contract definition should indicate their possible existence.
         If an Encumbrance is specified, then unless a party is marked as 'MustSign', any appropriate encumbrance may be used without party signature or
         payment signature.
         In the case where a party IS marked as 'MustSign', then the party must be signed but, again, the payments need not be.

         Encumbrance logic :

            Only used at all if 'encumbrance.UseCreatorEncumbrance' is true.

            Only one encumbrance per payment is checked, encumbrance name priority is PaymentEncumbrance -> ContractEncumbrance -> ContractAddress

         Payment logic :

           If No Encumbrance :
           .
           .  If Signed :
           .  .
           .  .  If Issuance :
           .  .
           .  .    PAY
           .  .
           .  .  If Unencumbered holding sufficient :
           .  .
           .  .    PAY
           .
           If Has Encumbrance :
           .
           .  If sufficient Encumbrance
           .
           .  .    PAY
           .
           .  If insufficient Encumbrance :
           .  .
           .  .  If Signed and Unencumbered balance :
           .  .  .
           .  .  .  PAY
           .  .  .
           .  .  If Signed and Is Issuance :
           .  .  .
           .  .  .  PAY

         Locked Assets are checked when a DVP Contract is created (The TX will fail) and in the updateevent process which will catch
         Commits and time events to valid contracts. If the contract fails to complete in the updateevent process because of an
         Asset lock, it will not automatically re-try.

         For POA initiated contracts, the attorney may sign for the poa party and for payments from the poaAddress if the POA references the relevant assets.
         Quantities from the POA are not consumed as the amounts could be Strings or Formulae.

         Please note, where '(API Optional)' is shown above, that these are optional parameters. This means that items from this point do not need to be
         specified. Where the item is part of an array (i.e. not top level) the array can be shortened. DO NOT USE null in place of these parameters
         unless it is a String-Type parameter, specify default values.
         Where arrays are used, they are all position sensitive. Do not omit an '(API Optional)' element. If you want to provide an element that comes
         after an Optional element specify default values instead.


  */

  public static class DvpAddEncumbrance implements ToObjectArray {

    public final List<EncumbranceDetail> administrators;

    public final Balance amount;

    public final String amountString;

    public final List<EncumbranceDetail> beneficiaries;

    public final String fullAssetID;

    public final String reference;

    // Short-term, non-persisted value used when evaluating addEncumbrances.
    // NOT TO BE in the Hash, encode or decode methods !!!
    @SuppressWarnings("squid:ClassVariableVisibilityCheckClass")
    public Balance calculatedAmount = BALANCE_ZERO;

    private String poaPublicKey;

    private String publicKey;

    private String signature;


    /**
     * DvpAddEncumbrance().
     *
     * @param fullAssetID :
     * @param reference   :
     * @param amount      :
     * @param publicKey   :
     * @param signature   :
     */
    public DvpAddEncumbrance(
        String fullAssetID,
        String reference,
        Number amount,
        String publicKey,
        String signature
    ) {

      this.fullAssetID = fullAssetID;
      this.reference = reference;
      this.amount = new Balance(amount);
      this.amountString = null;
      this.publicKey = (publicKey == null ? "" : publicKey);
      this.signature = signature;
      this.poaPublicKey = "";

      this.beneficiaries = new ArrayList<>();
      this.administrators = new ArrayList<>();
    }


    /**
     * DvpAddEncumbrance().
     *
     * @param fullAssetID :
     * @param reference   :
     * @param amount      :
     * @param publicKey   :
     * @param signature   :
     */
    public DvpAddEncumbrance(
        String fullAssetID,
        String reference,
        String amount,
        String publicKey,
        String signature
    ) {

      this.fullAssetID = fullAssetID;
      this.reference = reference;
      this.amount = null;
      this.amountString = amount;
      this.publicKey = (publicKey == null ? "" : publicKey);
      this.signature = signature;
      this.poaPublicKey = "";

      this.beneficiaries = new ArrayList<>();
      this.administrators = new ArrayList<>();
    }


    /**
     * DvpAddEncumbrance Constructor.
     *
     * @param fullAssetID  : Asset ID to add Encumbrance for.
     * @param reference    : Encumbrance Reference to add.
     * @param amount       : Amount of encumbrance.
     * @param poaAddress   : Address against which to add the Encumbrance
     * @param poaPublicKey : POA Public Key relating to the signature
     * @param signature    : Signature
     */
    public DvpAddEncumbrance(
        String fullAssetID,
        String reference,
        Number amount,
        String poaAddress,
        String poaPublicKey,
        String signature
    ) {

      this(
          fullAssetID,
          reference,
          amount,
          poaAddress,
          signature
      );

      this.poaPublicKey = poaPublicKey;
    }


    /**
     * DvpAddEncumbrance Constructor.
     *
     * @param fullAssetID  : Asset ID to add Encumbrance for.
     * @param reference    : Encumbrance Reference to add.
     * @param amount       : Amount of encumbrance.
     * @param poaAddress   : Address against which to add the Encumbrance
     * @param poaPublicKey : POA Public Key relating to the signature
     * @param signature    : Signature
     */
    public DvpAddEncumbrance(
        String fullAssetID,
        String reference,
        String amount,
        String poaAddress,
        String poaPublicKey,
        String signature
    ) {

      this(
          fullAssetID,
          reference,
          amount,
          poaAddress,
          signature
      );

      this.poaPublicKey = poaPublicKey;
    }


    /**
     * DvpAddEncumbrance().
     * <p>Constructor : From Object array.</p>
     *
     * @param dvpData :
     */
    public DvpAddEncumbrance(Object[] dvpData) {

      this(new MPWrappedArrayImpl(dvpData));
    }


    /**
     * DvpAddEncumbrance().
     * <p>Constructor : From MPWrappedArray.</p>
     *
     * @param dvpData :
     */
    public DvpAddEncumbrance(@Nonnull MPWrappedArray dvpData) {

      int dLen = dvpData.size();
      beneficiaries = new ArrayList<>();
      administrators = new ArrayList<>();

      this.publicKey = (dLen > 0 ? dvpData.asString(0) : "");
      this.fullAssetID = (dLen > 1 ? dvpData.asString(1) : "");
      this.reference = (dLen > 2 ? dvpData.asString(2) : "");

      if (dLen > 3) {
        if (dvpData.get(3) instanceof String) {
          this.amount = null;
          this.amountString = dvpData.asString(3);
        } else {
          this.amount = new Balance(dvpData.get(3));
          this.amountString = null;
        }
      } else {
        this.amount = BALANCE_ZERO;
        this.amountString = null;
      }

      this.signature = (dLen > 6 ? dvpData.asString(6) : "");
      this.poaPublicKey = (dLen > 7 ? dvpData.asString(7) : "");

      if (this.publicKey == null) {
        this.publicKey = "";
      }

      if (this.poaPublicKey == null) {
        this.poaPublicKey = "";
      }

      MPWrappedArray myBeneficiaries = (dLen > 4 ? dvpData.asWrapped(4) : null);
      MPWrappedArray myAdministrators = (dLen > 5 ? dvpData.asWrapped(5) : null);

      MPWrappedArray thisWrapped;

      if (myBeneficiaries != null) {
        for (int index = 0, len = myBeneficiaries.size(); index < len; index++) {
          thisWrapped = myBeneficiaries.asWrapped(index);
          this.beneficiaries.add(new EncumbranceDetail(thisWrapped.asString(0), thisWrapped.asLong(1), thisWrapped.asLong(2)));
        }
      }

      if (myAdministrators != null) {
        for (int index = 0, len = myAdministrators.size(); index < len; index++) {
          thisWrapped = myAdministrators.asWrapped(index);
          this.administrators.add(new EncumbranceDetail(thisWrapped.asString(0), thisWrapped.asLong(1), thisWrapped.asLong(2)));
        }
      }
    }


    /**
     * DvpAddEncumbrance().
     * <p>DvpAddEncumbrance : Copy constructor.</p>
     *
     * @param toCopy :
     */
    public DvpAddEncumbrance(DvpAddEncumbrance toCopy) {

      beneficiaries = new ArrayList<>();
      administrators = new ArrayList<>();

      this.publicKey = toCopy.publicKey;
      this.poaPublicKey = toCopy.poaPublicKey;
      this.fullAssetID = toCopy.fullAssetID;
      this.reference = toCopy.reference;
      this.amount = toCopy.amount;
      this.amountString = toCopy.amountString;
      this.signature = toCopy.signature;

      toCopy.beneficiaries.forEach(detail -> this.beneficiaries.add(new EncumbranceDetail(detail)));
      toCopy.administrators.forEach(detail -> this.administrators.add(new EncumbranceDetail(detail)));

    }


    @Override
    public Object[] encode(long unused) {

      Object[] encBeneficiaries = new Object[beneficiaries.size()];
      Object[] encAdmins = new Object[administrators.size()];

      final int[] index = {0};
      beneficiaries.forEach(detail -> encBeneficiaries[index[0]++] = detail.encode());

      index[0] = 0;
      administrators.forEach(detail -> encAdmins[index[0]++] = detail.encode());

      if (this.poaPublicKey.isEmpty()) {
        return new Object[]{
            this.publicKey,
            this.fullAssetID,
            this.reference,
            (this.amount != null ? this.amount.getValue() : this.amountString),
            encBeneficiaries,
            encAdmins,
            this.signature
        };
      } else {
        return new Object[]{
            this.publicKey,
            this.fullAssetID,
            this.reference,
            (this.amount != null ? this.amount.getValue() : this.amountString),
            encBeneficiaries,
            encAdmins,
            this.signature,
            this.poaPublicKey
        };
      }

    }


    /**
     * encodeJson.
     * <p>Return DVP AddEncumbrance data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      JSONArray encBeneficiaries = new JSONArray();
      JSONArray encAdmins = new JSONArray();

      beneficiaries.forEach(detail -> encBeneficiaries.add(detail.encodeJson()));
      administrators.forEach(detail -> encAdmins.add(detail.encodeJson()));

      rVal.put(PUBLIC_KEY, this.publicKey);
      rVal.put(ASSET_ID, this.fullAssetID);
      rVal.put(REFERENCE, reference);
      rVal.put(AMOUNT, (this.amount != null ? this.amount.getValue() : this.amountString));
      rVal.put(BENEFICIARIES, encBeneficiaries);
      rVal.put(ADMINS, encAdmins);
      rVal.put(SIGNATURE, this.signature);

      if (!this.poaPublicKey.isEmpty()) {
        rVal.put(POA_PUBLIC_KEY, this.poaPublicKey);
      }
      return rVal;
    }


    /**
     * encumbranceAmount getter, returns amount or calculatedAmount if amount is not set (i.e. a String amount is used).
     *
     * @return :
     */
    public Balance encumbranceAmount() {
      if (this.amount != null) {
        return this.amount;
      }

      return this.calculatedAmount;
    }


    /**
     * equals().
     * <p>Deep equality comparator.</p>
     *
     * @param toCompare :
     *
     * @return :
     */
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpAddEncumbrance)) {
        return false;
      }

      DvpAddEncumbrance theOther = (DvpAddEncumbrance) toCompare;

      if (!this.publicKey.equals(theOther.publicKey)) {
        return false;
      }

      if (!this.fullAssetID.equals(theOther.fullAssetID)) {
        return false;
      }

      if (!this.reference.equals(theOther.reference)) {
        return false;
      }

      if ((!Objects.equals(this.amount, theOther.amount)) && ((this.amount == null) || (!this.amount.equalTo(theOther.amount)))) {
        return false;
      }

      if (!Objects.equals(this.amountString, theOther.amountString)) {
        return false;
      }

      if (!this.signature.equals(theOther.signature)) {
        return false;
      }

      if (this.beneficiaries.size() != theOther.beneficiaries.size()) {
        return false;
      }

      if (this.administrators.size() != theOther.administrators.size()) {
        return false;
      }

      for (int index = 0, len = this.beneficiaries.size(); index < len; index++) {
        if (!this.beneficiaries.get(index).equals(theOther.beneficiaries.get(index))) {
          return false;
        }
      }

      for (int index = 0, len = this.administrators.size(); index < len; index++) {
        if (!this.administrators.get(index).equals(theOther.administrators.get(index))) {
          return false;
        }
      }

      if (!this.poaPublicKey.equalsIgnoreCase(theOther.poaPublicKey)) {
        return false;
      }

      return true;
    }


    public String getPoaPublicKey() {

      return poaPublicKey;
    }


    public String getPublicKey() {

      return publicKey;
    }


    public String getSignature() {

      return (signature == null ? "" : signature);
    }


    @Override
    public int hashCode() {

      return this.publicKey.hashCode();

    }


    public Object[] objectToHashToSign(String contractAddress) {

      return new Object[]{contractAddress, fullAssetID, reference, (amount != null ? amount.getValue() : amountString)};
    }


    public void setPoaPublicKey(String poaPublicKey) {

      this.poaPublicKey = poaPublicKey;
    }


    public void setPublicKey(String publicKey) {

      this.publicKey = publicKey;
    }


    public void setSignature(String signature) {

      this.signature = signature;
    }

  }



  /**
   * DvpAuthorisation.
   */
  public static class DvpAuthorisation implements ToObjectArray {

    public final String authorisationID;

    private final String[] addresses; // not used yet.

    private String address;  // Public Key or Address.

    private Integer contractSpecific;

    private String metadata;

    private String poaPublicKey;

    private boolean refused;

    private String signature;


    /**
     * DvpAuthorisation().
     * <p>DvpAuthorisation Constructor from Object Array.</p>
     *
     * @param dvpData :
     */
    DvpAuthorisation(Object[] dvpData) {

      this(new MPWrappedArrayImpl(dvpData));
    }


    /**
     * DvpAuthorisation().
     * <p>DvpAuthorisation Constructor from MPWrappedArray.</p>
     * <p>
     * [
     * addresses | [addresseses],
     * authorisationID,
     * signature,
     * metadata,
     * refused
     * ]
     * </p>
     *
     * @param dvpData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpAuthorisation(MPWrappedArray dvpData) {

      int dLen = (dvpData == null ? 0 : dvpData.size());

      if (dLen > 0) {
        if (dvpData.get(0) instanceof MPWrappedArray) {
          this.address = null;
          MPWrappedArray innerData = (MPWrappedArray) dvpData.get(0);

          addresses = new String[innerData.size()];

          for (int index = 0, l = innerData.size(); index < l; index++) {
            addresses[index] = innerData.asString(index);
          }

        } else if (dvpData.get(0) instanceof Object[]) {
          this.address = null;
          Object[] innerData = dvpData.asObjectArray(0);

          addresses = new String[innerData.length];

          for (int index = 0, l = innerData.length; index < l; index++) {
            addresses[index] = (String) innerData[index];
          }

        } else if (dvpData.get(0) instanceof List) {
          this.address = null;
          Object[] innerData = ((List) dvpData.get(0)).toArray();

          addresses = new String[innerData.length];

          for (int index = 0, l = innerData.length; index < l; index++) {
            addresses[index] = (String) innerData[index];
          }

        } else {
          addresses = null;
          this.address = dvpData.asString(0);
        }
      } else {
        addresses = null;
        this.address = "";
      }

      this.authorisationID = (dLen > 1 ? objectToString(dvpData.get(1)) : "");
      this.signature = (dLen > 2 ? dvpData.asString(2) : "");
      this.metadata = (dLen > 3 ? dvpData.asString(3) : "");
      this.refused = (dLen > 4 ? dvpData.asBoolean(4) : false);

      if (dLen > 5) {
        if (dvpData.get(5) instanceof Number) {
          this.contractSpecific = ((Number) dvpData.get(5)).intValue();
        } else {
          this.contractSpecific = Integer.valueOf(dvpData.get(5).toString());
        }
      } else {
        this.contractSpecific = 1;
      }

      if (dLen > 6) {
        this.poaPublicKey = (dvpData.get(6) != null ? dvpData.asString(6) : "");
      } else {
        this.poaPublicKey = "";
      }

    }


    /**
     * DvpAuthorisation().
     * <p>Constructor, single address.</p>
     *
     * @param address         : Address for Authorisation.
     * @param authorisationID :
     * @param signature       :
     * @param metadata        :
     * @param refused         :
     */
    public DvpAuthorisation(String address, String authorisationID, String signature, String metadata, boolean refused, Integer specific) {

      this.addresses = null;
      this.address = address;
      this.authorisationID = objectToString(authorisationID);
      this.signature = signature;
      this.metadata = objectToString(metadata);
      this.refused = refused;
      this.contractSpecific = (specific == null ? Integer.valueOf(1) : specific);
      this.poaPublicKey = "";
    }


    /**
     * DvpAuthorisation().
     * <p>Constructor, single address.</p>
     *
     * @param addresses       : Array of Addresses for Authorisation.
     * @param authorisationID :
     * @param signature       :
     * @param metadata        :
     * @param refused         :
     */

    public DvpAuthorisation(String[] addresses, String authorisationID, String signature, String metadata, boolean refused, Integer specific) {

      this.addresses = (addresses == null ? new String[0] : addresses.clone());
      this.address = null;
      this.authorisationID = objectToString(authorisationID);
      this.signature = signature;
      this.metadata = objectToString(metadata);
      this.refused = refused;
      this.contractSpecific = (specific == null ? Integer.valueOf(1) : specific);
      this.poaPublicKey = "";
    }


    /**
     * DvpAuthorisation().
     * <p>DvpAuthorisation Copy Constructor.</p>
     *
     * @param toCopy :
     */
    public DvpAuthorisation(DvpAuthorisation toCopy) {

      if (toCopy.addresses != null) {
        this.addresses = new String[toCopy.addresses.length];

        arraycopy(toCopy.addresses, 0, this.addresses, 0, toCopy.addresses.length);
      } else {
        this.addresses = null;
      }

      this.address = toCopy.address;
      this.authorisationID = toCopy.authorisationID;
      this.signature = toCopy.signature;
      this.poaPublicKey = toCopy.poaPublicKey;
      this.metadata = toCopy.metadata;
      this.refused = toCopy.refused;
      this.contractSpecific = toCopy.contractSpecific;

    }


    @Override
    public Object[] encode(long unused) {

      if ((!this.poaPublicKey.isEmpty()) || (this.contractSpecific.equals(0))) {
        return new Object[]{
            (this.address != null ? this.address : this.addresses),
            this.authorisationID,
            this.signature,
            this.metadata,
            this.refused,
            this.contractSpecific,
            this.poaPublicKey
        };
      } else {
        return new Object[]{
            (this.address != null ? this.address : this.addresses),
            this.authorisationID,
            this.signature,
            this.metadata,
            this.refused
        };
      }

    }


    /**
     * encodeJson.
     * <p>Return DVP Authorisation data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(ADDRESS, (this.address != null ? this.address : this.addresses));
      rVal.put(AUTHORISATION_ID, this.authorisationID);
      rVal.put(SIGNATURE, this.signature);
      rVal.put(METADATA, this.metadata);
      rVal.put(REFUSED, this.refused);

      if (!this.poaPublicKey.isEmpty()) {
        rVal.put(POA_PUBLIC_KEY, this.poaPublicKey);
      }

      rVal.put(CONTRACT_SPECIFIC, contractSpecific);

      return rVal;
    }


    /**
     * equals().
     * <p>Deep equality comparator.</p>
     *
     * @param toCompare :
     *
     * @return :
     */
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpAuthorisation)) {
        return false;
      }

      DvpAuthorisation theOther = (DvpAuthorisation) toCompare;

      if (address == null) {
        if (theOther.address != null) {
          return false;
        }
      } else {
        if (!this.address.equals(theOther.address)) {
          return false;
        }
      }

      if (addresses == null) {
        if (theOther.addresses != null) {
          return false;
        }
      } else {
        if (this.addresses.length != theOther.addresses.length) {
          return false;
        }

        for (int index = 0, l = this.addresses.length; index < l; index++) {
          if (!this.addresses[index].equals(theOther.addresses[index])) {
            return false;
          }
        }
      }

      if (!this.authorisationID.equals(theOther.authorisationID)) {
        return false;
      }

      if (!this.signature.equals(theOther.signature)) {
        return false;
      }

      if (!this.metadata.equals(theOther.metadata)) {
        return false;
      }

      if (!this.poaPublicKey.equalsIgnoreCase(theOther.poaPublicKey)) {
        return false;
      }

      if (!Objects.equals(contractSpecific, theOther.contractSpecific)) {
        return false;
      }

      return this.refused == theOther.refused;
    }


    // TODO - it is assumed that this address could actually be a public key. It would be better if it had a fixed data type.
    public String getAddress() {

      return address;
    }


    public String[] getAddresses() {

      return (addresses == null) ? null : addresses.clone();
    }


    public String getMetadata() {

      return metadata;
    }


    public String getPoaPublicKey() {

      return poaPublicKey;
    }


    /**
     * getRefused().
     *
     * @return :
     */
    public boolean getRefused() {

      return refused;
    }


    /**
     * getSignature().
     *
     * @return :
     */
    public String getSignature() {

      return signature;
    }


    @Override
    public int hashCode() {

      return this.address.hashCode();

    }


    public boolean isContractSpecific() {

      return (!this.contractSpecific.equals(0));
    }


    public void setAddress(String address) {

      this.address = address;
    }


    public void setMetadata(String metadata) {

      this.metadata = (metadata == null ? "" : metadata);
    }


    public void setPoaPublicKey(String poaPublicKey) {

      this.poaPublicKey = poaPublicKey;
    }


    /**
     * setRefused().
     *
     * @param refused :
     */
    public void setRefused(boolean refused) {

      this.refused = refused;
    }


    /**
     * setSignature().
     *
     * @param signature :
     */
    public void setSignature(String signature) {

      this.signature = signature;
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
      if (this.contractSpecific.equals(0)) {
        return String.format("%s_%d", authorisationID, (getRefused() ? 1 : 0));
      } else {
        return String.format("%s%s_%d", contractAddress, authorisationID, (getRefused() ? 1 : 0));
      }
    }

  }



  /**
   * DvpEncumbrance Class.
   */
  public static class DvpEncumbrance implements ToObjectArray {

    public final String encumbranceName;

    public final boolean useCreatorEncumbrance;


    /**
     * DvpEncumbrance().
     * <p>DvpEncumbrance Constructor.</p>
     *
     * @param useEncumbrance  :
     * @param encumbranceName :
     */
    public DvpEncumbrance(boolean useEncumbrance, String encumbranceName) {

      this.useCreatorEncumbrance = useEncumbrance;
      this.encumbranceName = (encumbranceName == null ? "" : encumbranceName);
    }


    /**
     * DvpEncumbrance().
     * <p>DvpEncumbrance Constructor, from MPWrappedArray.</p>
     *
     * @param dataArray :
     */
    public DvpEncumbrance(MPWrappedArray dataArray) {

      if (dataArray.size() > 1) {
        this.useCreatorEncumbrance = dataArray.asBoolean(0);
        this.encumbranceName = dataArray.asString(1);
      } else {
        this.useCreatorEncumbrance = false;
        this.encumbranceName = "";
      }
    }


    /**
     * DvpEncumbrance().
     * <p>DvpEncumbrance Constructor, from Object Array.</p>
     *
     * @param dataArray :
     */
    public DvpEncumbrance(Object[] dataArray) {

      if (dataArray.length > 1) {
        this.useCreatorEncumbrance = (boolean) dataArray[0];
        this.encumbranceName = (String) dataArray[1];
      } else {
        this.useCreatorEncumbrance = false;
        this.encumbranceName = "";
      }
    }


    /**
     * DvpEncumbrance().
     * <p>DvpEncumbrance Copy constructor.</p>
     *
     * @param toCopy :
     */
    public DvpEncumbrance(DvpEncumbrance toCopy) {

      this.useCreatorEncumbrance = toCopy.useCreatorEncumbrance;
      this.encumbranceName = toCopy.encumbranceName;
    }


    /**
     * encode().
     * <p>Return DvpEncumbrance as Object[] for Serialisation / Persistence.
     * </p>
     *
     * @param unused :
     *
     * @return : Object[]
     */
    @Override
    public Object[] encode(long unused) {

      return new Object[]{this.useCreatorEncumbrance, this.encumbranceName};

    }


    /**
     * encodeJson.
     * <p>Return DVP Encumbrance data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(USE_CREATOR_ENCUMBRANCE, this.useCreatorEncumbrance);
      rVal.put(ENCUMBRANCE_NAME, this.encumbranceName);

      return rVal;
    }


    /**
     * equals().
     * <p>Deep equality comparator.</p>
     *
     * @param toCompare :
     *
     * @return :
     */
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpEncumbrance)) {
        return false;
      }

      DvpEncumbrance theOther = (DvpEncumbrance) toCompare;

      if (this.useCreatorEncumbrance != theOther.useCreatorEncumbrance) {
        return false;
      }

      return this.encumbranceName.equals(theOther.encumbranceName);
    }


    @Override
    public int hashCode() {

      return this.encumbranceName.hashCode();

    }

  }



  /**
   * DvpEvent class.
   * <p>String Array of allowable events (not actually checked in code ?)</p>
   */
  public static class DvpEvent implements ToObjectArray {

    private String[] events;


    /**
     * DvpEvent().
     * <p>Constructor : From given String Array</p>
     *
     * @param events :
     */
    public DvpEvent(String[] events) {

      if (events != null) {
        this.events = events.clone();
      } else {
        this.events = new String[0];
      }

    }


    /**
     * DvpEvent().
     * <p>Constructor : From given Object Array (of Strings)</p>
     *
     * @param events :
     */
    public DvpEvent(Object[] events) {

      this(new MPWrappedArrayImpl(events));
    }


    /**
     * DvpEvent().
     * <p>Constructor : From given MPWrappedArray (of Strings)</p>
     *
     * @param events :
     */
    public DvpEvent(MPWrappedArray events) {

      if (events != null) {
        this.events = new String[events.size()];

        for (int index = 0, l = events.size(); index < l; index++) {
          this.events[index] = events.asString(index);
        }
      } else {
        this.events = new String[0];
      }

    }


    /**
     * DvpEvent().
     * <p>Copy Constructor.</p>
     *
     * @param toCopy :
     */
    public DvpEvent(DvpEvent toCopy) {

      if ((toCopy != null) && (toCopy.events != null)) {
        this.events = new String[toCopy.events.length];

        arraycopy(toCopy.events, 0, this.events, 0, toCopy.events.length);
      } else {
        this.events = new String[0];
      }

    }


    @Override
    public Object[] encode(long unused) {

      return (events == null) ? null : events.clone();
    }


    /**
     * encodeJson.
     * <p>Return DVP Event data as JSONArray</p>
     *
     * @return :
     */
    public JSONArray encodeJson() {

      JSONArray rVal = new JSONArray();

      if (this.events != null) {
        rVal.addAll(Arrays.asList(this.events));
      }

      return rVal;
    }


    /**
     * equals().
     * <p>Deep equality comparator.</p>
     *
     * @param toCompare :
     *
     * @return :
     */
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpEvent)) {
        return false;
      }

      DvpEvent theOther = (DvpEvent) toCompare;

      if (theOther.events.length != this.events.length) {
        return false;
      }

      for (int index = 0, l = this.events.length; index < l; index++) {
        if (!this.events[index].equals(theOther.events[index])) {
          return false;
        }
      }

      return true;
    }


    @Override
    public int hashCode() {

      return Arrays.hashCode(this.events);

    }

  }



  /**
   * DvpParameter Class.
   */
  public static class DvpParameter implements ToObjectArray {

    public final int calculatedIndex;

    public final int calculationOnly;

    public final int contractSpecific;

    private String address;

    private String[] addresses;

    private String poaPublicKey;

    private String signature;

    private Number valueNumber;

    private String valueString;


    /**
     * DvpParameter().
     * <p>DvpParameter Constructor from Object Array.</p>
     *
     * @param dvpData :
     */
    public DvpParameter(Object[] dvpData) {

      this(new MPWrappedArrayImpl(dvpData));
    }


    /**
     * DvpParameter().
     * <p>DvpParameter Constructor from MPWrappedArray.</p>
     *
     * @param dvpData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpParameter(MPWrappedArray dvpData) {

      int dLen = (dvpData != null ? dvpData.size() : 0);

      if (dLen > 0) {
        if ((dvpData.get(0) instanceof MPWrappedArray) || (dvpData.get(0) instanceof List)) {
          this.address = null;
          MPWrappedArray innerData = dvpData.asWrapped(0);

          addresses = new String[innerData.size()];

          for (int index = 0, l = innerData.size(); index < l; index++) {
            addresses[index] = innerData.asString(index);
          }

        } else if (dvpData.get(0) instanceof Object[]) {
          this.address = null;
          Object[] innerData = dvpData.asObjectArray(0);

          addresses = new String[innerData.length];

          for (int index = 0, l = innerData.length; index < l; index++) {
            addresses[index] = (String) innerData[index];
          }

        } else {
          addresses = null;
          this.address = dvpData.asString(0);
        }
      } else {
        addresses = null;
        this.address = "";
      }

      if ((dLen > 1) && (dvpData.get(1) != null)) {
        if (dvpData.get(1) instanceof Number) {
          valueNumber = (new Balance(dvpData.get(1))).getValue();
          valueString = null;
        } else {
          this.setValue(dvpData.asString(1));
        }
      }

      this.calculatedIndex = (dLen > 2 ? dvpData.asInt(2) : 0);
      this.contractSpecific = (dLen > 3 ? dvpData.asInt(3) : 0);
      this.calculationOnly = (dLen > 4 ? dvpData.asInt(4) : 0);
      this.signature = (dLen > 5 ? dvpData.asString(5) : "");

      this.poaPublicKey = (dLen > 6 ? dvpData.asString(6) : "");

      if (this.poaPublicKey == null) {
        this.poaPublicKey = "";
      }

    }


    /**
     * DvpParameter().
     * <p>DvpParameter Constructor.</p>
     *
     * @param address          :
     * @param value            :
     * @param calculatedIndex  :
     * @param contractSpecific :
     * @param calculationOnly  :
     * @param signature        :
     */
    public DvpParameter(String address, Number value, int calculatedIndex, int contractSpecific, int calculationOnly, String signature) {

      this.addresses = null;
      this.address = address;
      this.valueNumber = (new Balance(value)).getValue();
      this.valueString = null;
      this.calculatedIndex = calculatedIndex;
      this.contractSpecific = contractSpecific;
      this.calculationOnly = calculationOnly;
      this.signature = signature;
      this.poaPublicKey = "";
    }


    /**
     * DvpParameter().
     * <p>DvpParameter Constructor.</p>
     *
     * @param addresses        :
     * @param value            :
     * @param calculatedIndex  :
     * @param contractSpecific :
     * @param calculationOnly  :
     * @param signature        :
     */
    public DvpParameter(String[] addresses, Number value, int calculatedIndex, int contractSpecific, int calculationOnly, String signature) {

      if (addresses != null) {
        this.addresses = new String[addresses.length];

        arraycopy(addresses, 0, this.addresses, 0, addresses.length);
      } else {
        this.addresses = null;
      }

      this.address = null;
      this.valueNumber = (new Balance(value)).getValue();
      this.valueString = null;
      this.calculatedIndex = calculatedIndex;
      this.contractSpecific = contractSpecific;
      this.calculationOnly = calculationOnly;
      this.signature = signature;
      this.poaPublicKey = "";
    }


    /**
     * DvpParameter().
     * <p>DvpParameter Constructor.</p>
     *
     * @param address          :
     * @param value            :
     * @param calculatedIndex  :
     * @param contractSpecific :
     * @param calculationOnly  :
     * @param signature        :
     */
    public DvpParameter(String address, String value, int calculatedIndex, int contractSpecific, int calculationOnly, String signature) {

      this.addresses = null;
      this.address = address;
      this.valueNumber = null;
      this.setValue(value);
      this.calculatedIndex = calculatedIndex;
      this.contractSpecific = contractSpecific;
      this.calculationOnly = calculationOnly;
      this.signature = signature;
      this.poaPublicKey = "";
    }


    /**
     * DvpParameter().
     * <p>DvpParameter Constructor.</p>
     *
     * @param addresses        :
     * @param value            :
     * @param calculatedIndex  :
     * @param contractSpecific :
     * @param calculationOnly  :
     * @param signature        :
     */
    public DvpParameter(String[] addresses, String value, int calculatedIndex, int contractSpecific, int calculationOnly, String signature) {

      if (addresses != null) {
        this.addresses = new String[addresses.length];

        arraycopy(addresses, 0, this.addresses, 0, addresses.length);
      } else {
        this.addresses = null;
      }

      this.address = null;
      this.valueNumber = null;
      this.setValue(value);
      this.calculatedIndex = calculatedIndex;
      this.contractSpecific = contractSpecific;
      this.calculationOnly = calculationOnly;
      this.signature = signature;
      this.poaPublicKey = "";
    }


    /**
     * DvpParameter().
     * <p>DvpParameter Copy Constructor.</p>
     *
     * @param toCopy :
     */
    public DvpParameter(DvpParameter toCopy) {

      if (toCopy.addresses != null) {
        this.addresses = new String[toCopy.addresses.length];

        arraycopy(toCopy.addresses, 0, this.addresses, 0, toCopy.addresses.length);
      } else {
        this.addresses = null;
      }

      this.address = toCopy.address;
      this.valueNumber = toCopy.valueNumber;
      this.valueString = toCopy.valueString;
      this.calculatedIndex = toCopy.calculatedIndex;
      this.contractSpecific = toCopy.contractSpecific;
      this.calculationOnly = toCopy.calculationOnly;
      this.signature = toCopy.signature;
      this.poaPublicKey = toCopy.poaPublicKey;
    }


    @Override
    public Object[] encode(long unused) {

      if (this.poaPublicKey.isEmpty()) {
        return new Object[]{
            (this.address != null ? this.address : this.addresses),
            (this.valueNumber != null ? this.valueNumber : this.valueString),
            this.calculatedIndex,
            this.contractSpecific,
            this.calculationOnly,
            this.signature
        };
      } else {
        return new Object[]{
            (this.address != null ? this.address : this.addresses),
            (this.valueNumber != null ? this.valueNumber : this.valueString),
            this.calculatedIndex,
            this.contractSpecific,
            this.calculationOnly,
            this.signature,
            this.poaPublicKey
        };
      }

    }


    private JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(ADDRESS, (this.address != null ? this.address : this.addresses));
      rVal.put(VALUE, (this.valueNumber != null ? this.valueNumber : this.valueString));
      rVal.put(CALCULATED_INDEX, this.calculatedIndex);
      rVal.put(CONTRACT_SPECIFIC, this.contractSpecific);
      rVal.put(CALCULATION_ONLY, this.calculationOnly);
      rVal.put(SIGNATURE, this.signature);

      if (!this.poaPublicKey.isEmpty()) {
        rVal.put(POA_PUBLIC_KEY, this.poaPublicKey);
      }

      return rVal;
    }


    /**
     * equals().
     * <p>Deep equality comparator.</p>
     *
     * @param toCompare :
     *
     * @return :
     */
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpParameter)) {
        return false;
      }

      DvpParameter theOther = (DvpParameter) toCompare;

      if (address == null) {
        if (theOther.address != null) {
          return false;
        }
      } else {
        if (!this.address.equals(theOther.address)) {
          return false;
        }
      }

      if (addresses == null) {
        if (theOther.addresses != null) {
          return false;
        }
      } else {
        if (this.addresses.length != theOther.addresses.length) {
          return false;
        }

        for (int index = 0, l = this.addresses.length; index < l; index++) {
          if (!this.addresses[index].equals(theOther.addresses[index])) {
            return false;
          }
        }
      }

      if (this.valueNumber != null) {
        if (!this.valueNumber.equals(theOther.valueNumber)) {
          return false;
        }
      } else {
        if (!Objects.equals(this.valueString, theOther.valueString)) {
          return false;
        }
      }

      if (this.calculatedIndex != theOther.calculatedIndex) {
        return false;
      }

      if (this.contractSpecific != theOther.contractSpecific) {
        return false;
      }

      if (this.calculationOnly != theOther.calculationOnly) {
        return false;
      }

      if (!this.poaPublicKey.equalsIgnoreCase(theOther.poaPublicKey)) {
        return false;
      }

      return this.signature.equals(theOther.signature);
    }


    public String getAddress() {

      return address;
    }


    /**
     * Get the addresses associated with this contract.
     *
     * @return the addresses
     */
    @Nonnull
    public String[] getAddresses() {

      if (this.addresses == null) {
        return new String[]{this.address};
      }

      return this.addresses.clone();
    }


    public String getPoaPublicKey() {

      return poaPublicKey;
    }


    /**
     * getSignature().
     *
     * @return :
     */
    public String getSignature() {

      return signature;
    }


    /**
     * getValue().
     *
     * @return :
     */
    public Object getValue() {

      if (valueNumber == null) {
        return (valueString == null ? 0L : valueString);
      }

      return valueNumber;
    }


    public Number getValueNumber() {

      return valueNumber;
    }


    public String getValueString() {

      return valueString;
    }


    @Override
    public int hashCode() {

      return this.address.hashCode();

    }


    /**
     * Is the value of this parameter specified? A specified parameter has either a non-null numeric value, or a non-null, non-empty String value, or a
     * signed empty String value.
     *
     * @return true if the parameter has a value of some kind.
     */
    public boolean isSpecified() {
      return valueNumber != null || (valueString != null && (valueString.length() != 0 || signature != null));
    }


    public void setAddress(String address) {

      this.address = address;
    }


    public void setPoaPublicKey(String poaPublicKey) {

      this.poaPublicKey = poaPublicKey;
    }


    /**
     * setSignature().
     *
     * @param signature :
     */
    public void setSignature(String signature) {

      this.signature = signature;
    }


    /**
     * setValue().
     *
     * @param value :
     */
    public void setValue(String value) {
      // If the value is unspecified, it must remain unspecified.
      if (((value == null) || (value.isEmpty()))) {
        valueNumber = null;
        valueString = null;
        return;
      }

      // Check if the value is an integer.
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
        // Must be too large to fit in a Long. Any maths we try to do with this will require BigIntegers.
        valueNumber = null;
        valueString = value;
      }
    }


    /**
     * setValue().
     *
     * @param value :
     */
    public void setValue(Number value) {

      this.valueNumber = (new Balance(value)).getValue();
      this.valueString = null;
    }


    /**
     * stringToHashToSign().
     * <p>Return String message for this item that is used to sign, or verify a signature, this item.</p>
     *
     * @param keyName         :
     * @param contractAddress :
     *
     * @return :
     */
    public String stringToHashToSign(String keyName, String contractAddress) {

      if (contractSpecific == 0) {
        return String.format("%s%s", keyName, getValue());
      } else {
        return String.format("%s%s%s", contractAddress, keyName, getValue());
      }

    }

  }



  /**
   * DvpParty Class.
   * <p>
   * Data model representation of a DvP Party.
   * </p>
   */
  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpParty implements ToObjectArray {

    public boolean mustSign;

    public String partyIdentifier;

    public List<DvpPayItem> payList;

    public String publicKey;

    public List<DvpReceiveItem> receiveList;

    public String sigAddress;

    public String signature;


    /**
     * DvpParty().
     * <p>Constructor to use given values. Pay an dReceive lists are initialised, but must be populated separately.</p>
     *
     * @param partyIdentifier :
     * @param sigAddress      :
     * @param publicKey       :
     * @param signature       :
     * @param mustSign        :
     */
    public DvpParty(
        String partyIdentifier,
        String sigAddress,
        String publicKey,
        String signature,
        boolean mustSign
    ) {

      this.partyIdentifier = (partyIdentifier != null ? partyIdentifier : "");
      this.sigAddress = (sigAddress != null ? sigAddress : "");
      this.publicKey = (publicKey != null ? publicKey : "");
      this.signature = (signature != null ? signature : "");
      this.mustSign = mustSign;

      this.payList = new ArrayList<>();
      this.receiveList = new ArrayList<>();
    }


    /**
     * DvpParty().
     * <p>
     * Constructor from Object Array.
     * </p>
     *
     * @param partyData :
     */
    public DvpParty(Object partyData) {

      if (partyData instanceof Object[]) {
        decode(new MPWrappedArrayImpl((Object[]) partyData));
      } else if (partyData instanceof MPWrappedArray) {
        decode((MPWrappedArray) partyData);
      } else if (partyData instanceof List) {
        decode(new MPWrappedArrayImpl(((List) partyData).toArray()));
      }
    }


    /**
     * DvpParty().
     * <p>
     * Constructor from Object Array.
     * </p>
     *
     * @param partyData :
     */
    public DvpParty(Object[] partyData) {

      this(new MPWrappedArrayImpl(partyData));
    }


    /**
     * DvpParty().
     * <p>Constructor from MPWrappedArray.
     * [
     * partyIdentifier,
     * sigAddress,
     * [payList],
     * [receiveList],
     * publicKey,
     * signature,
     * mustsign
     * ]
     * </p>
     *
     * @param partyData :
     */
    public DvpParty(MPWrappedArray partyData) {

      this.decode(partyData);

    }


    /**
     * DvpParty() Copy Constructor.
     *
     * @param toCopy :
     */
    public DvpParty(DvpParty toCopy) {

      this.partyIdentifier = toCopy.partyIdentifier;
      this.sigAddress = toCopy.sigAddress;
      this.publicKey = toCopy.publicKey;
      this.signature = toCopy.signature;
      this.mustSign = toCopy.mustSign;

      if (this.partyIdentifier == null) {
        this.partyIdentifier = "";
      }
      if (this.sigAddress == null) {
        this.sigAddress = "";
      }
      if (this.publicKey == null) {
        this.publicKey = "";
      }
      if (this.signature == null) {
        this.signature = "";
      }

      payList = new ArrayList<>();
      receiveList = new ArrayList<>();

      toCopy.payList.forEach(detail -> this.payList.add(new DvpPayItem(detail)));
      toCopy.receiveList.forEach(detail -> this.receiveList.add(new DvpReceiveItem(detail)));

    }


    private void decode(MPWrappedArray partyData) {

      int dLen = partyData.size();

      this.partyIdentifier = (dLen > 0 ? partyData.asString(0) : "");
      this.sigAddress = (dLen > 1 ? partyData.asString(1) : "");
      this.publicKey = (dLen > 4 ? partyData.asString(4) : "");
      this.signature = (dLen > 5 ? partyData.asString(5) : "");
      this.mustSign = (dLen > 6 ? partyData.asBoolean(6) : false);

      if (this.partyIdentifier == null) {
        this.partyIdentifier = "";
      }
      if (this.sigAddress == null) {
        this.sigAddress = "";
      }
      if (this.publicKey == null) {
        this.publicKey = "";
      }
      if (this.signature == null) {
        this.signature = "";
      }

      payList = new ArrayList<>();
      receiveList = new ArrayList<>();

      if ((dLen > 2) && (partyData.get(2) != null)) {
        MPWrappedArray myPayList = partyData.asWrapped(2);

        if (myPayList != null) {
          for (int index = 0, len = myPayList.size(); index < len; index++) {
            this.payList.add(new DvpPayItem(myPayList.asWrapped(index)));
          }
        }
      }

      if ((dLen > 3) && (partyData.get(3) != null)) {
        MPWrappedArray myReceiveList = partyData.asWrapped(3);

        if (myReceiveList != null) {
          for (int index = 0, len = myReceiveList.size(); index < len; index++) {
            this.receiveList.add(new DvpReceiveItem(myReceiveList.asWrapped(index)));
          }
        }
      }
    }


    @Override
    public Object[] encode(long unused) {

      Object[] encPayList = new Object[payList.size()];
      Object[] encReceiveList = new Object[receiveList.size()];

      final int[] index = {0};
      payList.forEach(detail -> encPayList[index[0]++] = detail.encode(0));

      index[0] = 0;
      receiveList.forEach(detail -> encReceiveList[index[0]++] = detail.encode(0));

      return new Object[]{
          this.partyIdentifier,
          this.sigAddress,
          encPayList,
          encReceiveList,
          this.publicKey,
          this.signature,
          this.mustSign
      };

    }


    /**
     * encodeJson.
     * <p>Return DVP Party data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      JSONArray encPayList = new JSONArray();
      JSONArray encReceiveList = new JSONArray();

      payList.forEach(detail -> encPayList.add(detail.encodeJson()));
      receiveList.forEach(detail -> encReceiveList.add(detail.encodeJson()));

      rVal.put(PARTY_IDENTIFIER, this.partyIdentifier);
      rVal.put(SIG_ADDRESS, this.sigAddress);
      rVal.put(PAY_LIST, encPayList);
      rVal.put(RECEIVE_LIST, encReceiveList);
      rVal.put(PUBLIC_KEY, this.publicKey);
      rVal.put(SIGNATURE, this.signature);
      rVal.put(MUST_SIGN, this.mustSign);

      return rVal;
    }


    /**
     * equals().
     * <p>Deep equality comparator.</p>
     *
     * @param toCompare :
     *
     * @return :
     */
    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpParty)) {
        return false;
      }

      DvpParty theOther = (DvpParty) toCompare;

      if (!this.partyIdentifier.equals(theOther.partyIdentifier)) {
        return false;
      }

      if (!this.sigAddress.equals(theOther.sigAddress)) {
        return false;
      }

      if (!this.publicKey.equals(theOther.publicKey)) {
        return false;
      }

      if (!this.signature.equals(theOther.signature)) {
        return false;
      }

      if (this.mustSign != theOther.mustSign) {
        return false;
      }

      if (this.payList.size() != theOther.payList.size()) {
        return false;
      }

      if (this.receiveList.size() != theOther.receiveList.size()) {
        return false;
      }

      for (int index = 0, len = this.payList.size(); index < len; index++) {
        if (!this.payList.get(index).equals(theOther.payList.get(index))) {
          return false;
        }
      }

      for (int index = 0, len = this.receiveList.size(); index < len; index++) {
        if (!this.receiveList.get(index).equals(theOther.receiveList.get(index))) {
          return false;
        }
      }

      return true;
    }


    @Override
    public int hashCode() {

      return this.sigAddress.hashCode();

    }

  }



  /**
   * DvpPayItem.
   */
  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpPayItem implements ToObjectArray, Comparable<DvpPayItem> {

    public final String classID;

    public final String encumbrance;

    public final boolean issuance;

    public final String metadata;

    public final String namespace;

    public String address;

    public Balance amountNumber;

    public String amountString;

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD") // I do not know why FindBugs thinks this field is never used
    public String effectiveEncumbrance = null; // Ephemeral property used in Contract processing. Not to be serialised.

    @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD") // I do not know why FindBugs thinks this field is never used
    public Balance encumbranceAmount = BALANCE_ZERO; // Ephemeral property used in Contract processing. Not to be serialised.

    public String publicKey;

    public String signature;

    private String fullAssetID;


    /**
     * DvpPayItem().
     * <p>
     * Constructor, using numeric amount.
     * </p>
     *
     * @param address      :
     * @param namespace    :
     * @param classID      :
     * @param amountNumber :
     * @param publicKey    :
     * @param signature    :
     * @param issuance     :
     * @param metadata     :
     * @param encumbrance  : 'null' for Python compatability. "" for not used.
     */
    @SuppressWarnings("squid:S00107") // Parameter Count.
    public DvpPayItem(
        String address,
        String namespace,
        String classID,
        Number amountNumber,
        String publicKey,
        String signature,
        boolean issuance,
        String metadata,
        String encumbrance
    ) {

      this.address = address;
      this.namespace = (namespace == null ? "" : namespace);
      this.classID = (classID == null ? "" : classID);
      this.amountNumber = new Balance(amountNumber);
      this.amountString = null;
      this.publicKey = publicKey;
      this.signature = signature;
      this.issuance = issuance;
      this.metadata = (metadata == null ? "" : metadata);
      this.encumbrance = encumbrance;

    }


    /**
     * DvpPayItem().
     * <p>
     * Constructor, using string amount.
     * </p>
     *
     * @param address      :
     * @param namespace    :
     * @param classID      :
     * @param amountString :
     * @param publicKey    :
     * @param signature    :
     * @param issuance     :
     * @param metadata     :
     * @param encumbrance  : 'null' for Python compatability. "" for not used.
     */
    @SuppressWarnings("squid:S00107") // Params > 7
    public DvpPayItem(
        String address,
        String namespace,
        String classID,
        String amountString,
        String publicKey,
        String signature,
        boolean issuance,
        String metadata,
        String encumbrance
    ) {

      this.address = address;
      this.namespace = (namespace == null ? "" : namespace);
      this.classID = (classID == null ? "" : classID);
      setAmount(amountString);
      this.publicKey = publicKey;
      this.signature = signature;
      this.issuance = issuance;
      this.metadata = (metadata == null ? "" : metadata);
      this.encumbrance = encumbrance;
    }


    /**
     * DvpPayItem().
     * <p>Constructor, from ObjectArray.</p>
     * <p>
     * address      :
     * namespace    :
     * classID  :
     * amount       :
     * publicKey    :
     * signature    :
     * issuance     :
     * metadata     :
     * encumbrance  :
     * </p>
     *
     * @param partyData :
     */
    public DvpPayItem(Object[] partyData) {

      this(new MPWrappedArrayImpl(partyData));
    }


    /**
     * DvpPayItem().
     * <p>Constructor, from MPWrappedArray.</p>
     * <p>
     * address      :
     * namespace    :
     * classID      :
     * amount       :
     * publicKey    :
     * signature    :
     * issuance     :
     * metadata     :
     * encumbrance  :
     * </p>
     *
     * @param itemData :
     */
    public DvpPayItem(MPWrappedArray itemData) {

      int dLen = (itemData != null ? itemData.size() : 0);

      address = (dLen > 0 ? itemData.asString(0) : "");

      if (dLen > 1) {
        namespace = (itemData.asString(1) == null ? "" : itemData.asString(1));
      } else {
        namespace = "";
      }

      if (dLen > 2) {
        classID = (itemData.asString(2) == null ? "" : itemData.asString(2));
      } else {
        classID = "";
      }

      if (dLen > 3) {
        if (itemData.get(3) instanceof Number) {
          amountNumber = new Balance(itemData.get(3));
          amountString = null;
        } else {
          setAmount(itemData.asString(3));
        }
      } else {
        amountString = null;
        amountNumber = BALANCE_ZERO;
      }

      publicKey = (dLen > 4 ? itemData.asString(4) : "");
      signature = (dLen > 5 ? itemData.asString(5) : "");
      issuance = (dLen > 6 ? itemData.asBoolean(6) : false);
      metadata = (dLen > 7 ? itemData.asString(7) : "");
      encumbrance = (dLen > 8 ? itemData.asString(8) : null);

    }


    /**
     * DvpPayItem(), Copy constructor.
     *
     * @param toCopy :
     */
    public DvpPayItem(DvpPayItem toCopy) {

      address = toCopy.address;
      namespace = toCopy.namespace;
      classID = toCopy.classID;
      fullAssetID = toCopy.fullAssetID;
      amountNumber = toCopy.amountNumber;
      amountString = toCopy.amountString;
      publicKey = toCopy.publicKey;
      signature = toCopy.signature;
      issuance = toCopy.issuance;
      metadata = toCopy.metadata;
      encumbrance = toCopy.encumbrance;

    }


    /**
     * compareTo.
     * <p>Comparable implemented for the express purpose of sorting payments in the DvP Events code.</p>
     *
     * @param compareItem :
     *
     * @return :
     */
    @Override
    public int compareTo(DvpPayItem compareItem) {
      // paymentAddress, thisNS, thisClass, paymentQuantity, thisSigned, thisIsIssuance, thisMetadata

      int rVal;

      if (compareItem == null) {
        return 1;
      }

      // Address
      if (this.address == null) {
        if (compareItem.address != null) {
          return -1;
        }
      } else {
        if (compareItem.address == null) {
          return 1;
        }

        rVal = this.address.compareTo(compareItem.address);
        if (rVal != 0) {
          return rVal;
        }
      }

      // namespace
      if (this.namespace == null) {
        if (compareItem.namespace != null) {
          return -1;
        }
      } else {
        if (compareItem.namespace == null) {
          return 1;
        }

        rVal = this.namespace.compareTo(compareItem.namespace);
        if (rVal != 0) {
          return rVal;
        }
      }

      // classID
      if (this.classID == null) {
        if (compareItem.classID != null) {
          return -1;
        }
      } else {
        if (compareItem.classID == null) {
          return 1;
        }

        rVal = this.classID.compareTo(compareItem.classID);
        if (rVal != 0) {
          return rVal;
        }
      }

      // amountNumber
      if (this.amountNumber == null) {
        if (compareItem.amountNumber != null) {
          return -1;
        }
      } else {
        if (compareItem.amountNumber == null) {
          return 1;
        }

        rVal = this.amountNumber.compareTo(compareItem.amountNumber);
        if (rVal != 0) {
          return rVal;
        }
      }

      // isSigned
      if (this.isSigned() != compareItem.isSigned()) {
        if (this.isSigned()) {
          return 1;
        }
        return -1;
      }

      // issuance
      if (this.issuance != compareItem.issuance) {
        if (this.issuance) {
          return 1;
        }
        return -1;
      }

      // metadata
      if (this.metadata == null) {
        if (compareItem.metadata != null) {
          return -1;
        }
      } else {
        if (compareItem.metadata == null) {
          return 1;
        }

        rVal = this.metadata.compareTo(compareItem.metadata);
        if (rVal != 0) {
          return rVal;
        }
      }

      // publicKey
      if (this.publicKey == null) {
        if (compareItem.publicKey != null) {
          return -1;
        }
      } else {
        if (compareItem.publicKey == null) {
          return 1;
        }

        rVal = this.publicKey.compareTo(compareItem.publicKey);
        if (rVal != 0) {
          return rVal;
        }
      }

      return 0;
    }


    @Override
    public Object[] encode(long unused) {

      if (this.encumbrance == null) {

        return new Object[]{
            this.address,
            this.namespace,
            this.classID,
            (this.amountNumber != null ? this.amountNumber.getValue() : this.amountString),
            this.publicKey,
            this.signature,
            this.issuance,
            this.metadata
        };

      } else {
        return new Object[]{
            this.address,
            this.namespace,
            this.classID,
            (this.amountNumber != null ? this.amountNumber.getValue() : this.amountString),
            this.publicKey,
            this.signature,
            this.issuance,
            this.metadata,
            this.encumbrance
        };

      }

    }


    /**
     * encodeJson.
     * <p>Return DVP PayItem data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(ADDRESS, this.address);
      rVal.put(NAMESPACE, this.namespace);
      rVal.put(CLASSID, this.classID);
      rVal.put(AMOUNT, (this.amountNumber != null ? this.amountNumber.getValue() : this.amountString));
      rVal.put(PUBLIC_KEY, this.publicKey);
      rVal.put(SIGNATURE, this.signature);
      rVal.put(ISSUANCE, this.issuance);
      rVal.put(METADATA, this.metadata);

      if (this.encumbrance != null) {
        rVal.put(ENCUMBRANCE, this.encumbrance);
      }

      return rVal;
    }


    @Override
    @SuppressWarnings("squid:S1126") // Return of boolean expressions should not be wrapped into an "if-then-else" statement
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpPayItem)) {
        return false;
      }

      DvpPayItem theOther = (DvpPayItem) toCompare;

      if (!this.address.equals(theOther.address)) {
        return false;
      }

      if (!this.namespace.equals(theOther.namespace)) {
        return false;
      }

      if (!this.classID.equals(theOther.classID)) {
        return false;
      }

      if (this.amountNumber != null) {
        if (!this.amountNumber.equalTo(theOther.amountNumber)) {
          return false;
        }
      } else {
        if (!this.amountString.equals(theOther.amountString)) {
          return false;
        }
      }

      if (this.issuance != theOther.issuance) {
        return false;
      }

      if (!this.publicKey.equals(theOther.publicKey)) {
        return false;
      }

      if (!this.metadata.equals(theOther.metadata)) {
        return false;
      }

      if (!this.signature.equals(theOther.signature)) {
        return false;
      }

      if (!Objects.equals(this.encumbrance, theOther.encumbrance)) {
        return false;
      }

      return true;
    }


    public Object getAmount() {

      return (amountNumber == null ? amountString : amountNumber.getValue());
    }


    /**
     * getFullAssetID().
     * <p>Return 'full' asset ID for thid Pay Item, the Asset ID is cached for minor performance reasons.</p>
     *
     * @return :
     */
    public String getFullAssetID() {

      if (fullAssetID == null) {
        fullAssetID = namespace + "|" + classID;
      }

      return fullAssetID;
    }


    @Override
    public int hashCode() {

      return this.classID.hashCode();

    }


    /**
     * isSigned().
     * <p>Return 'true' if this PayItem appears to have been signed.
     * Note that signature validation occurs in the 'updateState' function.
     * Thereafter, if a signature is present it is assumed to be good.
     * SIGNATURES ARE NOT RE-CHECKED. </p>
     *
     * @return :
     */
    public boolean isSigned() {
      // Utility function used in DvP Event code.
      return ((publicKey != null) && (!publicKey.isEmpty()) && (signature != null) && (!signature.isEmpty()));

    }


    public Object[] objectToHashToSign(String contractAddress) {

      return new Object[]{contractAddress, namespace, classID, getAmount()};
    }


    private void setAmount(String text) {
      if (((text == null) || (text.isEmpty()))) {
        amountNumber = BALANCE_ZERO;
        amountString = null;
        return;
      }

      for (int i = text.length() - 1; i >= 0; i--) {
        char ch = text.charAt(i);
        if ((ch < '0') || ('9' < ch)) {
          // definitely not a number
          amountNumber = null;
          amountString = text;
          return;
        }
      }

      try {
        // All numeric, so should be a number.
        amountNumber = new Balance(text);
        amountString = null;
      } catch (NumberFormatException e) {
        // Must be too large to fit in a Long. Any maths we try to do with this will require BigIntegers.
        amountNumber = null;
        amountString = text;
      }
    }

  }



  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class DvpReceiveItem implements ToObjectArray {

    public final String classID;

    public final String namespace;

    public String address;

    public Balance amountNumber;

    public String amountString;

    private String fullAssetID;


    /**
     * DvpReceiveItem().
     * <p>
     * Constructor, using numeric amount.
     * </p>
     *
     * @param address      :
     * @param namespace    :
     * @param classID      :
     * @param amountNumber :
     */
    public DvpReceiveItem(
        String address,
        String namespace,
        String classID,
        Number amountNumber

    ) {

      this.address = address;
      this.namespace = (namespace == null ? "" : namespace);
      this.classID = (classID == null ? "" : classID);
      this.amountNumber = (amountNumber == null ? BALANCE_ZERO : new Balance(amountNumber));
      this.amountString = null;
    }


    /**
     * DvpReceiveItem().
     * <p>
     * Constructor, using String amount (When specifying parameter names or furmulae).
     * </p>
     *
     * @param address      :
     * @param namespace    :
     * @param classID      :
     * @param amountString :
     */
    public DvpReceiveItem(
        String address,
        String namespace,
        String classID,
        String amountString

    ) {
      this.address = address;
      this.namespace = (namespace == null ? "" : namespace);
      this.classID = (classID == null ? "" : classID);
      setAmount(amountString);
    }


    public DvpReceiveItem(Object[] partyData) {

      this(new MPWrappedArrayImpl(partyData));
    }


    /**
     * DvpReceiveItem().
     *
     * @param partyData :
     */
    @SuppressWarnings("squid:S2259") // NullPointerCouldBeThrown. Code confuses sonarqube.
    public DvpReceiveItem(MPWrappedArray partyData) {

      int dLen = (partyData != null ? partyData.size() : 0);

      address = (dLen > 0 ? partyData.asString(0) : "");
      namespace = (dLen > 1 ? partyData.asString(1) : "");
      classID = (dLen > 2 ? partyData.asString(2) : "");

      if (dLen > 3) {
        if (partyData.get(3) instanceof Number) {
          amountNumber = new Balance(partyData.get(3));
          amountString = null;
        } else {
          setAmount(partyData.asString(3));
        }
      } else {
        amountNumber = BALANCE_ZERO;
        amountString = null;
      }
    }


    /**
     * DvpReceiveItem().
     * <p>Copy Constructor.
     * </p>
     *
     * @param toCopy :
     */
    public DvpReceiveItem(DvpReceiveItem toCopy) {

      address = toCopy.address;
      namespace = toCopy.namespace;
      classID = toCopy.classID;
      amountNumber = toCopy.amountNumber;
      amountString = toCopy.amountString;

    }


    @Override
    public Object[] encode(long unused) {

      return new Object[]{
          this.address,
          this.namespace,
          this.classID,
          (this.amountNumber != null ? this.amountNumber.getValue() : this.amountString)
      };

    }


    /**
     * encodeJson.
     * <p>Return DVP ReceiveItem data as JSONObject</p>
     *
     * @return :
     */
    public JSONObject encodeJson() {

      JSONObject rVal = new JSONObject(true);

      rVal.put(ADDRESS, this.address);
      rVal.put(NAMESPACE, this.namespace);
      rVal.put(CLASSID, this.classID);
      rVal.put(AMOUNT, (this.amountNumber != null ? this.amountNumber.getValue() : this.amountString));

      return rVal;
    }


    @Override
    public boolean equals(Object toCompare) {

      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof DvpReceiveItem)) {
        return false;
      }

      DvpReceiveItem theOther = (DvpReceiveItem) toCompare;

      if (!this.address.equals(theOther.address)) {
        return false;
      }

      if (!this.namespace.equals(theOther.namespace)) {
        return false;
      }

      if (!this.classID.equals(theOther.classID)) {
        return false;
      }

      if (this.amountNumber != null) {
        return this.amountNumber.equalTo(theOther.amountNumber);
      } else {
        return this.amountString.equals(theOther.amountString);
      }
    }


    public Object getAmount() {

      return (amountNumber == null ? amountString : amountNumber.getValue());
    }


    /**
     * getFullAssetID().
     * <p>Return 'full' asset id for this Receive Item.</p>
     *
     * @return :
     */
    public String getFullAssetID() {

      if (fullAssetID == null) {
        fullAssetID = namespace + "|" + classID;
      }

      return fullAssetID;
    }


    @Override
    public int hashCode() {

      return (this.address.hashCode() + this.getFullAssetID().hashCode()) % Integer.MAX_VALUE;

    }


    private void setAmount(String text) {
      if ((text == null) || (text.isEmpty())) {
        amountNumber = BALANCE_ZERO;
        amountString = null;
        return;
      }

      for (int i = text.length() - 1; i >= 0; i--) {
        char ch = text.charAt(i);
        if ((ch < '0') || ('9' < ch)) {
          // definitely not a number
          amountNumber = null;
          amountString = text;
          return;
        }
      }

      try {
        // All numeric, so should be a number.
        amountNumber = new Balance(text);
        amountString = null;
      } catch (NumberFormatException e) {
        // Must be too large to fit in a Long. Any maths we try to do with this will require BigIntegers.
        amountNumber = null;
        amountString = text;
      }
    }

  }



  private List<DvpAddEncumbrance> addencumbrances;

  private List<DvpAuthorisation> authorisations;

  private boolean autosign = true;

  private long canceltime;

  private int completed;

  private String contractAddress;

  private String contractFunction = CONTRACT_NAME_DVP_UK;

  private DvpEncumbrance encumbrance;

  private DvpEvent events;

  private Long expiry;

  private String issuingaddress;

  private String metadata;

  private Map<String, DvpParameter> parameters;

  private List<DvpParty> parties;

  private String protocol;

  private Long startdate;

  private String status;

  private long thisDvpDeleteDelayOnComplete = CLASS_DELETE_DELAY_ON_COMPLETE;

  private long timeevent;


  /**
   * private default constructor.
   */
  private DvpUkContractData() {

    contractAddress = "";
    completed = 0;
    autosign = true;
    contractFunction = "";
    timeevent = 0L;
    canceltime = 0L;
    startdate = 0L;
    expiry = 0L;
    issuingaddress = "";
    status = "";
    protocol = "";
    metadata = "";

  }


  /**
   * DvpUkContractData.
   * <p>DvpUkContractData Copy constructor.</p>
   *
   * @param toCopy :
   */
  public DvpUkContractData(DvpUkContractData toCopy) {

    if (toCopy == null) {
      return;
    }

    timeevent = toCopy.get__timeevent();
    startdate = toCopy.getStartdate();
    expiry = toCopy.getExpiry();
    completed = toCopy.get__completed();
    contractAddress = toCopy.get__address();
    contractFunction = toCopy.get__function();

    issuingaddress = toCopy.getIssuingaddress();
    protocol = toCopy.getProtocol();
    metadata = toCopy.getMetadata();
    thisDvpDeleteDelayOnComplete = toCopy.thisDvpDeleteDelayOnComplete;
    status = "";
    autosign = toCopy.autosign;

    if (toCopy.getParties() != null) {
      parties = new ArrayList<>();
      toCopy.getParties().forEach(thisParty -> parties.add(new DvpParty(thisParty)));
    }

    if (toCopy.getAuthorisations() != null) {
      authorisations = new ArrayList<>();
      toCopy.getAuthorisations().forEach(thisAuthorisation -> authorisations.add(new DvpAuthorisation(thisAuthorisation)));
    }

    if (toCopy.getAddencumbrances() != null) {
      addencumbrances = new ArrayList<>();
      toCopy.getAddencumbrances().forEach(thisAddencumbrance -> addencumbrances.add(new DvpAddEncumbrance(thisAddencumbrance)));
    }

    if (toCopy.getEvents() != null) {
      events = new DvpEvent(toCopy.getEvents());
    }

    if (toCopy.getParameters() != null) {
      parameters = new HashMap<>();
      toCopy.getParameters().forEach((key, value) ->
          parameters.put(key, new DvpParameter(value))
      );
    }

    if (toCopy.getEncumbrance() != null) {
      encumbrance = new DvpEncumbrance(toCopy.getEncumbrance());
    }

  }


  /**
   * Constructor.
   * <p>
   * Create new DvpContract Data Model based on the given parameters.
   * Note that Parties, Authorisations are intentionally left as null.
   * </p>
   *
   * @param contractAddress  :
   * @param isCompleted      :
   * @param contractFunction :
   * @param nextTimeEvent    :
   * @param startDate        :
   * @param expiryDate       :
   * @param issuingAddress   :
   * @param protocol         :
   * @param metadata         :
   * @param events           :
   * @param encumbranceToUse :
   */
  @SuppressWarnings("squid:S00107") // Params > 7
  public DvpUkContractData(
      String contractAddress,
      int isCompleted,
      String contractFunction,
      long nextTimeEvent,
      long startDate,
      long expiryDate,
      String issuingAddress,
      String protocol,
      String metadata,
      String[] events,
      String encumbranceToUse
  ) {

    this.timeevent = nextTimeEvent;
    this.startdate = startDate;
    this.expiry = expiryDate;
    this.completed = isCompleted;
    this.contractAddress = contractAddress;
    this.contractFunction = contractFunction;
    this.issuingaddress = issuingAddress;
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.events = new DvpEvent(events);

    if (encumbranceToUse == null) {
      this.encumbrance = null;
    } else {
      this.encumbrance = new DvpEncumbrance(true, encumbranceToUse);
    }
  }


  /**
   * DvpUkContractData().
   * <p>
   * Constructor : Create DvpUK Data model from the given [String, Object] Map.
   * </p>
   *
   * @param sourceMap :
   */
  public DvpUkContractData(Map<String, Object> sourceMap) {

    decodeMap(sourceMap);
  }


  /**
   * DvpUkContractData().
   * <p>
   * Constructor : Create DvpUK Data model from the given MPWrappedMap[String, Object] object.
   * </p>
   *
   * @param asMapValue :
   */
  @SuppressWarnings("squid:S1188") // 'Lambdas and anonymous classes should not have too many lines of code' : Intentional.
  public DvpUkContractData(MPWrappedMap<String, Object> asMapValue) {
    /*
    Used a Switch here rather than create a temporary map. Hopefully faster.
    
     */

    this();
    final boolean[] seenFunction = {false};
    asMapValue.iterate((k, v) -> {
      switch (k.toLowerCase()) {

        case __STATUS:
          status = objectToString(v);
          break;

        case __ADDRESS:
          contractAddress = objectToString(v);
          break;

        case __FUNCTION:
          // __function has precedence over contractfunction
          contractFunction = objectToString(v);
          seenFunction[0] = true;
          break;

        case CONTRACT_FUNCTION:
          if (!seenFunction[0]) {
            contractFunction = objectToString(v);
          }
          break;

        case __COMPLETED:
          completed = (v == null ? 0 : ((Number) v).intValue());
          break;

        case __TIMEEVENT:
          timeevent = objectToLong(v, 0L);
          break;

        case __CANCELTIME:
          canceltime = objectToLong(v, 0L);
          break;

        case START_DATE:
          startdate = objectToLong(v, 0L);
          break;

        case EXPIRY:
          expiry = objectToLong(v, 0L);
          break;

        case ISSUING_ADDRESS:
          issuingaddress = objectToString(v);
          break;

        case DELAY_ON_COMPLETE:
          thisDvpDeleteDelayOnComplete = (v == null ? CLASS_DELETE_DELAY_ON_COMPLETE : ((Number) v).longValue());
          break;

        case AUTO_SIGN:
          setAutosign(TypeSafeMap.asBoolean(v));
          break;

        case PROTOCOL:
          setProtocol(v);
          break;

        case METADATA:
          setMetadata(v);
          break;

        case PARTIES:
          setParties(v);
          break;

        case AUTHORISATIONS:
          setAuthorisations(v);
          break;

        case PARAMETERS:
          setParameters(v);
          break;

        case ADD_ENCUMBRANCES:
          setAddencumbrances(v);
          break;

        case EVENTS:
          setEvents(v);
          break;

        case ENCUMBRANCE:
          setEncumbrance(v);
          break;

        default:
          break;
      }


    });
  }


  /**
   * addAddEncumbrance().
   * <p>Add individual AddEncumbrance instance to this Contract. Initialises addencumbrances Array if necessary.</p>
   *
   * @param item :
   */
  public void addAddEncumbrance(DvpAddEncumbrance item) {

    if (this.addencumbrances == null) {
      this.addencumbrances = new ArrayList<>();
    }
    this.addencumbrances.add(item);
  }


  /**
   * addAuthorisation().
   * <p>Add individual authorisation instance to this Contract. Initialises authorisations Array if necessary.</p>
   *
   * @param item :
   */
  public void addAuthorisation(DvpAuthorisation item) {

    if (this.authorisations == null) {
      this.authorisations = new ArrayList<>();
    }
    this.authorisations.add(item);
  }


  /**
   * addParameter().
   * <p>Add individual Parameter instance to this Contract. Initialises parameters Array if necessary.</p>
   *
   * @param item :
   */
  public void addParameter(String key, DvpParameter item) {

    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, item);

  }


  /**
   * addParty().
   * <p>Add individual party instance to this Contract. Initialises parties Array if necessary.</p>
   *
   * @param party :
   */
  public void addParty(DvpParty party) {

    if (this.parties == null) {
      this.parties = new ArrayList<>();
    }
    this.parties.add(party);

  }


  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    if (parties != null) {
      for (DvpParty thisitem : parties) {
        if ((thisitem.sigAddress != null) && (thisitem.sigAddress.length() > 0)) {
          if (AddressUtil.verifyAddress(thisitem.sigAddress)) {
            rVal.add(thisitem.sigAddress);
          }
        }
      }
    }

    if (authorisations != null) {
      for (DvpAuthorisation thisitem : authorisations) {
        if ((thisitem.getAddress() != null) && (thisitem.getAddress().length() > 0)) {
          if (AddressUtil.verifyAddress(thisitem.getAddress())) {
            rVal.add(thisitem.getAddress());
          } else if (AddressUtil.verifyPublicKey(thisitem.getAddress())) {
            rVal.add(AddressUtil.publicKeyToAddress(thisitem.getAddress(), AddressType.NORMAL));
          }
        }
      }
    }

    if (addencumbrances != null) {
      for (DvpAddEncumbrance thisitem : addencumbrances) {
        if ((thisitem.getPublicKey() != null) && (thisitem.getPublicKey().length() > 0)) {
          if (AddressUtil.verifyAddress(thisitem.getPublicKey())) {
            rVal.add(thisitem.getPublicKey());
          } else if (AddressUtil.verifyPublicKey(thisitem.getPublicKey())) {
            rVal.add(AddressUtil.publicKeyToAddress(thisitem.getPublicKey(), AddressType.NORMAL));
          }
        }
      }
    }

    if (parameters != null) {
      for (DvpParameter thisitem : parameters.values()) {
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


  /**
   * copy function, because Interfaces do not support constructors.
   *
   * @return :
   */
  @Override
  public IContractData copy() {

    return new DvpUkContractData(this);
  }


  private List<DvpAddEncumbrance> decodeAddEncumbrances(Object[] addEncumbrancesData) {

    return decodeAddEncumbrances(new MPWrappedArrayImpl(addEncumbrancesData));
  }


  private List<DvpAddEncumbrance> decodeAddEncumbrances(MPWrappedArray addEncumbrancesData) {

    if (addEncumbrancesData == null) {
      return null;
    }

    ArrayList<DvpAddEncumbrance> theseAddEncumbrances = new ArrayList<>();

    for (int index = 0, l = addEncumbrancesData.size(); index < l; index++) {
      theseAddEncumbrances.add(new DvpAddEncumbrance(addEncumbrancesData.asWrapped(index)));
    }

    return theseAddEncumbrances;

  }


  private List<DvpAuthorisation> decodeAuthorisations(Object[] authorisations) {

    return decodeAuthorisations(new MPWrappedArrayImpl(authorisations));
  }


  private List<DvpAuthorisation> decodeAuthorisations(MPWrappedArray authorisationsData) {

    if (authorisationsData == null) {
      return null;
    }

    ArrayList<DvpAuthorisation> theseAuthorisations = new ArrayList<>();

    for (int index = 0, l = authorisationsData.size(); index < l; index++) {
      theseAuthorisations.add(new DvpAuthorisation(authorisationsData.asWrapped(index)));
    }

    return theseAuthorisations;
  }


  /**
   * decodeMap().
   * <p>
   * Build DvpUK Object map from the given Map parameter.
   * For format, see class header.
   * </p>
   *
   * @param sourceMap :
   */
  private void decodeMap(Map<String, Object> sourceMap) {
    // Must relate to DvpUkContractData(MPWrappedMap<String, Object> asMapValue)
    status = (String) sourceMap.getOrDefault(__STATUS, "");
    contractAddress = (String) sourceMap.getOrDefault(__ADDRESS, "");
    contractFunction = (String) sourceMap.getOrDefault(__FUNCTION, sourceMap.getOrDefault(CONTRACT_FUNCTION, ""));
    completed = ((Number) sourceMap.getOrDefault(__COMPLETED, 0)).intValue();
    timeevent = ((Number) sourceMap.getOrDefault(__TIMEEVENT, 0L)).longValue();
    canceltime = ((Number) sourceMap.getOrDefault(__CANCELTIME, 0L)).longValue();

    startdate = ((Number) sourceMap.getOrDefault(START_DATE, 0L)).longValue();
    expiry = ((Number) sourceMap.getOrDefault(EXPIRY, 0L)).longValue();
    issuingaddress = (String) sourceMap.getOrDefault(ISSUING_ADDRESS, "");
    thisDvpDeleteDelayOnComplete = ((Number) sourceMap.getOrDefault(DELAY_ON_COMPLETE, CLASS_DELETE_DELAY_ON_COMPLETE)).longValue();
    autosign = TypeSafeMap.asBoolean(sourceMap.getOrDefault(AUTO_SIGN, Boolean.TRUE));

    setProtocol(sourceMap.get(PROTOCOL));
    setMetadata(sourceMap.get(METADATA));
    setParties(sourceMap.get(PARTIES));
    setAuthorisations(sourceMap.get(AUTHORISATIONS));
    setParameters(sourceMap.get(PARAMETERS));
    setAddencumbrances(sourceMap.get(ADD_ENCUMBRANCES));
    setEvents(sourceMap.get(EVENTS));
    setEncumbrance(sourceMap.get(ENCUMBRANCE));

  }


  private DvpParameter decodeParameter(Object value) {

    if (value == null) {
      return null;
    }
    if (value instanceof DvpParameter) {
      return new DvpParameter((DvpParameter) value);
    }
    if (value instanceof MPWrappedArray) {
      return new DvpParameter((MPWrappedArray) value);
    }
    if (value instanceof Object[]) {
      return new DvpParameter((Object[]) value);
    }
    if (value instanceof List) {
      return new DvpParameter(((List<?>) value).toArray());
    }
    throw new IllegalArgumentException("Cannot convert value of class " + value.getClass() + " to DvpParameter");
  }


  private Map<String, DvpParameter> decodeParameters(Object[] parametersData) {

    return decodeParameters(new MPWrappedMap<>(parametersData));
  }


  private Map<String, DvpParameter> decodeParameters(Map<String, Object> parametersData) {

    if (parametersData == null) {
      return null;
    }

    Map<String, DvpParameter> theseParameters = new HashMap<>();
    parametersData.forEach((key, value) -> {
      if (value != null) {
        theseParameters.put(key, decodeParameter(value));
      }
    });

    return theseParameters;
  }


  private Map<String, DvpParameter> decodeParameters(MPWrappedMap<String, Object> parametersData) {

    if (parametersData == null) {
      return null;
    }

    Map<String, DvpParameter> theseParameters = new HashMap<>();
    parametersData.iterate((key, value) -> theseParameters.put(key, decodeParameter(value)));
    return theseParameters;
  }


  private List<DvpParty> decodeParties(Object[] parties) {

    return decodeParties(new MPWrappedArrayImpl(parties));
  }


  private List<DvpParty> decodeParties(MPWrappedArray partiesData) {

    if ((partiesData == null) || (partiesData.isEmpty())) {
      return null;
    }

    int partyCount = partiesData.asInt(0);

    if (partiesData.size() != (partyCount + 1)) {
      throw new RuntimeException("Bad Contract Parties list. (Size does not match.)");
    }

    ArrayList<DvpParty> theseParties = new ArrayList<>();

    for (int index = 1, l = partiesData.size(); index < l; index++) {
      theseParties.add(new DvpParty(partiesData.get(index)));
    }

    return theseParties;

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


  private Object[] encodeAddEncumbrances() {

    if (this.addencumbrances == null) {
      return null;
    }

    Object[] rVal = new Object[this.addencumbrances.size()];

    final int[] index = {0};

    this.addencumbrances.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;
  }


  /**
   * encodeAddEncumbrancesJson.
   * <p>Return DVP AddEncumbrances data as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeAddEncumbrancesJson() {

    JSONArray rVal = new JSONArray();

    this.addencumbrances.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;
  }


  private Object[] encodeAuthorisations() {

    if (this.authorisations == null) {
      return null;
    }

    Object[] rVal = new Object[this.authorisations.size()];

    final int[] index = {0};

    this.authorisations.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;
  }


  /**
   * encodeAuthorisationsJson.
   * <p>Return DVP Authorisations data as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeAuthorisationsJson() {

    JSONArray rVal = new JSONArray();

    this.authorisations.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;
  }


  @Nullable
  private Object[] encodeEncumbrance() {

    if (this.encumbrance == null) {
      return null;
    }

    return this.encumbrance.encode(0);
  }


  /**
   * encodeEncumbranceJson.
   * <p>Return DVP Encumbrance data as JSONArray</p>
   *
   * @return :
   */
  public JSONObject encodeEncumbranceJson() {

    if (this.encumbrance != null) {
      return this.encumbrance.encodeJson();
    }

    return new JSONObject(true);

  }


  private Object[] encodeEvents() {

    if (this.events == null) {
      return null;
    }

    return this.events.encode(0);
  }


  /**
   * encodeEventsJson.
   * <p>Return DVP Events as JSONArray</p>
   *
   * @return :
   */
  public JSONArray encodeEventsJson() {

    return this.events.encodeJson();

  }


  private Map<String, Object> encodeParameters() {

    if (this.parameters == null) {
      return null;
    }

    Map<String, Object> rVal = new TreeMap<>();

    this.parameters.forEach((key, value) -> rVal.put(key, value.encode(0)));

    return rVal;
  }


  /**
   * encodeParametersJson.
   * <p>Return DVP Parameters data as JSONObject</p>
   *
   * @return :
   */
  public JSONObject encodeParametersJson() {

    JSONObject rVal = new JSONObject(true);

    if (this.parameters != null) {

      this.parameters.forEach((key, value) -> rVal.put(key, value.encodeJson()));

    }

    return rVal;
  }


  private Object[] encodeParties() {

    if (this.parties == null) {
      return null;
    }

    Object[] rVal = new Object[this.parties.size() + 1];

    rVal[0] = this.parties.size();

    final int[] index = {1};

    this.parties.forEach(detail -> rVal[index[0]++] = detail.encode(0));

    return rVal;
  }


  private JSONArray encodePartiesJson() {

    JSONArray rVal = new JSONArray();

    if (this.parties == null) {
      return rVal;
    }

    this.parties.forEach(detail -> rVal.add(detail.encodeJson()));

    return rVal;

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

    rVal.put(__FUNCTION, contractFunction);
    rVal.put(__COMPLETED, completed);
    rVal.put(__ADDRESS, contractAddress);
    rVal.put(__TIMEEVENT, timeevent);

    if (canceltime > 0L) {
      rVal.put(__CANCELTIME, canceltime);
    }

    rVal.put(ISSUING_ADDRESS, issuingaddress);
    rVal.put(START_DATE, startdate);
    rVal.put(EXPIRY, expiry);

    if (CLASS_DELETE_DELAY_ON_COMPLETE != thisDvpDeleteDelayOnComplete) {
      rVal.put(DELAY_ON_COMPLETE, thisDvpDeleteDelayOnComplete);
    }

    if ((status != null) && (status.length() > 0)) {
      rVal.put(__STATUS, status);
    }

    if (protocol != null) {
      rVal.put(PROTOCOL, protocol);
    }

    if (metadata != null) {
      rVal.put(METADATA, metadata);
    }

    if (!autosign) {
      rVal.put(AUTO_SIGN, false);
    }

    if (this.parties != null) {
      rVal.put(PARTIES, encodeParties());
    }

    if (this.authorisations != null) {
      rVal.put(AUTHORISATIONS, encodeAuthorisations());
    }

    if (this.parameters != null) {
      rVal.put(PARAMETERS, encodeParameters());
    }

    if (this.addencumbrances != null) {
      rVal.put(ADD_ENCUMBRANCES, encodeAddEncumbrances());
    }

    if (this.events != null) {
      rVal.put(EVENTS, encodeEvents());
    }

    if (this.encumbrance != null) {
      rVal.put(ENCUMBRANCE, encodeEncumbrance());
    }

    return rVal;
  }


  /**
   * encodeToMapForTxParameter.
   * <p>Encode Contract data structure as a map, but omit some internal fields that do not need to appear in the transaction data.</p>
   * <p>The purpose of this is to allow the use of the DvPContractData classes for the construction of a New Contract.</p>
   *
   * @return :
   */
  @Override
  public Map<String, Object> encodeToMapForTxParameter() {

    final String thisContractFunction = CONTRACT_FUNCTION; // Sonarlint smell fix.

    Map<String, Object> rVal = encodeToMap();

    if (!rVal.containsKey(__FUNCTION) && (rVal.containsKey(thisContractFunction))) {
      rVal.put(__FUNCTION, rVal.get(thisContractFunction));
    }

    // Strip out defined keys...
    for (String keyName : Arrays.asList(thisContractFunction, __COMPLETED, __ADDRESS, __TIMEEVENT)) {
      rVal.remove(keyName);
    }

    return rVal;

  }


  @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
  @Override
  public boolean equals(Object toCompare) {

    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof DvpUkContractData)) {
      return false;
    }

    DvpUkContractData theOther = (DvpUkContractData) toCompare;

    if ((!Objects.equals(contractFunction, theOther.get__function())) && ((contractFunction == null) || (!contractFunction
        .equalsIgnoreCase(theOther.get__function())))) {
      return false;
    }

    if (completed != theOther.get__completed()) {
      return false;
    }

    if (autosign != theOther.getAutosign()) {
      return false;
    }

    if ((!Objects.equals(contractAddress, theOther.get__address())) && ((contractAddress == null) || (!contractAddress
        .equalsIgnoreCase(theOther.get__address())))) {
      return false;
    }

    if (timeevent != theOther.get__timeevent()) {
      return false;
    }

    if ((!Objects.equals(issuingaddress, theOther.getIssuingaddress())) && ((issuingaddress == null) || (!issuingaddress
        .equalsIgnoreCase(theOther.getIssuingaddress())))) {
      return false;
    }

    if ((!Objects.equals(startdate, theOther.getStartdate())) && ((startdate == null) || (!startdate.equals(theOther.getStartdate())))) {
      return false;
    }

    if ((!Objects.equals(expiry, theOther.getExpiry())) && ((expiry == null) || (!expiry.equals(theOther.getExpiry())))) {
      return false;
    }

    if ((!Objects.equals(protocol, theOther.getProtocol())) && ((protocol == null) || (!protocol.equalsIgnoreCase(theOther.getProtocol())))) {
      return false;
    }

    if ((!Objects.equals(metadata, theOther.getMetadata())) && ((metadata == null) || (!metadata.equalsIgnoreCase(theOther.getMetadata())))) {
      return false;
    }

    if ((parties != theOther.getParties()) && ((parties == null) || (!parties.equals(theOther.getParties())))) {
      return false;
    }

    if ((authorisations != theOther.getAuthorisations()) && ((authorisations == null) || (!authorisations.equals(theOther.getAuthorisations())))) {
      return false;
    }

    if ((parameters != theOther.getParameters()) && ((parameters == null) || (!parameters.equals(theOther.getParameters())))) {
      return false;
    }

    if ((addencumbrances != theOther.getAddencumbrances()) && ((addencumbrances == null) || (!addencumbrances.equals(theOther.getAddencumbrances())))) {
      return false;
    }

    if ((events != theOther.getEvents()) && ((events == null) || (!events.equals(theOther.getEvents())))) {
      return false;
    }

    return (encumbrance == theOther.getEncumbrance()) || ((encumbrance != null) && (encumbrance.equals(theOther.getEncumbrance())));
  }


  public List<DvpAddEncumbrance> getAddencumbrances() {

    return addencumbrances;
  }


  public List<DvpAuthorisation> getAuthorisations() {

    return authorisations;
  }


  public boolean getAutosign() {

    return autosign;
  }


  @Override
  public String getContractType() {

    return get__function();
  }


  public long getDeleteDelayOnComplete() {
    return thisDvpDeleteDelayOnComplete;
  }


  public DvpEncumbrance getEncumbrance() {

    return encumbrance;
  }


  public DvpEvent getEvents() {

    return events;
  }


  public Long getExpiry() {

    return expiry;
  }


  public String getIssuingaddress() {

    return issuingaddress;
  }


  public String getMetadata() {

    return metadata;
  }


  public Map<String, DvpParameter> getParameters() {

    return parameters;
  }


  public List<DvpParty> getParties() {

    return parties;
  }


  public String getProtocol() {

    return protocol;
  }


  public Long getStartdate() {

    return startdate;
  }


  public String get__address() {

    return contractAddress;
  }


  public long get__canceltime() {

    return canceltime;
  }


  public int get__completed() {

    return completed;
  }


  @Override
  public String get__function() {

    return contractFunction;
  }


  public String get__status() {

    return status;
  }


  public long get__timeevent() {

    return timeevent;
  }


  @Override
  public int hashCode() {

    return (this.issuingaddress.hashCode() + this.contractFunction.hashCode() + this.contractAddress.hashCode()) % Integer.MAX_VALUE;

  }


  public void setAddencumbrances(List<DvpAddEncumbrance> addencumbrances) {

    this.addencumbrances = addencumbrances;
  }


  private void setAddencumbrances(Object addencumbrances) {

    if (addencumbrances != null) {
      if (addencumbrances instanceof MPWrappedArray) {
        this.addencumbrances = decodeAddEncumbrances((MPWrappedArray) addencumbrances);
      } else if (addencumbrances instanceof Object[]) {
        this.addencumbrances = decodeAddEncumbrances((Object[]) addencumbrances);
      } else if (addencumbrances instanceof List) {
        this.addencumbrances = decodeAddEncumbrances(((List) addencumbrances).toArray());
      }
    } else {
      this.addencumbrances = null;
    }

  }


  private void setAuthorisations(Object authorisations) {

    if (authorisations != null) {
      if (authorisations instanceof MPWrappedArray) {
        this.authorisations = decodeAuthorisations((MPWrappedArray) authorisations);
      } else if (authorisations instanceof Object[]) {
        this.authorisations = decodeAuthorisations((Object[]) authorisations);
      } else if (authorisations instanceof List) {
        this.authorisations = decodeAuthorisations(((List) authorisations).toArray());
      }
    } else {
      this.authorisations = null;
    }

  }


  public void setAuthorisations(List<DvpAuthorisation> authorisations) {

    this.authorisations = authorisations;
  }


  public void setAutosign(boolean autosign) {

    this.autosign = autosign;
  }


  public void setEncumbrance(DvpEncumbrance encumbrance) {

    this.encumbrance = encumbrance;
  }


  private void setEncumbrance(Object encumbrance) {

    if (encumbrance != null) {
      if (encumbrance instanceof MPWrappedArray) {
        this.encumbrance = new DvpEncumbrance((MPWrappedArray) encumbrance);
      } else if (encumbrance instanceof Object[]) {
        this.encumbrance = new DvpEncumbrance((Object[]) encumbrance);
      } else if (encumbrance instanceof List) {
        this.encumbrance = new DvpEncumbrance(((List) encumbrance).toArray());
      }

    } else {
      this.encumbrance = null;
    }

  }


  private void setEvents(Object events) {

    if (events != null) {
      if (events instanceof MPWrappedArray) {
        this.events = new DvpEvent((MPWrappedArray) events);
      } else if (events instanceof Object[]) {
        this.events = new DvpEvent((Object[]) events);
      } else if (events instanceof List) {
        this.events = new DvpEvent(((List) events).toArray());
      }

    } else {
      this.events = null;
    }

  }


  public void setEvents(DvpEvent events) {

    this.events = events;
  }


  public void setIssuingaddress(String issuingaddress) {

    this.issuingaddress = issuingaddress;
  }


  private void setMetadata(Object metadata) {

    if (metadata != null) {
      this.metadata = String.valueOf(metadata);
    } else {
      this.metadata = null;
    }
  }


  @Override
  public long setNextTimeEvent(long updateTime, boolean forceToUpdateTime) {

    if (forceToUpdateTime) {
      set__timeevent(updateTime);
    } else {
      if (this.get__completed() != 0) {
        set__timeevent(Math.min(getExpiry(), updateTime) + thisDvpDeleteDelayOnComplete);
      } else {
        if ((getStartdate() > updateTime) && (getStartdate() < getExpiry())) {
          set__timeevent(getStartdate());
        } else {
          set__timeevent(getExpiry());
        }
      }
    }

    return get__timeevent();
  }


  public void setParameters(Map<String, DvpParameter> parameters) {

    this.parameters = parameters;
  }


  private void setParameters(Object parameters) {

    if (parameters != null) {
      if (parameters instanceof MPWrappedMap) {
        this.parameters = decodeParameters((MPWrappedMap) parameters);
      } else if (parameters instanceof Object[]) {
        this.parameters = decodeParameters((Object[]) parameters);
      } else if (parameters instanceof Map) {
        this.parameters = decodeParameters((Map) parameters);
      } else if (parameters instanceof List) {
        this.parameters = decodeParameters(((List) parameters).toArray());
      }
    } else {
      this.parameters = null;
    }

  }


  /**
   * setParties().
   * <p>Set contract parties from given list of DvpParty objects.</p>
   *
   * @param parties :
   */
  public void setParties(List<DvpParty> parties) {

    this.parties = parties;

  }


  private void setParties(Object parties) {

    if (parties != null) {
      if (parties instanceof MPWrappedArray) {
        this.parties = decodeParties((MPWrappedArray) parties);
      } else if (parties instanceof Object[]) {
        this.parties = decodeParties((Object[]) parties);
      } else if (parties instanceof List) {
        this.parties = decodeParties(((List) parties).toArray());
      }
    } else {
      this.parties = null;
    }

  }


  private void setProtocol(Object protocol) {

    if (protocol != null) {
      this.protocol = String.valueOf(protocol);
    } else {
      this.protocol = null;
    }
  }


  public void set__address(String address) {

    this.contractAddress = (address != null ? address : "");
  }


  public void set__canceltime(long cancelTime) {

    this.canceltime = cancelTime;
  }


  public void set__completed(boolean isCompleted) {

    this.completed = (isCompleted ? -1 : 0);
  }


  public void set__function(String function) {

    this.contractFunction = (function != null ? function : "");
  }


  /**
   * set__status().
   *
   * @param status : Status message to set.
   */
  public void set__status(String status) {

    if ((this.status == null) || (this.status.isEmpty())) {
      this.status = status;
    } else {
      this.status = this.status + "\n" + status;
    }
  }


  protected void set__timeevent(long timeEvent) {

    this.timeevent = timeEvent;
  }


  @Override
  public JSONObject toFriendlyJSON() {
    JSONObject myJson = toJSON();
    BiFunction<String, Object, Object> toTime = (k, in) -> {
      if (!(in instanceof Number)) {
        return in;
      }
      return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
          LocalDateTime.ofInstant(Instant.ofEpochSecond(((Number) in).longValue()), ZoneOffset.systemDefault()));
    };
    BiFunction<String, Object, Object> toBoolean = (k, in) -> {
      if (in instanceof Number) {
        return Boolean.valueOf(((Number) in).intValue() != 0);
      }
      return in;
    };

    myJson.compute(__COMPLETED, toBoolean);

    // Top level date-times.
    myJson.compute(__TIMEEVENT, toTime);
    myJson.compute(__CANCELTIME, toTime);
    myJson.compute(START_DATE, toTime);
    myJson.compute(EXPIRY, toTime);

    // convert date-times on encumbrances
    JSONArray addEnc = (JSONArray) myJson.get(ADD_ENCUMBRANCES);
    if (addEnc != null) {
      // for each added encumbrance
      for (Object o1 : addEnc) {
        JSONObject encumbranceDetail = (JSONObject) o1;

        // convert beneficiaries
        JSONArray array = (JSONArray) encumbranceDetail.get(BENEFICIARIES);
        for (Object o2 : array) {
          JSONObject json = (JSONObject) o2;
          json.compute(TxParameters.START_TIME, toTime);
          json.compute(TxParameters.END_TIME, toTime);
        }

        // convert admins
        array = (JSONArray) encumbranceDetail.get(ADMINS);
        for (Object o2 : array) {
          JSONObject json = (JSONObject) o2;
          json.compute(TxParameters.START_TIME, toTime);
          json.compute(TxParameters.END_TIME, toTime);
        }
      }
    }

    // return friendly JSON
    return myJson;
  }


  @Override
  public JSONObject toJSON() {

    JSONObject rVal = new JSONObject(true);

    rVal.put(__FUNCTION, contractFunction);
    rVal.put(__COMPLETED, completed);
    rVal.put(__ADDRESS, contractAddress);
    rVal.put(__TIMEEVENT, timeevent);
    rVal.put(__CANCELTIME, canceltime);
    rVal.put(ISSUING_ADDRESS, issuingaddress);
    rVal.put(START_DATE, startdate);
    rVal.put(EXPIRY, expiry);

    if ((status != null) && (status.length() > 0)) {
      rVal.put(__STATUS, status);
    }

    if (protocol != null) {
      rVal.put(PROTOCOL, protocol);
    }

    if (metadata != null) {
      rVal.put(METADATA, metadata);
    }

    if (!autosign) {
      rVal.put(AUTO_SIGN, false);
    }

    if (this.parties != null) {
      rVal.put(PARTIES, encodePartiesJson());
    }

    if (this.authorisations != null) {
      rVal.put(AUTHORISATIONS, encodeAuthorisationsJson());
    }

    if (this.parameters != null) {
      rVal.put(PARAMETERS, encodeParametersJson());
    }

    if (this.addencumbrances != null) {
      rVal.put(ADD_ENCUMBRANCES, encodeAddEncumbrancesJson());
    }

    if (this.events != null) {
      rVal.put(EVENTS, encodeEventsJson());
    }

    if (this.encumbrance != null) {
      rVal.put(ENCUMBRANCE, encodeEncumbranceJson());
    }

    return rVal;
  }

}
