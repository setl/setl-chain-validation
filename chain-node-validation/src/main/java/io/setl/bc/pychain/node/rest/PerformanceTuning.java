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
package io.setl.bc.pychain.node.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import io.setl.bc.pychain.HashedMap;
import io.setl.bc.pychain.node.txpool.LargeTransactionPool;
import io.setl.bc.pychain.peer.NettyPeerManager;
import io.setl.util.PerformanceDrivenPriorityExecutor;

@Component
@ConditionalOnProperty(value = "transport", havingValue = "netty")
@RestControllerEndpoint(id = "performance")
public class PerformanceTuning {

  private final NettyPeerManager peerManager;

  private final LargeTransactionPool txPool;

  private final PerformanceDrivenPriorityExecutor priorityExecutor;


  /**
   * Constructor.
   *
   * @param peerManager      sets the tx package limit
   * @param txPool           sets the tx pool level min and max values
   * @param priorityExecutor sets the executor work queue size min and max values
   */
  public PerformanceTuning(NettyPeerManager peerManager, LargeTransactionPool txPool, PerformanceDrivenPriorityExecutor priorityExecutor) {
    this.peerManager = peerManager;
    this.txPool = txPool;
    this.priorityExecutor = priorityExecutor;
  }


  /**
   * Performance tuning POST request.
   *
   * @param request contains the values for the tuning parameters
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ResponseEntity<String> performance(@RequestBody Map<TuningParameter, Integer> request) {
    List<String> invalidParameters = checkRequestValues(request);

    if (!invalidParameters.isEmpty()) {
      return ResponseEntity.badRequest().body(invalidParameters.toString());
    }

    setRequestValues(request);
    return ResponseEntity.ok("All tuning parameters have been set.");
  }


  private List<String> checkRequestValues(Map<TuningParameter, Integer> request) {
    List<String> invalidParameters = new ArrayList<>();

    for (Entry<TuningParameter, Integer> entry : request.entrySet()) {
      TuningParameter parameter = entry.getKey();
      int value = entry.getValue();

      if (!parameter.check(value)) {
        invalidParameters.add(String.format("'%s' is not within its limits of %d and %d", parameter.label, parameter.min, parameter.max));
      }
    }

    return invalidParameters;
  }


  private void setRequestValues(Map<TuningParameter, Integer> request) {
    for (Entry<TuningParameter, Integer> entry : request.entrySet()) {
      TuningParameter parameter = entry.getKey();
      int value = entry.getValue();

      switch (parameter) {
        case TX_PACKAGE_LIMIT:
          peerManager.setTxPackageLimit(value);
          break;
        case TX_POOL_LEVEL_MIN:
          txPool.setMinLevel(value);
          break;
        case TX_POOL_LEVEL_MAX:
          txPool.setMaxLevel(value);
          break;
        case WORK_QUEUE_SIZE_MIN:
          priorityExecutor.setMinWorkQueueSize(value);
          break;
        case WORK_QUEUE_SIZE_MAX:
          priorityExecutor.setMaxWorkQueueSize(value);
          break;
        default:
          break;
      }
    }
  }


  @ExceptionHandler(Exception.class)
  private ResponseEntity<String> handleException(Exception ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }


  public enum TuningParameter {

    TX_PACKAGE_LIMIT("txPackageLimit", 1, 10_000),
    TX_POOL_LEVEL_MIN("txPoolMinLevel", 100, 10_000),
    TX_POOL_LEVEL_MAX("txPoolMaxLevel", 1000, 100_000),
    WORK_QUEUE_SIZE_MIN("workQueueSizeMin", 4, 512),
    WORK_QUEUE_SIZE_MAX("workQueueSizeMax", 16, 1024);

    private final String label;

    private final int min;

    private final int max;

    private static final Map<String, TuningParameter> LUT = new HashedMap<>();

    static {
      for (TuningParameter tuningParameter : TuningParameter.values()) {
        LUT.put(tuningParameter.label, tuningParameter);
      }
    }


    TuningParameter(String label, int min, int max) {
      this.label = label;
      this.min = min;
      this.max = max;
    }


    /**
     * Check that the value provided for the tuning parameter is within the established limits: min <= value <= max.
     *
     * @param value the assigned value for the tuning parameter
     *
     * @return {@code true} if the value is within the limits, otherwise {@code false}
     */
    public boolean check(int value) {
      return value >= min && value <= max;
    }


    /**
     * Get a tuning parameter based on the label.
     *
     * @param label associated with the tuning parameter
     *
     * @return the corresponding TuningParameter
     *
     * @throws IllegalArgumentException if the label is not valid
     */
    @JsonCreator
    public static TuningParameter getParameterByLabel(String label) {
      if (LUT.containsKey(label)) {
        return LUT.get(label);
      }

      throw new IllegalArgumentException(String.format("Tuning parameter '%s' is not valid", label));
    }

  }

}
