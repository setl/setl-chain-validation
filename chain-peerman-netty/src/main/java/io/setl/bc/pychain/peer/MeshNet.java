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
package io.setl.bc.pychain.peer;

import static io.setl.bc.logging.LoggingConstants.MARKER_CONNECT;

import io.setl.common.CommonPy.NodeType;
import io.setl.common.Hex;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeshNet {

  private static final Logger logger = LoggerFactory.getLogger(MeshNet.class);



  protected static class RemoteAddresses {


    private final Map<String, Object> properties;

    private InetSocketAddress socketAddress;

    private NodeType nodeType;


    private String uuid;


    private int port;
    private String hostname;



    public RemoteAddresses(InetSocketAddress address,String hostname,int port,Map<String,Object> properties) {
      this.socketAddress = address;
      this.port=port;
      this.hostname=hostname;
      this.properties=properties;
    }


    public String getHostName() {
      return hostname;
    }


    public Map<String, Object> getProperties() {
      return properties;
    }


    public InetSocketAddress getSocketAddress() {
      return socketAddress;
    }

    public int getPort() {
      return port;
    }


    public String getUuid() {
      return uuid;
    }



    public void setUuid(String uuid) {
      this.uuid = uuid;
    }


    public NodeType getNodeType() {
      return nodeType;
    }


    public void setNodeType(NodeType nodeType) {
      this.nodeType = nodeType;
    }
  }


  static String toHostOrIP(InetAddress inetAddress) {
    // Standardize loopback to a single representation. This covers all 127.x.x.x and ::1.
    if (inetAddress.isLoopbackAddress()) {
      return "LOOPBACK";
    }

    // Handle link local addresses by interface
    if (inetAddress.isLinkLocalAddress() && inetAddress instanceof Inet6Address) {
      NetworkInterface ni = ((Inet6Address) inetAddress).getScopedInterface();
      if (ni != null) {
        return String.format("%s/%d", Hex.encode(inetAddress.getAddress()), ni.getIndex());
      }
    }

    return Hex.encode(inetAddress.getAddress());
  }


  static String toKey(InetSocketAddress inetSocketAddress) {
    if(inetSocketAddress.isUnresolved())
      return toKey(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
    return toKey(inetSocketAddress.getAddress(), inetSocketAddress.getPort());
  }


  static String toKey(String host, int port) {
    return String.format("%s:%d", host, port);
  }

  static String toKey(InetAddress inetAddress, int port) {
    return toKey(toHostOrIP(inetAddress), port);
  }


  private final List<String> localAddresses;

  private final Object mapLock = new Object();

  private Map<String, RemoteAddresses> remoteAddresses = new HashMap<>();


  public MeshNet(String[] localaddresses, int listenPort) {
    this.localAddresses = new ArrayList<>();

    this.localAddresses.add(String.format("LOOPBACK:%d", listenPort));

    for (String address : localaddresses) {
      try {
        InetAddress inet = InetAddress.getByName(address);
        localAddresses.add(toKey(inet, listenPort));
      } catch (UnknownHostException e) {
        continue;
      }
    }
  }


  /**
   * Get the addresses of all remote nodes.
   *
   * @return a copy of the collection of the addresses for all remote nodes.
   */
  public Collection<RemoteAddresses> getRemoteAddresses() {
    synchronized (mapLock) {
      return new ArrayList<>(remoteAddresses.values());
    }
  }


  /**
   * Get the list of connections that are required but not currently connected.
   *
   * @param l the list of currently connected connections.
   *
   * @return the list of connections that need to be established
   */
  public List<RemoteAddresses> getRequiredConnections(String uuid, List<InetSocketAddress> l) {
    List<RemoteAddresses> r = new ArrayList<>();
    Set<String> connected = new HashSet<>();
    for (InetSocketAddress a : l) {
      connected.add(toKey(a));
    }
    logger.debug(MARKER_CONNECT, "Number of Connected addresses: {}", connected.size());

    synchronized (mapLock) {
      Set<String> notConnected = new HashSet<>();
      remoteAddresses.forEach((k, v) -> {
        if (!connected.contains(k) && (uuid == null || !uuid.equals(v.getUuid()))) {
          notConnected.add(k);
        }
      });

      notConnected.removeAll(connected);
      logger.debug(MARKER_CONNECT, "Not-Connected Addresses: {}", notConnected);

      for (String c : notConnected) {
        if (!isLocalAddress(c)) {
          RemoteAddresses address = remoteAddresses.get(c);

          if (address.getPort() != 0) {
            logger.debug(MARKER_CONNECT, "Local address {} not equal to not-connected address {}", localAddresses.get(0), c);
            r.add(address);
          }
        }
      }
    }

    logger.debug(MARKER_CONNECT, "Required connection list: {}", r);

    return r;
  }

  private boolean isLocalAddress(String c) {
    return localAddresses.contains(c);
  }


  /**
   * Get the universally unique ID associated with a socket address, or null if there is no known association.
   *
   * @param socketAddress the socket address
   *
   * @return the associated UUID
   */
  public String getUuid(InetSocketAddress socketAddress) {
    RemoteAddresses ra;
    synchronized (mapLock) {
      ra = remoteAddresses.get(toKey(socketAddress));
    }
    return (ra != null) ? ra.uuid : null;
  }


  /**
   * Get type of node on the other end of connection.
   *
   * @param socketAddress the socket address
   * @return the associated UUID
   */
  public NodeType getNodeType(InetSocketAddress socketAddress) {
    RemoteAddresses ra;
    synchronized (mapLock) {
      ra = remoteAddresses.get(toKey(socketAddress));
    }
    return (ra != null) ? ra.nodeType : null;
  }


  public void registerListenAddress(RemoteAddresses remoteAddress) {
    synchronized (mapLock) {
      remoteAddresses.computeIfAbsent(toKey(remoteAddress.getSocketAddress()), k-> remoteAddress);
    }
  }


  /**
   * Register a socket that this mesh should connect to.
   *
   * @param socketAddress the socket address
   * @param port          the port
   */
  public void registerListenAddress(InetAddress socketAddress, int port, Map<String, Object> properties) {
    registerListenAddress(new RemoteAddresses(new InetSocketAddress(socketAddress.getHostName(), port),socketAddress.getHostName(), port, properties));
  }


  /**
   * Register a socket address that will be interested in listening to messages.
   *
   * @param ip   the IP address or host name
   * @param port the port number
   */
  public void registerListenAddress(String ip, int port, Map<String, Object> properties) {
    registerListenAddress(new RemoteAddresses(new InetSocketAddress(ip, port),ip, port, properties));
  }


  /**
   * Register a socket address as an origin.
   *
   * @param inetSocketAddress the socket address
   * @param uuid              the associated UUID
   * @param nodeType          node type
   */
  public void registerNode(InetSocketAddress inetSocketAddress, String uuid, NodeType nodeType) {
    RemoteAddresses ra;
    synchronized (mapLock) {
      ra = remoteAddresses.get(toKey(inetSocketAddress));
    }
    if (ra != null) {
      ra.setUuid(uuid);
      ra.setNodeType(nodeType);
    }
  }


}
