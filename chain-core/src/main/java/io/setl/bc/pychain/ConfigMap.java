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
package io.setl.bc.pychain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.setl.bc.pychain.msgpack.MPWrappedMap;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.bc.serialise.SafeMsgPackable;
import io.setl.util.CopyOnWriteMap;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import org.msgpack.core.MessagePacker;

/**
 * A special map to store the state configuration data. The map contains multiple settings which are converted to the correct type on access. The map also
 * supports serialization and digest derivation directly.
 *
 * @author Simon Greatrix on 18/12/2018.
 */
public class ConfigMap implements SafeMsgPackable {

  /**
   * The definition of a setting in the configuration. The setting has a name and a type. The definitions for Settings provide some mechanism to convert
   * whatever is actually stored in the map to the correct type.
   *
   * @param <Q> the required value type
   */
  public static class Setting<Q> {

    /** The conversion function. */
    private final Function<Object, Q> caster;

    /** The setting's name in the map. */
    private final String label;


    /**
     * Create new instance.
     *
     * @param label  the label
     * @param caster the conversion function
     */
    public Setting(String label, Function<Object, Q> caster) {
      this.label = label;
      this.caster = caster;
    }


    /**
     * Convert  the map value to the correct type.
     *
     * @param o the map value
     *
     * @return the actual setting value
     */
    public Q cast(Object o) {
      return caster.apply(o);
    }


    public String getLabel() {
      return label;
    }
  }



  /** The serialized form of this map, if known. */
  protected MPWrappedMap<String, Object> encoded = null;

  /** The hash of this map. */
  protected Hash hash = null;

  /** The digest type used to derive the hash. */
  protected int hashType = 0;

  /** The actual map where the settings are stored. */
  protected CopyOnWriteMap<String, Object> map = new CopyOnWriteMap<>(new TreeMap<>(), s -> {
    hash = null;
    encoded = null;
    return new TreeMap<>();
  });


  /**
   * Standard constructor.
   */
  public ConfigMap() {
    // do nothing
  }


  /**
   * Create an instance as a copy of another map. Used when deserializing to convert the input map to this class.
   *
   * @param encoded the original map.
   */
  @JsonCreator
  public ConfigMap(Map<String, Object> encoded) {
    map.replace(encoded);
  }


  /**
   * New instance which is a copy of another.
   *
   * @param original the original instance
   */
  public ConfigMap(ConfigMap original) {
    map = original.map;
    encoded = original.encoded;
    hash = original.hash;
    hashType = original.hashType;
  }


  /**
   * Convert this map into a regular map. Changes to the returned map are NOT reflected in this.
   *
   * @return a copy of this map as a regular map.
   */
  @JsonValue
  public Map<String, Object> asMap() {
    return map.copy();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConfigMap)) {
      return false;
    }
    ConfigMap configMap = (ConfigMap) o;
    return map.equals(configMap.map);
  }


  /**
   * Get a setting from this map.
   *
   * @param setting the setting to retrieve
   * @param <Q>     the setting's type
   *
   * @return the value, if present
   */
  public <Q> Q get(Setting<Q> setting) {
    Object o = map.get(setting.label);
    return (o != null) ? setting.cast(o) : null;
  }


  /**
   * Get a setting from this map.
   *
   * @param setting the setting to retrieve
   * @param dflt    a default value if not configured
   * @param <Q>     the setting's type
   *
   * @return the value, if present
   */
  public <Q> Q get(Setting<Q> setting, Q dflt) {
    Object o = map.get(setting.label);
    return (o != null) ? setting.cast(o) : dflt;
  }


  /**
   * Get the encoded form of this instance.
   *
   * @return the encoded data
   */
  public MPWrappedMap<String, Object> getEncoded() {
    if (encoded == null) {
      encoded = new MPWrappedMap<>(map);
    }
    return encoded;
  }


  /**
   * Get the hash of this map.
   *
   * @param digestType the type of digest to use
   *
   * @return the hash
   */
  public Hash getHash(int digestType) {
    if (hash == null || hashType != digestType) {
      Digest digest = Digest.create(digestType);
      hash = digest.digest(this);
      hashType = digestType;
    }
    return hash;
  }


  @Override
  public int hashCode() {
    return Objects.hash(map);
  }


  @Override
  public void pack(MessagePacker p) throws IOException {
    synchronized (map.getLock()) {
      MsgPackUtil.packAnything(p, map);
    }
  }


  /**
   * Put all the settings into this map.
   *
   * @param newValues the new values
   */
  public void putAll(Map<String, Object> newValues) {
    map.putAll(newValues);
  }


  /**
   * Replace all entries in this map with those in the new map.
   *
   * @param replacements the new values
   */
  public void replace(Map<String, Object> replacements) {
    map.replace(replacements);
  }
}

