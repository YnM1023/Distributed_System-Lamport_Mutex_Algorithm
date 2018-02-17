import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** 
 * Author: Yingnan Mei
 * version 1.0
 * Created to implement client-side
 */
public class Client implements Runnable{
	
	private LamportClock localClock;
	private String clientID;
	private int serverNumb;
	// servers stores serverID and corresponds sockets 
	private HashMap<String, Socket> servers;
	private ArrayList<String> serverIDs;
	
	public Client(String clientID, int d, int serverNumb) {
		this.localClock = new LamportClock(d);
		this.clientID = clientID;
		this.servers = new HashMap<String, Socket>();
		this.serverNumb = serverNumb;
		this.serverIDs = new ArrayList<>();
	}
	
	public void addServer(String serverID, String ip, String port) {
		try {
			Socket sock = new Socket(ip,Integer.parseInt(port));
			this.servers.put(serverID, sock);
			this.serverIDs.add(serverID);
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setServerNumb(int n) {
		this.serverNumb = n;
	}
	
	public Message select_operation(List<String> files) {
		Random rand = new Random();
		int opt = rand.nextInt(2);
		int target = rand.nextInt(files.size()+1);
		Message m;
		if(opt==0) {
			m = new Message("READ", null, this.localClock.get_time());
		}else {
			String info = "<"+this.clientID+", "+this.localClock.get_time()+">";
			m = new Message("WRITE", info, this.localClock.get_time());
		}
		this.localClock.event();
		return m;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("client-"+this.clientID+" is started!");
		System.out.println("  There are "+this.serverNumb+" server processes!");
		System.out.println("  "+this.serverIDs);

		for(int i=0;i<20;i++) {
			Message m = this.select_operation(new ArrayList<String>());
			System.out.println(m.opt+" - "+m.timeStamp+" - "+m.newLine);
		}
	}
	
	
	
	
	
	public static void main(String[] args) {
		try {
			BufferedReader read = new BufferedReader(new FileReader(args[2]));
			int serverNumb = Integer.parseInt(read.readLine());
			Client c = new Client(args[0], Integer.parseInt(args[1]), serverNumb);
			String line;
			int index = 0;
			while(index<serverNumb && (line = read.readLine()) != null) {
				String[] part = line.split(" ");
				c.addServer(part[0], part[1], part[2]);
				index++;
			}
			c.setServerNumb(index);
			Thread t = new Thread(c);
			t.start();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
