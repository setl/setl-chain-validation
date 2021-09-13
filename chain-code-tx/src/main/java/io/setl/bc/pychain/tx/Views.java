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
package io.setl.bc.pychain.tx;

/**
 * Views upon the JSON transactions.
 *
 * @author Simon Greatrix on 01/02/2021.
 */
public interface Views {

  /**
   * Fields that are specified when a transaction is submitted to the block chain, but are not part of the output.
   */
  interface Input {
    // Marker interface
  }



  /**
   * Fields that are part of the output from the block chain after consensus processing, but are never part of the input.
   */
  interface Output {
    // Marker interface
  }



  /**
   * A field that is only input to the submission node.
   */
  interface Submission extends Input {
    // Marker interface
  }

}
