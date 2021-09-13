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
package io.setl.common;

import static io.setl.common.CommonPy.TxType.GRANT_VOTING_POWER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.setl.common.CommonPy.AuthorisedAddressConstants;
import io.setl.common.CommonPy.ConfigConstants;
import io.setl.common.CommonPy.ContractConstants;
import io.setl.common.CommonPy.ItemType;
import io.setl.common.CommonPy.NodeType;
import io.setl.common.CommonPy.P2PType;
import io.setl.common.CommonPy.TxGeneralFields;
import io.setl.common.CommonPy.TxType;
import io.setl.common.CommonPy.XChainParameters;
import io.setl.common.CommonPy.XChainStatus;
import io.setl.common.CommonPy.XChainTypes;
import java.io.IOException;
import java.util.Set;
import org.junit.Test;

public class TxTypeTest {

  @Test
  public void runTest() {

    // Test to see if this affects code coverage.

    assertTrue(GRANT_VOTING_POWER.getAbbreviation().equals("stbon"));
    assertTrue(GRANT_VOTING_POWER.getPermissionId() == 101);
    assertTrue(GRANT_VOTING_POWER.getId() == 0x01);
    assertTrue(GRANT_VOTING_POWER.getPriority() == 0x00);

    assertTrue(TxType.get(0x01).equals(GRANT_VOTING_POWER));
    assertTrue(TxType.get("stbon").equals(GRANT_VOTING_POWER));
    assertTrue(TxType.get("1").equals(GRANT_VOTING_POWER));
    assertTrue(TxType.get("crujd") == null);

    assertTrue(ItemType.PROPOSAL.getId().equals("proposal"));

    assertTrue(NodeType.Validation.id == 0x00);
    assertTrue(NodeType.Witness.id == 0x10);
    assertTrue(NodeType.Wallet.id == 0x20);
    assertTrue(NodeType.Report.id == 0x30);
    assertTrue(NodeType.Cobalt.id == 0x40);
    assertTrue(NodeType.TxLoader.id == 0x50);

    assertTrue(P2PType.PROPOSAL.getId() == 0x02);
    assertTrue(P2PType.get(0x02).equals(P2PType.PROPOSAL));

    Set<String> s0 = AuthorisedAddressConstants.forCode(256L);
    assertTrue(((String) s0.toArray()[0]).equals("bond"));
    assertTrue(AuthorisedAddressConstants.forCode(null) == null);
    assertTrue(AuthorisedAddressConstants.AP_BOND == 0x000100);
    assertTrue(AuthorisedAddressConstants.forName("bond") == 256L);
    assertTrue(AuthorisedAddressConstants.forName(null) == 0L);
    assertTrue(AuthorisedAddressConstants.forName("256") == 256L);
    assertTrue(AuthorisedAddressConstants.forNames(s0) == 256L);
    assertTrue(AuthorisedAddressConstants.forNames(null) == 0L);

    assertTrue(ConfigConstants.DEFAULT_MAX_TX_AGE == 86400L);

    assertTrue(ContractConstants.CONTRACT_NAME_DVP_UK.equals("dvp_uk"));

    assertTrue(TxGeneralFields.TX_CHAIN == 0);

    assertTrue(XChainParameters.AcceptAddress == 0x0001);
    s0 = XChainParameters.forCode(1L);
    assertTrue(((String) s0.toArray()[0]).equals("acceptaddress"));
    assertTrue(XChainParameters.forName("acceptaddress") == 1L);
    assertTrue(XChainParameters.forName("1") == 1L);
    assertTrue(XChainParameters.forNames(s0) == 1L);
    assertTrue(XChainParameters.toList(1L).equals("acceptaddress"));

    assertTrue(XChainStatus.InitialClassSynchComplete == 0x8000);

    assertTrue(XChainTypes.CHAIN_ADDRESS_FORMAT.equals("Chain_%d"));
  }

  public static class Holder {
    private TxType txType =TxType.EXERCISE_ENCUMBRANCE;


    public TxType getTxType() {
      return txType;
    }


    public void setTxType(TxType txType) {
      this.txType = txType;
    }
  }

  @Test
  public void testJacksonEncoding() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    String msg = objectMapper.writeValueAsString(new Holder());
    System.out.println(msg);
  }


  @Test
  public void testJacksonDecoding() throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    Holder h = objectMapper.readValue("{\"txType\":\"ISSUE_ASSET\"}", Holder.class);
    assertEquals(TxType.ISSUE_ASSET,h.getTxType());

    h = objectMapper.readValue("{\"txType\":\"nsreg\"}", Holder.class);
    assertEquals(TxType.REGISTER_NAMESPACE,h.getTxType());

    h = objectMapper.readValue("{\"txType\":\"NsReg\"}", Holder.class);
    assertEquals(TxType.REGISTER_NAMESPACE,h.getTxType());

    h = objectMapper.readValue("{\"txType\":\"#8\"}", Holder.class);
    assertEquals(TxType.REGISTER_ADDRESS,h.getTxType());
  }
}
