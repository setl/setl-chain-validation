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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static io.setl.bc.pychain.state.tx.Hash.computeHash;
import static io.setl.common.AddressUtil.verifyPublicKey;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.DVP_DELETE_DELAY_ON_COMPLETE;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAddEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpAuthorisation;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpEvent;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpPayItem;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUkContractData.DvpReceiveItem;
import io.setl.common.Balance;
import io.setl.common.Hex;
import io.setl.crypto.SHA256;

/**
 * Created by nicholas on 17/08/2017.
 */
@SuppressWarnings("unlikely-arg-type")
public class DvpUkContractDataTest {

  String contractAddress = "";

  String contractFunction = CONTRACT_NAME_DVP_UK;

  String encumbranceToUse = "enc.";

  String[] events = new String[]{"EventType1", "EventType2"};

  long expiryDate = 99999999L;

  int isCompleted = 0;

  String issuingAddress = "Address1";

  String metadata = "MetaData";

  boolean mustSign1 = false;

  boolean mustSign2 = false;

  long nextTimeEvent = 8888888L;

  // Party 1
  String partyIdentifier1 = "party1";

  // Party 2
  String partyIdentifier2 = "party2";

  String protocol = "Protocol";

  String publicKey1 = "party1pub";

  String publicKey2 = "party2pub";

  String sigAddress1 = "party1sigaddress";

  String sigAddress2 = "party2sigaddress";

  String signature1 = "party1sig";

  String signature2 = "party2sig";

  long startDate = 1L;


  @Test
  public void dvpAddEncumbranceStringTest() throws Exception {

    DvpAddEncumbrance enc1 = new DvpAddEncumbrance("AssetID", "Reference", "200", "", "");
    enc1.setPoaPublicKey("POAPubKey");
    enc1.setSignature("sig5");
    enc1.setPublicKey("PubKey");
    enc1.beneficiaries.add(new EncumbranceDetail("BAddress", 0L, 999999L));
    enc1.administrators.add(new EncumbranceDetail("AAddress", 1L, 888888L));

    // Copy / construct

    DvpAddEncumbrance enc2 = new DvpAddEncumbrance("AssetID", "Reference", "200", "PubKey", "POAPubKey", "sig5");
    enc2.beneficiaries.add(new EncumbranceDetail("BAddress", 0L, 999999L));
    enc2.administrators.add(new EncumbranceDetail("AAddress", 1L, 888888L));

    DvpAddEncumbrance enc3 = new DvpAddEncumbrance(enc1);
    DvpAddEncumbrance enc4 = new DvpAddEncumbrance(enc1.encode(0));
    final DvpAddEncumbrance enc5 = new DvpAddEncumbrance(new MPWrappedArrayImpl(enc1.encode(0)));

    assertTrue(enc1.equals(enc2));
    assertTrue(enc1.equals(enc3));
    assertTrue(enc1.equals(enc4));
    assertTrue(enc1.equals(enc5));
    assertTrue(enc5.equals(enc4));
    assertTrue(enc5.equals(enc3));
    assertTrue(enc5.equals(enc2));

    // Values

    assertTrue(enc5.getPublicKey().equals("PubKey"));
    assertTrue(enc5.getSignature().equals("sig5"));
    assertTrue(enc5.getPoaPublicKey().equals("POAPubKey"));
    assertTrue(enc5.encumbranceAmount().equals(0L)); // CalculatedAmount will not be set at this point.
    assertTrue(enc5.amountString.equals("200"));
    assertTrue(enc5.reference.equals("Reference"));
    assertTrue(enc5.hashCode() == "PubKey".hashCode());

    // Hash object

    String sigMessage = computeHash(enc1.objectToHashToSign(""));
    String hash = Hex.encode(SHA256.sha256(sigMessage.getBytes(StandardCharsets.UTF_8)));
    assertTrue(hash.equals("9461afbe2ef77ecd701612b25e253aeaab826c6c365f040c597af72ec51cf7b1"));

    // Equals

    assertFalse(enc1.equals(null));
    assertFalse(enc1.equals("Not right Class"));
    assertFalse(enc1.equals(new DvpAddEncumbrance("", "", 0L, "", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("", "", 0L, "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "", 0L, "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "Reference", 0L, "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "Reference", "200", "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "Reference", "200", "PubKey", "", "sig5")));

    enc2 = new DvpAddEncumbrance("AssetID", "Reference", "200", "PubKey", "", "sig5");

    enc2.beneficiaries.add(new EncumbranceDetail("BAddress", 0L, 999999L));
    assertFalse(enc1.equals(enc2));

    enc2.administrators.add(new EncumbranceDetail("AAddress", 1L, 888888L));
    assertFalse(enc1.equals(enc2));

    enc2.setPoaPublicKey("POAPubKey");
    assertTrue(enc1.equals(enc2));

    enc2.administrators.add(new EncumbranceDetail("AAddress2", 1L, 888888L));
    assertFalse(enc1.equals(enc2));

    enc2.administrators.clear();
    enc2.administrators.add(new EncumbranceDetail("AAddress", 1L, 111111L));
    assertFalse(enc1.equals(enc2));

  }


  @Test
  public void dvpAddEncumbranceTest() throws Exception {

    DvpAddEncumbrance enc1 = new DvpAddEncumbrance("AssetID", "Reference", 100L, "", "");
    enc1.setPoaPublicKey("POAPubKey");
    enc1.setSignature("sig5");
    enc1.setPublicKey("PubKey");
    enc1.beneficiaries.add(new EncumbranceDetail("BAddress", 0L, 999999L));
    enc1.administrators.add(new EncumbranceDetail("AAddress", 1L, 888888L));

    // Copy / construct

    DvpAddEncumbrance enc2 = new DvpAddEncumbrance("AssetID", "Reference", 100L, "PubKey", "POAPubKey", "sig5");
    enc2.beneficiaries.add(new EncumbranceDetail("BAddress", 0L, 999999L));
    enc2.administrators.add(new EncumbranceDetail("AAddress", 1L, 888888L));

    DvpAddEncumbrance enc3 = new DvpAddEncumbrance(enc1);
    DvpAddEncumbrance enc4 = new DvpAddEncumbrance(enc1.encode(0));
    final DvpAddEncumbrance enc5 = new DvpAddEncumbrance(new MPWrappedArrayImpl(enc1.encode(0)));

    assertTrue(enc1.equals(enc2));
    assertTrue(enc1.equals(enc3));
    assertTrue(enc1.equals(enc4));
    assertTrue(enc1.equals(enc5));
    assertTrue(enc5.equals(enc4));
    assertTrue(enc5.equals(enc3));
    assertTrue(enc5.equals(enc2));

    // Values

    assertTrue(enc5.getPublicKey().equals("PubKey"));
    assertTrue(enc5.getSignature().equals("sig5"));
    assertTrue(enc5.getPoaPublicKey().equals("POAPubKey"));
    assertTrue(enc5.encumbranceAmount().equals(100L));
    assertTrue(enc5.amountString == null);
    assertTrue(enc5.reference.equals("Reference"));

    // Hash object

    String sigMessage = computeHash(enc1.objectToHashToSign(""));
    String hash = Hex.encode(SHA256.sha256(sigMessage.getBytes(StandardCharsets.UTF_8)));
    assertTrue(hash.equals("0a321be51aee5ad2d1eb792fca1c5ab80d9d855e527b866ac5fe6957190f958c"));

    // Equals

    assertFalse(enc1.equals(null));
    assertFalse(enc1.equals("Not right Class"));
    assertFalse(enc1.equals(new DvpAddEncumbrance("", "", 0L, "", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("", "", 0L, "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "", 0L, "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "Reference", 0L, "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "Reference", 100L, "PubKey", "", "")));
    assertFalse(enc1.equals(new DvpAddEncumbrance("AssetID", "Reference", 100L, "PubKey", "", "sig5")));

    enc2 = new DvpAddEncumbrance("AssetID", "Reference", 100L, "PubKey", "", "sig5");

    enc2.beneficiaries.add(new EncumbranceDetail("BAddress", 0L, 999999L));
    assertFalse(enc1.equals(enc2));

    enc2.administrators.add(new EncumbranceDetail("AAddress", 1L, 888888L));
    assertFalse(enc1.equals(enc2));

    enc2.setPoaPublicKey("POAPubKey");
    assertTrue(enc1.equals(enc2));

    enc2.administrators.add(new EncumbranceDetail("AAddress2", 1L, 888888L));
    assertFalse(enc1.equals(enc2));

    enc2.administrators.clear();
    enc2.administrators.add(new EncumbranceDetail("AAddress", 1L, 111111L));
    assertFalse(enc1.equals(enc2));

  }


  @Test
  public void dvpAuthorisationTest() throws Exception {

    final String address = "Address0";
    final String[] addresses = new String[]{"Address1", "Address2"};
    final String authorisationID = "aid";
    final String signature = "thissignature";
    final String metadata = "thismetadata";
    final String poaPublicKey = "thispoaPublicKey";

    // Empty Instance

    DvpAuthorisation da1 = new DvpAuthorisation(new MPWrappedArrayImpl(new Object[0]));

    assertTrue(da1.getAddress().equals(""));
    assertTrue(da1.getAddresses() == null);
    assertTrue(da1.authorisationID.equals(""));
    assertTrue(da1.getSignature().equals(""));
    assertTrue(da1.getMetadata().equals(""));
    assertTrue(da1.getPoaPublicKey().equals(""));
    assertTrue(da1.getRefused() == false);

    // Real Instances

    boolean refused = false;

    Object[] realData = new Object[]{
        null,
        authorisationID,
        signature,
        metadata,
        refused,
        1,
        poaPublicKey
    };

    da1 = new DvpAuthorisation(realData);

    assertTrue(da1.getAddress() == null); // Note null.toString == null.
    assertTrue(da1.getAddresses() == null);
    assertTrue(da1.authorisationID.equals(authorisationID));
    assertTrue(da1.getSignature().equals(signature));
    assertTrue(da1.getMetadata().equals(metadata));
    assertTrue(da1.getPoaPublicKey().equals(poaPublicKey));
    assertTrue(da1.getRefused() == refused);

    //

    realData = new Object[]{
        null,
        authorisationID,
        "Not Sig",
        null,
        !refused,
        1,
        poaPublicKey
    };

    da1 = new DvpAuthorisation(realData);

    da1.setAddress(address);
    da1.setMetadata(metadata);

    assertTrue(da1.hashCode() == address.hashCode());

    assertTrue(da1.getAddress().equals(address));
    assertTrue(da1.getAddresses() == null);
    assertTrue(da1.authorisationID.equals(authorisationID));
    assertFalse(da1.getSignature().equals(signature));
    da1.setSignature(signature);
    assertTrue(da1.getSignature().equals(signature));
    assertTrue(da1.getMetadata().equals(metadata));
    assertTrue(da1.getPoaPublicKey().equals(poaPublicKey));
    assertFalse(da1.getRefused() == refused);
    da1.setRefused(refused);
    assertTrue(da1.getRefused() == refused);

    realData = new Object[]{
        addresses,
        authorisationID,
        signature,
        metadata,
        refused,
        1,
        poaPublicKey
    };

    da1 = new DvpAuthorisation(realData);

    assertTrue(da1.getAddress() == null);
    assertTrue(Arrays.equals(da1.getAddresses(), addresses));
    assertTrue(da1.authorisationID.equals(authorisationID));
    assertTrue(da1.getSignature().equals(signature));
    assertTrue(da1.getMetadata().equals(metadata));
    assertTrue(da1.getPoaPublicKey().equals(poaPublicKey));
    assertTrue(da1.getRefused() == refused);

    realData = new Object[]{
        new MPWrappedArrayImpl(addresses),
        authorisationID,
        signature,
        metadata,
        refused,
        1,
        poaPublicKey
    };

    DvpAuthorisation da2 = new DvpAuthorisation(realData);

    assertTrue(da1.equals(da2));

    ArrayList<String> alist = new ArrayList<String>();
    alist.addAll(Arrays.asList(addresses));

    realData = new Object[]{
        alist,
        authorisationID,
        signature,
        metadata,
        refused,
        1,
        poaPublicKey
    };

    final DvpAuthorisation da3 = new DvpAuthorisation(realData);

    final DvpAuthorisation da4 = new DvpAuthorisation(da1);

    final DvpAuthorisation da5 = new DvpAuthorisation(addresses, authorisationID, signature, metadata, refused, 1);

    final DvpAuthorisation da6 = new DvpAuthorisation(da5.encode(0)); // poaPublicKey isEmpty

    da5.setPoaPublicKey(poaPublicKey);
    da6.setPoaPublicKey(poaPublicKey);

    final DvpAuthorisation da7 = new DvpAuthorisation(da5.encode(0)); // poaPublicKey isNotEmpty

    // Equals
    assertTrue(da1.equals(da2));
    assertTrue(da1.equals(da3));
    assertTrue(da1.equals(da4));
    assertTrue(da1.equals(da5));
    assertTrue(da1.equals(da6));
    assertTrue(da1.equals(da7));
    assertTrue(da7.equals(da6));
    assertTrue(da7.equals(da5));
    assertTrue(da7.equals(da4));
    assertTrue(da7.equals(da3));
    assertTrue(da7.equals(da2));

    //
    refused = true;

    realData = new Object[]{
        addresses,
        authorisationID,
        signature,
        metadata,
        refused,
        1
    };

    da1 = new DvpAuthorisation(realData);

    assertFalse(da1.equals(null));
    assertFalse(da1.equals("Not a DvpAuthorisation"));
    assertFalse(da1.equals(new DvpAuthorisation(address, "", "", "", false, 1)));
    assertFalse(da1.equals(new DvpAuthorisation(addresses, "", "", "", false, 1)));
    assertFalse(da1.equals(new DvpAuthorisation(addresses, authorisationID, "", "", false, 1)));
    assertFalse(da1.equals(new DvpAuthorisation(addresses, authorisationID, signature, "", false, 1)));
    assertFalse(da1.equals(new DvpAuthorisation(addresses, authorisationID, signature, metadata, false, 1)));
    assertTrue(da1.equals(new DvpAuthorisation(addresses, authorisationID, signature, metadata, refused, 1)));

    da1.setPoaPublicKey(poaPublicKey);

    assertFalse(da1.equals(new DvpAuthorisation(addresses, authorisationID, signature, metadata, refused, 1)));

    da2 = new DvpAuthorisation(addresses, authorisationID, signature, metadata, refused, 1);
    da2.setPoaPublicKey(poaPublicKey);

    assertTrue(da1.equals(da2));

    // Hash

    // Hash object

    String sigMessage = da1.stringToHashToSign("contract");
    String hash = Hex.encode(SHA256.sha256(sigMessage.getBytes(StandardCharsets.UTF_8)));
    assertTrue(hash.equals("121d8f1703027baeeffc4be451d5e4e939a37ffbf64aa359cd3eec0b7b781ace"));

  }


  @Test
  public void dvpEncumbranceTest() throws Exception {

    boolean useEncumbrance = true;
    String encumbranceName = "myName";

    final DvpEncumbrance de1 = new DvpEncumbrance(useEncumbrance, encumbranceName);
    final DvpEncumbrance de2 = new DvpEncumbrance(new Object[]{useEncumbrance, encumbranceName});
    final DvpEncumbrance de3 = new DvpEncumbrance(new MPWrappedArrayImpl(new Object[]{useEncumbrance, encumbranceName}));
    final DvpEncumbrance de4 = new DvpEncumbrance(de1);
    final DvpEncumbrance de5 = new DvpEncumbrance(de1.encode(0));

    final DvpEncumbrance bad1 = new DvpEncumbrance(new Object[]{useEncumbrance});
    final DvpEncumbrance bad2 = new DvpEncumbrance(new MPWrappedArrayImpl(new Object[]{encumbranceName}));

    // equals

    assertTrue(de1.equals(de2));
    assertTrue(de1.equals(de3));
    assertTrue(de1.equals(de4));
    assertTrue(de1.equals(de5));
    assertTrue(de5.equals(de4));
    assertTrue(de5.equals(de3));
    assertTrue(de5.equals(de2));

    assertTrue(bad1.equals(bad2));

    assertFalse(de1.equals(null));
    assertFalse(de1.equals(""));
    assertFalse(de1.equals(new DvpEncumbrance(!useEncumbrance, "")));
    assertFalse(de1.equals(new DvpEncumbrance(useEncumbrance, "")));
    assertTrue(de1.equals(new DvpEncumbrance(useEncumbrance, encumbranceName)));
    assertTrue(de1.hashCode() == encumbranceName.hashCode());

  }


  @Test
  public void dvpEventTest() throws Exception {

    String[] events = new String[]{"event1", "event2"};
    Object[] eventso = new Object[]{"event1", "event2"};

    DvpEvent ev1 = new DvpEvent(events);
    DvpEvent ev2 = new DvpEvent(eventso);
    DvpEvent ev3 = new DvpEvent(ev1);
    DvpEvent ev4 = new DvpEvent(ev1.encode(0));

    assertTrue(ev1.equals(ev2));
    assertTrue(ev1.equals(ev3));
    assertTrue(ev1.equals(ev4));

    assertFalse(ev1.equals(null));
    assertFalse(ev1.equals("fred"));
    assertFalse(ev1.equals(new DvpEvent(new String[]{"event1"})));
    assertFalse(ev1.equals(new DvpEvent(new String[]{"event1", "event3"})));

    assertTrue(ev1.hashCode() == Arrays.hashCode(events));
  }


  @Test
  public void dvpParameter() throws Exception {

    String address = "Address";
    final String[] addresses = new String[]{"Address", "Address1"};
    Long valueNumber = 42L;
    final String valueString = "a + b";
    int index = 5;
    int contractSpecific = 6;
    int calculationOnly = 7;
    final String signature = "Sig";
    final String poaPublicKey = "poaKey";

    // Constructors :

    DvpParameter dvp1 = new DvpParameter(address, valueNumber, index, contractSpecific, calculationOnly, signature);

    assertTrue(dvp1.getAddress().equals(address));
    assertTrue(Arrays.equals(dvp1.getAddresses(), new String[]{address}));
    assertTrue(dvp1.getValueNumber().equals(valueNumber));
    assertTrue(dvp1.getValueString() == null);
    assertTrue(dvp1.calculatedIndex == index);
    assertTrue(dvp1.contractSpecific == contractSpecific);
    assertTrue(dvp1.calculationOnly == calculationOnly);
    assertTrue(dvp1.getSignature().equals(signature));
    assertTrue(dvp1.getPoaPublicKey().equals(""));

    dvp1.setPoaPublicKey(poaPublicKey);
    assertTrue(dvp1.getPoaPublicKey().equals(poaPublicKey));

    dvp1 = new DvpParameter(address, valueString, index, contractSpecific, calculationOnly, signature);

    assertTrue(dvp1.hashCode() == address.hashCode());
    assertTrue(dvp1.getAddress().equals(address));
    assertTrue(Arrays.equals(dvp1.getAddresses(), new String[]{address}));
    assertTrue(dvp1.getValueNumber() == null);
    assertTrue(dvp1.getValueString().equals(valueString));
    assertTrue(dvp1.calculatedIndex == index);
    assertTrue(dvp1.contractSpecific == contractSpecific);
    assertTrue(dvp1.calculationOnly == calculationOnly);
    assertTrue(dvp1.getSignature().equals(signature));
    assertTrue(dvp1.getPoaPublicKey().equals(""));

    dvp1.setPoaPublicKey(poaPublicKey);
    assertTrue(dvp1.getPoaPublicKey().equals(poaPublicKey));

    dvp1 = new DvpParameter(addresses, valueNumber, index, contractSpecific, calculationOnly, signature);

    assertTrue(dvp1.getAddress() == null);
    assertTrue(Arrays.equals(dvp1.getAddresses(), addresses));
    assertTrue(dvp1.getValueNumber().equals(valueNumber));
    assertTrue(dvp1.getValueString() == null);
    assertTrue(dvp1.calculatedIndex == index);
    assertTrue(dvp1.contractSpecific == contractSpecific);
    assertTrue(dvp1.calculationOnly == calculationOnly);
    assertTrue(dvp1.getSignature().equals(signature));
    assertTrue(dvp1.getPoaPublicKey().equals(""));

    dvp1.setPoaPublicKey(poaPublicKey);
    assertTrue(dvp1.getPoaPublicKey().equals(poaPublicKey));

    dvp1 = new DvpParameter(addresses, valueString, index, contractSpecific, calculationOnly, signature);

    assertTrue(dvp1.getAddress() == null);
    assertTrue(Arrays.equals(dvp1.getAddresses(), addresses));
    assertTrue(dvp1.getValueNumber() == null);
    assertTrue(dvp1.getValueString().equals(valueString));
    assertTrue(dvp1.calculatedIndex == index);
    assertTrue(dvp1.contractSpecific == contractSpecific);
    assertTrue(dvp1.calculationOnly == calculationOnly);
    assertTrue(dvp1.getSignature().equals(signature));
    assertTrue(dvp1.getPoaPublicKey().equals(""));

    dvp1.setPoaPublicKey(poaPublicKey);
    assertTrue(dvp1.getPoaPublicKey().equals(poaPublicKey));

    dvp1 = new DvpParameter("", 0L, 0, 0, 0, "");

    assertFalse(dvp1.getAddress().equals(address));
    assertFalse(Arrays.equals(dvp1.getAddresses(), new String[]{address}));
    assertFalse(dvp1.getValueNumber().equals(valueNumber));
    assertTrue(dvp1.getValueString() == null);
    assertFalse(dvp1.calculatedIndex == index);
    assertFalse(dvp1.contractSpecific == contractSpecific);
    assertFalse(dvp1.calculationOnly == calculationOnly);
    assertFalse(dvp1.getSignature().equals(signature));

    dvp1 = new DvpParameter("", 0L, index, contractSpecific, calculationOnly, "");

    dvp1.setAddress(address);
    dvp1.setValue(valueString);
    dvp1.setSignature(signature);
    dvp1.setPoaPublicKey(poaPublicKey);
    dvp1.setSignature(signature);

    assertTrue(dvp1.getAddress().equals(address));
    assertTrue(Arrays.equals(dvp1.getAddresses(), new String[]{address}));
    assertTrue(dvp1.getValueNumber() == null);
    assertTrue(dvp1.getValueString().equals(valueString));
    assertTrue(dvp1.calculatedIndex == index);
    assertTrue(dvp1.contractSpecific == contractSpecific);
    assertTrue(dvp1.calculationOnly == calculationOnly);
    assertTrue(dvp1.getSignature().equals(signature));
    assertTrue(dvp1.getPoaPublicKey().equals(poaPublicKey));

    dvp1.setValue(valueNumber);
    assertTrue(dvp1.getValueNumber().equals(valueNumber));
    assertNull(dvp1.getValueString());

    // Default data set.

    dvp1 = new DvpParameter(new Object[0]);

    assertTrue(dvp1.getAddress().equals(""));
    assertTrue(Arrays.equals(dvp1.getAddresses(), new String[]{""}));
    assertTrue(dvp1.getValueNumber() == null);
    assertTrue(dvp1.getValueString() == null);
    assertTrue(dvp1.calculatedIndex == 0);
    assertTrue(dvp1.contractSpecific == 0);
    assertTrue(dvp1.calculationOnly == 0);
    assertTrue(dvp1.getSignature().equals(""));
    assertTrue(dvp1.getPoaPublicKey().equals(""));

    // Copy

    dvp1 = new DvpParameter(addresses, valueString, index, contractSpecific, calculationOnly, signature);

    Object[] encoded = dvp1.encode(0);
    DvpParameter dvp2 = new DvpParameter(encoded);

    dvp1.setPoaPublicKey(poaPublicKey);
    dvp2.setPoaPublicKey(poaPublicKey);

    encoded = dvp1.encode(0);

    DvpParameter dvp3 = new DvpParameter(encoded);
    encoded[0] = new MPWrappedArrayImpl((Object[]) encoded[0]);
    DvpParameter dvp4 = new DvpParameter(encoded);
    final DvpParameter dvp5 = new DvpParameter(dvp1);

    assertTrue(dvp1.equals(dvp2));
    assertTrue(dvp1.equals(dvp3));
    assertTrue(dvp1.equals(dvp4));
    assertTrue(dvp1.equals(dvp5));

    // equals

    assertFalse(dvp1.equals(null));
    assertFalse(dvp1.equals("Not right"));

    dvp2 = new DvpParameter("", 0L, 0, 0, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(new String[]{address}, 0L, 0, 0, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(address, 0L, 0, 0, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(addresses, 0L, 0, 0, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(addresses, valueNumber, 0, 0, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(addresses, valueString, 0, 0, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(addresses, valueString, index, 0, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(addresses, valueString, index, contractSpecific, 0, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(addresses, valueString, index, contractSpecific, calculationOnly, "");
    assertFalse(dvp1.equals(dvp2));

    dvp2 = new DvpParameter(addresses, valueString, index, contractSpecific, calculationOnly, signature);
    assertFalse(dvp1.equals(dvp2));

    dvp2.setPoaPublicKey(poaPublicKey);
    assertTrue(dvp1.equals(dvp2));

    // Hash

    String sigMessage = dvp1.stringToHashToSign("keyname", "contract");
    String hash = Hex.encode(SHA256.sha256(sigMessage.getBytes(StandardCharsets.UTF_8)));
    assertTrue(hash.equals("0c91b025eb8acaa4bcfff65d2918f89c7ddadcb1c82f91a08cb5e523fcee5849"));

    dvp1 = new DvpParameter(addresses, valueString, index, 0, calculationOnly, signature);
    sigMessage = dvp1.stringToHashToSign("keyname", "contract");
    hash = Hex.encode(SHA256.sha256(sigMessage.getBytes(StandardCharsets.UTF_8)));
    assertTrue(hash.equals("bf34959e629f5de077051745b194b181344ee6cf3a504021c2c966851f83f8e5"));

  }


  @Test
  public void dvpParty() throws Exception {

    final String partyIdentifier = "p1";
    final String sigAddress = "Address1";
    final String publicKey = "PubKey1";
    final String signature = "Signature1";
    final boolean mustSign = true;
    final String nullString = null;

    final String recAddress1 = "RecAddress1";
    final String payAdd1 = "PayAddress1";
    final String payNS1 = "PayNS1";
    final String payClass1 = "PayClass1";
    final Long payAmount1 = 999888L;
    final String payPubKey1 = "PayPub1";
    final String paySignature1 = "PaySig1";
    final String payMeta = "PayMeta1";
    final String payEncumbrance = "PayEncumbrance1";
    final boolean payIsIssuance1 = true;

    final DvpPayItem payItem1 = new DvpPayItem(payAdd1, payNS1, payClass1, payAmount1, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance);
    final DvpReceiveItem recItem1 = new DvpReceiveItem(recAddress1, payNS1, payClass1, payAmount1);

    // Constructors :

    //  Default
    DvpParty party1 = new DvpParty(nullString, nullString, nullString, nullString, !mustSign);

    assertTrue(party1.partyIdentifier.equals(""));
    assertTrue(party1.sigAddress.equals(""));
    assertTrue(party1.publicKey.equals(""));
    assertTrue(party1.signature.equals(""));
    assertTrue(party1.mustSign == (!mustSign));
    assertTrue(party1.hashCode() == party1.sigAddress.hashCode());

    party1 = new DvpParty(new MPWrappedArrayImpl(new Object[5]));

    assertTrue(party1.partyIdentifier.equals(""));
    assertTrue(party1.sigAddress.equals(""));
    assertTrue(party1.publicKey.equals(""));
    assertTrue(party1.signature.equals(""));
    assertTrue(party1.mustSign == (false));

    party1 = new DvpParty(new Object[0]);

    assertTrue(party1.partyIdentifier.equals(""));
    assertTrue(party1.sigAddress.equals(""));
    assertTrue(party1.publicKey.equals(""));
    assertTrue(party1.signature.equals(""));
    assertTrue(party1.mustSign == (false));

    //  Simple
    party1 = new DvpParty(partyIdentifier, sigAddress, publicKey, signature, mustSign);

    assertTrue(party1.partyIdentifier.equals(partyIdentifier));
    assertTrue(party1.sigAddress.equals(sigAddress));
    assertTrue(party1.publicKey.equals(publicKey));
    assertTrue(party1.signature.equals(signature));
    assertTrue(party1.mustSign == mustSign);

    party1.payList.add(payItem1);
    party1.receiveList.add(recItem1);

    DvpParty party2 = new DvpParty(party1);
    DvpParty party3 = new DvpParty(party1.encode(0));
    DvpParty party4 = new DvpParty((Object) (party1.encode(0)));
    final DvpParty party5 = new DvpParty((Object) (new MPWrappedArrayImpl(party1.encode(0))));
    final DvpParty party6 = new DvpParty((Object) (Arrays.asList(party1.encode(0))));

    assertTrue(party1.equals(party2));
    assertTrue(party1.equals(party3));
    assertTrue(party1.equals(party4));
    assertTrue(party1.equals(party5));
    assertTrue(party1.equals(party6));
    assertTrue(party6.equals(party5));
    assertTrue(party6.equals(party4));
    assertTrue(party6.equals(party3));
    assertTrue(party6.equals(party2));
    assertTrue(party6.equals(party1));

    // equals;

    party2 = null;
    assertFalse(party1.equals(party2));
    assertFalse(party1.equals("Not a Party"));
    assertFalse(party1.equals(new DvpParty(new Object[0])));
    assertFalse(party1.equals(new DvpParty(partyIdentifier, "", publicKey, signature, mustSign)));
    assertFalse(party1.equals(new DvpParty(partyIdentifier, sigAddress, "", signature, mustSign)));
    assertFalse(party1.equals(new DvpParty(partyIdentifier, sigAddress, publicKey, "", mustSign)));
    assertFalse(party1.equals(new DvpParty(partyIdentifier, sigAddress, publicKey, signature, !mustSign)));

    party2 = new DvpParty(partyIdentifier, sigAddress, publicKey, signature, mustSign);
    assertFalse(party1.equals(party2));

    party2.payList.add(payItem1);
    assertFalse(party1.equals(party2));

    party2.receiveList.add(recItem1);
    assertTrue(party1.equals(party2));

  }


  @Test
  public void dvpPayitem() throws Exception {

    final String recAddress1 = "RecAddress1";
    final String payAdd1 = "PayAddress1";
    final String payNS1 = "PayNS1";
    final String payClass1 = "PayClass1";
    final Long payAmount1 = 999888L;
    final String payString = "1 + 1";
    final String payStringNumeric = "42";
    final String payPubKey1 = "PayPub1";
    final String paySignature1 = "PaySig1";
    final String payMeta = "PayMeta1";
    final String payEncumbrance = "PayEncumbrance1";
    final boolean payIsIssuance1 = true;
    final String nullString = null;

    // 'Default' Constructor
    DvpPayItem payItem1 = new DvpPayItem(new Object[0]);

    assertTrue(payItem1.address.equals(""));
    assertTrue(payItem1.namespace.equals(""));
    assertTrue(payItem1.classID.equals(""));
    assertTrue(payItem1.amountString == null);
    assertTrue(payItem1.amountNumber.equals(0L));
    assertTrue(payItem1.publicKey.equals(""));
    assertTrue(payItem1.signature.equals(""));
    assertTrue(payItem1.issuance == false);
    assertTrue(payItem1.metadata.equals(""));
    assertTrue(payItem1.encumbrance == null);
    assertFalse(payItem1.isSigned());

    // nulls
    payItem1 = new DvpPayItem(nullString, nullString, nullString, nullString, nullString, nullString, false, nullString, nullString);

    assertTrue(payItem1.address == null);
    assertTrue(payItem1.namespace.equals(""));
    assertTrue(payItem1.classID.equals(""));
    assertTrue(payItem1.amountString == null);
    assertTrue(payItem1.amountNumber.equals(0L));
    assertTrue(payItem1.publicKey == null);
    assertTrue(payItem1.signature == null);
    assertTrue(payItem1.issuance == false);
    assertTrue(payItem1.metadata.equals(""));
    assertTrue(payItem1.encumbrance == null);

    // Normal Amount
    payItem1 = new DvpPayItem(payAdd1, payNS1, payClass1, payAmount1, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance);

    assertTrue(payItem1.address.equals(payAdd1));
    assertTrue(payItem1.namespace.equals(payNS1));
    assertTrue(payItem1.classID.equals(payClass1));
    assertTrue(payItem1.amountString == null);
    assertTrue(payItem1.amountNumber.equals(payAmount1));
    assertTrue(payItem1.publicKey.equals(payPubKey1));
    assertTrue(payItem1.signature.equals(paySignature1));
    assertTrue(payItem1.issuance == payIsIssuance1);
    assertTrue(payItem1.metadata.equals(payMeta));
    assertTrue(payItem1.encumbrance.equals(payEncumbrance));
    assertTrue(payItem1.getFullAssetID().equals(payNS1 + "|" + payClass1));
    assertTrue(payItem1.isSigned());

    assertTrue(payItem1.equals(new DvpPayItem(payItem1)));
    assertTrue(payItem1.compareTo(new DvpPayItem(payItem1)) == 0);

    // Test Encode with null encumbrance (it's different).
    payItem1 = new DvpPayItem(payAdd1, payNS1, payClass1, payAmount1, payPubKey1, paySignature1, payIsIssuance1, payMeta, null);
    assertTrue(payItem1.equals(new DvpPayItem(payItem1.encode(0))));

    // String Amount (numeric)
    payItem1 = new DvpPayItem(payAdd1, payNS1, payClass1, payStringNumeric, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance);
    assertTrue(payItem1.getAmount() instanceof Number);
    assertTrue(((Number) payItem1.getAmount()).equals(Long.parseLong(payStringNumeric)));

    // String Amount
    payItem1 = new DvpPayItem(payAdd1, payNS1, payClass1, payString, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance);

    assertTrue(payItem1.address.equals(payAdd1));
    assertTrue(payItem1.namespace.equals(payNS1));
    assertTrue(payItem1.classID.equals(payClass1));
    assertTrue(payItem1.getAmount() instanceof String);
    assertTrue(payItem1.getAmount().equals(payString));
    assertTrue(payItem1.amountNumber == null);
    assertTrue(payItem1.publicKey.equals(payPubKey1));
    assertTrue(payItem1.signature.equals(paySignature1));
    assertTrue(payItem1.issuance == payIsIssuance1);
    assertTrue(payItem1.metadata.equals(payMeta));
    assertTrue(payItem1.encumbrance.equals(payEncumbrance));

    // Copy

    DvpPayItem payItem2 = new DvpPayItem(payItem1);
    DvpPayItem payItem3 = new DvpPayItem(payItem1.encode(0));

    assertTrue(payItem1.equals(payItem2));
    assertTrue(payItem1.compareTo(new DvpPayItem(payItem1)) == 0);
    assertTrue(payItem1.equals(payItem3));
    assertTrue(payItem2.compareTo(payItem1) == 0);

    //

    assertFalse(payItem1.equals(null));
    assertFalse(payItem1.equals("XXX"));
    assertFalse(payItem1.equals(new DvpPayItem(new Object[0])));
    assertFalse(payItem1.equals(new DvpPayItem("", payNS1, payClass1, payString, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, "", payClass1, payString, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, payNS1, "", payString, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, payNS1, payClass1, "", payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, payNS1, payClass1, payString, "", paySignature1, payIsIssuance1, payMeta, payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, payNS1, payClass1, payString, payPubKey1, "", payIsIssuance1, payMeta, payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, payNS1, payClass1, payString, payPubKey1, paySignature1, !payIsIssuance1, payMeta, payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, payNS1, payClass1, payString, payPubKey1, paySignature1, payIsIssuance1, "", payEncumbrance)));
    assertFalse(payItem1.equals(new DvpPayItem(payAdd1, payNS1, payClass1, payString, payPubKey1, paySignature1, payIsIssuance1, payMeta, "")));
    assertTrue(payItem1.equals(new DvpPayItem(payAdd1, payNS1, payClass1, payString, payPubKey1, paySignature1, payIsIssuance1, payMeta, payEncumbrance)));

    // Hash

    String sigMessage = computeHash(payItem1.objectToHashToSign("contract"));
    String hash = Hex.encode(SHA256.sha256(sigMessage.getBytes(StandardCharsets.UTF_8)));
    assertTrue(hash.equals("9a378315751b572ae4160eca47294cf99649bca2c706dec9b1076f713d3d3ddc"));

  }


  @Test
  public void dvpReceiveitem() throws Exception {

    final String recAddress1 = "RecAddress1";
    final String payNS1 = "PayNS1";
    final String payClass1 = "PayClass1";
    final Long payAmount1 = 999888L;
    final String payString = "1 + 1";
    final String payStringNumeric = "42";
    final String nullString = null;

    // 'Default' Constructor
    DvpReceiveItem recItem1 = new DvpReceiveItem(new Object[0]);

    assertTrue(recItem1.address.equals(""));
    assertTrue(recItem1.namespace.equals(""));
    assertTrue(recItem1.classID.equals(""));
    assertTrue(recItem1.amountString == null);
    assertTrue(recItem1.amountNumber.equals(0L));

    recItem1 = new DvpReceiveItem(nullString, nullString, nullString, nullString);

    assertTrue(recItem1.address == null);
    assertTrue(recItem1.namespace.equals(""));
    assertTrue(recItem1.classID.equals(""));
    assertTrue(recItem1.amountString == null);
    assertTrue(recItem1.amountNumber.equals(0L));

    // Normal Amount
    recItem1 = new DvpReceiveItem(recAddress1, payNS1, payClass1, payAmount1);

    assertTrue(recItem1.address.equals(recAddress1));
    assertTrue(recItem1.namespace.equals(payNS1));
    assertTrue(recItem1.classID.equals(payClass1));
    assertTrue(recItem1.amountString == null);
    assertTrue(recItem1.amountNumber.equals(payAmount1));
    assertTrue(recItem1.getAmount().equals(payAmount1));
    assertTrue(recItem1.getFullAssetID().equals(payNS1 + "|" + payClass1));
    assertTrue(recItem1.equals(new DvpReceiveItem(recItem1.encode(0))));

    // String Amount
    recItem1 = new DvpReceiveItem(recAddress1, payNS1, payClass1, payStringNumeric);
    assertTrue(recItem1.getAmount() instanceof Number);
    assertTrue(((Number) recItem1.getAmount()).equals(Long.parseLong(payStringNumeric)));

    // String Amount
    recItem1 = new DvpReceiveItem(recAddress1, payNS1, payClass1, payString);

    assertTrue(recItem1.address.equals(recAddress1));
    assertTrue(recItem1.namespace.equals(payNS1));
    assertTrue(recItem1.classID.equals(payClass1));
    assertTrue(recItem1.getAmount() instanceof String);
    assertTrue(recItem1.getAmount().equals(payString));
    assertTrue(recItem1.amountNumber == null);
    assertTrue(recItem1.equals(new DvpReceiveItem(recItem1.encode(0))));

    DvpReceiveItem recItem2 = new DvpReceiveItem(recItem1);
    DvpReceiveItem recItem3 = new DvpReceiveItem(recItem1.encode(0));

    assertTrue(recItem1.equals(recItem2));
    assertTrue(recItem1.equals(recItem3));
    assertTrue(recItem3.equals(recItem2));

    // Equals

    assertFalse(recItem1.equals(nullString));
    assertFalse(recItem1.equals("XXX"));
    assertFalse(recItem1.equals(new DvpReceiveItem("", payNS1, payClass1, payString)));
    assertFalse(recItem1.equals(new DvpReceiveItem(recAddress1, "", payClass1, payString)));
    assertFalse(recItem1.equals(new DvpReceiveItem(recAddress1, payNS1, "", payString)));
    assertFalse(recItem1.equals(new DvpReceiveItem(recAddress1, payNS1, payClass1, "")));
    assertTrue(recItem1.equals(new DvpReceiveItem(recAddress1, payNS1, payClass1, payString)));

    recItem1 = new DvpReceiveItem(recAddress1, payNS1, payClass1, payAmount1);
    assertFalse(recItem1.equals(new DvpReceiveItem(recAddress1, payNS1, payClass1, "")));
    assertFalse(recItem1.equals(new DvpReceiveItem(recAddress1, payNS1, payClass1, payAmount1 + 1)));
    assertTrue(recItem1.equals(new DvpReceiveItem(recAddress1, payNS1, payClass1, payAmount1)));

  }


  @Test
  public void dvpUkContractDataEquals() throws Exception {

    DvpUkContractData testData = getTestData1();

    assertTrue(!testData.equals(null));
    assertTrue(!testData.equals("Not a DvpUkContractData Object"));

    // Just for coverage !
    testData.toFriendlyJSON();
    testData.toJSON();

    // OK
    HashMap<String, Object> templateMap = new HashMap<>();

    DvpUkContractData duffData = new DvpUkContractData(new HashMap<>());

    assertTrue(!testData.equals(duffData));

    templateMap.put("__status", null); // not used in .equals.

    templateMap.put("__function", contractFunction);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.get__function().equals(testData.get__function()));
    assertTrue(!testData.equals(duffData));

    templateMap.put("__completed", isCompleted);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.get__completed() == testData.get__completed());
    assertTrue(!testData.equals(duffData));

    templateMap.put("__address", contractAddress);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.get__address().equals(testData.get__address()));
    assertTrue(!testData.equals(duffData));

    templateMap.put("__timeevent", nextTimeEvent);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.get__timeevent() == testData.get__timeevent());
    assertTrue(!testData.equals(duffData));

    templateMap.put("issuingaddress", issuingAddress);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.getIssuingaddress().equals(testData.getIssuingaddress()));
    assertTrue(!testData.equals(duffData));

    templateMap.put("startdate", startDate);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.getStartdate().equals(testData.getStartdate()));
    assertTrue(!testData.equals(duffData));

    templateMap.put("expiry", expiryDate);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.getExpiry().equals(testData.getExpiry()));
    assertTrue(!testData.equals(duffData));

    templateMap.put("protocol", protocol);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.getProtocol().equals(testData.getProtocol()));
    assertTrue(!testData.equals(duffData));

    templateMap.put("metadata", metadata);
    duffData = new DvpUkContractData(templateMap);
    assertTrue(duffData.getMetadata().equals(testData.getMetadata()));
    assertTrue(!testData.equals(duffData));

    duffData.setParties(new ArrayList<>());
    for (DvpParty thisParty : testData.getParties()) {
      duffData.getParties().add(thisParty);
    }
    assertTrue(!testData.equals(duffData));

    duffData.setAuthorisations(new ArrayList<>());
    for (DvpAuthorisation thisOne : testData.getAuthorisations()) {
      duffData.addAuthorisation(thisOne);
    }
    assertTrue(!testData.equals(duffData));

    duffData.setParameters(new HashMap<>());
    final Map<String, DvpParameter> duffparams = duffData.getParameters();
    testData.getParameters().forEach(duffparams::put);
    assertTrue(!testData.equals(duffData));

    duffData.setAddencumbrances(new ArrayList<>());
    for (DvpAddEncumbrance thisOne : testData.getAddencumbrances()) {
      duffData.addAddEncumbrance(thisOne);
    }
    assertTrue(!testData.equals(duffData));

    duffData.setEvents(testData.getEvents());
    assertTrue(!testData.equals(duffData));

    duffData.setEncumbrance(testData.getEncumbrance());

    // TaDa !
    assertTrue(testData.equals(duffData));

    assertFalse(verifyPublicKey("1a2b3c"));
    assertTrue(verifyPublicKey(
        "1a2b3c4d5e6f77881a2b3c4d5e6f77881a2b3c4d5e6f77881a2b3c4d5e6f7788"));
    assertFalse(verifyPublicKey(
        "1a2b3c4d5e6f77881a2b3c4d5e6f77881a2b3c4d5e6f77881a2b3c4d5e6f778"));
    assertFalse(verifyPublicKey(
        "1a2b3c4d5e6f77881a2b3c4d5e6f77881a2b3c4d5e6f77881a2b3c4d5e6f77889"));
    assertTrue(verifyPublicKey(
        "3059301306072a8648ce3d020106082a8648ce3d030107034200041c92f671add14f15883aefd422d91904dc8df469993191e6dfdc38146a89c705737f7924c5f51c8e02503b65b9e6"
            + "4286098662e5788f3a5117bc8a113d3795b5"));
    // Addresses
    Set<String> theseAddresses = testData.addresses();

    // no valid addresses or public keys in the test data !
    assertTrue(theseAddresses.size() == 0);
  }


  @Test
  public void encode() throws Exception {

    DvpUkContractData testData = getTestData1();

    MPWrappedMap<String, Object> encoded = testData.encode();
    final DvpUkContractData testDataCopy1 = new DvpUkContractData(encoded);
    final DvpUkContractData testDataCopy2 = new DvpUkContractData(testData.encodeToMap());
    final DvpUkContractData testDataCopy3 = new DvpUkContractData(testData);
    DvpUkContractData testDataNull = null;
    final DvpUkContractData testDataCopy4 = new DvpUkContractData(testDataNull);
    final DvpUkContractData testDataCopy5 = (DvpUkContractData) testData.copy();

    assertTrue(!testDataCopy1.equals(testDataCopy4));

    assertTrue(testDataCopy1.equals(testData));
    assertTrue(testData.equals(testDataCopy1));
    assertTrue(testDataCopy2.equals(testData));
    assertTrue(testData.equals(testDataCopy2));
    assertTrue(testDataCopy2.equals(testDataCopy1));
    assertTrue(testDataCopy2.equals(testDataCopy3));
    assertTrue(testDataCopy1.equals(testDataCopy2));
    assertTrue(testDataCopy1.equals(testDataCopy3));
    assertTrue(testDataCopy1.equals(testDataCopy5));
    assertTrue(testDataCopy3.equals(testDataCopy1));
    assertTrue(testDataCopy3.equals(testDataCopy2));

    String j1 = testData.toJSON().toJSONString();

    assertTrue(testData.toJSON().toJSONString().equals(testDataCopy1.toJSON().toJSONString()));
    assertTrue(testDataCopy1.toJSON().toJSONString().equals(testDataCopy2.toJSON().toJSONString()));
    assertTrue(testDataCopy1.toJSON().toJSONString().equals(testDataCopy3.toJSON().toJSONString()));

    //

    testData = getTestData1();

    testData.set__status("X");
    assertTrue(testData.get__status().equals("X"));
    testData.set__status("X");
    assertTrue(testData.get__status().equals("X" + "\n" + "X"));

    //

    assertTrue(testData.get__timeevent() != 69L);
    testData.set__timeevent(69L);
    assertTrue(testData.get__timeevent() == 69L);

    testData.setNextTimeEvent(70L, true);
    assertTrue(testData.get__timeevent() == 70L);

    testData.setNextTimeEvent(70L, false);
    assertTrue(testData.get__timeevent() == testData.getExpiry());

    testData.set__completed(true);
    testData.setNextTimeEvent(70L, false);
    assertTrue(testData.get__timeevent() == (70L + DVP_DELETE_DELAY_ON_COMPLETE));

    //

    assertFalse(testData.get__function().equals("fred"));
    testData.set__function("fred");
    assertTrue(testData.get__function().equals("fred"));
    assertTrue(testData.getContractType().equals("fred"));

    //

    testData.set__completed(false);
    assertTrue(testData.get__completed() == 0);
    testData.set__completed(true);
    assertTrue(testData.get__completed() != 0);
    testData.set__completed(false);
    assertTrue(testData.get__completed() == 0);

    //

    assertTrue(testData.get__canceltime() != 69L);
    testData.set__canceltime(69L);
    assertTrue(testData.get__canceltime() == 69L);

    //

    assertFalse(testData.get__address().equals("fred"));
    testData.set__address("fred");
    assertTrue(testData.get__address().equals("fred"));
    testData.set__address(null);
    assertTrue(testData.get__address().equals(""));

    //

    assertFalse(testData.getIssuingaddress().equals("fred"));
    testData.setIssuingaddress("fred");
    assertTrue(testData.getIssuingaddress().equals("fred"));
    testData.setIssuingaddress(null);
    assertNull(testData.getIssuingaddress());

    //

    testData.setAutosign(false);
    assertFalse(testData.getAutosign());
    testData.setAutosign(true);
    assertTrue(testData.getAutosign());
    testData.setAutosign(false);
    assertFalse(testData.getAutosign());

    //

    testData = getTestData1();

    final List<DvpParty> parties = testData.getParties();

    DvpUkContractData testData2 = getTestData1();

    testData2.setParties(new ArrayList<>());

    assertTrue(testData2.getParties().size() == 0);
    assertFalse(testData.equals(testData2));

    testData2.setParties(parties);
    assertTrue(testData.equals(testData2));
    testData2.setParties(new ArrayList<>());


  }


  private DvpUkContractData getTestData1() {

    DvpParty party1 = new DvpParty(
        partyIdentifier1,
        sigAddress1,
        publicKey1,
        signature1,
        mustSign1
    );

    party1.payList.add(new DvpPayItem(
        "payadd1",
        "payNs1",
        "payClass",
        "43",
        "payPubKey1",
        "paySig1",
        true,
        "meta1",
        "enc1"

    ));

    party1.receiveList.add(new DvpReceiveItem(
        "payadd1",
        "payNs2",
        "payClass2",
        "432"
    ));

    DvpParty party2 = new DvpParty(
        partyIdentifier2,
        sigAddress2,
        publicKey2,
        signature2,
        mustSign2
    );

    party2.payList.add(new DvpPayItem(
        "payadd2",
        "payNs2",
        "payClass2",
        432L,
        "payPubKey2",
        "paySig2",
        true,
        "meta2",
        "enc2"
    ));

    party1.receiveList.add(new DvpReceiveItem(
        "payadd2",
        "payNs1",
        "payClass1",
        "43"
    ));

    DvpUkContractData testData = new DvpUkContractData(
        contractAddress,
        isCompleted,
        contractFunction,
        nextTimeEvent,
        startDate,
        expiryDate,
        issuingAddress,
        protocol,
        metadata,
        events,
        encumbranceToUse
    );

    testData.addParty(party1);
    testData.addParty(party2);

    DvpAddEncumbrance yy = new DvpAddEncumbrance("AssetID", "Reference", 100L, "PubKey", "sig5");
    yy.beneficiaries.add(new EncumbranceDetail("Address", 0L, 999999L));

    testData.addAddEncumbrance(yy);

    testData.addAuthorisation(new DvpAuthorisation("Address", "AuthID", "Address2", "AuthMeta", false, 1));
    testData.addParameter("P1", new DvpParameter("AddressP", 7L, 0, 0, 0, ""));

    return testData;
  }


  /**
   * testTest.
   */
  @Test
  public void testDvpPayItem() {

    DvpPayItem p2;
    DvpPayItem p1 = new DvpPayItem(
        "payadd1",
        "payNs1",
        "payClass",
        "43",
        "payPubKey1",
        "paySig1",
        true,
        "meta1",
        "enc1"
    );

    assertTrue(p1.compareTo(null) > 0);

    p2 = new DvpPayItem(
        null,
        "payNs1",
        "payClass",
        "43",
        "payPubKey1",
        "paySig1",
        true,
        "meta1",
        "enc1"
    );

    assertTrue(p1.compareTo(p2) > 0);
    assertTrue(p2.compareTo(p1) < 0);

    p2.address = "payadd0";
    assertTrue(p1.compareTo(p2) > 0);
    assertTrue(p2.compareTo(p1) < 0);
    p2.address = "payadd1";

    //

    p1.amountNumber = new Balance(42L);
    p2.amountNumber = null;
    assertTrue(p1.compareTo(p2) > 0);
    assertTrue(p2.compareTo(p1) < 0);
    p2.amountNumber = new Balance(43L);
    assertTrue(p1.compareTo(p2) < 0);
    assertTrue(p2.compareTo(p1) > 0);
    p1.amountNumber = null;
    p2.amountNumber = null;

    //

  }


  /**
   * testTest.
   */
  @Test
  public void testTest() {

    // Required for DvP Contracts (Signature checking).

    //  Testing return value from getValue works OK (Object, could be String or Number).
    final DvpParameter dvp1 = new DvpParameter("address1", 42, 0, 1, 0, "sig");
    final DvpParameter dvp2 = new DvpParameter("address2", "44", 0, 1, 0, "sig");
    final DvpParameter dvp3 = new DvpParameter("address3", "44 + 1", 0, 1, 0, "sig");

    assertTrue("address1key42".equals(String.format("%s%s%s", dvp1.getAddress(), "key", dvp1.getValue())));
    assertTrue("address1key42".equals(String.format("%s%s%s", dvp1.getAddress(), "key", dvp1.getValueNumber())));
    assertTrue("address2key44".equals(String.format("%s%s%s", dvp2.getAddress(), "key", dvp2.getValue())));
    assertTrue("address2key44".equals(String.format("%s%s%s", dvp2.getAddress(), "key", dvp2.getValueNumber())));
    assertTrue("address3key44 + 1".equals(String.format("%s%s%s", dvp3.getAddress(), "key", dvp3.getValue())));
    assertTrue("address3key44 + 1".equals(String.format("%s%s%s", dvp3.getAddress(), "key", dvp3.getValueString())));
  }


  /**
   * testTest.
   */
  public void testTest2() throws Exception {

    // Ad-hoc test function.

    String json = "";

    JSONParser parser = new JSONParser();

    Object obj = parser.parse(json);

    JSONObject jsonObject = (JSONObject) obj;

    JSONObject jsonObject1 = (JSONObject) jsonObject.get("messagebody");
    JSONObject contractdata = (JSONObject) jsonObject1.get("contractdata");

    DvpUkContractData dd = new DvpUkContractData(contractdata);

  }

}
