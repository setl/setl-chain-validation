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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.spec.EdDSAParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.setl.common.Hex;
import io.setl.common.StringUtils;
import io.setl.crypto.provider.EdDsaPrivateKey;
import io.setl.crypto.provider.EdDsaPublicKey;
import io.setl.crypto.provider.SetlProvider;
import io.setl.util.LRUCache;

/**
 * Generate a new key pair of the specified type.
 *
 * @author Simon Greatrix on 01/12/2017.
 */
public class KeyGen {

  /**
   * Aliases for key types.
   */
  private static final Map<String, Type> ALIASES;

  /**
   * Default key type to create.
   */
  private static final Type DEFAULT = Type.EC_NIST_P256;

  private static final Logger logger = LoggerFactory.getLogger(KeyGen.class);

  private static final LRUCache<String, PublicKey> PUBLIC_KEYS = new LRUCache<>(1000, k -> {
    try {
      return getPublicKey(Hex.decode(k));
    } catch (IllegalArgumentException e) {
      logger.info("Invalid public key passed in as: \"{}\"", StringUtils.logSafe(k));
    }
    return null;
  });

  /**
   * Source of randomness for keys.
   */
  static SecureRandom defaultRandom = SetlProvider.getSecureRandom();



  /**
   * Key types we can generate easily.
   */
  public enum Type {
    /**
     * Keys for secp256r1. Rated at 128 bits.
     */
    EC_NIST_P256() {
      @Override
      public KeyPair generate(SecureRandom secureRandom) {
        try {
          KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
          generator.initialize(new ECGenParameterSpec("NIST P-256"), secureRandom);
          return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
          throw new AssertionError("Required cryptography primitives not working", e);
        }
      }


      public String[] names() {
        return new String[]{"EC/NIST P-256", "EC/secp256r1", "EC/prime256v1"};
      }
    },

    /**
     * Keys for secp384r1. Rated at 192 bits.
     */
    EC_NIST_P384() {
      @Override
      public KeyPair generate(SecureRandom secureRandom) {
        try {
          KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
          generator.initialize(new ECGenParameterSpec("NIST P-384"), secureRandom);
          return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
          throw new AssertionError("Required cryptography primitives not working", e);
        }
      }


      public String[] names() {
        return new String[]{"EC/NIST P-384", "EC/secp384r1"};
      }

    },

    /**
     * Keys for secp521r1. Rated at 256 bits.
     */
    EC_NIST_P521() {
      @Override
      public KeyPair generate(SecureRandom secureRandom) {
        try {
          KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
          generator.initialize(new ECGenParameterSpec("NIST P-521"), secureRandom);
          return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
          throw new AssertionError("Required cryptography primitives not working", e);
        }
      }


      public String[] names() {
        return new String[]{"EC/NIST P-521", "EC/secp521r1"};
      }

    },

    /**
     * Edwards curve 25519 keys. Rated at 128 bits.
     */
    ED25519() {
      @Override
      public KeyPair generate(SecureRandom secureRandom) {
        try {
          KeyPairGenerator generator = KeyPairGenerator.getInstance("EdDSA");
          generator.initialize(new EdDSAParameterSpec("Ed25519"), secureRandom);
          return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
          throw new AssertionError("Required cryptography primitives not working", e);
        }
      }


      public String[] names() {
        return new String[]{"EdDSA", "EdDSA/C25519", "ed25519-sha-512"};
      }
    };


    /**
     * Generate a key pair.
     *
     * @return the key pair
     */
    public KeyPair generate() {
      return generate(defaultRandom);
    }


    /**
     * Generate a key pair.
     *
     * @param secureRandom the random source to use
     *
     * @return the key pair
     */
    public abstract KeyPair generate(SecureRandom secureRandom);

    /**
     * Name and alias for this generator.
     *
     * @return name and aliases
     */
    public abstract String[] names();
  }


  /**
   * Generate a key pair using the default algorithm.
   *
   * @return A key pair
   */
  public static KeyPair generateKeyPair() {
    return DEFAULT.generate();
  }


  /**
   * Generate a key pair using the named algorithm.
   *
   * @param name the name
   *
   * @return a key pair
   */
  public static KeyPair generateKeyPair(String name) {
    return generateKeyPair(name, null);
  }


  /**
   * Generate a key pair using the named algorithm and the provided secure random number generator.
   *
   * @param name   the name
   * @param random a secure PRNG
   *
   * @return a key pair
   */
  public static KeyPair generateKeyPair(String name, SecureRandom random) {
    if (random == null) {
      random = defaultRandom;
    }
    Type t = ALIASES.get(name);
    if (t == null) {
      // General handling for EC/curve name
      if (name.startsWith("EC/")) {
        String curve = name.substring(3);
        try {
          KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
          generator.initialize(new ECGenParameterSpec(curve), random);
          return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
          throw new IllegalArgumentException("Unknown key pair type: " + name, e);
        }
      }

      // Out of ideas for what it could be
      throw new IllegalArgumentException("Unknown key pair type: " + name);
    }

    return t.generate(random);
  }


  /**
   * This method guesses what kind of encoded key has been provided and tries to create a suitable private key instance.
   *
   * @param bytes the encoded key
   *
   * @return the private key
   */
  public static PrivateKey getPrivateKey(byte[] bytes) {
    PrivateKeyInfo info;
    PKCS8EncodedKeySpec spec;
    if (bytes.length != 32) {
      // A normal key which we assume to be PKCS#8 encoded so we identify via the algorithm's OID.
      info = PrivateKeyInfo.getInstance(bytes);
      spec = new PKCS8EncodedKeySpec(bytes);
    } else {
      // An Edwards curve key which is not X.509 encoded
      if (SetlProvider.isNativeMode()) {
        try {
          return new EdDsaPrivateKey(bytes);
        } catch (InvalidKeySpecException e) {
          // the only way it can be invalid is if it is not 32 bytes, so this never happens
          throw new IllegalArgumentException("Invalid EdDSA key", e);
        }
      }

      // Wrap bytes in private key info.
      try {
        info = new PrivateKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), new DEROctetString(bytes));
        spec = new PKCS8EncodedKeySpec(info.getEncoded());
      } catch (IOException e) {
        // presumably an invalid key
        throw new IllegalArgumentException("Invalid EdDSA key", e);
      }
    }

    ASN1ObjectIdentifier alg = info.getPrivateKeyAlgorithm().getAlgorithm();

    try {
      KeyFactory kf = KeyFactory.getInstance(alg.toString());
      return kf.generatePrivate(spec);
    } catch (NoSuchAlgorithmException nsa) {
      throw new AssertionError("No key factory for OID." + alg, nsa);
    } catch (InvalidKeySpecException iks) {
      throw new IllegalArgumentException("Bad key", iks);
    }
  }


  /**
   * Get a public key that corresponds to the hexadecimal representation of its X.509 encoding.
   *
   * @param hexBytes the hexadecimal bytes
   *
   * @return the public key.
   */
  public static PublicKey getPublicKey(String hexBytes) {
    PublicKey publicKey = PUBLIC_KEYS.get(hexBytes);
    if (publicKey == null) {
      throw new IllegalArgumentException("Invalid key \"" + StringUtils.logSafe(hexBytes) + "\"");
    }
    return publicKey;
  }


  /**
   * This method guesses what kind of encoded key has been provided and tries to create a suitable public key
   * instance.
   *
   * @param bytes the encoded key
   *
   * @return the public key
   */
  public static PublicKey getPublicKey(byte[] bytes) {
    if (bytes == null) {
      return null;
    }

    X509EncodedKeySpec spec;
    SubjectPublicKeyInfo info;

    if (bytes.length != 32) {
      // A normal key which we assume to be X.509 encoded so we identify via the algorithm's OID
      spec = new X509EncodedKeySpec(bytes);
      info = SubjectPublicKeyInfo.getInstance(bytes);
    } else {
      // An Edwards curve key which is not X.509 encoded
      if (SetlProvider.isNativeMode()) {
        try {
          return new EdDsaPublicKey(bytes);
        } catch (InvalidKeySpecException e) {
          // it's invalid if the required bits are not set.
          throw new IllegalArgumentException("Invalid Ed25519 public key", e);
        }
      }

      // Wrap bytes in subject public key info
      info = new SubjectPublicKeyInfo(new AlgorithmIdentifier(EdECObjectIdentifiers.id_Ed25519), bytes);
      try {
        spec = new X509EncodedKeySpec(info.getEncoded());
      } catch (IOException e) {
        // Not sure when bouncy-castle throws this :-(
        throw new IllegalArgumentException("Invalid Ed25519 public key", e);
      }
    }

    ASN1ObjectIdentifier alg = info.getAlgorithm().getAlgorithm();

    try {
      KeyFactory kf = KeyFactory.getInstance(alg.toString());
      return kf.generatePublic(spec);
    } catch (NoSuchAlgorithmException nsa) {
      throw new IllegalArgumentException("No key factory for OID." + alg, nsa);
    } catch (InvalidKeySpecException iks) {
      throw new IllegalArgumentException("Bad key", iks);
    }
  }


  static {
    Map<String, Type> aliases = new HashMap<>();
    for (Type t : Type.values()) {
      aliases.put(t.name(), t);
      aliases.put(t.name().toLowerCase(), t);
      for (String s : t.names()) {
        aliases.put(s, t);
        aliases.put(s.toLowerCase(), t);
      }
    }
    ALIASES = Collections.unmodifiableMap(aliases);

    // To be able to process Edwards curves using the native Donna library, we need our provider set up.
    SetlProvider.install();
  }

}
