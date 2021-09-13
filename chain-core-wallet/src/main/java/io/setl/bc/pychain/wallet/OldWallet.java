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

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A representation of an old-style wallet which maintains two lists of key pairs and their nonces. No wallet has both kinds of keys, so there is no need to
 * distinguish between them.
 *
 * @deprecated Replaced by new-style wallets.
 */
@Deprecated
public class OldWallet {

  private static final int DEFAULT_VERSION = 2;


  /**
   * Createa an old-style wallet from its message pack encoding.
   *
   * @param va the encoded wallet
   *
   * @return the wallet
   */
  public static OldWallet fromList(MPWrappedArray va) throws IOException {
    // int version=va.asInt(0)
    return new OldWallet(PublicPrivatePairForAddress.decode(va.asWrapped(1)),
        PublicPrivatePairForAddress.decode(va.asWrapped(2), true));
  }


  private final List<PublicPrivatePairForAddress> addressList;

  private final List<PublicPrivatePairForAddress> signNodeList;

  private int id;

  private int version = DEFAULT_VERSION;


  /**
   * Create a new old-style wallet with the given addresses. An old-style wallet either has normal addresses or sig-node addresses.
   *
   * @param addressList  the normal addresses
   * @param signNodeList the sig-node specific addresses
   */
  public OldWallet(List<PublicPrivatePairForAddress> addressList, List<PublicPrivatePairForAddress> signNodeList) {
    this.addressList = addressList;
    this.signNodeList = signNodeList;
  }


  /**
   * Convert this old-style wallet into a new-style Wallet.
   *
   * @return this wallet as a new-style wallet
   * @throws GeneralSecurityException if private key encryption fails
   */
  public Wallet asWallet() throws GeneralSecurityException {
    List<WalletAddress> addresses = new ArrayList<>();
    for (PublicPrivatePairForAddress p : addressList) {
      addresses.add(p.convert(id));
    }
    for (PublicPrivatePairForAddress p : signNodeList) {
      addresses.add(p.convert(id));
    }

    return new Wallet(addresses);
  }


  /**
   * Encode this wallet for message packing.
   *
   * @return the encoded data
   */
  public MPWrappedArray encode() {
    return new MPWrappedArrayImpl(
        new Object[]{version, PublicPrivatePairForAddress.encode(addressList, false),
            PublicPrivatePairForAddress.encode(signNodeList, true)});
  }


  public void forEachAddressList(Consumer<PublicPrivatePairForAddress> consumer) {
    addressList.forEach(consumer);
  }


  public void forEachSignNode(Consumer<PublicPrivatePairForAddress> consumer) {
    signNodeList.forEach(consumer);
  }


  public int getId() {
    return id;
  }


  public void setId(int newId) {
    id = newId;
  }
}
