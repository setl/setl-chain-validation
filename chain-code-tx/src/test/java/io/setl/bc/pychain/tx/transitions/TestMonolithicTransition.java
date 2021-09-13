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
package io.setl.bc.pychain.tx.transitions;

import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileBlockLoader;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.state.DbgCompareState;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.tx.DefaultProcessor;
import io.setl.bc.pychain.tx.Init;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMonolithicTransition {

  private static final Logger logger = LoggerFactory.getLogger(TestMonolithicTransition.class);

  private static final String path = "src/test/resources/test-transitions/mono";


  @Test
  // @Ignore
  public void loadStatesValidateHashedFileName() throws Exception {

    List<Object[]> exceptions = new ArrayList<>();
    Helper.findTransitionTestData(trans -> {
      try {
        singleTransitionTest(trans.state0.toString(), trans.state1.toString(), trans.blockPath.toString());
      } catch (Exception e) {
        exceptions.add(new Object[]{trans.state0.toString(), trans.state1.toString(), e});
        // throw new RuntimeException(e);
      }
    }, path);
    if (!exceptions.isEmpty()) {

      exceptions.forEach(e -> {
        System.err.println(String.format("Failed to do transitions from %s to %s", e[0], e[1]));
        Exception ee = ((Exception) e[2]);
        ee.printStackTrace();
      });
      Assert.fail();
    }
  }


  @Before
  public void setup() {

    Init.setup();
  }
  // src/test/resources/test-transitions/mono/task/226-227/226/aeada625b0d31a695071ba72aca9aa0ad7a470d8cf89f78f036627a81a75f813
  // to
  // src/test/resources/test-transitions/mono/task/226-227/227/1141cc4917ed2c3b1639014ebd5d59f09c61af4b10981d3040d2a468aed95571


  /**
   * single.
   *
   * @throws Exception :
   */
  //
  // @Test
  public void single() throws Exception {
    // singleTransitionTest(
    // "src/test/resources/test-transitions/mono/task/5-6/5/12fc3dced794ffd50f22e651d352e01fe2221b3980b6d2d3c01a19f5fa1746f5
    // to
    // src/test/resources/test-transitions/mono/task/5-6/6/14164a8607cbc6d4e8a9839e04d8b004ba7491f1fef90724f622f462fc19080b
    // "src/test/resources/test-transitions/mono/task/231-232/231/ba7720a633863d93a0598c0a377d33d41a6bce63e94d21a06cf797afbca18b66",
    // "src/test/resources/test-transitions/mono/task/231-232/232/07677e9667a6e4d8ccf9a587ff27879157722f4b32991c2e92f3dff9cf769499",
    // "src/test/resources/test-transitions/mono/task/231-232/808505f3d5bd883622ac2ceb5a8c966b9089fad2983189c851782a04b918513f");
    singleTransitionTest(
        "src/test/resources/test-transitions/mono/task/5-6/5/12fc3dced794ffd50f22e651d352e01fe2221b3980b6d2d3c01a19f5fa1746f5",
        "src/test/resources/test-transitions/mono/task/5-6/6/14164a8607cbc6d4e8a9839e04d8b004ba7491f1fef90724f622f462fc19080b",
        "src/test/resources/test-transitions/mono/task/5-6/47cc7420bda671f748b8b61bcf06ef275ebbb6236bf868ddab00efe9fff40bfb");


  }


  /**
   * singleTransitionTest.
   *
   * @param from  :
   * @param to    :
   * @param block :
   *
   * @throws Exception :
   */
  public void singleTransitionTest(String from, String to, String block) throws Exception {

    FileStateLoader sl = new FileStateLoader();
    FileBlockLoader bl = new FileBlockLoader();

    Block b = bl.loadBlockFromFile(block);

    ObjectEncodedState s0 = sl.loadStateFromFile(from);

    if (!s0.verifyAll()) {
      throw new RuntimeException(String.format("Loaded state(%d) does not verify", s0.getHeight()));
    }

    ObjectEncodedState s1 = sl.loadStateFromFile(to);
    if (!s1.verifyAll()) {
      throw new RuntimeException(String.format("Loaded state(%d) does not verify", s1.getHeight()));
    }

    validateTransition(s0, s1, b);
  }


  void validateTransition(ObjectEncodedState s0, ObjectEncodedState s1, Block block)
      throws IOException, Exception {

    StateSnapshot orgSnap = s0.createSnapshot();
    StateSnapshot newSnap = s0.createSnapshot();

    if (s1.getHeight() == 41) {
      logger.info("41");
    }
    if (!DefaultProcessor.getInstance().processTransactions(
        newSnap, block.getTransactions(), block.getTimeStamp(), true)) {
      throw new RuntimeException("Process Transactions Failed");
    }

    newSnap.commit();

    s0 = (ObjectEncodedState) newSnap.finalizeBlock(block);

    if (!s0.verifyAll()) {
      if (!s1.verifyAll()) {
        logger.error("New state also fails");
      } else {
        logger.info("Loaded state is ok :=transform failed");
      }

      logger.info("FAILED");
      DbgCompareState.debugCompare(s0, s1);
      throw new RuntimeException(
          String.format("State transform failed from %d to %d", s0.getHeight(), s1.getHeight()));
    } else {
      logger.info("Transition ok {}->{}", s0.getHeight(), s1.getHeight());
    }

  }

}
