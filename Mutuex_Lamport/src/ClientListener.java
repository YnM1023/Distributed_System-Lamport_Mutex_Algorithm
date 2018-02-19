import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientListener extends Thread{
	private Client client;
	public ClientListener(Client client) {
		this.client = client;
	}
	
	public void run() {
		System.out.println("client: "+client.getID()+" starts listenning at port: "+client.getPort());
		try(ServerSocket serverSock = new ServerSocket(client.getPort())){
			while(true) {
				Socket sock = serverSock.accept();
				System.out.println("[Listener]: get a socket from other provess");
				InputStream in = sock.getInputStream();
				ObjectInputStream inStream = new ObjectInputStream(in);
				
				try {
					Message m = (Message) inStream.readObject();
					System.out.println(client.getID()+":"+m);
					
					client.deliver_message(m);
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
