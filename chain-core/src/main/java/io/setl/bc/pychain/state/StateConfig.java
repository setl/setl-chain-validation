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

import static io.setl.common.CommonPy.ConfigConstants.DEFAULT_MAX_TIMERS_PER_BLOCK;
import static io.setl.common.CommonPy.ConfigConstants.DEFAULT_MAX_TX_AGE;
import static io.setl.common.CommonPy.ConfigConstants.DEFAULT_MAX_TX_PER_BLOCK;
import static io.setl.common.CommonPy.ConfigConstants.DEFAULT_MIN_AGE_ADDRESS_DELETE;
import static io.setl.common.CommonPy.ConfigConstants.DEFAULT_REQUIRE_AUTHORISED_ADDRESSES;
import static io.setl.common.CommonPy.ConfigConstants.MINIMUM_MAX_TIMERS_PER_BLOCK;
import static io.setl.common.CommonPy.ConfigConstants.MINIMUM_MAX_TX_AGE;
import static io.setl.common.CommonPy.ConfigConstants.MINIMUM_MAX_TX_PER_BLOCK;
import static io.setl.common.CommonPy.ConfigConstants.MINIMUM_MIN_AGE_ADDRESS_DELETE;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.common.TypeSafeMap;

/**
 * StateConfig.
 *
 * <p>Wrapper class to hold initial State Config values and provide performant access to standard values.</p>
 */
public class StateConfig {

  public static final Setting<Boolean> AUTHORISE_BY_ADDRESS = new Setting<>("authorisebyaddress", TypeSafeMap::asBoolean);

  public static final Setting<Long> MAX_TIMERS_PER_BLOCK = new Setting<>("maxtimersperblock", TypeSafeMap::asLong);

  public static final Setting<Long> MAX_TX_AGE = new Setting<>("maxtxage", TypeSafeMap::asLong);

  public static final Setting<Long> MAX_TX_PER_BLOCK = new Setting<>("maxtxperblock", TypeSafeMap::asLong);

  public static final Setting<Long> MIN_ADDRESS_AGE = new Setting<>("minaddressage", TypeSafeMap::asLong);

  public static final Setting<Boolean> MUST_REGISTER = new Setting<>("registeraddresses", TypeSafeMap::asBoolean);

  private final StateBase stateBase;

  private long maxTimersPerBlock;

  private boolean maxTimersPerBlockIsSet = false;

  private long maxTxAge;

  private boolean maxTxAgeIsSet = false;

  private long maxTxPerBlock;

  private boolean maxTxPerBlockIsSet = false;

  private boolean minAddresAgeIsSet = false;

  private long minAddressAge;

  private boolean mustRegister;

  private boolean mustRegisterIsSet = false;

  private boolean txAuthoriseByAddress = false;

  private boolean txAuthoriseByAddressIsSet = false;


  public StateConfig(StateBase stateBase) {
    this.stateBase = stateBase;
  }


  /**
   * Return `authorisebyaddress` Cache value.
   *
   * @return :
   */
  public boolean getAuthoriseByAddress() {
    /* Each address is individually authorised. */

    if (!txAuthoriseByAddressIsSet) {
      txAuthoriseByAddressIsSet = true;
      txAuthoriseByAddress = stateBase.getConfigValue(AUTHORISE_BY_ADDRESS, DEFAULT_REQUIRE_AUTHORISED_ADDRESSES).booleanValue();
    }

    return txAuthoriseByAddress;
  }


  /**
   * Get a configuration value from either the state or the snapshot.
   *
   * @param setting the configuration value's name
   *
   * @return the value
   */
  public <Q> Q getConfigValue(Setting<Q> setting) {
    return stateBase.getConfigValue(setting);
  }


  /**
   * Return `maxtimersperblock` Cache value.
   *
   * @return :
   */
  public long getMaxTimersPerBlock() {

    if (!maxTimersPerBlockIsSet) {
      maxTimersPerBlockIsSet = true;
      maxTimersPerBlock = Math.max(stateBase.getConfigValue(MAX_TIMERS_PER_BLOCK, DEFAULT_MAX_TIMERS_PER_BLOCK), MINIMUM_MAX_TIMERS_PER_BLOCK);
    }

    return maxTimersPerBlock;
  }


  /**
   * Return `maxtxage` Cache value.
   *
   * @return :
   */
  public long getMaxTxAge() {

    if (!maxTxAgeIsSet) {
      maxTxAgeIsSet = true;
      maxTxAge = Math.max(stateBase.getConfigValue(MAX_TX_AGE, DEFAULT_MAX_TX_AGE), MINIMUM_MAX_TX_AGE);
    }

    return maxTxAge;
  }


  /**
   * Return `maxtxperblock` Cache value.
   *
   * @return :
   */
  public long getMaxTxPerBlock() {
    if (!maxTxPerBlockIsSet) {
      maxTxPerBlockIsSet = true;
      maxTxPerBlock = Math.max(stateBase.getConfigValue(MAX_TX_PER_BLOCK, DEFAULT_MAX_TX_PER_BLOCK), MINIMUM_MAX_TX_PER_BLOCK);
    }

    return maxTxPerBlock;
  }


  /**
   * Return `minaddressage` Cache value.
   *
   * @return :
   */
  public long getMinAddressAgeToDelete() {

    if (!minAddresAgeIsSet) {
      minAddresAgeIsSet = true;
      minAddressAge = Math.max(MINIMUM_MIN_AGE_ADDRESS_DELETE, stateBase.getConfigValue(MIN_ADDRESS_AGE, DEFAULT_MIN_AGE_ADDRESS_DELETE));
    }

    return minAddressAge;
  }


  /**
   * Return `MustRegister` Cache value.
   *
   * @return :
   */
  public boolean getMustRegister() {

    if (!mustRegisterIsSet) {
      mustRegisterIsSet = true;
      mustRegister = stateBase.getConfigValue(MUST_REGISTER, Boolean.FALSE).booleanValue();
    }

    return mustRegister;
  }


  /**
   * <p>resetPerformanceCache : Some values are cached for performance reasons. This method resets that caching.</p>
   */
  public void resetPerformanceCache() {
    mustRegisterIsSet = false;
    maxTimersPerBlockIsSet = false;
    maxTxPerBlockIsSet = false;
    minAddresAgeIsSet = false;
    maxTxAgeIsSet = false;
    txAuthoriseByAddressIsSet = false;
  }

}
