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
package io.setl.scenario;

import io.setl.websocket.handlers.Scenario;
import org.springframework.stereotype.Component;

@Component
public class ScenarioStates {
  
  private static final String GOV_MONEY_UK = "Govt_Money_UK";
  private static final String GOVT_MONEY = "Govt_Money";
  private static final String BANK_MONEY = "Bank_Money";
  private static final String EQUITY = "Equity";
  private static final String TREASURY = "Treasury";
  
  private final ScenarioState[] scenarios;
  
  private int overdriveState = 0;
  private double overdriveFactor = 1.0;
  
  /**
   * ScenarioStates constructor.
   */
  public ScenarioStates() {
    
    scenarios = new ScenarioState[] {
        new ScenarioState(Scenario.CHAPS, 1.7, new String[] {GOV_MONEY_UK}, 10, 0.02, 0.2),
        new ScenarioState(Scenario.CREST, 2.2, new String[] {GOV_MONEY_UK, GOVT_MONEY, BANK_MONEY, EQUITY, TREASURY}, 10, 0.02, 0.2),
        new ScenarioState(Scenario.CLS, 10, new String[] {GOV_MONEY_UK, GOVT_MONEY}, 10, 0.02, 0.2),
        new ScenarioState(Scenario.FPS, 50, new String[] {GOV_MONEY_UK, GOVT_MONEY, BANK_MONEY}, 10, 0.02, 0.2),
        new ScenarioState(Scenario.BACS, 266, new String[] {GOV_MONEY_UK, GOVT_MONEY, BANK_MONEY}, 10, 0.02, 0.2),
        new ScenarioState(Scenario.LINK, 66.7, new String[] {GOV_MONEY_UK, GOVT_MONEY, BANK_MONEY}, 10, 0.02, 0.2)
    };
  }
  
  public double getEffectiveOverdrive() {
    
    return overdriveState == 0 ? 1.0 : overdriveFactor;
  }
  
  
  public ScenarioState[] getScenarios() {

    return scenarios.clone();
  }
  
  public ScenarioState[] getStates() {
    
    return scenarios.clone();
  }
  
  /**
   * setRuningState.
   *
   * @param scenario     :
   * @param runningState :
   */
  public void setRuningState(Scenario scenario, boolean runningState) {
    
    for (ScenarioState scenarioState : scenarios) {
      if (scenario == scenarioState.scenario) {
        scenarioState.setRunning(runningState);
      }
    }
  }
  
  /**
   * setOverdrive.
   *
   * @param overdrive :
   * @param newState  :
   */
  public void setOverdrive(double overdrive, int newState) {
    
    this.overdriveState = newState;
    this.overdriveFactor = overdrive;
    for (ScenarioState scenarioState : scenarios) {
      scenarioState.setRunning(scenarioState.isRunning());
    }
    
  }
  
  public int getOverdriveState() {
    
    return this.overdriveState;
  }
  
}
