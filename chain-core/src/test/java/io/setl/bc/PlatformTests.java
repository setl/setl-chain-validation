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
package io.setl.bc;

import static org.junit.Assert.assertNotEquals;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.consensus.ProposerElection;
import io.setl.utils.TimeUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Ignore;
import org.junit.Test;

public class PlatformTests {

  /**
   * Test to see if hash function could be used to select next proposer.
   */
  @Test
  @Ignore
  public void testHashChooser() throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    final int count = 1000;
    final int choices = 3;
    int[] spread = new int[choices];

    for (int t0 = TimeUtil.unixTime(), i = 0; i < count; i++) {
      int value = t0 + i * 5;
      byte[] x = digest.digest(new byte[]{
          (byte) (value >>> 24), (byte) (value >>> 16),
          (byte) (value >>> 8), (byte) value
      });
      int choice = Math.abs(x[0]) % choices;
      spread[choice]++;
    }
    for (int i = 0; i < choices; i++) {
      System.out.println(String.format("Spread %d=%d", i, spread[i]));

      // Expected to fail with a chance of 3*10^-79
      assertNotEquals(0, spread[i]);
    }
  }


  @Test
  @Ignore
  public void testHashChooser2() throws NoSuchAlgorithmException {

    Hash hash = Hash.fromHex("c1e191e11cdf66a7a0ab535996d180e9fc744b834f144f06b043a8665027d02f");
    long height = 71;
    final int count = 1000;
    final int choices = 6;
    int[] spread = new int[choices];
    //
    for (int t0 = TimeUtil.unixTime(), i = 0; i < count; i++) {
      int value = t0 + i * 5;

      int choice = ProposerElection.selectProposer(hash, value, height, choices);
      spread[choice]++;
      // System.out.println("Hash Chooser=" + choice);
    }
    for (int i = 0; i < choices; i++) {
      System.out.println(String.format("Spread %d=%d", i, spread[i]));

      // Expected to fail with a chance of 3*10^-79
      assertNotEquals(0, spread[i]);
    }
  }
}
