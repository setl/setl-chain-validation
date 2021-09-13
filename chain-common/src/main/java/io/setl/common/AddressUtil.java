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
package io.setl.common;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.zip.CRC32;
import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.crypto.KeyGen;
import io.setl.crypto.provider.EdDsaPublicKey;
import io.setl.utils.ByteUtil;

public class AddressUtil {

  public static final Logger logger = LoggerFactory.getLogger(AddressUtil.class);

  private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

  private static final Base64.Encoder BASE64_NO_PADDING = Base64.getUrlEncoder().withoutPadding();

  /**
   * Should we use Base58 keys for contracts and Curve 25519 keys? Base58 keys are slower to create and harder to validate, have variable sizes and their
   * definition is much more complex. In short they are inferior in almost every way.
   */
  private static boolean useBase58 = Boolean.getBoolean("io.setl.common.address.base58");


  /**
   * Convert an Edwards curve public key to an address.
   *
   * @param keyBytes    the binary representation of an address
   * @param addressType the desired address type
   *
   * @return the address
   */
  private static String edsaToAddress(byte[] keyBytes, int addressType, Object... context) {
    MessageDigest d = Sha256Hash.newDigest();
    String hexPubKey = Hex.encode(keyBytes);

    // Contract addresses include a nonce
    if (addressType == AddressType.CONTRACT.getId() && context.length > 0) {
      hexPubKey += context[0];
    }

    String baseAddress = Hex.encode(d.digest(hexPubKey.getBytes(ByteUtil.BINCHARSET)));
    baseAddress = String.format("%02x%s", addressType, baseAddress.substring(0, 40));
    String checkSum = Hex.encode(d.digest(baseAddress.getBytes(ByteUtil.BINCHARSET)));
    checkSum = Hex.encode(d.digest(checkSum.getBytes(ByteUtil.BINCHARSET)));
    checkSum = checkSum.substring(0, 8);

    return Base58.encode(Hex.decode(baseAddress + checkSum));
  }


  /**
   * Get the type of an address. If the address is invalid, or the address-type cannot be identified, INVALID is returned.
   *
   * @param address the address
   *
   * @return the type or INVALID
   */
  @Nonnull
  public static AddressType getAddressType(String address) {
    if (!verifyAddress(address)) {
      return AddressType.INVALID;
    }

    char firstChar = address.charAt(0);
    if (firstChar == '1') {
      // Base-58 normal address
      return AddressType.NORMAL;
    }
    if (firstChar == '3') {
      // Base-58 contract address
      return AddressType.CONTRACT;
    }

    // Should be a base-64 address. We need to recover the first byte and rotate it right 2 bits.
    byte[] b64 = new byte[]{(byte) firstChar, (byte) address.charAt(1), (byte) 'A', (byte) '='};
    byte[] rawType = BASE64_DECODER.decode(b64);
    int typeId = ((rawType[0] & 0xfc) >>> 2) | ((rawType[0] & 0x03) << 6);
    return AddressType.get(typeId);
  }


  /**
   * Convert a hexadecimal encoded public key to an address.
   *
   * @param hexPubKey   the hexadecimal encoded public key
   * @param addressType the desired address type
   *
   * @return the address
   */
  public static String publicKeyToAddress(String hexPubKey, AddressType addressType, Object... context) {
    return publicKeyToAddress(KeyGen.getPublicKey(hexPubKey), addressType, context);
  }


  /**
   * Convert the binary representation of a public key to an address.
   *
   * @param keyBytes    the key bytes
   * @param addressType the address type
   *
   * @return the address
   */
  public static String publicKeyToAddress(byte[] keyBytes, int addressType, Object... context) {
    if (useBase58) {
      // EdDSA key or any contract
      if ((keyBytes.length == 32 && addressType == AddressType.NORMAL.getId()) || addressType == AddressType.CONTRACT.getId()) {
        return edsaToAddress(keyBytes, addressType, context);
      }
    }

    PublicKey publicKey = KeyGen.getPublicKey(keyBytes);
    return publicKeyToAddress(publicKey, AddressType.get(addressType), context);
  }


  /**
   * Convert a public key to an address.
   *
   * @param pubKey      Java.Security PublicKey
   * @param addressType Desired address type
   *
   * @return String Address.
   */
  public static String publicKeyToAddress(PublicKey pubKey, AddressType addressType, Object... context) {
    // Check for Edwards curve
    if (useBase58) {
      if (pubKey.getAlgorithm().equals("Ed25519") && addressType == AddressType.NORMAL) {
        byte[] keyBytes;
        try {
          keyBytes = EdDsaPublicKey.from(pubKey).getKeyBytes();
        } catch (InvalidKeySpecException e) {
          logger.debug("Invalid Ed25519 key", e);
          throw new IllegalArgumentException("Invalid Ed25519 key", e);
        }
        return edsaToAddress(keyBytes, addressType.getId(), context);
      }
      if (addressType == AddressType.CONTRACT) {
        return edsaToAddress(pubKey.getEncoded(), addressType.getId(), context);
      }
    }
    return toB64Address(pubKey.getEncoded(), addressType.getId(), context);
  }


  /**
   * Convert a public key to an address.
   *
   * @param hexPubKey   Hex encoded public key
   * @param addressType Desired address type
   *
   * @return the address
   */
  public static String publicKeyToAddress(String hexPubKey, int addressType, Object... context) {
    return publicKeyToAddress(Hex.decode(hexPubKey), addressType, context);
  }


  /**
   * Set whether to use the inferior base-58 representation or not. Base-58 is slower, harder to validate, more complicated to define, and has variable size.
   * They are inferior in every way except that, just like regular addresses, the first character indicates the address type. Really, the only reason to set
   * this is for testing the out-dated Base-58 address still work.
   *
   * @param newValue if true, use base 58 addresses from now on.
   *
   * @return the previous setting
   */
  public static boolean setUseBase58(boolean newValue) {
    boolean oldValue = useBase58;
    useBase58 = newValue;
    return oldValue;
  }


  /**
   * Standard method of converting a public key to an address.
   *
   * @param keyBytes    the public key
   * @param addressType the address's type
   * @param context     additional type appropriate context
   */
  private static String toB64Address(byte[] keyBytes, int addressType, Object... context) {
    MessageDigest sha256 = Sha256Hash.newDigest();

    // The output contains 1 byte for the address type, 20 bytes of the hash, and a 4 byte checksum
    byte[] output = new byte[25];

    // We rotate the address type so that the first character of the base 64 encoding changes as
    // the least significant bits of the address type change. Note: This assumes the address type
    // fits into 8 bits.
    int rotatedAddress = ((addressType & 0x3f) << 2) | ((addressType & 0xc0) >> 6);
    output[0] = (byte) rotatedAddress;

    sha256.update(keyBytes);

    // Contract addresses include a nonce
    if (addressType == AddressType.CONTRACT.getId() && context.length > 0) {
      long nonce = ((Number) context[0]).longValue();

      if (nonce < Integer.MAX_VALUE) {
        byte[] nonceBytes = new byte[4];
        nonceBytes[0] = (byte) (nonce >>> 24);
        nonceBytes[1] = (byte) (nonce >>> 16);
        nonceBytes[2] = (byte) (nonce >>> 8);
        nonceBytes[3] = (byte) nonce;
        sha256.update(nonceBytes);
      } else {
        byte[] nonceBytes = new byte[8];
        nonceBytes[0] = (byte) (nonce >>> 56);
        nonceBytes[1] = (byte) (nonce >>> 48);
        nonceBytes[2] = (byte) (nonce >>> 40);
        nonceBytes[3] = (byte) (nonce >>> 32);
        nonceBytes[4] = (byte) (nonce >>> 24);
        nonceBytes[5] = (byte) (nonce >>> 16);
        nonceBytes[6] = (byte) (nonce >>> 8);
        nonceBytes[7] = (byte) nonce;
        sha256.update(nonceBytes);
      }
    }

    byte[] hash = sha256.digest();
    System.arraycopy(hash, 0, output, 1, 20);

    CRC32 crc32 = new CRC32();
    crc32.update(output, 0, 21);
    long checkSum = crc32.getValue();

    // If one adds the CRC of a bit stream to that bit stream, the CRC of the combined bits is zero.
    // However, CRC32 is defined to reverse the bits in each octet of its input, reverse the entire
    // 32 bits of its output, and XOR its output with 0xFFFFFFFF. We have to undo this to get the
    // check that we want.
    checkSum ^= 0xffff_ffffL;
    output[21] = (byte) (0xff & checkSum);
    output[22] = (byte) (0xff & (checkSum >> 8));
    output[23] = (byte) (0xff & (checkSum >> 16));
    output[24] = (byte) (0xff & (checkSum >> 24));

    return BASE64_NO_PADDING.encodeToString(output);
  }


  /**
   * Verify if an address was derived from a specified public key. Note that for backwards compatibility an EdDSA key may be represented in either Base-58 or
   * Base-64, therefore one cannot verify an address simply be re-creating the address and comparing.
   *
   * @param address      the address.
   * @param hexPublicKey the hex encoding of either the X.509 encoded subject public key info or the raw bytes of an EdDSA curve point.
   * @param context      type specific context
   *
   * @return true if the public key and address match
   */
  public static boolean verify(String address, String hexPublicKey, Object... context) {
    return verify(address, hexPublicKey, getAddressType(address), context);
  }


  /**
   * Verify if an address was derived from a specified public key. Note that for backwards compatibility an EdDSA key may be represented in either Base-58 or
   * Base-64, therefore one cannot verify an address simply be re-creating the address and comparing.
   *
   * @param address      the address.
   * @param hexPublicKey the hex encoding of either the X.509 encoded subject public key info or the raw bytes of an EdDSA curve point.
   * @param addressType  the address type
   * @param context      type specific context
   *
   * @return true if the public key and address match
   */
  public static boolean verify(String address, String hexPublicKey, AddressType addressType, Object... context) {
    boolean addressBad = address == null || address.isEmpty();
    boolean keyBad = hexPublicKey == null || hexPublicKey.isEmpty();
    if (addressBad || keyBad) {
      // both bad means neither is specified.
      return addressBad && keyBad;
    }
    return verify(address, KeyGen.getPublicKey(hexPublicKey), addressType, context);
  }


  /**
   * Verify if an address was derived from a specified public key. Note that for backwards compatibility an EdDSA key may be represented in either Base-58 or
   * Base-64, therefore one cannot verify an address simply be re-creating the address and comparing.
   *
   * @param address     the address.
   * @param publicKey   the public key.
   * @param addressType the address type
   * @param context     type specific context
   *
   * @return true if the public key and address match
   */
  public static boolean verify(String address, PublicKey publicKey, AddressType addressType, Object... context) {
    boolean addressBad = address == null || address.isEmpty();
    boolean keyBad = publicKey == null;
    if (addressBad || keyBad) {
      // both bad means neither is specified.
      return addressBad && keyBad;
    }

    char typeIndicator = address.charAt(0);

    // Old style Edwards DSA key
    if ((typeIndicator == '1' && addressType == AddressType.NORMAL) || (typeIndicator == '3' && addressType == AddressType.CONTRACT)) {
      EdDsaPublicKey edDsaPublicKey;
      try {
        edDsaPublicKey = EdDsaPublicKey.from(publicKey);
      } catch (InvalidKeySpecException e) {
        logger.debug("Invalid EdDSA key", e);
        return false;
      }
      byte[] raw = edDsaPublicKey.getKeyBytes();
      String actualAddress = edsaToAddress(raw, addressType.getId(), context);
      return address.equals(actualAddress);
    }

    addressType = getAddressType(address);
    String actualAddress = toB64Address(publicKey.getEncoded(), addressType.getId(), context);
    return address.equals(actualAddress);
  }


  /**
   * Verify if an address was derived from a specified public key. Note that for backwards compatibility an EdDSA key may be represented in either Base-58 or
   * Base-64, therefore one cannot verify an address simply be re-creating the address and comparing.
   *
   * @param address     the address.
   * @param publicKey   the public key as either the X.509 encoded subject public key info or the raw bytes of an EdDSA curve point.
   * @param addressType the address type
   * @param context     type specific context
   *
   * @return true if the public key and address match
   */
  public static boolean verify(String address, byte[] publicKey, AddressType addressType, Object... context) {
    boolean addressBad = address == null || address.isEmpty();
    boolean keyBad = publicKey == null || publicKey.length == 0;
    if (addressBad || keyBad) {
      // both bad means neither is specified which is OK (happens with X-Chain TX packages)
      return addressBad && keyBad;
    }

    return verify(address, KeyGen.getPublicKey(publicKey), addressType, context);
  }


  /**
   * verifyAddress. Verify embedded checksum to ensure that this is a valid address.
   *
   * @param address the address to check
   *
   * @return true if the addess is valid
   */
  public static boolean verifyAddress(String address) {
    // Check for an Edwards curve address. These addresses start with either a '1' for Normal
    // addresses, or a '3' for Contract address with no other types being defined. These would
    // correspond to address types 53 and 55 for a NIST address, so lets hope those don't come
    // into use before we can remove Edwards curves.
    if ((address == null) || (address.isEmpty())) {
      return false;
    }

    char firstChar = address.charAt(0);
    if (firstChar == '1' || firstChar == '3') {
      return verifyEdDsaAddress(address);
    }

    CRC32 crc32 = new CRC32();
    try {
      byte[] bytes = BASE64_DECODER.decode(address);
      crc32.update(bytes);
      return crc32.getValue() == 0xffff_ffffL;
    } catch (IllegalArgumentException iae) {
      // Invalid Base64, so not a valid address.
      return false;
    }
  }


  /**
   * Verify an Edwards Digital Signature Algorithm address. These addresses are Base58 encoded and double SHAed.
   *
   * @param address the address
   *
   * @return true if the address has a valid checksum.
   */
  private static boolean verifyEdDsaAddress(String address) {

    if ((address == null) || (address.length() > 40)) {
      return false;
    }

    try {
      MessageDigest d = Sha256Hash.newDigest();

      String addr = Hex.encode(Base58.decode(address));
      String cs = addr.substring(addr.length() - 8);
      String vk = addr.substring(0, addr.length() - 8);

      String cscheck = Hex.encode(d.digest(vk.getBytes(ByteUtil.BINCHARSET)));
      cscheck = Hex.encode(d.digest(cscheck.getBytes(ByteUtil.BINCHARSET)));

      return cs.equalsIgnoreCase(cscheck.substring(0, 8));

    } catch (AddressFormatException e) {
      return false;
    }

  }


  /**
   * verifyPublicKey.
   *
   * @param publicKey :
   *
   * @return :
   */
  public static boolean verifyPublicKey(String publicKey) {
    if ((publicKey == null) || (publicKey.length() == 0)) {
      return false;
    }

    // We require at least 256 bits in a public key, so that is 64 hexadecimal digits, and must be in pairs
    boolean isNumeric = publicKey.matches("(?:\\p{XDigit}\\p{XDigit}){32,}");

    if (!isNumeric) {
      return false;
    }

    try {
      KeyGen.getPublicKey(publicKey);
      return true;
    } catch (IllegalArgumentException e) {
      // invalid public key
      return false;
    }
  }


  protected AddressUtil() {

  }

}
