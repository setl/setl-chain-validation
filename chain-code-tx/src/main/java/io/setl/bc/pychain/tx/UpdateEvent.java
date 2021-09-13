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
package io.setl.bc.pychain.tx;

import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_DVP_UK;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_EXCHANGE;
import static io.setl.common.CommonPy.ContractConstants.CONTRACT_NAME_TOKENS_NOMINATE;

import java.text.MessageFormat;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.EventData;
import io.setl.bc.pychain.state.tx.contractdataclasses.IContractData;
import io.setl.bc.pychain.tx.updateevent.DVP;
import io.setl.bc.pychain.tx.updateevent.Exchange;
import io.setl.bc.pychain.tx.updateevent.TokensNominate;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;
import io.setl.util.CollectionUtils;

public class UpdateEvent {

  private static final Logger logger = LoggerFactory.getLogger(UpdateEvent.class);


  /**
   * Convert contract data to a human friendly YAML document for logging.
   *
   * @param contractData the contract data
   *
   * @return the pretty printed equivalent
   */
  public static String prettyContractData(IContractData contractData) {

    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setAllowReadOnlyProperties(true);
    dumperOptions.setIndent(2);
    dumperOptions.setPrettyFlow(true);
    Yaml yaml = new Yaml(dumperOptions);
    JSONObject json = CollectionUtils.order(contractData.toFriendlyJSON());
    return yaml.dump(json);
  }


  /**
   * updateEvent().
   *
   * @param stateSnapshot   : State handle
   * @param contractAddress :
   * @param updateTime      : Time of Update (Block time)
   *
   * @return :
   */
  public static ReturnTuple updateEvent(
      StateSnapshot stateSnapshot, String contractAddress,
      EventData eventData, long updateTime, boolean checkOnly
  ) {

    /*
    updateEvent() MUST (And I mean *MUST* return 'True' unless it is a CRITICAL error.
    Code in Node.finaliseTransactionsAndIncrementState() looks like this :

    if (!transactionProcessor.postProcessTransactions(s1, block, block.getTimeStamp())) {
      logger.error("Post process transactions failed");
      throw new RuntimeException("postProcess:Failed");
    }

    Which can not be a good thing !

    -----------------------------------------------------

    def dvp_uk_updateevent(thisStateView, updateTimestamp, contractAddress, thisContract, eventData):

     thisStateView   : StateView Object
     updateTimestamp : 'Block' time
     contractAddress :
     thisContract    : ContractData from contracts tree.

     eventData :

        Possible Event details

        ["time",       <int ReferenceTime>]
        ["commit",     <not Used>]
        ["cancel",     TX Id]
        ["status",     <not used>]

     */

    ContractEntry thisContract = stateSnapshot.getContracts().find(contractAddress);
    if (thisContract == null) {
      if (logger.isWarnEnabled()) {
        logger.warn("Update Event : Contract {} not found.", contractAddress);
      }
      return new ReturnTuple(
          (checkOnly ? SuccessType.WARNING : SuccessType.PASS),
          MessageFormat.format("Update Event : Contract {0} not found.", contractAddress)
      );
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Commencing processing event on contract {}:\n{}", contractAddress, prettyContractData(thisContract.getContractData()));
    }

    ReturnTuple rVal = null;

    try {
      switch (thisContract.getFunction().toLowerCase()) {

        case CONTRACT_NAME_DVP_UK:
          StateSnapshot wrapper = stateSnapshot.createSnapshot();
          try {
            rVal = DVP.updateEvent(wrapper, updateTime, contractAddress, eventData, checkOnly);
          } finally {
            // on success, commit to parent snapshot
            if ((!checkOnly) && (rVal != null) && (rVal.success == SuccessType.PASS)) {

              String m = wrapper.commitIfNotCorrupt();
              if (m != null) {
                // Sonarqube : Don't return from finally, thus set new rVal value.
                rVal = new ReturnTuple(SuccessType.FAIL, m);
              }
            }
          }

          break;

        case CONTRACT_NAME_TOKENS_NOMINATE:

          rVal = TokensNominate.updateEvent(stateSnapshot, updateTime, contractAddress, eventData, checkOnly);

          break;

        case CONTRACT_NAME_EXCHANGE:

          rVal = Exchange.updateEvent(stateSnapshot, updateTime, contractAddress, eventData, checkOnly);

          break;

        default:
          throw new NotImplementedException(
              MessageFormat.format("UpdateEvent for unknown Contract type `{0}` on contract {1} returned null.", thisContract.getFunction(), contractAddress));

      } // Switch

      if (rVal == null) {
        rVal = new ReturnTuple(
            (checkOnly ? SuccessType.WARNING : SuccessType.PASS),
            MessageFormat.format("UpdateEvent for Contract type `{0}` on contract {1} returned null.", thisContract.getFunction(), contractAddress)
        );
      }

    } finally {
      if (logger.isDebugEnabled()) {
        logger.debug("Finished processing event on contract {}:\n{}", contractAddress, prettyContractData(thisContract.getContractData()));
      }

      if (rVal != null) {
        if (rVal.success == SuccessType.FAIL) {
          if (logger.isWarnEnabled()) {
            logger.warn(MessageFormat.format("UpdateEvent : Failed contract {0} : status {1}", contractAddress, rVal.status));
          }
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format("UpdateEvent `{0}` : contract {1} : status {2}", eventData.getEventName(), contractAddress,
                rVal.status
            ));
          }
        }
      } else {
        // God only knows how rVal could be null at this point, but checkstyle would not let it lie...
        rVal = new ReturnTuple(
            (checkOnly ? SuccessType.WARNING : SuccessType.PASS),
            MessageFormat.format("UpdateEvent for Contract type `{0}` on contract {1} returned null.", thisContract.getFunction(), contractAddress)
        );
      }

    } // Try-Finally

    return rVal;

  } // updateEvent()

}
