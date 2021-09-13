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
package io.setl.bc.pychain.consensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.setl.bc.pychain.Hash;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;

public class ConsensusTest {

  @Test
  public void curIntervalTest() {

    assertEquals("Expected 10", 10L, ProposerElection.curInterval(12));
    assertEquals("Expected 15", 15L, ProposerElection.curInterval(15));
  }

  @Test
  public void testHashChooser3() throws NoSuchAlgorithmException {

    Hash blockHash = Hash.fromHex("c1e191e11cdf66a7a0ab535996d180e9fc744b834f144f06b043a8665027d02f");
    long interval = 1490273600;
    long height = 71;

    assertEquals(99843, ProposerElection.selectProposer(blockHash, interval, height, 100000));
  }
}
