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
package io.setl.bc.pychain.consensus;

import io.setl.bc.pychain.Hash;
import io.setl.common.Sha256Hash;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.security.DigestException;
import java.security.MessageDigest;

public class ProposerElection {

  private static final long DEFAULT_INTERVAL_LENGTH = 5L;


  /**
   * Get the mod'd start interval for any given time.
   *
   * @param timestamp      The current timestamp
   * @param intervalPeriod The consensus interval
   *
   * @return The mod'd interval start.
   */
  public static long curInterval(long timestamp, long intervalPeriod) {
    return ((timestamp / intervalPeriod) * intervalPeriod);

  }


  /**
   * Get the mod'd start interval for any given time.
   *
   * @param timestamp The current timestamp
   *
   * @return The mod'd interval start.
   */
  public static long curInterval(long timestamp) {
    return curInterval(timestamp, DEFAULT_INTERVAL_LENGTH);
  }


  /**
   * Select the proposer index.
   *
   * @param hash          The current hash
   * @param interval      The current interval start
   * @param height        The current height
   * @param proposerCount The current validation node count
   *
   * @return The selected proposer index, in the range 0..(proposerCount-1)
   */
  public static int selectProposer(Hash hash, long interval, long height, int proposerCount) {
    if (proposerCount <= 0) {
      throw new IllegalArgumentException("Proposer must be positive, not " + proposerCount);
    }

    // Proposer is determined from the current hash, the interval, and the chain height.

    MessageDigest digest = Sha256Hash.newDigest();
    byte[] h = hash.get();
    if (h != null) {
      digest.update(h);
    }

    byte[] buf = new byte[32];
    ByteBuffer bbuf = ByteBuffer.wrap(buf);
    bbuf.order(ByteOrder.BIG_ENDIAN);
    LongBuffer lbuf = bbuf.asLongBuffer();
    lbuf.put(0, interval);
    lbuf.put(1, height);
    digest.update(buf, 0, 16);
    try {
      digest.digest(buf, 0, 32);
    } catch (DigestException e) {
      throw new InternalError("SHA-256 failed", e);
    }

    long v = lbuf.get(0) & 0x7fff_ffff_ffff_ffffL;

    return (int) (v % proposerCount);
  }


  private ProposerElection() {

  }

}
