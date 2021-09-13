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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An executor which will allows prioritisation of tasks.
 *
 * @author Simon Greatrix on 2019-03-18.
 */
public class PriorityExecutor implements Executor {

  /** A shared global instance of a priority executor. */
  public static final PriorityExecutor INSTANCE = new PriorityExecutor(0);

  private static final Map<Integer, String> NAMED_PRIORITIES;

  private static final Logger logger = LoggerFactory.getLogger(PriorityExecutor.class);



  /**
   * A counter for logging. Records the number of tasks running and queued for a priority level.
   */
  static class LogCounter {

    final AtomicInteger queued = new AtomicInteger(0);

    final AtomicInteger running = new AtomicInteger(0);


    public String toString() {
      return "[ R:" + running.get() + ", Q:" + queued.get() + " ]";
    }

  }



  static class TaskRunner extends Thread {

    private TaskContext taskContext;


    TaskRunner(Runnable r) {
      super(r, "SETL-Prioritised-Thread");
    }


    TaskContext getTaskContext() {
      return taskContext;
    }


    void setTaskContext(TaskContext taskContext) {
      this.taskContext = taskContext;
    }

  }



  static {
    Class<Priorities> prioritiesClass = Priorities.class;
    Field[] fields = prioritiesClass.getFields();
    HashMap<Integer, String> map = new HashMap<>();
    for (Field f : fields) {
      if (Modifier.isPublic(f.getModifiers()) && Modifier.isStatic(f.getModifiers()) && f.getType().equals(Integer.TYPE)) {
        try {
          map.put(f.getInt(null), f.getName());
        } catch (IllegalAccessException e) {
          logger.info("Unable to access named priority {}", f);
        }
      }
    }
    NAMED_PRIORITIES = Collections.unmodifiableMap(map);
  }



  class Task implements Comparable<Task>, Runnable {

    private final Runnable job;

    private final TaskContext taskContext;


    Task(long id, int priority, Runnable job) {
      taskContext = new TaskContext(priority, id);
      this.job = job;
    }


    Task(TaskContext context, Runnable job) {
      this.taskContext = context;
      this.job = job;
    }


    @Override
    public int compareTo(Task o) {
      return taskContext.compareTo(o.taskContext);
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Task)) {
        return false;
      }

      Task task = (Task) o;
      return taskContext.equals(task.taskContext);
    }


    @Override
    public int hashCode() {
      return taskContext.hashCode();
    }


    public void run() {
      TaskRunner me = (TaskRunner) Thread.currentThread();
      me.setTaskContext(taskContext);

      int priority = taskContext.priority;
      if (priority >= Priorities.ELEVATED_PRIORITY_MARK) {
        me.setPriority(Priorities.THREAD_ELEVATED_PRIORITY);
        logger.trace("Starting elevated priority job {}", taskContext);
      } else if (priority < 0) {
        me.setPriority(Priorities.THREAD_REDUCED_PRIORITY);
        logger.trace("Starting reduced priority job {}", taskContext);
      } else {
        logger.trace("Starting normal priority job {}", taskContext);
      }
      me.setName("SETL-prioritised-thread-" + me.getId() + "@" + taskContext.getPriorityName());

      if (logger.isTraceEnabled()) {
        LogCounter lc = countersForLogging.get(priority);
        lc.queued.decrementAndGet();
        lc.running.incrementAndGet();
        logger.trace("Queue status: {}", countersForLogging);
      }

      try {
        job.run();
      } catch (RuntimeException e) {
        logger.error("Job {} failed", taskContext, e);
      } finally {
        // Always reset thread priority
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        me.setTaskContext(null);
        logger.trace("Completed job {}", taskContext);

        if (logger.isTraceEnabled()) {
          LogCounter lc = countersForLogging.get(priority);
          lc.running.decrementAndGet();
          logger.trace("Queue status: {}", countersForLogging);
        }
      }
      me.setName("SETL-prioritised-thread-" + me.getId());
    }

  }



  public class TaskContext implements Comparable<TaskContext> {

    private final int priority;

    private final long subId;

    private final AtomicLong subIdSrc;

    private final long taskId;


    TaskContext(int priority, long taskId) {
      this.priority = priority;
      this.taskId = taskId;
      subIdSrc = new AtomicLong(0);
      this.subId = subIdSrc.getAndIncrement();
    }


    TaskContext(TaskContext original) {
      this.priority = original.priority;
      this.taskId = original.taskId;
      this.subIdSrc = original.subIdSrc;
      subId = subIdSrc.getAndIncrement();
    }


    @Override
    public int compareTo(TaskContext o) {
      // bigger priorities come first
      int c = Integer.compare(o.priority, priority);
      if (c != 0) {
        return c;
      }

      // IDs are in ascending order
      c = Long.compare(taskId, o.taskId);
      if (c != 0) {
        return c;
      }
      return Long.compare(subId, o.subId);
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TaskContext)) {
        return false;
      }

      TaskContext that = (TaskContext) o;
      return priority == that.priority && subId == that.subId && taskId == that.taskId;
    }


    /**
     * Submit a new job that runs in the current task context.
     *
     * @param job the job to run
     */
    public void fork(Consumer<TaskContext> job) {
      TaskContext newContext = new TaskContext(this);
      Task task = new Task(newContext, () -> job.accept(newContext));
      executor.execute(task);
    }


    /**
     * Submit a new job that runs in the current task context.
     *
     * @param job the job to run
     */
    public void fork(Runnable job) {
      TaskContext newContext = new TaskContext(this);
      Task task = new Task(newContext, job);
      executor.execute(task);
    }


    /**
     * Get the name of the current priority level, if it has one.
     *
     * @return the name
     */
    public String getPriorityName() {
      String p = NAMED_PRIORITIES.get(priority);
      if (p == null) {
        p = Integer.toString(priority);
      }
      return p;
    }


    @Override
    public int hashCode() {
      int result = priority;
      result = 31 * result + (int) (subId ^ (subId >>> 32));
      result = 31 * result + (int) (taskId ^ (taskId >>> 32));
      return result;
    }


    public String toString() {
      return taskId + "-" + subId + " @ " + getPriorityName();
    }

  }



  private final AtomicLong counter = new AtomicLong(0);

  private final Map<Integer, LogCounter> countersForLogging = new CopyOnWriteMap<Integer,LogCounter>(i -> new TreeMap<>(Comparator.reverseOrder()));

  protected final ThreadPoolExecutor executor;


  /**
   * Create a new thread pool.
   *
   * @param corePoolSize the number of threads to keep in the pool
   * @param maxPoolSize  the maximum number of threads allowed in the pool
   */
  public PriorityExecutor(int corePoolSize, int maxPoolSize) {
    if (corePoolSize < 0 || maxPoolSize <= 0) {
      corePoolSize = 2 * Runtime.getRuntime().availableProcessors();
      maxPoolSize = corePoolSize;
    }

    PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>();
    ThreadFactory threadFactory = TaskRunner::new;
    executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 1, TimeUnit.SECONDS, queue, threadFactory);
    executor.allowCoreThreadTimeOut(true);
  }


  /**
   * Create a new thread pool.
   *
   * @param threads the number of threads. Zero or negative uses the number of available processors
   */
  public PriorityExecutor(int threads) {
    this(threads, threads);
  }


  public Executor asExecutor(final int priority) {
    return command -> submit(priority, command);
  }


  @Override
  public void execute(Runnable command) {
    submit(Priorities.DEFAULT, command);
  }


  /**
   * Get a task context that executes at the specified priority. If called from a task that is running at the specified priority, returns the task's context.
   *
   * @param priority the desired priority
   *
   * @return the context
   */
  public TaskContext getTaskContext(int priority) {
    Thread me = Thread.currentThread();
    if (!(me instanceof TaskRunner)) {
      return newTaskContext(priority);
    }

    TaskRunner taskRunner = (TaskRunner) me;
    TaskContext context = taskRunner.getTaskContext();
    if (context != null && context.priority == priority) {
      return context;
    }
    return newTaskContext(priority);
  }


  public TaskContext newTaskContext(int priority) {
    return new TaskContext(priority, counter.getAndIncrement());
  }


  public void shutdown() {
    logger.info("Shutting down priorities executor.");
    executor.shutdown();
  }


  /**
   * Submit a job at the specified priority.
   *
   * @param priority the job priority
   * @param job      the job
   */
  public void submit(int priority, Consumer<TaskContext> job) {
    if (logger.isTraceEnabled()) {
      countersForLogging.computeIfAbsent(priority, i -> new LogCounter()).queued.incrementAndGet();
      logger.trace("Task at priority {} enqueued: {}", priority, countersForLogging);
    }
    TaskContext context = new TaskContext(priority, counter.getAndIncrement());
    Task task = new Task(context, () -> job.accept(context));
    executor.execute(task);
  }


  /**
   * Submit a job at the specified priority.
   *
   * @param priority the job priority
   * @param job      the job
   */
  public void submit(int priority, Runnable job) {
    if (logger.isTraceEnabled()) {
      countersForLogging.computeIfAbsent(priority, i -> new LogCounter()).queued.incrementAndGet();
      logger.trace("Task at priority {} enqueued: {}", priority, countersForLogging);
    }
    Task task = new Task(counter.getAndIncrement(), priority, job);
    executor.execute(task);
  }

}
