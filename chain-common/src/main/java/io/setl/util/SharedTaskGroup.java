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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A collection of shared tasks. A shared task is similar to a Future, but the waiting thread may help the task, or another task in the same group complete.
 *
 * @author Simon Greatrix on 2019-05-31.
 */
public class SharedTaskGroup implements Runnable {

  public static class SharedTask<T> implements Runnable {

    /** Synchronization lock. */
    private final Object myLock = new Object();

    /** This task's task. */
    private final Callable<T> myTask;

    /** Reason for failure, if any. */
    private Exception failure = null;

    /** Result of task. */
    private T result = null;

    /** Current task state. */
    private int state = 0;


    SharedTask(Callable<T> myTask) {
      this.myTask = myTask;
    }


    /**
     * Check if the task failed and get its result. If the task is awaiting execution, this thread will execute it. If the task is being executed by
     * another thread, this thread will execute a different task whilst it waits.
     *
     * @return the result if the task succeeded
     * @throws Exception if the task failed
     */
    public T checkAndGet() throws Exception {
      while (true) {
        synchronized (myLock) {
          switch (state) {
            case 0:
              // Process this task.
              run();
              break;
            case 1:
              // We will be notified when the task completes.
              myLock.wait();
              break;
            case 2:
              // Task is finished. Did it fail?
              if (failure != null) {
                throw failure;
              }
              // Tasks is finished and was successful
              return result;
            default:
              throw new IllegalStateException("Task state should not be " + state);
          }
        }
      }
    }


    /**
     * Get the result of this task, waiting if necessary. If the task is awaiting execution, this thread will execute it. If the task is being executed by
     * another thread, this thread will execute a different task whilst it waits.
     *
     * @return the result.
     */
    public T get() {
      try {
        return checkAndGet();
      } catch (Exception e) {
        throw new UndeclaredThrowableException(e);
      }
    }


    @Override
    public void run() {
      synchronized (myLock) {
        // If already in progress or finished, then stop.
        if (state != 0) {
          return;
        }
        // Mark this task as in progress
        state = 1;

        try {
          // run the task
          result = myTask.call();
        } catch (Exception e) {
          failure = e;
        } finally {
          // Mark this task as done
          state = 2;
          myLock.notifyAll();
        }
      }
    }
  }



  /** Synchronization lock. */
  final Object taskLock = new Object();

  /** Executor for asynchronous processing. */
  private final Executor executor;

  /** The maximum number of executors to have at any one time. */
  private final int maxThreads;

  /** Awaiting tasks. */
  private final LinkedList<SharedTask<?>> tasks = new LinkedList<>();

  /** The current number of running executors. */
  private int runningThreads = 0;


  /**
   * New instance.
   *
   * @param maxThreads maximum number of executors to have concurrently.
   */
  public SharedTaskGroup(int maxThreads) {
    this(Executors.newFixedThreadPool(maxThreads), maxThreads);
  }


  /**
   * New instance.
   *
   * @param executor executor for asynchronous operation
   */
  public SharedTaskGroup(Executor executor) {
    this(executor, Runtime.getRuntime().availableProcessors());
  }


  /**
   * New instance.
   *
   * @param executor   executor for asynchronous operation
   * @param maxThreads maximum number of executors to have concurrently.
   */
  public SharedTaskGroup(Executor executor, int maxThreads) {
    this.executor = executor;
    this.maxThreads = maxThreads;
  }


  @Override
  public void run() {
    try {
      while (true) {
        SharedTask<?> nextTask;
        synchronized (taskLock) {
          if (tasks.isEmpty()) {
            return;
          }
          nextTask = tasks.removeFirst();
        }

        nextTask.run();
      }
    } finally {
      synchronized (taskLock) {
        runningThreads--;
      }
    }
  }


  /**
   * Submit a task to this task group.
   *
   * @param callable the task
   * @param <T>      the output of the task
   *
   * @return the shared task
   */
  public <T> SharedTask<T> submit(Callable<T> callable) {
    SharedTask<T> task = new SharedTask<>(callable);
    synchronized (taskLock) {
      tasks.addLast(task);
      taskLock.notifyAll();

      if (runningThreads < maxThreads) {
        runningThreads++;
        executor.execute(this);
      }
    }
    return task;
  }
}
