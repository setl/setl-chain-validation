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
import java.util.List;
import java.util.Map;

public class ServerStatusAPIMessage implements SubscriptionMessage {
  private Boolean txLogBlock;
  private Map<String, Integer> subscriptions;
  private int logLevel;
  private List<Object> scenarios;
  private Boolean txLogScenario;
  private int overdrive;
  private int logAreas;


  /**
   * New instance.
   *
   * @param txLogBlock    message parameter
   * @param subscriptions message parameter
   * @param logLevel      message parameter
   * @param scenarios     message parameter
   * @param txLogScenario message parameter
   * @param overdrive     message parameter
   * @param logAreas      message parameter
   */
  public ServerStatusAPIMessage(Boolean txLogBlock, Map<String, Integer> subscriptions, int logLevel, List<Object> scenarios,
      Boolean txLogScenario, int overdrive, int logAreas
  ) {
    this.txLogBlock = txLogBlock;
    this.subscriptions = subscriptions;
    this.logLevel = logLevel;
    this.scenarios = scenarios;
    this.txLogScenario = txLogScenario;
    this.overdrive = overdrive;
    this.logAreas = logAreas;
  }

  @JsonProperty("txLogBlock")
  public Boolean getTxLogBlock() {
    return txLogBlock;
  }

  public void setTxLogBlock(Boolean txLogBlock) {
    this.txLogBlock = txLogBlock;
  }

  @JsonProperty("Subscriptions")
  public Map<String, Integer> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(Map<String, Integer> subscriptions) {
    this.subscriptions = subscriptions;
  }


  @JsonProperty("LogLevel")
  public int getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(int logLevel) {
    this.logLevel = logLevel;
  }


  @JsonProperty("Scenarios")
  public List<Object> getScenarios() {
    return scenarios;
  }

  public void setScenarios(List<Object> scenarios) {
    this.scenarios = scenarios;
  }

  @JsonProperty("txLogScenario")
  public Boolean getTxLogScenario() {
    return txLogScenario;
  }

  public void setTxLogScenario(Boolean txLogScenario) {
    this.txLogScenario = txLogScenario;
  }

  @JsonProperty("Overdrive")
  public int getOverdrive() {
    return overdrive;
  }

  public void setOverdrive(int overdrive) {
    this.overdrive = overdrive;
  }

  @JsonProperty("LogAreas")
  public int getLogAreas() {
    return logAreas;
  }

  public void setLogAreas(int logAreas) {
    this.logAreas = logAreas;
  }

  public void addScenario(Object... scenarioObj) {
    scenarios.add(scenarioObj);
  }
}
