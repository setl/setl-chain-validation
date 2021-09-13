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
package io.setl.bc.json.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * @author Simon Greatrix on 27/02/2020.
 */
public class ValidationFailureTest {

  @Test
  public void getColumn() {
    ValidationFailure failure = new ValidationFailure();
    assertEquals(-1, failure.getColumn());
    failure.setColumn(10);
    assertEquals(10, failure.getColumn());

    failure = new ValidationFailure("Message", 5, 50, 500);
    assertEquals(50, failure.getColumn());
  }


  @Test
  public void getLine() {
    ValidationFailure failure = new ValidationFailure();
    assertEquals(-1, failure.getLine());
    failure.setLine(10);
    assertEquals(10, failure.getLine());

    failure = new ValidationFailure("Message", 5, 50, 500);
    assertEquals(5, failure.getLine());
  }


  @Test
  public void getMessage() {
    ValidationFailure failure = new ValidationFailure();
    assertEquals("", failure.getMessage());
    failure.setMessage("Reason");
    assertEquals("Reason", failure.getMessage());

    failure = new ValidationFailure("Message", 5, 50, 500);
    assertEquals("Message", failure.getMessage());
  }


  @Test
  public void getStreamOffset() {
    ValidationFailure failure = new ValidationFailure();
    assertEquals(-1, failure.getStreamOffset());
    failure.setStreamOffset(10);
    assertEquals(10, failure.getStreamOffset());

    failure = new ValidationFailure("Message", 5, 50, 500);
    assertEquals(500, failure.getStreamOffset());
  }


  @Test
  public void testToString() {
    ValidationFailure failure = new ValidationFailure();
    assertNotNull(failure.toString());
  }

}