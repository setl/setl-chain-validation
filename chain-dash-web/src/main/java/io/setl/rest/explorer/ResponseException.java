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
package io.setl.rest.explorer;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * @author Simon Greatrix on 2019-03-04.
 */
public class ResponseException extends Exception {

  final ResponseEntity<String> responseEntity;


  /**
   * Create a response exception.
   *
   * @param status  the desired failure code
   * @param message the associated message
   */
  public ResponseException(HttpStatus status, String message) {
    super(message);
    responseEntity = ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN).body(message);
  }


  /**
   * Create a response exception from an internal Exception.
   *
   * @param internalError the internal Exception
   */
  public ResponseException(Exception internalError) {
    super(internalError);

    // In contravention of good security, I am outputting the error message. If this is ever moved to production code, this should be removed.
    StringWriter writer = new StringWriter();
    PrintWriter printWriter = new PrintWriter(writer);
    printWriter.println(internalError.getMessage());
    internalError.printStackTrace(printWriter);
    printWriter.flush();

    responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN).body(writer.toString());
  }


  public ResponseEntity<String> getResponse() {
    return responseEntity;
  }
}
