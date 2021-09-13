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
package io.setl.websocket.handlers;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.p2p.message.SignatureMessage;
import io.setl.bc.pychain.p2p.message.SignatureMessage.SignatureDetail;
import io.setl.bc.pychain.p2p.message.SignatureMessage.XCSignatureDetail;
import io.setl.bc.pychain.state.monolithic.NamespaceList;
import io.setl.bc.serialise.UUIDEncoder;
import io.setl.common.Hex;
import io.setl.crypto.KeyGen;
import io.setl.util.CollectionUtils;
import io.setl.websocket.messages.APITextMessage;
import io.setl.websocket.messages.BalanceViewAPIMessage;
import io.setl.websocket.messages.BlockAPIMessage;
import io.setl.websocket.messages.ProposalAPIMessage;
import io.setl.websocket.messages.ServerStatusAPIMessage;
import io.setl.websocket.messages.TransactionAPIMessage;
import io.setl.websocket.messages.types.Location;
import io.setl.websocket.messages.types.NetworkState;
import io.setl.websocket.messages.types.Peer;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class APITextMessageTest {

  private static final List<Object> emptyArray = new ArrayList<>();

  private static final String expBalanceViewJson = "{\"Request\":{},\"Data\":{\"Status\":\"OK\",\"ChainID\":20,\"Hash\":"
      + "\"1725f72aee0b98b31ff702e24fb7132d076a537978cc0f24ea18cb8b6e8226e1\",\"TXCount\":130219,\"xcdetails\":[],\"Height\":1000,\"Namespaces\":"
      + "[[0,\"SYS\",[[\"SYS\",\"1BumbbJgZzoT7cdrjrJf3oPqhbwGwYCiCi\",{\"STAKE\":[\"STAKE\",\"oA==\"]},\"oA==\"],null]]],\"Assetbalances\":"
      + "{\"1BumbbJgZzoT7cdrjrJf3oPqhbwGwYCiCi\":{\"SYS|STAKE\":-3000000}},\"assetList\":{},\"Timestamp\":123456789},\"RequestID\":\"Update\",\"MessageType\":"
      + "\"balanceview\"}";

  private static final String expBlockJson = "{\"Data\":{\"AverageBlockTime\":0.04973921775817871,\"BaseHash\":\"964d23724448a12584580b024080af123ab785995be"
      + "c4a394e2274ec11232710\",\"ContractEvents\":[],\"EffectiveTxList\":[],\"Error\":\"\",\"Hash\":\"6af6e2ea7733055be4afe753d5d7c8bb546519e7496568be680ac"
      + "538e4b0c1e0\",\"Height\":999,\"Hostname\":\"IM000.local\",\"LastBlockTime\":0.0743720531463623,\"Networkstate\":{\"last_request\":1502720375.05364,\""
      + "lastblockrequest\":0.0,\"peersheight\":{\"127.0.0.1:13500\":[999,1502720405.073928]}},\"ProtocolCount\":{\"BACS\":0,\"CHAPS\":0,\"CLS\":0,"
      + "\"CONTRACT\":0,\"CREST\":0,\"FPS\":0,\"HEARTBEAT\":0,\"LINK\":0},\"Siglist\":[[\"MEQCICBx1Is7RDUUYzcYtSUTxJ\\/5K8xtszzvx6xSm\\/466fftAiBkhRa6eH1p1m"
      + "To76I8bxZLswixKAulH7EDfTef28nqDA==\",\"3059301306072a8648ce3d020106082a8648ce3d03010703420004fad512715ce488d76e5c7fbfac6186de4a68b1f5e612bd1c3a981b"
      + "e8be27e96e9ceefc7c5fed68a16c9f23fa8d8ea3e229492135cca49bea9fb6724c9b9dd983\",15],[\"MEUCIHpDrgOBORCDUP2Jkb8N\\/\\/imlqiaOrOjYtIep+jThvPnAiEArdJn"
      + "3iF\\/jt5bs0jd6OMtsZqg4yCT6g\\/iwbJ7R0KXrwk=\",\"3059301306072a8648ce3d020106082a8648ce3d03010703420004b410149eaa88f865c1d140d995487dee221b35af531"
      + "c2912e9c7a9ac49027eeffa8b06cc6ae5ce2b7bb88aa7724d1d2f9010cd5f7c135417cc4bfc6d6ef43a4d\",15]],\"SiglistXC\":[[\"MEQCIHEuiTe00juaHJCanpEv+6jz0GUoWP0y"
      + "8Nv2HhcQ1j87AiAR57a3VBVw75Kgl7cXubVmaq0SaBwQqUHFOCJbfCSiCA==\",\"3059301306072a8648ce3d020106082a8648ce3d03010703420004fad512715ce488d76e5c7fbfac61"
      + "86de4a68b1f5e612bd1c3a981be8be27e96e9ceefc7c5fed68a16c9f23fa8d8ea3e229492135cca49bea9fb6724c9b9dd983\",\"XC\"],[\"MEUCIGIU1PTTsaQXJdv4mS7uTVGs60E1z"
      + "bhWEh6SGKIAAbCfAiEAwvIrtzGhZpwuK5YOfGbYdHq2pbIVHr\\/q9snVF2W4j7U=\",\"3059301306072a8648ce3d020106082a8648ce3d03010703420004b410149eaa88f865c1d140"
      + "d995487dee221b35af531c2912e9c7a9ac49027eeffa8b06cc6ae5ce2b7bb88aa7724d1d2f9010cd5f7c135417cc4bfc6d6ef43a4d\",\"XC\"]],\"SignPercentage\":0.6666666"
      + "666666666,\"TX24Hours\":45,\"TXCount\":15,\"TXIn\":0,\"TXOut\":0,\"TXPoolSize\":0,\"TimeEvents\":[],\"Timestamp\":1502720375,\"Transacti"
      + "ons\":[[16,\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"14fb7008faff44bc96a4780a9e759aed1ea45542b506e8c8b46690ee8f989f11\",999,\"\"],"
      + "[16,\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"8dd10c2ce7b76896edcfa9a055a3e5eba550e6aa860d695a97077fcd3bd1129d\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"618731cf0933c23590129e2b29ae156756e3dc5ac14f7cfdedb65f7336704bec\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"c65200a83183faa0384bebf45556b136e47fe77b643f7e72180ee7f56a2edb35\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"01d5b5f9746262108763fb2109474214d38009150c29f8217244a21b5d019fdf\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"8d2cc64ce7ea55423854c3c52717a150528503c9e4a740227259c6cface938a7\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"3e2dd5ccc25993eeeb5be66b9193712084dfbae2c410fcc2deace97a35a0204b\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"0b67dd6d72449534a03a7976f3bb97d3cf27490f72d4aa45b2cfab557a160d8d\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"b3e1910a68d866899de70fe4f2a8ed46af86c2a43886e836b0f3201ebad4c631\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"89ab1484f469e23f10f77f327892eda5f9714d3545ef85ec7955e46f5cc469cf\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"a3b276ff8d4d12e7310fad28a22b04dbffc766f9c264b3575495f547f272e06a\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"7a101f0fe750aeb157ed5a5ef3d31fc22d73446b8e8e1abc9a7010d76c873e51\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"7f73e94751366a5af563e32b51acb2f30ff6e430d0213ba0f3362380b5d2a844\",999,\"\"],[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"53f756d868ced32e974528f3ea27cfd5bed56e16a1c5330d5042a049ec1bcaa8\",999,\"\"],[16,\""
      + "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"e9cce634eb11b72db47f5021333709b1af78679553e362f351c1b117904e6073\",999,\"\"]],"
      + "\"VotePercentage\":0.6666666666666666},\"MessageType\":\"block\",\"Request\":{},\"RequestID\":\"Update\"}";

  private static final String expProposalJson = "{\"Request\":{},\"Data\":{\"VotePercentage\":0.6666666666666666,\"BaseHash\":\"964d23724448a12584580b024080"
      + "af123ab785995bec4a394e2274ec11232710\",\"SignaturesXC\":[[\"MEQCIHEuiTe00juaHJCanpEv+6jz0GUoWP0y8Nv2HhcQ1j87AiAR57a3VBVw75Kgl7cXubVmaq0SaBwQqUHFOC"
      + "JbfCSiCA==\",\"3059301306072a8648ce3d020106082a8648ce3d03010703420004fad512715ce488d76e5c7fbfac6186de4a68b1f5e612bd1c3a981be8be27e96e9ceefc7c5fed6"
      + "8a16c9f23fa8d8ea3e229492135cca49bea9fb6724c9b9dd983\",\"XC\"],[\"MEUCIGIU1PTTsaQXJdv4mS7uTVGs60E1zbhWEh6SGKIAAbCfAiEAwvIrtzGhZpwuK5YOfGbYdHq2pbIVH"
      + "r/q9snVF2W4j7U=\",\"3059301306072a8648ce3d020106082a8648ce3d03010703420004b410149eaa88f865c1d140d995487dee221b35af531c2912e9c7a9ac49027eeffa8b06cc"
      + "6ae5ce2b7bb88aa7724d1d2f9010cd5f7c135417cc4bfc6d6ef43a4d\",\"XC\"]],\"Timestamp\":1502720375,\"Hostname\":\"IM000.local\",\"Height\":999,\"Signatu"
      + "res\":[[\"MEQCICBx1Is7RDUUYzcYtSUTxJ/5K8xtszzvx6xSm/466fftAiBkhRa6eH1p1mTo76I8bxZLswixKAulH7EDfTef28nqDA==\",\"3059301306072a8648ce3d020106082a864"
      + "8ce3d03010703420004fad512715ce488d76e5c7fbfac6186de4a68b1f5e612bd1c3a981be8be27e96e9ceefc7c5fed68a16c9f23fa8d8ea3e229492135cca49bea9fb6724c9b9dd98"
      + "3\",15],[\"MEUCIHpDrgOBORCDUP2Jkb8N//imlqiaOrOjYtIep+jThvPnAiEArdJn3iF/jt5bs0jd6OMtsZqg4yCT6g/iwbJ7R0KXrwk=\",\"3059301306072a8648ce3d020106082a86"
      + "48ce3d03010703420004b410149eaa88f865c1d140d995487dee221b35af531c2912e9c7a9ac49027eeffa8b06cc6ae5ce2b7bb88aa7724d1d2f9010cd5f7c135417cc4bfc6d6ef43a"
      + "4d\",15]],\"ProposedBlockHash\":\"6af6e2ea7733055be4afe753d5d7c8bb546519e7496568be680ac538e4b0c1e0\",\"Location\":{\"latitude\":51.4964,\"country"
      + "\":\"GB\",\"regionname\":null,\"longitude\":-0.1224,\"ip\":\"82.69.97.26\"},\"TXCount\":15,\"Error\":\"\",\"SignPercentage\":0.6666666666666666},"
      + "\"RequestID\":\"Update\",\"MessageType\":\"proposal\"}";

  private static final String expServerStatusJson = "{\"Request\":{},\"Data\":{\"txLogBlock\":false,\"Subscriptions\":{\"status\":0,\"stateview\":0,"
      + "\"transaction\":1,\"balanceview\":1,\"terminal\":0,\"serverstatus\":1,\"proposal\":1,\"block\":1},\"LogLevel\":3,\"Scenarios\":[[\"1\",false,\"CHAPS\""
      + "],[\"3\",false,\"CLS\"],[\"2\",false,\"CREST\"],[\"5\",false,\"BACS\"],[\"4\",false,\"FPS\"],[\"7\",false,\"CONTRACT\"],[\"6\",false,\"LINK\"],[\"8\","
      + "false,\"HEARTBEAT\"]],\"txLogScenario\":false,\"Overdrive\":0,\"LogAreas\":34816},\"RequestID\":\"Update\",\"MessageType\":\"serverstatus\"}";

  private static final String expTransactionJson = "{\"Request\":{},\"Data\":{\"Timestamp\":1502720376.020737,\"Transactions\":[[16,"
      + "\"1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz\",\"\",\"\",\"\",0,0,\"c584326431683429e620e5a309d4f9711219f0535253f3ae757ada429d5a275c\",-1,\"\",130219,"
      + "\"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\"]],\"Error\":\"\"},\"RequestID\":\"Update\","
      + "\"MessageType\":\"transaction\"}";


  private static void jsonEquals(String expected, String actual) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONObject expJson = (JSONObject) parser.parse(expected);
    JSONObject actJson = (JSONObject) parser.parse(actual);

    expJson = CollectionUtils.order(expJson);
    actJson = CollectionUtils.order(actJson);

    Assert.assertEquals(expJson.toJSONString(), actJson.toJSONString());
  }


  @Test
  public void serializeBalanceViewMessage() throws Exception {
    // Namespaces
    Object[] classData = new Object[]{"STAKE", "oA=="};
    Map<String, Object> classMap = new HashMap<>();
    classMap.put("STAKE", classData);
    Object[] data = new Object[]{"SYS", "1BumbbJgZzoT7cdrjrJf3oPqhbwGwYCiCi", new MPWrappedMap<>(classMap), ""};
    Object[] tree = new Object[]{data, null};
    Object[] namespace = new Object[]{0, "SYS", new MPWrappedArrayImpl(tree)};
    Object[] namespaceArray = new Object[]{0, 0, 0, 0, new Object[]{namespace}};
    NamespaceList namespaces = new NamespaceList(new MPWrappedArrayImpl(namespaceArray), 3);

    // Asset Balance
    Map<String, Number> assetBalanceData = new HashMap<>();
    assetBalanceData.put("SYS|STAKE", -3000000L);
    Map<String, Map<String, Number>> assetBalances = new HashMap<>();
    assetBalances.put("1BumbbJgZzoT7cdrjrJf3oPqhbwGwYCiCi", assetBalanceData);

    BalanceViewAPIMessage balanceViewMgs = new BalanceViewAPIMessage(
        "OK",
        20,
        Hash.fromHex("1725f72aee0b98b31ff702e24fb7132d076a537978cc0f24ea18cb8b6e8226e1"),
        130219,
        emptyArray,
        1000,
        namespaces,
        assetBalances,
        new HashMap<>(),
        123456789.0
    );

    APITextMessage balanceViewTxtMsg = new APITextMessage(new Object(), null, balanceViewMgs, "Update", "balanceview");
    String actBalanceViewJson = balanceViewTxtMsg.toJSON();

    jsonEquals(expBalanceViewJson, actBalanceViewJson);
  }


  @Test
  public void serializeBlockTextMessage() throws Exception {
    Map<String, Integer> protocolCount = new LinkedHashMap<>();
    protocolCount.put("LINK", 0);
    protocolCount.put("CONTRACT", 0);
    protocolCount.put("CHAPS", 0);
    protocolCount.put("BACS", 0);
    protocolCount.put("FPS", 0);
    protocolCount.put("HEARTBEAT", 0);
    protocolCount.put("CREST", 0);
    protocolCount.put("CLS", 0);

    List<Object> transactions = new ArrayList<>();
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "14fb7008faff44bc96a4780a9e759aed1ea45542b506e8c8b46690ee8f989f11", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "8dd10c2ce7b76896edcfa9a055a3e5eba550e6aa860d695a97077fcd3bd1129d", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "618731cf0933c23590129e2b29ae156756e3dc5ac14f7cfdedb65f7336704bec", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "c65200a83183faa0384bebf45556b136e47fe77b643f7e72180ee7f56a2edb35", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "01d5b5f9746262108763fb2109474214d38009150c29f8217244a21b5d019fdf", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "8d2cc64ce7ea55423854c3c52717a150528503c9e4a740227259c6cface938a7", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "3e2dd5ccc25993eeeb5be66b9193712084dfbae2c410fcc2deace97a35a0204b", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "0b67dd6d72449534a03a7976f3bb97d3cf27490f72d4aa45b2cfab557a160d8d", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "b3e1910a68d866899de70fe4f2a8ed46af86c2a43886e836b0f3201ebad4c631", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "89ab1484f469e23f10f77f327892eda5f9714d3545ef85ec7955e46f5cc469cf", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "a3b276ff8d4d12e7310fad28a22b04dbffc766f9c264b3575495f547f272e06a", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "7a101f0fe750aeb157ed5a5ef3d31fc22d73446b8e8e1abc9a7010d76c873e51", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "7f73e94751366a5af563e32b51acb2f30ff6e430d0213ba0f3362380b5d2a844", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "53f756d868ced32e974528f3ea27cfd5bed56e16a1c5330d5042a049ec1bcaa8", 999, ""});
    transactions.add(new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "e9cce634eb11b72db47f5021333709b1af78679553e362f351c1b117904e6073", 999, ""});

    byte[] uuidBytes = UUIDEncoder.encode(UUID.randomUUID());
    SignatureMessage sigMsg1 = new SignatureMessage(new MPWrappedArrayImpl(new Object[]{
        15, 13, 20, uuidBytes, new byte[]{49, 50, 51, 52}, new Object[0], "XC", new byte[]{53, 54, 55, 56},
        "3059301306072a8648ce3d020106082a8648ce3d03010703420004fad512715ce488d76e5c7fbfac6186de4a68b1f5e612bd1c3a981be8be27e96e9ceefc7c5fed68a16c9f23fa8d8ea3e229492135cca49bea9fb6724c9b9dd983",
        new byte[]{48, 68, 2, 32, 32, 113, -44, -117, 59, 68, 53, 20, 99, 55, 24, -75, 37, 19, -60, -97, -7, 43, -52, 109, -77, 60, -17, -57, -84, 82, -101,
            -2, 58, -23, -9, -19, 2, 32, 100, -123, 22, -70, 120, 125, 105, -42, 100, -24, -17, -94, 60, 111, 22, 75, -77, 8, -79, 40, 11, -91, 31, -79, 3, 125,
            55, -97, -37, -55, -22, 12},
        new byte[]{48, 68, 2, 32, 113, 46, -119, 55, -76, -46, 59, -102, 28, -112, -102, -98, -111, 47, -5, -88, -13, -48, 101, 40, 88, -3, 50, -16, -37, -10,
            30, 23, 16, -42, 63, 59, 2, 32, 17, -25, -74, -73, 84, 21, 112, -17, -110, -96, -105, -73, 23, -71, -75, 102, 106, -83, 18, 104, 28, 16, -87, 65,
            -59, 56, 34, 91, 124, 36, -94, 8}
    }));

    SignatureMessage sigMsg2 = new SignatureMessage(new MPWrappedArrayImpl(new Object[]{
        15, 13, 20, uuidBytes, new byte[]{49, 50, 51, 52}, new Object[0], "XC", new byte[]{53, 54, 55, 56},
        "3059301306072a8648ce3d020106082a8648ce3d03010703420004b410149eaa88f865c1d140d995487dee221b35af531c2912e9c7a9ac49027eeffa8b06cc6ae5ce2b7bb88aa7724d1d2f9010cd5f7c135417cc4bfc6d6ef43a4d",
        new byte[]{48, 69, 2, 32, 122, 67, -82, 3, -127, 57, 16, -125, 80, -3, -119, -111, -65, 13, -1, -8, -90, -106, -88, -102, 58, -77, -93, 98, -46, 30,
            -89, -24, -45, -122, -13, -25, 2, 33, 0, -83, -46, 103, -34, 33, 127, -114, -34, 91, -77, 72, -35, -24, -29, 45, -79, -102, -96, -29, 32, -109, -22,
            15, -30, -63, -78, 123, 71, 66, -105, -81, 9},
        new byte[]{48, 69, 2, 32, 98, 20, -44, -12, -45, -79, -92, 23, 37, -37, -8, -103, 46, -18, 77, 81, -84, -21, 65, 53, -51, -72, 86, 18, 30, -110, 24,
            -94, 0, 1, -80, -97, 2, 33, 0, -62, -14, 43, -73, 49, -95, 102, -100, 46, 43, -106, 14, 124, 102, -40, 116, 122, -74, -91, -78, 21, 30, -65, -22,
            -10, -55, -43, 23, 101, -72, -113, -75}
    }));

    List<XCSignatureDetail> signaturesXC = new ArrayList<>();
    signaturesXC.add(sigMsg1.getXChainSignature());
    signaturesXC.add(sigMsg2.getXChainSignature());

    List<SignatureDetail> signatures = new ArrayList<>();
    signatures.add(sigMsg1.getSignature());
    signatures.add(sigMsg2.getSignature());

    List<Peer> peers = new ArrayList<>();
    peers.add(new Peer("127.0.0.1:13500", 999, 1502720405.073928));

    BlockAPIMessage blockMsg = new BlockAPIMessage(
        protocolCount,
        emptyArray,
        "IM000.local",
        new NetworkState(1502720375.05364, 0, peers),
        Hash.fromHex("964d23724448a12584580b024080af123ab785995bec4a394e2274ec11232710"),
        Hash.fromHex("6af6e2ea7733055be4afe753d5d7c8bb546519e7496568be680ac538e4b0c1e0"),
        transactions,
        1502720375,
        signaturesXC,
        45,
        0,
        0.0743720531463623,
        0.04973921775817871,
        0,
        emptyArray,
        "",
        emptyArray,
        0.6666666666666666,
        0,
        999,
        0.6666666666666666,
        15,
        signatures
    );

    APITextMessage blockTxtMsg = new APITextMessage(new Object(), null, blockMsg, "Update", "block");
    String actBlockJson = blockTxtMsg.toJSON();

    jsonEquals(expBlockJson, actBlockJson);
  }


  @Test
  public void serializeProposalMessage() throws Exception {
    byte[] uuidBytes = UUIDEncoder.encode(UUID.randomUUID());
    SignatureMessage sigMsg1 = new SignatureMessage(new MPWrappedArrayImpl(new Object[]{
        15, 13, 20, uuidBytes, new byte[]{49, 50, 51, 52}, new Object[0], "XC", new byte[]{53, 54, 55, 56},
        "3059301306072a8648ce3d020106082a8648ce3d03010703420004fad512715ce488d76e5c7fbfac6186de4a68b1f5e612bd1c3a981be8be27e96e9ceefc7c5fed68a16c9f23fa8d8ea3e229492135cca49bea9fb6724c9b9dd983",
        new byte[]{48, 68, 2, 32, 32, 113, -44, -117, 59, 68, 53, 20, 99, 55, 24, -75, 37, 19, -60, -97, -7, 43, -52, 109, -77, 60, -17, -57, -84, 82, -101,
            -2, 58, -23, -9, -19, 2, 32, 100, -123, 22, -70, 120, 125, 105, -42, 100, -24, -17, -94, 60, 111, 22, 75, -77, 8, -79, 40, 11, -91, 31, -79, 3, 125,
            55, -97, -37, -55, -22, 12},
        new byte[]{48, 68, 2, 32, 113, 46, -119, 55, -76, -46, 59, -102, 28, -112, -102, -98, -111, 47, -5, -88, -13, -48, 101, 40, 88, -3, 50, -16, -37, -10,
            30, 23, 16, -42, 63, 59, 2, 32, 17, -25, -74, -73, 84, 21, 112, -17, -110, -96, -105, -73, 23, -71, -75, 102, 106, -83, 18, 104, 28, 16, -87, 65,
            -59, 56, 34, 91, 124, 36, -94, 8}
    }));

    SignatureMessage sigMsg2 = new SignatureMessage(new MPWrappedArrayImpl(new Object[]{
        15, 13, 20, uuidBytes, new byte[]{49, 50, 51, 52}, new Object[0], "XC", new byte[]{53, 54, 55, 56},
        "3059301306072a8648ce3d020106082a8648ce3d03010703420004b410149eaa88f865c1d140d995487dee221b35af531c2912e9c7a9ac49027eeffa8b06cc6ae5ce2b7bb88aa7724d1d2f9010cd5f7c135417cc4bfc6d6ef43a4d",
        new byte[]{48, 69, 2, 32, 122, 67, -82, 3, -127, 57, 16, -125, 80, -3, -119, -111, -65, 13, -1, -8, -90, -106, -88, -102, 58, -77, -93, 98, -46, 30,
            -89, -24, -45, -122, -13, -25, 2, 33, 0, -83, -46, 103, -34, 33, 127, -114, -34, 91, -77, 72, -35, -24, -29, 45, -79, -102, -96, -29, 32, -109, -22,
            15, -30, -63, -78, 123, 71, 66, -105, -81, 9},
        new byte[]{48, 69, 2, 32, 98, 20, -44, -12, -45, -79, -92, 23, 37, -37, -8, -103, 46, -18, 77, 81, -84, -21, 65, 53, -51, -72, 86, 18, 30, -110, 24,
            -94, 0, 1, -80, -97, 2, 33, 0, -62, -14, 43, -73, 49, -95, 102, -100, 46, 43, -106, 14, 124, 102, -40, 116, 122, -74, -91, -78, 21, 30, -65, -22,
            -10, -55, -43, 23, 101, -72, -113, -75}
    }));
    List<XCSignatureDetail> signaturesXC = new ArrayList<>();
    signaturesXC.add(sigMsg1.getXChainSignature());
    signaturesXC.add(sigMsg2.getXChainSignature());

    List<SignatureDetail> signatures = new ArrayList<>();
    signatures.add(sigMsg1.getSignature());
    signatures.add(sigMsg2.getSignature());

    ProposalAPIMessage proposalMsg = new ProposalAPIMessage(
        0.6666666666666666,
        "964d23724448a12584580b024080af123ab785995bec4a394e2274ec11232710",
        signaturesXC,
        1502720375,
        "IM000.local",
        999,
        signatures,
        "6af6e2ea7733055be4afe753d5d7c8bb546519e7496568be680ac538e4b0c1e0",
        new Location(51.4964, -0.1224, "GB", null, "82.69.97.26"),
        15,
        "",
        0.6666666666666666
    );

    APITextMessage proposalTxtMsg = new APITextMessage(new Object(), null, proposalMsg, "Update", "proposal");
    String actProposalJson = proposalTxtMsg.toJSON();

    Assert.assertEquals(expProposalJson, actProposalJson);
  }


  @Test
  public void serializeServerStatusMessage() throws Exception {
    Map<String, Integer> subscriptionData = new LinkedHashMap<>();
    subscriptionData.put("status", 0);
    subscriptionData.put("stateview", 0);
    subscriptionData.put("transaction", 1);
    subscriptionData.put("balanceview", 1);
    subscriptionData.put("terminal", 0);
    subscriptionData.put("serverstatus", 1);
    subscriptionData.put("proposal", 1);
    subscriptionData.put("block", 1);

    List<Object> scenarios = new ArrayList<>();
    scenarios.add(new Object[]{"1", false, "CHAPS"});
    scenarios.add(new Object[]{"3", false, "CLS"});
    scenarios.add(new Object[]{"2", false, "CREST"});
    scenarios.add(new Object[]{"5", false, "BACS"});
    scenarios.add(new Object[]{"4", false, "FPS"});
    scenarios.add(new Object[]{"7", false, "CONTRACT"});
    scenarios.add(new Object[]{"6", false, "LINK"});
    scenarios.add(new Object[]{"8", false, "HEARTBEAT"});

    ServerStatusAPIMessage serverStatusMsg = new ServerStatusAPIMessage(false, subscriptionData, 3, scenarios, false, 0, 34816);

    APITextMessage serverStatusTxtMsg = new APITextMessage(new Object(), null, serverStatusMsg, "Update", "serverstatus");
    String actServerStatusJson = serverStatusTxtMsg.toJSON();

    Assert.assertEquals(expServerStatusJson, actServerStatusJson);
  }


  @Test
  public void serializeTransactionTextMessage() throws Exception {
    Object[] txData = new Object[]{16, "1Jd7CswTe3YzVfo59SbgZKaLvTk5AKyCpz", "", "", "", 0, 0,
        "c584326431683429e620e5a309d4f9711219f0535253f3ae757ada429d5a275c", -1,
        "", 130219,
        "AAAAAAAAAIAAAAAFAAADHAAAAAAAAACAAAABAAAAAAUAAABYk1OHqcfCoRdbB6jemvx1kpdQbswjtKcBrt/IK/jhXVbAK6SIdSIow8xPMfPq9uBUMnaZFu/RIX3c1MFlcYgBmEYM2"
            + "S47SwXHNxY3ETsWp9aTJ7uWRexEsQAAAFi/tF7zBXYRx9+JzOFnPzM23wec7S8yobba0Qj3JGOuSwdoBoiLqh2nk5XiLh4r4b714GU4Ns1S+lwGpzCGC/k17PDPkl5zMjn0UG3Zd63n"
            + "+rxKzPlNEXbwAAAAWHmJhWbiDsKsdYD1HYT6tkexWIASV19wsQVDDlRYsV5DLjY84M/jPNN18WY/7iWuvaM8XoY2mSr5udKdfDCIw92wK/5YTWLORiBGDPRp95CvFpxEszb01aEAAABYBKzb"
            + "IT/PMKwZQ3PjO9o09Me+hxpt+TocRj+YR8zMinRQ/I9nQdFp2821hO9ZkdEmLHuVj651Gu8kgg1cmkp72wuKZpQtK9M+hMKGVkbfnTwxNbBjJHf6mAAAAFjU+NkB+rOeSxXTDRGAGeY7V9AO"
            + "qvGd/G64JnDoR5ahcYz3ACROMd3LC+W0ZFakP4kq0y7hhUtIDgJkvaLk3WTkVdsaiNwqTdDPaWwklNjhZ/iWami2oyakDFoPjH01J8YcSM5vjTb3P/0ffryb9Y802yRTCo73AC6XQTIZYmlF"
            + "qg5Z8RxkjIwJC5qtqAqvHf97Dj85LgwvVnxLauBI/yHOUiG1vJ7RoeimCEKDhJF7QSPNUJcTSfiPiG2O4Sacc3t/yZ0lY0lWCxObgGVR/qOGbO9rvUUi1ngaSoiTfHfoRS5YWmvDUmT+pVMV"
            + "MpbMp58Ca/ChVkCKF/FV90U+e5jxGaapr+zjCpttehV2bhKowr7yxN3J0doUxwLlALV3PSKn3aDi6Utjcsxu7d50C5/DTkgeQUDfGSexd9if/LPYWiKUqX6ovy0UcCGdWpHPmL7j"
            + "+bqFZeyTYRJ0onvQQgjiSUDxhCwVA5Vh7hEiaLTvKFrWqEcc4Rzpcqjf6AgT49JbomJwxs7mZFTDGvalYy50Lmn3/tcSiZ4AAAEYAAAAAAAAAIAAAAEAAAAAAgAAAFj9r"
            + "+bvIu8VU6ZWxqIGrjetYMieBU7vJoKy08wktaGyOBSasaRpgZEQpCgZiyVNnOBsa41qjrCxgQDSa+P/GXAhsuwG83126jYRDJD6xkHn7wY"
            + "/rClu3T4QAAAAWO7hC6Z8nUJD5osnCETttJpJPdIfccoyRx7c4doxslVACVy45dDA11amsCOVOP7UBFfkUed7wdLikS56uUOMQedMxliPBUzwtINOzcO1K7jWndnIzOlOZ8UH3fXmh1lpueC"
            + "olEMijPCbx4muaeTehoX0gYkgfl/WVRVut7xpOeFsTvVsMXe5wpf7BLhNl871dH1F0ZJ8qiWRddWQkj6IU31MWtZI8H71/gAAASgAAAAAAAAAgAAAAQAAAAACAAAAWPN8XODqVAGseiClghQ"
            + "+tMG0QremmYBgHxqtBsbTJmZdKPOOXMka6EfnytBKYoKKjgzW+kE5Jh1fTueRcyFx5poF5Kt4lpL30GBoOBX93k+6ULofMfm7klsAAABY6C9HSBE6/8Skr4M/Jm/eqAPiXAWLOREILmGRVp3"
            + "33TUOeN0TsxmE4pzgW2MbTwecGiVPvvrYI/GPrz4ESuIKEH9G5ja+m8Ws6pRWZWHOsx3dcxS1MjlmrDLfEoRVlDmFCujUmcPxChO8hAIKK8jCc9bz+asf60i62Uelmp/lV1yPf7q61wpZ3eG"
            + "g3uh/wTp4FkGxhO4FIB2ZziGpniZXXb/YK46HkZ4J2PqMFW8sBntD4gbagmzrvwAAARgAAAAAAAAAgAAAAQAAAAACAAAAWF78IE/hXfOW5yrzcS44TJMigFahDeLOwKw3sg9/Zgkv7sk1b2m"
            + "i8xdJRMWr5FClSAeRnd643+LxMqJBwAPWaKswUUNUqJ7uNwFTXv0vZOXPSunEW3n0C3gAAABYbuT0erzsuQ8LsDnLizJHFJ1Y16CirQ153YTySp0MdEsv1wBOgZcbWSeG6MgyvQP7fMkWmLe"
            + "DCacJLnxt+KiodIxAs9g6SRsyuhncm9VFz+p7Bgb8fXXaN/LZaCISQhqmD8YaiugMjK9xpCfgHbAAasS4OCeJPhmcsgNodDP0Jdt3qPd4RN5X+EXkbTfSD7rkVtm1QmNBtaco0w4nJQ6WZPT"
            + "IQiIFqwU5AAABGAAAAAAAAACAAAABAAAAAAIAAABYuq8NPYJQjsjl+RTb9wvTHHBEbwDByTXJYUft+IFo5SK1nmDDtN3BHhv38qycIt145I7xr2ukfyQE0CEhnQG+A1FYglZfiTOhMp29EcY"
            + "v4cMo4lJOyUpSsgAAAFj7q52WeBBmdXowsEa7KtQ+3+gNKg94o7w/YmdAOSPyMAOiTN/Xxj2PxoJuaxhqgsL9jgru6EsUQWt3Zc54hdsLx3aPjGlE8yquXbg7FzjOCd/oRoPlPfkXv6nfT8Y"
            + "fJ22pdoO05A6C7mFdJlPm5TXWCVxN7wXw3y/tidoC1mU5ahdQMDQwieQ4pWJValo7OoiIfljUTY1Y6dvb0jhatu0qAt9KERp2b+I="
    };
    List<Object> outboundTx = new ArrayList<>();
    outboundTx.add(txData);

    TransactionAPIMessage txMsg = new TransactionAPIMessage(1502720376.020737, outboundTx, "");

    APITextMessage transactionTxtMsg = new APITextMessage(new Object(), null, txMsg, "Update", "transaction");
    String actTxJson = transactionTxtMsg.toJSON();

    Assert.assertEquals(expTransactionJson, actTxJson);
  }
}
