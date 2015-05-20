package poke.server.storage.jdbc;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.mysql.jdbc.Driver;

import com.google.protobuf.ByteString;
import com.mysql.jdbc.ResultSet;
import com.mysql.jdbc.Statement;

import java.sql.PreparedStatement;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class DatabaseClass {

	public static Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", "");
		} catch (Exception e) {
			System.out.println("Error Occured While Getting the Connection: - "+ e);
		}
		return connection;
	}

	/*public static ByteString pokeImageSerialize(String img) {
			File fnew=new File(img);
			ByteString data = null;
			try {
			BufferedImage originalImage=ImageIO.read(fnew);
			ByteArrayOutputStream baos=new ByteArrayOutputStream();		
		    ImageIO.write(originalImage, "png", baos );			
			byte[] image=baos.toByteArray();
	         data=ByteString.copyFrom(image);		        
			} catch (IOException e) {
				e.printStackTrace();
			}
		     return data;  		
	}*/
	public static void insertImage(ByteString image) {
		Connection connection = null;
		PreparedStatement statement = null;
		PreparedStatement prestmt = null;
		Statement stmt = null;
		ResultSet rs=null;
//		FileInputStream inputStream = null;

		try {
			connection = getConnection();
			byte[] imageInfo=image.toByteArray();	
			stmt = (Statement) connection.createStatement();
			rs= (ResultSet) stmt.executeQuery("select count(*) from imageinfo order by image_id desc limit 1");
			while(rs.next())
			{
				if(rs.getInt(1)<10)
				{
					System.out.println("less tgan 10 images can be stored");
					statement = connection.prepareStatement("insert into imageinfo(image_data) "+ "values(?)");
					statement.setBytes(1,imageInfo);
					statement.executeUpdate();
				}
			
				else
				{
					stmt = (Statement) connection.createStatement();
					rs= (ResultSet) stmt.executeQuery("select image_id from imageinfo order by image_id asc limit 1");		
					while(rs.next())
					{
						String query= "delete from imageinfo where image_id= ?";
						prestmt= connection.prepareStatement(query);
						int image_id=rs.getInt(1);
						prestmt.setInt(1,image_id);
						int rows= prestmt.executeUpdate();
						if(rows==1)
							System.out.println("oldest image deleted");
						statement = connection.prepareStatement("insert into imageinfo(image_data) "+ "values(?)");
						statement.setBytes(1,imageInfo);
						statement.executeUpdate();
					
					}
				}
			}
			
		} catch (SQLException e) {
			System.out.println("SQLException: - " + e);
		} finally {
			try {
				connection.close();
				//statement.close();
			} catch (SQLException e) {
				System.out.println("SQLException Finally: - " + e);
			}
		}
	}
	
	public static ArrayList<ByteString> retrieveImage()
	{
		ArrayList <ByteString> imageList= new ArrayList<ByteString>();
		try{
			Connection connection = getConnection();
			Statement stmt = (Statement) connection.createStatement();
			ResultSet rs = (ResultSet) stmt.executeQuery("select image_data from imageinfo");
			
			int i = 0;
			while (rs.next()) {
				byte[] blob= rs.getBytes(1);
				
				imageList.add(ByteString.copyFrom(blob));

			}
		}catch(Exception ex){
			System.out.println(ex.getMessage());
		}
		return imageList;
	}


}
