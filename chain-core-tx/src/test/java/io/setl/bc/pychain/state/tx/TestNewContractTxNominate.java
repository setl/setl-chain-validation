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
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;
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
import io.setl.crypto.SHA256;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import io.setl.common.Hex;
import org.junit.Test;


/**
 * Created by emh on 2017.Jun.20.
 */
public class TestNewContractTxNominate {
  
  /**
   * testStatic.
   */
  @Test
  public void testStatic() {
    
    final Map<String, Object> map = new HashMap<>();
    map.put("__function", CONTRACT_NAME_DVP_UK);
    
    final MPWrappedMap<String, Object> dictionary = new MPWrappedMap<>(map);
    
    IContractData testData1 = NewContractTx.getContractDataFromDictionary(dictionary);

    assertEquals(testData1.get__function(), CONTRACT_NAME_DVP_UK);
  }
  
  /**
   * Test that a serialise/deserialised pair.
   */
  @Test
  public void testSerialiseCopyCreate() throws Exception {
    
    final String fromPubKey = "3059301306072a8648ce3d020106082a8648ce3d03010703420004b9d9c6dbfa46b2c2d0db216f2aa3ad0a93f6f6e188872c23ca4c1a2af5dd044c93e953ddb03fa77c01db6bf68641e8292890f31c22b5732b4ec67c25aaec7324";
    final String fromAddress = "AHyIcFZzI4Wci9BiAUjUwejR00MQY6umnw";
    final String nameSpace = "namespace";
    final String classId = "classId";
    final long nonce = 3;
    final String contractAddress = AddressUtil.publicKeyToAddress(fromPubKey , AddressType.CONTRACT,nonce);
    final String signature = "signature";
    final String protocol = "protocol";
    final String meta = "meta";
    final String poa = "poa";
    final String proto = "proto";
    String hash = "HASH";
    
    final Map<String, Object> map = new HashMap<>();
    map.put("__function", CONTRACT_NAME_TOKENS_NOMINATE);
    
    final MPWrappedMap<String, Object> dictionary = new MPWrappedMap<>(map);
    
    NewContractTx tx = new NewContractTx(1,            //  chainId
        hash,
        nonce,            //  nonce
        true,
        fromPubKey,
        fromAddress,
        dictionary,
        signature,
        5,            //  height
        poa,
        6);           //  timestamp
    
    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertEquals("72c1d398ecdeeeaad25d1a9f379af703c79a25906a0b97aa587fe90e04889357", hash);
    tx.setHash(hash);

    assertEquals(tx.getTxType(), TxType.NEW_CONTRACT);
    assertEquals(((AbstractTx) tx).getTxType(), TxType.NEW_CONTRACT);
    assertFalse(tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();
    
    NewContractTx tx2 = NewContractTx.decodeTX(encoded);
    NewContractTx tx3 = new NewContractTx(tx);
    NewContractTx tx4 = (NewContractTx) TxFromList.txFromList(new MPWrappedArrayImpl(encoded));
    NewContractTx tx5 = new NewContractTx(tx);
    tx5.setContractDictionary(dictionary); // Constructor, but overwrite contrantDictionary with test data.

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(NewContractTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (NewContractTx thisTx : Arrays.asList(tx, tx2, tx3, tx4, tx5)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertEquals(thisTx.addresses().size(), tx.addresses().size());
      assertEquals(thisTx.getPriority(), TxType.NEW_CONTRACT.getPriority());

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
          assertEquals("NO", CONTRACT_NAME_TOKENS_NOMINATE, v);
        });
        assertEquals("NO", 1, count[0]);
      } else {
        // tx3 is a Constructor copy. The dictionary comes back as a proper tokens_nominate initialised dictionary.
        NavigableMap cMap = fred.toMap();
        assertEquals("No", CONTRACT_NAME_TOKENS_NOMINATE, cMap.get("__function"));
        assertTrue(cMap.containsKey("__address"));
        assertTrue(cMap.containsKey("__timeevent"));
        assertTrue(cMap.containsKey("expiry"));
        assertTrue(cMap.containsKey("issuingaddress"));
        assertTrue(cMap.containsKey("blocksizein"));
        assertTrue(cMap.containsKey("blocksizeout"));
        assertTrue(cMap.containsKey("inputtokenclass"));
        assertTrue(cMap.containsKey("metadata"));
        assertTrue(cMap.containsKey("namespace"));
        assertTrue(cMap.containsKey("outputtokenclass"));
        
        IContractData thisCD = thisTx.getContractData();
        assertEquals("No", CONTRACT_NAME_TOKENS_NOMINATE, cMap.get("__function"));
      }
      
      
      assertEquals("signature does not match", thisTx.getSignature(), signature);
      assertEquals("height does not match", 5, thisTx.getHeight());
      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
      
    }
    
  }
  
}
