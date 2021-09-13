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
package io.setl.util;

import static io.setl.bc.logging.LoggingConstants.MARKER_PERFORMANCE;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.performance.PerformanceEventHandler;
import io.setl.bc.performance.PerformanceEventListener;

/**
 * Specialisation of the PriorityExecutor used for performance tuning.
 *
 * @author Valerio Trigari, SETL Ltd, 2020
 */
public class PerformanceDrivenPriorityExecutor extends PriorityExecutor implements PerformanceEventHandler {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceDrivenPriorityExecutor.class);

  private final List<PerformanceEventListener> executorListeners = new CopyOnWriteArrayList<>();

  private int minWorkQueueSize;

  private int maxWorkQueueSize;

  private boolean notifyAbove;

  private boolean notifyBelow;

  private final Object txFlowLock = new Object();


  /**
   * Constructor.
   *
   * @param minThreadPoolSize the number of threads to keep in the pool
   * @param maxThreadPoolSize the maximum number of threads allowed in the pool
   * @param minWorkQueueSize  the minimum number of jobs in the Work queue, below which we resume adding them
   * @param maxWorkQueueSize  the maximum number of jobs in the Work queue, above which we pause adding them
   */
  public PerformanceDrivenPriorityExecutor(int minThreadPoolSize, int maxThreadPoolSize, int minWorkQueueSize, int maxWorkQueueSize) {
    super(minThreadPoolSize, maxThreadPoolSize);
    this.minWorkQueueSize = minWorkQueueSize;
    this.maxWorkQueueSize = maxWorkQueueSize;
  }


  @Override
  public void submit(int priority, Consumer<TaskContext> job) {
    super.submit(priority, job);
    notifyListeners();
  }


  @Override
  public void submit(int priority, Runnable job) {
    super.submit(priority, job);
    notifyListeners();
  }


  @Override
  public void addListener(PerformanceEventListener listener) {
    executorListeners.add(listener);
  }


  @Override
  public void notifyListeners() {
    int queueSize = executor.getQueue().size();

    if (queueSize < minWorkQueueSize && !notifyBelow) {
      if (logger.isWarnEnabled(MARKER_PERFORMANCE)) {
        logger.warn(MARKER_PERFORMANCE, "Priority Executor Worker Queue size is below minimum level: {}", queueSize);
      }

      synchronized (txFlowLock) {
        notifyBelow = true;
        notifyAbove = false;
        executorListeners.forEach(PerformanceEventListener::belowMinimumEvent);
      }
    }

    if (queueSize > maxWorkQueueSize && !notifyAbove) {
      if (logger.isWarnEnabled(MARKER_PERFORMANCE)) {
        logger.warn(MARKER_PERFORMANCE, "Priority Executor Worker Queue size is above maximum level: {}", queueSize);
      }

      synchronized (txFlowLock) {
        notifyBelow = false;
        notifyAbove = true;
        executorListeners.forEach(PerformanceEventListener::aboveMaximumEvent);
      }
    }
  }


  public void setMinWorkQueueSize(int minWorkQueueSize) {
    this.minWorkQueueSize = minWorkQueueSize;
  }


  public void setMaxWorkQueueSize(int maxWorkQueueSize) {
    this.maxWorkQueueSize = maxWorkQueueSize;
  }

}
