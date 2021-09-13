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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("setl.defaults")
public class Defaults {

  private static Defaults instance = null;


  private static void ensureFolder(File f) {
    if (!f.isDirectory() && !f.mkdirs()) {
      throw new IllegalStateException("Cannot create folder " + f.getAbsolutePath());
    }
  }


  /**
   * get()
   *
   * @return Defaults.
   */
  public static Defaults get() {
    if (instance == null) {
      instance = new Defaults("/setl/testnets_java/20/", 0, -1);
    }
    return instance;
  }


  /**
   * init.
   *
   * @param basedir :
   */
  public static void init(String basedir, int nodeId, int chainId) {
    if (instance != null) {
      throw new IllegalStateException("Defaults already initialised");
    }
    if (!basedir.endsWith(File.separator)) {
      basedir = basedir.concat(File.separator);
    }

    instance = new Defaults(basedir, nodeId, chainId);
  }


  public static void reset() {
    instance = null;
  }


  private final String basedir;

  private final int chainId;

  private final Integer nodeId;


  /**
   * New instance, which replaces the global instance.
   *
   * @param basedir the base folder
   * @param nodeId  the local node's ID
   * @param chainId the chain's ID
   */
  public Defaults(
      @Value("${basedir:#{null}}") String basedir,
      @Value("${sourcenodeid:#{null}}") Integer nodeId,
      @Value("${chainid}") int chainId
  ) {
    if (basedir != null) {
      this.basedir = basedir + (basedir.endsWith(File.separator) ? "" : File.separator);
    } else {
      this.basedir = null;
    }
    this.nodeId = nodeId;
    this.chainId = chainId;

    instance = this;
  }


  public String getAbsolutePath(String fileName) {
    return fileName.startsWith(File.separator) ? fileName : String.format("%s%s", getBaseFolder(), fileName);
  }


  public String getBalanceFolder() {
    return String.format("%s%d/Balance/", getBaseFolder(), getNodeId());
  }

  public String getBalanceSubFolder() {
    return String.format("%s%d/Balance/Sub", Defaults.get().basedir, nodeId);
  }


  public String getBaseFolder() {
    if (basedir == null) {
      throw new IllegalArgumentException("basedir was not set");
    }
    return basedir;
  }


  public String getBlockFolder() {
    return String.format("%s%d/Block/", getBaseFolder(), getNodeId());
  }


  public int getChainId() {
    return chainId;
  }


  public int getNodeId() {
    if (nodeId == null) {
      throw new IllegalArgumentException("sourcenodeid was not set");
    }
    return nodeId;
  }


  /**
   * Create required Block/Balance folders.
   */
  @SuppressFBWarnings("PATH_TRAVERSAL_IN") // Paths are not specified by an external user
  public void validateFolders() {
    ensureFolder(new File(getBaseFolder() + getNodeId() + File.separator + "Block"));
    ensureFolder(new File(getBaseFolder() + getNodeId() + File.separator + "Balance"));
  }
}
