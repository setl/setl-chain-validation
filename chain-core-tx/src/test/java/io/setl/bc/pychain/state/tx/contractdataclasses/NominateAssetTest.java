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
package io.setl.bc.pychain.state.tx.contractdataclasses;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class NominateAssetTest {

  @Test
  public void nominateAssetTest() throws Exception {

    String namespace = "namespace";
    String classid = "classid";
    Long blocksize = 567L;
    String address = "address";
    String reference = "reference";
    String publickey = "publickey";
    String signature = "signature";

    final NominateAsset n1 = new NominateAsset(namespace, classid, blocksize);
    final NominateAsset n2 = new NominateAsset(n1);
    final NominateAsset n3 = new NominateAsset(n1.encode(0));
    final NominateAsset n4 = new NominateAsset(new MPWrappedArrayImpl(n1.encode(0)));

    // equals

    assertTrue(n1.equals(n2));
    assertTrue(n1.equals(n3));
    assertTrue(n1.equals(n4));
    assertTrue(n4.equals(n3));
    assertTrue(n4.equals(n1));
    assertTrue(n4.equals(n2));


    final NominateAsset n11 = new NominateAsset(namespace, classid, blocksize, address, reference, publickey, signature);
    final NominateAsset n12 = new NominateAsset(n11);
    final NominateAsset n13 = new NominateAsset(n11.encode(0));
    final NominateAsset n14 = new NominateAsset(new MPWrappedArrayImpl(n11.encode(0)));

    // equals

    assertTrue(n11.equals(n12));
    assertTrue(n11.equals(n13));
    assertTrue(n11.equals(n14));
    assertTrue(n14.equals(n13));
    assertTrue(n14.equals(n11));
    assertTrue(n14.equals(n12));


    final NominateAsset bad1 = new NominateAsset("Bad", classid, blocksize);
    final NominateAsset bad2 = new NominateAsset(namespace, "Bad", blocksize);

    assertFalse(bad1.equals(bad2));

    assertFalse(n1.equals(null));
    assertFalse(n1.equals(""));
    assertFalse(n1.equals(new NominateAsset("", "", 0L)));
    assertFalse(n1.equals(new NominateAsset(namespace, "", 0L)));
    assertFalse(n1.equals(new NominateAsset(namespace, classid, 0L)));

  }


}