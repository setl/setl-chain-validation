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

import io.setl.util.PriorityExecutor.TaskContext;
import java.util.LinkedList;
import java.util.Spliterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * @author Simon Greatrix on 2019-04-16.
 */
@SuppressWarnings("squid:S2142")
public class ParallelTask {

  private static final int SEQUENTIAL_TASK_LIMIT = 20; //The limit at which transaction verification will be done in parallel.



  @FunctionalInterface
  public interface IntPredicate {

    /**
     * Process the specified value. Returns true if the processing should stop
     *
     * @param index the index value
     *
     * @return FALSE if processing continues, TRUE if it should stop.
     */
    boolean test(int index);
  }



  static class Section {

    int end;

    int start;


    public Section(int start, int end) {
      this.start = start;
      this.end = end;
    }
  }



  static class Split<T> implements Runnable {

    final AtomicBoolean carryOn;

    final Consumer<T> consumer;

    final LinkedBlockingDeque<Spliterator<T>> queue;

    final int sequentialLimit;

    final AtomicInteger toDo;


    Split(LinkedBlockingDeque<Spliterator<T>> queue, int sequentialLimit, AtomicInteger toDo, AtomicBoolean carryOn, Consumer<T> consumer) {
      this.consumer = consumer;
      this.queue = queue;
      this.toDo = toDo;
      this.carryOn = carryOn;
      this.sequentialLimit = sequentialLimit;
    }


    public void run() {
      while (true) {
        Spliterator<T> spliterator = queue.poll();
        if (spliterator == null) {
          return;
        }
        try {
          Spliterator<T> s2;
          while (
              carryOn.get()
                  && spliterator.estimateSize() > sequentialLimit
                  && (s2 = spliterator.trySplit()) != null) {
            queue.addLast(s2);
            toDo.incrementAndGet();
          }

          while (carryOn.get() && spliterator.tryAdvance(consumer)) {
            // do nothing
          }
        } finally {
          if (toDo.decrementAndGet() == 0) {
            synchronized (toDo) {
              toDo.notifyAll();
            }
          }
        }
      }
    }
  }



  static class Task implements Runnable {

    final IntPredicate implementation;

    final int sequentialLimit;

    final AtomicBoolean shouldStop;

    final BlockingQueue<Section> taskQueue;

    final AtomicInteger toDo;


    Task(BlockingQueue<Section> taskQueue, int sequentialLimit, AtomicInteger toDo, AtomicBoolean shouldStop, IntPredicate implementation) {
      this.taskQueue = taskQueue;
      this.sequentialLimit = sequentialLimit;
      this.toDo = toDo;
      this.shouldStop = shouldStop;
      this.implementation = implementation;
    }


    void doSection(Section section) {
      int start = section.start;
      int end = section.end;

      // If we are stopping, do not enqueue further sections
      if (!shouldStop.get()) {
        while ((end - start) > sequentialLimit) {
          int middle = (start + end) / 2;
          toDo.incrementAndGet();
          if (!taskQueue.offer(new Section(middle, end))) {
            throw new IllegalStateException("Unable to enqueue additional tasks");
          }
          end = middle;
        }
      }

      try {
        for (int i = start; i < end; i++) {
          if (shouldStop.get()) {
            return;
          }

          if (implementation.test(i)) {
            shouldStop.set(true);
            return;
          }
        }
      } finally {

        if (toDo.decrementAndGet() == 0) {
          synchronized (toDo) {
            toDo.notifyAll();
          }
        }
      }
    }


    public void run() {
      while (true) {
        Section section = taskQueue.poll();
        if (section == null) {
          return;
        }
        doSection(section);
      }
    }
  }


  /**
   * Process some task that can be defined by an integer range in parallel.
   *
   * @param taskContext    the context
   * @param length         the end of the range
   * @param implementation the implementation of the index
   */
  public static void process(TaskContext taskContext, int length, IntConsumer implementation) {
    process(taskContext, SEQUENTIAL_TASK_LIMIT, length, index -> {
      implementation.accept(index);
      return false;
    });
  }


  /**
   * Process some task that can be defined by an integer range in parallel.
   *
   * @param taskContext    the context
   * @param length         the end of the range
   * @param implementation the implementation of the index
   */
  public static void process(TaskContext taskContext, int length, IntPredicate implementation) {
    process(taskContext, SEQUENTIAL_TASK_LIMIT, length, implementation);
  }


  /**
   * Process some task that can be defined by an integer range in parallel.
   *
   * @param taskContext    the context
   * @param length         the end of the range
   * @param implementation the implementation of the index
   */
  public static void process(TaskContext taskContext, int sequentialLimit, int length, IntConsumer implementation) {
    process(taskContext, sequentialLimit, length, index -> {
      implementation.accept(index);
      return false;
    });
  }


  /**
   * Process some task that can be defined by an integer range in parallel.
   *
   * @param taskContext    the context
   * @param length         the end of the range
   * @param implementation the implementation of the index
   */
  public static void process(TaskContext taskContext, int sequentialLimit, int length, IntPredicate implementation) {
    int cpuCount = Runtime.getRuntime().availableProcessors();

    // How many parts should we break the job into?
    int startingParts = Math.min(cpuCount, (int) Math.ceil(((double) length) / sequentialLimit));
    final AtomicInteger toDo = new AtomicInteger(startingParts);
    final AtomicBoolean shouldStop = new AtomicBoolean(false);
    final LinkedBlockingQueue<Section> taskQueue = new LinkedBlockingQueue<>();

    Task myTask;
    if (startingParts < 2) {
      // We will do everything in this thread.
      Section section = new Section(0, length);
      taskQueue.add(section);
      myTask = new Task(taskQueue, sequentialLimit, toDo, shouldStop, implementation);

    } else {

      float end = 0f;
      float step = (float) length / startingParts;
      for (int i = 0; i < startingParts - 1; i++) {
        int s = (int) end;
        end += step;
        taskQueue.add(new Section(s, (int) end));
      }
      // Make final part explicitly cover to the full length, in case of rounding errors.
      taskQueue.add(new Section((int) end, length));

      // Now start the parallel processing.
      for (int i = 0; i < startingParts - 1; i++) {
        Task task = new Task(taskQueue, sequentialLimit, toDo, shouldStop, implementation);
        taskContext.fork(task);
      }

      myTask = new Task(taskQueue, sequentialLimit, toDo, shouldStop, implementation);
    }

    myTask.run();

    // wait for job to complete
    synchronized (toDo) {
      while (toDo.get() > 0) {
        try {
          toDo.wait();
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
    }
  }


  /**
   * Process some task that operates on a spliterator in parallel.
   */
  public static <T> void process(TaskContext taskContext, Spliterator<T> spliterator, Predicate<T> implementation) {
    process(taskContext, SEQUENTIAL_TASK_LIMIT, spliterator, implementation);
  }


  /**
   * Process some task that operates on a spliterator in parallel.
   */
  public static <T> void process(TaskContext taskContext, int sequentialLimit, Spliterator<T> spliterator, Predicate<T> implementation) {
    final LinkedBlockingDeque<Spliterator<T>> queue = splitTasks(sequentialLimit, spliterator);
    final AtomicBoolean carryOn = new AtomicBoolean(true);
    final Consumer<T> consumer = t -> {
      if (carryOn.get() && implementation.test(t)) {
        carryOn.set(false);
      }
    };

    AtomicInteger toDo = new AtomicInteger(queue.size());

    Split<T> mySplit = new Split<>(queue, sequentialLimit, toDo, carryOn, consumer);

    // NB: The to-do counter may change as this loop runs, so we count down not up.
    for (int i = toDo.get(); i > 1; i--) {
      taskContext.fork(new Split<>(queue, sequentialLimit, toDo, carryOn, consumer));
    }

    mySplit.run();

    // wait for job to complete
    synchronized (toDo) {
      while (toDo.get() != 0) {
        try {
          toDo.wait();
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
    }
  }


  private static <T> LinkedBlockingDeque<Spliterator<T>> splitTasks(int sequentialLimit, Spliterator<T> spliterator) {
    int cpuCount = Runtime.getRuntime().availableProcessors();
    final LinkedBlockingDeque<Spliterator<T>> queue = new LinkedBlockingDeque<>();

    LinkedList<Spliterator<T>> canSplit = new LinkedList<>();
    canSplit.addLast(spliterator);
    for (int i = 0; i < cpuCount && !canSplit.isEmpty(); i++) {
      Spliterator<T> s1 = canSplit.removeFirst();
      long size = s1.estimateSize();
      if (size > sequentialLimit) {
        Spliterator<T> s2 = s1.trySplit();
        if (s2 != null) {
          // worth looking at again
          canSplit.addLast(s1);
          canSplit.addLast(s2);
        } else {
          // cannot split
          queue.addLast(s1);
        }
      } else {
        // too small to split
        queue.addLast(s1);
      }
    }
    queue.addAll(canSplit);
    return queue;
  }
}

