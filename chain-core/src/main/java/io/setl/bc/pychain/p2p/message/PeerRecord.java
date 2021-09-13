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
package io.setl.bc.pychain.p2p.message;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.msgpack.MPWrappedArrayImpl;
import io.setl.common.CommonPy.NodeType;
import io.setl.common.CommonPy.P2PType;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Simon Greatrix on 2019-03-26.
 */
public class PeerRecord implements Message {

  public static class Record {

    private String hostName;

    private NodeType nodeType;

    private int port;

    private String uuid;


    /**
     * New instance.
     *
     * @param hostName the host's name
     * @param port     the TCP/IP port
     * @param nodeType the node's type
     * @param uuid     the nodes unique identifier
     */
    public Record(String hostName, int port, NodeType nodeType, String uuid) {
      this.hostName = hostName;
      this.port = port;
      this.nodeType = nodeType != null ? nodeType : NodeType.Unknown;
      this.uuid = uuid;
    }


    Record(Object[] array) {
      hostName = (String) array[0];
      port = ((Number) array[1]).intValue();
      nodeType = NodeType.byId(((Number) array[2]).intValue());
      uuid = (String) array[3];
    }


    public Object[] encode() {
      return new Object[]{hostName, port, nodeType.id, uuid};
    }


    public String getHostName() {
      return hostName;
    }


    public NodeType getNodeType() {
      return nodeType;
    }


    public int getPort() {
      return port;
    }


    public String getUuid() {
      return uuid;
    }
  }



  private final MPWrappedArray message;

  private Collection<Record> records;


  public PeerRecord(MPWrappedArray message) {
    this.message = message;
  }


  public PeerRecord(int chainId, Collection<Record> records) {
    this.message = new MPWrappedArrayImpl(new Object[]{chainId, P2PType.PEER_RECORD.getId(), records.stream().map(Record::encode).toArray()});
    this.records = records;
  }


  @Override
  public Object[] encode() {
    return message.unwrap();
  }


  @Override
  public int getChainId() {
    return message.asInt(0);
  }


  /**
   * Get the details of individual peers.
   *
   * @return the records
   */
  public Collection<Record> getRecords() {
    if (records == null) {
      records = Arrays.stream(message.asObjectArray(2)).map(o -> new Record((Object[]) o)).collect(Collectors.toList());
    }
    return records;
  }


  @Override
  public P2PType getType() {
    return P2PType.PEER_RECORD;
  }
}
