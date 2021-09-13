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
package io.setl.bc.pychain.file;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.pychain.wallet.OldWallet;
import io.setl.bc.pychain.wallet.Wallet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

public class WalletLoader {

  private static final byte[] VERSION_PREFIX = "SETL_00002".getBytes(StandardCharsets.US_ASCII);


  private Wallet loadWallet(byte[] contents) throws IOException {
    for (int i = 0; i < VERSION_PREFIX.length; i++) {
      if (contents[i] != VERSION_PREFIX[i]) {
        throw new IllegalArgumentException("Bad version prefix: Saw " + (contents[i] & 0xff) + " at " + i);
      }
    }

    final int off = VERSION_PREFIX.length;
    final int len = contents.length - off;
    int version;
    try (MessageUnpacker unpacker = MsgPackUtil.newUnpacker(contents, off, len)) {
      int arraySize = unpacker.unpackArrayHeader();
      if (arraySize == 0) {
        throw new IllegalArgumentException("Encoded wallet has 0 data fields.");
      }
      version = unpacker.unpackInt();
    }
    try (MessageUnpacker unpacker2 = MsgPackUtil.newUnpacker(contents, off, len)) {
      if (version == 2) {
        try {
          MPWrappedArray va = MsgPackUtil.unpackWrapped(unpacker2);
          return OldWallet.fromList(va).asWallet();
        } catch (GeneralSecurityException e) {
          throw new IOException("Invalid security requirements", e);
        }
      } else {
        return new Wallet(unpacker2);
      }
    }
  }


  /**
   * Load a wallet from the designated file path.
   *
   * @param path the path
   *
   * @return the wallet
   */
  public Wallet loadWallet(Path path) throws IOException {
    byte[] contents = Files.readAllBytes(path);
    return loadWallet(contents);
  }


  /**
   * Load a wallet from a file.
   *
   * @param fname the name of the input file
   *
   * @return the Wallet
   *
   * @throws IOException if the load operation fails
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public Wallet loadWalletFromFile(String fname) throws IOException {
    File file = new File(fname);
    byte[] contents = Files.readAllBytes(file.toPath());
    return loadWallet(contents);
  }


  private void saveWallet(OutputStream bin, Wallet wallet) throws IOException {
    bin.write(VERSION_PREFIX);
    try (MessagePacker packer2 = MsgPackUtil.newPacker(bin)) {
      wallet.pack(packer2);
    }
  }


  /**
   * Save a wallet to a designated file path.
   *
   * @param path   the output path
   * @param wallet the wallet
   */
  public void saveWallet(Path path, Wallet wallet) throws IOException {
    try (OutputStream output = Files.newOutputStream(path)) {
      saveWallet(output, wallet);
    }
  }


  /**
   * Save the wallet to the specified file.
   *
   * @param fname  the name of the output file
   * @param wallet the wallet to save
   *
   * @throws IOException if the save operation fails
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_OUT") // Path traversal is intended here.
  public void saveWalletToFile(String fname, Wallet wallet) throws IOException {
    try (OutputStream output = new FileOutputStream(fname)) {
      saveWallet(output, wallet);
    }
  }
}
