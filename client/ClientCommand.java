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
package poke.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import poke.client.comm.CommConnection;
import poke.client.comm.CommListener;
import poke.comm.App.Header;
import poke.comm.App.PayLoad;
import poke.comm.App.Ping;
import poke.comm.App.Request;
import poke.comm.Image.*;
import poke.server.ServerCommConnection;

/**
 * The command class is the concrete implementation of the functionality of our
 * network. One can view this as a interface or facade that has a one-to-one
 * implementation of the application to the underlining communication.
 * 
 * IN OTHER WORDS (pay attention): One method per functional behavior!
 * 
 * @author gash
 * 
 */
public class ClientCommand {
	protected static Logger logger = LoggerFactory.getLogger("client");

	private String host;
	private int port;
	private CommConnection comm;

	public ClientCommand(String host, int port) {
		this.host = host;
		this.port = port;

		init();
	}

	
	private void init() {
		comm = new CommConnection(host, port);
	}

	/**
	 * add an application-level listener to receive messages from the server (as
	 * in replies to requests).
	 * 
	 * @param listener
	 */
	public void addListener(CommListener listener) {
		comm.addListener(listener);
	}

	/**
	 * Our network's equivalent to ping
	 * 
	 * @param tag
	 * @param num
	 */
	public void sendImage(String caption, ByteString image, int clientid ) {
		// data to send
	/*	Ping.Builder ping = Ping.newBuilder();
		ping.setIsPing(false);
		
		// payload containing data
		Request.Builder r = Request.newBuilder();
		PayLoad.Builder p = PayLoad.newBuilder();
		p.setData(image);
		r.setPayload(p.build());
		r.setPing(ping);
		
		

		// header with routing info
		Header.Builder h = Header.newBuilder();
		h.setOriginator(1000);
		h.setTag("test finger");
		h.setCaption(caption);
		h.setTime(System.currentTimeMillis());
		h.setIsClient(true);
		h.setClientId(clientid);
		h.setClusterId(6);
	
		h.setRoutingId(Header.Routing.PING);
		r.setHeader(h.build());
		

		Request req = r.build();

		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		}
		
		*/
		
		poke.comm.Image.Ping.Builder ping = poke.comm.Image.Ping.newBuilder();
		ping.setIsPing(false);
		
		// payload containing data
		poke.comm.Image.Request.Builder r = poke.comm.Image.Request.newBuilder();
		poke.comm.Image.PayLoad.Builder p = poke.comm.Image.PayLoad.newBuilder();
		p.setData(image);
		r.setPayload(p.build());
		r.setPing(ping);
		
		

		// header with routing info
		poke.comm.Image.Header.Builder h = poke.comm.Image.Header.newBuilder();
		h.setCaption(caption);
		h.setIsClient(true);
		h.setClientId(clientid);
		h.setClusterId(6);
		r.setHeader(h.build());
		poke.comm.Image.Request req = r.build();

		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		}
		
	}
	
	
	public void ping(int serverId) {
		
		
		poke.comm.Image.Ping.Builder ping = poke.comm.Image.Ping.newBuilder();
		ping.setIsPing(false);
		
		// payload containing data
		poke.comm.Image.Request.Builder r = poke.comm.Image.Request.newBuilder();
		poke.comm.Image.PayLoad.Builder p = poke.comm.Image.PayLoad.newBuilder();
		p.setData(null);
		r.setPayload(p.build());
		r.setPing(ping);
		
		

		// header with routing info
		poke.comm.Image.Header.Builder h = poke.comm.Image.Header.newBuilder();
		h.setCaption(null);
		h.setIsClient(false);
		h.setClientId(serverId);
		h.setClusterId(6);
		r.setHeader(h.build());
		poke.comm.Image.Request req = r.build();

		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		}
		
	}
	
	
	
	public void register(ByteString image, String tag, int num) {
		// data to send
		
		poke.comm.Image.Ping.Builder ping = poke.comm.Image.Ping.newBuilder();
		ping.setIsPing(false);
		
		// payload containing data
		poke.comm.Image.Request.Builder r = poke.comm.Image.Request.newBuilder();
		poke.comm.Image.PayLoad.Builder p = poke.comm.Image.PayLoad.newBuilder();
		p.setData(image);
		r.setPayload(p.build());
		r.setPing(ping);
		
		

		// header with routing info
		poke.comm.Image.Header.Builder h = poke.comm.Image.Header.newBuilder();
		h.setCaption("register");
		h.setIsClient(true);
		h.setClientId(num);
		h.setClusterId(6);
		r.setHeader(h.build());
		poke.comm.Image.Request req = r.build();

		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		} 
		
		/*Ping.Builder ping = Ping.newBuilder();
		ping.setIsPing(false);
		
		// payload containing data
		Request.Builder r = Request.newBuilder();
		PayLoad.Builder p = PayLoad.newBuilder();
		p.setData(image);
		r.setPayload(p.build());
		r.setPing(ping);
		
		

		// header with routing info
		Header.Builder h = Header.newBuilder();
		h.setOriginator(1000);
		h.setTag("register");
		h.setCaption("register");
		h.setTime(System.currentTimeMillis());
		h.setIsClient(true);
		h.setClientId(num);
		h.setClusterId(6);
	
		h.setRoutingId(Header.Routing.PING);
		r.setHeader(h.build());
		

		Request req = r.build();

		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		} */
	}
	
	public void poke(String tag, int num) {
		// data to send
		Ping.Builder f = Ping.newBuilder();
		f.setTag(tag);
		f.setNumber(num);

		// payload containing data
		Request.Builder r = Request.newBuilder();
		PayLoad.Builder p = PayLoad.newBuilder();
		p.setPing(f.build());
		r.setPayload(p.build());

		// header with routing info
		Header.Builder h = Header.newBuilder();
		h.setOriginator(1000);
		h.setTag("test finger");
		h.setTime(System.currentTimeMillis());
		h.setIsClient(true);
		h.setRoutingId(Header.Routing.PING);
		r.setHeader(h.build());
		

		Request req = r.build();

		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		}
	}


	public void sendImageToServer(int serverId, poke.comm.Image.Request req) {
	
		try {
			comm.sendMessage(req);
		} catch (Exception e) {
			logger.warn("Unable to deliver message, queuing");
		}
		
	}
}
