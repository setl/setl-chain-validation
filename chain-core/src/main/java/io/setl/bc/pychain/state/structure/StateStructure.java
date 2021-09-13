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
package io.setl.bc.pychain.state.structure;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.setl.bc.exception.NoStateFoundException;
import io.setl.bc.pychain.Digest;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.serialise.ContentFactory;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.HashStore;
import io.setl.bc.pychain.state.entry.HashWithType;
import java.security.DigestException;

/**
 * The structure of a state. This is equivalent to a Root instance except the sub structures are materialized rather than just having their identifying hashes.
 *
 * @author Simon Greatrix on 2019-02-22.
 */
public class StateStructure {

  private static final String P_MANAGEMENT = "management";

  private static final String P_MERKLES = "merkles";

  private static final String P_METADATA = "metadata";

  private static final String P_NAMED_DATA = "namedData";

  private static final String P_VERSION = "version";


  private static String getKeyDigestType(AbstractState state) {
    return Digest.name(Digest.getRecommended(state));
  }


  private static <T> T load(String type, ContentFactory<T> factory, HashStore store, Hash hash) throws NoStateFoundException {
    if (hash == null) {
      throw new NoStateFoundException("Invalid state object - cannot have null content hash for " + type);
    }
    byte[] data = store.get(hash);
    if (data == null) {
      throw new NoStateFoundException("No state object found of type " + type + " with hash " + hash.toHexString());
    }
    T value = factory.asValue(data);
    if (value == null) {
      throw new NoStateFoundException("Empty state object found of type " + type + " with hash " + hash.toHexString());
    }
    return value;
  }


  /**
   * Save the structural information of a state.
   *
   * @param loadContext the JSON load context
   * @param state       the state
   */
  public static void save(LoadContext loadContext, AbstractState state) {
    loadContext.setVersion(state.getVersion());
    loadContext.setKeyDigestType(Digest.typeForName(getKeyDigestType(state)));
    loadContext.setContentDigestType(Digest.getRecommended(state));

    ObjectNode data = (ObjectNode) loadContext.getOutput();
    data.set("keyDigestType", data.textNode(getKeyDigestType(state)));

    data.set(P_VERSION, data.numberNode(state.getVersion()));

    data.set(P_METADATA, Metadata.save(loadContext, state));
    data.set(P_MANAGEMENT, Management.save(loadContext, state));
    data.set(P_NAMED_DATA, NamedData.save(loadContext, state));
  }


  /**
   * Verify that an item is correctly stored. An item must be equal to the item that is stored under the hash, and the hash must match the digest of the item.
   *
   * @param value   the value
   * @param factory the factory to load the value
   * @param store   the store where the value is stored
   * @param hash    the typed hash for the value
   * @param <T>     the value type
   *
   * @throws DigestException if the item is incorrectly stored. May be due to missing or mismatched data rather than a problem with the digest.
   */
  public static <T> void verifyItem(T value, ContentFactory<T> factory, HashStore store, HashWithType hash) throws DigestException {
    byte[] data = store.get(hash.getHash());
    if (data == null) {
      throw new DigestException("No data found for " + hash.getHash().toHexString());
    }
    T stored = factory.asVerifiedValue(hash, data);
    if (!value.equals(stored)) {
      throw new DigestException("Stored value does not match loaded value");
    }
  }


  /** The hash of the loaded state. */
  Hash loadedHash;

  /** The management data associated with state. */
  Management management;

  /** The meta-data associated with state. */
  Metadata metadata;

  /** The map containing roots for named data. */
  NamedData namedData = new NamedData();

  /** The root instance equivalent to this structure. */
  Root root;


  StateStructure() {
    // do nothing
  }



  /**
   * Load a state structure from storage.
   *
   * @param store the storage
   * @param hash  the hash of the Root structure
   */
  public StateStructure(HashStore store, Hash hash) throws NoStateFoundException {
    loadedHash = hash;
    root = load("root", Root.FACTORY, store, hash);
    management = load(P_MANAGEMENT, Management.FACTORY, store, root.management.getHash());
    metadata = load(P_METADATA, Metadata.FACTORY, store, root.metadata.getHash());
    namedData = load(P_NAMED_DATA, NamedData.FACTORY, store, root.namedData.getHash());
  }


  /**
   * Create a structure for the given state.
   *
   * @param state the state
   */
  public StateStructure(AbstractState state) {
    int type = Digest.getRecommended(state);
    root = new Root(state, getKeyDigestType(state));
    management = new Management(state, type);
    metadata = new Metadata(state);
    namedData = new NamedData(state.getNamedDataHashes());

    root.management = management.computeHash(type);
    root.metadata = metadata.computeHash(type);
    root.namedData = new HashWithType(namedData.getHash(type), type);
    root.computeHash(type);
    loadedHash = root.getHash().getHash();
  }


  public Hash getLoadedHash() {
    return loadedHash;
  }


  public Management getManagement() {
    return management;
  }




  public Metadata getMetadata() {
    return metadata;
  }


  public NamedData getNamedData() {
    return namedData;
  }


  public Root getRoot() {
    return root;
  }


  /**
   * Save this structure to storage.
   *
   * @param store      the storage
   */
  public void save(HashStore store) {
    root.save(store);
    management.save(store);
    metadata.save(store);
  }




  /**
   * Verify this structure is properly stored.
   *
   * @param store the storage
   */
  public void verify(HashStore store, int rootHashType) throws DigestException {
    verifyItem(root, Root.FACTORY, store, new HashWithType(loadedHash, rootHashType));
    verifyItem(management, Management.FACTORY, store, root.management);
    verifyItem(metadata, Metadata.FACTORY, store, root.metadata);
    verifyItem(namedData, NamedData.FACTORY, store, root.namedData);
  }
}
