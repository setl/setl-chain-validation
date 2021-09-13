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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.crypto.KeyGen;
import io.setl.crypto.KeyGen.Type;
import io.setl.crypto.SHA256;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import io.setl.common.Hex;
import org.junit.Test;


/**
 * Created by emh on 2017.Jun.20.
 */
public class PoaNewContractTxDvPTest {
  
  /**
   * testStatic.
   */
  @Test
  public void testStatic() {
    
    final Map<String, Object> map = new HashMap<>();
    map.put("__function", CONTRACT_NAME_DVP_UK);
    
    final MPWrappedMap<String, Object> dictionary = new MPWrappedMap<>(map);
    
    IContractData testData1 = PoaNewContractTx.getContractDataFromDictionary(dictionary);

    assertEquals(testData1.get__function(), CONTRACT_NAME_DVP_UK);
  }
  
  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseCopyCreate() throws Exception {

    final String fromPubKey = "3059301306072a8648ce3d020106082a8648ce3d0301070342000471e94d9cab0149d812b90b325a34ac239321cc948e07ae7de3b12e0d95296e4333da2ab2eb477904b392c08a2645cd5a4949eef962a8311ab8da4b12e5a6a9f2";
    final String fromAddress = "AOUpXR-dNyhf0FiN1ACmeLsRFfDomFINgw";
    final String reference = "toKey2";
    final String poaAddress = "AD8fV8vOFwDPxmwZhwcPts8rCA41z0SlLg";
    final String nameSpace = "namespace";
    final String classId = "classId";
    final long nonce = 3;
    final String contractAddress = AddressUtil.publicKeyToAddress(fromPubKey, AddressType.CONTRACT,nonce);
    final String signature = "signature";
    final String protocol = "protocol";
    final String metadata = "meta";
    final String poa = "poa";
    final String proto = "proto";
    String hash = "HASH";
    final long startDate = 0L;
    long endDate = Instant.now().getEpochSecond() + 999L;
    final String poaReference = "poaRef1";
  
    final Map<String, Object> map = new HashMap<>();
    map.put("__function", CONTRACT_NAME_DVP_UK);
    
    final MPWrappedMap<String, Object> dictionary = new MPWrappedMap<>(map);
    
    PoaNewContractTx tx = new PoaNewContractTx(
        1,            //  chainId
        hash,
        nonce,            //  nonce
        true,
        fromPubKey,
        fromAddress,
        poaAddress,
        reference,
        dictionary,
        signature,
        protocol,
        metadata,
        5,            //  height
        poa,
        6);           //  timestamp
    
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertEquals("bd6693aeabde81e705f9582f891ca0dc0cbc2f05f5e562455906d276460e51db", hash);
    tx.setHash(hash);

    assertEquals(tx.getTxType(), TxType.POA_NEW_CONTRACT);
    assertEquals(((AbstractTx) tx).getTxType(), TxType.POA_NEW_CONTRACT);
    assertFalse(tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();
    
    PoaNewContractTx tx2 = PoaNewContractTx.decodeTX(encoded);
    PoaNewContractTx tx3 = new PoaNewContractTx(tx);
    PoaNewContractTx tx4 = (PoaNewContractTx) TxFromList.txFromList(new MPWrappedArrayImpl(encoded));
    PoaNewContractTx tx5 = new PoaNewContractTx(tx);
    tx5.setContractDictionary(dictionary); // Constructor, but overwrite contrantDictionary with test data.

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(PoaNewContractTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (PoaNewContractTx thisTx : Arrays.asList(tx, tx2, tx3, tx4, tx5)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertEquals(thisTx.addresses().size(), tx.addresses().size());
      assertEquals(thisTx.getPriority(), TxType.POA_NEW_CONTRACT.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", thisTx.getNonce(), nonce);
      assertTrue("updated does not match", thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("toAddress does not match", thisTx.getContractAddress(), contractAddress);
      
      MPWrappedMap<String, Object> fred = thisTx.getContractDictionary();
      
      if (thisTx != tx3) {
        int[] count = new int[] {0};
        fred.iterate((k, v) -> {
          count[0]++;
          assertEquals("NO", "__function", k);
          assertEquals("NO", CONTRACT_NAME_DVP_UK, v);
        });
        assertEquals("NO", 1, count[0]);
      } else {
        // tx3 is a Constructor copy. The dictionary comes back as a proper dvp_uk initialised dictionary.
        NavigableMap dvpMap = fred.toMap();
        assertEquals("No", CONTRACT_NAME_DVP_UK, dvpMap.get("__function"));
        assertTrue(dvpMap.containsKey("__address"));
        assertTrue(dvpMap.containsKey("__completed"));
        assertTrue(dvpMap.containsKey("__timeevent"));
        assertTrue(dvpMap.containsKey("expiry"));
        assertTrue(dvpMap.containsKey("issuingaddress"));
        assertTrue(dvpMap.containsKey("startdate"));
        
        IContractData thisCD = thisTx.getContractData();
        assertEquals(thisCD.get__function(), CONTRACT_NAME_DVP_UK);
      }
      
      
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("height does not match", 5, thisTx.getHeight());
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
      
    }
    
  }
  
}
