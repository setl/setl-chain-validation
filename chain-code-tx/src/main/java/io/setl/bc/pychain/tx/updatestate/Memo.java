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
import io.setl.bc.pychain.state.tx.MemoTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.SuccessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Memo {
  
  private static final Logger logger = LoggerFactory.getLogger(Memo.class);
  
  /**
   * updatestate.
   * Makes no change to state. The purpose of this transaction is solely to be recorded in a block.
   *
   * @param thisTX        : Transaction Object
   * @param stateSnapshot : State Snapshot to update (or not in this case).
   * @param updateTime    : Block time.
   * @param priority      : Update priority, passed from multi-pass block update process.
   * @param checkOnly     :
   * @return :
   */
  public static ReturnTuple updatestate(MemoTx thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
    
    try {
      
      if (thisTX.getChainId() == stateSnapshot.getChainId()) {

        // Check From Address
        String fromAddress = thisTX.getFromAddress();
        
        // If only checking, not updating, this is the time to exit.
        if (checkOnly) {
          return new ReturnTuple(SuccessType.PASS, "Check Only.");
        }
        
      }
  
      return new ReturnTuple(SuccessType.PASS, "");
  
    } catch (Exception e) {
      
      logger.error("Error in Memo.updatestate()", e);
      return new ReturnTuple(SuccessType.FAIL, "Error in Memo.updatestate.");
    }
    
  }
  
}
