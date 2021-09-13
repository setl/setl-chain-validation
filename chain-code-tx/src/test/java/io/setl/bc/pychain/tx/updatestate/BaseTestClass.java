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
package io.setl.bc.pychain.tx.updatestate;

import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.file.FileStateLoader;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.wallet.TestWallets;
import io.setl.bc.serialise.SerialiseToByte;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.Hex;
import io.setl.common.Pair;
import io.setl.crypto.KeyGen;
import io.setl.crypto.KeyGen.Type;
import io.setl.passwd.vault.TestVault;
import io.setl.passwd.vault.VaultAccessor;

public class BaseTestClass {

  private static final String[][] keyPairDefinitions = {
      {
          "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDx8qawU4x1AF7mWbbvx/CognvTtiIkQbOw5jSl52jFMCadAS+16mwYoK+pjs2kiR1YzbByxCS3rZwiJCr3l3ag==",
          "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDFfdBsDS/xJfOWJvZff7MNiDbp17ccxZmgDmXOuO+BHA=="
      }
  };



  /** An entity with a key pair and address. */
  public static class Entity {

    public String address;

    public String privateHex;

    public PrivateKey privateKey;

    public String publicHex;

    public PublicKey publicKey;


    /** New instance. */
    public Entity() {
      KeyPair keyPair = Type.ED25519.generate();
      publicKey = keyPair.getPublic();
      privateKey = keyPair.getPrivate();
      publicHex = Hex.encode(publicKey.getEncoded());
      privateHex = Hex.encode(privateKey.getEncoded());
      address = AddressUtil.publicKeyToAddress(publicKey, AddressType.NORMAL);
    }

  }


  @AfterClass
  public static void clearVault() {
    TestVault.setInstance().clear();
  }


  @BeforeClass
  public static void createVault() {
    TestWallets.reset();
  }


  /**
   * getRandomAddress.
   *
   * @return :
   */
  public static String getRandomAddress() {
    return AddressUtil.publicKeyToAddress(getRandomKeyPair().getPublic(), AddressType.NORMAL);
  }


  /**
   * Create a random key pair with the keys represented in hexadecimal.
   *
   * @return the key pair
   */
  public static Pair<String, String> getRandomHexKeyPair() {
    KeyPair keyPair = getRandomKeyPair();
    return new Pair<>(Hex.encode(keyPair.getPublic().getEncoded()), Hex.encode(keyPair.getPrivate().getEncoded()));
  }


  /**
   * Create a random public key.
   *
   * @return a hex encoded public key
   */
  public static KeyPair getRandomKeyPair() {
    return Type.ED25519.generate();
  }


  /**
   * Create a random public key.
   *
   * @return a hex encoded public key
   */
  public static String getRandomPublicKey() {
    return Hex.encode(getRandomKeyPair().getPublic().getEncoded());
  }

  public MessageDigest digest;

  public FileStateLoader fileStateLoaded;

  public SerialiseToByte hashSerialiser;

  private KeyPair[] keyPairs;


  /**
   * Create a key pair from a list of key definitions. The key pair for any index will always be the same.
   *
   * @param i the key pair's index
   *
   * @return the key pair.
   */
  public KeyPair getStandardKeyPair(int i) {
    if (keyPairs == null) {
      keyPairs = new KeyPair[keyPairDefinitions.length];
    }
    if (keyPairs[i] == null) {
      Base64.Decoder decoder = Base64.getDecoder();
      keyPairs[i] = new KeyPair(
          KeyGen.getPublicKey(decoder.decode(keyPairDefinitions[i][0])),
          KeyGen.getPrivateKey(decoder.decode(keyPairDefinitions[i][1]))
      );
    }
    return keyPairs[i];
  }


  @Before
  public void setUp() throws Exception {
    digest = MessageDigest.getInstance("SHA-256");
    hashSerialiser = new HashSerialisation();

    Defaults.reset();
    fileStateLoaded = new FileStateLoader();
  }


  @After
  public void tearDown() throws Exception {
    Defaults.reset();
  }

}
