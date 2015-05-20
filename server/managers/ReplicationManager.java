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

import poke.comm.App.Header;
import poke.comm.App.PayLoad;
import poke.comm.App.Ping;
import poke.comm.App.Request;
import poke.comm.Image.*;
import poke.core.Mgmt.ChatMessage;
import poke.core.Mgmt.Heartbeat;
import poke.core.Mgmt.Log;
import poke.core.Mgmt.Management;
import poke.core.Mgmt.MgmtHeader;
import poke.server.conf.ServerConf;
import poke.server.storage.jdbc.DatabaseClass;


public class ReplicationManager {
	protected static Logger logger = LoggerFactory.getLogger("management");
 
	/*private*/ public static ServerConf conf;
	
	static HashMap <Integer,Channel> hm=new HashMap<Integer,Channel>();
	
	static String[] leaderLog=new String[11];
	private static DatabaseClass db=new DatabaseClass();
	public static void createMessage(String s)
	{
		
	}
	
	public static void setLeaderLog(int clusterId, String IP)
	{
		leaderLog[clusterId]=IP;
		implementLogReplication();
	}
	
	private static void implementLogReplication() {
		// TODO Auto-generated method stub
		Log.Builder l = Log.newBuilder();
		for(int i=0;i<leaderLog.length;i++)
		{
		l.setIP(i, leaderLog[i]);
		}
		
		MgmtHeader.Builder mhb = MgmtHeader.newBuilder();
		mhb.setOriginator(ReplicationManager.conf.getNodeId());
		mhb.setTime(System.currentTimeMillis());
		
		// TODO token must be known between nodes
		mhb.setSecurityCode(-999);

		Management.Builder b = Management.newBuilder();
		b.setHeader(mhb.build());
		b.setLogValue(l);

		ConnectionManager.broadcast(b.build());
		logger.info("Log message replicated by leader");
		
	}

	public static String getLedaer(int clusterId)
	{
		
		return leaderLog[clusterId];
	}
	
	
	public static /*ReplicationManager*/void initManager(ServerConf conf) {
		
		ReplicationManager.conf = conf;
		for(int i=1; i<=10;i++)
		{
		leaderLog[i]=null;
		}
		
	}

	
	public static Management getMgmtProtoFormat(Request request)
	{
		ChatMessage.Builder l = ChatMessage.newBuilder();
		l.setCaption(request.getHeader().getCaption());
		l.setImage(request.getPayload().getData());
		l.setClientId(request.getHeader().getClientId());
		l.setClusterId(request.getHeader().getClusterId());
	
		// Todo: get current term from and add it here
		

		MgmtHeader.Builder mhb = MgmtHeader.newBuilder();
	
		mhb.setOriginator(ReplicationManager.conf.getNodeId());

		mhb.setTime(System.currentTimeMillis());
		
		// TODO token must be known between nodes
		mhb.setSecurityCode(-999);

		Management.Builder b = Management.newBuilder();
		b.setHeader(mhb.build());
		b.setMessage(l);
		
		return b.build();
	}
	
	public static Request getRequestProtoFormat1(Management mgmt)
	{
		
		Ping.Builder ping = Ping.newBuilder();
		ping.setIsPing(false);


		// payload containing data
		Request.Builder r = Request.newBuilder();
		PayLoad.Builder p = PayLoad.newBuilder();
		p.setData(mgmt.getMessage().getImage());

		r.setPayload(p.build());

		r.setPing(ping);


		// header with routing info

		Header.Builder h = Header.newBuilder();

		h.setOriginator(1000);

		h.setTag("Any tag");

		h.setCaption(mgmt.getMessage().getCaption());

		h.setTime(System.currentTimeMillis());

		h.setIsClient(false);

		h.setClientId(mgmt.getMessage().getClientId());

		h.setClusterId(6);


		h.setRoutingId(Header.Routing.PING);

		r.setHeader(h.build());

		Request req = r.build();
		return req;
	}
	
	public static poke.comm.Image.Request getRequestProtoFormat2(Management mgmt)
	{
		
		poke.comm.Image.Ping.Builder ping = poke.comm.Image.Ping.newBuilder();
		ping.setIsPing(false);

		// payload containing data
		poke.comm.Image.Request.Builder r = poke.comm.Image.Request.newBuilder();
		poke.comm.Image.PayLoad.Builder p = poke.comm.Image.PayLoad.newBuilder();
		p.setData(mgmt.getMessage().getImage());
		r.setPayload(p.build());
		r.setPing(ping);

		// header with routing info
		poke.comm.Image.Header.Builder h = poke.comm.Image.Header.newBuilder();
		h.setCaption(mgmt.getMessage().getCaption());
		h.setIsClient(false);
		h.setClientId(mgmt.getMessage().getClientId());
		h.setClusterId(6);
		r.setHeader(h.build());
		poke.comm.Image.Request req = r.build();
		return req;
	}

	public static void processRequest(Management mgmt)

	{//This method will process the replication msg from other nodes

		if(ElectionManager.getInstance().whoIsTheLeader2()!=null&&ElectionManager.getInstance().whoIsTheLeader2()==ReplicationManager.conf.getNodeId())
		{
		
		/** Convert the message in image.proto form and broadcast it to all leaders of other clusters*/
			
		poke.comm.Image.Request req=ReplicationManager.getRequestProtoFormat2(mgmt);
		broadcastLeaders(req);

		/********************************/
		//Todo: When a new leader gets elected 
		//Create a new channel to the servers 
		
	
	}

		
	/**Covert it into App.Request format and broadcast it to all clients*/	
	
	ChatMessage l= mgmt.getMessage();

	if(l==null)
	return;

	Request req=ReplicationManager.getRequestProtoFormat1(mgmt);

	// We broadcast all the emssages to other servers

	ConnectionManager.broadcast(req);
	logger.info("Replication message received from other node: \ncaption"+l.getCaption()+" "+l.getImage());
	//replication manager adds images to db.  
	
	/**The below code is to add all the images to the database when a server receives an image. We have 
	 * commented it because it requires a local database to be installed. Uncomment for testing.  **/
	//db.insertImage(l.getImage());
	
	/***********/
	}

	public static void processLog(Management mgmt) {
		for(int i=0;i<leaderLog.length;i++)
		{
	leaderLog[i]=mgmt.getLogValue().getIP(i);
		}
		
	}

	public static void broadcastLeaders(poke.comm.Image.Request req) {
		
		for(String s: leaderLog)
		{
			if(s!=null)
				;
			//ExternalServersManager.pingClustersWithImage(req,s,conf.getNodeId());
			
		}
		
	}
	
	/*	private static Management generateAppendRPC(int term, String caption, Bytes image ) {
	Log.Builder l = Log.newBuilder();
	l.setLog1(caption);
	l.setLog2(image);

	MgmtHeader.Builder mhb = MgmtHeader.newBuilder();
	logger.info("Before getnodeid");
	mhb.setOriginator(ReplicationManager.conf.getNodeId());
	logger.info("After getnodeid");
	mhb.setTime(System.currentTimeMillis());
	
	// TODO token must be known between nodes
	mhb.setSecurityCode(-999);

	Management.Builder b = Management.newBuilder();
	b.setHeader(mhb.build());
	b.setLogValue(l);

	return b.build();
} */

/*public static void  replicate(String log)
{
	int term=0; //Dhanu: change it wen needed
	logger.info("Replicate method called");
Management mgmt=ReplicationManager.generateAppendRPC(term, log);
logger.info("Mangement message generated");
ConnectionManager.broadcast(mgmt);	
logger.info("Message brodcasted");
} */

/*	public static void  replicate(Request  )
{
	int term=0; //Dhanu: change it wen needed
	logger.info("Replicate method called");
Management mgmt=ReplicationManager.generateAppendRPC(term, log);
logger.info("Mangement message generated");
ConnectionManager.broadcast(mgmt);	
logger.info("Message brodcasted");
} 
*/
}
