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

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_POA_EXERCISES;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaDetail;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaHeader;
import io.setl.bc.pychain.state.tx.poadataclasses.PoaItem;
import io.setl.common.CommonPy.SuccessType;
import io.setl.common.CommonPy.TxType;

@SuppressWarnings("squid:S1118") // Suppress private constructor warning.
public class UpdateStateUtils {

  public static class ReturnTuple {

    /** Standard pass indicator. */
    public static final ReturnTuple PASS = new ReturnTuple(SuccessType.PASS, "");

    /** Standard pass indicator for check only. */
    public static final ReturnTuple PASS_CHECKS = new ReturnTuple(SuccessType.PASS, "Check only");


    /** Get the standard pass for a check or an apply. */
    public static ReturnTuple pass(boolean checkOnly) {
      return checkOnly ? PASS_CHECKS : PASS;
    }


    @JsonIgnore
    public final Object returnObject;

    public final String status;

    public final SuccessType success;


    /**
     * ReturnTuple Constructor.
     *
     * @param success :
     * @param status  :
     */
    @JsonCreator
    public ReturnTuple(@JsonProperty("success") SuccessType success, @JsonProperty("status") String status) {
      this.success = success;
      this.status = status;
      returnObject = null;
    }


    /**
     * ReturnTuple Constructor.
     *
     * @param success      :
     * @param status       :
     * @param returnObject :
     */
    public ReturnTuple(SuccessType success, String status, Object returnObject) {
      this.success = success;
      this.status = status;
      this.returnObject = returnObject;
    }

  }


  /**
   * checkAddressPermissions.
   * Check Address permissions for both the Attorney address and the POA Address.
   *
   * @param stateSnapshot              :
   * @param attorneyAddress            :
   * @param txID                       :
   * @param requiredAddressPermissions :
   *
   * @return :
   */
  public static ReturnTuple checkAddressPermissions(StateSnapshot stateSnapshot, String attorneyAddress, TxType txID, long requiredAddressPermissions) {

    long keyPermission = stateSnapshot.getAddressPermissions(attorneyAddress);
    boolean hasPermission = stateSnapshot.canUseTx(attorneyAddress, txID);
    if ((!hasPermission) && ((keyPermission & requiredAddressPermissions) == 0)) {
      return new ReturnTuple(SuccessType.FAIL, "Inadequate Address permissioning");
    }

    return new ReturnTuple(SuccessType.PASS, "");
  }


  /**
   * checkPoaAddressPermissions.
   * Check Address permissions for both the Attorney address and the POA Address.
   *
   * @param stateSnapshot                 :
   * @param attorneyAddress               :
   * @param poaAddress                    :
   * @param poaTxId                       :
   * @param effectiveTxID                 :
   * @param requiredPoaAddressPermissions :
   *
   * @return :
   */
  public static ReturnTuple checkPoaAddressPermissions(
      StateSnapshot stateSnapshot, String attorneyAddress, String poaAddress, TxType poaTxId,
      TxType effectiveTxID, long requiredPoaAddressPermissions
  ) {

    // Address permissions.
    long keyPermission = stateSnapshot.getAddressPermissions(attorneyAddress);
    boolean hasPermission = stateSnapshot.canUseTx(attorneyAddress, poaTxId);
    if ((!hasPermission) && ((keyPermission & AP_POA_EXERCISES) == 0)) {
      return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("No POA_Exercise permission for Attorney Address {0}", attorneyAddress));
    }

    // poaAddress permissions.
    if (!poaAddress.equalsIgnoreCase(attorneyAddress)) {
      keyPermission = stateSnapshot.getAddressPermissions(poaAddress);
    }
    hasPermission = stateSnapshot.canUseTx(poaAddress, effectiveTxID);
    if ((!hasPermission) && ((keyPermission & requiredPoaAddressPermissions) == 0)) {
      return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("No Tx permission for POA Address {0}", poaAddress));
    }

    return new ReturnTuple(SuccessType.PASS, "");
  }


  /**
   * checkPoaDetail. Check the given requirements against the given entry,
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
  public static ReturnTuple checkPoaDetail(
      String poaReference, PoaDetail thisDetail, String attorneyAddress,
      String poaAddress, TxType effectiveTxID, String[] assetIDs, Number amount
  ) {
    /*
     * Returns matching poaItem where TxID and amount are fulfilled and all assetIDs are present. */

    // This TX Author is authorised by the POA ?
    if (!attorneyAddress.equalsIgnoreCase(thisDetail.getAttorneyAddress())) {
      return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("This POA `{0}` is not for this attorney address {1}", poaReference, attorneyAddress));
    }

    // Get POA item for this effective TX.
    List<PoaItem> itemList = thisDetail.getItem(effectiveTxID);
    //PoaItem thisItem = thisDetail.getItem(effectiveTxID);
    if ((itemList == null) || (itemList.isEmpty())) {
      return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("No POA item for this effective TX Type : {0}", effectiveTxID));
    }

    // Check Namespace matches.
    boolean noAllowance = false;

    if ((assetIDs != null) && (assetIDs.length > 0)) {

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
            return new ReturnTuple(SuccessType.PASS, "", thisPoaItem);
          }
        }
      }

    } else {
      return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Can not match empty Asset ID to poa item `{0}`", poaReference));
    }

    if (noAllowance) {
      // No remaining allowance.
      return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("No remaining allowance for {0} in PoA `{1}`", Arrays.toString(assetIDs), poaReference));
    }

    return new ReturnTuple(SuccessType.FAIL, MessageFormat.format("Asset ID(s) `{0}` do(es) not match poa item `{1}`", Arrays.toString(assetIDs),
        poaReference
    ));

  }


  /**
   * checkPoaTransactionPermissions().
   * Check POA Permissons for the given Address under the given circumstances.
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
   * @return :
   */
  @SuppressWarnings({"squid:S00107", "squid:S3776"}) // Suppress parameter count and cognitive complexity warning.
  public static ReturnTuple checkPoaTransactionPermissions(
      StateSnapshot stateSnapshot, long updateTime, String poaReference, String attorneyAddress,
      String poaAddress, TxType effectiveTxID, String[] assetIDs, Number amount, boolean checkOnly
  ) {

    ReturnTuple rVal = getPoaDetailEntry(stateSnapshot, updateTime, poaAddress, poaReference, checkOnly);

    if (rVal.success != SuccessType.PASS) {
      // Fail or Warn.
      return rVal;
    }

    PoaDetail thisDetail = (PoaDetail) rVal.returnObject;

    return checkPoaDetail(poaReference, thisDetail, attorneyAddress, poaAddress, effectiveTxID, assetIDs, amount);

  }


  /**
   * checkPoaTransactionPermissions().
   * Check POA Permissons for the given Address under the given circumstances.
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
   * @return :
   */
  @SuppressWarnings("squid:S00107") // Suppress parameter count warning.
  public static ReturnTuple checkPoaTransactionPermissions(
      StateSnapshot stateSnapshot, long updateTime, String poaReference, String attorneyAddress,
      String poaAddress, TxType effectiveTxID, String assetID, Number amount, boolean checkOnly
  ) {

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
  public static ReturnTuple getPoaDetailEntry(StateSnapshot stateSnapshot, long updateTime, String poaAddress, String poaReference, boolean checkOnly) {

    // Check POA Permission.

    MutableMerkle<PoaEntry> poaList = stateSnapshot.getPowerOfAttorneys();
    PoaEntry addressPOA = poaList.find(poaAddress);

    if (addressPOA == null) {
      // No POA !
      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("No POAs for POA Address {0}", poaAddress));
    }

    // Get Header Details, check start / finish.
    PoaHeader thisPOA = addressPOA.getReference(poaReference);

    if (thisPOA == null) {
      return new ReturnTuple(
          (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
          MessageFormat.format("POA `{0}` is null for address {1}.", poaReference, poaAddress)
      );

    } else if ((thisPOA.startDate > updateTime) || (thisPOA.expiryDate < updateTime)) {
      return new ReturnTuple(
          (checkOnly ? SuccessType.WARNING : SuccessType.FAIL),
          MessageFormat.format("POA `{0}` for address {1} is not valid at time {2}.", poaReference, poaAddress, updateTime)
      );
    }

    // Check details for Tx Type 'effectiveTxID'

    PoaEntry poaDetailEntry;
    if (checkOnly) {
      poaDetailEntry = poaList.find(addressPOA.getFullReference(poaReference));
    } else {
      poaDetailEntry = poaList.findAndMarkUpdated(addressPOA.getFullReference(poaReference));
    }

    if (poaDetailEntry == null) {
      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("No POA detail entry for reference {0}", poaReference));
    }

    PoaDetail thisDetail = poaDetailEntry.getPoaDetail();
    if (thisDetail == null) {
      return new ReturnTuple((checkOnly ? SuccessType.WARNING : SuccessType.FAIL), MessageFormat.format("No POA detail for reference {0}", poaReference));
    }

    return new ReturnTuple(SuccessType.PASS, "", thisDetail);
  }


  /**
   * tidyPoaReference().
   * `Tidy` a POA Reference, which means remove it if consumed or expired.
   *
   * @param stateSnapshot :
   * @param updateTime    :
   * @param poaReference  :
   * @param poaAddress    :
   */
  @SuppressWarnings({"squid:S3776", "squid:S2159"}) // Suppress cognitive complexity warning.
  public static void tidyPoaReference(StateSnapshot stateSnapshot, long updateTime, String poaReference, String poaAddress) {

    // Check POA Permission.

    MutableMerkle<PoaEntry> poaList = stateSnapshot.getPowerOfAttorneys();
    PoaEntry addressPOA = poaList.findAndMarkUpdated(poaAddress);

    if (addressPOA == null) {
      // No POA !
      return;
    }

    // Get Header Details, check start / finish.
    PoaHeader thisPOA = addressPOA.getReference(poaReference);

    if (thisPOA != null) {
      if (thisPOA.expiryDate < updateTime) {
        // Expired :-

        // Delete Header element.
        addressPOA.removeReference(poaReference);

        // Delete POA Entry.
        poaList.delete(addressPOA.getFullReference(poaReference));

      } else {

        // Not Expired

        PoaEntry poaDetailEntry = poaList.find(addressPOA.getFullReference(poaReference));

        if (poaDetailEntry == null) {
          // No Detail entry, delete header reference
          addressPOA.removeReference(poaReference);

        } else {
          PoaDetail thisDetail = poaDetailEntry.getPoaDetail();

          if (thisDetail == null) {
            // No Detail, delete this entry.

            // Delete POA Entry.
            poaList.delete(addressPOA.getFullReference(poaReference));
            addressPOA.removeReference(poaReference);
          } else {
            // OK, detail exists...

            if (thisDetail.getSumAmounts().equalTo(0L)) {
              // If all consumed, delete :

              poaList.delete(addressPOA.getFullReference(poaReference));
              addressPOA.removeReference(poaReference);
            }
          }
        } // poaDetailEntry == null
      } // expiry date
    } // thisPOA != null

    if (addressPOA.getReferenceCount() == 0) {
      poaList.delete(poaAddress);
    }
  }


}
