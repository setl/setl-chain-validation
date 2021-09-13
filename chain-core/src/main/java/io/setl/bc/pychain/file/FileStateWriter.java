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
import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.Defaults;
import io.setl.bc.pychain.StateWriter;
import io.setl.bc.pychain.dbstore.DBStoreException;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.monolithic.ObjectEncodedState;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.msgpack.core.MessagePacker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "statemode", havingValue = "mono", matchIfMissing = true)
public class FileStateWriter implements StateWriter {

  @Override
  @SuppressFBWarnings("PATH_TRAVERSAL_IN")
  public void writeState(AbstractState state) throws DBStoreException {

    if (state instanceof ObjectEncodedState) {
      Path path = Paths.get(Defaults.get().getBalanceFolder()).resolve(state.getLoadedHash().toHexString());
      ObjectEncodedState oes = (ObjectEncodedState) state;
      Object[] fileEncodedState = {oes.getChainId(), oes.encode()};
      try (
          BufferedOutputStream bos = new BufferedOutputStream(
              new FileOutputStream(path.toFile()))
      ) {
        try (MessagePacker packer = MsgPackUtil.newPacker(bos)) {
          try {
            MsgPackUtil.packAnything(packer, fileEncodedState);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      } catch (IOException ioe) {
        throw new DBStoreException(ioe);
      }
      return;
    }
    throw new NotImplementedException();
  }


  @Override
  public void shutdown(AbstractState state) {
    // do nothing
  }
}
