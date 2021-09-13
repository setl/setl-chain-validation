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
package io.setl.bc.pychain.state;

import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.serialise.hash.HashSerialisation;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.util.MsgPackUtil;
import java.util.HashSet;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug functionality to compare two complete states.
 */
public class DbgCompareState {

  private static final Logger logger = LoggerFactory.getLogger(DbgCompareState.class);


  private static <V extends MEntry> void compare(String name, Merkle<V> l0, Merkle<V> l1) {
    HashSet<String> seenKeys = new HashSet<>();
    for (V v0 : l0) {
      String key = v0.getKey();
      seenKeys.add(key);
      V v1 = l1.find(key);
      if (v1 == null) {
        logger.info("{} contains {} only on left hand side: {}", name, key, v0);
      } else if (!Objects.equals(v0, v1)) {
        logger.info("{} contains mismatched entries for {}: {} {}", name, key, v0, v1);
      }
    }

    for (V v1 : l1) {
      String key = v1.getKey();
      if (seenKeys.add(key)) {
        logger.info("{} contains {} only on right hand side: {}", name, key, v1);
      }
    }
  }


  /**
   * Debug Utility to compare State Objects.
   *
   * @param s0 :
   * @param s1 :
   */
  public static void compareStateObjects(AbstractState s0, AbstractState s1) {

    // Assets
    Merkle<AddressEntry> s0Assets = s0.getAssetBalances();
    Merkle<AddressEntry> s1Assets = s1.getAssetBalances();

    String assetbalancelisth = s0.getAssetBalances().computeRootHash();
    String assetbalancelisth1 = s1.getAssetBalances().computeRootHash();

    if (assetbalancelisth.compareTo(assetbalancelisth1) != 0) {
      compare("Asset balances", s0Assets, s1Assets);
    }

    // Contracts
    String contractslisth = s0.getContracts().computeRootHash();
    String contractslisth1 = s1.getContracts().computeRootHash();

    Merkle<ContractEntry> s0Contracts = s0.getContracts();
    Merkle<ContractEntry> s1Contracts = s1.getContracts();

    if (contractslisth.compareTo(contractslisth1) != 0) {
      compare("Contracts", s0Contracts, s1Contracts);
    }


  }


  /**
   * debugCompare.
   */
  @Deprecated
  public static void debugCompare(AbstractEncodedState s0, AbstractEncodedState s1) {

    String signodelisth = s0.getSignNodes().computeRootHash();
    String namespacelisth = s0.getNamespaces().computeRootHash();
    String assetbalancelisth = s0.getAssetBalances().computeRootHash();

    String signodelisth1 = s1.getSignNodes().computeRootHash();
    String namespacelisth1 = s1.getNamespaces().computeRootHash();
    String assetbalancelisth1 = s1.getAssetBalances().computeRootHash();

    if (signodelisth.compareTo(signodelisth1) != 0) {
      logger.info("signodelisth:{}!={}", signodelisth, signodelisth1);
    }

    if (namespacelisth.compareTo(namespacelisth1) != 0) {
      logger.info("namespacelisth:{}!={}", namespacelisth, namespacelisth1);
    }

    if (assetbalancelisth.compareTo(assetbalancelisth1) != 0) {

      logger.info("assetbalancelisth:{}!={}", assetbalancelisth, assetbalancelisth1);
      if (s0.getAssetBalances().debugGetChangedObjectArray() != null) {
        logger.info("state s0 changed:{}", MsgPackUtil.unpackWrapped(
            MsgPackUtil.pack(s0.getAssetBalances().debugGetChangedObjectArray()), true));
      } else {
        logger.info("state S0 nochnge:{}", s0.getAssetBalances().debugGetWrappedMerkleArray());
      }

      if (s1.getAssetBalances().debugGetChangedObjectArray() != null) {
        logger.info("state s1 changed:{}", MsgPackUtil.unpackWrapped(
            MsgPackUtil.pack(s1.getAssetBalances().debugGetChangedObjectArray()), true));
      } else {
        logger.info("state s1 nochnge:{}", s1.getAssetBalances().debugGetWrappedMerkleArray());
      }
    }

    final String thisXChainSignodesHash;
    MPWrappedMap<Long, Object[]> thisEncodedXChainSignodes = s0.getEncodedXChainSignNodes();

    if (thisEncodedXChainSignodes == null || thisEncodedXChainSignodes.size() == 0) {

      thisXChainSignodesHash = "";
    } else {

      Hash hash = HashSerialisation.getInstance().hash(thisEncodedXChainSignodes);
      thisXChainSignodesHash = hash.toHexString();
    }

    final String thisXChainSignodesHash1;
    thisEncodedXChainSignodes = s1.getEncodedXChainSignNodes();

    if (thisEncodedXChainSignodes == null || thisEncodedXChainSignodes.size() == 0) {

      thisXChainSignodesHash1 = "";
    } else {

      Hash hash = HashSerialisation.getInstance().hash(thisEncodedXChainSignodes);
      thisXChainSignodesHash1 = hash.toHexString();
    }
    if (thisXChainSignodesHash.compareTo(thisXChainSignodesHash1) != 0) {
      logger.info("thisXChainSignodesHash:{}!={}", thisXChainSignodesHash, thisXChainSignodesHash1);
    }

    String contractslisth = s0.getContracts().computeRootHash();
    String contractslisth1 = s1.getContracts().computeRootHash();

    if (contractslisth.compareTo(contractslisth1) != 0) {
      logger.info("contractslisth:{}!={}", contractslisth, contractslisth1);
      if (s0.getContracts().debugGetChangedObjectArray() != null) {
        logger.info("contract s0 changed:{}", MsgPackUtil.unpackWrapped(
            MsgPackUtil.pack(s0.getContracts().debugGetChangedObjectArray()), true));
      } else {
        logger.info("contract S0 nochnge:{}", s0.getContracts().debugGetWrappedMerkleArray());
      }

      if (s1.getContracts().debugGetChangedObjectArray() != null) {
        logger.info("contract s1 changed:{}", MsgPackUtil.unpackWrapped(
            MsgPackUtil.pack(s1.getContracts().debugGetChangedObjectArray()), true));
      } else {
        logger.info("contract S1 nochnge:{}", s1.getContracts().debugGetWrappedMerkleArray());
      }
    }

    String encumbranceslisth = s0.getEncumbrances().computeRootHash();
    String encumbranceslisth1 = s1.getEncumbrances().computeRootHash();

    if (encumbranceslisth.compareTo(encumbranceslisth1) != 0) {
      logger.info("encumbranceslisth:{}!={}", encumbranceslisth, encumbranceslisth1);
    }

    String poaHash =
        s0.getPowerOfAttorneys() == null ? "" : s0.getPowerOfAttorneys().computeRootHash();

    String poaHash1 =
        s1.getPowerOfAttorneys() == null ? "" : s1.getPowerOfAttorneys().computeRootHash();

    if (poaHash.compareTo(poaHash1) != 0) {
      logger.info("poaHash:{}!={}", poaHash, poaHash1);
    }

    final String thisunusedKeysHash;
    final String thisunusedKeysHash1;
    MPWrappedMap<String, Object[]> thisEncodedUnusedKeys;
    thisEncodedUnusedKeys = s0.getEncodedPrivilegedKeys();

    if (thisEncodedUnusedKeys == null || thisEncodedUnusedKeys.size() == 0) {

      thisunusedKeysHash = "";
    } else {
      Hash hash = HashSerialisation.getInstance().hash(s0.getEncodedPrivilegedKeys());
      thisunusedKeysHash = hash.toHexString();
    }

    thisEncodedUnusedKeys = s1.getEncodedPrivilegedKeys();

    if (thisEncodedUnusedKeys == null || thisEncodedUnusedKeys.size() == 0) {

      thisunusedKeysHash1 = "";
    } else {
      Hash hash = HashSerialisation.getInstance().hash(s1.getEncodedPrivilegedKeys());
      thisunusedKeysHash1 = hash.toHexString();
    }

    if (thisunusedKeysHash.compareTo(thisunusedKeysHash1) != 0) {
      logger.info("thisunusedKeysHash:{}!={}", thisunusedKeysHash, thisunusedKeysHash1);
    }

    final String thisLockedAssetsHash;

    if (s0.getEncodedLockedAssets() == null || s0.getEncodedLockedAssets().size() == 0) {

      thisLockedAssetsHash = "";
    } else {
      Hash hash = HashSerialisation.getInstance().hash(s0.getEncodedLockedAssets());
      thisLockedAssetsHash = hash.toHexString();
    }

    final String thisLockedAssetsHash1;

    if (s1.getEncodedLockedAssets() == null || s1.getEncodedLockedAssets().size() == 0) {

      thisLockedAssetsHash1 = "";
    } else {
      Hash hash = HashSerialisation.getInstance().hash(s1.getEncodedLockedAssets());
      thisLockedAssetsHash1 = hash.toHexString();
    }
    if (thisLockedAssetsHash.compareTo(thisLockedAssetsHash1) != 0) {
      logger.info("thisLockedAssetsHash:{}!={}", thisLockedAssetsHash, thisLockedAssetsHash1);
    }

    if (s0.getChainId() != s1.getChainId()) {
      logger.info("chainid:{}!={}", s0.getChainId(), s1.getChainId());
    }

    if (s0.getHeight() != s1.getHeight()) {
      logger.info("height:{}!={}", s0.getHeight(), s1.getHeight());
    }

    if (!s0.getBlockHash().equals(s1.getBlockHash())) {
      logger.info("blockHash:{}!={}", s0.getBlockHash(), s1.getBlockHash());
    }

    if (s0.getConfigHash().compareTo(s1.getConfigHash()) != 0) {
      logger.info("configHash:{}!={}", s0.getConfigHash(), s1.getConfigHash());
    }

    if (s0.getTimestamp() != s1.getTimestamp()) {
      logger.info("Timestamp:{}!={}", s0.getTimestamp(), s1.getTimestamp());
    }

  }


  /**
   * DbgCompareState, Private constructor to prevent instantiation of Static utility class.
   */
  private DbgCompareState() {

  }

}
