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

import io.setl.bc.pychain.state.entry.MEntry;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A memory based mutable wrapper around a Merkle.
 *
 * @author aanten
 */
public abstract class AbstractMutableMerkle<ValueType extends MEntry>
    implements MutableMerkle<ValueType> {

  private static final Logger logger = LoggerFactory.getLogger(AbstractMutableMerkle.class);

  /** Entries changed in the current update. */
  protected final Map<String, NamedObject<ValueType>> changes = new HashMap<>();

  /** Un-changed entries to avoid having to repeatedly deserialize from storage. */
  protected final Map<String, NamedObject<ValueType>> unchanged = new HashMap<>();


  /**
   * Add an entry to this list. Does nothing if the key is already present, which means the current value is NOT changed.
   *
   * @param entry the new entry
   */
  @Override
  public void add(ValueType entry) {
    String key = entry.getKey();
    NamedObject<ValueType> current = findEntry(key, false);
    if (current != null) {
      // already present, unless deleted
      if (current.isDeleted) {
        logger.debug("Added entry with key {} as it was previously deleted.", key);
        current.isDeleted = false;
        current.sequence = nextSequence();
        current.object = entry;
      } else {
        logger.debug("AbstractMutableMerkle : Did not add entry with key {} as it was already present", key);
      }
      return;
    }

    current = new NamedObject<>(nextSequence(), true, key, entry);
    changes.put(entry.getKey(), current);
    logger.debug("Added entry with key {}", key);
  }


  /**
   * Accepts a bulk update from a wrapped mutable list.
   *
   * @param newChanges : changed entries
   */
  protected void bulkChange(Map<String, NamedObject<ValueType>> newChanges) {
    newChanges.forEach((key, value) -> {
      unchanged.remove(key);
      changes.put(key, value);
    });
  }





  @Override
  public void delete(String key) {
    // Try and get the entry
    NamedObject<ValueType> entry = findEntry(key, true);

    // Did the entry exist?
    if (entry == null || entry.isDeleted) {
      return;
    }

    entry.isDeleted = true;
    entry.sequence = nextSequence();
  }


  @Override
  public ValueType find(String key) {
    return find(key, false);
  }


  /**
   * Return value for requested key from underlying state structure. If required for update, the returned value
   * is duplicated and cached for later application to state.
   *
   * @param key       : Key to find value for e.g. Address.
   * @param forUpdate : If 'true', duplicate value and cache locally.
   *
   * @return : Value or null.
   */
  protected ValueType find(String key, boolean forUpdate) {
    NamedObject<ValueType> changedEntry = findEntry(key, forUpdate);
    if (changedEntry == null) {
      return null;
    }
    return changedEntry.isDeleted ? null : changedEntry.object;
  }


  @Override
  public ValueType findAndMarkUpdated(String key) {
    return find(key, true);
  }


  protected NamedObject<ValueType> findEntry(String key, boolean forUpdate) {
    // Already changed?
    NamedObject<ValueType> changedEntry = changes.get(key);
    if (changedEntry != null) {
      return changedEntry;
    }

    // Fetched and unchanged?
    changedEntry = unchanged.get(key);
    if (changedEntry != null) {
      // Unchanged entry exists.

      if (forUpdate) {
        // Migrate to changedEntries.

        changedEntry = changedEntry.copy(nextSequence(), false);
        changes.put(key, changedEntry);
        unchanged.remove(key);
      }

      // return.
      return changedEntry;
    }

    // previously untouched entry, need to fetch from wrapped state
    changedEntry = findEntryInState(key);
    if (changedEntry == null) {
      // entry does not exist
      return null;
    }

    if (forUpdate) {
      //Copy the object ready for modification
      NamedObject<ValueType> copy = changedEntry.copy(nextSequence(), false);
      changes.put(key, copy);
      return copy;
    }

    // store and return
    unchanged.put(key, changedEntry);
    return changedEntry;
  }


  protected abstract NamedObject<ValueType> findEntryInState(String key);


  /**
   * Get the number of entries that have been changed in this snapshot.
   *
   * @return : the number of changes
   */
  public int getChangedEntriesCount() {
    return changes.size();
  }



  @Override
  public Set<String> getUpdatedKeys() {
    return changes.keySet();
  }


  @Override
  public boolean itemExists(String key) {
    NamedObject<ValueType> changedEntry = changes.get(key);
    if (changedEntry != null) {
      return !changedEntry.isDeleted;
    }
    if (unchanged.containsKey(key)) {
      return true;
    }

    return itemExistsInState(key);
  }


  /**
   * Check if the item is known to the wrapped state.
   *
   * @param key the key
   *
   * @return true if the item is known
   */
  protected abstract boolean itemExistsInState(String key);


  abstract int nextSequence();


  /**
   * Reset this mutable list to match the underlying list.
   */
  public void reset() {
    unchanged.clear();
    changes.clear();
  }

}
