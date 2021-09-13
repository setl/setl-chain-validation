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
package io.setl.bc.pychain;

import io.setl.bc.pychain.ConfigMap.Setting;
import io.setl.bc.pychain.msgpack.MsgPackable;
import io.setl.bc.pychain.state.StateBase;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.function.Function;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;

/**
 * Support for message digest which know what kind of digest they are calculating to support configuration of digest algorithms and recording how digests
 * were calculated in state.
 *
 * @author Simon Greatrix on 2019-02-13.
 */
public class Digest {

  /** Identifier for the NIST SHA-256 algorithm. */
  public static final int TYPE_SHA_256 = 1;

  /** Identifier for the NIST SHA-3 256-bit algorithm. */
  public static final int TYPE_SHA_3_256 = 3;

  /** Identifier for the NIST SHA-512 algorithm truncated to 256 bits. */
  public static final int TYPE_SHA_512_256 = 2;

  /** Names of digests for use in configuration. */
  private static final String[] DIGEST_NAMES = new String[]{"Test", "SHA-256", "SHA-512/256", "SHA3-256"};

  /**
   * Setting function that can convert a configuration value to a digest identifier.
   */
  static final Function<Object, Integer> PARSE_TYPE = val -> {
    // If null, use default.
    if (val == null) {
      return TYPE_SHA_512_256;
    }
    // If a name, match it against standard names
    if (val instanceof String) {
      return typeForName((String) val);
    }

    // If a number, use its integer value.
    if (val instanceof Number) {
      int i = ((Number) val).intValue();
      if (0 <= i && i < DIGEST_NAMES.length) {
        return i;
      }
    }
    throw new IllegalArgumentException("Unknown preferred digest type (" + val.getClass() + "): " + val.toString());
  };

  /**
   * The state configuration which specifies which digest should be used to calculate hashes in state.
   */
  public static final Setting<Integer> DIGEST_SETTING = new Setting<>("preferredDigestType", PARSE_TYPE);


  /**
   * Create a new instance of a specified type.
   *
   * @param type the digest type required. Must be one of the standard identifiers.
   *
   * @return the Digest instance
   */
  public static Digest create(int type) {
    if (type < 0 || type >= DIGEST_NAMES.length) {
      throw new IllegalArgumentException("Unknown digest type " + type);
    }
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance(DIGEST_NAMES[type]);
    } catch (NoSuchAlgorithmException e) {
      throw new InternalError("Message digest \"" + DIGEST_NAMES[type] + "\" was not available", e);
    }
    return new Digest(type, messageDigest);
  }


  /**
   * Currently recommend SHA-512/256.
   *
   * @return the recommended digest type
   */
  public static int getRecommended(StateBase state) {
    return state.getConfigValue(DIGEST_SETTING, TYPE_SHA_512_256);
  }


  /**
   * Currently recommend SHA-512/256.
   *
   * @return the recommended digest type
   */
  public static int getRecommended() {
    return TYPE_SHA_512_256;
  }


  /**
   * Get the digest name for a given type identifier.
   *
   * @param type the identifier
   *
   * @return the matching name
   */
  public static String name(int type) {
    if (type < 0 || type >= DIGEST_NAMES.length) {
      return "Unknown (" + type + ")";
    }
    return DIGEST_NAMES[type];
  }


  /**
   * Get the type identifier for a given digest name.
   *
   * @param name the name
   *
   * @return the identifier
   */
  public static int typeForName(String name) {
    if (name == null || name.isEmpty() || name.equalsIgnoreCase("null") || name.equalsIgnoreCase("default")) {
      return getRecommended();
    }
    for (int i = 0; i < DIGEST_NAMES.length; i++) {
      if (DIGEST_NAMES[i].equalsIgnoreCase(name)) {
        return i;
      }
    }
    try {
      int val = Integer.parseInt(name);
      if (val < 0 || val >= DIGEST_NAMES.length) {
        throw new IllegalArgumentException("Unrecognised hash name: " + name);
      }
      return val;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Unrecognised hash name: " + name);
    }
  }


  static {
    // ensure Bouncy Castle is loaded.
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  /** The actual message digest implementation. */
  private final MessageDigest messageDigest;

  /** The type identifier for the message digest. */
  private final int type;


  /**
   * New instance.
   *
   * @param type          the digest type
   * @param messageDigest the digest implementation
   */
  private Digest(int type, MessageDigest messageDigest) {
    this.type = type;
    this.messageDigest = messageDigest;
  }


  /**
   * Calculate the Hash of the data which has been fed into this digest.
   *
   * @return the Hash
   */
  public Hash digest() {
    return new Hash(messageDigest.digest());
  }


  /**
   * Append data into this digest and calculate the Hash of all the data inserted.
   *
   * @param data the final data
   *
   * @return the resulting Hash
   */
  public Hash digest(byte[] data) {
    return new Hash(messageDigest.digest(data));
  }


  /**
   * Append a message-packable object to this digest's data and calculate the resulting Hash.
   *
   * @param data the data to append
   *
   * @return the resulting Hash
   */
  public Hash digest(MsgPackable data) {
    try (MessageBufferPacker output = MessagePack.newDefaultBufferPacker()) {
      data.pack(output);
      return new Hash(messageDigest.digest(output.toByteArray()));
    } catch (IOException ioe) {
      throw new InternalError("IOException without IO", ioe);
    } catch (Exception e) {
      throw new InternalError("Packing failed", e);
    }
  }


  /**
   * Get the digest type identifier.
   *
   * @return the type identifier
   */
  public int getType() {
    return type;
  }


  /**
   * Append the supplied data to this digest.
   *
   * @param data the data to append
   */
  public void update(byte data) {
    messageDigest.update(data);
  }


  /**
   * Append the supplied data to this digest.
   *
   * @param data the data to append
   */
  public void update(byte[] data) {
    messageDigest.update(data);
  }


  /**
   * Append the supplied data to this digest.
   *
   * @param data the data to append
   * @param off  the offest into the byte array
   * @param len  the number of bytes to append
   */
  public void update(byte[] data, int off, int len) {
    messageDigest.update(data, off, len);
  }
}
