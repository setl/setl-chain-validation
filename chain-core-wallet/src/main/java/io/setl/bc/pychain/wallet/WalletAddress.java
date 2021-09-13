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
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Hex;
import io.setl.crypto.provider.SetlProvider;
import io.setl.passwd.secret.AESWrap;
import io.setl.passwd.secret.KDF;
import io.setl.passwd.secret.SPKCS8EncodedKeySpec;
import io.setl.passwd.secret.SSecretKeySpec;
import io.setl.passwd.secret.SX509EncodedKeySpec;
import io.setl.passwd.vault.SharedSecret;
import io.setl.passwd.vault.SharedSecret.Type;
import io.setl.passwd.vault.VaultAccessor;

/**
 * A public/private key pair associated with a wallet, together with a transaction signing nonce.
 *
 * @author Simon Greatrix on 20/11/2017.
 */
public class WalletAddress implements MsgPackable {

  /**
   * The password vault key used to specify which AES key wrap key is used for new addresses.
   */
  public static final String VAULT_DEFAULT_WALLET_KEY_NAME = "wallet-default-key";

  /** Prefix used in the vault for key-wrapping keys to protect the private key. */
  public static final String WRAP_KEY_PREFIX = "wallet-";

  private static final Logger logger = LoggerFactory.getLogger(WalletAddress.class);


  /**
   * Get a key-wrapper cipher for the specified address and wrap ID.
   *
   * @param address the add
   * @param wrapId  the wrap ID
   *
   * @return the cipher
   */
  private static AESWrap getWrapper(String address, String wrapId) {
    VaultAccessor accessor = VaultAccessor.getInstance();
    try (SharedSecret secret = accessor.get(WRAP_KEY_PREFIX + wrapId)) {
      AESWrap aesWrap;
      if (secret.getType() == Type.SECRET_KEY) {
        // Secret key specified explicitly
        SSecretKeySpec aesKey = secret.secretKey();
        aesWrap = new AESWrap(aesKey);
        aesKey.destroy();
      } else {
        // Derive a unique key by combining the address and the bytes of the secret with a key derivation function.
        byte[] tmp = KDF.derive(address, "SETL Wallet Private Key Encryption Key", secret.binary(), 232, KDF.ALG_SHA_512_256);
        SSecretKeySpec aesKey = new SSecretKeySpec(tmp, 200, 32, "AES");
        aesWrap = new AESWrap(aesKey);
        aesKey.destroy();
      }
      return aesWrap;
    }
  }


  /**
   * Address as derived from the public key.
   */
  private String address;

  /**
   * Type of address.
   */
  private AddressType addressType = null;

  /**
   * Type of address.
   */
  private byte addressTypeValue;

  /**
   * The Key Type. For example, EC or EdDSA.
   */
  private String keyType;

  /**
   * Legal entity ID. Synonymous with Wallet ID.
   */
  private long leiId;

  /**
   * Number used once during signing.
   */
  private long nonce;

  /**
   * The actual private key.
   */
  private PrivateKey privateKey = null;

  /**
   * Encrypted bytes of the private key.
   */
  private byte[] privateKeyBytes;

  /**
   * The actual public key.
   */
  private PublicKey publicKey = null;

  /**
   * Bytes of the public key.
   */
  private byte[] publicKeyBytes;

  /**
   * Public key bytes as hexadecimal.
   */
  private String publicKeyHex = null;

  /**
   * The wallet which contains this address.
   */
  private Wallet wallet;

  /**
   * The ID of the AES Key Wrap cipher key.
   */
  private String wrapId;


  /**
   * Create a new instance.
   *
   * @param leiId       the legal entity ID
   * @param publicKey   the public key
   * @param privateKey  the private key
   * @param addressType the address type
   */
  public WalletAddress(int leiId, PublicKey publicKey, PrivateKey privateKey, AddressType addressType)
      throws GeneralSecurityException {
    this(leiId, publicKey, privateKey, addressType, 0);
  }


  /**
   * Create a new instance.
   *
   * @param leiId       the legal entity ID
   * @param publicKey   the public key
   * @param privateKey  the private key
   * @param addressType the address type
   * @param nonce       initial nonce
   */
  public WalletAddress(int leiId, PublicKey publicKey, PrivateKey privateKey, AddressType addressType, long nonce)
      throws GeneralSecurityException {
    this.leiId = leiId;
    this.addressType = addressType;
    addressTypeValue = (byte) addressType.getId();
    keyType = publicKey.getAlgorithm();
    if (!keyType.equals(privateKey.getAlgorithm())) {
      throw new IllegalArgumentException(
          "Public key has type " + keyType + " but private key has type " + privateKey.getAlgorithm());
    }
    if (!"X.509".equals(publicKey.getFormat())) {
      throw new IllegalArgumentException("Public key has format " + publicKey.getFormat() + " not X.509");
    }
    if (!"PKCS#8".equals(privateKey.getFormat())) {
      throw new IllegalArgumentException("Private key has format " + publicKey.getFormat() + " not PKCS#8");
    }

    this.publicKey = publicKey;
    this.privateKey = privateKey;
    publicKeyBytes = publicKey.getEncoded();
    address = AddressUtil.publicKeyToAddress(publicKey, addressType);
    this.nonce = nonce;

    // Choose a wrap ID at random
    VaultAccessor accessor = VaultAccessor.getInstance();
    SharedSecret secret = accessor.get(VAULT_DEFAULT_WALLET_KEY_NAME);
    String[] wrapIds = secret.plain().trim().split("\\s*,\\s*");
    wrapId = wrapIds[SetlProvider.getSecureRandom().nextInt(wrapIds.length)].intern();

    // Wrap the private key
    AESWrap aesWrap = getWrapper(address, wrapId);
    privateKeyBytes = aesWrap.wrap(privateKey.getEncoded());
  }


  public WalletAddress(int leiId, KeyPair keyPair, AddressType addressType) throws GeneralSecurityException {
    this(leiId, keyPair.getPublic(), keyPair.getPrivate(), addressType);
  }


  /**
   * Load a new wallet address from a complete secure specification. The address will have been created previously and its private key AES wrapped.
   *
   * @param leiID       the wallet ID
   * @param addressType the address type
   * @param address     the address
   * @param nonce       the nonce
   * @param keyType     the key's cryptographic type
   * @param wrapId      the AES wrapper key ID
   * @param publicKey   the public key
   * @param privateKey  the encrypted private key
   */
  // squid:S00107 - this constructor needs more than 7 parameters
  @SuppressWarnings("squid:S00107")
  public WalletAddress(
      int leiID,
      int addressType,
      String address,
      long nonce,
      String keyType,
      String wrapId,
      byte[] publicKey,
      byte[] privateKey
  ) {
    this.leiId = leiID;
    this.addressTypeValue = (byte) addressType;
    this.address = address;
    this.nonce = nonce;
    this.keyType = keyType;
    this.wrapId = wrapId;
    publicKeyBytes = publicKey.clone();
    privateKeyBytes = privateKey.clone();
  }


  /**
   * Create an UNSAFE wallet address. This wallet address does not have an encrypted public key.
   *
   * @param leiId            the legal entity ID
   * @param addressTypeValue the address type code
   * @param keyType          the key type
   * @param nonce            the nonce
   * @param b64PrivateKey    base-64 representation of unencrypted private key
   * @param b64PublicKey     base-64 representation of public key
   */
  public WalletAddress(
      long leiId,
      int addressTypeValue,
      long nonce,
      String keyType,
      String b64PublicKey,
      String b64PrivateKey
  ) throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] publicBytes = Base64.getDecoder().decode(b64PublicKey);
    byte[] privateBytes = Base64.getDecoder().decode(b64PrivateKey);
    this.address = AddressUtil.publicKeyToAddress(publicBytes, addressTypeValue);
    this.addressTypeValue = (byte) addressTypeValue;
    this.keyType = keyType;
    this.leiId = leiId;
    this.nonce = nonce;
    KeyFactory keyFactory = KeyFactory.getInstance(keyType);
    try (SPKCS8EncodedKeySpec spec = new SPKCS8EncodedKeySpec(privateBytes)) {
      privateKey = keyFactory.generatePrivate(spec);
    }
    this.privateKeyBytes = privateBytes;
    this.publicKeyBytes = publicBytes;
    this.publicKeyHex = Hex.encode(publicBytes);
  }


  /**
   * Read this wallet address from a message packed data stream.
   *
   * @param unpacker the data stream
   */
  public WalletAddress(MessageUnpacker unpacker) throws IOException {
    unpack(unpacker);
  }


  /**
   * Encode this address in binary format.
   *
   * @return this address
   */
  public byte[] encode() {
    try (MessageBufferPacker pp = MsgPackUtil.newBufferPacker()) {
      pack(pp);
      return pp.toByteArray();
    } catch (IOException ioe) {
      throw new AssertionError("Allegedly in-memory operation produced IO Exception", ioe);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WalletAddress that = (WalletAddress) o;

    if (addressTypeValue != that.addressTypeValue) {
      return false;
    }
    if (leiId != that.leiId) {
      return false;
    }
    if (!address.equals(that.address)) {
      return false;
    }
    if (!keyType.equals(that.keyType)) {
      return false;
    }
    if (!Arrays.equals(privateKeyBytes, that.privateKeyBytes)) {
      return false;
    }
    if (!Arrays.equals(publicKeyBytes, that.publicKeyBytes)) {
      return false;
    }
    return wrapId.equals(that.wrapId);
  }


  /**
   * Get the address.
   *
   * @return the address
   */
  public String getAddress() {
    return address;
  }


  /**
   * Get the address type.
   *
   * @return the address type
   */
  public AddressType getAddressType() {
    if (addressType == null) {
      addressType = AddressType.get(addressTypeValue & 0xff);
    }
    return addressType;
  }


  /**
   * Get the public key in hexadecimal.
   *
   * @return the public key
   */
  public String getHexPublicKey() {
    if (publicKeyHex == null) {
      PublicKey key = getPublicKey();
      publicKeyHex = Hex.encode(key.getEncoded());
    }
    return publicKeyHex;
  }


  /**
   * Get the cryptographic key type.
   *
   * @return the key type
   */
  public String getKeyType() {
    return keyType;
  }


  /**
   * Get the legal entity ID. (Also known as the wallet ID).
   *
   * @return the LEI ID
   */
  public long getLeiId() {
    return leiId;
  }


  /**
   * Get the transaction signing nonce. This value may be lower last used nonce value for this address.
   *
   * @return the nonce
   */
  public long getNonce() {
    return nonce;
  }


  /**
   * Get the private key, decoding and decrypting it if necessary.
   *
   * @return the private key
   */
  public PrivateKey getPrivateKey() {
    if (privateKey == null) {
      try {
        AESWrap wrapper = getWrapper(address, wrapId);
        byte[] plainBytes = wrapper.unwrap(privateKeyBytes);

        KeyFactory keyFactory = KeyFactory.getInstance(keyType);
        try (SPKCS8EncodedKeySpec spec = new SPKCS8EncodedKeySpec(plainBytes)) {
          Arrays.fill(plainBytes, (byte) 0);
          privateKey = keyFactory.generatePrivate(spec);
        }
      } catch (GeneralSecurityException e) {
        logger.error("Failed to decrypt private key with key type {} and binary value {}.",
            keyType, Base64.getEncoder().encodeToString(privateKeyBytes), e
        );
        throw new AssertionError("Private key of type " + keyType + " could not be decoded.", e);
      }
    }
    return privateKey;
  }


  /**
   * Get the encrypted representation of the private key.
   *
   * @return the encrypted representation
   */
  public byte[] getPrivateKeyBytes() {
    return privateKeyBytes != null ? privateKeyBytes.clone() : null;
  }


  /**
   * Get the public key, decoding it if necessary.
   *
   * @return the public key
   */
  public PublicKey getPublicKey() {
    if (publicKey == null && publicKeyBytes != null) {
      try {
        KeyFactory keyFactory = KeyFactory.getInstance(keyType);
        SX509EncodedKeySpec spec = new SX509EncodedKeySpec(publicKeyBytes);
        publicKey = keyFactory.generatePublic(spec);
        spec.destroy();
      } catch (GeneralSecurityException e) {
        logger.error("Failed to decrypt public key with key type {} and binary value {}.",
            keyType, Base64.getEncoder().encodeToString(publicKeyBytes), e
        );
        throw new AssertionError("Public key of type " + keyType + " could not be decoded.", e);
      }
    }
    return publicKey;
  }


  /**
   * Get the X.509 encoded representation of the public key.
   *
   * @return the binary representation
   */
  public byte[] getPublicKeyBytes() {
    return publicKeyBytes != null ? publicKeyBytes.clone() : null;
  }


  public Wallet getWallet() {
    return wallet;
  }


  /**
   * Get the ID of the cipher key used to wrap the private key.
   *
   * @return the ID of the wrapping cipher key
   */
  public String getWrapId() {
    return wrapId;
  }


  @Override
  public int hashCode() {
    int result = address.hashCode();
    result = 31 * result + (int) addressTypeValue;
    result = 31 * result + keyType.hashCode();
    result = 31 * result + (int) (leiId ^ (leiId >>> 32));
    result = 31 * result + Arrays.hashCode(privateKeyBytes);
    result = 31 * result + Arrays.hashCode(publicKeyBytes);
    result = 31 * result + wrapId.hashCode();
    return result;
  }


  /**
   * Pack this to the provided packer.
   *
   * @param pp the packer to pack to
   */
  @Override
  public void pack(MessagePacker pp) throws IOException {
    pp.packArrayHeader(8);
    pp.packLong(leiId);
    pp.packInt(addressTypeValue);
    pp.packString(address);
    pp.packLong(nonce);
    pp.packString(keyType);
    pp.packString(wrapId);
    pp.packBinaryHeader(publicKeyBytes.length);
    pp.writePayload(publicKeyBytes);
    pp.packBinaryHeader(privateKeyBytes.length);
    pp.writePayload(privateKeyBytes);
  }


  /**
   * Set the nonce. This is normally only called by the DAO layer to keep the nonce in keep with the DB.
   *
   * @param newNonce the new nonce.
   */
  public void setNonce(long newNonce) {
    nonce = newNonce;
  }


  /**
   * Get the wallet that contains this address, if known.
   *
   * @param wallet the wallet which contains this address
   */
  public void setWallet(Wallet wallet) {
    // The wallet's ID may not match the address's LEID. It should match if the wallet is loaded from the DB, but if it is loaded from the file there is no
    // such requirement.
    this.wallet = wallet;
  }


  /**
   * Change the AES wrapper on the private key.
   */
  public void setWrapId() throws GeneralSecurityException {
    setWrapId(null);
  }


  /**
   * Change the AES wrapper on the private key.
   *
   * @param newWrapId the new wrapper's ID
   */
  public void setWrapId(String newWrapId) throws GeneralSecurityException {
    if (newWrapId == null || newWrapId.isBlank()) {
      // Choose a wrap ID at random
      VaultAccessor accessor = VaultAccessor.getInstance();
      SharedSecret secret = accessor.get(VAULT_DEFAULT_WALLET_KEY_NAME);
      String[] wrapIds = secret.plain().trim().split("\\s*,\\s*");
      newWrapId = wrapIds[SetlProvider.getSecureRandom().nextInt(wrapIds.length)].intern();
    }

    AESWrap wrapper = getWrapper(address, newWrapId);
    PrivateKey myPrivateKey = getPrivateKey();
    privateKeyBytes = wrapper.wrap(myPrivateKey.getEncoded());
    wrapId = newWrapId;
  }


  /**
   * Read the values for this address from its message packed form.
   *
   * @param u the message unpacker
   */
  private void unpack(MessageUnpacker u) throws IOException {
    int l = u.unpackArrayHeader();
    if (l != 8) {
      throw new IOException("Expected 8 items in WalletAddress, not " + l);
    }

    leiId = u.unpackLong();
    addressTypeValue = (byte) u.unpackInt();
    address = u.unpackString();
    nonce = u.unpackInt();
    keyType = u.unpackString();
    wrapId = u.unpackString();
    int len = u.unpackBinaryHeader();
    publicKeyBytes = new byte[len];
    u.readPayload(publicKeyBytes);
    len = u.unpackBinaryHeader();
    privateKeyBytes = new byte[len];
    u.readPayload(privateKeyBytes);
  }


  /**
   * Read the values for this address from its message packed form.
   *
   * @param va the message unpacker
   */
  private void unpack(MPWrappedArray va) throws IOException {
    int l = va.size();
    if (l != 8) {
      throw new IOException("Expected 8 items in WalletAddress, not " + l);
    }

    leiId = va.asLong(0);
    addressTypeValue = (byte) va.asInt(1);
    address = va.asString(2);
    nonce = va.asInt(3);
    keyType = va.asString(4);
    wrapId = va.asString(5);
    publicKeyBytes = va.asByte(6);
    privateKeyBytes = va.asByte(7);
  }

}
