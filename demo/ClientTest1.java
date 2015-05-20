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

/**
 * DEMO: how to use the command class
 * 
 * @author gash
 * 
 */
public class ClientTest1 {
	private String tag;
	private int count;
	private ClientCommand cc;
	private CommListener listener;
	private SerialDeserial obj;
	
	public ClientTest1(String tag) {
		this.tag = tag;
	}

	public void run() {
		cc = new ClientCommand("10.0.6.2", 5570);
		listener = new ClientPrintListener("client1");
		cc.addListener(listener);
		obj =new SerialDeserial();
			
		
	}

	public void register()
	{
		ByteString image=obj.pokeImageSerialize("test/poke/server/resources/1.png");
		cc.register(image,tag, 1001);
	}
	
	public void sendImage()
	{
		ByteString image=obj.pokeImageSerialize("test/poke/server/resources/1.png");
		cc.sendImage("Hello", image, 1001);
	}
	public void register1()
	{
		ByteString image=obj.pokeImageSerialize("test/poke/server/resources/Beastie.png");
		cc.register(image,tag, 1001);
	}
	
	public void sendImage1()
	{
		ByteString image=obj.pokeImageSerialize("test/poke/server/resources/Beastie.png");
		cc.sendImage("Hello", image, 1001);
	}
	public static void main(String[] args) {
		try {
			final ClientTest1 jab = new ClientTest1("client1");
			//jab.run();
			//jab.register();
			//jab.sendImage();			
			 Runnable t1 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register();
			        	jab.sendImage();
			        }
			    };
			    Runnable t3 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register();
			        	jab.sendImage();
			        }
			    };

			    Runnable t2 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register1();
			        	jab.sendImage1();

			        }
			    };
			    Runnable t4 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register1();
			        	jab.sendImage1();

			        }
			    };
			    Runnable t5 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register();
			        	jab.sendImage();

			        }
			    };
			    Runnable t6 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register1();
			        	jab.sendImage1();

			        }
			    };
			    Runnable t7 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register();
			        	jab.sendImage();

			        }
			    };
			    Runnable t8 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register1();
			        	jab.sendImage1();

			        }
			    };
			    Runnable t9 = new Runnable() {
			        public void run() {
			        	jab.run();
						jab.register();
			        	jab.sendImage();

			        }
			    };

			    new Thread(t1).start();			 
			    new Thread(t2).start();
			    new Thread(t3).start();
			    new Thread(t4).start();
			    new Thread(t5).start();
			    new Thread(t6).start();
			    new Thread(t7).start();
			    new Thread(t8).start();
			    new Thread(t9).start();
			// we are running asynchronously
			System.out.println("\nExiting in 5 seconds");
			//Thread.sleep(5000);
			//System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
