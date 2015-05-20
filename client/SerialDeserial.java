package poke.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.google.protobuf.ByteString;

import poke.comm.App;
import poke.comm.App.Header;
import poke.comm.App.PayLoad;
import poke.comm.App.Ping;
import poke.comm.App.Request;



public class SerialDeserial {
	
	 
	public ByteString pokeImageSerialize(String img) {
	 	   
			
			PayLoad.Builder pay= PayLoad.newBuilder();
			File fileImage=new File(img);
			ByteString data = null;
			try {
			BufferedImage originalImage=ImageIO.read(fileImage);
			ByteArrayOutputStream os=new ByteArrayOutputStream();		
			ImageIO.write(originalImage, "png", os );			
			byte[] image=os.toByteArray();
	         data=ByteString.copyFrom(image);	
	        
	        
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	   
		     return data;      

		
		
	}
	
	public void pokeImageDeSerialize(ByteString bd) {	
		       
		byte[] imageInfo=bd.toByteArray();		
		InputStream in = new ByteArrayInputStream(imageInfo);		
		BufferedImage bImageFromConvert;
	  try{
			bImageFromConvert = ImageIO.read(in);	
			System.out.println(bImageFromConvert);
			
			Calendar calendar = Calendar.getInstance();			
	        long  a = calendar.getTimeInMillis();
			ImageIO.write(bImageFromConvert, "jpg", new File("test/poke/server/responses/image"+a+".png"));

			
		} catch (IOException e1) {
			
			System.out.println(e1);
			e1.printStackTrace();
		}

	
	}
}
