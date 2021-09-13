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

import io.setl.bc.pychain.file.WalletLoader;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.wallet.Wallet;
import io.setl.bc.pychain.wallet.WalletAddress;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultWalletPosition implements WalletPosition {

  private static final Logger logger = LoggerFactory.getLogger(DefaultWalletPosition.class);

  private Set<String> assetOfInterest;

  private final Map<String, AddressPosition> currentPosition = new HashMap<>();

  private final WalletLoader walletLoader = new WalletLoader();


  @Override
  public void addAddress(String newAddr, KeyPair keyPair) {
    WalletAddress address;
    try {
      address = new WalletAddress(0, keyPair, AddressType.NORMAL);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Required security not supported", e);
    }
    currentPosition.put(newAddr, new AddressPosition(0, new HashMap<>(), address));

  }


  @Override
  public Set<String> getAddresses() {

    return currentPosition.keySet();
  }


  @Override
  public WalletAddress getKeyPair(String address) {

    AddressPosition a = currentPosition.get(address);

    if (a != null) {
      return a.entry;
    }

    return null;
  }


  public Map<String, AddressPosition> getMap() {

    return currentPosition;
  }


  @Override
  public long getNonce(String address) {

    AddressPosition a = currentPosition.get(address);

    if (a != null) {
      synchronized (a) {
        return a.getNonce();
      }
    }

    return 0L;
  }


  public AddressPosition getPosition(String address) {

    return currentPosition.get(address);
  }


  @Nullable
  @Override
  public byte[] getPrivateKey(String address) {
    AddressPosition a = currentPosition.get(address);
    if (a != null) {
      return a.entry.getPrivateKey().getEncoded();
    }

    return null;
  }


  @Override
  public String getPublicKey(String addr) {

    AddressPosition a = currentPosition.get(addr);

    if (a != null) {
      return a.entry.getHexPublicKey();
    }

    return null;
  }


  //TODO - Locking of currentPosition is a mess - resolve and refactor to new bean.
  @Override
  public void increaseNonce(String address) {

    AddressPosition a = currentPosition.get(address);

    if (a != null) {
      synchronized (a) {
        a.setNonce(a.getNonce() + 1);
      }
    } else {
      throw new IllegalArgumentException();
    }
  }


  /**
   * Load wallet details from a file and read the associated asset balances from state.
   *
   * @param walletFilePath the file containing the wallet
   * @param assetBalances  the balances in state
   *
   * @throws IOException if wallet file cannot be accessed
   */
  public synchronized void load(String walletFilePath, Merkle<AddressEntry> assetBalances) throws IOException {
    Wallet wallet = walletLoader.loadWalletFromFile(walletFilePath);
    wallet.forEachAddress(entry -> {
      String addr = AddressUtil.publicKeyToAddress(entry.getHexPublicKey(), AddressType.NORMAL);
      AddressEntry addressEntry;
      Map<String, Number> addressPosition = new HashMap<>();
      addressEntry = assetBalances.find(addr);
      if (addressEntry != null) {
        long nonce = addressEntry.getNonce();
        currentPosition.put(addr, new AddressPosition(nonce, addressPosition, entry));
        if ((assetOfInterest != null) && (addressEntry.getClassBalance() != null)) {
          addressEntry.getClassBalance().forEach((assetID, assetBalance) -> {
                if (assetOfInterest.contains(assetID)) {
                  addressPosition.put(assetID, assetBalance.getValue());
                  logger.debug("ClassBal:{}={}", assetID, assetBalance);
                }
              }
          );
        }
      }
    });
  }


  public void setAssetOfInterest(Set<String> assetOfInterest) {

    this.assetOfInterest = assetOfInterest;
  }

}
