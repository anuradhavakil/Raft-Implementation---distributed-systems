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
package poke.server.managers;

import java.beans.Beans;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.core.Mgmt.LeaderElection;
import poke.core.Mgmt.LeaderElection.ElectAction;
import poke.core.Mgmt.Management;
import poke.core.Mgmt.MgmtHeader;
import poke.core.Mgmt.RaftVote;
import poke.core.Mgmt.RaftVoteResponse;
import poke.core.Mgmt.VectorClock;
import poke.server.Server;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.election.Election;
import poke.server.election.ElectionListener;
//import poke.server.election.FloodMaxElection;
import poke.server.election.RaftElection;
import poke.server.election.RaftState;
import poke.server.management.InboundMgmtWorker;

/**
 * The election manager is used to determine leadership within the network. The
 * leader is not always a central point in which decisions are passed. For
 * instance, a leader can be used to break ties or act as a scheduling dispatch.
 * However, the dependency on a leader in a decentralized design (or any design,
 * matter of fact) can lead to bottlenecks during heavy, peak loads.
 * 
 * TODO An election is a special case of voting. We should refactor to use only
 * voting.
 * 
 * QUESTIONS:
 * 
 * Can we look to the PAXOS alg. (see PAXOS Made Simple, Lamport, 2001) to model
 * our behavior where we have no single point of failure; each node within the
 * network can act as a consensus requester and coordinator for localized
 * decisions? One can envision this as all nodes accept jobs, and all nodes
 * request from the network whether to accept or reject the request/job.
 * 
 * Does a 'random walk' approach to consistent data replication work?
 * 
 * What use cases do we would want a central coordinator vs. a consensus
 * building model? How does this affect liveliness?
 * 
 * Notes:
 * <ul>
 * <li>Communication: the communication (channel) established by the heartbeat
 * manager is used by the election manager to communicate elections. This
 * creates a constraint that in order for internal (mgmt) tasks to be sent to
 * other nodes, the heartbeat must have already created the channel.
 * </ul>
 * 
 * @author gash
 * 
 */
public class ElectionManager implements ElectionListener {
	protected static Logger logger = LoggerFactory.getLogger("election");
	protected static AtomicReference<ElectionManager> instance = new AtomicReference<ElectionManager>();

	private static ServerConf conf;
	private static Server server;

	// number of times we try to get the leader when a node starts up
	
	/** The election that is in progress - only ONE! */
	private Election election;
	private int electionCycle = -1;
	
	/** The leader */
	Integer leaderNode;
	
	public int voteCounter;
	
	private int leader;
	
	

	public static ElectionManager initManager(ServerConf conf, Server serverNode) {
		logger.info("Initializing Election Manager....");
		ElectionManager.conf = conf;
		server = serverNode;
		instance.compareAndSet(null, new ElectionManager());
		return instance.get();
	}
	
	//create instance of RaftElection
	RaftElection raftElection = new RaftElection();
	
	/**
	 * Access a consistent instance for the life of the process.
	 * 
	 * TODO do we need to have a singleton for this? What happens when a process
	 * acts on behalf of separate interests?
	 * 
	 * @return
	 */
	public static ElectionManager getInstance() {
		// TODO throw exception if not initialized!
		return instance.get();
	}

	/**
	 * returns the leader of the network
	 * 
	 * @return
	 */
	public Integer whoIsTheLeader() {
		return this.leaderNode;
	}

	public Integer whoIsTheLeader2() {
		return this.leader;
	}
	/**
	 * 
	 */
	
	//start election called by CANDIDATE in TimeoutHandler.java when timeout for FOLLOWER takes place.
	public static void startElection(){
		server.term ++ ;
		server.voteCounter++;		
		RaftElection.sendVoteRequest(conf.getNodeId(), server.term);
		
				
	}

	/**
	 * @param args
	 */
	public void processRequest(Management mgmt) {
	
		
		//All the servers who receive vote request from CANDIDATE/s come here	
		if(mgmt.hasRaftVote()){
		logger.info("GOT RAFT REQUEST from: "+mgmt.getHeader().getOriginator()+"Requester Term:"+mgmt.getRaftVote().getTerm());
		logger.info("Server(my) term: "+server.term);
			
		//Send response for the vote only if I am a FOLLOWER and if my term is less then requester's term
			if(server.serverState.equals(RaftState.FOLLOWER.toString()) && mgmt.getRaftVote().getTerm() > server.term){
				server.resetServerTimer();
				raftElection.sendVoteResponse(conf.getNodeId(),mgmt.getHeader().getOriginator(), mgmt.getRaftVote().getTerm());
				
			}
			
			// check if for vote request with higher term .. go back to follower for voting
			if((server.serverState.equals(RaftState.CANDIDATE.toString()) || server.serverState.equals(RaftState.LEADER.toString()))
					&& mgmt.getRaftVote().getTerm() > server.term) {
				server.serverState = RaftState.FOLLOWER.toString();
				server.resetServerTimer();
				raftElection.sendVoteResponse(conf.getNodeId(),mgmt.getHeader().getOriginator(), mgmt.getRaftVote().getTerm());
				
			}
			}else if(mgmt.hasRaftVoteResponse()){ //All the CANDIDATES who receive the response come here.
		
			//Count the number of votes received.	
			int count = server.voteCounter + mgmt.getRaftVoteResponse().getResponse() ;
			
			// Count the total number of votes that would make the CANDIDATE have majority
			int totalCount = (int)(conf.getAdjacent().getAdjacentNodes().values().size()/2) + 1;
			
			//If majority earned by CANDIDATE
			if (count >= totalCount){
				server.serverState = RaftState.LEADER.toString();
				System.out.println("Leader declared :)))))  nodeid: "+conf.getNodeId());
				raftElection.setLeader(server);
				
				MgmtHeader.Builder mhb = MgmtHeader.newBuilder();
				mhb.setOriginator(conf.getNodeId());
				mhb.setTime(System.currentTimeMillis());
				mhb.setSecurityCode(-999); // TODO add security
				
				VectorClock.Builder rpb = VectorClock.newBuilder();
				rpb.setNodeId(conf.getNodeId());
				rpb.setTime(mhb.getTime());
				rpb.setVersion(electionCycle);
				mhb.addPath(rpb);
				
				LeaderElection.Builder elb = LeaderElection.newBuilder();
				elb.setElectId(electionCycle);
				elb.setAction(ElectAction.DECLAREWINNER);
				elb.setLeaderTerm(server.term);
				elb.setDesc("Node " + this.leaderNode + " is the leader");
				elb.setCandidateId(conf.getNodeId());
				elb.setExpires(-1);

				Management.Builder mb = Management.newBuilder();
				mb.setHeader(mhb.build());
				mb.setElection(elb.build());

				// now send it out to all my edges
				logger.info("Leader established" + conf.getNodeId());
				ConnectionManager.broadcast(mb.build());
			}
		} else if (mgmt.hasElection()) {									//already ongoing election
			LeaderElection req = mgmt.getElection();						  
			if(req.getAction().getNumber() == LeaderElection.ElectAction.DECLAREWINNER_VALUE) {
				
				if(server.serverState == RaftState.LEADER.toString()){		//if the current server is leader only enter the block
					
				logger.info("server.term----  "+server.term);				//current server term
				logger.info("req.getLeaderTerm()----------"+ req.getLeaderTerm());	//server term of already existing leader
				
				if(server.term > req.getLeaderTerm()){		
						logger.info(server.toString() +"needs to be leader"); //current server needs to be the leader
					}else{
						logger.info("Sorry I need to be a FOLLOWER");			
						server.serverState = RaftState.FOLLOWER.toString();   // current server who is also the leader has discovered another leader with higher term
					}														  // so convert to follower state	
						
				} else {
					server.serverState = RaftState.FOLLOWER.toString();
					logger.info("I got a message that there is Leader Now.... Congrats!!!");
				}
				server.resetServerTimer();	
				
			}
		}
		
		if (!mgmt.hasElection())
			return;
}

	/**
	 * check the health of the leader (usually called after a HB update)
	 * 
	 * @param mgmt
	 */


	/** election listener implementation */
	@Override
	
	public void concludeWith(boolean success, Integer leaderID) {
		if (success) {
			logger.info("----> the leader is " + leaderID);
			this.leaderNode = leaderID;
		}

		election.clear();
	}

	/*private Election electionInstance() {
		if (election == null) {
			synchronized (syncPt) {
				if (election !=null)
					return election;
				
				// new election
				String clazz = ElectionManager.conf.getElectionImplementation();

				// if an election instance already existed, this would
				// override the current election
				try {
					election = (Election) Beans.instantiate(this.getClass().getClassLoader(), clazz);
					election.setNodeId(conf.getNodeId());
					election.setListener(this);

					// this sucks - bad coding here! should use configuration
					/* properties
					if (election instanceof FloodMaxElection) {
						logger.warn("Node " + conf.getNodeId() + " setting max hops to arbitrary value (4)");
						((FloodMaxElection) election).setMaxHops(4);
					}*/

		/*		} catch (Exception e) {
					logger.error("Failed to create " + clazz, e);
				}
			}
		}

		return election;

	}*/

	
	
}
