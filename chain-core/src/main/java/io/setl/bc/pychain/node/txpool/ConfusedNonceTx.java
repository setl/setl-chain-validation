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
package io.setl.bc.pychain.node.txpool;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.accumulator.HashAccumulator;
import io.setl.bc.pychain.state.tx.Txi;
import io.setl.common.CommonPy.TxType;
import io.setl.common.Hex;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A holder for multiple TXs that share the same nonce and nonce address.
 *
 * @author Simon Greatrix on 2019-04-14.
 */
class ConfusedNonceTx implements Txi {

  /** It is important that no real hash matches this hash. The length of 13 bytes should enforce that. */
  static final String CONFUSED_HASH = Hex.encode("Confused Hash".getBytes(StandardCharsets.UTF_8));

  private static final Logger logger = LoggerFactory.getLogger(ConfusedNonceTx.class);


  public static Txi merge(Txi current, Txi newTxi) {
    if (current instanceof ConfusedNonceTx) {
      ((ConfusedNonceTx) current).txis.put(Hash.fromHex(newTxi.getHash()), newTxi);
      return current;
    }

    if (current.getHash().equals(newTxi.getHash())) {
      // same transaction
      return current;
    }

    if (logger.isErrorEnabled()) {
      logger.error("Multiple TXs with same address and nonce: {} and {}", current.getHash(), newTxi.getHash());
    }
    return new ConfusedNonceTx(current, newTxi);
  }


  private final Hash firstHash;

  private final long nonce;

  private final String nonceAddress;

  private final Map<Hash, Txi> txis = new HashMap<>();


  private ConfusedNonceTx(Txi txi1, Txi txi2) {
    nonce = txi1.getNonce();
    nonceAddress = txi1.getNonceAddress();
    firstHash = Hash.fromHex(txi1.getHash());

    txis.put(Hash.fromHex(txi1.getHash()), txi1);
    txis.put(Hash.fromHex(txi2.getHash()), txi2);
  }


  @Override
  public Set<String> addresses() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public HashAccumulator buildHash(HashAccumulator hacc) {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public Object[] encodeTx() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public String getFromAddress() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public String getFromPublicKey() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public String getHash() {
    // It is important that this does not match a real hash.
    return CONFUSED_HASH;
  }


  @Override
  public int getHeight() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Nullable
  @Override
  public String getMetadata() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public long getNonce() {
    return nonce;
  }


  @Override
  public String getNonceAddress() {
    return nonceAddress;
  }


  public Txi getPrimaryTx() {
    return txis.get(firstHash);
  }


  @Override
  public int getPriority() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public String getSignature() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public long getTimestamp() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public int getToChainId() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  /**
   * Get the TX with the matching hash.
   *
   * @param hash the hash to match
   *
   * @return the TX
   */
  public Txi getTx(Hash hash) {
    return txis.get(hash);
  }


  @Override
  public TxType getTxType() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public boolean isGood() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public boolean isPOA() {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public void setGood(boolean isGood) {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public void setHeight(int height) {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public void setSignature(String sig) {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public void setUpdated(boolean isApplied) {
    throw new UnsupportedOperationException("Multiple TXs with same nonce.");
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ConfusedNonceTx{");
    sb.append("nonce=").append(nonce);
    sb.append(", nonceAddress='").append(nonceAddress).append('\'');
    sb.append(", txis=").append(txis);
    sb.append('}');
    return sb.toString();
  }
}
