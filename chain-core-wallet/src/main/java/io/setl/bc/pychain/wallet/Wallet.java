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

import java.io.IOException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import io.setl.bc.pychain.msgpack.MsgPackable;

public class Wallet implements MsgPackable {

  private static final int ENCODE_VERSION = 3;


  /**
   * Create a new empty wallet.
   *
   * @return an empty wallet
   */
  public static Wallet emptyWallet() {
    return new Wallet(new ArrayList<>());
  }


  /**
   * For thread safety we use this data type. It is more expensive than an ordinary ArrayList but is safer.
   */
  private CopyOnWriteArrayList<WalletAddress> addressList;

  /**
   * The wallet's ID.
   */
  private int id;


  /**
   * New wallet containing the specified list of addresses.
   *
   * @param addressList the addresses
   */
  public Wallet(List<WalletAddress> addressList) {
    this.addressList = addressList != null ? new CopyOnWriteArrayList<>(addressList) : new CopyOnWriteArrayList<>();
    forEachAddress(wa -> wa.setWallet(this));
  }


  /**
   * Wallet, Copy constructor.
   *
   * @param toCopy : Wallet to copy.
   */
  public Wallet(Wallet toCopy) {
    addressList = new CopyOnWriteArrayList<>();
    toCopy.forEachAddress(w -> addressList.add(w));
    id = toCopy.getId();
  }


  /**
   * Create a Wallet instance by unpacking it.
   *
   * @param unpacker the source
   */
  public Wallet(MessageUnpacker unpacker) throws IOException {
    int l = unpacker.unpackArrayHeader();
    if (l != 3) {
      throw new IOException("Packed wallet should contain 3 fields, not " + l);
    }
    int v = unpacker.unpackInt();
    if (v != ENCODE_VERSION) {
      throw new IOException("Packed wallet is version " + v + ", not " + ENCODE_VERSION);
    }
    id = unpacker.unpackInt();
    l = unpacker.unpackArrayHeader();
    addressList = new CopyOnWriteArrayList<>();
    for (int i = 0; i < l; i++) {
      WalletAddress wa = new WalletAddress(unpacker);
      addressList.add(wa);
      wa.setWallet(this);
    }
  }


  /**
   * Add an address to this wallet. Ths wallet must be saved to make the addition permanent.
   *
   * @param address the address to add
   */
  public void addAddressEntry(WalletAddress address) {

    if (address != null) {

      String addr = address.getAddress();

      // Get the address list instance for thread safety, in-case another thread changes it
      List<WalletAddress> myAddress = addressList;

      myAddress.removeIf(a -> a.getAddress().equals(addr));
      myAddress.add(address);
      address.setWallet(this);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Wallet)) {
      return false;
    }

    Wallet wallet = (Wallet) o;

    return addressList.equals(wallet.addressList);
  }


  /**
   * For each address in the wallet, do something.
   *
   * @param consumer the thing to do
   */
  public void forEachAddress(Consumer<WalletAddress> consumer) {
    addressList.forEach(consumer);
  }


  /**
   * Get the wallet address instance that corresponds to the given address.
   *
   * @param address the address
   *
   * @return the key pair, or null if it was not found
   */
  public WalletAddress getAddress(String address) {
    // Get the address list instance for thread safety, in-case another thread changes it
    List<WalletAddress> myAddressList = addressList;
    if (address == null) {
      return myAddressList.isEmpty() ? null : myAddressList.get(0);
    }

    for (WalletAddress a : myAddressList) {
      if (address.equals(a.getAddress())) {
        // found
        return a;
      }
    }

    // not found
    return null;
  }


  /**
   * Create a map of addresses to the associated details.
   *
   * @return the mapping
   */
  public NavigableMap<String, WalletAddress> getAddresses() {
    NavigableMap<String, WalletAddress> map = new TreeMap<>();
    forEachAddress(p -> map.put(p.getAddress(), p));
    return Collections.unmodifiableNavigableMap(map);
  }


  public int getId() {
    return id;
  }


  /**
   * Get the wallet address instance that corresponds to the given public key.
   *
   * @param hexPublicKey the address
   *
   * @return the key pair, or null if it was not found
   */
  public WalletAddress getMatch(String hexPublicKey) {
    // Get the address list instance for thread safety, in-case another thread changes it
    List<WalletAddress> myAddressList = addressList;
    if (hexPublicKey == null) {
      return myAddressList.get(0);
    }

    for (WalletAddress a : myAddressList) {
      if (hexPublicKey.equals(a.getHexPublicKey())) {
        // found
        return a;
      }
    }

    // not found
    return null;
  }


  /**
   * Get the private key associated with the given public key.
   *
   * @param publicKey the public key (hex encoded)
   *
   * @return the private key, or null if not found
   */
  public PrivateKey getPrivateKey(String publicKey) {
    // Although the loop reads from addressList multiple times, it counts as a single usage as there is really just one
    // access to create the Iterator which is tied to the list instance. Hence this is thread safe.
    for (WalletAddress a : addressList) {
      if (publicKey.equals(a.getHexPublicKey())) {
        return a.getPrivateKey();
      }
    }

    return null;
  }


  @Override
  public int hashCode() {
    return addressList.hashCode();
  }


  public boolean isEmpty() {
    return addressList.isEmpty();
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    p.packArrayHeader(3);
    p.packInt(ENCODE_VERSION);
    p.packInt(id);
    // Get the address list instance for thread safety, in-case another thread changes it
    List<WalletAddress> myAddressList = addressList;
    p.packArrayHeader(myAddressList.size());
    for (WalletAddress wa : myAddressList) {
      wa.pack(p);
    }
  }


  public void setAddresses(List<WalletAddress> addresses) {
    this.addressList = addresses != null ? new CopyOnWriteArrayList<>(addresses) : new CopyOnWriteArrayList<>();
    addressList.forEach(wa -> wa.setWallet(this));
  }


  public void setId(int newId) {
    id = newId;
  }


  /**
   * The number of addresses in this wallet.
   *
   * @return the size
   */
  public int size() {
    return addressList.size();
  }


  /**
   * Stream through this wallet's addresses.
   *
   * @return a stream
   */
  public Stream<WalletAddress> stream() {
    return Collections.unmodifiableList(addressList).stream();
  }

}
