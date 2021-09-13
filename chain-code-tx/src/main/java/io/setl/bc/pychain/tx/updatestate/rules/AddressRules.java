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
package io.setl.bc.pychain.tx.updatestate.rules;

import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_POA_EXERCISES;
import static io.setl.common.CommonPy.AuthorisedAddressConstants.AP_TX_LIST;
import static io.setl.common.StringUtils.logSafe;

import java.text.MessageFormat;
import java.util.Collection;

import io.setl.bc.pychain.state.MutableMerkle;
import io.setl.bc.pychain.state.StateSnapshot;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.tx.updatestate.TxFailedException;
import io.setl.common.AddressType;
import io.setl.common.AddressUtil;
import io.setl.common.CommonPy.TxType;

/**
 * @author Simon Greatrix on 20/01/2020.
 */
public class AddressRules {

  /**
   * Check an entry is a valid address.
   *
   * @param address the address to validate
   */
  public static String checkAddress(String address) throws TxFailedException {
    if (!AddressUtil.verifyAddress(address)) {
      throw new TxFailedException("Invalid address \"" + logSafe(address) + "\"");
    }
    return address;
  }


  public static String checkAddress(String address, String publicKey) throws TxFailedException {
    checkAddress(address);
    AddressType addressType = AddressUtil.getAddressType(address);
    if (AddressUtil.verify(address, publicKey, addressType)) {
      return address;
    }
    throw new TxFailedException("Address \"" + logSafe(address) + "\" and public key \"" + logSafe(publicKey) + "\" do not match");
  }


  /**
   * Check that an address is valid and, if it must be registered in advance, exists in state.
   *
   * @param snapshot the snapshot
   * @param address  the address
   */
  public static String checkAddress(StateSnapshot snapshot, String address) throws TxFailedException {
    if (!AddressUtil.verifyAddress(address)) {
      throw new TxFailedException("Invalid address \"" + logSafe(address) + "\"");
    }
    AddressType type = AddressUtil.getAddressType(address);
    if (type == AddressType.INVALID) {
      throw new TxFailedException("Invalid address type: \"" + logSafe(address) + "\"");
    }
    switch (type) {
      case NORMAL:
        if (snapshot.getStateConfig().getMustRegister()) {
          // addresses have to exist in advance
          MutableMerkle<AddressEntry> merkle = snapshot.getMerkle(AddressEntry.class);
          if (merkle.itemExists(address)) {
            // address exists
            return address;
          }
        } else {
          // no need to pre-register, so OK
          return address;
        }
        break;
      case PRIVILEGED:
        if (snapshot.getPrivilegedKey(address) != null) {
          // address exists as a privileged key
          return address;
        }
        break;
      default:
        // other address type, so decidedly not OK
        break;
    }
    throw new TxFailedException("Address \"" + logSafe(address) + "\" does not exist");
  }


  /**
   * Verify Address permissions for the 'from' address.
   */
  public static void checkAddressPermissions(StateSnapshot stateSnapshot, String fromAddress, TxType txID, long requiredAddressPermissions)
      throws TxFailedException {
    // Privileged address have all TX permissions
    AddressType addressType = AddressUtil.getAddressType(fromAddress);
    if (addressType == AddressType.PRIVILEGED) {
      return;
    }

    // Normal addresses can run TXs. Others cannot.
    if (addressType != AddressType.NORMAL) {
      throw new TxFailedException("Address of type \"" + addressType + "\" cannot execute transactions");
    }

    if (!stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
      return;
    }

    long keyPermission = stateSnapshot.getAddressPermissions(fromAddress);
    boolean hasPermission = stateSnapshot.canUseTx(fromAddress, txID);
    if ((!hasPermission) && ((keyPermission & requiredAddressPermissions) == 0)) {
      throw new TxFailedException("Inadequate Address permissions");
    }
  }


  /**
   * Check that addresses are valid, and, if they must be registered in advance, exist in state.
   *
   * @param snapshot  the snapshot
   * @param addresses the addresses
   */
  public static void checkAddresses(StateSnapshot snapshot, Collection<String> addresses) throws TxFailedException {
    for (String a : addresses) {
      checkAddress(snapshot, a);
    }
  }


  /**
   * Check that addresses are valid. If they do not exist in state then if "must register" is true the transaction fails, but if "must register" is false then
   * the addresses are created.
   *
   * @param snapshot  the snapshot
   * @param addresses the addresses
   */
  public static void checkAndCreateAddresses(StateSnapshot snapshot, Collection<String> addresses) throws TxFailedException {
    for (String a : addresses) {
      if (!AddressUtil.verifyAddress(a)) {
        throw new TxFailedException("Invalid address \"" + logSafe(a) + "\"");
      }
    }

    // Do addresses have to exist?
    boolean mustRegister = snapshot.getStateConfig().getMustRegister();

    // Verify if the addresses exist and, if they don't, either create them or abort the TX
    MutableMerkle<AddressEntry> merkle = snapshot.getMerkle(AddressEntry.class);
    for (String a : addresses) {
      if (!merkle.itemExists(a)) {
        if (mustRegister) {
          // TX fails as the address does not exist
          throw new TxFailedException("Address \"" + logSafe(a) + "\" does not exist");
        } else {
          AddressType type = AddressUtil.getAddressType(a);
          if (type == AddressType.NORMAL) {
            // Address should be created
            AddressEntry.addDefaultAddressEntry(merkle, a, snapshot.getVersion());
          } else {
            throw new TxFailedException("Cannot create an address of type " + type + " for address \"" + logSafe(a) + "\".");
          }
        }
      }
    }
  }


  /**
   * Check address permissions for both the Attorney address and the POA Address.
   */
  public static void checkPoaAddressPermissions(
      StateSnapshot stateSnapshot, String attorneyAddress, String poaAddress, TxType poaTxId,
      TxType effectiveTxID, long requiredPoaAddressPermissions
  ) throws TxFailedException {
    if (!stateSnapshot.getStateConfig().getAuthoriseByAddress()) {
      return;
    }

    // Address permissions.
    long keyPermission = stateSnapshot.getAddressPermissions(attorneyAddress);
    boolean hasPermission = stateSnapshot.canUseTx(attorneyAddress, poaTxId);
    if ((!hasPermission) && ((keyPermission & AP_POA_EXERCISES) == 0)) {
      throw new TxFailedException(MessageFormat.format("No POA_Exercise permission for Attorney Address {0}", attorneyAddress));
    }

    // poaAddress permissions.
    if (!poaAddress.equalsIgnoreCase(attorneyAddress)) {
      keyPermission = stateSnapshot.getAddressPermissions(poaAddress);
    }
    hasPermission = stateSnapshot.canUseTx(poaAddress, effectiveTxID);
    if ((!hasPermission) && ((keyPermission & requiredPoaAddressPermissions) == 0)) {
      throw new TxFailedException(MessageFormat.format("No Tx permission for POA Address {0}", poaAddress));
    }
  }


  /**
   * Create default address entries for all missing addresses, if addresses do not have to be registered in advance. Does not check that the addresses are
   * validated, as that should already have been checked.
   *
   * @param snapshot  the snapshot
   * @param addresses the addresses
   */
  public static void createDefaultAddresses(StateSnapshot snapshot, Collection<String> addresses) {
    if (snapshot.getStateConfig().getMustRegister()) {
      return;
    }

    // create any addresses that are referenced
    MutableMerkle<AddressEntry> addressMerkle = snapshot.getMerkle(AddressEntry.class);
    for (String address : addresses) {
      if (!addressMerkle.itemExists(address)) {
        AddressEntry.addDefaultAddressEntry(addressMerkle, address, snapshot.getVersion());
      }
    }
  }

}
