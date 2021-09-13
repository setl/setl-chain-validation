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

import static io.setl.common.StringUtils.notNull;

import javax.json.stream.JsonLocation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Simon Greatrix on 15/02/2020.
 */
@Schema(description = "A description of a document validation problem.")
public class ValidationFailure {

  @Schema(description = "The column in the input where the problem was observed. If not known, will be '-1'.")
  private long column = -1;

  @Schema(description = "The line in the input where the problem was observed. If not known, will be '-1'.")
  private long line = -1;

  @Schema(description = "The description of the problem.")
  private String message = "";

  @Schema(description = "The byte or character location in the input where the problem was observed. If not known, will be '-1'.")
  private long streamOffset = -1;


  public ValidationFailure() {

  }


  /**
   * New instance specifying all properties of the message.
   *
   * @param message      the message text
   * @param line         the line where the validation failure occurred
   * @param column       the text column where the validation failure occurred
   * @param streamOffset the byte or character location where the validation failure occurred
   */
  public ValidationFailure(String message, long line, long column, long streamOffset) {
    this.column = column;
    this.message = notNull(message);
    this.line = line;
    this.streamOffset = streamOffset;
  }


  /**
   * New instance from a JSON location.
   *
   * @param message  the message text
   * @param location the location
   */
  public ValidationFailure(String message, JsonLocation location) {
    this.column = location.getColumnNumber();
    this.message = notNull(message);
    this.line = location.getLineNumber();
    this.streamOffset = location.getStreamOffset();
  }


  public long getColumn() {
    return column;
  }


  public long getLine() {
    return line;
  }


  public String getMessage() {
    return message;
  }


  public long getStreamOffset() {
    return streamOffset;
  }


  public void setColumn(long column) {
    this.column = column;
  }


  public void setLine(long line) {
    this.line = line;
  }


  public void setMessage(String message) {
    this.message = notNull(message);
  }


  public void setStreamOffset(long streamOffset) {
    this.streamOffset = streamOffset;
  }


  @Override
  public String toString() {
    return String.format("[%d, %d, %d]: %s", line, column, streamOffset, message);
  }

}
