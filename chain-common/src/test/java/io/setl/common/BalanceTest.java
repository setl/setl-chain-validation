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
import static org.junit.Assert.assertFalse;

import java.math.BigInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unlikely-arg-type")
public class BalanceTest {

  Integer v1 = 123456;
  Integer v2 = -987654;
  Long v3 = 9123456L;
  Long v4 = -10987654L;
  BigInteger bv1 = BigInteger.valueOf(v1);
  BigInteger bv2 = BigInteger.valueOf(v2);
  BigInteger bv3 = BigInteger.valueOf(v3);
  BigInteger bv4 = BigInteger.valueOf(v4);

  BigInteger bv5 = new BigInteger("123456789012345678901234567890123456789012345678901234567890");
  BigInteger bv6 = new BigInteger("-523456789012345678901234567890123456789012345678901234567890");
  BigInteger bv7 = new BigInteger("923456789012345678901234567890123456789012345678901234567890");

  @Before
  public void setUp() throws Exception {
  }


  @After
  public void tearDown() throws Exception {
  }


  @Test
  public void abs() {


    assertTrue(Balance.abs(new Balance(v1)).equalTo(Math.abs(v1)));
    assertTrue(Balance.abs(new Balance(v2)).equalTo(Math.abs(v2)));
    assertTrue(Balance.abs(new Balance(v3)).equalTo(Math.abs(v3)));
    assertTrue(Balance.abs(new Balance(v4)).equalTo(Math.abs(v4)));

    assertTrue(Balance.abs(new Balance(bv5)).equalTo(bv5.abs()));
    assertTrue(Balance.abs(new Balance(bv6)).equalTo(bv6.abs()));
    assertTrue(Balance.abs(new Balance(bv7)).equalTo(bv7.abs()));

  }


  @Test
  public void max() {

    assertTrue(Balance.max(new Balance(v1), new Balance(v2)).equalTo(Math.max(v1, v2)));
    assertTrue(Balance.max(new Balance(v4), new Balance(v2)).equalTo(Math.max(v4, v2)));
    assertTrue(Balance.max(new Balance(v1), new Balance(bv1)).equalTo(v1));
    assertTrue(Balance.max(new Balance(v1), new Balance(bv1)).equalTo(bv1));
    assertTrue(Balance.max(new Balance(bv2), new Balance(bv3)).equalTo(v3));
    assertTrue(Balance.max(new Balance(bv5), new Balance(bv6)).equalTo(bv5));

  }


  @Test
  public void min() {

    assertTrue(Balance.min(new Balance(v1), new Balance(v2)).equalTo(Math.min(v1, v2)));
    assertTrue(Balance.min(new Balance(v4), new Balance(v2)).equalTo(Math.min(v4, v2)));
    assertTrue(Balance.min(new Balance(v1), new Balance(bv1)).equalTo(v1));
    assertTrue(Balance.min(new Balance(v1), new Balance(bv1)).equalTo(bv1));
    assertTrue(Balance.min(new Balance(bv2), new Balance(bv3)).equalTo(v2));
    assertTrue(Balance.min(new Balance(bv5), new Balance(bv6)).equalTo(bv6));

  }


  @Test
  public void add() {

    assertTrue((new Balance(v1)).add(v1).equalTo(v1 * 2));
    assertTrue((new Balance(v1)).add(new Balance(v1)).equalTo(v1 * 2));
    assertTrue((new Balance(v1)).add(new Balance(v1)).equalTo(new Balance(v1 * 2)));

    assertTrue((new Balance(v1)).add(bv7).equalTo(bv7.add(bv1)));
    assertTrue((new Balance(v2)).add(bv6).equalTo(bv2.add(bv6)));
    assertTrue((new Balance(v3)).add(bv6).equalTo(bv3.add(bv6)));
    assertTrue((new Balance(v3)).add(bv5).equalTo(bv3.add(bv5)));

  }


  @Test
  public void bigintValue() {

    assertTrue((new Balance(0)).bigintValue().equals(BigInteger.ZERO));
    assertTrue((new Balance(v1)).bigintValue().equals(bv1));
    assertTrue((new Balance(v4)).bigintValue().equals(bv4));
    assertFalse((new Balance(v1)).bigintValue().equals(v1));
  }


  @Test
  public void compareTo() {

    assertTrue((new Balance(v1)).compareTo(new Balance(v1)) == 0);
    assertTrue((new Balance(v1)).compareTo(v1) == 0);
    assertTrue((new Balance(v1)).compareTo(new Balance(v2)) == v1.compareTo(v2));

    assertTrue((new Balance(v2)).compareTo(null) == v2.compareTo(0));
    assertTrue((new Balance(v3)).compareTo(null) == v3.compareTo(0L));

    assertTrue((new Balance(bv5)).compareTo(new Balance(bv6)) == bv5.compareTo(bv6));
    assertTrue((new Balance(bv5)).compareTo(new Balance(bv7)) == bv5.compareTo(bv7));

  }


  @Test
  public void divideBy() {

    // Specific 'null' case
    assertTrue((new Balance(v1)).divideBy(null).equals(Balance.BALANCE_ZERO));

    assertTrue((new Balance(v1)).divideBy(new Balance(v1)).equalTo(1));

    assertTrue((new Balance(bv5)).divideBy(BigInteger.valueOf(1L)).equalTo(bv5));
    assertTrue((new Balance(bv5)).divideBy(1L).equalTo(bv5));
    assertTrue((new Balance(v2)).divideBy(BigInteger.valueOf(1L)).equalTo(v2));
    assertTrue((new Balance(v2)).divideBy(1L).equalTo(v2));

  }


  @Test
  public void doubleValue() {

    assertTrue((new Balance(0)).doubleValue() == 0);
    assertTrue((new Balance(v1)).doubleValue() == v1);
    assertFalse(v1.equals((new Balance(v1)).doubleValue()));

  }


  @Test
  public void equalTo() {

    assertFalse((new Balance(v2)).equalTo(null));
    assertTrue((new Balance(v2)).equalTo(new Balance(v2)));
    assertTrue((new Balance(v2)).equalTo(v2));
    assertTrue((new Balance(v2)).equalTo(bv2));
    assertTrue((new Balance(bv7)).equalTo(bv7));

  }


  @Test
  public void equals() {

    assertTrue((new Balance(v2)).equals(new Balance(v2)));

  }


  @Test
  public void floatValue() {

    assertTrue((new Balance(0)).floatValue() == 0);
    assertTrue((new Balance(v1)).floatValue() == v1);
    assertFalse(v1.equals((new Balance(v1)).floatValue()));

  }


  @Test
  public void getValue() {

    assertTrue((new Balance(v1)).getValue() instanceof Long);
    assertFalse((new Balance(v1)).getValue() instanceof BigInteger);
    assertTrue((new Balance(bv6)).getValue() instanceof BigInteger);
    assertFalse((new Balance(bv6)).getValue() instanceof Long);

  }


  @Test
  public void greaterThan() {

    assertTrue((new Balance(v1)).greaterThan(v1 - 1));
    assertFalse((new Balance(v1)).greaterThan(v1));
    assertFalse((new Balance(v1)).greaterThan(v1 + 1));

    assertTrue((new Balance(v2)).greaterThan(v2 - 1));
    assertFalse((new Balance(v2)).greaterThan(v2));
    assertFalse((new Balance(v2)).greaterThan(v2 + 1));

    assertTrue((new Balance(bv6)).greaterThan(bv6.subtract(BigInteger.ONE)));
    assertFalse((new Balance(bv6)).greaterThan(bv6));
    assertFalse((new Balance(bv6)).greaterThan(bv6.add(BigInteger.ONE)));

    assertTrue((new Balance(bv7)).greaterThan(bv7.subtract(BigInteger.ONE)));
    assertFalse((new Balance(bv7)).greaterThan(bv7));
    assertFalse((new Balance(bv7)).greaterThan(bv7.add(BigInteger.ONE)));

  }


  @Test
  public void greaterThanEqualTo() {

    assertTrue((new Balance(v1)).greaterThanEqualTo(v1 - 1));
    assertTrue((new Balance(v1)).greaterThanEqualTo(v1));
    assertFalse((new Balance(v1)).greaterThanEqualTo(v1 + 1));

    assertTrue((new Balance(v2)).greaterThanEqualTo(v2 - 1));
    assertTrue((new Balance(v2)).greaterThanEqualTo(v2));
    assertFalse((new Balance(v2)).greaterThanEqualTo(v2 + 1));

    assertTrue((new Balance(bv6)).greaterThanEqualTo(bv6.subtract(BigInteger.ONE)));
    assertTrue((new Balance(bv6)).greaterThanEqualTo(bv6));
    assertFalse((new Balance(bv6)).greaterThanEqualTo(bv6.add(BigInteger.ONE)));

    assertTrue((new Balance(bv7)).greaterThanEqualTo(bv7.subtract(BigInteger.ONE)));
    assertTrue((new Balance(bv7)).greaterThanEqualTo(bv7));
    assertFalse((new Balance(bv7)).greaterThanEqualTo(bv7.add(BigInteger.ONE)));

  }


  @Test
  public void greaterThanEqualZero() {

    assertTrue((new Balance(v1)).greaterThanEqualZero());

    assertFalse((new Balance(v2)).greaterThanEqualZero());

    assertFalse((new Balance(bv6)).greaterThanEqualZero());

    assertTrue((new Balance(bv7)).greaterThanEqualZero());

    assertTrue((new Balance(0L)).greaterThanEqualZero());

  }


  @Test
  public void greaterThanZero() {

    assertTrue((new Balance(v1)).greaterThanZero());

    assertFalse((new Balance(v2)).greaterThanZero());

    assertFalse((new Balance(bv6)).greaterThanZero());

    assertTrue((new Balance(bv7)).greaterThanZero());

    assertFalse((new Balance(0L)).greaterThanZero());

  }


  @Test
  public void intValue() {

    assertTrue((new Balance(0)).intValue() == 0);
    assertTrue((new Balance(v1)).intValue() == v1);
    assertTrue((new Balance(v2)).intValue() == v2);


  }


  @Test
  public void lessThan() {

    assertFalse((new Balance(v1)).lessThan(v1 - 1));
    assertFalse((new Balance(v1)).lessThan(v1));
    assertTrue((new Balance(v1)).lessThan(v1 + 1));

    assertFalse((new Balance(v2)).lessThan(v2 - 1));
    assertFalse((new Balance(v2)).lessThan(v2));
    assertTrue((new Balance(v2)).lessThan(v2 + 1));

    assertFalse((new Balance(bv6)).lessThan(bv6.subtract(BigInteger.ONE)));
    assertFalse((new Balance(bv6)).lessThan(bv6));
    assertTrue((new Balance(bv6)).lessThan(bv6.add(BigInteger.ONE)));

    assertFalse((new Balance(bv7)).lessThan(bv7.subtract(BigInteger.ONE)));
    assertFalse((new Balance(bv7)).lessThan(bv7));
    assertTrue((new Balance(bv7)).lessThan(bv7.add(BigInteger.ONE)));

  }


  @Test
  public void lessThanEqualTo() {

    assertFalse((new Balance(v1)).lessThanEqualTo(v1 - 1));
    assertTrue((new Balance(v1)).lessThanEqualTo(v1));
    assertTrue((new Balance(v1)).lessThanEqualTo(v1 + 1));

    assertFalse((new Balance(v2)).lessThanEqualTo(v2 - 1));
    assertTrue((new Balance(v2)).lessThanEqualTo(v2));
    assertTrue((new Balance(v2)).lessThanEqualTo(v2 + 1));

    assertFalse((new Balance(bv6)).lessThanEqualTo(bv6.subtract(BigInteger.ONE)));
    assertTrue((new Balance(bv6)).lessThanEqualTo(bv6));
    assertTrue((new Balance(bv6)).lessThanEqualTo(bv6.add(BigInteger.ONE)));

    assertFalse((new Balance(bv7)).lessThanEqualTo(bv7.subtract(BigInteger.ONE)));
    assertTrue((new Balance(bv7)).lessThanEqualTo(bv7));
    assertTrue((new Balance(bv7)).lessThanEqualTo(bv7.add(BigInteger.ONE)));


  }


  @Test
  public void lessThanEqualZero() {

    assertFalse((new Balance(v1)).lessThanEqualZero());

    assertTrue((new Balance(v2)).lessThanEqualZero());

    assertTrue((new Balance(bv6)).lessThanEqualZero());

    assertFalse((new Balance(bv7)).lessThanEqualZero());

    assertTrue((new Balance(0L)).lessThanEqualZero());

  }


  @Test
  public void lessThanZero() {

    assertFalse((new Balance(v1)).lessThanZero());

    assertTrue((new Balance(v2)).lessThanZero());

    assertTrue((new Balance(bv6)).lessThanZero());

    assertFalse((new Balance(bv7)).lessThanZero());

    assertFalse((new Balance(0L)).lessThanZero());

  }


  @Test
  public void longValue() {

    assertEquals(0, (new Balance(0)).longValue());
    assertEquals((new Balance(v1)).longValue(), (int) v1);
    assertFalse(v1.equals((new Balance(v1)).longValue()));

  }


  @Test
  public void multiplyBy() {

    assertTrue((new Balance(v1)).multiplyBy(v1).equalTo(Long.valueOf(v1) * v1));
    assertTrue((new Balance(v1)).multiplyBy(new Balance(v1)).equalTo(Long.valueOf(v1) * v1));
    assertTrue((new Balance(v1)).multiplyBy(new Balance(v1)).equalTo(new Balance(Long.valueOf(v1) * v1)));

    assertTrue((new Balance(v1)).multiplyBy(bv7).equalTo(bv7.multiply(bv1)));
    assertTrue((new Balance(v2)).multiplyBy(bv6).equalTo(bv2.multiply(bv6)));
    assertTrue((new Balance(v3)).multiplyBy(bv6).equalTo(bv3.multiply(bv6)));
    assertTrue((new Balance(v3)).multiplyBy(bv5).equalTo(bv3.multiply(bv5)));

    Long tl1 = 3037000499L; // Sqrt(Max_Long) rounded down
    Long tl2 = 3037000500L; // Sqrt(Max_Long) rounded up

    assertTrue((new Balance(tl1)).multiplyBy(tl1).equalTo(tl1 * tl1));
    assertFalse((new Balance(tl2)).multiplyBy(tl2).equalTo(tl2 * tl2)); // Long overflow
    assertTrue((new Balance(tl2)).multiplyBy(tl2).equalTo(BigInteger.valueOf(tl2).multiply(BigInteger.valueOf(tl2))));

  }


  @Test
  public void modulus() {

    assertTrue((new Balance(v1)).modulus(v1).equalTo(0L));
    assertTrue((new Balance(v1)).modulus(new Balance(v1)).equalTo(0L));

    assertTrue((new Balance(v1)).modulus(v2).equalTo(v1 % v2));
    assertTrue((new Balance(v2)).modulus(v3).equalTo(v2 % v3));
    assertTrue((new Balance(bv3)).modulus(bv6).equalTo(bv3.mod(bv6.abs())));
    assertTrue((new Balance(bv3)).modulus(bv5).equalTo(bv3.mod(bv5)));

  }


  @Test
  public void subtract() {

    assertTrue((new Balance(v1)).subtract(v1).equalTo(0));
    assertTrue((new Balance(v1)).subtract(new Balance(v1)).equalTo(0));
    assertTrue((new Balance(v1)).subtract(new Balance(v1)).equalTo(new Balance(0)));

    assertTrue((new Balance(v1)).subtract(bv7).equalTo(bv1.subtract(bv7)));
    assertTrue((new Balance(v2)).subtract(bv6).equalTo(bv2.subtract(bv6)));
    assertTrue((new Balance(v3)).subtract(bv6).equalTo(bv3.subtract(bv6)));
    assertTrue((new Balance(v3)).subtract(bv5).equalTo(bv3.subtract(bv5)));

  }
}