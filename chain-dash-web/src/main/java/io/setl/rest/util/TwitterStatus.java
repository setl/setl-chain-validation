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
package io.setl.rest.util;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.text.SimpleDateFormat;
import twitter4j.Status;
import twitter4j.User;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class TwitterStatus {

  private final SimpleDateFormat df;

  private final Status status;

  public TwitterStatus(Status status, SimpleDateFormat df) {
    this.status = status;
    this.df = df;
  }

  public String getCreatedAt() {
    return df.format(status.getCreatedAt());
  }

  public User getUser() {
    return status.getUser();
  }

  public String getText() {
    return status.getText();
  }

}
