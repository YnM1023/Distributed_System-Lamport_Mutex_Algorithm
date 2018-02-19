import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerListener extends Thread{
	private Server server;
	public ServerListener(Server server) {
		this.server = server;
	}
	
	public void run() {
		System.out.println("server: "+server.getID()+" starts listenning at port: "+server.getPort());
		try(ServerSocket serverSock = new ServerSocket(server.getPort())){
			while(true) {
				Socket sock = serverSock.accept();
				
				InputStream in = sock.getInputStream();
				ObjectInputStream inStream = new ObjectInputStream(in);
				
				try {
					Message m = (Message) inStream.readObject();
					System.out.println(server.getID()+":"+m);
					
					// 3 opt: WRITE - READ - ENQUIRY
					server.deliver_message(m);
					
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				in.close();
				sock.close();
				
			}
		} catch(IOException e) {
			System.err.println(e);
		}
	}
	
}
