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
package io.setl.bc.pychain.block;

import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.state.tx.Txi;
import io.setl.bc.pychain.state.tx.helper.AddressSet;
import io.setl.common.CommonPy.TxType;
import io.setl.common.NearlyConstant;

public class TransactionForProcessing {

  /** Sort Transactions into from-address, nonce, hash order, which is how they are presented in the Block. Block order is from-address, nonce, hash. */
  @SuppressWarnings("squid:S4973") // (a1 != a2) is intended as object comparison
  public static final Comparator<TransactionForProcessing> SORT_BLOCK_ORDER = (o1, o2) -> {
    String a1 = o1.fromAddress;
    String a2 = o2.fromAddress;
    // Object non-identity is intentional
    int c = (a1 != a2) ? a1.compareTo(a2) : 0;
    if (c != 0) {
      return c;
    }

    long n1 = o1.nonce;
    long n2 = o2.nonce;
    if (n1 < n2) {
      return -1;
    }
    if (n1 > n2) {
      return 1;
    }

    return o1.getHash().compareTo(o2.getHash());
  };

  /** Nonce check order is nonce address and then nonce. */
  @SuppressWarnings("squid:S4973") // (a1 != a2) is intended as object comparison
  public static final Comparator<TransactionForProcessing> SORT_NONCE_CHECK_ORDER = (o1, o2) -> {
    String a1 = o1.nonceAddress;
    String a2 = o2.nonceAddress;
    // Object non-identity is intentional
    int c = (a1 != a2) ? a1.compareTo(a2) : 0;
    if (c != 0) {
      return c;
    }

    return Long.compare(o1.nonce, o2.nonce);
  };

  /** Sort transactions into priority, address, nonce, hash order for processing. Processing order is priority, nonce address, nonce, hash. */
  @SuppressWarnings("squid:S4973") // (a1 != a2) is intended as object comparison
  public static final Comparator<TransactionForProcessing> SORT_PROCESSING_ORDER = (o1, o2) -> {
    int p1 = o1.priority;
    int p2 = o2.priority;
    if (p1 != p2) {
      return (p1 < p2) ? -1 : 1;
    }
    String a1 = o1.nonceAddress;
    String a2 = o2.nonceAddress;
    // Object non-identity is intentional
    int c = (a1 != a2) ? a1.compareTo(a2) : 0;
    if (c != 0) {
      return c;
    }

    long n1 = o1.nonce;
    long n2 = o2.nonce;
    if (n1 < n2) {
      return -1;
    }
    if (n1 > n2) {
      return 1;
    }

    return o1.getHash().compareTo(o2.getHash());
  };

  private static final Logger logger = LoggerFactory.getLogger(TransactionForProcessing.class);


  /**
   * TransactionForProcessingWrapper().
   * <p>Transaction wrapper. Intended to allow TXs to be marked during the proposal generation process.</p>
   *
   * @param txi :TX Array
   */
  public static TransactionForProcessing[] wrap(Txi[] txi) {
    TransactionForProcessing[] transactionForProcessing = new TransactionForProcessing[txi.length];
    for (int i = 0; i < txi.length; i++) {
      transactionForProcessing[i] = new TransactionForProcessing(txi[i]);
    }
    return transactionForProcessing;
  }

  private final AddressSet addresses;

  private final String fromAddress;

  private final String hash;

  private final long nonce;

  private final String nonceAddress;

  private final int priority;

  private final TxType txType;

  private final Txi txi;

  private boolean isFlawed;

  private boolean isFuture;

  private boolean isReplay;

  /**
   * TransactionForProcessingWrapper().
   * <p>Transaction wrapper. Intended to allow TXs to be marked during the proposal generation process.</p>
   *
   * @param txi : Tx
   */
  TransactionForProcessing(Txi txi) {
    this.txi = Objects.requireNonNull(txi, "A transaction for processing cannot be null");

    this.isFuture = false;
    this.isReplay = false;

    // These methods are REQUIRED to be failure free and non-null.
    try {
      hash = Objects.requireNonNull(txi.getHash(), "A transaction must have a non-null hash");
      nonce = txi.getNonce();
      nonceAddress = NearlyConstant.fixed(Objects.requireNonNull(txi.getNonceAddress(), "A transaction must have a non-null nonce address"));
      txType = Objects.requireNonNull(txi.getTxType(), "A transaction must have a non-null type");
    } catch (RuntimeException failure) {
      // A serious bug in our code. Throw an internal error to highlight this.
      throw new InternalError("Method required to be safe threw unchecked exception", failure);
    }

    // No transactions have a customisable priority (August 2020), so this method should always produce the correct priority.
    priority = safeRead(txi, Txi::getPriority, txType.getPriority());

    // There is quite a lot that can go wrong when processing addresses.
    fromAddress = NearlyConstant.fixed(safeRead(txi, Txi::getFromAddress, ""));
    addresses = AddressSet.of(safeRead(txi, Txi::addresses, Collections.emptySet()));
  }


  public AddressSet addresses() {
    return addresses;
  }


  public String getFromAddress() {
    return fromAddress;
  }


  public String getHash() {
    return hash;
  }


  public long getNonce() {
    return nonce;
  }


  public String getNonceAddress() {
    return nonceAddress;
  }


  public int getPriority() {
    return priority;
  }


  public TxType getTxType() {
    return txType;
  }


  public Txi getWrapped() {
    return txi;
  }


  public boolean isFlawed() {
    return isFlawed;
  }


  public boolean isFuture() {
    return isFuture;
  }


  public boolean isGood() {
    return txi.isGood();
  }


  public boolean isReplay() {
    return isReplay;
  }


  public void rejectNonceInFuture() {
    isFuture = true;
    isFlawed = true;
  }


  public void rejectNonceInPast() {
    isReplay = true;
    isFlawed = true;
  }


  /**
   * Invoke a getter on a TX. If the getter fails, mark the TX as bad and use a default value.
   *
   * @param txi          the TX
   * @param function     the getter
   * @param defaultValue the default value
   * @param <T>          the type of the property
   *
   * @return the value
   */
  private <T> T safeRead(Txi txi, Function<Txi, T> function, T defaultValue) {
    try {
      return function.apply(txi);
    } catch (RuntimeException failure) {
      setFlawed(true);
      logger.error("Failed to access required property of TX {}:{}/{} of type {}. TX will be marked as flawed.",
          txi.getNonceAddress(), txi.getNonce(), txi.getHash(), txi.getTxType(),
          failure
      );
    }
    return defaultValue;
  }


  public void setFlawed(boolean flawed) {
    isFlawed = flawed;
  }

}
