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
package io.setl.bc.pychain.tx.updatestate;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.tx.NullTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NullTx Update.
 */
public class Null {
  
  private static final Logger logger = LoggerFactory.getLogger(Null.class);
  
  /**
   * updatestate.
   * Makes no change to state, it's only useful function is to advance the nonce, check consensus or appear fast.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   * @return :
   */
  public static ReturnTuple updatestate(NullTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    
    try {
      
      // Check that this Tx is on the correct chain.
      
      if (thisTX.getChainId() == stateSnapshot.getChainId()) {
        
        // Native chain
        
        // Verification : Apply only to the 'Native' chain...
        
        if ((thisTX.getTimestamp() > 0) && (Math.abs(thisTX.getTimestamp() - updateTime) > stateSnapshot.getStateConfig().getMaxTxAge())) {
          return new ReturnTuple(SuccessType.FAIL, "Tx Timestamp invalid.");
        }
        
      }
      
      return new ReturnTuple(SuccessType.PASS, "");
  
    } catch (Exception e) {
      logger.error("Error in Null.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in Null.Updatestate");
    }
    
  }
  
}