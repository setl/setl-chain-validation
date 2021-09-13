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

import static io.setl.bc.logging.LoggingConstants.MARKER_BROADCAST;
import static io.setl.bc.logging.LoggingConstants.MARKER_CONNECT;
import static io.setl.bc.logging.LoggingConstants.MARKER_PERFORMANCE;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.setl.bc.pychain.msgpack.MPWrappedArray;
import io.setl.bc.pychain.p2p.MsgFactory;
import io.setl.bc.pychain.p2p.message.CheckOrigin;
import io.setl.bc.pychain.p2p.message.ListenPort;
import io.setl.bc.pychain.p2p.message.Message;
import io.setl.bc.pychain.p2p.message.PeerRecord;
import io.setl.bc.pychain.p2p.message.PeerRecord.Record;
import io.setl.bc.pychain.p2p.message.PeerRequest;
import io.setl.bc.pychain.p2p.message.TxPackage;
import io.setl.bc.pychain.p2p.message.UnknownMessage;
import io.setl.bc.pychain.peer.MeshNet.RemoteAddresses;
import io.setl.bc.pychain.peer.pipeline.FrameDecoder;
import io.setl.bc.pychain.util.MsgPackUtil;
import io.setl.common.CommonPy.NodeType;
import io.setl.common.CommonPy.P2PType;
import io.setl.common.Pair;
import io.setl.common.Sync;
import io.setl.util.CopyOnWriteSet;
import io.setl.util.LoggedThread;
import io.setl.util.Priorities;
import io.setl.util.PriorityExecutor;

/**
 * Peer manager using netty and implementing pychain compatible message pipeline.
 *
 * @author aanten
 */
@Component
@ConditionalOnProperty(value = "transport", havingValue = "netty")
@Primary
public class NettyPeerManager implements PeerManager, DisposableBean, InitializingBean, ConnectionHandler {

  public static final AttributeKey<Long> ACTIVATE_TIME = AttributeKey.valueOf("ACTIVATE_TIME");

  public static final AttributeKey<NodeType> NODE_TYPE = AttributeKey.valueOf("NODE_TYPE");

  public static final AttributeKey<Sync<TxPackageNotifyTask>> NOTIFY_TASK = AttributeKey.valueOf("NOTIFY_TASK");

  public static final AttributeKey<String> ORIGIN_KEY = AttributeKey.valueOf("ORIGIN_KEY");

  public static final AttributeKey<Boolean> READY = AttributeKey.valueOf("CTYPE");

  private static final long CHANNEL_ACTIVATE_TIMEOUT_MS = 30000;

  private static final EnumSet<P2PType> PRIORITY_MESSAGE = EnumSet
      .of(
          P2PType.PREPARING_PROPOSAL, P2PType.EMPTY_PROPOSAL, P2PType.ITEM_REQUEST, P2PType.PROPOSAL, P2PType.SIGNATURE, P2PType.VOTE,
          P2PType.TX_PACKAGE_RESPONSE, P2PType.PROPOSED_TXS, P2PType.BLOCK_COMMITTED
      );


  private static final Logger logger = LoggerFactory.getLogger(NettyPeerManager.class);

  private static final ChannelFutureListener channelErrorListener = future -> {
    if (!future.isSuccess()) {
      logger.error(MARKER_CONNECT, "sendError:", future.cause());
    }
  };


  private static void handleUnreadyChannel(Channel channel) {
    if (channel.hasAttr(ACTIVATE_TIME) && channel.attr(ACTIVATE_TIME).get() != null) {
      Long time = channel.attr(ACTIVATE_TIME).get();
      if (time.longValue() + CHANNEL_ACTIVATE_TIMEOUT_MS < System.currentTimeMillis()) {
        logger.warn(MARKER_CONNECT, "Channel to {} has not completed activation after {}ms. Closing channel.", channel.remoteAddress(),
            CHANNEL_ACTIVATE_TIMEOUT_MS
        );
        channel.close();
      }
    } else {
      channel.attr(ACTIVATE_TIME).set(Long.valueOf(System.currentTimeMillis()));
    }
  }


  public static void writeAndFlush(ChannelOutboundInvoker channel, Object[] msg) {
    ByteBuf buffer = Unpooled.wrappedBuffer(MsgPackUtil.pack(msg));
    channel.writeAndFlush(buffer);
  }


  @Sharable
  protected class P2P extends ChannelInboundHandlerAdapter {

    static final String NODE_NAME = "changeMe";

    private boolean flowPaused = false;

    //TODO
    //private final Map<P2PType,MessageHandler> messageHandlers;


    /**
     * Invoked when a channel becomes active. Adds the channel to those which this PeerManager knows about.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      activeConnectionSet.add(ctx);
      ctx.channel().attr(ACTIVATE_TIME).set(Long.valueOf(System.currentTimeMillis()));
      logger.debug(MARKER_CONNECT, "Connection to {} added for context {}. Current number of connections: {}", ctx.channel().remoteAddress(), ctx,
          activeConnectionSet.size()
      );
      ctx.fireChannelActive();
    }


    /**
     * Invoked when a channel becomes inactive. Removes the channel from those which this PeerManager knows about.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      activeConnectionSet.remove(ctx);
      logger.debug(MARKER_CONNECT, "Disconnect from address: {}. Current number of connections: {}", ctx.channel().remoteAddress(), activeConnectionSet.size());
      ctx.fireChannelInactive();
    }


    @Override
    public void channelRead(final ChannelHandlerContext channelContext, Object msg) {
      Message message = msgFactory.create((MPWrappedArray) msg);
      int msgChainId = message.getChainId();
      logger.trace("received message [{}]", message.getType().name());
      if (NettyPeerManager.this.chainsId == null) {
        logger.error("no chains defined, i won't handle message");
        return;
      }

      if (Collections.binarySearch(NettyPeerManager.this.chainsId, msgChainId) < 0) {
        logger.error("Incorrect chain {}", msgChainId);
        return;
      }

      if (!checkChannel(channelContext)) {
        logger.warn("Message come from node which shares uuid with this node, connection will be dropped");
        channelContext.disconnect();
        return;
      }

      P2PType type = message.getType();
      if (type == P2PType.UNKNOWN) {
        logger.error("Unknown message type: {}", ((UnknownMessage) message).getTypeId());
        return;
      }

      switch (type) {
        case CLOSE_REQUEST:
          logger.info(MARKER_CONNECT, "CLOSE REQUEST");
          channelContext.channel().close();
          break;
        case PEER_RECORD:
          executor.submit(Priorities.NETWORK_MANAGE, () -> handlePeerRecord((PeerRecord) message));
          break;
        case PEER_REQUEST:
          executor.submit(Priorities.NETWORK_MANAGE, () -> handlePeerRequest(message, channelContext));
          break;
        case LISTEN_PORT:
          if (peerlistonly == false) {
            executor.submit(Priorities.NETWORK_MANAGE, () -> handleListenPort(
                (ListenPort) message,
                (InetSocketAddress) channelContext.channel().remoteAddress()
            ));
          }
          break;
        case CHECK_ORIGIN:
          executor.submit(Priorities.NETWORK_MANAGE, () -> handleCheckOrigin((CheckOrigin) message, channelContext));
          break;
        case TX_PACKAGE_ORIGINAL:
          if (broadcastTransactions) {
            enqueuePackageForward((TxPackage) message);
          }
          enqueueTxReceived(channelContext, (TxPackage) message);
          break;
        case TX_PACKAGE_FORWARD:
          enqueueTxReceived(channelContext, (TxPackage) message);
          break;
        default:
          executor.submit(
              PRIORITY_MESSAGE.contains(type) ? Priorities.PROPOSAL : Priorities.DEFAULT,
              () -> fireBlockchainEventReceived(channelContext, message)
          );
          break;
      }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      // Log the error and close the channel
      logger.error(MARKER_CONNECT, "Peer Manager channel error:", cause);
      ctx.close();
      activeConnectionSet.remove(ctx);
      logger.debug(MARKER_CONNECT, "Disconnect from address: {} after error. Current number of connections: {}", ctx.channel().remoteAddress(),
          activeConnectionSet.size()
      );
    }


    private void fireBlockchainEventReceived(ChannelHandlerContext ctx, Message message) {
      NettyPeerAddress addr = new NettyPeerAddress(ctx.channel());

      for (BlockChainListener listener : bcListener) {
        listener.eventReceived(addr, message);
      }
    }


    private void handleCheckOrigin(CheckOrigin checkOrigin, ChannelHandlerContext channelContext) {
      Channel channel = channelContext.channel();
      InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.remoteAddress();
      String uuid = checkOrigin.getUniqueNodeIdentifier();

      logger.error(MARKER_CONNECT, "Received origin message from {} channel is ready={}", inetSocketAddress, channel.attr(READY));
      if (!channel.hasAttr(ORIGIN_KEY) || (channel.hasAttr(ORIGIN_KEY) && channel.attr(ORIGIN_KEY).get() == null)) {
        if (channel.hasAttr(ORIGIN_KEY) && channel.attr(ORIGIN_KEY).get() == null) {
          logger.warn("ORIGIN key is set to null, it was read before check");
        }

        NodeType nodeType = checkOrigin.getNodeType();
        logger.info(MARKER_CONNECT, "Origin:{} {}", uuid, nodeType);
        meshNet.registerNode(inetSocketAddress, uuid, nodeType);
        channelContext.channel().attr(ORIGIN_KEY).set(uuid);
        channelContext.channel().attr(NODE_TYPE).set(checkOrigin.getNodeType());
      }
      if (!channelContext.channel().hasAttr(READY) || (channelContext.channel().hasAttr(READY) && (channelContext.channel().attr(READY).get() == null
          || !channelContext.channel().attr(READY).get().equals(Boolean.TRUE)))) {
        channelContext.channel().attr(READY).set(true);
        writeAndFlush(channelContext, new MsgFactory().checkOrigin(checkOrigin.getChainId(), uniqueNodeIdentifier, nodeType));
      }
    }


    private void handleListenPort(ListenPort listenPort, InetSocketAddress socketAddress) {
      int port = listenPort.getListenPort();
      Map<String, Object> properties = new HashMap<>();
      properties.put("chain", listenPort.getChainId());

      meshNet.registerListenAddress(socketAddress.getAddress(), port, properties);
      logger.info(MARKER_CONNECT, "Listen port:{}", port);
    }


    private void handlePeerRecord(PeerRecord peerRecord) {
      if (ignorePeerList || autodiscover == false) {//TODO should this be checked before task is submitted?
        return;
      }

      Collection<Record> peers = peerRecord.getRecords();
      for (Record peer : peers) {
        if (logger.isTraceEnabled(MARKER_CONNECT)) {
          logger.trace(MARKER_CONNECT, "Peer: {} {} name:{}", peer.getHostName(), peer.getPort(), peer.getUuid());
        }

        InetSocketAddress isa = new InetSocketAddress(peer.getHostName(), peer.getPort());

        //walkaround to provide chain id
        Map<String, Object> properties = new HashMap<>();
        properties.put("chain", peerRecord.getChainId());

        meshNet.registerListenAddress(peer.getHostName(), peer.getPort(), properties);
        meshNet.registerNode(isa, peer.getUuid(), peer.getNodeType());
      }
    }


    private void handlePeerRequest(Message message, ChannelHandlerContext context) {
      if (ignorePeerList) {
        return;
      }

      Collection<RemoteAddresses> rads = meshNet.getRemoteAddresses();
      ArrayList<Record> peers = new ArrayList<>(rads.size());
      for (RemoteAddresses r : rads) {
        peers.add(new Record(r.getHostName(), r.getPort(), r.getNodeType(), r.getUuid()));
      }
      writeAndFlush(context, new MsgFactory().peerRequestResponse(message.getChainId(), peers));
    }

  }



  private class TxPackageForwardTask extends TxPackageTask {

    TxPackageForwardTask(TxPackage original) {
      super(new TxPackage(P2PType.TX_PACKAGE_FORWARD, original));
    }


    void doIt() {
      broadcast(msg);
    }

  }



  private class TxPackageNotifyTask extends TxPackageTask {

    private final PeerAddress peerAddress;


    TxPackageNotifyTask(PeerAddress peerAddress, TxPackage original) {
      super(original);
      this.peerAddress = peerAddress;
    }


    void doIt() {
      // notify the listeners
      for (TransactionListener listener : txListener) {
        listener.transactionReceived(peerAddress, msg);
      }
    }

  }



  private abstract class TxPackageTask implements Runnable {

    final TxPackage msg;

    boolean isDone = false;


    TxPackageTask(TxPackage msg) {
      this.msg = msg;
    }


    public boolean add(TxPackage original) {
      synchronized (this) {
        if (isDone) {
          return false;
        }

        // Chain-IDs must match
        if (msg.getChainId() != original.getChainId()) {
          return false;
        }

        // will merged packages be a reasonable size?
        if (msg.size() + original.size() > txPackageLimit) {
          return false;
        }

        // merge the two packages
        msg.merge(original);
        return true;
      }
    }


    abstract void doIt();


    public void run() {
      synchronized (this) {
        // do not send twice
        if (isDone) {
          return;
        }
        isDone = true;
      }
      logger.debug("Forwarding {} transactions", msg.size());
      doIt();
    }

  }


  public void setTxPackageLimit(int txPackageLimit) {
    this.txPackageLimit = txPackageLimit;
  }


  private final Set<ChannelHandlerContext> activeConnectionSet = new CopyOnWriteSet<>();

  private final List<BlockChainListener> bcListener = new CopyOnWriteArrayList<>();

  private final ChannelInitializer<SocketChannel> channelInitialiser;

  final private ConnectionHandler connectionHandler;

  private final PriorityExecutor executor;

  final private NodeType nodeType;

  // List of TxListeners to notify of incoming TxMessages. At present, this is the ValidationNode object.
  private final List<TransactionListener> txListener = new CopyOnWriteArrayList<>();

  Bootstrap clientBS = new Bootstrap();

  /**
   * Is this instance being managed by Spring?.
   */
  boolean isNotUsingSpring = true;

  private MsgFactory msgFactory = new MsgFactory();

  /**
   * Timer for scheduling, if Spring is not available.
   */
  Timer timer = null;

  @Value("${p2p.autodiscover:true}")
  private boolean autodiscover;

  private boolean broadcastTransactions;

  private List<Integer> chainsId;

  @Value("${p2p.ignore-peerlist:false}")
  private boolean ignorePeerList;

  /**
   * Has this peer manager been started?.
   */
  private boolean isRunning = false;

  @Value("${p2p.listen.port}")
  private int listenPort;

  @Value("${p2p.localaddresses:localhost}")
  private String[] localaddresses;

  private MeshNet meshNet;

  private P2P p2p;

  private List<Pair<String, Map<String, Object>>> peerList = new ArrayList<>();

  @Value("${p2p.peer.list:}")
  private String[] peerListProp;

  @Value("${p2p.peer.host:}")
  private String remoteHost = "localhost";

  @Value("${p2p.peer.port:0}")
  private int remotePort;

  @Value("${p2p.peerlistonly:true}")
  private boolean peerlistonly;

  private Sync<TxPackageForwardTask> txPackageForwardTask = new Sync<>();

  @Value("${txPackageLimit:1000}")
  private int txPackageLimit;

  private String uniqueNodeIdentifier;

  private EventLoopGroup workerGroup = new NioEventLoopGroup();

  private EventLoopGroup bossGroup = workerGroup;


  /**
   * Create a new instance.
   */
  @Autowired
  public NettyPeerManager(@Autowired(required = false) PriorityExecutor priorityExecutor, NodeType nodeType, FrameDecoder frameDecoder) {
    executor = (priorityExecutor != null) ? priorityExecutor : PriorityExecutor.INSTANCE;
    this.nodeType = nodeType;
    clientBS.group(workerGroup);
    clientBS.channel(NioSocketChannel.class);
    clientBS.option(ChannelOption.SO_KEEPALIVE, true);
    channelInitialiser = new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        frameDecoder.addPipeline(pipeline);
        SimplePipeline.build(pipeline);
        pipeline.addLast(p2p);
      }
    };
    clientBS.handler(channelInitialiser);
    connectionHandler = this;
  }


  public NettyPeerManager(PriorityExecutor priorityExecutor, NodeType nodeType, FrameDecoder frameDecoder, ConnectionHandler connectionHandler) {
    executor = (priorityExecutor != null) ? priorityExecutor : PriorityExecutor.INSTANCE;
    this.nodeType = nodeType;
    clientBS.group(workerGroup);
    clientBS.channel(NioSocketChannel.class);
    clientBS.option(ChannelOption.SO_KEEPALIVE, true);
    channelInitialiser = new ChannelInitializer<SocketChannel>() {
      @Override
      public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        frameDecoder.addPipeline(pipeline);
        SimplePipeline.build(pipeline);
        pipeline.addLast(p2p);
      }
    };
    clientBS.handler(channelInitialiser);
    this.connectionHandler = connectionHandler;
  }


  @Override
  public void addListener(BlockChainListener blockChainListener) {
    bcListener.add(blockChainListener);
  }


  @Override
  public void addPeer(String name, int port, Map<String, Object> properties) {
    //TODO add properties
    if (isRunning) {
      registerAndConnectRemotePeer(name, port, properties);
    } else {
      remoteHost = "";
      remotePort = 0;
      String nameAndPort = name + ":" + port;
      peerList.add(new Pair<>(nameAndPort, properties));
    }
  }


  @Override
  public void addTransactionListener(TransactionListener listener) {
    txListener.add(listener);
  }


  @Override
  public void afterPropertiesSet() throws Exception {
    isNotUsingSpring = false;
  }


  @Override
  public void broadcast(Message message) {
    ArrayList<ChannelHandlerContext> channels = new ArrayList<>(activeConnectionSet);
    logger.debug("Broadcast:{}={}", channels.size(), message.getType());
    if (channels.isEmpty()) {
      return;
    }

    // Priority messages go out on the first channel for a UUID. Non-priority messages go out on the last channel for a UUID. As the mesh is doubly-linked,
    // this normally puts priority messages on a separate channel from the non-priority ones.
    if (!PRIORITY_MESSAGE.contains(message.getType())) {
      Collections.reverse(channels);
    }

    //Pre-pack broadcast messages to remove multi-pack overhead
    byte[] binary = MsgPackUtil.pack(message.encode());

    // Don't broadcast to self
    Set<String> sentUUIDs = new HashSet<>();
    if (uniqueNodeIdentifier != null) {
      sentUUIDs.add(uniqueNodeIdentifier);
    }

    // Note direct write to channel may fill buffers - some mechanism to back pressure needed ?
    for (ChannelHandlerContext c : channels) {
      Channel channel = c.channel();
      InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
      NodeType nodeType = channel.attr(NODE_TYPE).get();
      if (nodeType == NodeType.Unknown) {
        logger.info(MARKER_BROADCAST, "Not broadcasting to {} as it is an unknown node type", address);
        continue;
      }

      String uuid = null;

      if (channel.hasAttr(ORIGIN_KEY)) {
        uuid = channel.attr(ORIGIN_KEY).get();
      }

      if (!channel.hasAttr(READY) || channel.attr(READY).get() == null || !channel.attr(READY).get()) {
        logger.warn(MARKER_BROADCAST, "Broadcast to:{} failed, connection is not ready", address);
        handleUnreadyChannel(channel);
        continue;
      }

      if (uuid == null) {
        uuid = meshNet.getUuid(address);
      }
      if (uuid == null || sentUUIDs.add(uuid)) {
        logger.debug(MARKER_BROADCAST, "Broadcast to:{} {}", address, nodeType);

        c.writeAndFlush(Unpooled.wrappedBuffer(binary));
      }
    }
    logger.debug(MARKER_BROADCAST, "Broadcast complete");
  }


  private boolean checkChannel(ChannelHandlerContext channelContext) {
    if (channelContext.channel().hasAttr(ORIGIN_KEY)) {
      String uuid = channelContext.channel().attr(ORIGIN_KEY).get();
      return !uniqueNodeIdentifier.equals(uuid);
    }
    return true;
  }


  private ChannelFuture connect(String host, int port) {
    return clientBS.connect(host, port);
  }


  @Override
  public void consensusFinished() {
    for (ChannelHandlerContext chc : activeConnectionSet) {
      Channel channel = chc.channel();
      InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
      NodeType nodeType = channel.attr(NODE_TYPE).get();
      if (nodeType == NodeType.Unknown) {
        logger.info(MARKER_CONNECT, "Enabling auto-read from {}", address);
        channel.config().setAutoRead(true);
        channel.read();
      }
    }
  }


  @Override
  public void consensusStarted() {
    synchronized (txPackageForwardTask) {
      TxPackageForwardTask task = txPackageForwardTask.get();
      if (task != null && !task.isDone) {
        task.run();
      }
    }

    for (ChannelHandlerContext chc : activeConnectionSet) {
      Channel channel = chc.channel();
      InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
      NodeType nodeType = channel.attr(NODE_TYPE).get();
      if (nodeType == NodeType.Unknown) {
        logger.info(MARKER_CONNECT, "Disabling auto-read from {}", address);
        channel.config().setAutoRead(false);
      }
    }
  }


  @Override
  public void destroy() throws Exception {
    stop();
  }


  void enqueuePackageForward(TxPackage msg) {
    TxPackageForwardTask newTask = null;
    synchronized (txPackageForwardTask) {
      TxPackageForwardTask task = txPackageForwardTask.get();
      if (task == null || !task.add(msg)) {
        newTask = new TxPackageForwardTask(msg);
        txPackageForwardTask.set(newTask);
      }
    }

    if (newTask != null) {
      executor.submit(Priorities.NETWORK_WRITE, newTask);
    }
  }


  void enqueueTxReceived(ChannelHandlerContext context, TxPackage message) {
    Attribute<Sync<TxPackageNotifyTask>> attribute = context.channel().attr(NOTIFY_TASK);
    Sync<TxPackageNotifyTask> sync = attribute.get();
    if (sync == null) {
      sync = new Sync<>();
      attribute.set(sync);
    }
    TxPackageNotifyTask newTask = null;
    synchronized (sync) {
      TxPackageNotifyTask task = sync.get();
      if (task == null || !task.add(message)) {
        newTask = new TxPackageNotifyTask(new NettyPeerAddress(context.channel()), message);
        sync.set(newTask);
      }
    }

    if (newTask != null) {
      executor.submit(Priorities.TX_VERIFY, newTask);
    }
  }


  @Override
  public List<Object> getActiveConnectionSnapshot() {
    final List<Object> r = new ArrayList<>();

    activeConnectionSet.forEach(ctx -> {
      String origin = "NO-ORIGIN";
      if (ctx.channel().hasAttr(ORIGIN_KEY)) {
        origin = ctx.channel().attr(ORIGIN_KEY).get();
      }

      r.add(new Object[]{origin, ctx.channel().localAddress(), ctx.channel().remoteAddress()});

    });

    return r;
  }


  private void handleConnection(String hostName, int port, ChannelFuture future, Map<String, Object> properties) {
    GenericFutureListener<? extends Future<? super Void>> futureListener = listener -> {
      if (!listener.isSuccess()) {
        if (listener.isCancelled()) {
          logger.error(MARKER_CONNECT, "Connection attempt to {}:{} was cancelled", hostName, port);
        } else {
          logger.error(MARKER_CONNECT, "Could not connect to {}:{}", hostName, port, future.cause());
        }
      } else {
        SocketAddress remote = future.channel().remoteAddress();
        logger.debug(MARKER_CONNECT, "Connected to {}   sending handshake", remote);
        Channel channel = future.channel();
        for (Entry<String, Object> property : properties.entrySet()) {
          AttributeKey<Object> attribute = AttributeKey.valueOf(property.getKey());
          if (!channel.hasAttr(attribute)) {
            channel.attr(attribute).set(property.getValue());
          }
        }

        connectionHandler.onConnect(channel);
      }
    };
    future.addListener(futureListener);

  }


  @Override
  public void init(List<Integer> chainsId, String uniqueNodeIdentifier, boolean broadcastTransactions) {
    this.chainsId = chainsId;
    Collections.sort(this.chainsId);
    this.uniqueNodeIdentifier = uniqueNodeIdentifier;
    this.broadcastTransactions = broadcastTransactions;
    p2p = new P2P();

    meshNet = new MeshNet(localaddresses, listenPort);
    if (this.chainsId == null) {
      logger.error("Chains not defined");
      throw new RuntimeException("Chains not defined");//TODO change exception name
    }

    if (peerListProp.length != 0 && (!remoteHost.isEmpty() || remotePort != 0)) {
      throw new IllegalArgumentException("p2p.peer.list cannot be used in conjunction with p2p.peer.host or p2p.peer.port");
    }

    if (peerListProp.length != 0) {
      for (String peer : peerListProp) {
        peerList.add(new Pair<>(peer, Collections.emptyMap()));
      }
    } else {
      logger.warn(MARKER_CONNECT, "DEPRECATED - Usage of p2p.peer.host and p2p.peer.port is obsolete. Please use p2p.peer.list instead.");
      peerList.add(new Pair<>(remoteHost + ":" + remotePort, Collections.emptyMap()));
    }

  }


  private void listen(int port, EventLoopGroup workerGroup, EventLoopGroup bossGroup)
      throws InterruptedException {
    // Configure the server.
    ServerBootstrap serverBS = new ServerBootstrap();
    serverBS.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 100).handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(channelInitialiser);

    // Start the server.
    serverBS.bind(port).sync();
  }


  @Override
  public void onConnect(Channel channel) {
    for (int chainId : this.chainsId) {
      writeAndFlush(channel, msgFactory.checkOrigin(chainId, uniqueNodeIdentifier, nodeType));
      writeAndFlush(channel, msgFactory.peerRequest(chainId));
      writeAndFlush(channel, msgFactory.listenPort(chainId, listenPort, P2P.NODE_NAME));

      // TODO:Maybe stateRequest should be instigated from the chain?
      writeAndFlush(channel, msgFactory.stateRequest(chainId));
    }
  }


  private void registerAndConnectRemotePeer(String host, int port, Map<String, Object> properties) {
    if (port != 0) {
      meshNet.registerListenAddress(host, port, properties);
      logger.debug(MARKER_CONNECT, "Start future connection to: {}:{}", host, port);
      final ChannelFuture future = connect(host, port);

      handleConnection(host, port, future, properties);
    }
  }


  @Scheduled(fixedDelay = 1000)
  private void scheduled() {
    if (isRunning) {
      LoggedThread.logged(this::scheduledImpl).run();
    }
  }


  private void scheduledImpl() {
    //Get latest peers
    for (int chainId : chainsId) {
      broadcast(new PeerRequest(chainId));
    }

    logger.trace(MARKER_CONNECT, "Active connection set: {}", activeConnectionSet);

    List<InetSocketAddress> l = new ArrayList<>();
    for (ChannelHandlerContext ctx : activeConnectionSet) {
      l.add((InetSocketAddress) ctx.channel().remoteAddress());
    }

    List<RemoteAddresses> newConns = meshNet.getRequiredConnections(uniqueNodeIdentifier, l);

    for (RemoteAddresses a : newConns) {
      logger.info(MARKER_CONNECT, "Connecting to new address: {}", a);
      ChannelFuture future = clientBS.connect(a.getSocketAddress());

      handleConnection(a.getHostName(), a.getPort(), future, a.getProperties());
    }
  }


  @Override
  public boolean send(PeerAddress addr, Object[] msg) {
    return send(addr, MsgPackUtil.pack(msg));
  }


  @Override
  public boolean send(PeerAddress addr, byte[] msg) {
    Channel channel = ((NettyPeerAddress) addr).getCtx();

    if (channel.isActive() && channel.isOpen() && channel.hasAttr(READY) && channel.attr(READY).get().equals(Boolean.TRUE)) {
      channel.writeAndFlush(Unpooled.wrappedBuffer(msg)).addListener(channelErrorListener);
      return true;
    } else {
      logger.warn(MARKER_CONNECT, "Unable to send message to [{}] as connection is not ready yet", channel.remoteAddress());
      handleUnreadyChannel(channel);
      return false;
    }
  }


  @Override
  public void setListenPort(int port) {
    if (isRunning) {
      throw new IllegalStateException("Cannot change listen port after peer manager has started");
    }
    listenPort = port;
  }


  @Override
  public void start() {
    if (chainsId == null) {
      logger.error("Chains not defined");
      throw new RuntimeException("Chains not defined");//TODO change exception name
    }
    try {
      if (listenPort != 0) {
        listen(listenPort, workerGroup, bossGroup);
      } else {
        logger.info(MARKER_CONNECT, "No listening port");
      }

      for (Pair<String, Map<String, Object>> peer : peerList) {
        String[] hostAndPort = peer.left().split(":");
        registerAndConnectRemotePeer(hostAndPort[0], Integer.parseInt(hostAndPort[1]), peer.right());
      }

    } catch (InterruptedException e) {
      //Restore interrupted state...
      logger.error(MARKER_CONNECT, "Interrupted");
      Thread.currentThread().interrupt();
    }

    // If we are not using spring, we have to schedule updates ourselves
    if (isNotUsingSpring) {
      timer = new Timer();
      TimerTask task = new TimerTask() {
        public void run() {
          NettyPeerManager.this.scheduled();
        }
      };
      timer.scheduleAtFixedRate(task, 1000, 1000);
    }
    isRunning = true;
  }


  @Override
  public void stop() {
    isRunning = false;
    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
    if (timer != null) {
      timer.cancel();
    }
  }


  private boolean flowPaused = false;


  @Override
  public void pause() {
    for (ChannelHandlerContext chc : activeConnectionSet) {
      Channel channel = chc.channel();
      InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
      NodeType nodeType = channel.attr(NODE_TYPE).get();

      if (nodeType == NodeType.TxLoader && !flowPaused) {
        if (logger.isWarnEnabled(MARKER_PERFORMANCE)) {
          logger.warn(MARKER_PERFORMANCE, "Pausing flow of txs from {}{}", nodeType.name(), address);
        }

        channel.config().setAutoRead(false);
        flowPaused = true;
      }
    }
  }


  @Override
  public void resume() {
    for (ChannelHandlerContext chc : activeConnectionSet) {
      Channel channel = chc.channel();
      InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
      NodeType nodeType = channel.attr(NODE_TYPE).get();

      if (nodeType == NodeType.TxLoader && flowPaused) {
        if (logger.isWarnEnabled(MARKER_PERFORMANCE)) {
          logger.warn(MARKER_PERFORMANCE, "Resuming flow of txs from {}{}", nodeType.name(), address);
        }

        channel.config().setAutoRead(true);
        channel.read();
        flowPaused = false;
      }
    }
  }

}
