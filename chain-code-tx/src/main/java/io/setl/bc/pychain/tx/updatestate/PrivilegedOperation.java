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

import static io.setl.common.StringUtils.logSafe;

import java.security.PublicKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.tx.PrivilegedOperationTx;
import io.setl.bc.pychain.tx.updatestate.UpdateStateUtils.ReturnTuple;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.TypeSafeList;
import io.setl.common.TypeSafeMap;
import io.setl.crypto.KeyGen;

/**
 * @author Simon Greatrix on 07/07/2020.
 */
public class PrivilegedOperation extends TxProcessor<PrivilegedOperationTx> {

  @Override
  protected Class<PrivilegedOperationTx> fulfills() {
    return PrivilegedOperationTx.class;
  }


  private void tryChangeConfiguration(PrivilegedOperationTx txi, StateSnapshot snapshot, boolean checkOnly) {
    if (checkOnly) {
      return;
    }
    Map<String, Object> config = txi.getOperationInput();
    config.forEach(snapshot::setConfigValue);
  }


  @Override
  protected ReturnTuple tryChecks(PrivilegedOperationTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    String address = txi.getFromAddress();
    if (AddressUtil.getAddressType(address) != AddressType.PRIVILEGED) {
      throw fail("Only privileged address can run privileged operations");
    }
    PrivilegedKey privilegedKey = snapshot.getPrivilegedKey(address);
    if (privilegedKey == null) {
      throw fail("Privileged key {0} is unrecognised.", address);
    }
    if (privilegedKey.getExpiry() < updateTime) {
      throw fail("Privileged key {0} expired at {1}", address, DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(privilegedKey.getExpiry())));
    }
    if (!privilegedKey.getHexPublicKey().equals(txi.getFromPublicKey())) {
      throw fail("Specified public key for address does not match");
    }
    if (!privilegedKey.getPermissions().contains(txi.getOperationName())) {
      throw fail("Privileged key {0} is not permitted to do operation {1}", address, logSafe(txi.getOperationName()));
    }

    // We do not validate the operation input at this point

    return super.tryChecks(txi, snapshot, updateTime, checkOnly);
  }


  private void tryCreatePrivilegedUser(PrivilegedOperationTx txi, StateSnapshot snapshot, boolean checkOnly) throws TxFailedException {
    TypeSafeMap config = new TypeSafeMap(txi.getOperationInput());
    String name = config.getString("name");
    if (name == null) {
      throw fail("Privileged Users 'name' field is required");
    }
    String publicKey = config.getString("hexPublicKey");
    if (publicKey == null) {
      throw fail("Privileged Users 'hexPublicKey' field is required");
    }
    try {
      KeyGen.getPublicKey(publicKey);
    } catch (IllegalArgumentException e) {
      throw fail("Unusable public key: " + logSafe(publicKey));
    }

    long expiry = config.getLong("expiry", Long.MAX_VALUE);
    List<String> permissions = config.getList(TypeSafeList.F_STRING, "permissions");
    if (permissions == null) {
      permissions = Collections.emptyList();
    }
    String address = AddressUtil.publicKeyToAddress(publicKey, AddressType.PRIVILEGED);

    // Verify the privileged key is not duplicating an address nor a name
    if (snapshot.getPrivilegedKey(address) != null) {
      throw fail("State already contains a privileged key with address {0}", address);
    }
    if (snapshot.getPrivilegedKey(name) != null) {
      throw fail("State already contains a privileged key with name {0}", logSafe(address));
    }

    // Can create new privileged key
    PrivilegedKey privilegedKey = new PrivilegedKey(address, expiry, publicKey, name, permissions, snapshot.getHeight());
    if (checkOnly) {
      return;
    }

    snapshot.setPrivilegedKey(address, privilegedKey);

    MutableMerkle<AddressEntry> merkle = snapshot.getMerkle(AddressEntry.class);
    if (!merkle.itemExists(address)) {
      AddressEntry.addDefaultAddressEntry(merkle, address, snapshot.getVersion());
    }
  }


  private void tryModifyPrivilegedUser(PrivilegedOperationTx txi, StateSnapshot snapshot, boolean checkOnly) throws TxFailedException {
    TypeSafeMap config = new TypeSafeMap(txi.getOperationInput());
    String keyId = config.getString("id");
    if (keyId == null) {
      throw fail("Privileged Users identifier is required for update");
    }
    PrivilegedKey privilegedKey = snapshot.getPrivilegedKey(keyId);
    if (privilegedKey == null) {
      throw fail("No privileged key with ID: {0} exists", logSafe(keyId));
    }

    long expiry = config.getLong("expiry", privilegedKey.getExpiry());
    String publicKey = config.getString("hexPublicKey");
    if (publicKey != null) {
      PublicKey actualKey = KeyGen.getPublicKey(publicKey);
      privilegedKey = privilegedKey.setKey(expiry, actualKey, snapshot.getHeight());
    }

    List<String> permissions = config.getList(TypeSafeList.F_STRING, "deletedPermissions");
    if (permissions != null) {
      for (String p : permissions) {
        privilegedKey = privilegedKey.deletePermission(p, snapshot.getHeight());
      }
    }
    permissions = config.getList(TypeSafeList.F_STRING, "insertedPermissions");
    if (permissions != null) {
      for (String p : permissions) {
        privilegedKey = privilegedKey.addPermission(p, snapshot.getHeight());
      }
    }

    // can apply update
    if (!checkOnly) {
      snapshot.setPrivilegedKey(privilegedKey.getAddress(), privilegedKey);
    }
  }


  @Override
  protected ReturnTuple tryUpdate(PrivilegedOperationTx txi, StateSnapshot snapshot, long updateTime, boolean checkOnly) throws TxFailedException {
    switch (txi.getOperationName()) {
      case "Create Privileged User":
        tryCreatePrivilegedUser(txi, snapshot, checkOnly);
        break;
      case "Modify Privileged User":
        tryModifyPrivilegedUser(txi, snapshot, checkOnly);
        break;
      case "Change Configuration":
        tryChangeConfiguration(txi, snapshot, checkOnly);
        break;
      default:
        throw fail("Unknown privileged operation: {0}", logSafe(txi.getOperationName()));
    }
    return null;
  }

}
