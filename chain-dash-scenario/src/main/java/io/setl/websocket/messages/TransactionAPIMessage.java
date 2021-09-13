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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.setl.websocket.messages.serializers.TimestampSerializer;
import java.util.List;

public class TransactionAPIMessage implements SubscriptionMessage {
  private double timestamp;
  private List<Object> transactions;
  private String error;


  /**
   * New instance.
   *
   * @param timestamp    message parameter
   * @param transactions message parameter
   * @param error        message parameter
   */
  public TransactionAPIMessage(double timestamp, List<Object> transactions, String error) {
    this.timestamp = timestamp;
    this.transactions = transactions;
    this.error = error;
  }

  @JsonSerialize(using = TimestampSerializer.class)
  @JsonProperty("Timestamp")
  public double getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(double timestamp) {
    this.timestamp = timestamp;
  }

  @JsonProperty("Transactions")
  public List<Object> getTransactions() {
    return transactions;
  }

  public void setTransactions(List<Object> transactions) {
    this.transactions = transactions;
  }

  @JsonProperty("Error")
  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public void addTransaction(Object... transaction) {
    transactions.add(transaction);
  }
}
