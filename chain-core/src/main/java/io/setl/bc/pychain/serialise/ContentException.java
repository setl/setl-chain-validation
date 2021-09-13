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

/**
 * An exception thrown when conversion between content and the object instance fails.
 *
 * @author Simon Greatrix on 2019-05-31.
 */
public class ContentException extends RuntimeException {

  public ContentException() {
  }


  public ContentException(String message) {
    super(message);
  }


  public ContentException(String message, Throwable cause) {
    super(message, cause);
  }


  public ContentException(Throwable cause) {
    super(cause);
  }


  public ContentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
