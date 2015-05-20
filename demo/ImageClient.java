/*
 * copyright 2012, gash
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
package poke.demo;

import com.google.protobuf.ByteString;

import poke.client.ClientCommand;
import poke.client.ClientPrintListener;
import poke.client.SerialDeserial;
import poke.client.comm.CommListener;
import poke.comm.Image.Request;

/**
 * DEMO: how to use the command class
 * 
 * @author gash
 * 
 */
public class ImageClient {
	private String tag;
	private int count;
	private ClientCommand cc;
	private CommListener listener;
	private SerialDeserial obj;
	private String ip;
	private int serverId;
	private Request req;
	
	public ImageClient(Request req,String ip, int serverId) {
		this.ip = ip;
		this.serverId = serverId;
		this.req = req;
	}

	public void run() {
		cc = new ClientCommand(ip, 5570);
		listener = new ClientPrintListener(ip);
		cc.addListener(listener);
		obj =new SerialDeserial();
			
		
	}

	public void sendImageToServer()
	{
		
		cc.sendImageToServer(serverId,req);
	}
	
	
}
