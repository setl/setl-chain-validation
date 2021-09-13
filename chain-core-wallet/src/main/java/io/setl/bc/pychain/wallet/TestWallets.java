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
package io.setl.bc.pychain.wallet;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

import io.setl.passwd.vault.TestVault;
import io.setl.passwd.vault.VaultAccessor;

/**
 * Configure a security vault for use in testing wallets. This should not be used outside of a unit test.
 *
 * @author Simon Greatrix on 18/05/2021.
 */
public class TestWallets {

  /**
   * Set the secrets used to protect wallet addresses. Uses the prefix "wa" and generates 4 secrets.
   */
  public static void reset() {
    reset("wa", 4);
  }


  /**
   * Set the secrets used to protect wallet addresses.
   *
   * @param prefix prefix used to name secrets
   * @param count  the number of secrets to generate
   */
  public static void reset(String prefix, int count) {
    TestVault vault = TestVault.setInstance();
    if (!(VaultAccessor.getInstance() instanceof TestVault)) {
      throw new IllegalStateException("The TestVault could not be installed.");
    }
    ArrayList<String> names = new ArrayList<>();
    Random random = new Random();
    for (int i = 0; i < count; i++) {
      String key = prefix + i;
      names.add(key);
      if (random.nextBoolean()) {
        vault.createAesKey(WalletAddress.WRAP_KEY_PREFIX + key);
      } else {
        byte[] salt = new byte[20];
        random.nextBytes(salt);
        vault.setBinary(WalletAddress.WRAP_KEY_PREFIX + key, salt);
      }
    }
    vault.setPlain(WalletAddress.VAULT_DEFAULT_WALLET_KEY_NAME, names.stream().collect(Collectors.joining(",")));
  }

}
