package poke.server;


import io.netty.util.Timer;

import org.h2.bnf.Sentence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.core.Mgmt;
import poke.server.conf.*;
//import poke.server.election.FloodMaxElection;
import poke.server.election.RaftElection;
import poke.server.election.RaftState;
import poke.server.managers.ElectionManager;
import poke.server.managers.HeartbeatData;
import poke.server.managers.HeartbeatManager;
import poke.server.managers.HeartbeatPusher;
import poke.server.managers.NetworkManager;
import poke.server.*;
/** Anuradha's Changes
 * 
 * Responsible for all the events, to be executed after timeout
*/

public class TimeoutHandler implements Runnable {
	
	protected ServerConf conf;
	protected Server server;
	protected HeartbeatManager heartbeatMgr;
	
	
	
	public ServerConf getConf() {
		return conf;
	}

	public void setConf(ServerConf conf) {
		this.conf = conf;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public HeartbeatManager getHeartbeatMgr() {
		return heartbeatMgr;
	}

	public void setHeartbeatMgr(HeartbeatManager heartbeatMgr) {
		this.heartbeatMgr = heartbeatMgr;
	}
	
	protected static Logger logger = LoggerFactory.getLogger("TimeoutHandler");

	public void timeout(ServerConf conf, Server server, HeartbeatManager hbMgr){
		Thread t = new Thread(new TimeoutHandler(conf, server, hbMgr));
		t.setName(conf.getNodeId() + "TimeoutHandler");
		t.setDaemon(true);
		t.start();
		
	}
	
	public TimeoutHandler(){
		
	}
	
	public TimeoutHandler(ServerConf conf, Server server, HeartbeatManager hbMgr) {
		this.conf = conf;
		this.server = server;
		this.heartbeatMgr = hbMgr;
		setHeartbeatMgr(hbMgr);
	}
	@Override
	public void run(){ 
	//	logger.info(conf.getNodeId() + " state:" + server.serverState  + " timeout!!");
		
		//when state changed to candidate
		if(server.serverState == RaftState.FOLLOWER.toString()) {
			
			server.serverState = RaftState.CANDIDATE.toString();
			ElectionManager.startElection();
			logger.info("Server state: "+server.serverState);
			
		} else if(server.serverState == RaftState.CANDIDATE.toString()){//when candidate timeouts
			
			ElectionManager.startElection();
			server.resetServerTimer();
			logger.info("Server state: "+server.serverState);
			
		} else if (server.serverState == RaftState.LEADER.toString()){//when leader timeouts
			logger.info("Server state: "+server.serverState);
			//start sending out heartbeat
			heartbeatMgr = HeartbeatManager.initManager(conf,server);
			for (NodeDesc nn : conf.getAdjacent().getAdjacentNodes().values()) {
				HeartbeatData node = new HeartbeatData(nn.getNodeId(), nn.getHost(), nn.getPort(), nn.getMgmtPort());
				// fn(from, to)
				HeartbeatPusher.getInstance().connectToThisNode(conf.getNodeId(), node);
				}
			
			server.resetServerTimer();
			
		}else 
			server.serverState = RaftState.FOLLOWER.toString();
		
	}

}
