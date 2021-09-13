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


import io.setl.bc.pychain.PrivateKeySource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class KeyRepository {

  private final StateManager stateManager;
  private PrivateKeySource signNodePrivateKeySource;
  private final List<String> ownedPublicKeys = new ArrayList<>();

  public KeyRepository(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  public PrivateKeySource getSignNodePrivateKeySource() {
    return signNodePrivateKeySource;
  }

  public void setSignNodePrivateKeySource(PrivateKeySource signNodePrivateKeySource) {
    this.signNodePrivateKeySource = signNodePrivateKeySource;
    if (stateManager.getState() != null) {
      updateOwnedPublicKeys();
    }
  }


  public synchronized List<String> getOwnedPublicKeys() {
    // Create a copy of the list to avoid concurrency issues
    return Collections.unmodifiableList(new ArrayList<>(ownedPublicKeys));
  }


  public synchronized void updateOwnedPublicKeys() {
    ownedPublicKeys.clear();
    stateManager.getSortedSignerKeys().forEach(pk -> {
      if (signNodePrivateKeySource.getPrivateKey(pk) != null) {
        ownedPublicKeys.add(pk);
      }
    });
  }

}
