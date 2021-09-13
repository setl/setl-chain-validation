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
package io.setl.bc.pychain.exception;

/**
 * A transaction request was invalid.
 *
 * @author Simon Greatrix on 27/09/2017.
 */
public class InvalidTransactionException extends Exception {

  private static final long serialVersionUID = -4953709502336366600L;


  public InvalidTransactionException(String message) {
    super(message);
  }


  public InvalidTransactionException(String message, Exception cause) {
    super(message, cause);
  }


  public InvalidTransactionException(Exception e) {
    super("Internal error", e);
  }
}
