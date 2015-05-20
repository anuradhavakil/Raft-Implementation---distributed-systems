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
public class Client {
	private String tag;
	private int count;
	private ClientCommand cc;
	private CommListener listener;
	private SerialDeserial obj;
	
	public Client(String tag) {
		this.tag = tag;
	}

	public void run() {
		cc = new ClientCommand("localhost", 5570);
		listener = new ClientPrintListener("client1");
		cc.addListener(listener);
		obj =new SerialDeserial();
			
		
	}

	public void register()
	{
		ByteString image=obj.pokeImageSerialize("test/poke/server/resources/zero.png");
		cc.register(image,tag, 1000);
	}
	
	public void sendImage()
	{
		ByteString image= obj.pokeImageSerialize("test/poke/server/resources/zero.png");
		cc.sendImage("Hello", image, 1000);
	}
	
	public static void main(String[] args) {
		try {
			Client jab = new Client("client1");
			jab.run();
			jab.register();
			//jab.sendImage();
			
			// we are running asynchronously
			System.out.println("\nExiting in 5 seconds");
			//Thread.sleep(5000);
			//System.exit(0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
