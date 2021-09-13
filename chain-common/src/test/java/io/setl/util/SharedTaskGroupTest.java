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

import static org.junit.Assert.assertEquals;

import io.setl.util.SharedTaskGroup.SharedTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class SharedTaskGroupTest {

  @Test
  public void testMultithreading() {
    Executor executor = Executors.newFixedThreadPool(4);
    SharedTaskGroup stg = new SharedTaskGroup(executor, 4);
    SharedTask<Integer> task = stg.submit(() -> 0);
    for (int i = 0; i < 20; i++) {
      final SharedTask<Integer> previous = task;
      task = stg.submit(() -> previous.get() + 1);
    }

    assertEquals(20, task.get().intValue());
  }


  @Test
  public void testWaitersWillRunTasks() {
    // An executor that never runs the commands passed to it.
    Executor doNothing = new Executor() {
      @Override
      public void execute(Runnable command) {
        // do nothing
      }
    };

    SharedTaskGroup stg = new SharedTaskGroup(doNothing);
    SharedTask<Integer> task = stg.submit(() -> 0);
    for (int i = 0; i < 20; i++) {
      final SharedTask<Integer> previous = task;
      task = stg.submit(() -> previous.get() + 1);
    }

    assertEquals(20, task.get().intValue());
  }
}