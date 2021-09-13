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
package io.setl.bc.pychain.transitions;

import io.setl.bc.Init;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Helper {

  private static final Logger logger = LoggerFactory.getLogger(Helper.class);

  public static Path getFirst(Path pathName) throws IOException {
    return Files.list(pathName).findFirst().get();
  }

  /**
   * findTransitionTestData.
   *
   * @param consumer      :
   * @param path          :
   * @throws IOException  :
   */
  public static void findTransitionTestData(Consumer<Transition> consumer, String path) throws IOException {

    Init.listFolders(Paths.get(path), pp -> {
      // Need to have 1 block file, and two numeric folders, n and n+1

      try {
        List<Integer> list = new ArrayList<>();
        Transition transition = new Transition();

        Files.list(pp).forEach(path2 -> {
          if (path2.toFile().isDirectory()) {
            try {
              int state = Integer.parseInt(path2.getFileName().toString());
              if (list.size() == 0) {
                transition.state0 = getFirst(path2);
              } else {
                transition.state1 = getFirst(path2);
              }
              list.add(state);
              // logger.info("Found:{}", state);
            } catch (NumberFormatException | IOException e) {
              // This isn't the folder we're looking for
            }
          } else if (!path2.toString().endsWith(".txt")) {
            // The block - hopefully
            transition.blockPath = path2;
          }
        });

        if (list.size() == 2 && transition.valid && transition.blockPath != null) {
          int r = list.get(1) - list.get(0);
          if (r == 1) {
            logger.info("Processing transition:{}", transition);
            consumer.accept(transition);
          } else if (r == -1) {
            // Swap paths
            Path statePath = transition.state0;
            transition.state0 = transition.state1;
            transition.state1 = statePath;
            logger.info("Processing transition:{}", transition);
            consumer.accept(transition);
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public static class Transition {

    public Path state0;
    public Path state1;
    public Path blockPath;
    public boolean valid = true;

    public String toString() {
      return state0.getParent().getFileName() + "->" + state1.getParent().getFileName();
    }
  }

}
