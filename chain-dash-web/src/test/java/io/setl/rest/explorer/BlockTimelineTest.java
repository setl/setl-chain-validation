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
package io.setl.rest.explorer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.setl.bc.pychain.dbstore.DBStore;
import io.setl.bc.pychain.dbstore.DBStoreException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

/**
 * @author Simon Greatrix on 2019-03-04.
 */
public class BlockTimelineTest {

  @Test
  public void test() throws DBStoreException {
    HttpServletRequest request = mock(HttpServletRequestWrapper.class);
    DBStore dbStore = mock(DBStore.class);
    when(dbStore.getHeight()).thenReturn(3);
    when(dbStore.getBlockHash(eq(2))).thenReturn("block2");
    when(dbStore.getStateHash(eq(2))).thenReturn("state2");
    when(dbStore.getBlockHash(eq(3))).thenReturn("block3");
    when(dbStore.getStateHash(eq(3))).thenReturn("state3");

    BlockTimeline instance = new BlockTimeline(dbStore, null);
    ResponseEntity<String> output = instance.get(request,2, null, Optional.of(Boolean.FALSE));

    // TODO - Finish this
  }

}