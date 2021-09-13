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
package io.setl.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PBEParameter;
import org.bouncycastle.asn1.pkcs.PBES2Parameters;
import org.bouncycastle.asn1.pkcs.PBKDF2Params;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jcajce.spec.PBKDF2KeySpec;

import io.setl.crypto.provider.SetlProvider;

/**
 * Support for password based encryption to protect private keys.
 *
 * <p>For PBES2, RFC8018 mentions several weak ciphers which we do not support. From the RFC ciphers we only support: AES-128, AES-192, AES-256, DES-ede.
 *
 * <p>We extend PBES2 to add support for AES Padded Key Wrap.</p>
 *
 * @author Simon Greatrix on 11/11/2019.
 */
public class PBES {


  /**
   * Map of algorithm IDs to key sizes.
   */
  private static final Map<ASN1ObjectIdentifier, Integer> KEY_SIZES = Collections.unmodifiableMap(Map.of(
      PKCSObjectIdentifiers.des_EDE3_CBC, 192,
      NISTObjectIdentifiers.id_aes128_CBC, 128,
      NISTObjectIdentifiers.id_aes192_CBC, 192,
      NISTObjectIdentifiers.id_aes256_CBC, 256,
      NISTObjectIdentifiers.id_aes128_wrap_pad, 128,
      NISTObjectIdentifiers.id_aes192_wrap_pad, 192,
      NISTObjectIdentifiers.id_aes256_wrap_pad, 256
  ));

  /** Encryption and KDF algorithms used with PBE scheme 1. */
  private static final Set<ASN1ObjectIdentifier> PBES1_OIDS = Collections.unmodifiableSet(Set.of(
      PKCSObjectIdentifiers.pbeWithMD2AndDES_CBC,
      PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC,
      PKCSObjectIdentifiers.pbeWithMD2AndRC2_CBC,
      PKCSObjectIdentifiers.pbeWithMD5AndRC2_CBC,
      PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC,
      PKCSObjectIdentifiers.pbeWithSHA1AndRC2_CBC
  ));

  /** Encryption and KDF algorithms used with Public Key Cryptography Scheme #12. */
  private static final Set<ASN1ObjectIdentifier> PKCS12_OIDS = Collections.unmodifiableSet(Set.of(
      PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4,
      PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC4,
      PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC,
      PKCSObjectIdentifiers.pbeWithSHAAnd2_KeyTripleDES_CBC,
      PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC2_CBC,
      PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC2_CBC
  ));



  public enum EncryptionType {
    /** Best encryption, using AES in padded key wrapping with a 256 bit key. A SETL extension to PBES2. */
    AES_WRAP,

    /** Middling encryption, using AES in CBC mode with a 256 bit key. A standard PBES2 output format. */
    AES_CBC,

    /** Lower encryption, using triple key DES in CBC mode. A standard PBES1 output format. */
    TRIPLE_DES_CBC
  }


  /**
   * Decrypt a private key.
   *
   * @param password the password to decipher the private key.
   * @param pkInfo   the encrypted private key.
   *
   * @return the private key
   */
  public static PrivateKey decrypt(char[] password, EncryptedPrivateKeyInfo pkInfo) throws GeneralSecurityException {
    AlgorithmIdentifier algId = pkInfo.getEncryptionAlgorithm();
    ASN1ObjectIdentifier oid = algId.getAlgorithm();
    if (oid.equals(PKCSObjectIdentifiers.id_PBES2)) {
      return PEMReader.createPrivateKey(decryptPBES2(password, pkInfo));
    }

    if (PBES1_OIDS.contains(oid)) {
      return PEMReader.createPrivateKey(decryptPBES1(password, pkInfo));
    }

    if (PKCS12_OIDS.contains(oid)) {
      // PKCS12 uses the same parameter structure as PBES1
      return PEMReader.createPrivateKey(decryptPBES1(password, pkInfo));
    }

    throw new NoSuchAlgorithmException("No handling for key encryption algorithm: " + oid.getId());
  }


  private static byte[] decryptPBES1(char[] password, EncryptedPrivateKeyInfo pkInfo) throws GeneralSecurityException {
    AlgorithmIdentifier algId = pkInfo.getEncryptionAlgorithm();
    ASN1ObjectIdentifier oid = algId.getAlgorithm();

    PBEParameter pbeParameter = PBEParameter.getInstance(algId.getParameters());
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password, pbeParameter.getSalt(), pbeParameter.getIterationCount().intValueExact());

    SecretKeyFactory factory = SecretKeyFactory.getInstance(oid.getId());
    SecretKey secretKey = factory.generateSecret(pbeKeySpec);

    Cipher cipher = Cipher.getInstance(oid.getId());
    cipher.init(Cipher.DECRYPT_MODE, secretKey);

    return cipher.doFinal(pkInfo.getEncryptedData());
  }


  private static byte[] decryptPBES2(char[] password, EncryptedPrivateKeyInfo pkInfo) throws GeneralSecurityException {
    AlgorithmIdentifier encAlgId = pkInfo.getEncryptionAlgorithm();
    PBES2Parameters pbes2Parameters = PBES2Parameters.getInstance(encAlgId.getParameters());

    // The RFC requires the KDF be PBKDF2, but it is obviously intended to be pluggable.
    KeyDerivationFunc kdf = pbes2Parameters.getKeyDerivationFunc();
    if (!kdf.getAlgorithm().equals(PKCSObjectIdentifiers.id_PBKDF2)) {
      throw new NoSuchAlgorithmException("PBES2 key derivation algorithm is not PBKDF2 but: " + kdf.getAlgorithm().getId());
    }

    PBKDF2KeySpec params = getKDF(password, PBKDF2Params.getInstance(kdf.getParameters()), pbes2Parameters);
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2");
    SecretKey secretKey = factory.generateSecret(params);

    EncryptionScheme scheme = pbes2Parameters.getEncryptionScheme();
    ASN1ObjectIdentifier oid = scheme.getAlgorithm();
    Cipher cipher = Cipher.getInstance(oid.getId());
    if (scheme.getParameters() != null && !DERNull.INSTANCE.equals(scheme.getParameters())) {
      // It should always be an octet string
      ASN1OctetString octetString = ASN1OctetString.getInstance(scheme.getParameters());
      IvParameterSpec iv = new IvParameterSpec(octetString.getOctets());
      cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
    } else {
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
    }

    try {
      return cipher.doFinal(pkInfo.getEncryptedData());
    } catch (NegativeArraySizeException e) {
      // Bug in bouncy castle.
      throw new GeneralSecurityException("Incorrect cipher key", e);
    }
  }


  /**
   * Encrypt a private key.
   *
   * @param type       the encryption type to use
   * @param password   the password for the encryption
   * @param privateKey the private key
   *
   * @return encrypted private key
   */
  public static EncryptedPrivateKeyInfo encrypt(EncryptionType type, char[] password, PrivateKey privateKey) throws GeneralSecurityException {
    KeyFactory keyFactory = KeyFactory.getInstance(privateKey.getAlgorithm());
    PKCS8EncodedKeySpec keySpec = keyFactory.getKeySpec(privateKey, PKCS8EncodedKeySpec.class);

    switch (type) {
      case AES_CBC:
        return encryptAesCbc(password, keySpec);
      case AES_WRAP:
        return encryptAesWrap(password, keySpec);
      case TRIPLE_DES_CBC:
        return encrypt3DES(password, keySpec);
      default:
        throw new InternalError("ENUM not matched");
    }
  }


  private static EncryptedPrivateKeyInfo encrypt(
      char[] password,
      int rounds,
      int keyLengthBits,
      PKCS8EncodedKeySpec keySpec,
      String cipherName,
      ASN1ObjectIdentifier cipherId,
      IvParameterSpec cipherParamSpec,
      ASN1Encodable cipherParam,
      String prfName,
      ASN1ObjectIdentifier prfId
  ) throws GeneralSecurityException {
    byte[] salt = makeSalt();
    PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, rounds, keyLengthBits);
    SecretKeyFactory keyFac = SecretKeyFactory.getInstance(prfName);
    SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

    // Create cipher.
    Cipher pbeCipher = Cipher.getInstance(cipherName);
    if (cipherParamSpec != null) {
      pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, cipherParamSpec);
    } else {
      pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey);
    }
    byte[] cipherText = pbeCipher.doFinal(keySpec.getEncoded());

    // Create the EncryptedPrivateKeyInfo directly
    DERSequence info = new DERSequence(new ASN1Encodable[]{
        new DERSequence(new ASN1Encodable[]{
            // PBES2
            PKCSObjectIdentifiers.id_PBES2,
            new DERSequence(new ASN1Encodable[]{
                // KDF function
                new DERSequence(new ASN1Encodable[]{
                    // PBKDF2 with Hmac SHA512
                    PKCSObjectIdentifiers.id_PBKDF2,
                    new DERSequence(new ASN1Encodable[]{
                        new DEROctetString(salt),
                        new ASN1Integer(rounds),
                        new ASN1Integer(keyLengthBits / 8), //key length in octets
                        new DERSequence(new ASN1Encodable[]{
                            prfId,
                            DERNull.INSTANCE
                        })
                    })
                }),
                // Encrypt function
                new DERSequence(new ASN1Encodable[]{
                    cipherId,
                    cipherParam
                })
            })
        }),
        new DEROctetString(cipherText)
    });

    return EncryptedPrivateKeyInfo.getInstance(info);
  }


  private static EncryptedPrivateKeyInfo encrypt3DES(char[] password, PKCS8EncodedKeySpec keySpec) throws GeneralSecurityException {
    byte[] iv = new byte[8];
    SetlProvider.getSecureRandom().nextBytes(iv);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    DEROctetString octetString = new DEROctetString(iv);

    return encrypt(
        password,
        0x12345,
        192,
        keySpec,
        "DESede/CBC/PKCS5Padding",
        PKCSObjectIdentifiers.des_EDE3_CBC,
        ivParameterSpec,
        octetString,
        "PBKDF2WithHmacSHA1",
        PKCSObjectIdentifiers.id_hmacWithSHA1
    );
  }


  private static EncryptedPrivateKeyInfo encryptAesCbc(char[] password, PKCS8EncodedKeySpec keySpec) throws GeneralSecurityException {
    byte[] iv = new byte[16];
    SetlProvider.getSecureRandom().nextBytes(iv);
    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
    DEROctetString octetString = new DEROctetString(iv);

    return encrypt(
        password,
        0x12345,
        256,
        keySpec,
        "AES/CBC/PKCS5Padding",
        NISTObjectIdentifiers.id_aes256_CBC,
        ivParameterSpec,
        octetString,
        "PBKDF2WithHmacSHA512",
        PKCSObjectIdentifiers.id_hmacWithSHA512
    );
  }


  private static EncryptedPrivateKeyInfo encryptAesWrap(char[] password, PKCS8EncodedKeySpec keySpec) throws GeneralSecurityException {
    return encrypt(
        password,
        0x12345,
        256,
        keySpec,
        "AESWrapPad",
        NISTObjectIdentifiers.id_aes256_wrap_pad,
        null,
        DERNull.INSTANCE,
        "PBKDF2WithHmacSHA512",
        PKCSObjectIdentifiers.id_hmacWithSHA512
    );
  }


  private static PBKDF2KeySpec getKDF(char[] password, PBKDF2Params params, PBES2Parameters pbes2Parameters) throws GeneralSecurityException {
    // If the params are fully specified, we use the provided key length.
    Integer keySize;
    if (params.getKeyLength() != null) {
      // key length is in octets, but key size is in bits
      keySize = 8 * Integer.valueOf(params.getKeyLength().intValueExact());
    } else {
      // Need to infer the key length from the cipher algorithm
      ASN1ObjectIdentifier cipherId = pbes2Parameters.getEncryptionScheme().getAlgorithm();
      keySize = KEY_SIZES.get(cipherId);
      if (keySize == null) {
        throw new NoSuchAlgorithmException("PBES2 encryption algorithm " + cipherId.getId() + " is not supported.");
      }
    }

    // Add key size to PBKDF2 parameters
    return new PBKDF2KeySpec(password, params.getSalt(), params.getIterationCount().intValueExact(), keySize.intValue(), params.getPrf());
  }


  private static byte[] makeSalt() {
    byte[] salt = new byte[32];
    SetlProvider.getSecureRandom().nextBytes(salt);
    return salt;
  }

}
