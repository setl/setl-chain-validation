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
package io.setl.rest.util;

import io.setl.bc.pychain.AddressToKeysMapper;
import io.setl.bc.pychain.AddressToNonceMapper;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.wallet.WalletAddress;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface WalletPosition extends AddressToKeysMapper, AddressToNonceMapper {

  class AddressPosition {

    public final WalletAddress entry;

    public final Map<String, Number> nsClsBalance;

    private long nonce;


    AddressPosition(long nonce, Map<String, Number> nsClsBalance, WalletAddress entry) {

      this.nsClsBalance = nsClsBalance;
      this.setNonce(nonce);
      this.entry = entry;
    }


    public AddressPosition(long nonce) {

      this(nonce, new HashMap<>(), null);
    }


    public long getNonce() {
      return nonce;
    }


    public void setNonce(long nonce) {
      this.nonce = nonce;
    }
  }

  Map<String, AddressPosition> getMap();

  AddressPosition getPosition(String address);

  void load(String walletFilePath, Merkle<AddressEntry> assetBalances) throws IOException;

  void setAssetOfInterest(Set<String> assetOfInterest);
}
