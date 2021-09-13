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

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;

import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.tx.contractdataclasses.ContractLifeCycle;
import io.setl.bc.pychain.state.tx.contractdataclasses.ExchangeContractData;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;
import java.io.IOException;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exchange {

  private static final Logger logger = LoggerFactory.getLogger(Exchange.class);


  /**
   * updateEvent().
   * <p>Process time event for the Tokens Nominate contract.</p>
   *
   * @param stateSnapshot   :
   * @param updateTime      :
   * @param contractAddress :
   * @param eventData       :
   *
   * @return :
   * @throws IOException :
   */
  public static ReturnTuple updateEvent(StateSnapshot stateSnapshot, long updateTime, String contractAddress, EventData eventData, boolean checkOnly) {

    ContractEntry contractEntry = stateSnapshot.getContracts().findAndMarkUpdated(contractAddress);

    if (contractEntry == null) {

      if ("time".equalsIgnoreCase(eventData.getEventName())) {
        // Just a 'Ghost' time update, ignore.
        return new ReturnTuple(SuccessType.PASS, "Ghost time update.");
      }

      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.PASS), MessageFormat
          .format("TokensNominate updateEvent() contractEntry == null, eventType = {0}. Address : {1}", eventData.getEventName(), contractAddress));
    }

    ExchangeContractData contractData = (ExchangeContractData) contractEntry.getContractData();

    if (contractData == null) {
      logger.error("TokensNominate event, ContractData missing for contract {}", contractAddress);
      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.PASS),
          MessageFormat.format("ContractData missing for contract {0}", contractAddress));
    }

    if (!CONTRACT_NAME_EXCHANGE.equalsIgnoreCase(contractData.get__function())) {
      logger.error("TokensNominate updateEvent() fed contract of type : {}. Address : {}", contractEntry.getContractData().getContractType(), contractAddress);

      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.PASS),
          MessageFormat.format("TokensNominate updateEvent() fed contract of type : {0}. Address : {1}", contractEntry.getContractData().getContractType(),
              contractAddress));
    }

    if (!contractAddress.equalsIgnoreCase(contractData.get__address())) {
      logger.error("Contract address mismatch in contract {}", contractAddress);
      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.PASS),
          MessageFormat.format("Contract address mismatch in contract {0}.", contractAddress));
    }

    //

    switch (eventData.getEventName().toLowerCase()) {

      case "time":

        long referenceTime = eventData.getLongValue();

        if (referenceTime >= contractData.get__timeevent()) {

          if (referenceTime >= contractData.getExpiry()) {
            // Expiry
            // In production, this should trigger some external notification.

            logger.info("Expiry of contract : {}", contractAddress);

            // Simple Delete

            if (!checkOnly) {
              stateSnapshot.addLifeCycleEvent(ContractLifeCycle.EXPIRE, contractAddress, contractData.addresses());
              stateSnapshot.addLifeCycleEvent(ContractLifeCycle.DELETE, contractAddress, contractData.addresses());
              stateSnapshot.getContracts().delete(contractAddress);
            }
          }

        } else {
          // The Exchange contract should only have one time event, at expiry. Odd that this code should get run at all.
          // Set expiry time event
          if (!checkOnly) {
            contractData.setNextTimeEvent(referenceTime, false);
            stateSnapshot.addContractEventTime(contractAddress, contractData.get__timeevent());
          }
        }

        break;

      default:

        break;

    }

    return new ReturnTuple(SuccessType.PASS, (checkOnly ? "Check Only." : ""));

  }
}
