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
package io.setl.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// "squid:S1118" is Sonar Lint's requirement that utility classes have private constructors.
@SuppressWarnings("squid:S1118")
public class CommonPy {


  public enum ItemType {
    PROPOSAL("proposal"),
    PROPVOTES("propvotes"),
    TRANSACTIONS("transactions");

    static final Map<String, ItemType> lookup = new HashMap<>();


    public static ItemType byId(String id) {
      return lookup.getOrDefault(id, null);
    }


    static {
      for (ItemType it : ItemType.values()) {
        lookup.put(it.id, it);
      }
    }

    private final String id;


    ItemType(String id) {
      this.id = id;
    }


    public String getId() {
      return id;
    }
  }



  @SuppressWarnings("squid:S00115") // Naming convention contravention. Prototype consistent.
  public enum NodeType {
    Unknown(-1),
    Validation(0x00),
    Witness(0x10),
    Wallet(0x20),
    Report(0x30),
    Cobalt(0x40),
    TxLoader(0x50);


    static final Map<Integer, NodeType> lookup = new HashMap<>();


    public static NodeType byId(int id) {
      return lookup.getOrDefault(id, Unknown);
    }


    static {
      for (NodeType nt : NodeType.values()) {
        lookup.put(nt.id, nt);
      }
    }

    public final int id;


    NodeType(int nodeTypeId) {
      this.id = nodeTypeId;
    }

  }



  public enum P2PType {
    STATE_VIEW(0x00), // Unused
    BLOCK_FINALIZED(0x01), // Response to BLOCK_REQUEST
    PROPOSAL(0x02),
    VOTE(0x03),
    TX(0x04), // Unused
    PEER_REQUEST(0x05),
    PEER_RECORD(0x06),
    STATE_REQUEST(0x07),
    BLOCK_REQUEST(0x08),
    LISTEN_PORT(0x09),
    STATE_RESPONSE(0x0A),
    SINGLE_PEER_MESSAGE(0x0B), // Unused
    NETWORK_INFO(0x0C), // Unused
    SIGNATURE(0x0D),
    ITEM_REQUEST(0x0E),
    BLOCK_COMMITTED(0x0F),
    EMPTY_PROPOSAL(0x10),
    PROPOSED_TXS(0x11),
    CHECK_ORIGIN(0xFFA0),
    PREPARING_PROPOSAL(0xFFA1),
    TX_PACKAGE_ORIGINAL(0xFFA2),
    TX_PACKAGE_FORWARD(0xFFA3),
    TX_PACKAGE_RESPONSE(0xFFA7),
    NAMESPACE_LIST(0xFFA4), // Unused
    CLASSES_LIST(0xFFA5), // Unused
    NONCE_DETAILS(0xFFA6), // Unused
    MESSAGE_FRAGMENT(0xFFB0), // Kafka message fragment
    BLOCK_INDEX(0xFFB1),      // Kafka Block Index message
    CLOSE_REQUEST(0xFFF0),
    UNKNOWN(-1);

    /**
     * P2PType get().
     *
     * @param mType :
     *
     * @return :
     */
    public static P2PType get(int mType) {

      for (P2PType v : values()) {
        if (v.id == mType) {
          return v;
        }
      }
      return null;
    }


    private final int id;


    P2PType(int id) {

      this.id = id;

    }


    public int getId() {

      return id;
    }

  }



  public enum SuccessType {

    PASS,
    FAIL,
    WARNING

    // MessageFormat.format("This POA `{0}`
    // (checkOnly ? SuccessType.WARNING : SuccessType.FAIL)
  }



  /**
   * IndexStatus.
   *
   * Index status values used for the messages in the Kafka 'blockindex' topic.
   * 'In_progress' messages are published to inform other validation nodes that a block is in the process of being published.
   * 'Success' messages are published (with the block message timestamp) once the block has been successfully published to the Kafka topic.
   * 'Success' messages form a succinct index into the main Blocks topic which could be very much larger.
   *
   */
  public enum IndexStatus {

    FAIL(0x00),
    IN_PROGRESS(0X01),
    SUCCESS(0x02);

    public static IndexStatus get(int mType) {

      for (IndexStatus v : values()) {
        if (v.id == mType) {
          return v;
        }
      }
      return null;
    }


    private final int id;


    IndexStatus(int id) {

      this.id = id;

    }


    public int getId() {

      return id;
    }
  }



  public enum TxType {
    // NOTE: The order of declaration MUST MATCH the ID order.

    GRANT_VOTING_POWER(0x01, PermissionIDs.GRANT_VOTING_POWER, TxAbbreviations.GRANT_VOTING_POWER, TxExternalNames.GRANT_VOTING_POWER),
    REVOKE_VOTING_POWER(0x02, PermissionIDs.REVOKE_VOTING_POWER, TxAbbreviations.REVOKE_VOTING_POWER, TxExternalNames.REVOKE_VOTING_POWER),
    DO_NOTHING(0x03, PermissionIDs.DO_NOTHING, TxAbbreviations.DO_NOTHING, TxExternalNames.DO_NOTHING),
    REGISTER_NAMESPACE(0x04, -20, PermissionIDs.NAMESPACE_REGISTER, TxAbbreviations.REGISTER_NAMESPACE, TxExternalNames.REGISTER_NAMESPACE),
    REGISTER_ASSET_CLASS(0x05, -18, PermissionIDs.ASSET_CLASS_REGISTER, TxAbbreviations.REGISTER_ASSET_CLASS, TxExternalNames.REGISTER_ASSET_CLASS),
    ISSUE_ASSET(0x06, -15, PermissionIDs.ASSET_ISSUE_TX, TxAbbreviations.ISSUE_ASSET, TxExternalNames.ISSUE_ASSET),
    TRANSFER_ASSET(0x07, PermissionIDs.ASSET_TRANSFER_TX, TxAbbreviations.TRANSFER_ASSET, TxExternalNames.TRANSFER_ASSET),
    REGISTER_ADDRESS(0x08, -21, PermissionIDs.REGISTER_ADDRESS, TxAbbreviations.REGISTER_ADDRESS, TxExternalNames.REGISTER_ADDRESS),
    TRANSFER_NAMESPACE(0x09, -19, PermissionIDs.NAMESPACE_TRANSFER, TxAbbreviations.TRANSFER_NAMESPACE, TxExternalNames.TRANSFER_NAMESPACE),
    UPDATE_ASSET_CLASS(0x0A, -17, PermissionIDs.ASSET_CLASS_UPDATE, TxAbbreviations.UPDATE_ASSET_CLASS, TxExternalNames.UPDATE_ASSET_CLASS),
    DELETE_ASSET_CLASS(0x0B, 1, PermissionIDs.ASSET_CLASS_DELETE, TxAbbreviations.DELETE_ASSET_CLASS, TxExternalNames.DELETE_ASSET_CLASS),
    DELETE_NAMESPACE(0x0C, 2, PermissionIDs.NAMESPACE_DELETE, TxAbbreviations.DELETE_NAMESPACE, TxExternalNames.DELETE_NAMESPACE),
    UPDATE_ADDRESS_PERMISSIONS(0x0D, 2, PermissionIDs.UPDATE_ADDRESS_PERMISSIONS, TxAbbreviations.UPDATE_ADDRESS_PERMISSIONS,
        TxExternalNames.UPDATE_ADDRESS_PERMISSIONS),
    DELETE_ADDRESS(0x0E, 20, PermissionIDs.ADDRESS_DELETE, TxAbbreviations.DELETE_ADDRESS, TxExternalNames.DELETE_ADDRESS),
    DO_PRIVILEGED_OPERATION(0x0F, -22, 0, "prvop", "DO_PRIVILEGED_OPERATION"),
    CREATE_MEMO(0x10, PermissionIDs.CREATE_MEMO, TxAbbreviations.CREATE_MEMO, TxExternalNames.CREATE_MEMO),
    TRANSFER_ASSET_AS_ISSUER(0x11, -10, PermissionIDs.TRANSFER_ASSET_AS_ISSUER, TxAbbreviations.TRANSFER_ASSET_AS_ISSUER,
        TxExternalNames.TRANSFER_ASSET_AS_ISSUER),
    ENCUMBER_ASSET(0x12, -10, PermissionIDs.ENCUMBER_ASSET, TxAbbreviations.ENCUMBER_ASSET, TxExternalNames.ENCUMBER_ASSET),
    UNENCUMBER_ASSET(0x13, -1, PermissionIDs.UNENCUMBER_ASSET, TxAbbreviations.UNENCUMBER_ASSET, TxExternalNames.UNENCUMBER_ASSET),
    EXERCISE_ENCUMBRANCE(0x14, -9, PermissionIDs.EXERCISE_ENCUMBRANCE, TxAbbreviations.EXERCISE_ENCUMBRANCE, TxExternalNames.EXERCISE_ENCUMBRANCE),
    LOCK_ASSET(0x15, 3, PermissionIDs.LOCK_ASSET, TxAbbreviations.LOCK_ASSET, TxExternalNames.LOCK_ASSET),
    UNLOCK_ASSET(0x16, 3, PermissionIDs.UNLOCK_ASSET, TxAbbreviations.UNLOCK_ASSET, TxExternalNames.UNLOCK_ASSET),
    GRANT_POA(0x17, PermissionIDs.GRANT_POA, TxAbbreviations.GRANT_POA, TxExternalNames.GRANT_POA),
    REVOKE_POA(0x18, PermissionIDs.REVOKE_POA, TxAbbreviations.REVOKE_POA, TxExternalNames.REVOKE_POA),
    TRANSFER_ASSET_FROM_MANY(0x19, PermissionIDs.TRANSFER_ASSET_FROM_MANY, TxAbbreviations.TRANSFER_ASSET_FROM_MANY,
        TxExternalNames.TRANSFER_ASSET_FROM_MANY
    ),
    LOCK_ASSET_HOLDING(0x1A, PermissionIDs.LOCK_ASSET_HOLDING, TxAbbreviations.LOCK_ASSET_HOLDING, TxExternalNames.LOCK_ASSET_HOLDING),
    UNLOCK_ASSET_HOLDING(0x1B, PermissionIDs.UNLOCK_ASSET_HOLDING, TxAbbreviations.UNLOCK_ASSET_HOLDING, TxExternalNames.UNLOCK_ASSET_HOLDING),
    ISSUE_AND_ENCUMBER_ASSET(0x1C, -15, PermissionIDs.ASSET_ISSUE_AND_ENCUMBER_TX, TxAbbreviations.ISSUE_AND_ENCUMBER_ASSET,
        TxExternalNames.ISSUE_AND_ENCUMBER_ASSET
    ),

    WFL_HOLD1(0x50, 0, TxAbbreviations.WFL_HOLD1, TxExternalNames.WFL_HOLD1),
    WFL_HOLD2(0x51, 0, TxAbbreviations.WFL_HOLD2, TxExternalNames.WFL_HOLD2),
    WFL_HOLD3(0x52, 0, TxAbbreviations.WFL_HOLD3, TxExternalNames.WFL_HOLD3),
    WFL_HOLD4(0x53, 0, TxAbbreviations.WFL_HOLD4, TxExternalNames.WFL_HOLD4),
    WFL_HOLD5(0x54, 0, TxAbbreviations.WFL_HOLD5, TxExternalNames.WFL_HOLD5),
    WFL_HOLD6(0x55, 0, TxAbbreviations.WFL_HOLD6, TxExternalNames.WFL_HOLD6),
    WFL_HOLD7(0x56, 0, TxAbbreviations.WFL_HOLD7, TxExternalNames.WFL_HOLD7),
    WFL_HOLD8(0x57, 0, TxAbbreviations.WFL_HOLD8, TxExternalNames.WFL_HOLD8),
    WFL_HOLD9(0x58, 0, TxAbbreviations.WFL_HOLD9, TxExternalNames.WFL_HOLD9),
    WFL_HOLD10(0x59, 0, TxAbbreviations.WFL_HOLD10, TxExternalNames.WFL_HOLD10),
    WFL_HOLD11(0x5A, 0, TxAbbreviations.WFL_HOLD11, TxExternalNames.WFL_HOLD11),
    WFL_HOLD12(0x5B, 0, TxAbbreviations.WFL_HOLD12, TxExternalNames.WFL_HOLD12),
    WFL_HOLD13(0x5C, 0, TxAbbreviations.WFL_HOLD13, TxExternalNames.WFL_HOLD13),
    WFL_HOLD14(0x5D, 0, TxAbbreviations.WFL_HOLD14, TxExternalNames.WFL_HOLD14),
    WFL_HOLD15(0x5E, 0, TxAbbreviations.WFL_HOLD15, TxExternalNames.WFL_HOLD15),
    WFL_HOLD16(0x5F, 0, TxAbbreviations.WFL_HOLD16, TxExternalNames.WFL_HOLD16),

    POA_REGISTER_NAMESPACE(0x60, -20, PermissionIDs.POA_NAMESPACE_REGISTER, TxAbbreviations.POA_REGISTER_NAMESPACE,
        TxExternalNames.POA_REGISTER_NAMESPACE),
    POA_TRANSFER_NAMESPACE(0x62, -19, PermissionIDs.POA_NAMESPACE_TRANSFER, TxAbbreviations.POA_TRANSFER_NAMESPACE,
        TxExternalNames.POA_TRANSFER_NAMESPACE),
    POA_DELETE_NAMESPACE(0x61, 2, PermissionIDs.POA_NAMESPACE_DELETE, TxAbbreviations.POA_DELETE_NAMESPACE, TxExternalNames.POA_DELETE_NAMESPACE),
    POA_REGISTER_ASSET_CLASS(0x63, -18, PermissionIDs.POA_ASSET_CLASS_REGISTER, TxAbbreviations.POA_REGISTER_ASSET_CLASS,
        TxExternalNames.POA_REGISTER_ASSET_CLASS
    ),
    POA_ISSUE_ASSET(0x64, -15, PermissionIDs.POA_ASSET_ISSUE, TxAbbreviations.POA_ISSUE_ASSET, TxExternalNames.POA_ISSUE_ASSET),
    POA_DELETE_ASSET_CLASS(0x65, 1, PermissionIDs.POA_ASSET_CLASS_DELETE, TxAbbreviations.POA_DELETE_ASSET_CLASS,
        TxExternalNames.POA_DELETE_ASSET_CLASS),
    POA_TRANSFER_ASSET_X_CHAIN(0x66, PermissionIDs.POA_ASSET_TRANSFER_X_CHAIN, TxAbbreviations.POA_TRANSFER_ASSET_X_CHAIN,
        TxExternalNames.POA_TRANSFER_ASSET_X_CHAIN
    ),

    /* @deprecated This was always the X chain transfer, so should be called "psstra" */
    @Deprecated
    POA_ASSET_TRANSFER(0x66, PermissionIDs.POA_ASSET_TRANSFER_X_CHAIN, "pastra", "pastra"),

    POA_TRANSFER_ASSET_AS_ISSUER(0x67, -10, PermissionIDs.POA_TRANSFER_ASSET_AS_ISSUER, TxAbbreviations.POA_TRANSFER_ASSET_AS_ISSUER,
        TxExternalNames.POA_TRANSFER_ASSET_AS_ISSUER),
    POA_TRANSFER_ASSET_FROM_MANY(0x68, PermissionIDs.POA_TRANSFER_ASSET_FROM_MANY, TxAbbreviations.POA_TRANSFER_ASSET_FROM_MANY,
        TxExternalNames.POA_TRANSFER_ASSET_FROM_MANY
    ),
    POA_TRANSFER_ASSET_TO_MANY(0x69, PermissionIDs.POA_TRANSFER_ASSET_TO_MANY, TxAbbreviations.POA_TRANSFER_ASSET_TO_MANY,
        TxExternalNames.POA_TRANSFER_ASSET_TO_MANY),
    POA_NEW_CONTRACT(0x6A, -10, PermissionIDs.POA_NEW_CONTRACT, TxAbbreviations.POA_NEW_CONTRACT, TxExternalNames.POA_NEW_CONTRACT),
    POA_COMMIT_TO_CONTRACT(0x6B, PermissionIDs.POA_COMMIT_TO_CONTRACT, TxAbbreviations.POA_COMMIT_TO_CONTRACT, TxExternalNames.POA_COMMIT_TO_CONTRACT),
    POA_CANCEL_CONTRACT(0x6C, PermissionIDs.POA_CANCEL_CONTRACT, TxAbbreviations.POA_CANCEL_CONTRACT, TxExternalNames.POA_CANCEL_CONTRACT),
    POA_LOCK_ASSET(0x6D, 3, PermissionIDs.POA_LOCK_ASSET, TxAbbreviations.POA_LOCK_ASSET, TxExternalNames.POA_LOCK_ASSET),
    POA_UNLOCK_ASSET(0x6E, 3, PermissionIDs.POA_UNLOCK_ASSET, TxAbbreviations.POA_UNLOCK_ASSET, TxExternalNames.POA_UNLOCK_ASSET),
    POA_ENCUMBER_ASSET(0x6F, -10, PermissionIDs.POA_ENCUMBER_ASSET, TxAbbreviations.POA_ENCUMBER_ASSET, TxExternalNames.POA_ENCUMBER_ASSET),
    POA_UNENCUMBER_ASSET(0x70, -1, PermissionIDs.POA_UNENCUMBER_ASSET, TxAbbreviations.POA_UNENCUMBER_ASSET, TxExternalNames.POA_UNENCUMBER_ASSET),
    POA_EXERCISE_ENCUMBRANCE(0x71, -9, PermissionIDs.POA_EXERCISE_ENCUMBRANCE, TxAbbreviations.POA_EXERCISE_ENCUMBRANCE,
        TxExternalNames.POA_EXERCISE_ENCUMBRANCE),
    POA_LOCK_ASSET_HOLDING(0x72, -11, PermissionIDs.POA_LOCK_ASSET_HOLDING, TxAbbreviations.POA_LOCK_ASSET_HOLDING,
        TxExternalNames.POA_LOCK_ASSET_HOLDING),
    POA_UNLOCK_ASSET_HOLDING(0x73, -11, PermissionIDs.POA_UNLOCK_ASSET_HOLDING, TxAbbreviations.POA_UNLOCK_ASSET_HOLDING,
        TxExternalNames.POA_UNLOCK_ASSET_HOLDING),
    POA_DELETE_ADDRESS(0x74, 20, PermissionIDs.POA_ADDRESS_DELETE, TxAbbreviations.POA_DELETE_ADDRESS, TxExternalNames.POA_DELETE_ADDRESS),
    POA_ISSUE_AND_ENCUMBER_ASSET(0x75, -15, PermissionIDs.POA_ASSET_ISSUE_AND_ENCUMBER_TX, TxAbbreviations.POA_ISSUE_AND_ENCUMBER_ASSET,
        TxExternalNames.POA_ISSUE_AND_ENCUMBER_ASSET
    ),

    TRANSFER_ASSET_X_CHAIN(0x80, PermissionIDs.ASSET_TRANSFER_X_CHAIN, TxAbbreviations.TRANSFER_ASSET_X_CHAIN, TxExternalNames.TRANSFER_ASSET_X_CHAIN),
    X_CHAIN_TX_PACKAGE(0x81, -16, PermissionIDs.X_CHAIN_TX_PACKAGE, TxAbbreviations.X_CHAIN_TX_PACKAGE,
        TxExternalNames.X_CHAIN_TX_PACKAGE
    ), // xcpkg is witness node only
    ADD_X_CHAIN(0x82, PermissionIDs.ADD_X_CHAIN, TxAbbreviations.ADD_X_CHAIN, TxExternalNames.ADD_X_CHAIN),
    REMOVE_X_CHAIN(0x83, PermissionIDs.REMOVE_X_CHAIN, TxAbbreviations.REMOVE_X_CHAIN, TxExternalNames.REMOVE_X_CHAIN),
    SPLIT_STOCK(0x84, PermissionIDs.SPLIT_STOCK, TxAbbreviations.SPLIT_STOCK, TxExternalNames.SPLIT_STOCK),
    DO_DIVIDEND(0x85, PermissionIDs.DO_DIVIDEND, TxAbbreviations.DO_DIVIDEND, TxExternalNames.DO_DIVIDEND), // Obsoleted.
    TRANSFER_ASSET_TO_MANY(0x86, PermissionIDs.TRANSFER_ASSET_TO_MANY, TxAbbreviations.TRANSFER_ASSET_TO_MANY, TxExternalNames.TRANSFER_ASSET_TO_MANY),
    NEW_CONTRACT(0x90, -10, PermissionIDs.NEW_CONTRACT, TxAbbreviations.NEW_CONTRACT, TxExternalNames.NEW_CONTRACT),
    CANCEL_CONTRACT(0x91, PermissionIDs.CANCEL_CONTRACT, TxAbbreviations.CANCEL_CONTRACT, TxExternalNames.CANCEL_CONTRACT),
    COMMIT_TO_CONTRACT(0x92, PermissionIDs.COMMIT_TO_CONTRACT, TxAbbreviations.COMMIT_TO_CONTRACT, TxExternalNames.COMMIT_TO_CONTRACT),
    NOTIFY_CONTRACT(0x93, PermissionIDs.NOTIFY_CONTRACT, TxAbbreviations.NOTIFY_CONTRACT, TxExternalNames.NOTIFY_CONTRACT),

    JSON_CREATE_DATA_NAMESPACE(0x100, 0, 0x100, "jcrdnm", "JSON_CREATE_DATA_NAMESPACE"),
    JSON_DELETE_DATA_NAMESPACE(0x101, 15, 0x101, "jdldnm", "JSON_DELETE_DATA_NAMESPACE"),
    JSON_UPDATE_DATA_NAMESPACE(0x102, 5, 0x102, "jupdnm", "JSON_UPDATE_DATA_NAMESPACE"),
    JSON_CREATE_DATA_DOCUMENT(0x103, 8, 0x103, "jcrdoc", "JSON_CREATE_DATA_DOCUMENT"),
    JSON_UPDATE_DATA_DOCUMENT(0x104, 10, 0x104, "jupdoc", "JSON_UPDATE_DATA_DOCUMENT"),
    JSON_DELETE_DATA_DOCUMENT(0x106, 11, 0x106, "jdldoc", "JSON_DELETE_DATA_DOCUMENT"),
    JSON_UPDATE_DESCRIPTION(0x105, 9, 0x105, "jupdes", "JSON_UPDATE_DESCRIPTION"),
    JSON_CREATE_VALIDATOR(0x107, 4, 0x107, "jcrval", "JSON_CREATE_VALIDATOR"),
    JSON_DELETE_VALIDATOR(0x10f, 14, 0x107, "jcrval", "JSON_DELETE_VALIDATOR"),
    JSON_CREATE_ALGORITHM(0x108, 3, 0x108, "jcralg", "JSON_CREATE_ALGORITHM"),
    JSON_CREATE_ACL(0x109, 2, 0x109, "jcracl", "JSON_CREATE_ACL"),
    JSON_DELETE_ACL(0x10d, 12, 0x10d, "jcdacl", "JSON_DELETE_ACL"),
    JSON_UPDATE_ACL(0x10a, 7, 0x10a, "jupacl", "JSON_UPDATE_ACL"),
    JSON_CREATE_ACL_ROLE(0x10b, 1, 0x10b, "jcracr", "JSON_CREATE_ACL_ROLE"),
    JSON_UPDATE_ACL_ROLE(0x10c, 6, 0x10c, "jupacr", "JSON_UPDATE_ACL_ROLE"),
    JSON_DELETE_ACL_ROLE(0x10e, 13, 0x10e, "jcdacr", "JSON_DELETE_ACL_ROLE");


    /** Constant used to select the use of abbreviations as the external TX representation. */
    public static final int EXTERNAL_NAME_ABBREVIATION = 1;

    /** Constant used to select the use of IDs as the external TX representation. */
    public static final int EXTERNAL_NAME_ID = 3;

    /** Constant used to select the use of long names as the external TX representation. */
    public static final int EXTERNAL_NAME_LONG = 2;

    /**
     * My copy of the values() array which we will not change, so that we don't need to create new copies.
     */
    private static final TxType[] myValues = values();

    /** The selected external name format. Defaults to long names. */
    private static int externalNameFormat = EXTERNAL_NAME_LONG;


    /**
     * TxType get.
     *
     * @param mType :
     *
     * @return :
     */
    public static TxType get(int mType) {

      for (TxType v : myValues) {
        if (v.id == mType) {
          return v;
        }
      }
      return null;
    }


    /**
     * get.
     *
     * @param abbreviation :
     *
     * @return :
     */
    @JsonCreator
    public static TxType get(String abbreviation) {

      for (TxType v : myValues) {
        if (v.longName.equalsIgnoreCase(abbreviation)) {
          return v;
        }
      }

      for (TxType v : myValues) {
        if (v.abbreviation.equalsIgnoreCase(abbreviation)) {
          return v;
        }
      }

      // try for a numeric ID
      try {
        int id = Integer.decode(abbreviation);
        return get(id);
      } catch (NumberFormatException e) {
        return null;
      }
    }


    public static int getExternalNameFormat() {
      return externalNameFormat;
    }


    @SuppressWarnings("squid:S3066") // This setter is mistakenly identified as manipulating the enumerations, not just selecting an output format.
    public static void setExternalNameFormat(int externalNameFormat) {
      TxType.externalNameFormat = externalNameFormat;
    }


    private final String abbreviation;

    private final int id;

    private final String longName;

    private final int permissionId;

    private final int priority;


    TxType(int id, int permissionId, String abbrev, String longName) {
      this(id, 0, permissionId, abbrev, longName);
    }


    TxType(int id, int priority, int permissionId, String abbrev, String longName) {
      this.id = id;
      this.longName = longName;
      this.priority = priority;
      this.permissionId = permissionId;
      this.abbreviation = abbrev;
    }


    public String getAbbreviation() {
      return abbreviation;
    }


    /**
     * Get the external name for this type. The external name's format is configurable to match the clients' expectation. Hence server code should not assume
     * the external name will return specific values.
     *
     * @return the external name, in the format required by the clients.
     */
    @JsonValue
    public String getExternalName() {
      switch (externalNameFormat) {
        case EXTERNAL_NAME_ABBREVIATION:
          return abbreviation;
        case EXTERNAL_NAME_LONG:
          return longName;
        case EXTERNAL_NAME_ID:
          return "#" + Integer.toHexString(id);
        default:
          throw new IllegalStateException("External name format has been set to " + externalNameFormat + " which is an unrecognised value");
      }
    }


    public int getId() {
      return id;
    }


    public String getLongName() {
      return longName;
    }


    public int getPermissionId() {
      return permissionId;
    }


    public int getPriority() {
      return priority;
    }
  }



  public static class AuthorisedAddressConstants {

    public static final long AP_ADMIN = 0x000001; // May change Address Permissions

    public static final long AP_BOND = 0x000100;

    public static final long AP_BONDING = AP_ADMIN | AP_BOND;

    public static final long AP_CLASS = 0x000020; // May Create / Delete Classes

    public static final long AP_CLASS_ADMIN = 0x000010; // May Set / Remove Class permissions, Create / Delete Namespace for others ?

    public static final long AP_CLASSES = AP_ADMIN | AP_CLASS_ADMIN | AP_CLASS;

    public static final long AP_COMMIT = 0x004000;

    public static final long AP_COMMITS = AP_ADMIN | AP_COMMIT;

    public static final long AP_CONTRACT = 0x000800;

    public static final long AP_CONTRACTS = AP_ADMIN | AP_CONTRACT;

    public static final long AP_DEFAULT_AUTHORISEDADDRESS = 0L;

    public static final long AP_DEFAULT_AUTHORISE_BY_ADDRESS = 0L;

    public static final long AP_DELETE_ADDRESS = 0x008000;

    public static final long AP_LOCK = 0x000400;

    public static final long AP_LOCKING = AP_ADMIN | AP_LOCK;

    public static final long AP_MANAGER = 0x000002;

    public static final long AP_NAMESPACE = 0x000008; // May Create Namespaces

    public static final long AP_NAMESPACE_ADMIN = 0x000004; // May Set / Remove Namespace permissions, Create / Delete Namespace for others ?

    public static final long AP_NAMESPACES = AP_ADMIN | AP_NAMESPACE_ADMIN | AP_NAMESPACE;

    public static final long AP_NONE = 0x000000;

    public static final long AP_POA = 0x001000; // Power to grant POA

    public static final long AP_POAS = AP_ADMIN | AP_POA;

    public static final long AP_POA_EXERCISE = 0x002000; // Power to use POA Txs.

    public static final long AP_POA_EXERCISES = AP_ADMIN | AP_POA_EXERCISE;

    public static final long AP_REGISTER_ADDRESS = 0x000040; // May Register Addresses, with default Address Permissions ?

    public static final long AP_ADDRESSES = AP_ADMIN | AP_REGISTER_ADDRESS | AP_DELETE_ADDRESS;

    public static final long AP_TX_ADMIN = 0x000080; // May Set TX_Update / Modify Tx Permissions for others

    public static final long AP_TX_LIST = 0x100000; // Txs limited to permissioned IDs

    public static final long AP_MANAGER_CONTROL =
        AP_NAMESPACE_ADMIN | AP_NAMESPACE
            | AP_CLASS_ADMIN | AP_CLASS
            | AP_REGISTER_ADDRESS | AP_TX_ADMIN | AP_LOCK | AP_CONTRACT | AP_TX_LIST;

    public static final long AP_XCHAIN = 0x000200;

    public static final long AP_XCHAINING = AP_ADMIN | AP_XCHAIN;

    public static final long HOLD5 = 0x010000;

    public static final long HOLD6 = 0x020000;

    public static final long HOLD7 = 0x040000;

    public static final long HOLD8 = 0x080000;

    private static final long ALL_AP_CODES;

    private static final NavigableMap<Long, String> CODES_TO_NAME;

    private static final Map<String, Long> NAMES_TO_CODE;


    /**
     * Convert a bitwise mask to a set of named values.
     *
     * @param code the code
     *
     * @return the values
     */
    // "squid:S1168" we can return null if we want to!
    @SuppressWarnings("squid:S1168")
    public static Set<String> forCode(Long code) {
      if (code == null) {
        return null;
      }
      long c = code & ALL_AP_CODES;
      HashSet<String> names = new HashSet<>();
      while (c != 0) {
        Map.Entry<Long, String> f = CODES_TO_NAME.floorEntry(c);
        names.add(f.getValue());
        c &= ~f.getKey();
      }
      return names;
    }


    /**
     * Get the code for a given name, if it exists.
     *
     * @param name the name
     *
     * @return the code, or zero
     */
    public static long forName(String name) {
      if (name == null) {
        return 0;
      }

      // Try as genuine name
      Long l = NAMES_TO_CODE.get(name.toLowerCase());
      if (l != null) {
        return l.longValue();
      }

      // Try as number passed in as a string.
      try {
        l = Long.decode(name);
      } catch (NumberFormatException e) {
        // just a bad name
        return 0L;
      }
      return l & ALL_AP_CODES;
    }


    /**
     * Convert a collection of names to the corresponding bit mask.
     *
     * @param names the names
     *
     * @return the bit mask
     */
    public static long forNames(Collection<?> names) {

      if (names == null) {
        return 0;
      }
      long val = 0;
      for (Object n : names) {
        if (n == null) {
          continue;
        }
        if (n instanceof Number) {
          val |= ((Number) n).longValue() & ALL_AP_CODES;
        }
        if (n instanceof String) {
          val |= forName((String) n);
        }
      }
      return val;
    }


    static {

      Field[] fs = AuthorisedAddressConstants.class.getDeclaredFields();
      NavigableMap<Long, String> c2n = new TreeMap<>();
      Map<String, Long> n2c = new HashMap<>();
      long allCodes = 0;
      for (Field f : fs) {
        // Check field is a public static final long called AP_*
        int mods = f.getModifiers();
        if (!(
            Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods)
                && f.getType().equals(Long.TYPE) && f.getName().startsWith("AP_"))
        ) {
          continue;
        }

        String n = f.getName().substring(3).toLowerCase().replace("_", "");

        try {

          Long c = (Long) f.get(null);

          if (c != null) {
            n2c.put(n, c);
            c2n.put(c, n);
            allCodes |= c;
          }

        } catch (IllegalAccessException e) {
          throw new AssertionError("Failed to access a public field", e);
        }
      }

      NAMES_TO_CODE = Collections.unmodifiableMap(n2c);
      CODES_TO_NAME = Collections.unmodifiableNavigableMap(c2n);
      ALL_AP_CODES = allCodes;

    }

  }



  public static class ConfigConstants {

    public static final long DEFAULT_MAX_TIMERS_PER_BLOCK = 2000L;

    public static final long DEFAULT_MAX_TX_AGE = 86400L;

    public static final long DEFAULT_MAX_TX_PER_BLOCK = 20000L;

    public static final long DEFAULT_MIN_AGE_ADDRESS_DELETE = 86500L;

    public static final boolean DEFAULT_REQUIRE_AUTHORISED_ADDRESSES = false;

    public static final long MINIMUM_MAX_TIMERS_PER_BLOCK = 100L;

    public static final long MINIMUM_MAX_TX_AGE = 5L;

    public static final long MINIMUM_MAX_TX_PER_BLOCK = 1000L;

    public static final long MINIMUM_MIN_AGE_ADDRESS_DELETE = 15L;

  }



  public static class ContractConstants {

    public static final String CONTRACT_NAME_DVP_UK = "dvp_uk";

    public static final String CONTRACT_NAME_DVP_UK_COMMIT = "dvp_uk_commit";

    public static final String CONTRACT_NAME_EXCHANGE = "exchange";

    public static final String CONTRACT_NAME_EXCHANGE_COMMIT = "exchange_commit";

    public static final String CONTRACT_NAME_NOMINATE = "nominate";

    public static final String CONTRACT_NAME_TOKENS_NOMINATE = "tokens_nominate";

    public static final long DVP_DELETE_DELAY_ON_COMPLETE = 300L;

    public static final long DVP_DELETE_DELAY_ON_DELETE = 5L; // TODO Change back to 30 ?

    public static final long DVP_RETRY_ON_ASSETS_UNAVAILABLE = 15L;

    public static final int MAX_DVP_CONTRACTDURATION = 15552000;

    public static final int MAX_DVP_PAYMENT_METADATA_LENGTH = 10240;

    public static final int MAX_DVP_STARTDELAY = 2678400;

    public static final int MAX_ENCUMBRANCE_NAME_LENGTH = 1024;

  }



  public static class EncumbranceConstants {

    // Assets locked at individual addresses use reserved Encumbrance names :
    // These two names will forbidden for use in any other way.

    public static final String HOLDER_LOCK = "_Holder_Locked_";

    public static final String ISSUER_LOCK = "_Issuer_Locked_";

  }



  /**
   * PermissionIDs.
   *
   * <p>OpenCSD Permission IDs for each transaction type.
   * These values MUST correspond to the values held in the OpenCSD tblTxPermissionAreas table, or viceversa!</p>
   */
  public static class PermissionIDs {

    public static final int ADDRESS_DELETE = 26;

    public static final int UPDATE_ADDRESS_PERMISSIONS = 20;

    public static final int ADD_X_CHAIN = 103;

    public static final int ASSET_CLASS_DELETE = 14;

    public static final int ASSET_CLASS_REGISTER = 3;

    public static final int ASSET_CLASS_UPDATE = 18;

    public static final int ASSET_ISSUE_AND_ENCUMBER_TX = 90;

    public static final int ASSET_ISSUE_TX = 4;

    public static final int ASSET_TRANSFER_TX = 5;

    public static final int ASSET_TRANSFER_X_CHAIN = 100;

    public static final int GRANT_VOTING_POWER = 101;

    public static final int CANCEL_CONTRACT = 61;

    public static final int COMMIT_TO_CONTRACT = 62;

    public static final int DO_DIVIDEND = 41;

    public static final int ENCUMBER_ASSET = 11;

    public static final int EXERCISE_ENCUMBRANCE = 13;

    public static final int TRANSFER_ASSET_AS_ISSUER = 10;

    public static final int TRANSFER_ASSET_FROM_MANY = 23;

    public static final int LOCK_ASSET = 16;

    public static final int LOCK_ASSET_HOLDING = 24;

    public static final int CREATE_MEMO = 19;

    public static final int NAMESPACE_DELETE = 15;

    public static final int NAMESPACE_REGISTER = 1;

    public static final int NAMESPACE_TRANSFER = 2;

    public static final int NEW_CONTRACT = 60;

    public static final int NOTIFY_CONTRACT = 63;

    public static final int DO_NOTHING = 6;

    public static final int GRANT_POA = 21;

    public static final int POA_ADDRESS_DELETE = 89;

    public static final int POA_ASSET_CLASS_DELETE = 74;

    public static final int POA_ASSET_CLASS_REGISTER = 72;

    public static final int POA_ASSET_ISSUE = 73;

    public static final int POA_ASSET_ISSUE_AND_ENCUMBER_TX = 91;

    public static final int POA_ASSET_TRANSFER_X_CHAIN = 75;

    public static final int POA_CANCEL_CONTRACT = 81;

    public static final int POA_COMMIT_TO_CONTRACT = 80;

    public static final int REVOKE_POA = 22;

    public static final int POA_ENCUMBER_ASSET = 84;

    public static final int POA_EXERCISE_ENCUMBRANCE = 86;

    public static final int POA_TRANSFER_ASSET_AS_ISSUER = 76;

    public static final int POA_TRANSFER_ASSET_FROM_MANY = 77;

    public static final int POA_LOCK_ASSET = 82;

    public static final int POA_LOCK_ASSET_HOLDING = 87;

    public static final int POA_NAMESPACE_DELETE = 71;

    public static final int POA_NAMESPACE_REGISTER = 70;

    public static final int POA_NAMESPACE_TRANSFER = 111;

    public static final int POA_NEW_CONTRACT = 79;

    public static final int POA_TRANSFER_ASSET_TO_MANY = 78;

    public static final int POA_UNENCUMBER_ASSET = 85;

    public static final int POA_UNLOCK_ASSET = 83;

    public static final int POA_UNLOCK_ASSET_HOLDING = 88;

    public static final int RAW_TX = 7;

    public static final int REGISTER_ADDRESS = 8;

    public static final int REMOVE_X_CHAIN = 104;

    public static final int SPLIT_STOCK = 40;

    public static final int TRANSFER_ASSET_TO_MANY = 9;

    public static final int REVOKE_VOTING_POWER = 102;

    public static final int UNENCUMBER_ASSET = 12;

    public static final int UNLOCK_ASSET = 17;

    public static final int UNLOCK_ASSET_HOLDING = 25;

    public static final int X_CHAIN_TX_PACKAGE = 105;

  }



  /**
   * Abbreviations for each transaction type. Whilst these abbreviations are properties of the TxType enumeration, there are times when the Java compiler
   * requires them to be compile time constants, which properties of an enumeration are not.
   */
  public static class TxAbbreviations {

    public static final String DELETE_ADDRESS = "addel";

    public static final String UPDATE_ADDRESS_PERMISSIONS = "adprm";

    public static final String ADD_X_CHAIN = "xcadd";

    public static final String DELETE_ASSET_CLASS = "asdel";

    public static final String REGISTER_ASSET_CLASS = "asreg";

    public static final String UPDATE_ASSET_CLASS = "asupd";

    public static final String ISSUE_ASSET = "asiss";

    public static final String ISSUE_AND_ENCUMBER_ASSET = "asien";

    public static final String TRANSFER_ASSET = "astra";

    public static final String TRANSFER_ASSET_X_CHAIN = "sttra";

    public static final String GRANT_VOTING_POWER = "stbon";

    public static final String CANCEL_CONTRACT = "cocan";

    public static final String COMMIT_TO_CONTRACT = "cocom";

    public static final String DO_DIVIDEND = "cadiv"; // Obsoleted.

    public static final String ENCUMBER_ASSET = "encum";

    public static final String EXERCISE_ENCUMBRANCE = "exenc";

    public static final String TRANSFER_ASSET_AS_ISSUER = "istra";

    public static final String TRANSFER_ASSET_FROM_MANY = "istfm";

    public static final String LOCK_ASSET = "asloc";

    public static final String LOCK_ASSET_HOLDING = "holoc";

    public static final String CREATE_MEMO = "txmem";

    public static final String DELETE_NAMESPACE = "nsdel";

    public static final String REGISTER_NAMESPACE = "nsreg";

    public static final String TRANSFER_NAMESPACE = "nstra";

    public static final String NEW_CONTRACT = "conew";

    public static final String NOTIFY_CONTRACT = "conot";

    public static final String DO_NOTHING = "txnul";

    public static final String GRANT_POA = "poaad";

    public static final String POA_DELETE_ADDRESS = "paddel";

    public static final String POA_DELETE_ASSET_CLASS = "pasdel";

    public static final String POA_REGISTER_ASSET_CLASS = "pasreg";

    public static final String POA_ISSUE_ASSET = "pasiss";

    public static final String POA_ISSUE_AND_ENCUMBER_ASSET = "pasien";

    public static final String POA_TRANSFER_ASSET_X_CHAIN = "psttra";

    public static final String POA_CANCEL_CONTRACT = "pcocan";

    public static final String POA_COMMIT_TO_CONTRACT = "pcocom";

    public static final String REVOKE_POA = "poade";

    public static final String POA_ENCUMBER_ASSET = "pencum";

    public static final String POA_EXERCISE_ENCUMBRANCE = "pexenc";

    public static final String POA_TRANSFER_ASSET_AS_ISSUER = "pistra";

    public static final String POA_TRANSFER_ASSET_FROM_MANY = "pistfm";

    public static final String POA_LOCK_ASSET = "pasloc";

    public static final String POA_LOCK_ASSET_HOLDING = "pholoc";

    public static final String POA_DELETE_NAMESPACE = "pnsdel";

    public static final String POA_REGISTER_NAMESPACE = "pnsreg";

    public static final String POA_TRANSFER_NAMESPACE = "pnstra";

    public static final String POA_NEW_CONTRACT = "pconew";

    public static final String POA_TRANSFER_ASSET_TO_MANY = "ptrtom";

    public static final String POA_UNENCUMBER_ASSET = "punenc";

    public static final String POA_UNLOCK_ASSET = "pasunl";

    public static final String POA_UNLOCK_ASSET_HOLDING = "phounl";

    public static final String REGISTER_ADDRESS = "adreg";

    public static final String REMOVE_X_CHAIN = "xcdel";

    public static final String SPLIT_STOCK = "caspl";

    public static final String TRANSFER_ASSET_TO_MANY = "trtom";

    public static final String REVOKE_VOTING_POWER = "stunb";

    public static final String UNENCUMBER_ASSET = "unenc";

    public static final String UNLOCK_ASSET = "asunl";

    public static final String UNLOCK_ASSET_HOLDING = "hounl";

    public static final String WFL_HOLD1 = "wfl001";

    public static final String WFL_HOLD10 = "wfl010";

    public static final String WFL_HOLD11 = "wfl011";

    public static final String WFL_HOLD12 = "wfl012";

    public static final String WFL_HOLD13 = "wfl013";

    public static final String WFL_HOLD14 = "wfl014";

    public static final String WFL_HOLD15 = "wfl015";

    public static final String WFL_HOLD16 = "wfl016";

    public static final String WFL_HOLD2 = "wfl002";

    public static final String WFL_HOLD3 = "wfl003";

    public static final String WFL_HOLD4 = "wfl004";

    public static final String WFL_HOLD5 = "wfl005";

    public static final String WFL_HOLD6 = "wfl006";

    public static final String WFL_HOLD7 = "wfl007";

    public static final String WFL_HOLD8 = "wfl008";

    public static final String WFL_HOLD9 = "wfl009";

    public static final String X_CHAIN_TX_PACKAGE = "xcpkg"; // xcpkg is witness node only

  }



  /**
   * These are the names used to indicate transaction types in our external API. The names either take the form of verb and then noun, or take the natural
   * English word order for such an instruction.
   */
  public static class TxExternalNames {

    public static final String DELETE_ADDRESS = "DELETE_ADDRESS";

    public static final String UPDATE_ADDRESS_PERMISSIONS = "UPDATE_ADDRESS_PERMISSIONS";

    public static final String ADD_X_CHAIN = "ADD_X_CHAIN";

    public static final String DELETE_ASSET_CLASS = "DELETE_ASSET_CLASS";

    public static final String REGISTER_ASSET_CLASS = "REGISTER_ASSET_CLASS";

    public static final String UPDATE_ASSET_CLASS = "UPDATE_ASSET_CLASS";

    public static final String ISSUE_ASSET = "ISSUE_ASSET";

    public static final String ISSUE_AND_ENCUMBER_ASSET = "ISSUE_AND_ENCUMBER_ASSET";

    public static final String TRANSFER_ASSET = "TRANSFER_ASSET";

    public static final String TRANSFER_ASSET_X_CHAIN = "TRANSFER_ASSET_X_CHAIN";

    public static final String GRANT_VOTING_POWER = "GRANT_VOTING_POWER";

    public static final String CANCEL_CONTRACT = "CANCEL_CONTRACT";

    public static final String COMMIT_TO_CONTRACT = "COMMIT_TO_CONTRACT";

    public static final String DO_DIVIDEND = "DO_DIVIDEND";

    public static final String ENCUMBER_ASSET = "ENCUMBER_ASSET";

    public static final String EXERCISE_ENCUMBRANCE = "EXERCISE_ENCUMBRANCE";

    public static final String TRANSFER_ASSET_AS_ISSUER = "TRANSFER_ASSET_AS_ISSUER";

    public static final String TRANSFER_ASSET_FROM_MANY = "TRANSFER_ASSET_FROM_MANY";

    public static final String LOCK_ASSET = "LOCK_ASSET";

    public static final String LOCK_ASSET_HOLDING = "LOCK_ASSET_HOLDING";

    public static final String CREATE_MEMO = "CREATE_MEMO";

    public static final String DELETE_NAMESPACE = "DELETE_NAMESPACE";

    public static final String REGISTER_NAMESPACE = "REGISTER_NAMESPACE";

    public static final String TRANSFER_NAMESPACE = "TRANSFER_NAMESPACE";

    public static final String NEW_CONTRACT = "NEW_CONTRACT";

    public static final String NOTIFY_CONTRACT = "NOTIFY_CONTRACT";

    public static final String DO_NOTHING = "DO_NOTHING";

    public static final String GRANT_POA = "GRANT_POA";

    public static final String POA_DELETE_ADDRESS = "POA_DELETE_ADDRESS";

    public static final String POA_DELETE_ASSET_CLASS = "POA_DELETE_ASSET_CLASS";

    public static final String POA_REGISTER_ASSET_CLASS = "POA_REGISTER_ASSET_CLASS";

    public static final String POA_ISSUE_ASSET = "POA_ISSUE_ASSET";

    public static final String POA_ISSUE_AND_ENCUMBER_ASSET = "POA_ISSUE_AND_ENCUMBER_ASSET";

    public static final String POA_TRANSFER_ASSET = "POA_TRANSFER_ASSET";

    public static final String POA_TRANSFER_ASSET_X_CHAIN = "POA_TRANSFER_ASSET_X_CHAIN";

    public static final String POA_CANCEL_CONTRACT = "POA_CANCEL_CONTRACT";

    public static final String POA_COMMIT_TO_CONTRACT = "POA_COMMIT_TO_CONTRACT";

    public static final String REVOKE_POA = "REVOKE_POA";

    public static final String POA_ENCUMBER_ASSET = "POA_ENCUMBER_ASSET";

    public static final String POA_EXERCISE_ENCUMBRANCE = "POA_EXERCISE_ENCUMBRANCE";

    public static final String POA_TRANSFER_ASSET_AS_ISSUER = "POA_TRANSFER_ASSET_AS_ISSUER";

    public static final String POA_TRANSFER_ASSET_FROM_MANY = "POA_TRANSFER_ASSET_FROM_MANY";

    public static final String POA_LOCK_ASSET = "POA_LOCK_ASSET";

    public static final String POA_LOCK_ASSET_HOLDING = "POA_LOCK_ASSET_HOLDING";

    public static final String POA_DELETE_NAMESPACE = "POA_DELETE_NAMESPACE";

    public static final String POA_REGISTER_NAMESPACE = "POA_REGISTER_NAMESPACE";

    public static final String POA_TRANSFER_NAMESPACE = "POA_TRANSFER_NAMESPACE";

    public static final String POA_NEW_CONTRACT = "POA_NEW_CONTRACT";

    public static final String POA_TRANSFER_ASSET_TO_MANY = "POA_TRANSFER_ASSET_TO_MANY";

    public static final String POA_UNENCUMBER_ASSET = "POA_UNENCUMBER_ASSET";

    public static final String POA_UNLOCK_ASSET = "POA_UNLOCK_ASSET";

    public static final String POA_UNLOCK_ASSET_HOLDING = "POA_UNLOCK_ASSET_HOLDING";

    public static final String REGISTER_ADDRESS = "REGISTER_ADDRESS";

    public static final String REMOVE_X_CHAIN = "REMOVE_X_CHAIN";

    public static final String SPLIT_STOCK = "SPLIT_STOCK";

    public static final String TRANSFER_ASSET_TO_MANY = "TRANSFER_ASSET_TO_MANY";

    public static final String REVOKE_VOTING_POWER = "REVOKE_VOTING_POWER";

    public static final String UNENCUMBER_ASSET = "UNENCUMBER_ASSET";

    public static final String UNLOCK_ASSET = "UNLOCK_ASSET";

    public static final String UNLOCK_ASSET_HOLDING = "UNLOCK_ASSET_HOLDING";

    public static final String WFL_HOLD1 = "WFL_HOLD1";

    public static final String WFL_HOLD10 = "WFL_HOLD10";

    public static final String WFL_HOLD11 = "WFL_HOLD11";

    public static final String WFL_HOLD12 = "WFL_HOLD12";

    public static final String WFL_HOLD13 = "WFL_HOLD13";

    public static final String WFL_HOLD14 = "WFL_HOLD14";

    public static final String WFL_HOLD15 = "WFL_HOLD15";

    public static final String WFL_HOLD16 = "WFL_HOLD16";

    public static final String WFL_HOLD2 = "WFL_HOLD2";

    public static final String WFL_HOLD3 = "WFL_HOLD3";

    public static final String WFL_HOLD4 = "WFL_HOLD4";

    public static final String WFL_HOLD5 = "WFL_HOLD5";

    public static final String WFL_HOLD6 = "WFL_HOLD6";

    public static final String WFL_HOLD7 = "WFL_HOLD7";

    public static final String WFL_HOLD8 = "WFL_HOLD8";

    public static final String WFL_HOLD9 = "WFL_HOLD9";

    public static final String X_CHAIN_TX_PACKAGE = "X_CHAIN_TX_PACKAGE";

  }



  /**
   * tx_general_fields. <p>Static class providing array index of the standard eight fields common to all TX Types.</p>
   */
  public static class TxGeneralFields {

    public static final int TX_CHAIN = 0x00;

    public static final int TX_FROM_ADDR = 0x07;

    public static final int TX_FROM_PUB = 0x06;

    public static final int TX_HASH = 0x03;

    public static final int TX_NONCE = 0x04;

    public static final int TX_TXTYPE = 0x02;

    public static final int TX_UPDATED = 0x05;

  }



  public static class VersionConstants {

    public static final int DVP_SET_STATUS = 5;

    /** The default version for new states. This constant will be amended as new versions of state are developed. */
    public static final int VERSION_DEFAULT = 5;

    public static final int VERSION_LOCKED_ASSET_AS_MERKLE = 5;

    public static final int VERSION_SET_ADDRESS_METADATA = 4;

    public static final int VERSION_SET_ADDRESS_TIME = 4;

    public static final int VERSION_SET_FULL_ADDRESS = 4;

    public static final int VERSION_USE_UPDATE_HEIGHT = 4;

  }



  @SuppressWarnings("squid:S00115") // Naming convention contravention. Prototype consistent.
  public static class XChainParameters {

    public static final long AcceptAddress = 0x0001;  // Accept xChain Address registration from this chain

    public static final long AcceptAnyInitial = 0xC000;

    public static final long AcceptAnyTx = 0x0055;  // AcceptClasses + AcceptNamespaces + AcceptAssets + AcceptAddress

    public static final long AcceptAssets = 0x0004;  // Accept xChain asset transactions from this chain

    public static final long AcceptClasses = 0x0040;  // Accept class transactions from this chain - To keep classes in synch after initial snapshot.

    public static final long AcceptInitialClass = 0x8000;  // Class definitions (initial snapshot) to be taken from xChain

    public static final long AcceptInitialNamespace = 0x4000;  // Namespaces (initial snapshot) to be taken from xChain

    public static final long AcceptNamespaces = 0x0010;  // Accept namespace transactions from this chain - To keep namespaces in synch after initial snapshot.

    public static final long AnySend = 0x00AA;  // Utility values, combinations of previous bits.

    public static final long ExternalClassPriority = 0x2000;  // Classes imported from an external xChain will override local definitions

    public static final long ExternalNamespacePriority = 0x1000;  // Namespaces imported from an external xChain will override (Move) local definitions

    public static final long SendAddress = 0x0002;  // Enable xChain Address registration to this xChain

    public static final long SendAnyAsset = 0x000A;

    public static final long SendAssets = 0x0008;  // Enable xChain asset transactions to this chain

    public static final long SendClasses = 0x0080;  // Allow Class transactions to propogate to this chain

    public static final long SendNamespaces = 0x0020;  // Allow Namespace transactions to propogate to this chain

    private static final long ALL_XC_CODES;

    private static final NavigableMap<Long, String> CODES_TO_NAME;

    private static final Map<String, Long> NAMES_TO_CODE;


    /**
     * Convert a bitwise mask to a set of named values.
     *
     * @param code the code
     *
     * @return the values
     */
    public static Set<String> forCode(Long code) {

      if (code == null) {
        return Collections.emptySet();
      }
      long c = code & ALL_XC_CODES;
      HashSet<String> names = new HashSet<>();
      while (c != 0) {
        Map.Entry<Long, String> f = CODES_TO_NAME.floorEntry(c);
        names.add(f.getValue());
        c &= ~f.getKey();
      }
      return names;
    }


    /**
     * Get the code for a given name, if it exists.
     *
     * @param name the name
     *
     * @return the code, or zero
     */
    public static long forName(String name) {

      if (name == null) {
        return 0;
      }

      // Try as genuine name
      Long l = NAMES_TO_CODE.get(name.toLowerCase());
      if (l != null) {
        return l.longValue();
      }

      // Try as number passed in as a string.
      try {
        l = Long.decode(name);
      } catch (NumberFormatException e) {
        // just a bad name
        return 0L;
      }
      return l & ALL_XC_CODES;
    }


    /**
     * Convert a collection of names to the corresponding bit mask.
     *
     * @param names the names
     *
     * @return the bit mask
     */
    public static long forNames(Collection<?> names) {

      if (names == null) {
        return 0;
      }
      long val = 0;
      for (Object n : names) {
        if (n == null) {
          continue;
        }
        if (n instanceof Number) {
          val |= ((Number) n).longValue() & ALL_XC_CODES;
        }
        if (n instanceof String) {
          val |= forName((String) n);
        }
      }
      return val;
    }


    /**
     * Convert a bit-wise mask to a comma-separated list of parameters.
     *
     * @param v the mask
     *
     * @return the parameters
     */
    public static String toList(long v) {

      StringBuilder buf = new StringBuilder();
      long v2 = v;
      for (Entry<Long, String> e : CODES_TO_NAME.entrySet()) {
        long m = e.getKey();
        if ((v2 & m) == m) {
          v2 &= ~m;
          buf.append(e.getValue()).append(", ");
        }
      }
      if (buf.length() > 0) {
        buf.setLength(buf.length() - 2);
      }
      return buf.toString();
    }


    static {
      Map<String, Long> names = new HashMap<>();
      NavigableMap<Long, String> masks = new TreeMap<>();
      Field[] fs = XChainParameters.class.getDeclaredFields();
      long allCodes = 0;

      for (Field f : fs) {

        int m = f.getModifiers();

        if (Modifier.isPublic(m) && Modifier.isFinal(m) && Modifier.isStatic(m) && f.getType().equals(Long.TYPE)) {
          Long v;
          try {
            v = (Long) f.get(null);
          } catch (IllegalAccessException e) {
            throw new AssertionError("Public static value was not accessible", e);
          }

          String n = f.getName().toLowerCase().replace("_", "");
          names.put(n, v);
          masks.put(v, n);
          allCodes |= v;
        }
      }

      NAMES_TO_CODE = Collections.unmodifiableMap(names);
      CODES_TO_NAME = Collections.unmodifiableNavigableMap(masks);

      ALL_XC_CODES = allCodes;
    }
  }



  @SuppressWarnings("squid:S00115") // Naming convention contravention. Prototype consistent.
  public static class XChainStatus {

    public static final long InitialClassSynchComplete = 0x8000;

    public static final long InitialNamespaceSynchComplete = 0x4000;

  }



  @SuppressWarnings("squid:S2386") // Make members protected.
  @SuppressFBWarnings("MS_MUTABLE_COLLECTION_PKGPROTECT")
  public static class XChainTypes {

    public static final String CHAIN_ADDRESS_FORMAT = "Chain_%d";

    public static final Set<TxType> XChainTxAddressTypes =
        new HashSet<>(Arrays.asList(TxType.REGISTER_ADDRESS)); // TxType.ADDRESS_PERMISSIONS

    // # ALL cross chain TX Types to move cross chain.
    public static final Set<TxType> XChainTxAllToList =
        new HashSet<>(Arrays.asList(TxType.GRANT_VOTING_POWER, TxType.REVOKE_VOTING_POWER, TxType.REGISTER_NAMESPACE,
            TxType.TRANSFER_NAMESPACE, TxType.REGISTER_ASSET_CLASS,
            TxType.UPDATE_ASSET_CLASS, TxType.REGISTER_ADDRESS, TxType.TRANSFER_ASSET_X_CHAIN,
            TxType.TRANSFER_ASSET_TO_MANY, TxType.TRANSFER_ASSET_AS_ISSUER, TxType.EXERCISE_ENCUMBRANCE
        )); // TxType.ADDRESS_PERMISSIONS

    public static final Set<TxType> XChainTxAssetTypes =
        new HashSet<>(Arrays.asList(TxType.TRANSFER_ASSET_X_CHAIN, TxType.TRANSFER_ASSET_TO_MANY, TxType.TRANSFER_ASSET_AS_ISSUER, TxType.EXERCISE_ENCUMBRANCE));

    // # All cross chain TX Types that specify a different 'tochain'.
    public static final Set<TxType> XChainTxAllAssetTypes = XChainTxAssetTypes;

    public static final Set<TxType> XChainTxBondTypes =
        new HashSet<>(Arrays.asList(TxType.GRANT_VOTING_POWER, TxType.REVOKE_VOTING_POWER));

    public static final Set<TxType> XChainTxClassTypes =
        new HashSet<>(Arrays.asList(TxType.REGISTER_ASSET_CLASS, TxType.UPDATE_ASSET_CLASS));

    public static final Set<TxType> XChainTxNamespaceTypes =
        new HashSet<>(Arrays.asList(TxType.REGISTER_NAMESPACE, TxType.TRANSFER_NAMESPACE));

  }

}
