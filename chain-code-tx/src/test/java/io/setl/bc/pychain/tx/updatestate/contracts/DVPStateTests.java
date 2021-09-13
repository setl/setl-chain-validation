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
package io.setl.bc.pychain.tx.updatestate.contracts;

import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.block.Block;
import io.setl.bc.pychain.file.FileBlockLoader;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.DbgCompareState;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.tx.DefaultProcessor;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.AddressUtil;
import java.io.IOException;
import java.security.MessageDigest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DVPStateTests {

  private static final Logger logger = LoggerFactory.getLogger(DVPStateTests.class);

  MessageDigest digest;

  FileStateLoader fileStateLoaded;

  SerialiseToByte hashSerialiser;


  @Before
  public void setUp() throws Exception {

    digest = MessageDigest.getInstance("SHA-256");
    hashSerialiser = new HashSerialisation();

    Defaults.reset();
    fileStateLoaded = new FileStateLoader();
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


  @Test
  public void stateContract2() throws Exception {

    // Transition involving the sucessful creation of a Namespace.
    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/c3f9c63a08522d98902019611d8698bb7c2692eef7e0ea6e1467cfd054199e6a",
        "src/test/resources/test-states/contracts/Balance/4c635faebe01547d10d04df2640fb21fa1f18466c7a7dfda92865423e842b3bf",
        "src/test/resources/test-states/contracts/Block/0b22372b3ec3a0bac0249675f7c04c2a47b7dd629b772da8319658abc7ddd75b");

    /*
    {
        "13arBFaYHA1599hrCMLpKVcxyEdKX8MMzH": {
            "BoJ|JPY": 200,
            "BofE|GBP": 100
        }
    }
    {
        "15kuZ2kTaANPivrbQkqQN1TTGZSarfbuZu": {
            "HSBC|HSBA": 50
        }
    }
    */

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/4c635faebe01547d10d04df2640fb21fa1f18466c7a7dfda92865423e842b3bf",
        "src/test/resources/test-states/contracts/Balance/90a187f779a6cd2512e43f9b92f2bd2fec1468db787a1f08e39b80517f923461",
        "src/test/resources/test-states/contracts/Block/8f0ed49282ae294f14332b46b75bb0236b2c38e427e9d1ee8e13595b1e1bce30");

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/90a187f779a6cd2512e43f9b92f2bd2fec1468db787a1f08e39b80517f923461",
        "src/test/resources/test-states/contracts/Balance/3423b44cffc3b9cce1c653827990cdd3fde19ed65ad6914c0bda81f2b2a0defe",
        "src/test/resources/test-states/contracts/Block/5b5df4d6cc62accd5e6eb0bdcfc4b7f23dd8f2de8a46576749a99338495f7b8d");

    // These test contain transactions that use EdDSA public key but do not specify the appropriate address, and the address in state requires Base-58. We
    // therefore have to enable Base-58 as the default representation.
    boolean oldSetting = AddressUtil.setUseBase58(true);
    try {
      singleTransitionTest(
          "src/test/resources/test-states/contracts/Balance/3423b44cffc3b9cce1c653827990cdd3fde19ed65ad6914c0bda81f2b2a0defe",
          "src/test/resources/test-states/contracts/Balance/82b8b19ffea1a760dc225bcc68b85f4b21633dcdfbbc26ceee6868b692fa4bb7",
          "src/test/resources/test-states/contracts/Block/74b649ed9cf591d55872b580a9a280445ba36a096b7741d2029d6a4270e03175");

      singleTransitionTest(
          "src/test/resources/test-states/contracts/Balance/82b8b19ffea1a760dc225bcc68b85f4b21633dcdfbbc26ceee6868b692fa4bb7",
          "src/test/resources/test-states/contracts/Balance/dde189630f79134c0002c8993387e31a1fa62991fc5a817481931be3e11e85a9",
          "src/test/resources/test-states/contracts/Block/121bf75c56f6bad5de2479a3f2259b85ced549d3d85f22b030845a5eb6ea6c31");
    } finally {
      AddressUtil.setUseBase58(oldSetting);
    }

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/dde189630f79134c0002c8993387e31a1fa62991fc5a817481931be3e11e85a9",
        "src/test/resources/test-states/contracts/Balance/3ab050a3dfc65e0d954ded808869d9f7579ba3dd3723cf06a1bb23179300b778",
        "src/test/resources/test-states/contracts/Block/cd969c513d3f21dc180edb13021f27e2d920ad9308cdfe63973ac50f32c293d4");

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/3ab050a3dfc65e0d954ded808869d9f7579ba3dd3723cf06a1bb23179300b778",
        "src/test/resources/test-states/contracts/Balance/2cded008d288db1aa554290a74fb9124d2f856454f587225573156dc12c9d726",
        "src/test/resources/test-states/contracts/Block/d9cf8cc6e1b37f9848c7d6ec3ae3f93c0b69aeab382996c6b715fa3b8336907b");
     /*
    {
        "13arBFaYHA1599hrCMLpKVcxyEdKX8MMzH": {
            "BoJ|JPY": 200,
            "BofE|GBP": 100
        }
    }
    {
        "15kuZ2kTaANPivrbQkqQN1TTGZSarfbuZu": {
            "HSBC|HSBA": 50
        }
    }
    {}
    {
        "13arBFaYHA1599hrCMLpKVcxyEdKX8MMzH": {
            "HSBC|HSBA": 50
        }
    }
    {
        "15kuZ2kTaANPivrbQkqQN1TTGZSarfbuZu": {
            "BoJ|JPY": 200,
            "BofE|GBP": 99
        }
    }
      */
  }


  @Test
  public void stateEncumbrance() throws Exception {

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/524d408aa63c609a2df71d431d8d8b4e1fa2b230320b53af46034e30b89a8b5e",
        "src/test/resources/test-states/contracts/Balance/01ad6eab6ac92a3767522558880b2db8ebff59c22799041bcecb7d58bc243317",
        "src/test/resources/test-states/contracts/Block/5394c05478ad55aea8fdad0bcd997d50d8b1033680a9202afb8bc5aa9eb42acd");

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/01ad6eab6ac92a3767522558880b2db8ebff59c22799041bcecb7d58bc243317",
        "src/test/resources/test-states/contracts/Balance/03ce31695c66ee5f11b73c5e2668b326083e0427d0d20a119d90d9d6dc233ee2",
        "src/test/resources/test-states/contracts/Block/ad28c298949255d049c12fb7c1f2c29fa9b5cae75c37c19809c169983cf462f0");

    /* Updated Encumber to not allow empty Administrators.
    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/03ce31695c66ee5f11b73c5e2668b326083e0427d0d20a119d90d9d6dc233ee2",
        "src/test/resources/test-states/contracts/Balance/e365a7d634e1dffa7a11390120d68be3659c7a15dabc8fa082254fa543c48e2c",
        "src/test/resources/test-states/contracts/Block/adda654824cc0f6d5739a78a3f82efb564a959673019fbdf1386ff5ef6876f51");
    */

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/e365a7d634e1dffa7a11390120d68be3659c7a15dabc8fa082254fa543c48e2c",
        "src/test/resources/test-states/contracts/Balance/ade2f36433e136b1173861f0dd8f8e69854a8079532edc16ece0096f4e599caf",
        "src/test/resources/test-states/contracts/Block/66a358e3bd4f250491b64523d78deb5ceb0590eb95da31ebd7fdf687317e40d6");

    singleTransitionTest(
        "src/test/resources/test-states/contracts/Balance/ade2f36433e136b1173861f0dd8f8e69854a8079532edc16ece0096f4e599caf",
        "src/test/resources/test-states/contracts/Balance/9f35c356f5d5a1bf660d1a4c8a3eb0d1e848cc0998b3530a66e3b41431bb5067",
        "src/test/resources/test-states/contracts/Block/22a0efc0cf8e287ac1e6707ffe4a199e5ecaa62f1517cb13c353f76aae3ed19e");


  }


  @After
  public void tearDown() throws Exception {

    Defaults.reset();

  }


  void validateTransition(ObjectEncodedState s0, ObjectEncodedState s1, Block block)
      throws IOException, Exception {

    StateSnapshot newSnap = s0.createSnapshot();

    boolean oldStyleForDebug = false;

    if (!DefaultProcessor.getInstance().processTransactions(
        newSnap, block.getTransactions(), block.getTimeStamp(), true)) {
      throw new RuntimeException("Process Transactions Failed");
    }

    DefaultProcessor.getInstance().postProcessTransactions(newSnap, block, block.getTimeStamp());

    newSnap.commit();

    DefaultProcessor.getInstance().removeProcessedTimeEvents(newSnap, block, block.getTimeStamp());

    s0 = (ObjectEncodedState) newSnap.finalizeBlock(block);

    if (!s0.verifyAll()) {
      if (!s1.verifyAll()) {
        logger.error("New state also fails");
      } else {
        logger.info("Loaded state is ok :=transform failed");
      }

      logger.info("FAILED");
      DbgCompareState.debugCompare(s0, s1);
      DbgCompareState.compareStateObjects(s0, s1);
      throw new RuntimeException(
          String.format("State transform failed from %d to %d", s0.getHeight(), s1.getHeight()));
    } else {
      logger.info("Transition ok {}->{}", s0.getHeight(), s1.getHeight());
    }

  }


}
