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
package io.setl.bc.pychain.state.entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.setl.bc.pychain.HashedMap;
import io.setl.bc.pychain.p2p.message.Encodable;
import io.setl.bc.pychain.state.monolithic.EncodedHashedMap;
import io.setl.common.Balance;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import org.msgpack.jackson.dataformat.MessagePackKeySerializer;

/**
 * @author Simon Greatrix on 30/12/2018.
 */
@JsonFormat(shape = Shape.ARRAY)
@JsonPropertyOrder(alphabetic = true)
public class XChainDetails implements Encodable {

  public static class Encoded extends EncodedHashedMap<Long, XChainDetails, Object[]> {

    public Encoded() {
      super(XChainDetails::encode);
    }


    public Encoded(Map<Long, Object[]> data) {
      super(XChainDetails::encode, XChainDetails::new, data);
    }
  }



  /**
   * Storage map for X-Chain details.
   */
  @JsonSerialize(keyUsing = MessagePackKeySerializer.class)
  public static class Store extends HashedMap<Long, XChainDetails> {

    public Store() {
      super();
    }


    @JsonCreator
    public Store(Map<Long, XChainDetails> data) {
      super(data);
    }
  }



  private final long blockHeight;

  private final int chainId;

  private final long parameters;

  private final Object[] rawSignNodes;

  private final SortedMap<String, Balance> signNodes;

  private final long status;


  /**
   * New instance from an encoded representation.
   *
   * @param v the encoded value
   */
  public XChainDetails(Object v) {
    Object[] array = (Object[]) v;
    chainId = ((Number) array[0]).intValue();
    blockHeight = ((Number) array[1]).intValue();
    rawSignNodes = (Object[]) array[2];
    parameters = ((Number) array[3]).longValue();
    status = ((Number) array[4]).longValue();

    signNodes = new TreeMap<>();
    for (int i = 0; i < rawSignNodes.length; i += 2) {
      signNodes.put(String.valueOf(rawSignNodes[i]), new Balance(rawSignNodes[i + 1]));
    }
  }


  /**
   * New instance.
   *
   * @param chainId     :
   * @param blockHeight :
   * @param signNodes   :
   * @param parameters  :
   * @param status      :
   */
  @JsonCreator
  public XChainDetails(
      @JsonProperty("chainId") int chainId,
      @JsonProperty("blockHeight") long blockHeight,
      @JsonProperty("signNodes") SortedMap<String, Balance> signNodes,
      @JsonProperty("parameters") long parameters,
      @JsonProperty("status") long status
  ) {
    this.blockHeight = blockHeight;
    this.chainId = chainId;
    this.parameters = parameters;
    this.signNodes = signNodes;
    this.status = status;

    this.rawSignNodes = new Object[signNodes.size() * 2];
    int i = 0;
    for (Entry<String, Balance> e : signNodes.entrySet()) {
      rawSignNodes[i++] = e.getKey();
      rawSignNodes[i++] = e.getValue().getValue();
    }
  }


  private XChainDetails(int chainId, long blockHeight, Object[] rawSignNodes, SortedMap<String, Balance> signNodes, long parameters, long status) {
    this.blockHeight = blockHeight;
    this.chainId = chainId;
    this.parameters = parameters;
    this.rawSignNodes = rawSignNodes;
    this.signNodes = signNodes;
    this.status = status;
  }


  public XChainDetails copy() {
    return this;
  }


  public Object[] encode() {
    return new Object[]{chainId, blockHeight, rawSignNodes, parameters, status};
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof XChainDetails)) {
      return false;
    }
    XChainDetails details = (XChainDetails) o;
    return blockHeight == details.blockHeight
        && chainId == details.chainId
        && parameters == details.parameters
        && status == details.status
        && signNodes.equals(details.signNodes);
  }


  @JsonProperty
  public long getBlockHeight() {
    return blockHeight;
  }


  @JsonProperty
  public int getChainId() {
    return chainId;
  }


  @Nonnull
  @JsonIgnore
  public Long getKey() {
    return Long.valueOf(chainId);
  }


  @JsonProperty
  public long getParameters() {
    return parameters;
  }


  @JsonProperty
  public SortedMap<String, Balance> getSignNodes() {
    return Collections.unmodifiableSortedMap(signNodes);
  }


  @JsonProperty
  public long getStatus() {
    return status;
  }


  @Override
  public int hashCode() {
    return Objects.hash(blockHeight, chainId, parameters, signNodes, status);
  }


  public XChainDetails setBlockHeight(long newHeight) {
    // Block height cannot be reduced
    return newHeight > blockHeight ? new XChainDetails(chainId, newHeight, rawSignNodes, signNodes, parameters, status) : this;
  }


  public XChainDetails setParameters(long newParameters) {
    return new XChainDetails(chainId, blockHeight, rawSignNodes, signNodes, newParameters, status);
  }


  /**
   * Create a new instance which changes the signing nodes.
   *
   * @param newNodes the new signing nodes
   *
   * @return the new instance
   */
  public XChainDetails setSignNodes(Map<String, Balance> newNodes) {
    TreeMap<String, Balance> newSignNodes = new TreeMap<>(newNodes);
    Object[] newRaw = new Object[newSignNodes.size() * 2];
    int i = 0;
    for (Entry<String, Balance> e : newSignNodes.entrySet()) {
      newRaw[i++] = e.getKey();
      newRaw[i++] = e.getValue().getValue();
    }
    return new XChainDetails(chainId, blockHeight, newRaw, newSignNodes, parameters, status);
  }


  public XChainDetails setStatus(long newStatus) {
    return new XChainDetails(chainId, blockHeight, rawSignNodes, signNodes, parameters, newStatus);
  }


  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("XChainDetails{");
    sb.append("blockHeight=").append(blockHeight);
    sb.append(", chainId=").append(chainId);
    sb.append(", parameters=").append(parameters);
    sb.append(", rawSignNodes=").append(Arrays.toString(rawSignNodes));
    sb.append(", signNodes=").append(signNodes);
    sb.append(", status=").append(status);
    sb.append('}');
    return sb.toString();
  }
}
