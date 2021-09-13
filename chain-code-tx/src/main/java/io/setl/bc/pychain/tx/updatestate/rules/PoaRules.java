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
package io.setl.bc.pychain.tx.updatestate.rules;

import static io.setl.bc.pychain.tx.updatestate.TxProcessor.fail;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_POA_EXERCISES;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.common.CommonPy.TxType;

/**
 * Standard PoA related rules for transactions.
 *
 * @author Simon Greatrix on 20/01/2020.
 */
public class PoaRules {


  /**
   * Check Address permissions for both the Attorney address and the POA Address.
   *
   * @param stateSnapshot                 :
   * @param attorneyAddress               :
   * @param poaAddress                    :
   * @param poaTxId                       :
   * @param effectiveTxID                 :
   * @param requiredPoaAddressPermissions :
   *
   * @throws TxFailedException if permissions are bad
   */
  public static void checkPoaAddressPermissions(
      StateSnapshot stateSnapshot, String attorneyAddress, String poaAddress, TxType poaTxId,
      TxType effectiveTxID, long requiredPoaAddressPermissions
  ) throws TxFailedException {
    // Only applicable if on-chain permissions are used
    if (!stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
      return;
    }

    // Address permissions.
    long keyPermission = stateSnapshot.getAddressPermissions(attorneyAddress);
    boolean hasPermission = stateSnapshot.canUseTx(attorneyAddress, poaTxId);

    if ((!hasPermission) && ((keyPermission & AP_POA_EXERCISES) == 0)) {
      throw fail("No POA_Exercise permission for Attorney Address {0}", attorneyAddress);
    }

    // poaAddress permissions.
    if (!poaAddress.equalsIgnoreCase(attorneyAddress)) {
      keyPermission = stateSnapshot.getAddressPermissions(poaAddress);
    }

    hasPermission = stateSnapshot.canUseTx(poaAddress, effectiveTxID);
    if ((!hasPermission) && ((keyPermission & requiredPoaAddressPermissions) == 0)) {
      throw fail("No Tx permission for POA Address {0}", poaAddress);
    }
  }


  /**
   * Check the given requirements against the given entry.
   * <p>Returns matching poaItem where TxID and amount are fulfilled and all assetIDs are present</p>
   *
   * @param poaReference    : For Error reporting only.
   * @param thisDetail      : Detail item to check against.
   * @param attorneyAddress :
   * @param poaAddress      :
   * @param effectiveTxID   :
   * @param assetIDs        :
   * @param amount          :
   *
   * @return :
   */
  public static PoaItem checkPoaDetail(
      String poaReference,
      PoaDetail thisDetail,
      String attorneyAddress,
      String poaAddress,
      TxType effectiveTxID,
      String[] assetIDs,
      Number amount
  ) throws TxFailedException {
    if (assetIDs == null || assetIDs.length == 0) {
      throw new TxFailedException(MessageFormat.format("Can not match empty Asset ID to poa item `{0}`", poaReference));
    }
    // This TX Author is authorised by the POA ?
    if (!attorneyAddress.equals(thisDetail.getAttorneyAddress())) {
      throw new TxFailedException(MessageFormat.format("This POA `{0}` is not for this attorney address {1}", poaReference, attorneyAddress));
    }

    // Get POA item for this effective TX.
    List<PoaItem> itemList = thisDetail.getItem(effectiveTxID);
    if ((itemList == null) || (itemList.isEmpty())) {
      throw new TxFailedException(MessageFormat.format("No POA item for this effective TX Type : {0}", effectiveTxID));
    }

    // Check Namespace matches.
    boolean noAllowance = false;
    // For each possible poaItem...
    for (PoaItem thisPoaItem : itemList) {
      boolean noMatch = false;
      // See if it matched all required assets...
      for (String thisAssetID : assetIDs) {
        if ((thisAssetID != null) && (!thisAssetID.isEmpty()) && (!thisPoaItem.matchAsset(thisAssetID))) {
          noMatch = true;
          break;
        }
      }

      // Return if it matches OK.
      if (!noMatch) {
        if (thisPoaItem.getAmount().lessThan(amount)) {
          noAllowance = true;
        } else {
          return thisPoaItem;
        }
      }
    }

    if (noAllowance) {
      // No remaining allowance.
      throw fail("No remaining allowance for {0} in PoA `{1}`", Arrays.toString(assetIDs), poaReference);
    }

    throw fail("Asset ID(s) `{0}` do(es) not match poa item `{1}`", Arrays.toString(assetIDs), poaReference);
  }


  /**
   * Verify POA Permissions for the given Address under the given circumstances.
   *
   * @param stateSnapshot   :
   * @param updateTime      : Block update time
   * @param poaReference    : POA Reference to check
   * @param attorneyAddress : Attorney Address (Address using POA)
   * @param poaAddress      : POA Address (Address that granted POA)
   * @param effectiveTxID   : TxID for the underlying Transaction being applied
   * @param assetIDs        : String[] Relevant Asset IDs
   * @param amount          : `Amount` of POA required
   * @param checkOnly       :
   *
   * @return the relevant PoA item
   */
  @SuppressWarnings({"squid:S00107", "squid:S3776"}) // Suppress parameter count and cognitive complexity warning.
  public static PoaItem checkPoaTransactionPermissions(
      StateSnapshot stateSnapshot, long updateTime, String poaReference, String attorneyAddress,
      String poaAddress, TxType effectiveTxID, String[] assetIDs, Number amount, boolean checkOnly
  ) throws TxFailedException {
    PoaDetail thisDetail = getPoaDetailEntry(stateSnapshot, updateTime, poaAddress, poaReference, checkOnly);
    return checkPoaDetail(poaReference, thisDetail, attorneyAddress, poaAddress, effectiveTxID, assetIDs, amount);
  }


  /**
   * Verify POA Permissions for the given Address under the given circumstances.
   *
   * @param stateSnapshot   :
   * @param updateTime      : Block update time
   * @param poaReference    : POA Reference to check
   * @param attorneyAddress : Attorney Address (Address using POA)
   * @param poaAddress      : POA Address (Address that granted POA)
   * @param effectiveTxID   : TxID for the underlying Transaction being applied
   * @param assetID         : Relevant Asset ID
   * @param amount          : `Amount` of POA required
   * @param checkOnly       :
   *
   * @return the relevant PoA item
   */
  @SuppressWarnings("squid:S00107") // Suppress parameter count warning.
  public static PoaItem checkPoaTransactionPermissions(
      StateSnapshot stateSnapshot, long updateTime, String poaReference, String attorneyAddress,
      String poaAddress, TxType effectiveTxID, String assetID, Number amount, boolean checkOnly
  ) throws TxFailedException {
    return checkPoaTransactionPermissions(stateSnapshot, updateTime, poaReference, attorneyAddress, poaAddress, effectiveTxID, new String[]{assetID}, amount,
        checkOnly
    );
  }


  /**
   * getPoaDetailEntry. Get PoaDetailEntry relating to the given terms.
   *
   * @param stateSnapshot :
   * @param updateTime    :
   * @param poaAddress    :
   * @param poaReference  :
   * @param checkOnly     :
   *
   * @return :
   */
  public static PoaDetail getPoaDetailEntry(StateSnapshot stateSnapshot, long updateTime, String poaAddress, String poaReference, boolean checkOnly)
      throws TxFailedException {
    // Check POA Permission.
    MutableMerkle<PoaEntry> poaList = stateSnapshot.getPowerOfAttorneys();
    PoaEntry addressPOA = poaList.find(poaAddress);

    if (addressPOA == null) {
      // No POA !
      throw fail("No POAs for POA Address {0}", poaAddress);
    }

    // Get Header Details, check start / finish.
    PoaHeader thisPOA = addressPOA.getReference(poaReference);

    if (thisPOA == null) {
      throw fail("POA `{0}` is null for address {1}.", poaReference, poaAddress);
    }
    if ((thisPOA.startDate > updateTime) || (thisPOA.expiryDate < updateTime)) {
      throw fail("POA `{0}` for address {1} is not valid at time {2}.", poaReference, poaAddress, updateTime);
    }

    // Check details for Tx Type 'effectiveTxID'
    PoaEntry poaDetailEntry;
    if (checkOnly) {
      poaDetailEntry = poaList.find(addressPOA.getFullReference(poaReference));
    } else {
      poaDetailEntry = poaList.findAndMarkUpdated(addressPOA.getFullReference(poaReference));
    }

    if (poaDetailEntry == null) {
      throw fail("No POA detail entry for reference {0}", poaReference);
    }

    PoaDetail thisDetail = poaDetailEntry.getPoaDetail();
    if (thisDetail == null) {
      throw fail("No POA detail for reference {0}", poaReference);
    }
    return thisDetail;
  }

}
