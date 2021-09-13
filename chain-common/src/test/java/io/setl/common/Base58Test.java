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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class Base58Test {

  @Test
  public void encode() {

    String string1 = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Nulla est. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit"
        + " aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Quisque tincidunt scelerisque libero. Sed elit dui, "
        + "pellentesque a, faucibus vel, interdum nec, diam. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequu"
        + "ntur magni dolores eos qui ratione voluptatem sequi nesciunt. Aliquam id dolor. Phasellus enim erat, vestibulum vel, aliquam a, posuere eu, vel"
        + "it. Mauris dolor felis, sagittis at, luctus sed, aliquam non, tellus. Fusce tellus. In sem justo, commodo ut, suscipit at, pharetra vitae, orci"
        + ". Fusce dui leo, imperdiet in, aliquam sit amet, feugiat eu, orci. Proin pede metus, vulputate nec, fermentum fringilla, vehicula vitae, justo."
        + " In sem justo, commodo ut, suscipit at, pharetra vitae, orci. Proin pede metus, vulputate nec, fermentum fringilla, vehicula vitae, justo. In s"
        + "em justo, commodo ut, suscipit at, pharetra vitae, orci. Sed vel lectus. Donec odio tempus molestie, porttitor ut, iaculis quis, sem.";

    String enc1 = Base58.encode(string1.getBytes());

    assertTrue(Arrays.equals(Base58.decode(enc1), string1.getBytes()));


  }

}