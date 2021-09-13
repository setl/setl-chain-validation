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
package io.setl.bc.pychain.state.ipfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.entry.EntryDecoder;
import io.setl.utils.ByteUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpfsListTest {

  private static final int TESTSIZE = 1024;

  private static final Logger logger = LoggerFactory.getLogger(IpfsListTest.class);



  private static class WalkConsumer implements Consumer<Object> {

    private final int i;

    int expectedIndex = 0;


    WalkConsumer(int i) {
      this.i = i;
    }


    @Override
    public void accept(Object o) {
      long index = (long) ((Object[]) o)[0];
      if (index != expectedIndex) {
        throw new RuntimeException(String.format("Expected %d, got %d", expectedIndex, index));
      }
      expectedIndex++;
    }


    public boolean isComplete() {
      return expectedIndex == i;
    }
  }



  private MerkleStore<Object> ms;

  private IpfsList<TestEntry> theList;


  @Test
  public void coverDebugMethods() {
    // we do not really care what these methods do
    theList.debugGetWrappedMerkleArray();
    theList.debugGetChangedObjectArray();
  }


  @Test
  public void partialTree() {
    for (int i = 0; i < 67; i++) {
      TestEntry newEntry = new TestEntry(i);
      theList.update(-1, newEntry);
    }
    PartialTree<TestEntry> tree = theList.getPartialTree(Collections.singleton(TestEntry.keyGen(13)));
    assertNull(tree);

    theList.computeRootHash();
    tree = theList.getPartialTree(Collections.singleton(TestEntry.keyGen(13)));
    assertNotNull(tree);

    // Select an index that will alternate between left and right nodes as it does down the tree.
    Map<String, Object> map = theList.getPartialTreeMap(
        Arrays.asList(TestEntry.keyGen(0b010101), TestEntry.keyGen(100)),
        e -> {
          JSONObject jo = new JSONObject();
          jo.put("K", e.getKey());
          return jo;
        }
    );
    JSONObject jsonObject = new JSONObject(map);

    String expectedJson =
        "{\n"
            + "  \"binary\": \"FBpq9rI3xcGiRbZsfvykwoBwN4lzON0ZXXZpJwT_45MnyM77uRv96ldLShd1LTQH24vYSYJc8qvbisjrU0Tfdg\",\n"
            + "  \"hash\": \"QP0zfW56X4EfRnAwWSSyPmueT5cWv1ue0yiccXNszJI\",\n"
            + "  \"left\": {\n"
            + "    \"binary\": \"OtyBbVbAZN-_A-5annyH8fK5KwyW65B5XzNrqKr-0NBnroBKwYL66fmS1NIJtKYaQHas05Rl9C0FRzDtg1Zb_w\",\n"
            + "    \"hash\": \"FBpq9rI3xcGiRbZsfvykwoBwN4lzON0ZXXZpJwT_45M\",\n"
            + "    \"left\": {\n"
            + "      \"binary\": \"Wvm0t_FgIOHYCx0BXiIL-h0V7MgZ4osFptnudtJUcc0FgPgJ9KuoeO0W4sdH4YF2N1PHDT36MFw2pYMTytqHeA\",\n"
            + "      \"hash\": \"OtyBbVbAZN-_A-5annyH8fK5KwyW65B5XzNrqKr-0NA\",\n"
            + "      \"left\": {\n"
            + "        \"hash\": \"Wvm0t_FgIOHYCx0BXiIL-h0V7MgZ4osFptnudtJUcc0\"\n"
            + "      },\n"
            + "      \"right\": {\n"
            + "        \"binary\": \"tM1yMUrI1U1kMhGL0dG7h4KtfVfULkleFQJS745miyJt9lzc0WilIGuG5oDE6bzBpnVyUcOfgFzkow3TNH9YyA\",\n"
            + "        \"hash\": \"BYD4CfSrqHjtFuLHR-GBdjdTxw09-jBcNqWDE8rah3g\",\n"
            + "        \"left\": {\n"
            + "          \"binary\": \"v4UQQzLaxGk_7F1oR7mqlJz6HR1ROI5_WuMRvGf58jAfiNJXkm4GFJulLQKWxy2hm8qYkklwZzYGBnRFePsWsg\",\n"
            + "          \"hash\": \"tM1yMUrI1U1kMhGL0dG7h4KtfVfULkleFQJS745miyI\",\n"
            + "          \"left\": {\n"
            + "            \"hash\": \"v4UQQzLaxGk_7F1oR7mqlJz6HR1ROI5_WuMRvGf58jA\"\n"
            + "          },\n"
            + "          \"right\": {\n"
            + "            \"binary\": \"b1tRWoaVuVr32iPNrkJPwIQFYb7iNlT0-cvaVRs1sEWHrs5-V_1Av1XfwU75Dwe_EYUDNqHhhHOeURRGGpXtGA\",\n"
            + "            \"hash\": \"H4jSV5JuBhSbpS0ClsctoZvKmJJJcGc2BgZ0RXj7FrI\",\n"
            + "            \"left\": {\n"
            + "              \"binary\": \"Y3hdML5oLpXTPaE5Ve9pQqVN9ZbTBc12WA8mA7zQ2kolyMKuV8OxwdU3_YpumxDbYaGE5sobKgSRZhvDvI9MKw\",\n"
            + "              \"hash\": \"b1tRWoaVuVr32iPNrkJPwIQFYb7iNlT0-cvaVRs1sEU\",\n"
            + "              \"left\": {\n"
            + "                \"hash\": \"Y3hdML5oLpXTPaE5Ve9pQqVN9ZbTBc12WA8mA7zQ2ko\"\n"
            + "              },\n"
            + "              \"right\": {\n"
            + "                \"binary\": \"k_-mS2V5PTIxrE1vcmUgc3R1ZmYyMQ\",\n"
            + "                \"hash\": \"JcjCrlfDscHVN_2KbpsQ22GhhObKGyoEkWYbw7yPTCs\",\n"
            + "                \"value\": {\n"
            + "                  \"K\": \"Key=21\"\n"
            + "                }\n"
            + "              }\n"
            + "            },\n"
            + "            \"right\": {\n"
            + "              \"hash\": \"h67Oflf9QL9V38FO-Q8HvxGFAzah4YRznlEURhqV7Rg\"\n"
            + "            }\n"
            + "          }\n"
            + "        },\n"
            + "        \"right\": {\n"
            + "          \"hash\": \"bfZc3NFopSBrhuaAxOm8waZ1clHDn4Bc5KMN0zR_WMg\"\n"
            + "        }\n"
            + "      }\n"
            + "    },\n"
            + "    \"right\": {\n"
            + "      \"hash\": \"Z66ASsGC-un5ktTSCbSmGkB2rNOUZfQtBUcw7YNWW_8\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"right\": {\n"
            + "    \"hash\": \"J8jO-7kb_epXS0oXdS00B9uL2EmCXPKr24rI61NE33Y\"\n"
            + "  }\n"
            + "}";
    expectedJson = expectedJson.replaceAll("\\s", "");
    assertEquals(expectedJson, jsonObject.toJSONString());

    // Select multiple nodes
    map = theList.getPartialTreeMap(
        Arrays.asList(TestEntry.keyGen(0), TestEntry.keyGen(1), TestEntry.keyGen(2)),
        e -> {
          JSONObject jo = new JSONObject();
          jo.put("K", e.getKey());
          return jo;
        }
    );
    jsonObject = new JSONObject(map);

    expectedJson =
        "{\n"
            + "  \"binary\": \"FBpq9rI3xcGiRbZsfvykwoBwN4lzON0ZXXZpJwT_45MnyM77uRv96ldLShd1LTQH24vYSYJc8qvbisjrU0Tfdg\",\n"
            + "  \"hash\": \"QP0zfW56X4EfRnAwWSSyPmueT5cWv1ue0yiccXNszJI\",\n"
            + "  \"left\": {\n"
            + "    \"binary\": \"OtyBbVbAZN-_A-5annyH8fK5KwyW65B5XzNrqKr-0NBnroBKwYL66fmS1NIJtKYaQHas05Rl9C0FRzDtg1Zb_w\",\n"
            + "    \"hash\": \"FBpq9rI3xcGiRbZsfvykwoBwN4lzON0ZXXZpJwT_45M\",\n"
            + "    \"left\": {\n"
            + "      \"binary\": \"Wvm0t_FgIOHYCx0BXiIL-h0V7MgZ4osFptnudtJUcc0FgPgJ9KuoeO0W4sdH4YF2N1PHDT36MFw2pYMTytqHeA\",\n"
            + "      \"hash\": \"OtyBbVbAZN-_A-5annyH8fK5KwyW65B5XzNrqKr-0NA\",\n"
            + "      \"left\": {\n"
            + "        \"binary\": \"ZJg2kyGUrYmdp79zJ9qXX7wgnQHCdP4RbHfkmRvu3iAp4jLTDwIRVHv-gk7z0pgLjgGoPpqJw29fUCInqBJVRA\",\n"
            + "        \"hash\": \"Wvm0t_FgIOHYCx0BXiIL-h0V7MgZ4osFptnudtJUcc0\",\n"
            + "        \"left\": {\n"
            + "          \"binary\": \"tsFT7LQuy6leNpjVGWCglknphRJjqgV1lF02d5UQzFzGUy6HvLy7asuEMMGfGAp0dJBPFQ8z-bQZMhKz5ROyAg\",\n"
            + "          \"hash\": \"ZJg2kyGUrYmdp79zJ9qXX7wgnQHCdP4RbHfkmRvu3iA\",\n"
            + "          \"left\": {\n"
            + "            \"binary\": \"EmfC1Lsk5CujqaFewS-L4Z_BChcTeda1xBhX2bHcaSYn0uiZ1sfALr3P8jP-K5edwcfqoDhkKZe2oWfIMP2ApQ\",\n"
            + "            \"hash\": \"tsFT7LQuy6leNpjVGWCglknphRJjqgV1lF02d5UQzFw\",\n"
            + "            \"left\": {\n"
            + "              \"binary\": \"kSNFudcGCkG1WHArkPZI0bK6O8qEuQJQbTuLh5A6bRrza6NVdBV9C6IBaOtQaVdk11--TAijY3ZtsXc3015zOA\",\n"
            + "              \"hash\": \"EmfC1Lsk5CujqaFewS-L4Z_BChcTeda1xBhX2bHcaSY\",\n"
            + "              \"left\": {\n"
            + "                \"binary\": \"k_-lS2V5PTCrTW9yZSBzdHVmZjA\",\n"
            + "                \"hash\": \"kSNFudcGCkG1WHArkPZI0bK6O8qEuQJQbTuLh5A6bRo\",\n"
            + "                \"value\": {\n"
            + "                  \"K\": \"Key=0\"\n"
            + "                }\n"
            + "              },\n"
            + "              \"right\": {\n"
            + "                \"binary\": \"k_-lS2V5PTGrTW9yZSBzdHVmZjE\",\n"
            + "                \"hash\": \"82ujVXQVfQuiAWjrUGlXZNdfvkwIo2N2bbF3N9Neczg\",\n"
            + "                \"value\": {\n"
            + "                  \"K\": \"Key=1\"\n"
            + "                }\n"
            + "              }\n"
            + "            },\n"
            + "            \"right\": {\n"
            + "              \"binary\": \"Swph43dQeBVUJrVpOLOa4UeutOrrw-fkxQwVd9stj7HSWT7Z_nxg6KyNwrLiDPEj4scNVyGrvFyM7sP9s2XKhQ\",\n"
            + "              \"hash\": \"J9LomdbHwC69z_Iz_iuXncHH6qA4ZCmXtqFnyDD9gKU\",\n"
            + "              \"left\": {\n"
            + "                \"binary\": \"k_-lS2V5PTKrTW9yZSBzdHVmZjI\",\n"
            + "                \"hash\": \"Swph43dQeBVUJrVpOLOa4UeutOrrw-fkxQwVd9stj7E\",\n"
            + "                \"value\": {\n"
            + "                  \"K\": \"Key=2\"\n"
            + "                }\n"
            + "              },\n"
            + "              \"right\": {\n"
            + "                \"hash\": \"0lk-2f58YOisjcKy4gzxI-LHDVchq7xcjO7D_bNlyoU\"\n"
            + "              }\n"
            + "            }\n"
            + "          },\n"
            + "          \"right\": {\n"
            + "            \"hash\": \"xlMuh7y8u2rLhDDBnxgKdHSQTxUPM_m0GTISs-UTsgI\"\n"
            + "          }\n"
            + "        },\n"
            + "        \"right\": {\n"
            + "          \"hash\": \"KeIy0w8CEVR7_oJO89KYC44BqD6aicNvX1AiJ6gSVUQ\"\n"
            + "        }\n"
            + "      },\n"
            + "      \"right\": {\n"
            + "        \"hash\": \"BYD4CfSrqHjtFuLHR-GBdjdTxw09-jBcNqWDE8rah3g\"\n"
            + "      }\n"
            + "    },\n"
            + "    \"right\": {\n"
            + "      \"hash\": \"Z66ASsGC-un5ktTSCbSmGkB2rNOUZfQtBUcw7YNWW_8\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"right\": {\n"
            + "    \"hash\": \"J8jO-7kb_epXS0oXdS00B9uL2EmCXPKr24rI61NE33Y\"\n"
            + "  }\n"
            + "}";
    expectedJson = expectedJson.replaceAll("\\s", "");
    assertEquals(expectedJson, jsonObject.toJSONString());
  }


  @Before
  public void setup() {
    //Create an empty memory backed IPFSList
    Hash rootHash = Hash.NULL_HASH;
    Map<Hash, Object> backingMap = new HashMap<>();
    ms = new MapBackedMerkleStore<>(backingMap);
    MemoryKeyToHashIndex<String> k2i = new MemoryKeyToHashIndex<>(rootHash, ms);
    EntryDecoder<TestEntry> decoder = new TestEntryDecoder();
    theList = new IpfsList<>(rootHash, ms, decoder, k2i, TestEntry.class);
  }


  /**
   * Test removal from middle of List.
   *
   * @throws Exception if fails
   */
  @Test
  public void testRemove100FromMiddle() throws Exception {
    //Set up computed hashes in range 0-499, 1000-1099, 600-999
    Hash[] computedHashes = new Hash[TESTSIZE];
    for (int i = 0, j = 0; i < 1000; i++) {
      if (i < 500 || i >= 600) {
        j = i;
      } else {
        j = i + 500;
      }
      TestEntry newEntry = new TestEntry(j);
      theList.update(i, newEntry);
      computedHashes[i] = theList.getHash();
    }

    final Hash expectedHash = theList.getHash();

    //Reset
    setup();

    //Now setup list with all 1100 entries
    for (int i = 0; i < 1100; i++) {
      TestEntry newEntry = new TestEntry(i);
      theList.update(i, newEntry);
    }

    //Delete from 500-599
    for (int i = 0; i < 100; i++) {
      theList.remove(-1, new TestEntry(599 - i).getKey());
    }

    //Finally compare the hash
    assertEquals("Hash does not match", expectedHash, theList.getHash());

  }


  /**
   * General test to add TESTSIZE entries, walk the tree, then delete 1 by 1 from the end, checking hashes and walking tree.
   */
  @Test
  public void testRemoveEntries() throws Exception {
    //Compute hashes for each state between 0 and TESTSIZE while entries are added
    Hash[] computedHashes = new Hash[TESTSIZE];
    assertEquals("", theList.computeRootHash());
    for (int i = 0; i < TESTSIZE; i++) {
      TestEntry newEntry = new TestEntry(i);
      theList.update(i, newEntry);
      computedHashes[i] = theList.getHash();
      WalkConsumer wc = new WalkConsumer(i + 1);
      IpfsWalker.walk(computedHashes[i], ms, wc);
      assertEquals("Walker did not complete", true, wc.isComplete());

      if (logger.isDebugEnabled()) {
        logger.debug(String.format("@%d Count:%d=%s", i, theList.getEntryCount(), ByteUtil.bytesToHex(computedHashes[i].get())));
      }
    }
    assertEquals(computedHashes[TESTSIZE - 1].toHexString(), theList.computeRootHash());
    assertEquals(TESTSIZE, theList.walk());

    //Check hashes match as entries are deleted
    for (int i = TESTSIZE - 1; i >= 1; i -= 1) {
      Hash h = theList.getHash();
      if (logger.isDebugEnabled()) {
        logger.debug(String.format("/@%d Count:%d=%s", i, theList.getEntryCount(), ByteUtil.bytesToHex(h.get())));
      }
      assertEquals("Hash does not match@" + i, computedHashes[i], h);
      theList.remove(i, TestEntry.keyGen(i));
    }

    //Check that all hashes are still stored
    for (int i = 0; i < TESTSIZE; i++) {
      WalkConsumer wc = new WalkConsumer(i + 1);
      IpfsWalker.walk(computedHashes[i], ms, wc);
      assertEquals("Walker did not complete", true, wc.isComplete());
    }
  }
}
