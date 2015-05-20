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

import io.netty.channel.Channel;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.comm.App.Request;
import poke.comm.Image.*;
import poke.core.Mgmt.Management;
import poke.server.queue.PerChannelQueue;

/**
 * the connection map for server-to-server communication.
 * 
 * Note the connections/channels are initialized through the heartbeat manager
 * as it starts (and maintains) the connections through monitoring of processes.
 * 
 * 
 * TODO refactor to make this the consistent form of communication for the rest
 * of the code
 * 
 * @author gash
 * 
 */
public class ConnectionManager {
	protected static Logger logger = LoggerFactory.getLogger("management");

	/** node ID to channel */
	private static HashMap<Integer, Channel> connections = new HashMap<Integer, Channel>();
	private static HashMap<Integer, Channel> mgmtConnections = new HashMap<Integer, Channel>();
	private static HashMap<Integer, Channel> leaderConnections = new HashMap<Integer, Channel>();


	public static void addConnection(Integer nodeId, Channel channel, boolean isMgmt) {
		logger.info("ConnectionManager adding connection to " + nodeId);

		if (isMgmt){
			mgmtConnections.put(nodeId, channel);
		}
		else
			connections.put(nodeId, channel);
	}
	

	public static Channel getConnection(Integer nodeId, boolean isMgmt) {

		if (isMgmt)
			return mgmtConnections.get(nodeId);
		else
			return connections.get(nodeId);
	}

	public synchronized static void removeConnection(Integer nodeId, boolean isMgmt) {
		if (isMgmt)
			mgmtConnections.remove(nodeId);
		else
			connections.remove(nodeId);
	}

	public synchronized static void removeConnection(Channel channel, boolean isMgmt) {

		if (isMgmt) {
			if (!mgmtConnections.containsValue(channel)) {
				return;
			}

			for (Integer nid : mgmtConnections.keySet()) {
				if (channel == mgmtConnections.get(nid)) {
					mgmtConnections.remove(nid);
					break;
				}
			}
		} else {
			if (!connections.containsValue(channel)) {
				return;
			}

			for (Integer nid : connections.keySet()) {
				if (channel == connections.get(nid)) {
					connections.remove(nid);
					break;
				}
			}
		}
	}
	
	/**Dhanu:************************/
	public static void addLeaderConnection(Integer nodeId, Channel channel) {
		
		logger.info("ConnectionManager adding connection to leader " + nodeId);

			leaderConnections.put(nodeId, channel);
		
	}
	

	public static Channel getLeaderConnection(Integer nodeId) 
	{
			return leaderConnections.get(nodeId);
		
	}

	public synchronized static void removeLeaderConnection(Integer nodeId) {
		
			leaderConnections.remove(nodeId);
	
	}

	public synchronized static void removeLeaderConnection(Channel channel) {

		
	
			if (!leaderConnections.containsValue(channel)) {
				return;
			}

			for (Integer nid : leaderConnections.keySet()) {
				if (channel == leaderConnections.get(nid)) {
					leaderConnections.remove(nid);
					break;
				}
			}
		
	}
	/****************************/

	public synchronized static void broadcast(Request req) {
		if (req == null)
			return;

		for (Channel ch : connections.values())
		{ 
			//Still figuring how to use the per channel queue already created when the message is received
			PerChannelQueue p = new PerChannelQueue(ch);
			p.enqueueResponse(req, ch);
			
		}
		
	}

	public synchronized static void broadcast(Management mgmt) {
		if (mgmt == null)
			return;

		for (Channel ch : mgmtConnections.values())
				ch.writeAndFlush(mgmt);
	}

	

	public static int getNumMgmtConnections() {
		return mgmtConnections.size();
	}
}
