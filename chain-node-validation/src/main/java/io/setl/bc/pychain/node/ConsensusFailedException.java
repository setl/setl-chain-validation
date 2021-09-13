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
package io.setl.bc.pychain.node;

/**
 * A runtime exception used to terminate a consensus process after some exception has arisen.
 *
 * @author Simon Greatrix on 2019-05-06.
 */
public class ConsensusFailedException extends RuntimeException {

  public ConsensusFailedException(Exception cause) {
    super(cause);
  }


  public ConsensusFailedException(String message) {
    super(message);
  }


  public ConsensusFailedException(String message, Exception cause) {
    super(message, cause);
  }

}
