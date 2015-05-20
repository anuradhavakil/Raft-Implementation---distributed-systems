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

import java.util.ArrayList;
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
import poke.demo.ImageClient;
import poke.demo.pingClient;
import poke.server.conf.ServerConf;

public class ExternalServersManager {

	public void informClusters(int serverId) {
		
	ArrayList<String> nodes = new ArrayList<String> ();
	nodes.add("10.0.1.1");
	nodes.add("10.0.1.3");
	nodes.add("10.0.1.2");
	nodes.add("10.0.1.5");
	nodes.add("10.0.2.1");
	nodes.add("10.0.2.2");
	nodes.add("10.0.2.3");
	nodes.add("10.0.2.4");
	nodes.add("10.0.3.1");
	nodes.add("10.0.3.2");
	nodes.add("10.0.3.3");
	nodes.add("10.0.3.4");
	nodes.add("10.0.4.1");
	nodes.add("10.0.4.2");
	nodes.add("10.0.4.3");
	nodes.add("10.0.4.4");
	nodes.add("10.0.5.1");
	nodes.add("10.0.5.2");
	nodes.add("10.0.5.3");
	nodes.add("10.0.5.4");
	nodes.add("10.0.7.1");
	nodes.add("10.0.7.2");
	nodes.add("10.0.7.3");
	nodes.add("10.0.7.4");
	nodes.add("10.0.8.1");
	nodes.add("10.0.8.2");
	nodes.add("10.0.8.3");
	nodes.add("10.0.8.4");
	nodes.add("10.0.9.1");
	nodes.add("10.0.9.2");
	nodes.add("10.0.9.3");
	nodes.add("10.0.9.4");
	nodes.add("10.0.10.1");
	nodes.add("10.0.10.2");
	nodes.add("10.0.10.3");
	nodes.add("10.0.10.4");
	
	for(String n: nodes)
	{
		pingClient c = new pingClient(n,serverId);
		c.run();
		c.ping();
	}
	
	}
	
	public void pingClustersWithImage(poke.comm.Image.Request req, String ip, int serverId){
		
		ImageClient c = new ImageClient(req,ip,serverId);
		c.run();
		c.sendImageToServer();
	
	}
}
