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
package poke.server.election;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.core.Mgmt.LeaderElection;
import poke.core.Mgmt.LeaderElection.ElectAction;
import poke.core.Mgmt.Management;
import poke.core.Mgmt.MgmtHeader;
import poke.core.Mgmt.RaftVote;
import poke.core.Mgmt.RaftVoteResponse;
import poke.core.Mgmt.VectorClock;
import poke.server.TimeoutHandler;
import poke.server.managers.ConnectionManager;
import poke.server.*;


//RAFT ELECTION IN PLACE OF FLOODMAX

public class RaftElection implements Election {
	
	protected static Logger logger = LoggerFactory.getLogger("raft");
	
	private Integer nodeId;
	private ElectionState current;
	private ElectionListener listener;

	private int termVotedFor = -1;
	
	public Server server;
	
//	HashMap<Integer, Integer> serverVoteStatus;

	@Override
	public void setListener(ElectionListener listener) {
		this.listener = listener;
		
	}

	@Override
	public synchronized void clear() {
		current = null ;
		
	}

	//??
	@Override
	public boolean isElectionInprogress() {
		return current != null;
	}

	

	@Override
	public Management process(Management req) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNodeId(int nodeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Integer getElectionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer createElectionID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Server getWinner() {
	
		return server;
		
	}
	
	public void setLeader(Server server){
		this.server = server;
	}
	
	/**
	 * anu's change
	 * RPC sent to followers for requesting vote by candidate
	 * @param id
	 * @param term
	 */

	public static void sendVoteRequest(int id, int term){

		int originatorNodeId = id;
		
		RaftVote.Builder rvb = RaftVote.newBuilder();
		rvb.setTerm(term);
		rvb.setOriginatorNodeId(originatorNodeId);
		
		MgmtHeader.Builder mhb = MgmtHeader.newBuilder();
		mhb.setOriginator(id);
		mhb.setTime(System.currentTimeMillis());
		mhb.setSecurityCode(-999); // TODO add security

		VectorClock.Builder rpb = VectorClock.newBuilder();
		rpb.setNodeId(id);
		rpb.setTime(mhb.getTime());
		rpb.setVersion(-1);
		mhb.addPath(rpb.build());
		
		Management.Builder mb = Management.newBuilder();
		mb.setHeader(mhb.build());
		mb.setRaftVote(rvb.build());
		
		
	//	logger.info("sendRequestVote from node: "+rvb.getOriginatorNodeId());

		ConnectionManager.broadcast(mb.build());
	}
	
	/**
	 * anu's change
	 * RPC sent by followers in response to cadidate's vote request
	 * if response is 1- FOLLOWER gave YES vote to  candidate
	 * if response is 0- FOLLOWER gave NO vote to candidate
	 * @param rnId
	 * @return
	 */
	
	
	public int sendVoteResponse(int voteSenderId, int rnId,int requestTerm){
		
		
		
		logger.info("from  sendVoteResponse");
		int flag = 0;
		

		
		if(requestTerm > termVotedFor ){
			termVotedFor = requestTerm;
			flag = 1;
		}
	//	logger.info("term "+term +"^^^^^^^^^^^^^term voted for "+termVotedFor);
		
	//	serverVoteStatus = new HashMap<Integer, Integer>();
	//	serverVoteStatus.put(term, responseNodeId);
		
		
		
		RaftVoteResponse.Builder rvr = RaftVoteResponse.newBuilder();
		rvr.setResponse(flag);
		rvr.setResponseNodeId(rnId);
		
		
			
		MgmtHeader.Builder mhb = MgmtHeader.newBuilder();
		mhb.setOriginator(voteSenderId);
		mhb.setTime(System.currentTimeMillis());
		mhb.setSecurityCode(-999); // TODO add security

		VectorClock.Builder rpb = VectorClock.newBuilder();
		rpb.setNodeId(voteSenderId);
		rpb.setTime(mhb.getTime());
		rpb.setVersion(-1);
		mhb.addPath(rpb.build());
		
		Management.Builder mb = Management.newBuilder();
		mb.setHeader(mhb.build());
		mb.setRaftVoteResponse(rvr.build());
		
		
		ConnectionManager.broadcast(mb.build());
		
		
	//	logger.info("sendVoteResponse from node: "+responseNodeId);
		
		return flag;
			
	}




}
