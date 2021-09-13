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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSETS_IN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ASSETS_OUT;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.AUTO_SIGN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.EVENTS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.EXPIRY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ISSUING_ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.MAX_BLOCKS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.METADATA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.MIN_BLOCKS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PROTOCOL;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.START_DATE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__STATUS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__TIMEEVENT;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static io.setl.util.Constants.I_ZERO;
import static io.setl.util.Constants.L_ZERO;
import static io.setl.util.Convert.objectToLong;
import static io.setl.util.Convert.objectToString;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEvent;
import io.setl.common.Balance;
import io.setl.common.TypeSafeMap;
import java.util.ArrayList;
import java.util.Arrays;
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

public class ExchangeContractData implements IContractData {

  @SuppressWarnings("squid:CommentedOutCodeLine")
  /*

  'function' = exchange :

     {
     assetsin  : [
                [
                namespace,   # NS & Class define the asset to be converted
                classid,     #
                blocksize    # Block size of converted asset
                address,     # (API Optional) If specified, this will constrain the Address from which this contract will accept this asset.
                reference,   # (API Optional) PoA reference to use for this Input. Ignored if this is not a PoA.
               ], ...
              ],
     assetsout : [
                [
                namespace,   # NS & Class define the asset to be returned
                classid,     #
                blocksize,   # Block size of returned asset
                address,     # (API Optional) Address from which to return the asset. May be blank or omitted, in which case the Contract issuing address is
                             # used.
                reference,   # (API Optional) PoA reference to use for this Output. Ignored if this is not a PoA.
                publickey,   # (API Optional) Usually inserted by the wallet node.
                signature    # (API Optional) Usually inserted by the wallet node.
                ], ...
              ],
     'minblocks'     : nnn,               # (API Optional) (Number) Minimum number of blocks converted by any single Commitment, Default 0.
     'maxblocks'     : nnn,               # (API Optional) (Number) Maximum number of blocks converted by any single Commitment, Default unlimited.
     'events'        : [                  # List of events that can occur.
                          'expiry'
                       ],
     'startdate'     : nnn,               # (API Optional) long(UTC Unix Epoch time, seconds) indicating the earliest time at which a Commit may execute.
     'expiry'        : nnn,               # long(UTC Unix Epoch time, seconds)
     'protocol'      : "",                # (API Optional) User Data
     'metadata'      : "",                # (API Optional)
     'autosign'      : Boolean            # (API Optional) Defaults to True. If True, the node will attempt to sign the contract with the authoring key
     'signforwallet' : Boolean            # (API Optional) Defaults to False. If True (and autosign is true), the node will attempt to sign the contract
                                            with any keys in the current wallet
     }

     Assets identifiers (NS|Class) must be unique within the Input set.
     Assets identifiers (Address|NS|Class) must be unique within the Output set.

     PoA requires a valid Poa for Type `NewContract` for each input and output asset from the Issuing Address or the Output Address (for Outputs) if specified.

     If the Contract Issuing address is the issuer (Owner of the Namespace) of the Output Asset, then Output assets will be issued as required.
  */

  private static final Logger logger = LoggerFactory.getLogger(ExchangeContractData.class);

  private long expiry;

  private String issuingaddress;

  private String status;

  private boolean autosign = true;

  private String contractAddress;

  private String contractFunction = CONTRACT_NAME_EXCHANGE;

  private DvpEvent events;

  private List<NominateAsset> inputs;

  private Balance maxblocks = BALANCE_ZERO;

  private String metadata;

  private Balance minblocks = BALANCE_ZERO;

  private List<NominateAsset> outputs;

  private String protocol;

  private Long startdate;

  private long timeevent;

  private ExchangeContractData() {

    initialise();

  }


  private void initialise() {
    issuingaddress = "";
    contractAddress = "";
    timeevent = 0L;
    startdate = 0L;
    expiry = 0L;
    protocol = "";
    metadata = "";
    status = "";
    inputs = new ArrayList<>();
    outputs = new ArrayList<>();
  }


  /**
   * NominateMultiContractData.
   * <p>NominateMultiContractData Copy constructor.</p>
   *
   * @param toCopy :
   */
  public ExchangeContractData(ExchangeContractData toCopy) {

    if (toCopy == null) {
      initialise();
      return;
    }

    issuingaddress = toCopy.issuingaddress;
    timeevent = toCopy.get__timeevent();
    contractAddress = toCopy.contractAddress;
    contractFunction = toCopy.get__function();
    startdate = toCopy.startdate;
    expiry = toCopy.expiry;
    autosign = toCopy.autosign;
    protocol = toCopy.protocol;
    metadata = toCopy.metadata;
    status = "";

    inputs = NominateAsset.cloneAssetList(toCopy.inputs);
    outputs = NominateAsset.cloneAssetList(toCopy.outputs);
    minblocks = toCopy.minblocks;
    maxblocks = toCopy.maxblocks;

    if (toCopy.events != null) {
      events = new DvpEvent(toCopy.events);
    } else {
      this.events = null;
    }

  }


  /**
   * NominateMultiContractData constructor.
   *
   * @param contractAddress  :
   * @param contractFunction :
   * @param inputs           : Expressed as an array or list of NominateAsset objects or encoded NominateAsset objects
   * @param outputs          : as inputs
   * @param startDate        :
   * @param expiryDate       :
   * @param events           :
   * @param issuingAddress   :
   * @param protocol         :
   * @param metadata         :
   */
  @SuppressWarnings("squid:S00107") // Parameter Count.
  public ExchangeContractData(
      String contractAddress,
      String contractFunction,
      Object inputs,
      Object outputs,
      Number minblocks,
      Number maxblocks,
      long startDate,
      long expiryDate,
      String[] events,
      String issuingAddress,
      String protocol,
      String metadata
  ) {

    this.contractAddress = contractAddress;
    this.contractFunction = contractFunction;
    this.inputs = NominateAsset.decodeAssetList(inputs);
    this.outputs = NominateAsset.decodeAssetList(outputs);
    this.minblocks = new Balance(minblocks);
    this.maxblocks = new Balance(maxblocks);

    this.issuingaddress = issuingAddress;
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.status = "";
    this.startdate = startDate;
    this.expiry = expiryDate;
    this.events = new DvpEvent(events);
    this.timeevent = expiryDate;

  }


  /**
   * ExchangeContractData Constructor.
   *
   * @param sourceMap :
   */
  public ExchangeContractData(Map<String, Object> sourceMap) {

    contractAddress = (String) sourceMap.getOrDefault(__ADDRESS, "");
    contractFunction = (String) sourceMap.getOrDefault(__FUNCTION, sourceMap.getOrDefault(CONTRACT_FUNCTION, ""));
    startdate = TypeSafeMap.asNumber(sourceMap.getOrDefault(START_DATE, L_ZERO)).longValue();
    expiry = TypeSafeMap.asNumber(sourceMap.getOrDefault(EXPIRY, I_ZERO)).longValue();
    timeevent = TypeSafeMap.asNumber(sourceMap.getOrDefault(__TIMEEVENT, expiry)).longValue();
    issuingaddress = (String) sourceMap.getOrDefault(ISSUING_ADDRESS, "");

    this.inputs = NominateAsset.decodeAssetList(sourceMap.get(ASSETS_IN));
    this.outputs = NominateAsset.decodeAssetList(sourceMap.get(ASSETS_OUT));

    this.minblocks = new Balance(sourceMap.getOrDefault(MIN_BLOCKS, L_ZERO));
    this.maxblocks = new Balance(sourceMap.getOrDefault(MAX_BLOCKS, L_ZERO));

    status = (String) sourceMap.getOrDefault(__STATUS, "");
    this.protocol = (String) sourceMap.getOrDefault(PROTOCOL, "");
    this.metadata = (String) sourceMap.getOrDefault(METADATA, "");
    this.autosign = TypeSafeMap.asBoolean(sourceMap.getOrDefault(AUTO_SIGN, Boolean.TRUE));

    setEvents(sourceMap.get(EVENTS));

  }


  /**
   * ExchangeContractData constructor.
   *
   * @param asMapValue :
   */
  public ExchangeContractData(MPWrappedMap<String, Object> asMapValue) {
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

        case __TIMEEVENT:
          timeevent = objectToLong(v, 0L);
          break;

        case START_DATE:
          startdate = objectToLong(v, 0L);
          break;

        case EXPIRY:
          expiry = objectToLong(v, 0L);
          break;

        case AUTO_SIGN:
          setAutosign(TypeSafeMap.asBoolean(v));
          break;

        case ISSUING_ADDRESS:
          issuingaddress = objectToString(v);
          break;

        case PROTOCOL:
          protocol = objectToString(v);
          break;

        case METADATA:
          metadata = objectToString(v);
          break;

        case ASSETS_IN:
          this.inputs = NominateAsset.decodeAssetList(v);
          break;

        case ASSETS_OUT:
          this.outputs = NominateAsset.decodeAssetList(v);
          break;

        case MIN_BLOCKS:
          if (v instanceof Number) {
            this.minblocks = new Balance((Number) v);
          }
          break;

        case MAX_BLOCKS:
          if (v instanceof Number) {
            this.maxblocks = new Balance((Number) v);
          }
          break;

        case EVENTS:
          setEvents(v);
          break;

        default:
          break;
      }

    });

  }


  @Override
  public Set<String> addresses() {

    Set<String> rVal = new TreeSet<>();

    if ((issuingaddress != null) && (!issuingaddress.isEmpty())) {
      rVal.add(issuingaddress);
    }

    if ((this.outputs != null) && (!this.outputs.isEmpty())) {
      for (NominateAsset thisOutput : this.outputs) {
        if ((thisOutput != null) && (thisOutput.getAddress() != null) && (thisOutput.getAddress().length() > 0)) {
          rVal.add(thisOutput.getAddress());
        }
      }
    }

    return rVal;

  }


  @Override
  public IContractData copy() {

    return new ExchangeContractData(this);

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


  @SuppressWarnings("squid:S1168")
  private Object[] encodeEvents() {

    if (this.events == null) {
      return null;
    }

    return this.events.encode(0);
  }


  /**
   * encodeJson.
   * <p>Encode Events data as JSONArray.</p>
   *
   * @return :
   */
  public JSONArray encodeEventsJson() {

    return this.events.encodeJson();

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
    rVal.put(__ADDRESS, contractAddress);
    rVal.put(__TIMEEVENT, timeevent);
    rVal.put(ISSUING_ADDRESS, issuingaddress);
    rVal.put(START_DATE, startdate);
    rVal.put(EXPIRY, expiry);

    if (!autosign) {
      rVal.put(AUTO_SIGN, false);
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

    rVal.put(ASSETS_IN, NominateAsset.encodeAssetList(this.inputs));
    rVal.put(ASSETS_OUT, NominateAsset.encodeAssetList(this.outputs));

    rVal.put(MIN_BLOCKS, this.minblocks);
    rVal.put(MAX_BLOCKS, this.maxblocks);

    if (this.events != null) {
      rVal.put(EVENTS, encodeEvents());
    }

    return rVal;
  }


  /**
   * encodeToMapForTxParameter.
   * <p>Encode Contract data structure as a map, but omit some internal fields that do not need to appear in the transaction data.</p>
   * <p>The purpose of this is to allow the use of the ContractData classes for the construction of a New Contract.</p>
   *
   * @return :
   */
  @Override
  public Map<String, Object> encodeToMapForTxParameter() {

    String functionName = CONTRACT_FUNCTION; // Sonarlint smell fix.

    Map<String, Object> rVal = encodeToMap();

    if (!rVal.containsKey(__FUNCTION) && (rVal.containsKey(functionName))) {
      rVal.put(__FUNCTION, rVal.get(functionName));
    }

    // Strip out defined keys...
    for (String keyName : Arrays.asList(functionName, __ADDRESS, __TIMEEVENT)) {
      rVal.remove(keyName);
    }

    return rVal;

  }


  @SuppressWarnings({"squid:S1126", "squid:S3776"})
  @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
  @Override
  public boolean equals(Object toCompare) {

    // squid:S3776 : If this is too cognitively complex ... hmmm.

    // Something does not equal Nothing
    if (toCompare == null) {
      return false;
    }

    // Dis-similar objects are not equal.
    if (!(toCompare instanceof ExchangeContractData)) {
      return false;
    }

    // Compare everything except for status

    ExchangeContractData theOther = (ExchangeContractData) toCompare;

    if ((!Objects.equals(contractFunction, theOther.get__function())) && ((contractFunction == null) || (!contractFunction
        .equalsIgnoreCase(theOther.get__function())))) {
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

    if ((!Objects.equals(issuingaddress, theOther.issuingaddress)) && ((issuingaddress == null) || (!issuingaddress
        .equalsIgnoreCase(theOther.issuingaddress)))) {
      return false;
    }

    if ((!Objects.equals(startdate, theOther.getStartdate())) && ((startdate == null) || (!startdate.equals(theOther.getStartdate())))) {
      return false;
    }

    if (expiry != theOther.getExpiry()) {
      return false;
    }

    if ((!Objects.equals(protocol, theOther.protocol)) && ((protocol == null) || (!protocol.equalsIgnoreCase(theOther.protocol)))) {
      return false;
    }

    if ((!Objects.equals(metadata, theOther.metadata)) && ((metadata == null) || (!metadata.equalsIgnoreCase(theOther.metadata)))) {
      return false;
    }

    if (!Objects.equals(maxblocks, theOther.maxblocks)) {
      return false;
    }

    if (!Objects.equals(minblocks, theOther.minblocks)) {
      return false;
    }

    if ((inputs != theOther.inputs) && ((inputs == null) || (!inputs.equals(theOther.inputs)))) {
      return false;
    }

    if ((outputs != theOther.outputs) && ((outputs == null) || (!outputs.equals(theOther.outputs)))) {
      return false;
    }

    // squid:S1126 : This does not just return here in case the number of checks increases in the future,
    if ((events != theOther.events) && ((events == null) || (!events.equals(theOther.events)))) {
      return false;
    }

    return true;
  }


  public boolean getAutosign() {

    return autosign;
  }


  @Override
  public String getContractType() {

    return contractFunction;
  }


  public DvpEvent getEvents() {
    return new DvpEvent(events);
  }


  public Long getExpiry() {

    return expiry;
  }


  public List<NominateAsset> getInputs() {

    return inputs;
  }


  public String getIssuingaddress() {
    return issuingaddress;
  }


  public void setIssuingaddress(String issuingaddress) {
    this.issuingaddress = issuingaddress;
  }


  public Balance getMaxblocks() {

    return maxblocks;
  }


  public String getMetadata() {
    return metadata;
  }


  public Balance getMinblocks() {

    return minblocks;
  }


  public List<NominateAsset> getOutputs() {

    return outputs;
  }


  public String getProtocol() {
    return protocol;
  }


  public Long getStartdate() {

    return startdate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String get__address() {

    return contractAddress;
  }


  @SuppressWarnings("squid:S4144") // Just for legacy reasons.
  @Override
  public String get__function() {

    return contractFunction;
  }


  public long get__timeevent() {

    return timeevent;
  }


  @Override
  public int hashCode() {

    // Everything except for status
    return Objects.hash(expiry, issuingaddress, autosign, contractAddress, contractFunction, events, inputs, maxblocks, metadata, minblocks, outputs,
        protocol, startdate, timeevent);

  }


  public void setAutosign(boolean autosign) {

    this.autosign = autosign;
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


  @Override
  public long setNextTimeEvent(long updateTime, boolean forceToUpdateTime) {

    if (forceToUpdateTime) {
      set__timeevent(updateTime);
    } else {
      set__timeevent(getExpiry());
    }

    return get__timeevent();
  }


  public void set__address(String address) {

    this.contractAddress = (address != null ? address : "");
  }


  public void set__function(String function) {

    this.contractFunction = (function != null ? function : "");
  }


  protected void set__timeevent(long timeEvent) {

    this.timeevent = timeEvent;
  }


  @Override
  public JSONObject toJSON() {

    JSONObject rVal = new JSONObject(true);

    rVal.put(__FUNCTION, contractFunction);
    rVal.put(__ADDRESS, contractAddress);
    rVal.put(__TIMEEVENT, timeevent);
    rVal.put(ISSUING_ADDRESS, issuingaddress);
    rVal.put(START_DATE, startdate);
    rVal.put(EXPIRY, expiry);

    if (!autosign) {
      rVal.put(AUTO_SIGN, false);
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

    rVal.put(ASSETS_IN, NominateAsset.encodeAssetListToJSON(this.inputs));
    rVal.put(ASSETS_OUT, NominateAsset.encodeAssetListToJSON(this.outputs));

    rVal.put(MIN_BLOCKS, this.minblocks);
    rVal.put(MAX_BLOCKS, this.maxblocks);

    if (this.events != null) {
      rVal.put(EVENTS, encodeEventsJson());
    }

    return rVal;
  }

}
