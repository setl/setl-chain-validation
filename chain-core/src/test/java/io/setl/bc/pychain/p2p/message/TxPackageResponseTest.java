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
package io.setl.bc.pychain.p2p.message;

import static org.junit.Assert.*;

import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.create.NullTX;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.CommonPy.P2PType;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-10.
 */
public class TxPackageResponseTest {

  UUID uuid = UUID.randomUUID();
  TxPackageResponse instance;
  Txi[] txis;
  Object[][] encoded;

  @Before
  public void before() {
    txis = new Txi[] {
        NullTX.nullUnsigned(1,2,"","",""),
        NullTX.nullUnsigned(1,3,"","",""),
        NullTX.nullUnsigned(1,4,"","",""),
    };

    encoded = new Object[txis.length][];
    for(int i=0;i<txis.length;i++) {
      encoded[i] = txis[i].encodeTx();
    }

    instance = new TxPackageResponse(1,uuid,encoded);
  }

  @Test
  public void encode() {
    byte[] data = MsgPackUtil.pack(instance);
    TxPackageResponse response = new TxPackageResponse(MsgPackUtil.unpackWrapped(data,true));
    assertEquals(instance.getChainId(),response.getChainId());
    assertEquals(instance.getType(),response.getType());
    assertEquals(instance.getUuid(),response.getUuid());
  }


  @Test
  public void getType() {
    assertEquals(P2PType.TX_PACKAGE_RESPONSE,instance.getType());
  }


  @Test
  public void getUuid() {
    assertEquals(uuid,instance.getUuid());
  }
}