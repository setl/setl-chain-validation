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
package io.setl.bc.pychain.state.yaml;

import io.setl.bc.exception.NotImplementedException;
import io.setl.bc.pychain.Hash;
import io.setl.bc.pychain.state.AbstractState;
import io.setl.bc.pychain.state.Merkle;
import io.setl.bc.pychain.state.SnapshotMerkle;
import io.setl.bc.pychain.state.entry.AddressEncumbrances;
import io.setl.bc.pychain.state.entry.AddressEntry;
import io.setl.bc.pychain.state.entry.CTEventsImpl;
import io.setl.bc.pychain.state.entry.ContractEntry;
import io.setl.bc.pychain.state.entry.ContractTimeEvents;
import io.setl.bc.pychain.state.entry.DataName;
import io.setl.bc.pychain.state.entry.LockedAsset;
import io.setl.bc.pychain.state.entry.MEntry;
import io.setl.bc.pychain.state.entry.NamedDatum;
import io.setl.bc.pychain.state.entry.NamespaceEntry;
import io.setl.bc.pychain.state.entry.PoaEntry;
import io.setl.bc.pychain.state.entry.PrivilegedKey;
import io.setl.bc.pychain.state.entry.SignNodeEntry;
import io.setl.bc.pychain.state.entry.XChainDetails;
import io.setl.bc.pychain.state.structure.NamedData;
import io.setl.common.TypeSafeMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Simon Greatrix on 13/02/2018.
 */
public class YamlState extends AbstractState {

  static Long getDate(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Number) {
      return ((Number) value).longValue();
    }

    if (value instanceof Date) {
      // Convert milliseconds to seconds
      return ((Date) value).getTime() / 1000;
    }

    if (value instanceof Calendar) {
      // Convert milliseconds to seconds
      return ((Calendar) value).getTimeInMillis() / 1000;
    }

    if (value instanceof TemporalAccessor) {
      // Convert milliseconds to seconds
      return (long) ((TemporalAccessor) value).get(ChronoField.INSTANT_SECONDS);
    }

    String text = value.toString();
    try {
      TemporalAccessor time = DateTimeFormatter.ISO_INSTANT.parse(text);
      return time.getLong(ChronoField.INSTANT_SECONDS);
    } catch (DateTimeParseException e) {
      return null;
    }
  }


  public static InputStream input(String resource) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
  }


  public static InputStream input(Class<?> context, String resource) {
    return context.getResourceAsStream(resource);
  }


  /**
   * Load a state instance from the provided stream.
   *
   * @param in the input stream
   *
   * @return the state
   */
  public static YamlState load(InputStream in) throws IOException {
    Yaml yaml = new Yaml();
    Object obj = yaml.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    if (!(obj instanceof Map)) {
      throw new IOException("Input was not a YAML map");
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) obj;
    TypeSafeMap myState = new TypeSafeMap(map);

    return new YamlState(myState);
  }


  private YamlAssetBalances assetBalanceList;

  private YamlContractsList contractsList;

  private YamlEncumbrancesList encumbrancesList;

  private YamlLockedAssetsList lockedAssetsList;

  private YamlNamespaceList namespaceList;

  private YamlPoaList poaList;

  private YamlSignodeList signodeList;


  private YamlState(TypeSafeMap myState) {
    super(
        myState.getInt("chainId", 0),
        myState.getInt("version", 3),
        myState.getInt("height", 100),
        Hash.fromHex(myState.getString("loadedHash", "0000000000000000")),
        Hash.fromHex(myState.getString("blockHash", "1000000000000000")),
        getDate(myState.getOrDefault("timestamp", "1970-01-01T00:00:00Z"))
    );
    privilegedKeys = new PrivilegedKey.Store();
    xChainSignNodes = new XChainDetails.Store();
    assetBalanceList = new YamlAssetBalances(myState);
    contractsList = new YamlContractsList(myState);
    encumbrancesList = new YamlEncumbrancesList(myState);
    namespaceList = new YamlNamespaceList(myState);
    poaList = new YamlPoaList(myState);
    signodeList = new YamlSignodeList(myState);
    lockedAssetsList = new YamlLockedAssetsList(myState);

    TypeSafeMap config = myState.getMap("config");
    this.config.putAll(config);
    initMerkleNames();
  }


  /**
   * Copy constructor.
   *
   * @param source the state to copy
   */
  public YamlState(AbstractState source) {
    super(source);
    initMerkleNames();
  }


  @Override
  public boolean anyPendingEventTime(long eventTime) {
    throw new NotImplementedException();
  }


  /**
   * Get this state as a map.
   *
   * @return this state as a map
   */
  public Map<String, Object> asMap() {
    LinkedHashMap<String, Object> state = new LinkedHashMap<>();
    state.put("chainId", getChainId());
    state.put("version", getVersion());
    state.put("height", getHeight());
    state.put("loadedHash", getLoadedHash().toHexString());
    state.put("blockHash", getBlockHash().toHexString());
    state.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(getTimestamp())));

    assetBalanceList.save(state);
    contractsList.save(state);
    encumbrancesList.save(state);
    namespaceList.save(state);
    poaList.save(state);
    signodeList.save(state);
    lockedAssetsList.save(state);

    return state;
  }


  @Override
  protected void computeRootHash() {
    // This state has no fixed representation and so does not support calculating a hash.
  }


  @Override
  protected <X extends MEntry> SnapshotMerkle<X> createMerkle(Class<X> name) {
    throw new UnsupportedOperationException();
  }


  @Override
  public Merkle<AddressEntry> getAssetBalances() {
    return assetBalanceList;
  }


  @Override
  public ContractTimeEvents getContractTimeEvents() {
    return contractTimeEvents;
  }


  @Override
  public Merkle<ContractEntry> getContracts() {
    return contractsList;
  }


  @Override
  public <N extends NamedDatum<N>> N getDatum(DataName<N> name) {
    if (name.equals(ContractTimeEvents.NAME)) {
      // Assume contract events does not need a hash store.
      return name.getType().cast(getContractTimeEvents());
    }
    throw new NoSuchElementException();
  }


  @Override
  public Merkle<AddressEncumbrances> getEncumbrances() {
    return encumbrancesList;
  }


  @Override
  public Merkle<LockedAsset> getLockedAssets() {
    return lockedAssetsList;
  }


  @Override
  protected AbstractState getMutableCopy() {
    return isInitialised ? new YamlState(this) : this;
  }


  @Override
  public NamedData getNamedDataHashes() {
    // not supported, so return empty list
    return new NamedData();
  }


  @Override
  public Merkle<NamespaceEntry> getNamespaces() {
    return namespaceList;
  }


  @Override
  public Merkle<PoaEntry> getPowerOfAttorneys() {
    return poaList;
  }


  @Override
  public Merkle<SignNodeEntry> getSignNodes() {
    return signodeList;
  }


  @Override
  public XChainDetails getXChainSignNodesValue(Number keyName) {
    throw new NotImplementedException();
  }


  /**
   * Initialise the by-name lookup of the fixed Merkles.
   */
  private void initMerkleNames() {
    merkles.put(AddressEntry.class, assetBalanceList);
    merkles.put(ContractEntry.class, contractsList);
    merkles.put(AddressEncumbrances.class, encumbrancesList);
    merkles.put(LockedAsset.class, lockedAssetsList);
    merkles.put(NamespaceEntry.class, namespaceList);
    merkles.put(PoaEntry.class, poaList);
  }


  @Override
  protected void initialiseMerklesFrom(AbstractState source) {
    // Create empty Merkles
    assetBalanceList = new YamlAssetBalances();
    contractsList = new YamlContractsList();
    encumbrancesList = new YamlEncumbrancesList();
    namespaceList = new YamlNamespaceList();
    poaList = new YamlPoaList();
    lockedAssetsList = new YamlLockedAssetsList();

    initMerkleNames();

    // Invoke super class to perform a copy
    super.initialiseMerklesFrom(source);
  }


  @Override
  protected void initialiseNamedDataFrom(AbstractState source) {
    // do nothing
  }


  @Override
  protected void initialisePrivilegedKeysFrom(AbstractState source) {
    privilegedKeys = new PrivilegedKey.Store();
    privilegedKeys.replace(source.getPrivilegedKeys());
  }


  @Override
  protected void initialiseSignNodesFrom(AbstractState source) {
    signodeList = new YamlSignodeList();
    super.initialiseSignNodesFrom(source);
  }


  @Override
  protected void initialiseXChainSignNodesFrom(AbstractState source) {
    xChainSignNodes = new XChainDetails.Store();
    xChainSignNodes.replace(source.getXChainSignNodes());
  }


  @Override
  public boolean isAssetLocked(String assetName) {
    throw new NotImplementedException();
  }


  @Override
  public long nextPendingEventTime() {
    throw new NotImplementedException();
  }


  @Override
  public SortedSet<String> pendingEventTimeAddresses(long eventTime) {
    throw new NotImplementedException();
  }


  /**
   * Save this state as YAML.
   *
   * @return the YAML representation of this state.
   */
  public String save() {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setAllowReadOnlyProperties(true);
    dumperOptions.setIndent(2);
    dumperOptions.setPrettyFlow(true);
    Yaml yaml = new Yaml(dumperOptions);
    return yaml.dump(asMap());
  }


  @Override
  protected void saveNamedData() {
    // do nothing
  }


  @Override
  public void setXChainSignNodes(Map<Long, XChainDetails> newXChainSignNodes, Set<Long> removedXChainSignodes, boolean overwrite) {
    if (newXChainSignNodes.isEmpty() && removedXChainSignodes.isEmpty()) {
      // We can ignore this
      return;
    }
    throw new NotImplementedException();
  }


  @Override
  public boolean verifyAll() {
    return true;
  }
}
