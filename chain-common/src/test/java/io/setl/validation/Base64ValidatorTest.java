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
package io.setl.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Random;

import org.junit.Test;

import io.setl.validation.annotations.ValidBase64;
import io.setl.validation.annotations.ValidBase64.Flavour;

/**
 * @author Simon Greatrix on 14/01/2021.
 */
public class Base64ValidatorTest {


  private Base64Validator get(String methodName) throws NoSuchMethodException {
    Method method = Base64ValidatorTest.class.getMethod(methodName);
    ValidBase64 annotation = method.getAnnotation(ValidBase64.class);
    Base64Validator validator = new Base64Validator();
    validator.initialize(annotation);
    return validator;
  }


  @Test
  @ValidBase64
  public void testDefault() throws NoSuchMethodException {
    Base64Validator validator = get("testDefault");
    testBasic(validator);
  }

  @Test
  @ValidBase64(flavour = Flavour.BASIC)
  public void testBasic() throws NoSuchMethodException {
    testBasic(get("testBasic"));
  }

  private void testBasic(Base64Validator validator) {
    Random random = new Random(0xcafed00d);
    for (int i = 0; i < 100; i++) {
      byte[] testData = new byte[random.nextInt(256)];
      random.nextBytes(testData);
      String b64 = java.util.Base64.getEncoder().encodeToString(testData);
      assertTrue(validator.isValid(b64, null));
    }

    assertTrue(validator.isValid("", null));
    assertTrue(validator.isValid(null, null));

    assertFalse(validator.isValid("01234", null));
    assertTrue(validator.isValid("01234z", null));

    assertFalse(validator.isValid("0", null));
    assertTrue(validator.isValid("01", null));
    assertTrue(validator.isValid("012", null));
    assertTrue(validator.isValid("0123", null));

    assertTrue(validator.isValid("0+", null));
    assertTrue(validator.isValid("0+/", null));
    assertTrue(validator.isValid("0/+3", null));

    assertTrue(validator.isValid("ABCDEF", null));
    assertFalse(validator.isValid("ABCDEF=", null));

    assertTrue(validator.isValid("ABCDEFG=", null));
    assertFalse(validator.isValid("ABCDEFG==", null));

    assertTrue(validator.isValid("ABCDEF==", null));
    assertFalse(validator.isValid("ABCDE==", null));
    assertFalse(validator.isValid("ABCDEF=", null));

    assertFalse(validator.isValid("ABCD=", null));
    assertFalse(validator.isValid("ABCD==", null));

    assertFalse(validator.isValid("ABCDE===", null));

    assertFalse(validator.isValid("ABCD EF==", null));
    assertFalse(validator.isValid("ABCD!EF==", null));

    assertTrue(validator.isValid("ABCD/+==", null));
    assertFalse(validator.isValid("ABCD_-==", null));
  }


  @Test
  @ValidBase64(flavour = Flavour.LENIENT_MIME)
  public void testLenient() throws NoSuchMethodException {
    Base64Validator validator = get("testLenient");
    Random random = new Random(0xcafed00d);
    for (int i = 0; i < 100; i++) {
      byte[] testData = new byte[random.nextInt(256)];
      random.nextBytes(testData);
      String b64 = java.util.Base64.getMimeEncoder().encodeToString(testData);
      assertTrue(validator.isValid(b64, null));
    }

    assertTrue(validator.isValid("", null));
    assertTrue(validator.isValid(null, null));

    assertFalse(validator.isValid("01234", null));
    assertTrue(validator.isValid("01234z", null));

    assertFalse(validator.isValid("0", null));
    assertTrue(validator.isValid("01", null));
    assertTrue(validator.isValid("012", null));
    assertTrue(validator.isValid("0123", null));

    assertTrue(validator.isValid("0/", null));
    assertTrue(validator.isValid("0/+", null));
    assertTrue(validator.isValid("0/+3", null));

    assertTrue(validator.isValid("ABCDEF", null));
    assertFalse(validator.isValid("ABCDEF=", null));
    assertTrue(validator.isValid("ABCDEF==", null));
    assertTrue(validator.isValid(" ABCDEF==", null));
    assertTrue(validator.isValid("ABC DEF==", null));
    assertTrue(validator.isValid("$ABCDEF==", null));
    assertTrue(validator.isValid("ABC#DEF==", null));
    assertTrue(validator.isValid("ABCDEF ==", null));
    assertTrue(validator.isValid("ABCDEF= =", null));
    assertTrue(validator.isValid("ABCDEF== ", null));
    assertTrue(validator.isValid("ABCDEF$==", null));
    assertTrue(validator.isValid("ABCDEF=!=", null));
    assertTrue(validator.isValid("ABCDEF==#", null));

    assertFalse(validator.isValid("ABCDE===", null));

    assertTrue(validator.isValid("ABCD EF==", null));
    assertTrue(validator.isValid("ABCD!EF==", null));

    assertTrue(validator.isValid("ABCD/+==", null));
    assertFalse(validator.isValid("ABCD_-==", null));
  }


  @Test
  @ValidBase64(flavour = Flavour.MIME)
  public void testMime() throws NoSuchMethodException {
    Base64Validator validator = get("testMime");
    Random random = new Random(0xcafed00d);
    for (int i = 0; i < 100; i++) {
      byte[] testData = new byte[random.nextInt(256)];
      random.nextBytes(testData);
      String b64 = java.util.Base64.getMimeEncoder().encodeToString(testData);
      assertTrue(validator.isValid(b64, null));
    }

    assertTrue(validator.isValid("", null));
    assertTrue(validator.isValid(null, null));

    assertFalse(validator.isValid("01234", null));
    assertTrue(validator.isValid("01234z", null));

    assertFalse(validator.isValid("0", null));
    assertTrue(validator.isValid("01", null));
    assertTrue(validator.isValid("012", null));
    assertTrue(validator.isValid("0123", null));

    assertFalse(validator.isValid("=", null));
    assertFalse(validator.isValid("==", null));
    assertFalse(validator.isValid("===", null));
    assertFalse(validator.isValid("====", null));
    assertFalse(validator.isValid(" =", null));
    assertFalse(validator.isValid("= =", null));
    assertFalse(validator.isValid(" ===", null));
    assertFalse(validator.isValid("== ==", null));

    assertTrue(validator.isValid("0/", null));
    assertTrue(validator.isValid("0/+", null));
    assertTrue(validator.isValid("0/+3", null));

    assertTrue(validator.isValid(" ABCDEF", null));
    assertTrue(validator.isValid("ABC  DEF ", null));
    assertTrue(validator.isValid(" ABCDEF==", null));
    assertTrue(validator.isValid("ABC DEF==", null));
    assertTrue(validator.isValid("ABCDEF ==", null));
    assertTrue(validator.isValid("ABCDEF= =", null));
    assertTrue(validator.isValid("ABCDEF== ", null));

    assertTrue(validator.isValid("ABCDEF", null));
    assertFalse(validator.isValid("ABCDEF=", null));

    assertTrue(validator.isValid("ABCDEFG=", null));
    assertFalse(validator.isValid("ABCDEFG==", null));

    assertTrue(validator.isValid("ABCDEF==", null));
    assertFalse(validator.isValid("ABCDE==", null));
    assertFalse(validator.isValid("ABCDEF=", null));

    assertFalse(validator.isValid("ABCD=", null));
    assertFalse(validator.isValid("ABCD==", null));

    assertFalse(validator.isValid("ABCDE===", null));

    assertFalse(validator.isValid("ABCDE===", null));

    assertTrue(validator.isValid("ABCD EF==", null));
    assertFalse(validator.isValid("ABCD!EF==", null));

    assertTrue(validator.isValid("ABCD/+==", null));
    assertFalse(validator.isValid("ABCD_-==", null));
  }


  @Test
  @ValidBase64(flavour = Flavour.URL)
  public void testUrl() throws NoSuchMethodException {
    Base64Validator validator = get("testUrl");
    Random random = new Random(0xcafed00d);
    for (int i = 0; i < 100; i++) {
      byte[] testData = new byte[random.nextInt(256)];
      random.nextBytes(testData);
      String b64 = java.util.Base64.getUrlEncoder().encodeToString(testData);
      assertTrue(validator.isValid(b64, null));
    }

    assertTrue(validator.isValid("", null));
    assertTrue(validator.isValid(null, null));

    assertFalse(validator.isValid("01234", null));
    assertTrue(validator.isValid("01234z", null));

    assertFalse(validator.isValid("0", null));
    assertTrue(validator.isValid("01", null));
    assertTrue(validator.isValid("012", null));
    assertTrue(validator.isValid("0123", null));

    assertTrue(validator.isValid("0_", null));
    assertTrue(validator.isValid("0-_", null));
    assertTrue(validator.isValid("0_-3", null));

    assertTrue(validator.isValid("ABCDEF", null));
    assertFalse(validator.isValid("ABCDEF=", null));
    assertTrue(validator.isValid("ABCDEF==", null));
    assertFalse(validator.isValid("ABCDE===", null));

    assertFalse(validator.isValid("ABCD EF==", null));
    assertFalse(validator.isValid("ABCD!EF==", null));

    assertTrue(validator.isValid("ABCD-_==", null));
    assertFalse(validator.isValid("ABCD/+==", null));
  }

}