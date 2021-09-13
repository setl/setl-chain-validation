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
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.crypto.KeyGen;
import io.setl.crypto.KeyGen.Type;
import io.setl.utils.Base64;
import io.setl.utils.ByteUtil;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;


/**
 * @deprecated Should use WalletAddress instead.
 */
@Deprecated
public class PublicPrivatePairForAddress {

  /**
   * Create a new instance using an Edwards 25519 key-pair.
   *
   * @return the new instance
   */
  public static PublicPrivatePairForAddress create() {
    KeyPair keys = Type.ED25519.generate();
    return new PublicPrivatePairForAddress(keys, 1);
  }


  /**
   * Decode a list of encoded pairs.
   *
   * @param va           the encoded pair data
   * @param signNodeMode is this pair a Sig Node, or a regular address?
   *
   * @return the list of pairs
   */
  public static List<PublicPrivatePairForAddress> decode(MPWrappedArray va, boolean signNodeMode) throws IOException {
    List<PublicPrivatePairForAddress> r = new ArrayList<>();

    for (int i = 0, l = va.size(); i < l; i++) {
      MPWrappedArray vaa = va.asWrapped(i);

      if (signNodeMode) {
        r.add(new PublicPrivatePairForAddress(Base64.decode(vaa.asString(0)), vaa.asInt(2), vaa.asString(1)));
      } else {
        r.add(new PublicPrivatePairForAddress(Base64.decode(vaa.asString(0)), vaa.asInt(1),
            vaa.asString(2)));
      }
    }

    return r;
  }


  /**
   * Decode a list of encoded pairs for a regular address.
   *
   * @param va the encoded pair data
   *
   * @return the list of pairs
   */
  public static List<PublicPrivatePairForAddress> decode(MPWrappedArray va) throws IOException {

    return decode(va, false);
  }


  /**
   * Encode a list of pairs.
   *
   * @param addressList  the list of pairs
   * @param signNodeMode is this for Sig Nodes or regular addresses?
   *
   * @return the encoded data
   */
  public static Object[] encode(List<PublicPrivatePairForAddress> addressList, boolean signNodeMode) {

    int l = addressList.size();
    Object[][] encoded = new Object[l][];
    for (int i = 0; i < l; i++) {
      PublicPrivatePairForAddress entry = addressList.get(i);

      if (signNodeMode) {
        encoded[i] = new Object[]{Base64.encode(entry.privateKey.getEncoded()), entry.hexPublicKey, entry.nonce};
      } else {
        encoded[i] = new Object[]{Base64.encode(entry.privateKey.getEncoded()), entry.nonce, entry.hexPublicKey};
      }
    }
    return encoded;
  }


  private String address;

  private String hexPublicKey;

  private long nonce;

  private PrivateKey privateKey;

  private PublicKey publicKey;


  public PublicPrivatePairForAddress(KeyPair pair, long nonce) {

    this(pair.getPublic(), pair.getPrivate(), nonce);
  }


  /**
   * Constructor.
   *
   * @param publicKey  : PublicKey
   * @param privateKey : PrivateKey
   * @param nonce      :
   */
  public PublicPrivatePairForAddress(PublicKey publicKey, PrivateKey privateKey, long nonce) {

    this.privateKey = privateKey;
    this.publicKey = publicKey;
    this.nonce = nonce;

    this.hexPublicKey = ByteUtil.bytesToHex(publicKey.getEncoded());

    address = AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL);
  }


  private PublicPrivatePairForAddress(byte[] privateKey, long nonce, String hexPublicKey) {

    this.nonce = nonce;
    this.hexPublicKey = hexPublicKey;
    final byte[] publicKeyBytes = ByteUtil.hexToBytes(hexPublicKey);

    this.privateKey = KeyGen.getPrivateKey(privateKey);
    this.publicKey = KeyGen.getPublicKey(publicKeyBytes);
    address = AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL);
  }


  /**
   * Convert this to a wallet address.
   *
   * @param walletId the wallet ID
   *
   * @return the wallet address version of this
   */
  public WalletAddress convert(int walletId) throws GeneralSecurityException {

    PublicKey pubKey = getPublicKey();
    PrivateKey privKey = getPrivateKey();

    return new WalletAddress(walletId, pubKey, privKey, AddressType.NORMAL, nonce);
  }


  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (!(o instanceof PublicPrivatePairForAddress)) {
      return false;
    }

    PublicPrivatePairForAddress that = (PublicPrivatePairForAddress) o;

    if (!address.equals(that.address)) {
      return false;
    }
    if (!hexPublicKey.equals(that.hexPublicKey)) {
      return false;
    }
    return privateKey.equals(that.privateKey);
  }


  public String getAddress() {

    return address;
  }


  public String getHexPublicKey() {

    return hexPublicKey;
  }


  public long getNonce() {

    return nonce;
  }


  public PrivateKey getPrivateKey() {

    return privateKey;
  }


  public PublicKey getPublicKey() {

    return publicKey;
  }


  @Override
  public int hashCode() {

    int result = address.hashCode();
    result = 31 * result + hexPublicKey.hashCode();
    result = 31 * result + privateKey.hashCode();
    return result;
  }


  public synchronized void incrementNonce() {

    nonce++;
  }


  @Override
  public String toString() {

    return address + "(" + nonce + ")";
  }
}
