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

import io.setl.common.MutableLong;
import org.junit.Test;

/**
 * @author Simon Greatrix on 2019-06-26.
 */
public class CallerRunsExecutorTest {

  @Test
  public void execute() {
    MutableLong otherId = new MutableLong(Thread.currentThread().getId() + 1234L);
    CallerRunsExecutor.INSTANCE.execute(() -> {
      otherId.set(Thread.currentThread().getId());
    });
    assertEquals(Thread.currentThread().getId(), otherId.longValue());
  }
}