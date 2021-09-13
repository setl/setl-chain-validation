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
package io.setl.bc.pychain.wallet;

import static org.junit.Assert.assertEquals;

import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.AddressType;
import io.setl.crypto.KeyGen;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessageUnpacker;

public class WalletAddressTest {

  @Test
  public void encode() throws Exception {
    final int id = 42;

    // Default Vault
    TestWallets.reset();

    // Simple Key details.
    byte[] keyBytes = Base64.getDecoder().decode(
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDx8qawU4x1AF7mWbbvx/CognvTtiIkQbOw5jSl52jFMCadAS+16mwYoK"
            + "+pjs2kiR1YzbByxCS3rZwiJCr3l3ag==");
    PublicKey publicKey = KeyGen.getPublicKey(keyBytes);
    keyBytes = Base64.getDecoder().decode("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDFfdBsDS/xJfOWJvZff7MNiDbp17ccxZmgDmXOuO+BHA==");
    PrivateKey privateKey = KeyGen.getPrivateKey(keyBytes);
    KeyPair pair = new KeyPair(publicKey, privateKey);

    // New Wallet
    WalletAddress wa = new WalletAddress(id, pair, AddressType.NORMAL);

    // Test Copy
    MessageBufferPacker pp = MsgPackUtil.newBufferPacker();
    wa.pack(pp);
    pp.close();

    MessageUnpacker qq = MsgPackUtil.newUnpacker(pp.toByteArray());
    WalletAddress wa3 = new WalletAddress(qq);

    // Test Copied OK.
    assertEquals(wa, wa3);

    // Test values.
    assertEquals("AOwfUtiocU1z7d-z4QIZb2vaP0-2dZqQHg", wa.getAddress());
    assertEquals(wa.getAddressType(), AddressType.NORMAL);
    assertEquals(wa.getHexPublicKey(),
        "3059301306072a8648ce3d020106082a8648ce3d030107034200040f1f2a6b0538c75005ee659b6efc7f0a8827bd3b6222441b3b0e634a5e768c530269d012fb5ea6c18a0afa98ecd"
            + "a4891d58cdb072c424b7ad9c22242af79776a");
    assertEquals("EC", wa.getKeyType());
    assertEquals(42, wa.getLeiId());
    assertEquals(0, wa.getNonce());
    assertEquals(wa.getPrivateKey(), pair.getPrivate());
    assertEquals(wa.getPublicKey(), pair.getPublic());

    assertEquals("AOwfUtiocU1z7d-z4QIZb2vaP0-2dZqQHg", wa3.getAddress());
    assertEquals(wa3.getAddressType(), AddressType.NORMAL);
    assertEquals(wa3.getHexPublicKey(),
        "3059301306072a8648ce3d020106082a8648ce3d030107034200040f1f2a6b0538c75005ee659b6efc7f0a8827bd3b6222441b3b0e634a5e768c530269d012fb5ea6c18a0afa98ecd"
            + "a4891d58cdb072c424b7ad9c22242af79776a");
    assertEquals("EC", wa3.getKeyType());
    assertEquals(42, wa3.getLeiId());
    assertEquals(0, wa3.getNonce());
    assertEquals(wa3.getPrivateKey(), pair.getPrivate());
    assertEquals(wa3.getPublicKey(), pair.getPublic());
    assertEquals(wa.hashCode(), wa3.hashCode());


  }
}