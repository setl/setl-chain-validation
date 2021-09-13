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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK_COMMIT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE_COMMIT;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_NOMINATE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.tx.CommitContractInterface;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.bc.pychain.tx.updatestate.contracts.DvpCommit;
import io.setl.bc.pychain.tx.updatestate.contracts.ExchangeCommit;
import io.setl.bc.pychain.tx.updatestate.contracts.Nominate;
import io.setl.common.CommonPy.SuccessType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CommitToContract.
 */
public class CommitToContract {
  
  private static final Logger logger = LoggerFactory.getLogger(CommitToContract.class);
  
  /**
   * updatestate.
   *
   * @param thisTX        :
   * @param stateSnapshot :
   * @param updateTime    :
   * @param priority      :
   * @param checkOnly     :
   * @return :
   */
  public static ReturnTuple updatestate(CommitContractInterface thisTX, StateSnapshot stateSnapshot, long updateTime, int priority, boolean checkOnly) {
  
    if (thisTX == null) {
      return new ReturnTuple(SuccessType.FAIL, "Tx is Null");
    }
  
    IContractData thisContractData = thisTX.getCommitmentData();
  
    if (thisContractData == null) {
      return new ReturnTuple(SuccessType.FAIL, "Tx ContractData is Null");
    }
  
    switch (thisContractData.get__function()) {
    
      case CONTRACT_NAME_DVP_UK:
      case CONTRACT_NAME_DVP_UK_COMMIT:
        return DvpCommit.updatestate(thisTX, stateSnapshot, updateTime, priority, checkOnly);
    
      case CONTRACT_NAME_NOMINATE:
      case CONTRACT_NAME_TOKENS_NOMINATE:
        return Nominate.updatestate(thisTX, stateSnapshot, updateTime, priority, checkOnly);

      case CONTRACT_NAME_EXCHANGE:
      case CONTRACT_NAME_EXCHANGE_COMMIT:
        return ExchangeCommit.updatestate(thisTX, stateSnapshot, updateTime, priority, checkOnly);

      default:
    
    }
  
    return new ReturnTuple(SuccessType.FAIL, "Unknown CommitContract Function.");
  
  }
  
}
