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

import static io.setl.bc.pychain.state.tx.helper.TxParameters.BLOCK_SIZE_IN;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.BLOCK_SIZE_OUT;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.CONTRACT_FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.EVENTS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.EXPIRY;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.INPUT_TOKEN_CLASS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.ISSUING_ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.METADATA;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.NAMESPACE;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.OUTPUT_TOKEN_CLASS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.PROTOCOL;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__ADDRESS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__FUNCTION;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__STATUS;
import static io.setl.bc.pychain.state.tx.helper.TxParameters.__TIMEEVENT;
import static io.setl.common.Balance.BALANCE_ZERO;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;
import static io.setl.util.Convert.objectToLong;
import static io.setl.util.Convert.objectToString;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEvent;
import io.setl.common.Balance;

@SuppressWarnings("squid:ClassVariableVisibilityCheck")
public class TokensNominateContractData implements IContractData {

  @SuppressWarnings("squid:CommentedOutCodeLine")
  /*

  'function' = tokens_nominate :
     
     {
     'namespace'        :
     'inputtokenclass'  : String,         #
     'outputtokenclass' : String,         #
     'blocksizein'      : Int,            # Integer, > 0
     'blocksizeout'     :
     'events'        : [                  # List of events that can occur.
                          'expiry'
                       ],
     'expiry'        : nnn,               # long(UTC Unix Epoch time, seconds)
     'protocol'      : "",                # User Data
     'metadata'      : ""
     
     }
  
   */
  private static final Logger logger = LoggerFactory.getLogger(TokensNominateContractData.class);

  public Balance blocksizein;

  public Balance blocksizeout;

  public long expiry;

  public String issuingaddress;

  public String status;

  private String contractAddress;

  private String contractFunction = CONTRACT_NAME_TOKENS_NOMINATE;

  private DvpEvent events;

  private String inputtokenassetid;

  private String inputtokenclass;

  private String metadata;

  private String namespace;

  private String outputtokenassetid;

  private String outputtokenclass;

  private String protocol;

  private long timeevent;

  private TokensNominateContractData() {

    issuingaddress = "";
    contractAddress = "";
    contractFunction = "";
    timeevent = 0L;
    namespace = "";
    inputtokenclass = "";
    outputtokenclass = "";
    blocksizein = BALANCE_ZERO;
    blocksizeout = BALANCE_ZERO;
    protocol = "";
    metadata = "";
    status = "";
    inputtokenassetid = null;
    outputtokenassetid = null;

  }


  /**
   * TokensNominateContractData.
   * <p>TokensNominateContractData Copy constructor.</p>
   *
   * @param toCopy :
   */
  public TokensNominateContractData(TokensNominateContractData toCopy) {

    if (toCopy == null) {
      return;
    }

    issuingaddress = toCopy.issuingaddress;
    timeevent = toCopy.get__timeevent();
    contractAddress = toCopy.get__address();
    contractFunction = toCopy.get__function();
    namespace = toCopy.namespace;
    inputtokenclass = toCopy.inputtokenclass;
    outputtokenclass = toCopy.outputtokenclass;
    blocksizein = toCopy.blocksizein;
    blocksizeout = toCopy.blocksizeout;
    expiry = toCopy.expiry;
    protocol = toCopy.protocol;
    metadata = toCopy.metadata;
    status = "";
    inputtokenassetid = null;
    outputtokenassetid = null;

    if (toCopy.events != null) {
      events = new DvpEvent(toCopy.events);
    }


  }


  /**
   * TokensNominateContractData Constructor.
   *
   * @param namespace        :
   * @param inputtokenclass  :
   * @param outputtokenclass :
   * @param blocksizein      :
   * @param blocksizeout     :
   * @param expiry           :
   * @param events           :
   * @param protocol         :
   * @param metadata         :
   */
  @SuppressWarnings("squid:S00107") // Parameter Count.
  public TokensNominateContractData(
      String contractAddress,
      String contractFunction,
      String namespace,
      String inputtokenclass,
      String outputtokenclass,
      Number blocksizein,
      Number blocksizeout,
      long expiry,
      String[] events,
      String issuingAddress,
      String protocol,
      String metadata
  ) {

    this.contractAddress = contractAddress;
    this.contractFunction = contractFunction;
    this.namespace = (namespace == null ? "" : namespace);
    this.inputtokenclass = (inputtokenclass == null ? "" : inputtokenclass);
    this.outputtokenclass = (outputtokenclass == null ? "" : outputtokenclass);
    this.blocksizein = new Balance(blocksizein);
    this.blocksizeout = new Balance(blocksizeout);
    this.issuingaddress = issuingAddress;
    this.protocol = (protocol == null ? "" : protocol);
    this.metadata = (metadata == null ? "" : metadata);
    this.status = "";
    this.expiry = expiry;
    this.events = new DvpEvent(events);
    this.timeevent = expiry;
    this.inputtokenassetid = null;
    this.outputtokenassetid = null;

  }


  /**
   * TokensNominateContractData().
   * <p>
   * Constructor : Create TokensNominateContractData Data model from the given [String, Object] Map.
   * </p>
   *
   * @param sourceMap :
   */
  public TokensNominateContractData(Map<String, Object> sourceMap) {

    contractAddress = (String) sourceMap.getOrDefault(__ADDRESS, "");
    contractFunction = (String) sourceMap.getOrDefault(__FUNCTION, sourceMap.getOrDefault(CONTRACT_FUNCTION, ""));
    expiry = ((Number) sourceMap.getOrDefault(EXPIRY, 0)).longValue();
    timeevent = ((Number) sourceMap.getOrDefault(__TIMEEVENT, expiry)).longValue();
    issuingaddress = (String) sourceMap.getOrDefault(ISSUING_ADDRESS, "");

    status = (String) sourceMap.getOrDefault(__STATUS, "");
    this.protocol = (String) sourceMap.getOrDefault(PROTOCOL, "");
    this.metadata = (String) sourceMap.getOrDefault(METADATA, "");

    this.namespace = (String) sourceMap.getOrDefault(NAMESPACE, "");
    this.inputtokenclass = (String) sourceMap.getOrDefault(INPUT_TOKEN_CLASS, "");
    this.outputtokenclass = (String) sourceMap.getOrDefault(OUTPUT_TOKEN_CLASS, "");
    this.blocksizein = new Balance(sourceMap.getOrDefault(BLOCK_SIZE_IN, BALANCE_ZERO));
    this.blocksizeout = new Balance(sourceMap.getOrDefault(BLOCK_SIZE_OUT, BALANCE_ZERO));
    setEvents(sourceMap.get(EVENTS));

    inputtokenassetid = null;
    outputtokenassetid = null;

  }


  /**
   * TokensNominateContractData().
   * <p>
   * Constructor : Create TokensNominate Data model from the given MPWrappedMap[String, Object] object.
   * </p>
   *
   * @param asMapValue :
   */
  public TokensNominateContractData(MPWrappedMap<String, Object> asMapValue) {
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

        case EXPIRY:
          expiry = objectToLong(v, 0L);
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

        case NAMESPACE:
          namespace = objectToString(v);
          break;

        case INPUT_TOKEN_CLASS:
          inputtokenclass = objectToString(v);
          break;

        case OUTPUT_TOKEN_CLASS:
          outputtokenclass = objectToString(v);
          break;

        case BLOCK_SIZE_IN:
          blocksizein = (v == null ? BALANCE_ZERO : new Balance(v));
          break;

        case BLOCK_SIZE_OUT:
          blocksizeout = (v == null ? BALANCE_ZERO : new Balance(v));
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

    return rVal;
  }


  /**
   * copy function, because Interfaces do not support constructors.
   *
   * @return :
   */
  @Override
  public IContractData copy() {

    return new TokensNominateContractData(this);
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

    rVal.put(NAMESPACE, (namespace == null ? "" : namespace));
    rVal.put(INPUT_TOKEN_CLASS, (inputtokenclass == null ? "" : inputtokenclass));
    rVal.put(OUTPUT_TOKEN_CLASS, (outputtokenclass == null ? "" : outputtokenclass));

    rVal.put(BLOCK_SIZE_IN, blocksizein.getValue());
    rVal.put(BLOCK_SIZE_OUT, blocksizeout.getValue());

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


  @Override
  @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
  public boolean equals(Object toCompare) {

    if (toCompare == null) {
      return false;
    }

    if (!(toCompare instanceof TokensNominateContractData)) {
      return false;
    }

    TokensNominateContractData theOther = (TokensNominateContractData) toCompare;

    if ((!Objects.equals(contractFunction, theOther.get__function())) && ((contractFunction == null) || (!contractFunction
        .equalsIgnoreCase(theOther.get__function())))) {
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

    if (expiry != theOther.expiry) {
      return false;
    }

    if (!blocksizein.equalTo(theOther.blocksizein)) {
      return false;
    }

    if (!blocksizeout.equalTo(theOther.blocksizeout)) {
      return false;
    }

    if ((!Objects.equals(namespace, theOther.namespace)) && ((namespace == null) || (!namespace.equalsIgnoreCase(theOther.namespace)))) {
      return false;
    }

    if ((!Objects.equals(inputtokenclass, theOther.inputtokenclass)) && ((inputtokenclass == null) || (!inputtokenclass
        .equalsIgnoreCase(theOther.inputtokenclass)))) {
      return false;
    }

    if ((!Objects.equals(outputtokenclass, theOther.outputtokenclass)) && ((outputtokenclass == null) || (!outputtokenclass
        .equalsIgnoreCase(theOther.outputtokenclass)))) {
      return false;
    }

    if ((!Objects.equals(protocol, theOther.protocol)) && ((protocol == null) || (!protocol.equalsIgnoreCase(theOther.protocol)))) {
      return false;
    }

    if ((!Objects.equals(metadata, theOther.metadata)) && ((metadata == null) || (!metadata.equalsIgnoreCase(theOther.metadata)))) {
      return false;
    }

    return (events == theOther.events) || ((events != null) && (events.equals(theOther.events)));
  }


  @Override
  public String getContractType() {

    return contractFunction;
  }


  /**
   * getInputAssetID.
   * <p>Return the Input Asset ID : Namespace|ClassID.
   * For performance reasons, this value is created and cached.</p>
   *
   * @return :
   */
  public String getInputAssetID() {

    if (inputtokenassetid == null) {
      inputtokenassetid = namespace + "|" + inputtokenclass;
    }

    return inputtokenassetid;
  }


  public String getInputtokenclass() {

    return inputtokenclass;
  }


  public String getMetadata() {
    return metadata;
  }


  public String getNamespace() {

    return namespace;
  }


  /**
   * getOutputAssetID.
   * <p>Return the Output Asset ID : Namespace|ClassID.
   * For performance reasons, this value is created and cached.</p>
   *
   * @return :
   */
  public String getOutputAssetID() {

    if (outputtokenassetid == null) {
      outputtokenassetid = namespace + "|" + outputtokenclass;
    }

    return outputtokenassetid;
  }


  public String getOutputtokenclass() {

    return outputtokenclass;
  }


  public String getProtocol() {
    return protocol;
  }


  public String get__address() {

    return contractAddress;
  }


  @Override
  public String get__function() {

    return contractFunction;
  }


  public long get__timeevent() {

    return timeevent;
  }


  @Override
  public int hashCode() {

    return (this.issuingaddress.hashCode() + this.contractFunction.hashCode() + this.contractAddress.hashCode()) % Integer.MAX_VALUE;

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
      set__timeevent(expiry);
    }

    return get__timeevent();
  }


  public void set__address(String address) {

    this.contractAddress = (address != null ? address : "");
  }


  public void set__function(String function) {

    contractFunction = (function != null ? function : "");
  }


  public void set__timeevent(long timeEvent) {

    this.timeevent = timeEvent;
  }


  @Override
  public JSONObject toJSON() {

    JSONObject rVal = new JSONObject(true);

    rVal.put(__FUNCTION, contractFunction);
    rVal.put(__ADDRESS, contractAddress);
    rVal.put(__TIMEEVENT, timeevent);
    rVal.put(ISSUING_ADDRESS, issuingaddress);
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

    rVal.put(NAMESPACE, (namespace == null ? "" : namespace));
    rVal.put(INPUT_TOKEN_CLASS, (inputtokenclass == null ? "" : inputtokenclass));
    rVal.put(OUTPUT_TOKEN_CLASS, (outputtokenclass == null ? "" : outputtokenclass));

    rVal.put(BLOCK_SIZE_IN, blocksizein.getValue());
    rVal.put(BLOCK_SIZE_OUT, blocksizeout.getValue());

    if (this.events != null) {
      rVal.put(EVENTS, encodeEventsJson());
    }

    return rVal;
  }

}
