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
package io.setl.bc.pychain.serialise.hash;

import static org.junit.Assert.*;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.HashableObjectArray;
import io.setl.common.Balance;
import io.setl.common.Hex;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-07.
 */
public class HashSerialisationTest {

  @Test
  public void getInstance() {
    assertNotNull(HashSerialisation.getInstance());
  }


  @Test
  public void hash() {
    Balance balance = new Balance(0x0123456789abcdefL);
    Hash h = HashSerialisation.getInstance().hash(balance);
    assertEquals("b8c3a13780c155b78ea70ec7580724801fba20ce9b836ab3708b3b2d8f205860",h.toHexString());
  }


  @Test
  public void hash1() {
    HashableObjectArray hoa = new HashableObjectArray() {
      @Override
      public Object[] getHashableObject() {
        return new Object[] { "Hello, World!"};
      }
    };
    Hash h = HashSerialisation.getInstance().hash(hoa);
    assertEquals("7f68a811203ed8a9cd5b4630cd9499e27f578aa5e69edcb3b856c0fd5c8b73de",h.toHexString());
  }


  @Test
  public void serialise() {
    Balance balance = new Balance(0x0123456789abcdefL);
    byte[] bytes = HashSerialisation.getInstance().serialise(balance);
    assertEquals("cf0123456789abcdef", Hex.encode(bytes));
  }

}