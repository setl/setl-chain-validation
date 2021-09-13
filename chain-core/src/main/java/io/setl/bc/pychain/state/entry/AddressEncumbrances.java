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
package io.setl.bc.pychain.state.entry;

import static io.setl.common.Balance.BALANCE_ZERO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.common.EncumbranceDetail;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.serialise.SafeMsgPackable;
import io.setl.common.Balance;
import io.setl.common.MutableInt;

// TODO removeSpecificEncumbrance...



/**
 * AddressEncumbrances.
 * <p>This class encapsulates all encumbrances stored at (related to, beholden upon) a particular address</p>
 * <p>Each encumbrance relates to a particular Asset, thus Encumbrances are held in a Map, the key being the
 * full AssetID and the Value being a collection of all encumbrances from this address relating to that Asset,
 * (held in an instance of AssetEncumbrances) </p>
 * <p> the AssetEncumbrances object holds a list of EncumbranceEntry Objects each of which will have a reference
 * name, an encumbrance amount and a list of beneficiaries and administrators to this encumbrance entry.</p>
 * <p>The beneficiaries and administrators are each a list of EncumbranceDetail Objects. Each detail object
 * specifies an address and a start and end time (UTC Unix epoch seconds). The time range defines the period
 * during which the beneficiary may exercise the encumbrance or the administrator may exercise or delete.</p>
 * <p> It is possible to have multiple entries with the same reference for a given Address / Asset, however it
 * is constrained such as they MUST have identical Administrator and Beneficiary details and dates.
 * In much of the code, encumbrance quantities are calculated without reference to Admins / Beneficiaries or dates
 * this is because the isAdministrator and isBeneficiary checks are usually done separately and if these checks
 * succeed, then the quantity of encumbrance (for a given Address / Asset / Reference) will be everything.
 * </p>
 * <pre>
 * # encumbrance data :
 * #
 * # At AddressEncumbrances level :
 * #
 * #      TreeMap&lt;String AssetId, AssetEncumbrances&gt;
 * #
 * # AssetEncumbrances contains List&lt;EncumbranceEntry&gt; specific to the Asset ID key in AddressEncumbrances Map
 * #      AssetEncumbrances maintains a totalAmount and expiryDate for convenience, based upon details in the
 * #      Listed EncumbranceEntries.
 * #
 * # EncumbranceEntry contains
 * #      String reference
 * #      Balance amount
 * #      long expiryDate
 * #      int priority
 * #      List&lt;EncumbranceDetail&gt; administrators
 * #      List&lt;EncumbranceDetail&gt; beneficiaries
 * #
 * # EncumbranceDetail contains
 * #      String address
 * #      long startTime
 * #      long endTime
 * #
 * # So :
 * #
 * # MerkleTree (thisAddress : Map(fullassetid ,[[total, expiry, [EncumbranceEntry], [EncumbranceEntry]], ...]))
 * # EncumbranceEntry : {reference, amount, expiryDate, priority, [beneficiaries], [administrators]}
 * # beneficiaries : [{Address, StartDate, EndDate}, ...]
 * # administrators : [{Address, StartDate, EndDate}, ...]
 * </pre>
 */
public class AddressEncumbrances implements MEntry, SafeMsgPackable {

  private static final Logger logger = LoggerFactory.getLogger(AddressEncumbrances.class);



  /**
   * Class AssetEncumbrances.
   * <p> the AssetEncumbrances object holds a list of EncumbranceEntry Objects each of which will have a reference
   * name, an encumbrance amount and a list of beneficiaries and administrators to this encumbrance entry.</p>
   * <p>The beneficiaries and administrators are each a list of EncumbranceDetail Objects. Each detail object
   * specifies an address and a start and end time (UTC Unix epoch seconds). The time range defines the period
   * during which the beneficiary may benefit from the encumbrance or the administrator may exercise or delete.</p>
   * <p>A beneficiary may not exercise an encumbrance unless they are an administrator also.</p>
   * <p>There exists the concept of an Encumbrance priority.
   * Priority is important when Encumbrances exist for more asset
   * than is owned, there needs to be a way to determine who gets the asset and who does not. In practice it is important the all
   * encumbrances do not rank equally otherwise an asset owner could just encumber to another Address and move all their asset making encumbrances meaningless.
   * At present, Encumbrances are prioritised in the order in which they are
   * added, there are internal fields that would enable specific priorities to be specified, but this functionality is neither
   * developed or exposed. At present the priority is re-calculated each time an EncumbranceEntry is added or deleted with
   * EncumbranceEntries being held in the AssetEncumbrances List in priority order for simplicity.</p>
   * <p>The only exception is, optionally, when an Encumbrance is created as a result of the execution of a contract, if an
   * asset is sent to an address with an accompanying encumbrance it was considered that that encumbrance should take priority,
   * thus, this encumbrance may be specified as high priority and will be appended to the front of the AssetEncumbrances List.</p>
   * <p>Encumbrance Entries must be maintained using the methods on the AssetEncumbrances class :
   * addEncumbrance      : Add an additional Encumbrance with the option to replace or extend an existing reference.
   * consumeEncumbrance  : Reduce an encumbrance balance consuming from the front.
   * That is, if only part of an encumbrance covered by an asset balance then the part that is covered will be reduced.
   * The idea is if an encumbrance of 100 is only covered by 40 of asset (leaving 60 uncovered) and 20 is 'exercised' (consumed) the
   * resulting situation is 20 covered and 60 uncovered.
   * reduceEncumbrance   : Reduce an encumbrance balance consuming from the back.
   * That is, if only part of an encumbrance covered by an asset balance then the part that is not covered will be reduced.
   * The idea is if an encumbrance of 100 is only covered by 40 of asset (leaving 60 uncovered) and 20 is un-Encumbered (reduced) the
   * resulting situation is 40 covered and 40 uncovered.
   * For an encumbrance fully covered by an asset holding, there is no functional difference between `consume` and `reduce`.
   * The AssetEncumbrances class also has methods for checking encumbrance validity and coverage (given an asset balance).
   * An expired Encumbrance may be deleted by anyone.
   * </p>
   */
  public static class AssetEncumbrances implements SafeMsgPackable {
    // [[total], [encumbrance], [encumbrance], ...]


    private final List<EncumbranceEntry> encumbrances;

    private long expiryDate;

    private Balance totalAmount;


    /**
     * AssetEncumbrances.
     * <p>Default Constructor.</p>
     */
    public AssetEncumbrances() {
      totalAmount = BALANCE_ZERO;
      encumbrances = new ArrayList<>();
      expiryDate = 0L;
    }


    /**
     * New instance from packed form.
     *
     * @param unpacker access to packed form
     */
    public AssetEncumbrances(MessageUnpacker unpacker) throws IOException {
      int s = unpacker.unpackArrayHeader();
      encumbrances = new ArrayList<>(s);
      expiryDate = Long.MIN_VALUE;

      for (int i = 0; i < s; i++) {
        EncumbranceEntry e = new EncumbranceEntry(unpacker);
        encumbrances.add(e);
        expiryDate = Math.max(expiryDate, e.expiryDate);
      }

      recalculateTotal();
    }


    /**
     * AssetEncumbrances().
     *
     * @param encodedEncumbrances :
     */
    public AssetEncumbrances(Object[] encodedEncumbrances) {
      this(new MPWrappedArrayImpl(encodedEncumbrances));
    }


    /**
     * AssetEncumbrances().
     *
     * @param encodedEncumbrances :
     */
    public AssetEncumbrances(MPWrappedArray encodedEncumbrances) {
      this();

      if (encodedEncumbrances != null) {
        int arrayLen = encodedEncumbrances.size();

        totalAmount = new Balance(arrayLen > 0 ? encodedEncumbrances.asWrapped(0).asLong(0) : 0L);
        expiryDate = Long.MIN_VALUE;
        EncumbranceEntry thisEntry;

        for (int index = 1; index < arrayLen; index++) {
          thisEntry = new EncumbranceEntry(encodedEncumbrances.asWrapped(index));
          encumbrances.add(thisEntry);
          expiryDate = Math.max(expiryDate, thisEntry.expiryDate);
        }

      }

    }


    /**
     * AssetEncumbrances().
     * <p>Constructor : </p>
     *
     * @param totalAmount  :
     * @param encumbrances :
     */
    public AssetEncumbrances(Number totalAmount, List<EncumbranceEntry> encumbrances) {
      this.totalAmount = new Balance(totalAmount);
      this.encumbrances = encumbrances;
    }


    /**
     * AssetEncumbrances().
     * <p>Copy Constructor.</p>
     *
     * @param toCopy :
     */
    public AssetEncumbrances(AssetEncumbrances toCopy) {
      this();

      if (toCopy != null) {
        totalAmount = toCopy.totalAmount;

        toCopy.encumbrances.forEach(detail -> encumbrances.add(new EncumbranceEntry(detail)));

        expiryDate = toCopy.expiryDate;
      }
    }


    /**
     * Create a list of encumbrances from the entries.
     *
     * @param entries the entries
     */
    @JsonCreator
    public AssetEncumbrances(List<EncumbranceEntry> entries) {
      if (entries != null) {
        encumbrances = new ArrayList<>(entries.size());
        expiryDate = Long.MIN_VALUE;

        for (EncumbranceEntry e : entries) {
          if (e != null) {
            encumbrances.add(e);
            expiryDate = Math.max(expiryDate, e.expiryDate);
          }
        }

        recalculateTotal();
      } else {
        totalAmount = BALANCE_ZERO;
        encumbrances = new ArrayList<>();
        expiryDate = 0L;
      }
    }


    /**
     * addEncumbrance().
     * <p>Add the given encumbrance to this AssetEncumbrance set.
     * If 'cumulative', administrators and beneficiaries are taken from existing Encumbrances.</p>
     *
     * @param newEncumbrance : New Encumbrance
     * @param assetBalance   : Not used
     * @param timeNow        : Not used
     * @param cumulative     : Cumulative : Overwrite or suppliment existing Encumbrance
     * @param highPriority   : If true, add to head of encumbrance list.
     *
     * @return true if encumbrance was added, false if it was not
     */
    public boolean addEncumbrance(EncumbranceEntry newEncumbrance, Number assetBalance, long timeNow, boolean cumulative, boolean highPriority) {
      if ((newEncumbrance == null) || (newEncumbrance.amount.lessThanEqualZero())) {
        return false;
      }

      String newReference = newEncumbrance.reference;
      EncumbranceEntry newEncumbranceEntry = new EncumbranceEntry(newEncumbrance);

      if (newReference == null) {
        return false;
      }

      // Verify the new entry is OK if the reference is already in use
      EncumbranceEntry existing = getAnyByReference(newReference);
      if (existing != null) {
        if (cumulative) {
          // Cumulative entries must match the existing beneficiaries and administrators
          if (!existing.canAccumulate(newEncumbranceEntry)) {
            return false;
          }

        } else {
          // Non-cumulative entries must have unique reference
          return false;
        }
      }

      if (highPriority) {
        newEncumbranceEntry.priority = EncumbranceEntry.HIGH_PRIORITY;
      }

      // Add

      encumbrances.add(newEncumbranceEntry);
      recalcPrioritiesSimple();

      return true;
    }


    /**
     * <p>Return the balance that is available to a given Encumbrance.
     * Algorithm assumes that this.encumbrances is left in a sorted order.
     * Excludes Expired Encumbrances.
     * </p>
     *
     * @param accountBalance : Asset balance against which to check encumbrances
     * @param referenceName  : Encumbrance Reference to check. May be multiple entries.
     * @param timeNow        : Time against which to judge 'expired' status.
     *
     * @return : Amount of asset available to the given encumbrance.
     */
    public Balance availableToEncumbrance(Number accountBalance, String referenceName, long timeNow) {
      Balance rVal = BALANCE_ZERO;  //  Accumulate asset related to this Reference.
      Balance remainingBalance = new Balance(accountBalance); // Running total of account balance.

      // Validate parameters.

      if ((referenceName == null) || ((new Balance(accountBalance)).lessThanZero())) {
        return BALANCE_ZERO;
      }

      // Iterate (already sorted) encumbrance list...
      for (EncumbranceEntry thisEntry : encumbrances) {

        // Ignore expired entries.
        if (!thisEntry.hasExpired(timeNow)) {

          // Break on 'out of asset' condition.
          if (remainingBalance.lessThanEqualZero()) {
            break;
          }

          // Accumulate vs matching Encumbrances.
          if (referenceName.equalsIgnoreCase(thisEntry.reference)) {
            rVal = rVal.add(remainingBalance.compareTo(thisEntry.amount) <= 0 ? remainingBalance : thisEntry.amount);
          }

          // Update remainingasset.
          remainingBalance = remainingBalance.subtract(thisEntry.amount);

        }

      }

      //
      return rVal;
    }


    /**
     * Consumes encumbrance in priority order (highest (lowest number)) first.
     * Remember that all fragments have the same admin / beneficary details.
     * Before Consume is called, the time-relative availability should have been checked,
     * so it is not checked here.
     *
     * @param referenceName :
     * @param consumeAmount :
     *
     * @return :
     */
    public Balance consumeEncumbrance(String referenceName, Number consumeAmount) {
      List<EncumbranceEntry> rVal = getByReference(referenceName);
      rVal.sort(EncumbranceEntry.HIGH_TO_LOW_PRIORITY);
      return encumbranceDeplete(rVal, consumeAmount);
    }


    /**
     * encode().
     * <p>Return Encumbrance entry as Object[].
     * </p>
     *
     * @return :
     */
    public Object[] encode() {

      Object[] rVal = new Object[encumbrances.size() + 1];
      final int[] counter = {1};

      rVal[0] = new Object[]{totalAmount};
      encumbrances.forEach(detail -> rVal[counter[0]++] = detail.encode());

      return rVal;
    }


    private Balance encumbranceDeplete(List<EncumbranceEntry> rVal, Number consumeAmount) {

      Balance thisConsumeAmount = new Balance(consumeAmount);
      thisConsumeAmount = Balance.abs(thisConsumeAmount);

      if (rVal.size() == 1) {
        EncumbranceEntry encumbranceEntry;

        encumbranceEntry = rVal.get(0);

        encumbranceEntry.amount = encumbranceEntry.amount.subtract(Balance.min(encumbranceEntry.amount, thisConsumeAmount));

      } else if (rVal.size() > 1) {
        final Balance[] remainingAmount = {thisConsumeAmount};
        final Balance[] deltaAmount = new Balance[1];

        rVal.forEach(encumbranceEntry -> {
          if (remainingAmount[0].greaterThanZero()) {
            deltaAmount[0] = Balance.min(remainingAmount[0], encumbranceEntry.amount);
            encumbranceEntry.amount = encumbranceEntry.amount.subtract(deltaAmount[0]);
            remainingAmount[0] = remainingAmount[0].subtract(deltaAmount[0]);
          }
        });

      }

      return recalculateTotal();
    }


    public boolean encumbrancesIsEmpty() {

      return encumbrances.isEmpty();
    }


    @SuppressWarnings("squid:CommentedOutCodeLine")
    @Override
    public boolean equals(Object toCompare) {
      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof AssetEncumbrances)) {
        return false;
      }

      AssetEncumbrances theOther = (AssetEncumbrances) toCompare;

      if (!totalAmount.equalTo(theOther.totalAmount)) {
        return false;
      }

      if (encumbrances.size() != theOther.encumbrances.size()) {
        return false;
      }

      for (int index = 0, len = encumbrances.size(); index < len; index++) {
        if (!encumbrances.get(index).equals(theOther.encumbrances.get(index))) {
          return false;
        }
      }

      /*
      This would compare by encumbrance reference (for effective equality) but this could result in a different hash, so not really equal for our purposes.

      for (EncumbranceEntry thisEntry : this.encumbrances) {
        if (!thisEntry.equals(theOther.getByReference(thisEntry.reference))) {
          return false;
        }
      }
      */

      return true;
    }


    public void forEach(Consumer<EncumbranceEntry> consumer) {
      encumbrances.forEach(consumer);
    }


    /**
     * <p>Return an aggregated EncumbranceEntry representing the Asset available under a given Encumbrance, based
     * on the given Address balance.
     * Assumes that the encumbrance entries are sorted in 'priority' order, as they should be.
     * For Zero availability, will return a zero valued Entry, consistent with getAggregateByReference().
     * Does not consider 'Expired' Status, the calling code should check this if necessary.</p>
     *
     * @param referenceName  :
     * @param addressHolding :
     *
     * @return :
     */
    public EncumbranceEntry getAggregateAvailableByReference(String referenceName, Number addressHolding) {
      if ((referenceName == null) || (addressHolding == null)) {
        return null;
      }

      Balance thisAddressHolding = new Balance(addressHolding);
      Balance remainingHolding = Balance.max(BALANCE_ZERO, thisAddressHolding);

      EncumbranceEntry rVal = null;

      // For each Encumbrance Entry
      for (EncumbranceEntry thisEntry : encumbrances) {

        // Does the Reference match ?
        // Remember, there may be multiple entries for a given reference
        if (referenceName.equalsIgnoreCase(thisEntry.reference)) {

          if (rVal == null) {
            // Set return value to this encumbrance amount (validated)
            rVal = new EncumbranceEntry(thisEntry);
            rVal.amount = Balance.min(thisEntry.amount, Balance.max(BALANCE_ZERO, remainingHolding));
          } else {
            // Accumulate this Encumbrance to return value
            // Checks mean that remainingHolding will never be negative at this point.
            rVal.amount = rVal.amount.add(Balance.min(thisEntry.amount, remainingHolding));
          }
        }

        // decrease remaining available account balance
        remainingHolding = remainingHolding.subtract(thisEntry.amount);

        // Break if account is empty. (Don't just break as if there is no matching reference, rVal is Null, if there is no asset rVal is zero.)
        if ((rVal != null) && (remainingHolding.lessThanEqualZero())) {
          break;
        }
      }

      // Return.
      return rVal;
    }


    /**
     * <p>Return an aggregated EncumbranceEntry representing the total Asset encumbered under a given Encumbrance reference.
     * There is no consideration of whether or not the asset is actually available at this address. See getAggregateAvailableByReference().
     * Neither do we consider whether or not this Encumbrance has expired as this is relative to the beneficiary address anyway.
     * The isBeneficiaryValid method is used elsewhere for this purpose.</p>
     *
     * @param referenceName :
     *
     * @return :
     */
    public EncumbranceEntry getAggregateByReference(String referenceName) {
      if (referenceName == null) {
        return null;
      }

      EncumbranceEntry rVal = null;

      for (EncumbranceEntry thisEntry : encumbrances) {
        if (referenceName.equalsIgnoreCase(thisEntry.reference)) {
          if (rVal == null) {
            rVal = new EncumbranceEntry(thisEntry);
          } else {
            rVal.amount = rVal.amount.add(thisEntry.amount);
          }
        }
      }

      return rVal;
    }


    /**
     * Get any entry with a matching reference.
     *
     * @param referenceName : the reference to match
     *
     * @return a matching reference, or null
     */
    public EncumbranceEntry getAnyByReference(String referenceName) {
      if (referenceName == null) {
        return null;
      }
      for (EncumbranceEntry thisEntry : encumbrances) {
        if (referenceName.equalsIgnoreCase(thisEntry.reference)) {
          // got a match
          return thisEntry;
        }
      }

      // no matches
      return null;
    }


    /**
     * getByReference().
     *
     * @param referenceName :
     *
     * @return :
     */
    @Nonnull
    public List<EncumbranceEntry> getByReference(String referenceName) {
      ArrayList<EncumbranceEntry> rVal = new ArrayList<>();

      for (EncumbranceEntry thisEntry : encumbrances) {
        if ((thisEntry.reference != null) && (thisEntry.reference.equalsIgnoreCase(referenceName))) {
          rVal.add(thisEntry);
        }
      }

      return rVal;
    }


    /**
     * getEncumbranceAmountByReference().
     *
     * @param referenceName :
     *
     * @return :
     */
    Balance getEncumbranceAmountByReference(String referenceName) {
      List<EncumbranceEntry> theseOnes = getByReference(referenceName);
      if (theseOnes.isEmpty()) {
        return BALANCE_ZERO;
      }

      final Balance[] value = {BALANCE_ZERO};
      theseOnes.forEach(thisOne -> value[0] = value[0].add(thisOne.amount));
      return value[0];
    }


    @JsonIgnore
    public Balance getEncumbranceTotal() {
      return totalAmount;
    }


    /**
     * getEncumbranceTotal.
     * <p>Return total encumbrance associated with this Asset (context this Address, of course).</p>
     *
     * @param recalc :
     *
     * @return :
     */
    Balance getEncumbranceTotal(boolean recalc) {
      if (recalc) {
        return recalculateTotal();
      } else {
        return getEncumbranceTotal();
      }
    }


    @JsonValue
    public List<EncumbranceEntry> getEncumbrances() {
      return Collections.unmodifiableList(encumbrances);
    }


    /**
     * <p>Return total asset encumbrance, ignoring expired encumbrances (vs the given UTC date/time)</p>.
     *
     * @param nowUTCSecs :
     *
     * @return :
     */
    public Balance getTotalAmount(long nowUTCSecs) {
      Balance amount = BALANCE_ZERO;

      for (EncumbranceEntry thisEntry : encumbrances) {
        if (!thisEntry.hasExpired(nowUTCSecs)) {
          amount = amount.add(thisEntry.amount);
        }
      }

      return amount;
    }


    @Override
    public int hashCode() {
      Balance rVal = totalAmount;

      for (EncumbranceEntry thisEntry : encumbrances) {
        rVal = rVal.add(thisEntry.hashCode());
      }

      return rVal.intValue();
    }


    @Override
    public void pack(MessagePacker p) throws IOException {
      int s = encumbrances.size();
      p.packArrayHeader(s);
      for (int i = 0; i < s; i++) {
        encumbrances.get(i).pack(p);
      }
    }


    /*
    Encumbrances not split, prioritised by insertion order, unless inserted as 'High' priority.
    Multiple pieces may exist for a reference name. Simple consolidation algorithm.
     */
    private void recalcPrioritiesSimple() {

      try {

        int priorityCount = 0;
        EncumbranceEntry lastEncumbrance = null;

        encumbrances.sort(EncumbranceEntry.HIGH_TO_LOW_PRIORITY);

        Iterator<EncumbranceEntry> it = encumbrances.iterator();

        while (it.hasNext()) {

          EncumbranceEntry thisEncumbrance = it.next();

          thisEncumbrance.priority = priorityCount;

          priorityCount += 10; // space out the priorities so we can insert between if desired. (Will change each time, not easy)

          if ((lastEncumbrance != null) && (thisEncumbrance.reference.equalsIgnoreCase(lastEncumbrance.reference))) {
            // Consolidate sequential references with the same name. Assumption is that administrators and beneficiaries are similar. (Is enforced).

            lastEncumbrance.amount = lastEncumbrance.amount.add(thisEncumbrance.amount);
            it.remove();
            priorityCount -= 10;

          } else {
            lastEncumbrance = thisEncumbrance;
          }
        }

      } finally {
        recalculateTotal();
      }
    }


    /**
     * recalculateTotal().
     * <p>Recalculate Total encumbrance for this Address Entry.
     * 'totalAmount' exists as a performance measure and as an integrity check.</p>
     *
     * @return :
     */
    @SuppressWarnings("squid:S2159") // Suppress '.equals on different types' warning. The Balance class overrides and allows.
    public Balance recalculateTotal() {
      Balance thisTotal = BALANCE_ZERO;

      Iterator<EncumbranceEntry> it = encumbrances.iterator();
      EncumbranceEntry thisOne;

      while (it.hasNext()) {

        thisOne = it.next();

        if (thisOne.amount.equalTo(BALANCE_ZERO)) {
          it.remove();
        } else {
          thisTotal = thisTotal.add(thisOne.amount);
        }

      }

      totalAmount = thisTotal;

      return totalAmount;
    }


    /**
     * Consumes encumbrance in reverse priority order (lowest (highest number)) first.
     *
     * @param referenceName :
     * @param consumeAmount :
     *
     * @return :
     */
    public Balance reduceEncumbrance(String referenceName, Number consumeAmount) {
      List<EncumbranceEntry> rVal = getByReference(referenceName);

      rVal.sort(EncumbranceEntry.LOW_TO_HIGH_PRIORITY);

      return encumbranceDeplete(rVal, consumeAmount);
    }


    /**
     * removeEncumbrance.
     * <p>Remove any encumbrance in this Asset entry that matches the given reference.</p>
     *
     * @param referenceName : (String) Encumberment Reference name.
     *
     * @return : List of removed EncumbranceEntries.
     */
    public List<EncumbranceEntry> removeEncumbrance(String referenceName) {
      ArrayList<EncumbranceEntry> rVal = new ArrayList<>();

      Iterator<EncumbranceEntry> it = encumbrances.iterator();

      while (it.hasNext()) {

        EncumbranceEntry thisOne = it.next();

        if (thisOne.reference.equalsIgnoreCase(referenceName)) {
          it.remove();
          rVal.add(thisOne);
        }

      }

      if (!rVal.isEmpty()) {
        recalculateTotal();
      }

      return rVal;
    }

  }



  /**
   * Decoder().
   * <p>Utility class to de-serialise AddressEncumbrances.</p>
   * # encumbrance data :
   * # merkle (Address : dict(fullassetid ,[[total], [encumbrance], [encumbrance], ...]))
   * # encumbrance : [reference, amount, [beneficiaries], [administrators]]
   * # beneficiaries : [[Address, StartDate, EndDate], ...]
   * # administrators : [[Address, StartDate, EndDate], ...]
   */
  public static class Decoder implements EntryDecoder<AddressEncumbrances> {


    @Override
    public AddressEncumbrances decode(MPWrappedArray merkleItem) {
      return new AddressEncumbrances(merkleItem);
    }

  }



  /**
   * EncumbranceEntry.
   * <p>Record details of an Encumbrance. This specifies a reference, an amount and lists of both beneficiaries and administrators.</p>
   * <p>EncumbranceEntry = [reference, amount, [beneficiaries], [administrators]]</p>
   */
  @SuppressWarnings("squid:ClassVariableVisibilityCheck")
  public static class EncumbranceEntry implements MsgPackable {

    public static final Comparator<EncumbranceEntry> HIGH_TO_LOW_PRIORITY = (s1, s2) -> {
      int rVal = (s1.priority - s2.priority);

      if (rVal == 0) {
        rVal = (s1.reference.compareTo(s2.reference));
      }

      if (rVal == 0) {
        rVal = (s1.amount.compareTo(s2.amount));
      }

      return rVal;
    };

    public static final Comparator<EncumbranceEntry> LOW_TO_HIGH_PRIORITY = (s1, s2) -> {
      int rVal = (s2.priority - s1.priority);

      if (rVal == 0) {
        rVal = (s2.reference.compareTo(s1.reference));
      }

      if (rVal == 0) {
        rVal = (s2.amount.compareTo(s1.amount));
      }

      return rVal;
    };

    private static final int DEFAULT_PRIORITY = Integer.MAX_VALUE;

    private static final int HIGH_PRIORITY = -1;

    public final String reference;

    private final List<EncumbranceDetail> administrators;

    private final List<EncumbranceDetail> beneficiaries;

    public Balance amount; // squid:ClassVariableVisibilityCheck, needs to be changeable.

    public long expiryDate;

    public int priority;


    /**
     * EncumbranceEntry.
     * <p>
     * Default Constructor.
     * </p>
     */
    public EncumbranceEntry() {
      reference = "";
      amount = BALANCE_ZERO;
      beneficiaries = new ArrayList<>();
      administrators = new ArrayList<>();
      priority = DEFAULT_PRIORITY;
      expiryDate = 0L;
    }


    /**
     * EncumbranceEntry().
     * <p>
     * Copy Constructor.
     * </p>
     *
     * @param toCopy :
     */
    public EncumbranceEntry(EncumbranceEntry toCopy) {
      reference = toCopy.reference;
      amount = toCopy.amount;
      beneficiaries = new ArrayList<>();
      administrators = new ArrayList<>();
      priority = toCopy.priority;
      toCopy.beneficiaries.forEach(detail -> beneficiaries.add(new EncumbranceDetail(detail)));
      toCopy.administrators.forEach(detail -> administrators.add(new EncumbranceDetail(detail)));
      expiryDate = toCopy.expiryDate;
    }


    /**
     * EncumbranceEntry.
     * <p>Constructor : From the given values.
     * </p>
     *
     * @param reference      : String
     * @param amount         : Long
     * @param beneficiaries  : List{EncumbranceDetail}
     * @param administrators : List{EncumbranceDetail}
     */
    @JsonCreator
    public EncumbranceEntry(
        @JsonProperty("reference") String reference,
        @JsonProperty("amount") Number amount,
        @JsonProperty("beneficiaries") List<EncumbranceDetail> beneficiaries,
        @JsonProperty("administrators") List<EncumbranceDetail> administrators
    ) {
      this.reference = reference;
      this.amount = new Balance(amount);
      this.beneficiaries = new ArrayList<>();
      setBeneficiaries(beneficiaries); // implied setExpiryDate
      this.administrators = new ArrayList<>();
      setAdministrators(administrators);
      priority = DEFAULT_PRIORITY;
    }


    /**
     * EncumbranceEntry.
     * <p>Constructor : From the given Object Array.</p>
     * <p>
     * [
     * reference,
     * amount,
     * [beneficiaries, ],
     * [administrators, ]
     * ]
     * </p>
     *
     * @param merkleItem :
     */
    public EncumbranceEntry(Object[] merkleItem) {
      this(new MPWrappedArrayImpl(merkleItem));
    }


    /**
     * New instance from binary representation.
     *
     * @param unpacker access to the binary representation
     */
    public EncumbranceEntry(MessageUnpacker unpacker) throws IOException {
      reference = unpacker.unpackString();
      priority = unpacker.unpackInt();
      amount = new Balance(unpacker);
      int s = unpacker.unpackArrayHeader();
      beneficiaries = new ArrayList<>(s);
      for (int i = 0; i < s; i++) {
        beneficiaries.add(i, new EncumbranceDetail(unpacker));
      }
      s = unpacker.unpackArrayHeader();
      administrators = new ArrayList<>(s);
      for (int i = 0; i < s; i++) {
        administrators.add(i, new EncumbranceDetail(unpacker));
      }
      setExpiryDate();
    }


    /**
     * EncumbranceEntry.
     * <p>Constructor : From the given MPWrappedArray.</p>
     * <p>
     * [
     * reference,
     * amount,
     * [beneficiaries, ],
     * [administrators, ]
     * ]
     * </p>
     *
     * @param merkleItem :
     */
    public EncumbranceEntry(MPWrappedArray merkleItem) {
      // Default constructor, initialises.

      //
      reference = merkleItem.asString(0);
      amount = new Balance(merkleItem.get(1));
      beneficiaries = new ArrayList<>();
      administrators = new ArrayList<>();

      // Priority only present if not 'Default' (allowed backwards compatability).
      if (merkleItem.size() > 4) {
        priority = merkleItem.asInt(4);
      } else {
        priority = DEFAULT_PRIORITY;
      }

      MPWrappedArray thisWrapped;
      MPWrappedArray myBeneficiaries = merkleItem.asWrapped(2);
      MPWrappedArray myAdministrators = merkleItem.asWrapped(3);

      for (int index = 0, len = myBeneficiaries.size(); index < len; index++) {
        thisWrapped = myBeneficiaries.asWrapped(index);
        beneficiaries.add(new EncumbranceDetail(thisWrapped.asString(0), thisWrapped.asLong(1), thisWrapped.asLong(2)));
      }

      for (int index = 0, len = myAdministrators.size(); index < len; index++) {
        thisWrapped = myAdministrators.asWrapped(index);
        administrators.add(new EncumbranceDetail(thisWrapped.asString(0), thisWrapped.asLong(1), thisWrapped.asLong(2)));
      }

      setExpiryDate();
    }


    /**
     * Can this entry be combined with a cumulative transaction with the other entry? Accumulation is only allowed if the reference, administrators and
     * beneficiaries all match.
     *
     * @param other the other entry
     *
     * @return true is accumulation is allowed.
     */
    public boolean canAccumulate(EncumbranceEntry other) {
      if (other == null) {
        // can always merge with null, as this is inserting the first entry
        return true;
      }
      if (reference == null || !reference.equalsIgnoreCase(other.reference)) {
        // reference must be set and match to combine
        return false;
      }

      // Beneficiaries must match
      BiPredicate<List<EncumbranceDetail>, List<EncumbranceDetail>> match = (a, b) -> {
        boolean aIsEmpty = a == null || a.isEmpty();
        boolean bIsEmpty = b == null || b.isEmpty();
        if (!(aIsEmpty || bIsEmpty)) {
          return (a.size() == b.size()) && a.containsAll(b);
        }

        // Only match if both are empty
        return aIsEmpty && bIsEmpty;
      };

      if (!match.test(beneficiaries, other.beneficiaries)) {
        return false;
      }

      // Administrators must match
      return match.test(administrators, other.administrators);
    }


    /**
     * encode().
     * <p>Encode AddressEncumbrance to an Object Array for serialisation : Hash or Persist.
     * </p>
     *
     * @return : Object[]
     */
    public Object[] encode() {
      Object[] encBeneficiaries = new Object[beneficiaries.size()];
      Object[] encAdmins = new Object[administrators.size()];

      final MutableInt index = new MutableInt(-1);
      beneficiaries.forEach(detail -> encBeneficiaries[index.increment()] = detail.encode());

      index.set(-1);
      administrators.forEach(detail -> encAdmins[index.increment()] = detail.encode());

      if (priority == DEFAULT_PRIORITY) {
        return new Object[]{
            reference,
            amount,
            encBeneficiaries,
            encAdmins
        };
      }

      return new Object[]{
          reference,
          amount,
          encBeneficiaries,
          encAdmins,
          priority
      };

    }


    @Override
    public boolean equals(Object toCompare) {
      if (toCompare == null) {
        return false;
      }

      if (!(toCompare instanceof EncumbranceEntry)) {
        return false;
      }

      EncumbranceEntry theOther = (EncumbranceEntry) toCompare;

      if (!reference.equalsIgnoreCase(theOther.reference)) {
        return false;
      }

      if (!amount.equalTo(theOther.amount)) {
        return false;
      }

      if (priority != theOther.priority) {
        return false;
      }

      if (beneficiaries.size() != theOther.beneficiaries.size()) {
        return false;
      }

      if (administrators.size() != theOther.administrators.size()) {
        return false;
      }

      for (int index = 0, len = beneficiaries.size(); index < len; index++) {
        if (!beneficiaries.get(index).equals(theOther.beneficiaries.get(index))) {
          return false;
        }
      }

      for (int index = 0, len = administrators.size(); index < len; index++) {
        if (!administrators.get(index).equals(theOther.administrators.get(index))) {
          return false;
        }
      }

      return true;
    }


    public List<EncumbranceDetail> getAdministrators() {
      return Collections.unmodifiableList(administrators);
    }


    public Balance getAmount() {
      return amount;
    }


    public List<EncumbranceDetail> getBeneficiaries() {
      return Collections.unmodifiableList(beneficiaries);
    }


    public String getReference() {
      return reference;
    }


    /**
     * hasExpired().
     * <p>Test that the given Address has a present or future administrator at the given time.
     * An Encumbrance is deemed to have expired when there are no present or future administrators.
     * The test used to be vs Beneficiaries, but it is valid to have encumbrances with no beneficiaries
     * and once there are no administrators, an encumbrance can not be utilised anyway.
     * </p>
     *
     * @param timeNow :
     *
     * @return : boolean.
     */
    public boolean hasExpired(long timeNow) {
      if (timeNow <= expiryDate) {
        return false;
      }

      for (EncumbranceDetail thisDetail : administrators) {
        if ((thisDetail.address.length() > 0) && ((thisDetail.endTime == 0) || (thisDetail.endTime > timeNow))) {
          return false;
        }
      }

      return true;
    }


    @Override
    public int hashCode() {
      return reference.hashCode();
    }


    /**
     * isAdministratorValid().
     * <p>Test that the given Address is an administrator at the given time.
     * Remember that an administrator address may appear more than once with different start / end dates.
     * </p>
     *
     * @param administratorAddress :
     * @param timeNow              :
     *
     * @return : boolean.
     */
    public boolean isAdministratorValid(String administratorAddress, long timeNow) {
      for (EncumbranceDetail thisDetail : administrators) {
        if ((thisDetail.startTime <= timeNow)
            && ((thisDetail.endTime == 0) || (thisDetail.endTime >= timeNow))
            && thisDetail.address.equalsIgnoreCase(administratorAddress)) {
          return true;
        }
      }

      return false;
    }


    /**
     * isBeneficiaryValid().
     * <p>Test that the given Address is a beneficiary at the given time.
     * Remember that a beneficiary address may appear more than once with different start / end dates.
     * </p>
     *
     * @param beneficiaryAddress :
     * @param timeNow            :
     *
     * @return : boolean.
     */
    public boolean isBeneficiaryValid(String beneficiaryAddress, long timeNow) {
      for (EncumbranceDetail thisEncumbrance : beneficiaries) {
        if ((thisEncumbrance.startTime <= timeNow)
            && ((thisEncumbrance.endTime == 0) || (thisEncumbrance.endTime >= timeNow))
            && thisEncumbrance.address.equalsIgnoreCase(beneficiaryAddress)) {
          return true;
        }
      }

      return false;
    }


    @Override
    public void pack(MessagePacker p) throws IOException {
      p.packString(reference);
      p.packInt(priority);
      amount.pack(p);
      p.packArrayHeader(beneficiaries.size());
      for (EncumbranceDetail ed : beneficiaries) {
        ed.pack(p);
      }
      p.packArrayHeader(administrators.size());
      for (EncumbranceDetail ed : administrators) {
        ed.pack(p);
      }
    }


    /**
     * setAdministrators.
     *
     * @param newAdministrators :
     */
    public void setAdministrators(List<EncumbranceDetail> newAdministrators) {
      administrators.clear();
      if (newAdministrators != null) {
        for (EncumbranceDetail thisDetail : newAdministrators) {
          administrators.add(new EncumbranceDetail(thisDetail));
        }
      }

      setExpiryDate();
    }


    /**
     * setBeneficiaries.
     *
     * @param newBeneficiaries :
     */
    public void setBeneficiaries(List<EncumbranceDetail> newBeneficiaries) {
      beneficiaries.clear();
      if (newBeneficiaries != null) {
        for (EncumbranceDetail thisDetail : newBeneficiaries) {
          beneficiaries.add(new EncumbranceDetail(thisDetail));
        }
      }
    }


    private void setExpiryDate() {
      expiryDate = 0L;

      if (administrators != null) {
        for (EncumbranceDetail thisDetail : administrators) {
          if (thisDetail != null) {
            if (thisDetail.endTime == 0) {
              expiryDate = Long.MAX_VALUE;
            } else {
              expiryDate = Math.max(expiryDate, thisDetail.endTime);
            }
          }
        }
      }
    }

  } // end class EncumbranceEntry



  private final Map<String, AssetEncumbrances> encumbrances = new TreeMap<>();

  private String address;

  private long updateHeight = -1;


  /**
   * AddressEncumbrances().
   * Base public constructor.
   * Initialises the encumbrance list.
   */
  AddressEncumbrances() {
    // do nothing
  }


  /**
   * AddressEntry constructor.
   *
   * @param address : Address to which this encumbrance data relates.
   */
  public AddressEncumbrances(String address) {
    this.address = address;
  }


  /**
   * AddressEntry constructor.
   *
   * @param address      : Address to which this encumbrance data relates.
   * @param encumbrances : Encumbrances
   */
  @JsonCreator
  public AddressEncumbrances(
      @JsonProperty("address") String address,
      @JsonProperty("encumbrances") Map<String, AssetEncumbrances> encumbrances
  ) {
    this.address = address;
    this.encumbrances.putAll(encumbrances);
  }


  /**
   * New instance from the encoded form.
   *
   * @param unpacker unpacker of the encoded form
   */
  public AddressEncumbrances(MessageUnpacker unpacker) throws IOException {
    byte version = unpacker.unpackByte();
    if (version < 0 || version > 1) {
      throw new IllegalArgumentException("Unrecognised encoding version: " + version);
    }
    address = unpacker.unpackString();
    int s = unpacker.unpackMapHeader();
    for (int i = 0; i < s; i++) {
      String key = unpacker.unpackString();
      AssetEncumbrances value = new AssetEncumbrances(unpacker);
      encumbrances.put(key, value);
    }
    if (version >= 1) {
      updateHeight = unpacker.unpackLong();
    }
  }


  /**
   * AddressEncumbrances().
   * <p>Constructor, taking an Object Array data.</p>
   *
   * @param merkleItem : Object array in required format.
   */
  public AddressEncumbrances(Object[] merkleItem) {
    this(new MPWrappedArrayImpl(merkleItem));
  }


  /**
   * AddressEncumbrances().
   * <p>Constructor, taking an MPWrappedArray of data.</p>
   * <p>MPWrappedArray is a helper class designed to make handling de-serialised data easier.</p>
   *
   * @param merkleItem : Object array in required format.
   */
  public AddressEncumbrances(MPWrappedArray merkleItem) {
    this();

    address = merkleItem.asString(1);

    MPWrappedArray dataArray = merkleItem.asWrapped(2);
    MPWrappedMap<String, Object> encs = dataArray.asWrappedMap(0);

    encs.iterate((assetID, encumbrances) -> {
      if (encumbrances instanceof MPWrappedArray) {
        this.encumbrances.put(assetID, new AssetEncumbrances((MPWrappedArray) encumbrances));
      } else if (encumbrances instanceof List) {
        this.encumbrances.put(assetID, new AssetEncumbrances(((List<?>) encumbrances).toArray()));
      } else {
        this.encumbrances.put(assetID, new AssetEncumbrances((Object[]) encumbrances));
      }

    });

    if (merkleItem.size() > 3) {
      updateHeight = merkleItem.asLong(3);
    }

  }


  /**
   * AddressEncumbrances().
   * <p>Copy constructor for AddressEncumbrances.</p>
   * <p>performs a complete, deep copy.
   * Does not duplicate final String and Long values of course as they are immutable anyway.</p>
   *
   * @param toCopy : AddressEncumbrances instance to Deep-Copy.
   */
  public AddressEncumbrances(AddressEncumbrances toCopy) {
    this();

    address = toCopy.getAddress();
    updateHeight = toCopy.getBlockUpdateHeight();

    toCopy.encumbrances.forEach((assetID, thisAssetEncumbrances) -> encumbrances.put(assetID, new AssetEncumbrances(thisAssetEncumbrances)));
  }


  /**
   * copy().
   * <p>Deep copy, as per the copy constructor.</p>
   *
   * @return AddressEncumbrances
   */
  @Override
  public AddressEncumbrances copy() {
    return new AddressEncumbrances(this);
  }


  /**
   * encode().
   * <p>Return this Encumbrance data set as an Object array in the prescribed format.</p>
   * <p>The format must be as required by the Decode function and the relevant constructor, must be MsgPackable and
   * at this point in time must be hash compatible with the legacy code base. </p>
   *
   * @param index : Index to encode.
   *
   * @return : Object[]
   *
   * @deprecated Use pack instead.
   */
  @Override
  @Deprecated(since = "28-11-2019")
  public Object[] encode(long index) {
    Map<String, Object> assetEncumbrances = new TreeMap<>();

    encumbrances.forEach((assetID, thisAssetEncumbrances) -> assetEncumbrances.put(assetID, thisAssetEncumbrances.encode()));

    // Note [ index, address, [data, NULL_TX!]] : Null entry required for Python compatability.
    if (updateHeight < 0) {
      return new Object[]{
          Math.max(0, index),
          address,
          new Object[]{new MPWrappedMap<>(assetEncumbrances), null}
      };
    } else {
      return new Object[]{
          Math.max(0, index),
          address,
          new Object[]{new MPWrappedMap<>(assetEncumbrances), null},
          updateHeight
      };
    }
  }


  public Object[] encode() {
    return encode(0);
  }


  /**
   * equals().
   * <p>Deep equality comparator.</p>
   *
   * @param toCompare : AddressEncumbrances Object to compare to.
   *
   * @return : boolean.
   */
  @Override
  public boolean equals(Object toCompare) {
    if (!(toCompare instanceof AddressEncumbrances)) {
      return false;
    }

    AddressEncumbrances theOther = (AddressEncumbrances) toCompare;

    if (!address.equalsIgnoreCase(theOther.getAddress())) {
      return false;
    }

    if (encumbrances.size() != theOther.encumbrances.size()) {
      return false;
    }

    for (Entry<String, AssetEncumbrances> e : encumbrances.entrySet()) {
      if (!e.getValue().equals(theOther.encumbrances.get(e.getKey()))) {
        return false;
      }
    }

    return true;
  }


  public String getAddress() {
    return getKey();
  }


  /**
   * <p>Return Aggregated available encumbrance for the given encumbrance Reference, limited by the balance at this address..</p>
   *
   * @param assetID        :
   * @param referenceName  :
   * @param addressHolding :
   *
   * @return :
   */
  public EncumbranceEntry getAggregateAvailableByReference(String assetID, String referenceName, Number addressHolding) {
    AssetEncumbrances thisAssetEncumbrance = encumbrances.get(assetID);

    if (thisAssetEncumbrance == null) {
      return null;
    }

    return thisAssetEncumbrance.getAggregateAvailableByReference(referenceName, addressHolding);
  }


  /**
   * <p>Return Aggregated encumbrance for the given encumbrance Reference.</p>
   *
   * @param assetID       :
   * @param referenceName :
   *
   * @return :
   */
  public EncumbranceEntry getAggregateByReference(String assetID, String referenceName) {
    AssetEncumbrances thisAssetEncumbrance = encumbrances.get(assetID);

    if (thisAssetEncumbrance == null) {
      return null;
    }

    return thisAssetEncumbrance.getAggregateByReference(referenceName);
  }


  /**
   * getAnyEncumbranceByReference().
   * <p>Return any EncumbranceEntry, identified by the given reference, relating to the given asset.</p>
   *
   * @param assetID       :
   * @param referenceName :
   *
   * @return : one of reference Encumbrances, or null
   */
  public EncumbranceEntry getAnyEncumbranceByReference(String assetID, String referenceName) {
    AssetEncumbrances thisAssetEncumbrance = encumbrances.get(assetID);
    if (thisAssetEncumbrance == null) {
      return null;
    }
    return thisAssetEncumbrance.getAnyByReference(referenceName);
  }


  /**
   * getAssetEncumbrance().
   * <p>Return AssetEncumbrances related to the given AssetID</p>
   *
   * @param assetID :
   *
   * @return AssetEncumbrances.
   */
  public AssetEncumbrances getAssetEncumbrance(String assetID) {
    return encumbrances.getOrDefault(assetID, null);
  }


  /**
   * getAssetEncumbrance().
   * <p>Return AssetEncumbrances related to the given AssetID (namespace + classID)</p>
   *
   * @param namespace :
   * @param classID   :
   *
   * @return : AssetEncumbrances
   */
  public AssetEncumbrances getAssetEncumbrance(String namespace, String classID) {
    return encumbrances.getOrDefault(namespace + "|" + classID, null);
  }


  public long getBlockUpdateHeight() {
    return updateHeight;
  }


  /**
   * getEncumbranceAmountByReference().
   * <p>Return the amount of the EncumbranceEntry, identified by the given reference, relating to the given asset.</p>
   *
   * @param assetID       :
   * @param referenceName :
   *
   * @return : Total Amount from the referenced Encumbrance.
   */
  public Balance getEncumbranceAmountByReference(String assetID, String referenceName) {

    List<EncumbranceEntry> theseOnes = getEncumbranceByReference(assetID, referenceName);

    if (theseOnes == null) {
      return BALANCE_ZERO;
    }

    final Balance[] value = {BALANCE_ZERO};

    theseOnes.forEach(thisOne -> value[0] = value[0].add(thisOne.amount));

    return value[0];
  }


  /**
   * getEncumbranceAmountByReference().
   * <p>Return the amount of the EncumbranceEntry, identified by the given reference, relating to the given asset.</p>
   *
   * @param namespace     :
   * @param classId       :
   * @param referenceName :
   *
   * @return : Total Amount from the referenced Encumbrance.
   */
  public Balance getEncumbranceAmountByReference(String namespace, String classId, String referenceName) {

    return getEncumbranceAmountByReference(namespace + "|" + classId, referenceName);
  }


  /**
   * getEncumbranceByReference().
   * <p>Return the EncumbranceEntry, identified by the given reference, relating to the given asset.</p>
   *
   * @param assetID       :
   * @param referenceName :
   *
   * @return : referenced Encumbrances.
   */
  public List<EncumbranceEntry> getEncumbranceByReference(String assetID, String referenceName) {
    AssetEncumbrances thisAssetEncumbrance = encumbrances.get(assetID);
    if (thisAssetEncumbrance == null) {
      return null;
    }
    return thisAssetEncumbrance.getByReference(referenceName);
  }


  /**
   * getEncumbranceByReference().
   * <p>Return the EncumbranceEntry, identified by the given reference, relating to the given asset (namespace + classID).</p>
   *
   * @param namespace     :
   * @param classId       :
   * @param referenceName :
   *
   * @return : referenced Encumbrances.
   */
  public List<EncumbranceEntry> getEncumbranceByReference(String namespace, String classId, String referenceName) {

    return getEncumbranceByReference(namespace + "|" + classId, referenceName);

  }


  @JsonProperty("encumbrances")
  public Map<String, AssetEncumbrances> getEncumbranceList() {
    return encumbrances;
  }


  /**
   * getEncumbranceTotal().
   * <p>Get the total encumbrance registered for the given asset against this address.</p>
   *
   * @param address     :
   * @param fullAssetID :
   * @param timeUTCSecs :
   *
   * @return : long, total encumbrance registered against the given Asset ID.
   */
  public Balance getEncumbranceTotal(String address, String fullAssetID, long timeUTCSecs) {

    if (!this.address.equalsIgnoreCase(address)) {
      return BALANCE_ZERO;
    }

    AssetEncumbrances thisAssetEncumbrance = encumbrances.get(fullAssetID);

    if (thisAssetEncumbrance == null) {
      return BALANCE_ZERO;
    }

    return thisAssetEncumbrance.getTotalAmount(timeUTCSecs);
  }


  /**
   * getEncumbranceTotal().
   * <p>Get the total encumbrance registered for the given asset (namespace + classID) against this address.</p>
   *
   * @param address     :
   * @param namespace   :
   * @param classId     :
   * @param timeUTCSecs :
   *
   * @return : long, total encumbrance registered against the given Asset ID.
   */
  public Balance getEncumbranceTotal(String address, String namespace, String classId, long timeUTCSecs) {

    return getEncumbranceTotal(address, namespace + "|" + classId, timeUTCSecs);

  }


  /**
   * getKey().
   * <p>Return key (Address) value for this encumbrance data set.</p>
   *
   * @return String : Encumbrance key (Address).
   */
  @Override
  @JsonIgnore
  @Nonnull
  public String getKey() {
    return address;
  }


  /**
   * hashCode().
   *
   * @return : hashCode (int).
   */
  @Override
  public int hashCode() {
    return address.hashCode();
  }


  @Override
  public void pack(MessagePacker packer) throws IOException {
    // Encoding version
    packer.packByte((byte) 1);

    packer.packString(address);

    Map<String, AssetEncumbrances> map = encumbrances;
    packer.packMapHeader(map.size());
    for (Entry<String, AssetEncumbrances> e : map.entrySet()) {
      packer.packString(e.getKey());
      e.getValue().pack(packer);
    }
    packer.packLong(updateHeight);
  }


  /**
   * removeAssetEncumbrance().
   * <p>Remove all Asset Encumbrances related to the given AssetID</p>
   *
   * @param assetID :
   *
   * @return : boolean - Success.
   */
  public boolean removeAssetEncumbrance(String assetID) {
    encumbrances.remove(assetID);
    return true;
  }


  /**
   * removeAssetEncumbrance().
   * <p>Remove all Asset Encumbrances related to the given AssetID (namespace + classID)</p>
   *
   * @param namespace :
   * @param classID   :
   *
   * @return : boolean - Success.
   */
  public boolean removeAssetEncumbrance(String namespace, String classID) {
    return removeAssetEncumbrance(namespace + "|" + classID);
  }


  public void setAddress(String address) {
    this.address = address;
  }


  /**
   * setAssetEncumbrance().
   * <p>Set AssetEncumbrances related to the given AssetID</p>
   *
   * @param assetID              :
   * @param thisAssetEncumbrance :
   *
   * @return : boolean - Success.
   */
  public boolean setAssetEncumbrance(String assetID, AssetEncumbrances thisAssetEncumbrance) {
    if ((thisAssetEncumbrance != null) && (thisAssetEncumbrance.getTotalAmount(0L).greaterThanEqualZero())) {
      encumbrances.put(assetID, thisAssetEncumbrance);
      return true;
    }

    return false;
  }


  @JsonProperty("updateHeight")
  public void setBlockUpdateHeight(long updateHeight) {
    this.updateHeight = Math.max(updateHeight, this.updateHeight);
  }


  /**
   * setEncumbranceEntry.
   * <p>Add Encumbrance entry against the given Asset ID</p>
   *
   * @param fullAssetID  : Asset ID.
   * @param newEntry     : (EncumbranceEntry) to add.
   * @param isCumulative : if cumulative, then any existing amount of Encumbrance will be added to this EncumbranceEntry.
   *                     : Note that Beneficiaries and Administrators will be overwritten.
   *
   * @return : (boolean) Success.
   */
  public boolean setEncumbranceEntry(
      String fullAssetID, Balance assetBalance, long timeNow, EncumbranceEntry newEntry, boolean isCumulative,
      boolean highPriority
  ) {
    try {
      AssetEncumbrances thisAssetEncumbrance = getAssetEncumbrance(fullAssetID);

      if (thisAssetEncumbrance == null) {
        // Only create a new AssetEncumbrances if the EncumbranceEntry is valid.
        if ((newEntry == null) || (newEntry.amount.lessThanEqualZero())) {
          return false;
        }

        thisAssetEncumbrance = new AssetEncumbrances();
        setAssetEncumbrance(fullAssetID, thisAssetEncumbrance);
      }

      return thisAssetEncumbrance.addEncumbrance(newEntry, assetBalance, timeNow, isCumulative, highPriority);
    } catch (Exception e) {
      logger.error("Error in setEncumbranceEntry()", e);
      return false;
    }
  }


}
