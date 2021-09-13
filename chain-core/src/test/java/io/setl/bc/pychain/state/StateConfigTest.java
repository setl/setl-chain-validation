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
package io.setl.bc.pychain.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.common.TypeSafeMap;

public class StateConfigTest {

  private final long defaultMaxTxAge = 86400L;

  private final long defaultMaxTxPerBlock = 20000L;

  private final long defaultMaxtimersperblock = 2000L;

  private final long largeMaxTxAge = 9753108642L;

  private final long largeMaxTxPerBlock = 2468013579L;

  private final long largeMaxtimersperblock = 1234567890L;

  private final long minimumMaxTxAge = 5L;

  private final long minimumMaxTxPerBlock = 1000L;

  private final long minimumMaxtimersperblock = 100L;

  private final String someOtherString = "SomeOtherString";

  private final long standardMaxTxAge = 20000L;

  private final long standardMaxTxPerBlock = 1234;

  private final long standardMaxtimersperblock = 20000L;

  String stateFile = "src/test/resources/test-states/genesis/20/e00f2e0ddc1e76879ce56437a2d153c7f039a44a784e9c659a5c581971c33999";

  private Setting<String> other = new Setting("other", TypeSafeMap::asString);

  private StateConfig stateConfigEmpty;

  private StateConfig stateConfigHigh;

  private StateConfig stateConfigLow;

  private StateConfig stateConfigStandard;

  private HashMap<String, Object> testMapEmpty;

  private HashMap<String, Object> testMapHigh;

  private HashMap<String, Object> testMapLow;

  private HashMap<String, Object> testMapStandard;


  @Test
  public void getConfigValue() throws Exception {
    assertTrue(someOtherString.equals(stateConfigStandard.getConfigValue(other)));
    assertTrue(someOtherString.equals(stateConfigStandard.getConfigValue(other)));

    assertNull(stateConfigStandard.getConfigValue(new Setting<String>("Unknown", TypeSafeMap::asString)));
  }


  @Test
  public void getMaxTimersPerBlock() throws Exception {
    // Verify Default values for Empty map.
    assertEquals(defaultMaxtimersperblock, stateConfigEmpty.getMaxTimersPerBlock());
    // And again...
    assertEquals(defaultMaxtimersperblock, stateConfigEmpty.getMaxTimersPerBlock());

    // Verify given values for Standard map.
    assertEquals(standardMaxtimersperblock, stateConfigStandard.getMaxTimersPerBlock());
    // And again...
    assertEquals(standardMaxtimersperblock, stateConfigStandard.getMaxTimersPerBlock());

    // Verify given values for Low map.
    assertEquals(minimumMaxtimersperblock, stateConfigLow.getMaxTimersPerBlock());
    // And again...
    assertEquals(minimumMaxtimersperblock, stateConfigLow.getMaxTimersPerBlock());

    // Verify given values for High map.
    assertEquals(largeMaxtimersperblock, stateConfigHigh.getMaxTimersPerBlock());
    // And again...
    assertEquals(largeMaxtimersperblock, stateConfigHigh.getMaxTimersPerBlock());

  }


  @Test
  public void getMaxTxAge() throws Exception {
    // Verify Default values for Empty map.
    assertEquals(defaultMaxTxAge, stateConfigEmpty.getMaxTxAge());
    // And again...
    assertEquals(defaultMaxTxAge, stateConfigEmpty.getMaxTxAge());

    // Verify given values for Standard map.
    assertEquals(standardMaxTxAge, stateConfigStandard.getMaxTxAge());
    // And again...
    assertEquals(standardMaxTxAge, stateConfigStandard.getMaxTxAge());

    // Verify given values for Low map.
    assertEquals(minimumMaxTxAge, stateConfigLow.getMaxTxAge());
    // And again...
    assertEquals(minimumMaxTxAge, stateConfigLow.getMaxTxAge());

    // Verify given values for High map.
    assertEquals(largeMaxTxAge, stateConfigHigh.getMaxTxAge());
    // And again...
    assertEquals(largeMaxTxAge, stateConfigHigh.getMaxTxAge());

  }


  @Test
  public void getMaxTxPerBlock() throws Exception {
    // Verify Default values for Empty map.
    assertEquals(defaultMaxTxPerBlock, stateConfigEmpty.getMaxTxPerBlock());
    // And again...
    assertEquals(defaultMaxTxPerBlock, stateConfigEmpty.getMaxTxPerBlock());

    // Verify given values for Standard map.
    assertEquals(standardMaxTxPerBlock, stateConfigStandard.getMaxTxPerBlock());
    // And again...
    assertEquals(standardMaxTxPerBlock, stateConfigStandard.getMaxTxPerBlock());

    // Verify given values for Low map.
    assertEquals(minimumMaxTxPerBlock, stateConfigLow.getMaxTxPerBlock());
    // And again...
    assertEquals(minimumMaxTxPerBlock, stateConfigLow.getMaxTxPerBlock());

    // Verify given values for High map.
    assertEquals(largeMaxTxPerBlock, stateConfigHigh.getMaxTxPerBlock());
    // And again...
    assertEquals(largeMaxTxPerBlock, stateConfigHigh.getMaxTxPerBlock());

  }


  @Test
  public void getMustRegister() throws Exception {
    // Verify Default values for Empty map.
    assertEquals(false, stateConfigEmpty.getMustRegister());
    // And again...
    assertEquals(false, stateConfigEmpty.getMustRegister());

    // Verify given values for Standard map.
    assertEquals(true, stateConfigStandard.getMustRegister());
    // And again...
    assertEquals(true, stateConfigStandard.getMustRegister());

    // Verify given values for Low map.
    assertEquals(false, stateConfigLow.getMustRegister());
    // And again...
    assertEquals(false, stateConfigLow.getMustRegister());

    // Verify given values for High map.
    assertEquals(true, stateConfigHigh.getMustRegister());
    // And again...
    assertEquals(true, stateConfigHigh.getMustRegister());

  }


  @Test
  public void getOther() throws Exception {
    // Verify Default values for Empty map.
    assertNull(stateConfigEmpty.getConfigValue(other));
    // And again...
    assertNull(stateConfigEmpty.getConfigValue(other));

    // Verify given values for Standard map.
    assertTrue(stateConfigStandard.getConfigValue(other).equals(someOtherString));
    // And again...
    assertTrue(stateConfigStandard.getConfigValue(other).equals(someOtherString));

    // Verify given values for Low map.
    assertTrue(stateConfigLow.getConfigValue(other).equals(someOtherString));
    // And again...
    assertTrue(stateConfigLow.getConfigValue(other).equals(someOtherString));

    // Verify given values for High map.
    assertTrue(stateConfigHigh.getConfigValue(other).equals(someOtherString));
    // And again...
    assertTrue(stateConfigHigh.getConfigValue(other).equals(someOtherString));

  }


  /**
   * setUp.
   *
   * @throws Exception :
   */
  @Before
  public void setUp() throws Exception {
    FileStateLoader fileStateLoaded = new FileStateLoader();

    testMapEmpty = new HashMap<>();
    StateSnapshot emptyMapSnap = new StateSnapshotImplementation(fileStateLoaded.loadStateFromFile(stateFile), NoOpChangeListener.INSTANCE);

    stateConfigEmpty = new StateConfig(emptyMapSnap);

    emptyMapSnap = new StateSnapshotImplementation(fileStateLoaded.loadStateFromFile(stateFile), NoOpChangeListener.INSTANCE);

    emptyMapSnap.setConfigValue("registeraddresses", (Long) 1L);
    emptyMapSnap.setConfigValue("maxtxage", (Long) standardMaxTxAge);
    emptyMapSnap.setConfigValue("maxtxperblock", (Long) standardMaxTxPerBlock);
    emptyMapSnap.setConfigValue("maxtimersperblock", (Long) standardMaxtimersperblock);
    emptyMapSnap.setConfigValue("other", someOtherString);
    stateConfigStandard = new StateConfig(emptyMapSnap);

    emptyMapSnap = new StateSnapshotImplementation(fileStateLoaded.loadStateFromFile(stateFile), NoOpChangeListener.INSTANCE);

    emptyMapSnap.setConfigValue("registeraddresses", (Long) 0L);
    emptyMapSnap.setConfigValue("maxtxage", (Long) 0L);
    emptyMapSnap.setConfigValue("maxtxperblock", (Long) 0L);
    emptyMapSnap.setConfigValue("maxtimersperblock", (Long) 0L);
    emptyMapSnap.setConfigValue("other", someOtherString);
    stateConfigLow = new StateConfig(emptyMapSnap);

    emptyMapSnap = new StateSnapshotImplementation(fileStateLoaded.loadStateFromFile(stateFile), NoOpChangeListener.INSTANCE);

    emptyMapSnap.setConfigValue("registeraddresses", (Long) 5L);
    emptyMapSnap.setConfigValue("maxtxage", (Long) largeMaxTxAge);
    emptyMapSnap.setConfigValue("maxtxperblock", (Long) largeMaxTxPerBlock);
    emptyMapSnap.setConfigValue("maxtimersperblock", (Long) largeMaxtimersperblock);
    emptyMapSnap.setConfigValue("other", someOtherString);
    stateConfigHigh = new StateConfig(emptyMapSnap);
  }


  @After
  public void tearDown() throws Exception {

    Defaults.reset();

  }

}
