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
package io.setl.bc.pychain.serialise;

import java.io.IOException;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class ContentExceptionTest {

  @Test(expected = ContentException.class)
  public void test1() {
    throw new ContentException();
  }


  @Test(expected = ContentException.class)
  public void test2() {
    throw new ContentException("A message");
  }


  @Test(expected = ContentException.class)
  public void test3() {
    throw new ContentException("A message", new IOException());
  }


  @Test(expected = ContentException.class)
  public void test4() {
    throw new ContentException(new IOException());
  }


  @Test(expected = ContentException.class)
  public void test5() {
    throw new ContentException("A message", new IOException(), true, true);
  }
}