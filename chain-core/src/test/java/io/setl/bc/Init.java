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
package io.setl.bc;

import io.setl.bc.pychain.Defaults;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Created by aanten on 05/01/2017.
 */
public class Init {

  private static boolean setUpIsDone = false;


  /**
   * listFiles.
   *
   * @param path :
   * @param fc   :
   *
   * @throws IOException :
   */
  public static void listFiles(Path path, Consumer<Path> fc) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          listFiles(entry, fc);
        } else {
          fc.accept(entry);
        }
      }
    }
  }


  /**
   * listFolders.
   *
   * @param path :
   * @param fc   :
   *
   * @throws IOException :
   */
  public static void listFolders(Path path, Consumer<Path> fc) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      for (Path entry : stream) {
        if (Files.isDirectory(entry)) {
          fc.accept(entry);
          listFolders(entry, fc);
        }
      }
    }
  }


  /**
   * setup.
   */
  public static synchronized void setup(String s) {
    if (!setUpIsDone) {
      Defaults.reset();
      Defaults.init(s, 0, -1);
    }
    setUpIsDone = true;
  }


  public static void setup() {
    // setup("src/test/resources/pychain/snapshot3/");
    setup("src/test/resources/pychain/snapshot2000/");
  }
}
