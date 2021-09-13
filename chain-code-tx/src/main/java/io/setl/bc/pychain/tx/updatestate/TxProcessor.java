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
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.CommonPy.SuccessType;
import java.text.MessageFormat;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Simon Greatrix on 20/01/2020.
 */
public class TxProcessor<T extends Txi> {

  private static final Logger logger = LoggerFactory.getLogger(TxProcessor.class);


  public static TxFailedException fail(String pattern, Object... params) {
    return new TxFailedException(MessageFormat.format(pattern, params));
  }


  /**
   * Specifies the TX type that this processor fulfills. Sub-classes MUST implement this.
   *
   * @return the TX type
   */
  protected Class<T> fulfills() {
    throw new UnsupportedOperationException();
  }


  /**
   * This method is called after <code>fulfills</code> but before all other methods.
   */
  protected void initialise(StateSnapshot snapshot, T txi) {
    // can be over-ridden in sub-classes
  }


  /**
   * Check if the transaction is applicable. Typically that means it matches the chain and priority.
   */
  protected boolean isNotApplicable(T txi, StateSnapshot snapshot, long updateTime, int priority) {
    return txi.getPriority() != priority || txi.getToChainId() != snapshot.getChainId();
  }


  /**
   * Try to apply the transaction to the snapshot.
   *
   * @param txi        the transaction
   * @param snapshot   the snapshot
   * @param updateTime the update time
   *
   * @return null for a successful normal completion, a suitable ReturnTuple for any other completion.
   *
   * @throws TxFailedException for a failed completion.
   */
  protected ReturnTuple tryApply(T txi, StateSnapshot snapshot, long updateTime) throws TxFailedException {
    // implement in sub-classes
    return null;
  }


  /**
   * Try to apply the transaction to the snapshot using power of attorney.
   *
   * @param txi        the transaction
   * @param snapshot   the snapshot
   * @param updateTime the update time
   *
   * @return null for a successful normal completion, a suitable ReturnTuple for any other completion.
   *
   * @throws TxFailedException for a failed completion.
   */
  protected ReturnTuple tryApplyPoa(T txi, StateSnapshot snapshot, long updateTime) throws TxFailedException {
    // implement in sub-classes
    return null;
  }


  /**
   * Check that the transaction can be applied to the snapshot. The "tryApply" method is invoked after this, provided the return value from this was null. A
   * non-null return terminates processing.
   *
   * @param txi        the transaction
   * @param snapshot   the snapshot
   * @param updateTime the update time
   * @param checkOnly  if true, the transaction will not actually be applied at this time
   *
   * @return null for a successful normal completion, any non-null value to complete without further processing.
   *
   * @throws TxFailedException for a failed completion.
   */
  protected ReturnTuple tryChecks(T txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // implement in sub-classes
    return null;
  }


  /**
   * Check that the transaction can be applied to the snapshot using power of attorney. The "tryApplyPoa" method is invoked after this, provided the return
   * value from this was null. A non-null return terminates processing.
   *
   * @param txi        the transaction
   * @param snapshot   the snapshot
   * @param updateTime the update time
   * @param checkOnly  if true, the transaction will not actually be applied at this time
   *
   * @return null for a successful normal completion, any non-null value to complete without further processing.
   *
   * @throws TxFailedException for a failed completion.
   */
  protected ReturnTuple tryChecksPoa(T txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    // implement in sub-classes
    return null;
  }


  /**
   * Try to update state with the transaction. This method invokes tryChecks and tryApply and traps RuntimeExceptions. A failure of tryApply is assumed to
   * corrupt the snapshot. Sub-classes should only over-ride this if the normal two step process is inappropriate.
   */
  protected ReturnTuple tryUpdate(T txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    ReturnTuple rt;
    try {
      rt = tryChecks(txi, snapshot, updateTime, checkOnly);
    } catch (RuntimeException re) {
      logger.error("Error checking {}:{}", txi.getTxType(), txi.getHash(), re);
      return new ReturnTuple(SuccessType.FAIL, "Internal Error", re);
    }
    if (rt != null) {
      return rt;
    }
    if (!checkOnly) {
      try {
        rt = tryApply(txi, snapshot, updateTime);
        if (rt != null) {
          return rt;
        }
      } catch (RuntimeException re) {
        // assume snapshot is corrupted
        snapshot.setCorrupted(true, "Corrupted by internal error: " + re.getClass() + " : " + re.getMessage());
        logger.error("Error processing {}:{}", txi.getTxType(), txi.getHash(), re);
        return new ReturnTuple(SuccessType.FAIL, "Internal Error", re);
      }
    }
    return null;
  }


  /**
   * Try to update state with the transaction with a power of attorney. This method invokes tryChecks and tryApply and traps RuntimeExceptions. A failure of
   * tryApply is assumed to corrupt the snapshot. Sub-classes should only over-ride this if the normal two step process is inappropriate.
   */
  protected ReturnTuple tryUpdatePoa(T txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    ReturnTuple rt;
    try {
      rt = tryChecksPoa(txi, snapshot, updateTime, checkOnly);
    } catch (RuntimeException re) {
      logger.error("Error checking {}:{}", txi.getTxType(), txi.getHash(), re);
      return new ReturnTuple(SuccessType.FAIL, "Internal Error", re);
    }
    if (rt != null) {
      return rt;
    }
    if (!checkOnly) {
      try {
        rt = tryApplyPoa(txi, snapshot, updateTime);
        if (rt != null) {
          return rt;
        }
      } catch (RuntimeException re) {
        // assume snapshot is corrupted
        snapshot.setCorrupted(true, "Corrupted by internal error: " + re.getClass() + " : " + re.getMessage());
        logger.error("Error processing {}", txi.getTxType(), re);
        return new ReturnTuple(SuccessType.FAIL, "Internal Error", re);
      }
    }
    return null;
  }


  /**
   * Try to update the provided snapshot with the transaction.
   *
   * @param txi        the transaction
   * @param snapshot   the snapshot
   * @param updateTime the time the TX was expected to be applied
   * @param priority   the TX's innate priority
   * @param checkOnly  if true, only perform checks
   *
   * @return the result of applying the TX.
   */
  @Nonnull
  public ReturnTuple update(Txi txi, StateSnapshot snapshot, long updateTime, int priority, boolean checkOnly) {
    T castTxi = fulfills().cast(txi);
    initialise(snapshot, castTxi);
    if (isNotApplicable(castTxi, snapshot, updateTime, priority)) {
      return ReturnTuple.pass(checkOnly);
    }

    try {
      ReturnTuple rt;
      if (txi.isPOA()) {
        rt = tryUpdatePoa(castTxi, snapshot, updateTime, checkOnly);
      } else {
        rt = tryUpdate(castTxi, snapshot, updateTime, checkOnly);
      }
      return (rt != null) ? rt : ReturnTuple.pass(checkOnly);
    } catch (TxFailedException failed) {
      return failed.getFailure(checkOnly);
    }
  }
}
