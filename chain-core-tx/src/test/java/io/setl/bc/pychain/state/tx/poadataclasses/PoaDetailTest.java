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
package io.setl.bc.pychain.state.tx.poadataclasses;

import static io.setl.common.StringUtils.matchString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.TxType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class PoaDetailTest {

  static final String attorneyAddress = "AttorneyAddress1";

  static final Long badDate = 99999L;

  static final Long endDate = 9999L;

  static final Long goodDate = 5555L;

  static final String issuerAddress = "IssuerAddress1";

  static final String reference = "Reference1";

  static final Long startDate = 0L;

  static int nbrTimes = 10000000;

  static String s = "somes|rn*gwinh|paces*ndun|prlinesasdsad";

  static String so = "some|string with spaces _and underlines";

  Set<String> assets1;


  @Before
  public void setUp() throws Exception {

    assets1 = new HashSet<>();
    assets1.add("Asset1");
    assets1.add("Asset2");
    assets1.add("Asset3");
  }


  @After
  public void tearDown() throws Exception {

  }


  @Test()
  public void testMatchString() throws Exception {
    //  “*met*” does not match "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

    assertTrue(matchString("Lorem ipsum dolor sit amet, consectetur adipiscing elit.", "*met*"));
    assertTrue(!matchString("Lorem ipsum dolor sit amt, consectetur adipiscing elit.", "*met*"));
    assertTrue(matchString("", "*"));
    assertTrue(!matchString("", "?"));
    assertTrue(matchString("fred", "fred"));
    assertTrue(!matchString("fred1", "fred"));
    assertTrue(!matchString("fred", "fred1"));
    assertTrue(matchString("fred1", "*???"));
    assertTrue(matchString("fred1", "*?????"));
    assertTrue(matchString("fred1", "*?*?*??*?*"));
    assertTrue(!matchString("fred1", "*??????"));
    assertTrue(!matchString("fred1", "*?????d?"));
    assertTrue(matchString("fred1", "*???d?"));
    assertTrue(matchString("fred1", "*???d?*"));
    assertTrue(matchString("fred", "fre?"));
    assertTrue(matchString("fred", "f?ed*"));
    assertTrue(matchString("fredfredfred", "fr****??***ed*f???"));

  }


  @Test()
  public void testPOADetail() throws Exception {

    final PoaItem thisItem = new PoaItem(TxType.ISSUE_ASSET, 1000000, assets1);
    final PoaItem thisItem2 = new PoaItem(TxType.REGISTER_ASSET_CLASS, 2000000, assets1);

    PoaDetail thisDetail1 = new PoaDetail(reference, issuerAddress, attorneyAddress, startDate, endDate, new PoaItem[]{thisItem});

    assertTrue(reference.equals(thisDetail1.getReference()));
    assertTrue(issuerAddress.equals(thisDetail1.getIssuerAddress()));
    assertTrue(attorneyAddress.equals(thisDetail1.getAttorneyAddress()));
    assertTrue(startDate.equals(thisDetail1.getStartTime()));
    assertTrue(endDate.equals(thisDetail1.getEndTime()));
    assertTrue(thisDetail1.getItem(thisItem.getTxType()).get(0).equals(thisItem));
    assertTrue(thisDetail1.getItem(TxType.WFL_HOLD1) == null);
    assertTrue(thisDetail1.getSumAmounts().equals(1000000));

    // Copy

    PoaDetail thisDetail2 = new PoaDetail(thisDetail1);
    PoaDetail thisDetail3 = new PoaDetail(new MPWrappedArrayImpl(thisDetail1.encode()));

    // Test Copy.

    assertTrue(thisDetail1.equals(thisDetail2));
    assertTrue(thisDetail1.equals(thisDetail3));
    assertTrue(thisDetail2.equals(thisDetail3));

    //
    thisDetail1 = new PoaDetail(reference, issuerAddress, attorneyAddress, startDate, endDate, new PoaItem[]{thisItem, thisItem2});

    assertTrue(thisDetail1.getSumAmounts().equals(3000000));

    // Copy2

    thisDetail2 = new PoaDetail(thisDetail1);
    thisDetail3 = new PoaDetail(new MPWrappedArrayImpl(thisDetail1.encode()));

    // Test Copy.

    assertTrue(thisDetail1.equals(thisDetail2));
    assertTrue(thisDetail1.equals(thisDetail3));
    assertTrue(thisDetail2.equals(thisDetail3));

    // Equals
    thisDetail1 = new PoaDetail(reference, issuerAddress, attorneyAddress, startDate, endDate, new PoaItem[]{thisItem});

    assertFalse(thisDetail1.equals(null));
    assertFalse(thisDetail1.equals(thisItem));
    assertFalse(thisDetail1.equals(new PoaDetail("", "", "", badDate, badDate, new PoaItem[]{})));
    assertFalse(thisDetail1.equals(new PoaDetail(reference, "", "", badDate, badDate, new PoaItem[]{})));
    assertFalse(thisDetail1.equals(new PoaDetail(reference, issuerAddress, attorneyAddress, badDate, badDate, new PoaItem[]{})));
    assertFalse(thisDetail1.equals(new PoaDetail(reference, issuerAddress, attorneyAddress, startDate, badDate, new PoaItem[]{})));
    assertFalse(thisDetail1.equals(new PoaDetail(reference, issuerAddress, attorneyAddress, startDate, endDate, new PoaItem[]{})));

    assertTrue(thisDetail1.equals(new PoaDetail(reference, issuerAddress, attorneyAddress, startDate, endDate, new PoaItem[]{thisItem})));

  }


  @Test()
  public void testPOAItem() throws Exception {

    final PoaItem thisItem = new PoaItem(TxType.ISSUE_ASSET, 1000000, assets1);

    assertTrue(thisItem.getAmount().equals(1000000));
    assertTrue(thisItem.getTxType() == TxType.ISSUE_ASSET);
    assertEquals(thisItem.getAssets(), assets1);

    // Match Asset
    assets1.forEach(thisAsset -> assertTrue(thisItem.matchAsset(thisAsset)));
    assertFalse(thisItem.matchAsset("MissingDrossAsset"));

    // Copy

    PoaItem thisItem2 = new PoaItem(thisItem);
    PoaItem thisItem3 = new PoaItem(thisItem.encode());

    // Test Copy.

    assertTrue(thisItem.equals(thisItem2));
    assertTrue(thisItem.equals(thisItem3));
    assertTrue(thisItem3.equals(thisItem2));

    // Test Verify.

    assertTrue(PoaItem.verifyItemData(thisItem.encode()));

    // Test Consume

    assertTrue(thisItem.consume(100000L).equals(900000L));
    assertTrue(thisItem.getAmount().equals(900000L));
    assertFalse(thisItem.consumed());
    assertFalse(thisItem.equals(thisItem2));

    assertTrue(thisItem.consume(900000L).equals(0));
    assertTrue(thisItem.consumed());
    assertFalse(thisItem.equals(thisItem2));

    // Equals
    PoaItem thisItem4 = new PoaItem(TxType.ISSUE_ASSET, 1000000, assets1);

    assertFalse(thisItem4.equals(null));
    assertFalse(thisItem4.equals("Not an Item"));
    assertFalse(thisItem4.equals(new PoaItem(TxType.WFL_HOLD2, 0, new ArrayList<>())));
    assertFalse(thisItem4.equals(new PoaItem(TxType.ISSUE_ASSET, 0, new ArrayList<>())));
    assertFalse(thisItem4.equals(new PoaItem(TxType.ISSUE_ASSET, 1000000, new ArrayList<>())));

    assertTrue(thisItem4.equals(new PoaItem(TxType.ISSUE_ASSET, 1000000, assets1)));
  }


}