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
package io.setl.websocket.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.setl.websocket.messages.serializers.RequestSerializer;

public class APITextMessage extends SerializableTextMessage {
  private Object request;
  private String hostname;
  private SubscriptionMessage data;
  private String requestID;
  private String messageType;



  /**
   * New instance.
   *
   * @param request     message parameter
   * @param hostname    message parameter
   * @param data        message parameter
   * @param requestID   message parameter
   * @param messageType message parameter
   */
  public APITextMessage(Object request, String hostname, SubscriptionMessage data, String requestID, String messageType) {
    super();
    this.request = request;
    this.hostname = hostname;
    this.data = data;
    this.requestID = requestID;
    this.messageType = messageType;
  }


  public APITextMessage(SubscriptionMessage data, String requestID, String messageType) {
    this(new Object(), null, data, requestID, messageType);
  }

  @JsonProperty("Request")
  @JsonSerialize(using = RequestSerializer.class)
  public Object getRequest() {
    return request;
  }


  @JsonProperty("Hostname")
  @JsonInclude(Include.NON_NULL)
  public String getHostname() {
    return hostname;
  }

  @JsonProperty("Data")
  public SubscriptionMessage getData() {
    return data;
  }

  @JsonProperty("RequestID")
  public String getRequestID() {
    return requestID;
  }

  @JsonProperty("MessageType")
  public String getMessageType() {
    return messageType;
  }
}
