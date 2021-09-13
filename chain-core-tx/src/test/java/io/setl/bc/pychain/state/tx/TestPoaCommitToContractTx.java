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
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.accumulator.JSONHashAccumulator;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
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
public class TestPoaCommitToContractTx {

  /**
   * testStatic.
   */
  @Test
  public void testStatic() {

    final Map<String, Object> map = new HashMap<>();
    map.put("__function", CONTRACT_NAME_DVP_UK);

    final MPWrappedMap<String, Object> dictionary = new MPWrappedMap<>(map);

    IContractData testData1 = PoaCommitToContractTx.getContractDataFromDictionary(dictionary);

    assertTrue(testData1.get__function().equals(CONTRACT_NAME_DVP_UK_COMMIT));
  }


  @Test
  public void testSerialiseDeserialise() throws Exception {

    final String fromPubKey = "fromPubKey";
    final String fromAddress = "fromAddress";
    final String poaAddress = "poaAddress";
    final String poaReference = "poaReference";
    final String signature = "signature";
    final String protocol = "protocol";
    final String meta = "meta";
    final String poa = "poa";
    final String contractAddress = "contractAddress";
    String hash = "HASH";

    //  create a wrapped map with a single key+value pair
    //
    final Map<String, Object> map = new HashMap<>();
    map.put("__function", CONTRACT_NAME_DVP_UK);
    final MPWrappedMap<String, Object> contractData = new MPWrappedMap<>(map);

    PoaCommitToContractTx tx = new PoaCommitToContractTx(1,            //  chainId
        hash,
        3,            //  nonce
        true,         //  updated
        fromPubKey,
        fromAddress,
        poaAddress,
        poaReference,
        contractAddress,
        contractData,
        signature,
        protocol,
        meta,
        5,            //  height
        poa,
        6);           //  timestamp

    hash = Hex.encode(SHA256.sha256(tx.buildHash(new JSONHashAccumulator()).getBytes()));
    assertTrue(hash.equals("6d2f68547d6628158f4f058b3ecdc1b12e23bdcaf4081a1f296e07eeb037683c"));
    tx.setHash(hash);

    assertTrue(tx.getTxType().equals(TxType.POA_COMMIT_TO_CONTRACT));
    assertTrue(((AbstractTx)tx).getTxType().equals(TxType.POA_COMMIT_TO_CONTRACT));
    assertTrue(!tx.getTxType().equals(TxType.ADD_X_CHAIN));

    Object[] encoded = tx.encodeTx();

    PoaCommitToContractTx tx2 = PoaCommitToContractTx.decodeTX(encoded);
    PoaCommitToContractTx tx3 = new PoaCommitToContractTx(tx);
    PoaCommitToContractTx tx4 = (PoaCommitToContractTx) TxFromList.txFromList(new MPWrappedArrayImpl(encoded));
    PoaCommitToContractTx tx5 = new PoaCommitToContractTx(tx);
    tx5.setCommitmentDictionary(contractData); // Constructor, but overwrite contractDictionary with test data.

    Object[] encodedBad = tx.encodeTx();
    encodedBad[TxGeneralFields.TX_TXTYPE] = -1;
    assertNull(PoaCommitToContractTx.decodeTX( new MPWrappedArrayImpl( encodedBad)));

    tx.toString();  // Just for code coverage. This method is just for display purposes at the moment.

    for (PoaCommitToContractTx thisTx : Arrays.asList(tx, tx2, tx3, tx4, tx5)) {

      assertTrue(thisTx.addresses().containsAll(tx.addresses()));
      assertTrue(thisTx.addresses().size() == tx.addresses().size());
      assertTrue(thisTx.getPriority() == TxType.POA_COMMIT_TO_CONTRACT.getPriority());

      assertEquals("chainId does not match", 1, thisTx.getChainId());
      assertEquals("tochainId does not match", 1, thisTx.getToChainId());
      assertEquals("hash does not match", thisTx.getHash(), hash);
      assertEquals("nonce does not match", 3, thisTx.getNonce());
      assertEquals("updated does not match", true, thisTx.isGood());
      assertEquals("fromPublicKey does not match", thisTx.getFromPublicKey(), fromPubKey);
      assertEquals("fromAddress does not match", thisTx.getFromAddress(), fromAddress);
      assertEquals("poaAddress does not match", thisTx.getPoaAddress(), poaAddress);
      assertEquals("poaReference does not match", thisTx.getPoaReference(), poaReference);

      assertEquals("contractAddress does not match", thisTx.getContractAddress().get(0), contractAddress);

      MPWrappedMap<String, Object> fred = thisTx.getCommitmentDictionary();

      if (thisTx != tx3) {
        int[] count = new int[] {0};
        fred.iterate((k, v) -> {
          count[0]++;
          assertEquals("NO", "__function", k);
          assertEquals("NO", CONTRACT_NAME_DVP_UK, v);
        });
        assertEquals("NO", 1, count[0]);
      } else {
        NavigableMap dvpMap = fred.toMap();
        assertEquals("No", CONTRACT_NAME_DVP_UK_COMMIT, dvpMap.get("__function"));
      }

      assertEquals("signature does not match", thisTx.getSignature(), signature);

      //  height is unused
      //
      //  assertEquals( "height does not match",        thisTx.getHeight(),          5);

      assertEquals("poa does not match", thisTx.getPowerOfAttorney(), poa);
      assertEquals("timestamp does not match", 6, thisTx.getTimestamp());
    }
  }

}
