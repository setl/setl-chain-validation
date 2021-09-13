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

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.crypto.KeyGen;
import io.setl.crypto.KeyGen.Type;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitAuthorise;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitCancel;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitEncumbrance;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParameter;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitParty;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitReceive;
import io.setl.bc.pychain.state.tx.contractdataclasses.DvpUKCommitData.DvpCommitment;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.crypto.MessageSignerVerifier;
import io.setl.crypto.MessageVerifierFactory;
import io.setl.common.Hex;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class DvpUKCommitDataTest {
  // Party 1
  String partyIdentifier1 = "party1";
  KeyPair partyKey1 = Type.ED25519.generate();
  String party1Address = AddressUtil.publicKeyToAddress(partyKey1.getPublic(), AddressType.NORMAL);
  String publicKey1 = Hex.encode(partyKey1.getPublic().getEncoded());
  PrivateKey privateKey1 = partyKey1.getPrivate();

  // AddressUtil.verifyAddress(party1Address);

  boolean mustSign1 = false;

  // Party 2
  String partyIdentifier2 = "party2";
  KeyPair partyKey2 = Type.ED25519.generate();
  String party2Address = AddressUtil.publicKeyToAddress(partyKey2.getPublic(), AddressType.NORMAL);
  String publicKey2 = Hex.encode(partyKey2.getPublic().getEncoded());
  PrivateKey privateKey2 = partyKey2.getPrivate();

  @Before
  public void setUp() throws Exception {

  }

  @After
  public void tearDown() throws Exception {

  }

  private DvpUKCommitData getCommitData1(String contractAddress) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpUKCommitData rVal = new DvpUKCommitData(new Object[] {});

    rVal.setParty(new DvpCommitParty(partyIdentifier1, publicKey1, verifier.createSignatureB64(contractAddress, privateKey1)));

    rVal.setCommitment(
        new DvpCommitment(
            0L,
            publicKey1,
            "Signature1"
        )
    );

    return rVal;
  }

  private DvpUKCommitData getCommitData2(String contractAddress) {

    MessageSignerVerifier verifier = MessageVerifierFactory.get();

    DvpUKCommitData rVal = new DvpUKCommitData(new Object[] {});

    rVal.setParty(new DvpCommitParty(partyIdentifier2, publicKey2, verifier.createSignatureB64(contractAddress, privateKey2)));

    rVal.setCommitment(
        new DvpCommitment(
            0L,
            publicKey2,
            "Signature2"
        )
    );

    DvpCommitReceive rr = new DvpCommitReceive(0L, "Address");
    rVal.setReceive(rr);

    DvpCommitEncumbrance yy = new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 100L, "Signature3");
    rVal.setEncumbrance(yy);

    DvpCommitAuthorise aa = new DvpCommitAuthorise(publicKey1, "AuthID", "Signature4", "AuthMeta", false, 1);
    rVal.setAuthorise(aa);

    DvpCommitParameter pp = new DvpCommitParameter("P1", 7L, 0, publicKey1, "Signature5");
    rVal.setParameter(pp);

    // rVal.setCancel(new DvpCommitCancel("PublicKey", "ThisSignature"));

    return rVal;
  }


  @Test
  public void equals() throws Exception {

    final MessageSignerVerifier verifier = MessageVerifierFactory.get();
    String contractAddress = "MyAddress";

    DvpUKCommitData testData1 = getCommitData2(contractAddress);
    DvpUKCommitData testData2 = new DvpUKCommitData(new Object[] {});

    assertTrue(!testData1.equals(null));
    assertTrue(!testData1.equals("Fred"));
    assertTrue(!testData1.equals(testData2));

    testData2.setParty(new DvpCommitParty(partyIdentifier2, publicKey2, verifier.createSignatureB64(contractAddress, privateKey2)));
    assertTrue(!testData1.equals(testData2));

    testData2.setCommitment(
        new DvpCommitment(
            0L,
            publicKey2,
            "Signature2"
        )
    );
    assertTrue(!testData1.equals(testData2));

    DvpCommitAuthorise aa = new DvpCommitAuthorise(publicKey1, "AuthID", "Signature4", "AuthMeta", false, 1);
    testData2.setAuthorise(aa);
    assertTrue(!testData1.equals(testData2));

    DvpCommitParameter pp = new DvpCommitParameter("P1", 7L, 0, publicKey1, "Signature5");
    testData2.setParameter(pp);
    assertTrue(!testData1.equals(testData2));

    DvpCommitReceive rr = new DvpCommitReceive(0L, "Address");
    testData2.setReceive(rr);
    assertTrue(!testData1.equals(testData2));

    DvpCommitEncumbrance yy = new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 100L, "Signature3");
    testData2.setEncumbrance(yy);
    assertTrue(testData1.equals(testData2));

    //testData2.setCancel(new DvpCommitCancel("PublicKey", "ThisSignature"));
    //assertTrue(testData1.equals(testData2));

    testData2.setParty(new DvpCommitParty("xxx", publicKey2, verifier.createSignatureB64(contractAddress, privateKey2)));
    assertTrue(!testData1.equals(testData2));

  }

  @Test
  public void encode() throws Exception {

    DvpUKCommitData testData1 = getCommitData1("MyAddress");

    MPWrappedMap<String, Object> encoded = testData1.encode();
    DvpUKCommitData testData2 = new DvpUKCommitData(encoded);
    DvpUKCommitData testData3 = new DvpUKCommitData(testData1);

    assertTrue(testData1.equals(testData2));
    assertTrue(testData1.equals(testData3));
    assertTrue(testData3.equals(testData2));

    testData1 = getCommitData2("MyAddress");

    encoded = testData1.encode();
    testData2 = new DvpUKCommitData(encoded);
    testData3 = new DvpUKCommitData(testData1);

    assertTrue(testData1.equals(testData2));
    assertTrue(testData1.equals(testData3));
    assertTrue(testData3.equals(testData2));

    String j1 = testData1.toJSON().toJSONString();

    assertTrue(testData1.toJSON().toJSONString().equals(testData2.toJSON().toJSONString()));
    assertTrue(testData1.toJSON().toJSONString().equals(testData3.toJSON().toJSONString()));
  }

  @Test
  public void dvpCommitAuthorise() throws Exception {

    DvpCommitAuthorise auth1 = new DvpCommitAuthorise(publicKey1, "AuthID", "Signature4", "AuthMeta", false, 1);
    DvpCommitAuthorise auth2 = new DvpCommitAuthorise(auth1);
    DvpCommitAuthorise auth3 = new DvpCommitAuthorise(auth1.encode(0));
    DvpCommitAuthorise auth4 = new DvpCommitAuthorise(new MPWrappedArrayImpl(auth1.encode(0)));

    assertTrue(auth1.equals(auth2));
    assertTrue(auth1.equals(auth3));
    assertTrue(auth1.equals(auth4));
    assertTrue(auth4.equals(auth3));
    assertTrue(auth4.equals(auth2));
    assertTrue(auth4.equals(auth1));

    assertFalse(auth4.equals(null));
    assertFalse(auth4.equals("fred"));
    assertFalse(auth4.equals(new DvpCommitAuthorise(publicKey1, "BadAuthID", "Signature4", "AuthMeta", false, 1)));
    assertFalse(auth4.equals(new DvpCommitAuthorise(publicKey2, "AuthID", "Signature4", "AuthMeta", false, 1)));
    assertFalse(auth4.equals(new DvpCommitAuthorise(publicKey1, "AuthID", "BadSignature4", "AuthMeta", false, 1)));
    assertFalse(auth4.equals(new DvpCommitAuthorise(publicKey1, "AuthID", "Signature4", "BadAuthMeta", false, 1)));

    assertTrue(auth4.getAddress().contains(party1Address));
    assertFalse(auth4.getAddress().contains(party2Address));

    assertTrue(auth4.stringToHashToSign("ca").equals("caAuthID_0"));

    auth1.encodeJson();

  }

  @Test
  public void dvpCommitCancel() throws Exception {

    DvpCommitCancel object1 = new DvpCommitCancel(publicKey1, "Signature4");
    DvpCommitCancel object2 = new DvpCommitCancel(object1);
    DvpCommitCancel object3 = new DvpCommitCancel(object1.encode(0L));
    DvpCommitCancel object4 = new DvpCommitCancel(new MPWrappedArrayImpl(object1.encode(0L)));

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));

    assertFalse(object1.equals(null));
    assertFalse(object1.equals("fred"));
    assertFalse(object1.equals(new DvpCommitCancel(publicKey2, "Signature4")));
    assertFalse(object1.equals(new DvpCommitCancel(publicKey1, "Signature5")));

    assertTrue(object1.stringToHashToSign("ca").equals("cancel_ca"));

    object1.encodeJson();

  }

  @Test
  public void dvpCommitEncumbrance() throws Exception {

    final DvpCommitEncumbrance object1 = new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 100L, "Signature3");
    final DvpCommitEncumbrance object2 = new DvpCommitEncumbrance(object1);
    final DvpCommitEncumbrance object3 = new DvpCommitEncumbrance(object2.encode(0L));
    final DvpCommitEncumbrance object4 = new DvpCommitEncumbrance(new MPWrappedArrayImpl(object3.encode(0L)));

    object1.encodeJson();

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));

    assertFalse(object1.equals(null));
    assertFalse(object1.equals("fred"));
    assertFalse(object1.equals(new DvpCommitEncumbrance(publicKey2, "AssetID", "Reference", 100L, "Signature3")));
    assertFalse(object1.equals(new DvpCommitEncumbrance(publicKey1, "BadAssetID", "Reference", 100L, "Signature3")));
    assertFalse(object1.equals(new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 100L, "BadSignature3")));
    assertFalse(object1.equals(new DvpCommitEncumbrance(publicKey1, "AssetID", "BadReference", 100L, "Signature3")));
    assertFalse(object1.equals(new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 200L, "Signature3")));
    assertTrue(object1.equals(new DvpCommitEncumbrance(publicKey1, "AssetID", "Reference", 100L, "Signature3")));

    assertTrue(object1.getAddress().contains(party1Address));
    assertFalse(object1.getAddress().contains(party2Address));

  }


  @Test
  public void dvpCommitParameter() throws Exception {

    DvpCommitParameter object1 = new DvpCommitParameter("P1", 7L, 0, publicKey1, "Signature5");
    final DvpCommitParameter object2 = new DvpCommitParameter(object1);
    final DvpCommitParameter object3 = new DvpCommitParameter(object2.encode(0L));
    final DvpCommitParameter object4 = new DvpCommitParameter(new MPWrappedArrayImpl(object3.encode(0L)));
    final DvpCommitParameter object5 = new DvpCommitParameter("P1", "7", 0, publicKey1, "Signature5");

    // Default values.
    final DvpCommitParameter object6 = new DvpCommitParameter(new Object[0]);
    final DvpCommitParameter object7 = new DvpCommitParameter(new MPWrappedArrayImpl(new Object[0]));

    object1.encodeJson();

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object1.equals(object5));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));
    assertTrue(object4.getValue().equals(7L));
    assertTrue(object4.getValueNumber().equals(7L));

    assertFalse(object4.equals(object6));
    assertTrue(object6.equals(object7));

    assertFalse(object1.equals(null));
    assertFalse(object1.equals("fred"));
    assertFalse(object1.equals(new DvpCommitParameter("BadP1", 7L, 0, publicKey1, "Signature5")));
    assertFalse(object1.equals(new DvpCommitParameter("P1", 77L, 0, publicKey1, "Signature5")));
    assertFalse(object1.equals(new DvpCommitParameter("P1", 7L, 1, publicKey1, "Signature5")));
    assertFalse(object1.equals(new DvpCommitParameter("P1", 7L, 0, publicKey2, "Signature5")));
    assertFalse(object1.equals(new DvpCommitParameter("P1", 7L, 0, publicKey1, "BadSignature5")));

    assertTrue(object1.getAddress().contains(party1Address));
    assertFalse(object1.getAddress().contains(party2Address));

    assertTrue(object1.stringToHashToSign("ca").equals("P17"));

    object1 = new DvpCommitParameter("P1", 7L, 1, publicKey1, "Signature5");

    assertTrue(object1.stringToHashToSign("ca").equals("caP17"));

  }

  @Test
  public void dvpCommitParty() throws Exception {

    DvpCommitParty object1 = new DvpCommitParty(partyIdentifier1, publicKey1, "Signature5");
    DvpCommitParty object2 = new DvpCommitParty(object1);
    DvpCommitParty object3 = new DvpCommitParty(object2.encode(0L));
    DvpCommitParty object4 = new DvpCommitParty(new MPWrappedArrayImpl(object3.encode(0L)));

    object1.encodeJson();

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));

    assertFalse(object1.equals(null));
    assertFalse(object1.equals("fred"));
    assertFalse(object1.equals(new DvpCommitParty(partyIdentifier2, publicKey1, "Signature5")));
    assertFalse(object1.equals(new DvpCommitParty(partyIdentifier1, publicKey2, "Signature5")));
    assertFalse(object1.equals(new DvpCommitParty(partyIdentifier1, publicKey1, "Signature6")));
    assertTrue(object1.equals(new DvpCommitParty(partyIdentifier1, publicKey1, "Signature5")));

  }

  @Test
  public void dvpCommitReceive() throws Exception {

    DvpCommitReceive object1 = new DvpCommitReceive(10L, "Address");
    DvpCommitReceive object2 = new DvpCommitReceive(object1);
    DvpCommitReceive object3 = new DvpCommitReceive(object2.encode(0L));
    DvpCommitReceive object4 = new DvpCommitReceive(new MPWrappedArrayImpl(object3.encode(0L)));

    object1.encodeJson();

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));

    assertFalse(object1.equals(null));
    assertFalse(object1.equals("fred"));
    assertFalse(object1.equals(new DvpCommitReceive(11L, "Address")));
    assertFalse(object1.equals(new DvpCommitReceive(10L, "BadAddress")));
    assertTrue(object1.equals(new DvpCommitReceive(10L, "Address")));

  }

  @Test
  public void dvpCommitment() throws Exception {

    DvpCommitment object1 = new DvpCommitment(1L, publicKey1, "Signature1");
    DvpCommitment object2 = new DvpCommitment(object1);
    DvpCommitment object3 = new DvpCommitment(object2.encode(0L));
    DvpCommitment object4 = new DvpCommitment(new MPWrappedArrayImpl(object3.encode(0L)));

    MPWrappedArrayImpl m1 = null;
    final DvpCommitment object5 = new DvpCommitment(m1);
    final DvpCommitment object6 = new DvpCommitment(new Object[0]);

    object1.encodeJson();

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));

    assertFalse(object4.equals(object5));
    assertFalse(object4.equals(object6));
    assertTrue(object5.equals(object6));

    assertTrue(object1.namespace.equals(""));
    assertTrue(object1.classID.equals(""));
    assertTrue(object1.amount.equals(0L));

    //

    object1 = new DvpCommitment(new Object[] {1L, "namespace", "class", 42L, publicKey1, "Signature1"});
    object2 = new DvpCommitment(object1);
    object3 = new DvpCommitment(object2.encode(0L));
    object4 = new DvpCommitment(new MPWrappedArrayImpl(new Object[] {1L, "namespace", "class", 42L, publicKey1, "Signature1"}));

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));

    assertTrue(object1.namespace.equals("namespace"));
    assertTrue(object2.namespace.equals("namespace"));
    assertTrue(object3.namespace.equals(""));
    assertTrue(object4.namespace.equals("namespace"));

    assertTrue(object1.classID.equals("class"));
    assertTrue(object2.classID.equals("class"));
    assertTrue(object3.classID.equals(""));
    assertTrue(object4.classID.equals("class"));

    assertTrue(object1.amount.equals(42L));
    assertTrue(object2.amount.equals(42L));
    assertTrue(object3.amount.equals(0L));
    assertTrue(object4.amount.equals(42L));

    //

    assertFalse(object1.equals(null));
    assertFalse(object1.equals("fred"));
    assertFalse(object1.equals(new DvpCommitment(2L, publicKey1, "Signature1")));
    assertFalse(object1.equals(new DvpCommitment(1L, publicKey2, "Signature1")));
    assertFalse(object1.equals(new DvpCommitment(1L, publicKey1, "Signature2")));
    assertTrue(object1.equals(new DvpCommitment(1L, publicKey1, "Signature1")));

    assertTrue(object1.getPayAddress().equals(party1Address));
    assertTrue(object1.getPayAddress().equals(party1Address));

    assertTrue(object1.objectToHashToSign("ca")[0].equals("ca"));
    assertTrue(object1.objectToHashToSign("ca")[1].equals("namespace"));
    assertTrue(object1.objectToHashToSign("ca")[2].equals("class"));
    assertTrue(object1.objectToHashToSign("ca")[3].equals(42L));
  }

  @Test
  public void dvpCommitmentString() throws Exception {


    DvpCommitment object1 = new DvpCommitment(new Object[] {1L, "namespace", "class", "999", publicKey1, "Signature1"});
    DvpCommitment object2 = new DvpCommitment(object1);
    DvpCommitment object3 = new DvpCommitment(object2.encode(0L));
    DvpCommitment object4 = new DvpCommitment(new MPWrappedArrayImpl(new Object[] {1L, "namespace", "class", "999", publicKey1, "Signature1"}));

    assertTrue(object1.equals(object2));
    assertTrue(object1.equals(object3));
    assertTrue(object1.equals(object4));
    assertTrue(object4.equals(object3));
    assertTrue(object4.equals(object2));
    assertTrue(object4.equals(object1));

    assertTrue(object1.namespace.equals("namespace"));
    assertTrue(object2.namespace.equals("namespace"));
    assertTrue(object3.namespace.equals(""));
    assertTrue(object4.namespace.equals("namespace"));

    assertTrue(object1.classID.equals("class"));
    assertTrue(object2.classID.equals("class"));
    assertTrue(object3.classID.equals(""));
    assertTrue(object4.classID.equals("class"));

    assertTrue(object1.amount == null);
    assertTrue(object2.amount == null);
    assertTrue(object3.amount.equals(0L));
    assertTrue(object4.amount == null);

    assertTrue(object1.amountString.equals("999"));
    assertTrue(object2.amountString.equals("999"));
    assertTrue(object3.amountString == null);
    assertTrue(object4.amountString.equals("999"));

    //

    assertFalse(object1.equals(null));
    assertFalse(object1.equals("fred"));
    assertFalse(object1.equals(new DvpCommitment(2L, publicKey1, "Signature1")));
    assertFalse(object1.equals(new DvpCommitment(1L, publicKey2, "Signature1")));
    assertFalse(object1.equals(new DvpCommitment(1L, publicKey1, "Signature2")));
    assertTrue(object1.equals(new DvpCommitment(1L, publicKey1, "Signature1")));

    assertTrue(object1.getPayAddress().equals(party1Address));
    assertTrue(object1.getPayAddress().equals(party1Address));

    assertTrue(object1.objectToHashToSign("ca")[0].equals("ca"));
    assertTrue(object1.objectToHashToSign("ca")[1].equals("namespace"));
    assertTrue(object1.objectToHashToSign("ca")[2].equals("class"));
    assertTrue(object1.objectToHashToSign("ca")[3].equals("999"));
  }

  @Test
  public void dvpUKCommitData() throws Exception {

    DvpUKCommitData testData1 = getCommitData2("contractAddress");

    assertTrue(testData1.addresses().contains(party1Address));
    assertTrue(testData1.addresses().contains(party2Address));
    assertTrue(testData1.addresses().size() == 2);
    assertFalse(testData1.addresses().contains("dross"));

  }
}
