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
package io.setl.bc.pychain.tx.create;

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_MANAGER_CONTROL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.state.tx.AddressPermissionsTx;
import io.setl.bc.pychain.state.tx.Hash;
import io.setl.common.CommonPy.TxType;
import java.util.EnumSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AddressPermissionsTest {

  @Test
  public void assetClassUnsigned() throws Exception {

    int chainID = 20;
    long nonce = 42;
    String fromPubKey = "demoPublicKey";
    String fromAddress = "demoAddress";
    String toAddress = "demoToAddress";
    String metadata = "";  // Base64(MsgPack(""))
    String poa = "";
    final long permissions = AP_MANAGER_CONTROL;
    final EnumSet<TxType> transactions = EnumSet.of(TxType.CREATE_MEMO, TxType.DO_NOTHING);

    String goodHash = "86d4432b268db55717f74f09d030429fe282b123a2ef1260d8d9abc6c0341f4c";

    AddressPermissionsTx rVal = AddressPermissions.addressPermissionsUnsigned(chainID, nonce, fromPubKey, fromAddress, toAddress, permissions, metadata,
        poa);

    rVal.setTimestamp(1501672669L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals("", rVal.getHash(), goodHash);

    AddressPermissionsTx txCopy = new AddressPermissionsTx(rVal);
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setHash(null);
    AddressPermissions thisClass = new AddressPermissions(rVal);

    txCopy = thisClass.create();
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals(txCopy.getHash(), goodHash);

    txCopy = AddressPermissionsTx.decodeTX(txCopy.encodeTx());
    txCopy.setHash(Hash.computeHash(txCopy));

    assertEquals("", txCopy.getHash(), goodHash);

    rVal.setTimestamp(rVal.getTimestamp() + 1);
    rVal.setHash(Hash.computeHash(rVal));

    assertFalse(rVal.getHash().equals(goodHash));

    // Constructor 2

    goodHash = "08e846e5476957dc30d77d64af6c0560e61950bfef479f6d61dadd50a51a9ca2";

    rVal = AddressPermissions.addressPermissionsUnsigned(chainID, nonce, fromPubKey, fromAddress, toAddress, permissions, transactions, metadata, poa);

    rVal.setTimestamp(1501672669L); // Test value

    rVal.setHash(Hash.computeHash(rVal));

    assertEquals(goodHash,rVal.getHash());

  }


  @Test
  public void getTxType() {

    AddressPermissions thisClass = new AddressPermissions();

    assertEquals(TxType.UPDATE_ADDRESS_PERMISSIONS, thisClass.getTxType());

  }


  @Before
  public void setUp() throws Exception {

  }


  @After
  public void tearDown() throws Exception {

  }

}