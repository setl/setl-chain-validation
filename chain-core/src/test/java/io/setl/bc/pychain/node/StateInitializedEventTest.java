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
package io.setl.bc.pychain.node;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import io.setl.bc.pychain.state.State;
import org.junit.Test;

/**
 * @author Simon Greatrix on 03/12/2019.
 */
public class StateInitializedEventTest {

  @Test
  public void test() {
    State state = mock(State.class);
    StateInitializedEvent event = new StateInitializedEvent(state);
    assertSame(state, event.getState());
  }

}