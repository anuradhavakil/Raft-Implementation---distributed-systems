/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.lang.*;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.core.Mgmt;
import poke.core.Mgmt.LeaderElection;
import poke.core.Mgmt.Management;
import poke.core.Mgmt.MgmtHeader;
import poke.core.Mgmt.RaftVote;
import poke.core.Mgmt.RaftVoteResponse;
import poke.core.Mgmt.RaftVoteResponse.Builder;
import poke.core.Mgmt.VectorClock;
import poke.server.conf.JsonUtil;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.election.RaftState;
import poke.server.management.ManagementInitializer;
import poke.server.management.ManagementQueue;
import poke.server.managers.ConnectionManager;
import poke.server.managers.ElectionManager;
import poke.server.managers.ExternalServersManager;
import poke.server.managers.HeartbeatData;
import poke.server.managers.HeartbeatManager;
import poke.server.managers.HeartbeatPusher;
import poke.server.managers.JobManager;
import poke.server.managers.NetworkManager;
import poke.server.monitor.HeartMonitor;
import poke.server.resources.ResourceFactory;

import com.google.protobuf.ByteString;

import poke.server.managers.ReplicationManager;

/**
 * Note high surges of messages can close down the channel if the handler cannot
 * process the messages fast enough. This design supports message surges that
 * exceed the processing capacity of the server through a second thread pool
 * (per connection or per server) that performs the work. Netty's boss and
 * worker threads only processes new connections and forwarding requests.
 * <p>
 * Reference Proactor pattern for additional information.
 * 
 * @author gash
 * 
 */
public class Server {
	protected static Logger logger = LoggerFactory.getLogger("server");

	protected static ChannelGroup allChannels;
	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();
	protected ServerConf conf;
	public String serverState;
	public int term;
	public int voteCounter;
	

	protected JobManager jobMgr;
	protected NetworkManager networkMgr;
	protected HeartbeatManager heartbeatMgr;
	protected ElectionManager electionMgr;
	
	protected RaftNodeTimer serverTimer;
	
	private int termVotedFor = -1;
	private ArrayList<Integer> serversVotedFor;
	
	protected ReplicationManager replicationManager;
	
	protected ServerCommConnection comm;
	
	

	/**
	 * static because we need to get a handle to the factory from the shutdown
	 * resource
	 */
	public static void shutdown() {
		try {
			if (allChannels != null) {
				ChannelGroupFuture grp = allChannels.close();
				grp.awaitUninterruptibly(5, TimeUnit.SECONDS);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		logger.info("Server shutdown");
		System.exit(0);
	}

	/**
	 * initialize the server with a configuration of it's resources
	 * 
	 * @param cfg
	 */
	public Server(File cfg) {
		init(cfg);
	}

	private void init(File cfg) {
		
		if (!cfg.exists())
			throw new RuntimeException(cfg.getAbsolutePath() + " not found");
		// resource initialization - how message are processed
		BufferedInputStream br = null;
		try {
			serverState = RaftState.FOLLOWER.toString();
			term = 0;
			voteCounter = 0;
			logger.info("Started server in FOLLOWER state");
			byte[] raw = new byte[(int) cfg.length()];
			br = new BufferedInputStream(new FileInputStream(cfg));
			br.read(raw);
			conf = JsonUtil.decode(new String(raw), ServerConf.class);
			if (!verifyConf(conf))
				throw new RuntimeException("verification of configuration failed");
			ResourceFactory.initialize(conf);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private boolean verifyConf(ServerConf conf) {
		boolean rtn = true;
		if (conf == null) {
			logger.error("Null configuration");
			return false;
		} else if (conf.getNodeId() < 0) {
			logger.error("Bad node ID, negative values not allowed.");
			rtn = false;
		} else if (conf.getPort() < 1024 || conf.getMgmtPort() < 1024) {
			logger.error("Invalid port number");
			rtn = false;
		}

		return rtn;
	}

	public void release() {
		if (HeartbeatManager.getInstance() != null)
			HeartbeatManager.getInstance().release();
	}

	/**
	 * initialize the outward facing (public) interface
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartCommunication implements Runnable {
		ServerConf conf;

		public StartCommunication(ServerConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(conf.getPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new ServerInitializer(compressComm));

				// Start the server.
				logger.info("Starting server " + conf.getNodeId() + ", listening on port = " + conf.getPort());
				ChannelFuture f = b.bind(conf.getPort()).syncUninterruptibly();

				// should use a future channel listener to do this step
				// allChannels.add(f.channel());

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup public handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}

			// We can also accept connections from a other ports (e.g., isolate
			// read
			// and writes)
		}
	}

	/**
	 * initialize the private network/interface
	 * 
	 * @param port
	 *            The port to listen to
	 */
	private static class StartManagement implements Runnable {
		private ServerConf conf;

		public StartManagement(ServerConf conf) {
			this.conf = conf;
		}

		public void run() {
			// construct boss and worker threads (num threads = number of cores)

			// UDP: not a good option as the message will be dropped

			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(conf.getMgmtPort(), b);

				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				// b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR);

				boolean compressComm = false;
				b.childHandler(new ManagementInitializer(compressComm));

				// Start the server.

				logger.info("Starting mgmt " + conf.getNodeId() + ", listening on port = " + conf.getMgmtPort());
				ChannelFuture f = b.bind(conf.getMgmtPort()).syncUninterruptibly();

				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {
				// on bind().sync()
				logger.error("Failed to setup public handler.", ex);
			} finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();
			}
		}
	}

	/**
	 * this initializes the managers that support the internal communication
	 * network.
	 * 
	 * TODO this should be refactored to use the conf file
	 */
	private void startManagers() {
		if (conf == null)
			return;

		// start the inbound and outbound manager worker threads
		ManagementQueue.startup();
		
		// create manager for network changes
		networkMgr = NetworkManager.initManager(conf);

		// create manager for leader election. The number of votes (default 1)
		// is used to break ties where there are an even number of nodes.
		electionMgr = ElectionManager.initManager(conf,this);

		
		
		// create manager for accepting jobs
		jobMgr = JobManager.initManager(conf);
		
		/**Dhanu******************/
		//prepare replication manager to replicate the message
		 ReplicationManager.initManager(conf);
		 /**************************/
		//ExternalServersManager.initManager(conf);
		
		//int clusterSize = conf.getAdjacent().getAdjacentNodes().size();
		System.out.println("---> Server.startManagers() expecting " + conf.getAdjacent().getAdjacentNodes().size()
				+ " connections");
		//System.out.println("clusterSize: " +conf.getAdjacent().getAdjacentNodes().size());
		
		
		
		// establish nearest nodes and start sending heartbeats
		heartbeatMgr = HeartbeatManager.initManager(conf,this);
		for (NodeDesc nn : conf.getAdjacent().getAdjacentNodes().values()) {
			HeartbeatData node = new HeartbeatData(nn.getNodeId(), nn.getHost(), nn.getPort(), nn.getMgmtPort());

			// fn(from, to)
			HeartbeatPusher.getInstance().connectToThisNode(conf.getNodeId(), node);
			
				
		}
		heartbeatMgr.start();
		
		
		
		// manage heartbeatMgr connections
		HeartbeatPusher conn = HeartbeatPusher.getInstance();
		conn.start();
		
		/**anu's change
		 * start TimeoutHandler
		*/	
		
		serverTimer = RaftNodeTimer.create("Node Timer for " +conf.getNodeId(),getRaftTimerRandomElectionTimeout(), new TimeoutHandler(conf, this, null));
	//	logger.info("AFTER RaftNodeTimer called");
		
		logger.info("Server " + conf.getNodeId() + ", managers initialized");
		
	}
	
	public static int getRaftTimerRandomElectionTimeout(){
		return RandomUtils.nextInt(2000) + 5000 + RandomUtils.nextInt(1000);		//max time in milli seconds
			 
	}
	
	/**anu's change
	 * resets server's time
	*/
	public void resetServerTimer(){
		serverTimer.reset();
	}
	
	/**anu's change
	 * stops the server
	*/
	public void stopServertimer() {
		serverTimer.stop();
	}
	
	
	/* * Start the communication for both external (public) and internal
	 * (management)
	 */
	public void sendPing(String host, int port) {
		// data to send
	 comm=new ServerCommConnection(host,port); 
		poke.comm.Image.Ping.Builder ping = poke.comm.Image.Ping.newBuilder();
		ping.setIsPing(true);
		
		// payload containing data
		poke.comm.Image.Request.Builder r = poke.comm.Image.Request.newBuilder();
		poke.comm.Image.PayLoad.Builder p = poke.comm.Image.PayLoad.newBuilder();
		p.setData(null);
		r.setPayload(p.build());
		r.setPing(ping);
		
		

		// header with routing info
		poke.comm.Image.Header.Builder h = poke.comm.Image.Header.newBuilder();
		h.setCaption("ping");
		h.setIsClient(false);
		h.setClientId(0);
		h.setClusterId(6);
		r.setHeader(h.build());
		poke.comm.Image.Request req = r.build();

		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}
	
	
	
	
	/**
	 * Start the communication for both external (public) and internal
	 * (management)
	 */
	public void run() {
		
		
		if (conf == null) {
			logger.error("Missing configuration file");
			return;
		}

		logger.info("Initializing server " + conf.getNodeId());

		// storage initialization
		// TODO storage setup (e.g., connection to a database)

		startManagers();
		
		if(ReplicationManager.conf.getNodeId()==1) //If its a leader node
		{
			//loop through
			/*String host;
			int port;
			sendPing(host, port);
			*/
			//sendPing("10.0.27.1", 5570);
			//sendPing(host, port);
			//sendPing(host, port);
			//sendPing(host, port);
			
		}

		StartManagement mgt = new StartManagement(conf);
		Thread mthread = new Thread(mgt);
		mthread.start();

		StartCommunication comm = new StartCommunication(conf);
		logger.info("Server " + conf.getNodeId() + " ready");

		Thread cthread = new Thread(comm);
		cthread.start();
		
	
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: java " + Server.class.getClass().getName() + " conf-file");
			System.exit(1);
		}

		File cfg = new File(args[0]);
		if (!cfg.exists()) {
			Server.logger.error("configuration file does not exist: " + cfg);
			System.exit(2);
		}
		
		
		
		Server svr = new Server(cfg);
		
		svr.run();
		
		
	}
	
		
}
