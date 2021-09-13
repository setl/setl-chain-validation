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
package io.setl.bc.pychain.tx.updateevent;

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileBlockLoader;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.state.State;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.exceptions.StateSnapshotCorruptedException;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.DefaultProcessor;
import io.setl.bc.pychain.tx.TransactionProcessor;
import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import org.junit.Assert;
import org.junit.Test;

public class EventsRemovalTest {

  @Test
  public void test() throws StateSnapshotCorruptedException {

    FileBlockLoader blockLoader = new FileBlockLoader();
    String contractAddress = "3LBXiu6Wj6QdbYfTq2MZy7MvwvyCBTmGpo";

    FileStateLoader l = new FileStateLoader();
//dbf05588ab62c4b624c767688b26a8fd595e9dea9d96dca9dbb9366e6bd5d9aa
    try {
      final ObjectEncodedState state1 =
          l.loadStateFromFile("./src/test/resources/test-events-rm/a8e55e69a0cdefb19fc2fa853a9c7502de1937498da198adba7a4d6a5bc99777");
      final ObjectEncodedState state2 =
          l.loadStateFromFile("./src/test/resources/test-events-rm/65e01ef3e1ce454e52e9cc622e0c09ae1979bb2d790448cb7d8ebbeb713b7825");

      Block block = blockLoader.loadBlockFromFile("./src/test/resources/test-events-rm/dbf05588ab62c4b624c767688b26a8fd595e9dea9d96dca9dbb9366e6bd5d9aa");
      Txi[] tx = block.getTransactions();
      StateSnapshot snap = state1.createSnapshot();
      TransactionProcessor transactionProcessor = DefaultProcessor.getInstance();

      Assert.assertTrue(transactionProcessor.processTransactions(snap, tx, block.getTimeStamp(), true));
      Assert.assertTrue(transactionProcessor.postProcessTransactions(snap, block, block.getTimeStamp()));

      snap.commit();
      transactionProcessor.removeProcessedTimeEvents(snap, block, block.getTimeStamp());
      snap.commit();
      State statex = snap.finalizeBlock(block);
      Map<Long, SortedSet<String>> eventsX = statex.getContractTimeEvents().getEventDetailsBefore(Long.MAX_VALUE - 1);
      Map<Long, SortedSet<String>> events1 = state1.getContractTimeEvents().getEventDetailsBefore(Long.MAX_VALUE - 1);
      Assert.assertTrue(eventsX.size() < events1.size());
      Assert.assertFalse(eventsX.entrySet().stream().filter(
          ev -> ev.getValue().contains(contractAddress)
              && ev.getKey() <= block.getTimeStamp()
      ).findFirst().isPresent());

      assert state1.getHeight() + 1 == state2.getHeight();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
