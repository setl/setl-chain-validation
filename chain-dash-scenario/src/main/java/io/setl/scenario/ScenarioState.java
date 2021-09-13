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

import io.setl.utils.TimeUtil;
import io.setl.websocket.handlers.Scenario;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScenarioState {

  private static final int MAXOVERDRIVE = 90;

  private static final Logger logger = LoggerFactory.getLogger(ScenarioState.class);

  private final String[] types;

  protected Scenario scenario;

  private List<String> goodAssets = new ArrayList<>();

  private boolean isRunning;

  private int startTime;

  private double tps;

  private int txCount = 0;


  /**
   * ScenarioState Constructor.
   *
   * @param scenario         :
   * @param tps              :
   * @param types            :
   * @param usedFromTrim     :
   * @param blockPercentage  :
   * @param xChainPercentage :
   */
  public ScenarioState(Scenario scenario, double tps, String[] types, int usedFromTrim, double blockPercentage, double xChainPercentage) {

    this.scenario = scenario;
    this.tps = tps;
    this.types = types != null ? types.clone() : null;
  }


  public void addGoodAsset(String nsClass) {

    goodAssets.add(nsClass);
  }


  public void addProcessedTransactions(int postedTXCount) {

    txCount += postedTXCount;
  }


  int computeRequiredTransactions(double overdrive) {

    int timeNow = TimeUtil.unixTime();
    double targetTx = (timeNow - startTime) * tps * overdrive;
    int txToDo = (int) Math.min(tps * MAXOVERDRIVE, targetTx - txCount);
    if (txToDo <= 0) {
      return 0;
    }

    int adjustedToDo = txToDo;
    logger.info("ScenarioTX {} tps {}, runtime {}, targettx- {}, txToDo {}", scenario.name(), tps, timeNow - startTime, targetTx - txCount, txToDo);
    return adjustedToDo;
  }


  public List<String> getGoodAssets() {

    return goodAssets;
  }


  public int getID() {

    return scenario.getId();
  }


  public String getName() {

    return scenario.name();
  }


  /**
   * handlesType.
   *
   * @param typ :
   *
   * @return :
   */
  public boolean handlesType(String typ) {

    for (String t : types) {
      if (t.equals(typ)) {
        return true;
      }
    }
    return false;
  }


  public boolean isRunning() {

    return isRunning;
  }


  /**
   * setRunning.
   *
   * @param runningState :
   */
  public void setRunning(boolean runningState) {

    isRunning = runningState;
    if (isRunning) {
      startTime = TimeUtil.unixTime();
      txCount = 0;
    }

  }
}
