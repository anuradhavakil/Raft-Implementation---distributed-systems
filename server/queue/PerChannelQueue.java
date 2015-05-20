/*
 * copyright 2015, gash
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
package poke.server.queue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.lang.Thread.State;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.comm.App.Request;
import poke.core.Mgmt.Management;

/**
 * A server queue exists for each connection (channel). A per-channel queue
 * isolates clients. However, with a per-client model. The server is required to
 * use a master scheduler/coordinator to span all queues to enact a QoS policy.
 * 
 * How well does the per-channel work when we think about a case where 1000+
 * connections?
 * 
 * @author gash
 * 
 */
public class PerChannelQueue implements ChannelQueue {
	protected static Logger logger = LoggerFactory.getLogger("server");

	// The queues feed work to the inbound and outbound threads (workers). The
	// threads perform a blocking 'get' on the queue until a new event/task is
	// enqueued. This design prevents a wasteful 'spin-lock' design for the
	// threads
	//
	// Note these are directly accessible by the workers
	LinkedBlockingDeque<PerChannelQueueEntry> inbound;
	LinkedBlockingDeque<PerChannelQueueEntry> outbound;
	Channel channel;

	// This implementation uses a fixed number of threads per channel
	private OutboundAppWorker oworker;
	private InboundAppWorker iworker;

	// not the best method to ensure uniqueness
	private ThreadGroup tgroup = new ThreadGroup("PerChannelQ-" + System.nanoTime());

	public PerChannelQueue(Channel channel) {
		this.channel = channel;
		init();
	}

	protected void init() {
		inbound = new LinkedBlockingDeque<PerChannelQueueEntry>();
		outbound = new LinkedBlockingDeque<PerChannelQueueEntry>();

		iworker = new InboundAppWorker(tgroup, 1, this);
		iworker.start();

		oworker = new OutboundAppWorker(tgroup, 1, this);
		oworker.start();

		// let the handler manage the queue's shutdown
		// register listener to receive closing of channel
		// channel.getCloseFuture().addListener(new CloseListener(this));
	}

	protected Channel getChannel() {
		return channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#shutdown(boolean)
	 */
	@Override
	public void shutdown(boolean hard) {
		logger.info("server is shutting down");

		channel = null;

		if (hard) {
			// drain queues, don't allow graceful completion
			inbound.clear();
			outbound.clear();
		}

		if (iworker != null) {
			iworker.forever = false;
			if (iworker.getState() == State.BLOCKED || iworker.getState() == State.WAITING)
				iworker.interrupt();
			iworker = null;
		}

		if (oworker != null) {
			oworker.forever = false;
			if (oworker.getState() == State.BLOCKED || oworker.getState() == State.WAITING)
				oworker.interrupt();
			oworker = null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueRequest(eye.Comm.Finger)
	 */
	@Override
	public void enqueueRequest(Request req, Channel ch) {
		try {
			PerChannelQueueEntry p = new PerChannelQueueEntry(req,ch);
			inbound.put(p);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}
	public void enqueueRequest(poke.comm.Image.Request req, Channel ch) {
		try {
			PerChannelQueueEntry p = new PerChannelQueueEntry(req,ch);
			inbound.put(p);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueResponse(eye.Comm.Response)
	 */
	@Override
	public void enqueueResponse(Request reply, Channel ch) {
		if (reply == null)
			return;

		try {
			PerChannelQueueEntry p = new PerChannelQueueEntry(reply,ch);
			outbound.put(p);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for reply", e);
		}
	}
	public void enqueueResponse2(poke.comm.Image.Request reply, Channel ch) {
		if (reply == null)
			return;

		try {
			PerChannelQueueEntry p = new PerChannelQueueEntry(reply,ch);
			outbound.put(p);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for reply", e);
		}
	}

	public class CloseListener implements ChannelFutureListener {
		private ChannelQueue sq;

		public CloseListener(ChannelQueue sq) {
			this.sq = sq;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			sq.shutdown(true);
		}
	}

	public LinkedBlockingDeque<PerChannelQueueEntry> getInbound() {
		return inbound;
	}

	public LinkedBlockingDeque<PerChannelQueueEntry> getOutbound() {
		return outbound;
	}
	public static class PerChannelQueueEntry {
		
		public PerChannelQueueEntry(Request req, Channel ch) {
			this.req = req;
			this.channel = ch;
		}
		
		public PerChannelQueueEntry(poke.comm.Image.Request req, Channel ch) {
			this.imgReq = req;
			this.channel = ch;
		}

		public Request req;
		public poke.comm.Image.Request imgReq;
		public Channel channel;
	}
}
