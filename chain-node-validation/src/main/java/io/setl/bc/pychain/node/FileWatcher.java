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
package io.setl.bc.pychain.node;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

public class FileWatcher implements DisposableBean {

  public static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);



  public interface Listener {

    void changed(String type, String file);
  }

  private final File walletFile;

  private final Kind<?>[] watchEventKinds;

  private Listener listener = null;

  private boolean run = true;

  private WatchService watchService = null;


  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public FileWatcher(String walletFile, Kind<?>... watchEventKinds) {
    this.walletFile = new File(walletFile);
    this.watchEventKinds = watchEventKinds;

  }


  @Override
  public void destroy() {
    run = false;
    try {
      watchService.close();
    } catch (IOException e) {
      logger.error("", e);
    }
  }


  private void run() {
    WatchKey key;
    while (run) {
      try {
        key = watchService.poll(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        logger.error("", e);
        Thread.currentThread().interrupt();
        continue;
      }

      if (key == null) {
        continue;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        String affectedFile = event.context().toString().trim();
        if (affectedFile.equals(walletFile.getName())) {
          listener.changed(event.kind().name(), affectedFile);
        }

      }
      key.reset();
    }
  }


  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public void watch(Listener listener) throws IOException {
    this.listener = listener;
    watchService = FileSystems.getDefault().newWatchService();
    Path path = Paths.get(walletFile.getParent());
    path.register(watchService, watchEventKinds);
    Thread thread = new Thread(this::run);
    thread.setName("WalletFileWatcher");
    thread.setDaemon(true);
    thread.start();

  }
}

