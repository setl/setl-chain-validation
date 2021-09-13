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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.setl.crypto.KeyGen;
import io.setl.crypto.KeyGen.Type;
import io.setl.crypto.provider.EdDsaPrivateKey;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;

public class AddressUtilTest {

  String address1;

  String address1c;

  byte[] bPubKey1;

  byte[] bprivKey1;

  String hPrivHey1;

  String hPubKey1;


  @Test
  public void publicKeyToAddress() {

    AddressUtil test = new AddressUtil(); // Pointless constructor.

    // All done as edsaToAddress()
    boolean oldValue = AddressUtil.setUseBase58(true);
    try {
      assertEquals("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.NORMAL.getId()));
      assertEquals("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.NORMAL.getId(), Integer.MAX_VALUE));
      assertEquals("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.NORMAL.getId(), Long.MAX_VALUE));
      assertEquals("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL.getId()));
      assertEquals("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL.getId(), Integer.MAX_VALUE));
      assertEquals("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL.getId(), Long.MAX_VALUE));
      assertEquals("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL));

      // Contract edsaToAddress()

      assertEquals("3CQRMEbk4Wtpn4bjmA8AphfdkMXE9FZHtR", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.CONTRACT.getId()));
      assertEquals("3589yWfwjRtAAn8GmWLoe86dPafuubM8Kh", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.CONTRACT.getId(), Integer.MAX_VALUE));
      assertEquals("371TEVMBFE1Sr71CPrdUDZE1LiE2VT3zpy", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.CONTRACT.getId(), Long.MAX_VALUE));

      //

      byte[] bNot32Bytes = Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAETVNsIKZ6ud4rYcolv/QRA6hWBTUIl0UU5Z+9u8DzsfIicjZZOwHB3BCP+D9MnshAA24gWqGECzJCmdw6T+872Q==");

      // NORMAL addresses, context not used.
      assertEquals("AFQ_aZPmJs0D2hy7QL9T11eQ-79EUYUr0g",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.NORMAL.getId()));
      assertEquals("AFQ_aZPmJs0D2hy7QL9T11eQ-79EUYUr0g",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.NORMAL.getId(), Integer.MAX_VALUE));
      assertEquals("AFQ_aZPmJs0D2hy7QL9T11eQ-79EUYUr0g",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.NORMAL.getId(), Long.MAX_VALUE));

      // CONTRACT addresses, context is used. Note these tests pass in an invalid key, so it is reasonable for them to fail on those grounds in the future.
      bNot32Bytes = Arrays.copyOf(bprivKey1, 30);
      assertEquals("3JeBDUJzmfhcvy4GXGGguVmpChJikD5Zzt",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.CONTRACT.getId()));
      assertEquals("3PmUBjvNhrXAs8BrXP4TS2z2iph7ei1tu9", AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.CONTRACT.getId(), Integer.MAX_VALUE));
      assertEquals("3Gnd1xFZbk2XCcwbkfKJcoYxx7XweFCbUo", AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.CONTRACT.getId(), Long.MAX_VALUE));
    } finally {
      AddressUtil.setUseBase58(oldValue);
    }
  }


  @Test
  public void publicKeyToAddress2() {

    AddressUtil test = new AddressUtil(); // Pointless constructor.

    boolean oldValue = AddressUtil.setUseBase58(false);
    try {
      assertEquals("ADCWHXOsUOctwFTm50KOo6TZhFz8T2-TDA", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.NORMAL.getId()));
      assertEquals("ADCWHXOsUOctwFTm50KOo6TZhFz8T2-TDA", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.NORMAL.getId(), Integer.MAX_VALUE));
      assertEquals("ADCWHXOsUOctwFTm50KOo6TZhFz8T2-TDA", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.NORMAL.getId(), Long.MAX_VALUE));
      assertEquals("ADCWHXOsUOctwFTm50KOo6TZhFz8T2-TDA", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL.getId()));
      assertEquals("ADCWHXOsUOctwFTm50KOo6TZhFz8T2-TDA", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL.getId(), Integer.MAX_VALUE));
      assertEquals("ADCWHXOsUOctwFTm50KOo6TZhFz8T2-TDA", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL.getId(), Long.MAX_VALUE));
      assertEquals("ADCWHXOsUOctwFTm50KOo6TZhFz8T2-TDA", AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL));

      // Contract edsaToAddress()

      assertEquals("FDCWHXOsUOctwFTm50KOo6TZhFz8GHmOVQ", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.CONTRACT.getId()));
      assertEquals("FO4pY6LWwTrTcM5f2hm6GcCL-jJvhELjHg", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.CONTRACT.getId(), Integer.MAX_VALUE));
      assertEquals("FGQkrEbZ4H5FY9taBn5cZmD76y59kGy4_Q", AddressUtil.publicKeyToAddress(bPubKey1, AddressType.CONTRACT.getId(), Long.MAX_VALUE));

      //

      byte[] bNot32Bytes = Base64.getDecoder().decode("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAETVNsIKZ6ud4rYcolv/QRA6hWBTUIl0UU5Z+9u8DzsfIicjZZOwHB3BCP+D9MnshAA24gWqGECzJCmdw6T+872Q==");

      // NORMAL addresses, context not used.
      assertEquals("AFQ_aZPmJs0D2hy7QL9T11eQ-79EUYUr0g",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.NORMAL.getId()));
      assertEquals("AFQ_aZPmJs0D2hy7QL9T11eQ-79EUYUr0g",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.NORMAL.getId(), Integer.MAX_VALUE));
      assertEquals("AFQ_aZPmJs0D2hy7QL9T11eQ-79EUYUr0g",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.NORMAL.getId(), Long.MAX_VALUE));

      // CONTRACT addresses, context is used.
      assertEquals("FFQ_aZPmJs0D2hy7QL9T11eQ-79EBpM2iw",AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.CONTRACT.getId()));
      assertEquals("FGKa5BZ9zZaDziRXI_1qMRr3Fnk-iu_GVw", AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.CONTRACT.getId(), Integer.MAX_VALUE));
      assertEquals("FEs2_YKK7BVvqgeptUTMZVKkN47LYKShgg", AddressUtil.publicKeyToAddress(bNot32Bytes, AddressType.CONTRACT.getId(), Long.MAX_VALUE));
    } finally {
      AddressUtil.setUseBase58(oldValue);
    }
  }

  @Before
  public void setUp() throws Exception {
    MessageDigest digest = Sha256Hash.newDigest();
    bprivKey1 = digest.digest("SETL Blockchain".getBytes());
    hPrivHey1 = Hex.encode(bprivKey1);
    bPubKey1 = new EdDsaPrivateKey(bprivKey1).getPublicKey().getKeyBytes();
    hPubKey1 = Hex.encode(bPubKey1);
    address1 = AddressUtil.publicKeyToAddress(hPubKey1, AddressType.NORMAL);
    address1c = AddressUtil.publicKeyToAddress(hPubKey1, AddressType.CONTRACT);

  }


  @Test
  public void verifyAddress() {

    assertTrue(AddressUtil.verifyAddress("1BiQRh7JWcaSgtuJe4TaQ5JhbqEWfEhqay"));
  }


  @Test
  public void verifyPublicKey() {

    assertTrue(AddressUtil.verifyPublicKey(hPubKey1));
  }
}
