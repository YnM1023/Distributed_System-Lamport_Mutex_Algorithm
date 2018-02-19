import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;

/** 
 * Author: Yingnan Mei
 * version 1.0
 * Created to implement client-side
 */
public class Client implements Runnable{
	//==========
	// FIELDS: |
	//==========
	private LamportClock localClock;
	private String clientID;
	private String clientIP;
	private int clientPort;
	// servers stores serverID and corresponds sockets 
	private HashMap<String, String> servers;
	private HashMap<String, String> clients;
	private ArrayList<String> serverIDs;
	private ArrayList<String> clientIDs;
	
	// files used to store the names of files in servers, got by Enquiry() function
	private ArrayList<String> files;
	private Mutex_imp mutex;
	
	private int finish_CS;
	private ArrayList<String> pending_servers;
	
	private boolean finished;
	private int waitingFinished;
	
	//===========
	// METHODS: |
	//===========
	public Client(String clientID, String configFile_server, String configFile_client) {
		finish_CS = 0;
		this.clientID = clientID;
		this.servers = new HashMap<String, String>();
		this.clients = new HashMap<String, String>();
		this.serverIDs = new ArrayList<>();
		this.clientIDs = new ArrayList<>();
		this.configServer(configFile_server);
		this.configClient(configFile_client);
		this.mutex = new Mutex_imp(this);
		this.pending_servers = new ArrayList<>(serverIDs);
		this.finished = false;
		this.waitingFinished = 0;
	}
	
	public String getIP() {return clientIP;}
	public int getPort() {return clientPort;}
	public String getID() {return clientID;}
	public LamportClock getClock() {return localClock;}
	public ArrayList<String> getOtherClients() {return clientIDs;}
	
	private void configServer(String file) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = r.readLine())!=null) {
				String[] parts = line.split(" ");
				this.addServer(parts[0], parts[1], parts[2]);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void configClient(String file) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = r.readLine())!=null) {
				String[] parts = line.split(" ");
				if(clientID.equals(parts[0])) {
					this.localClock = new LamportClock(Integer.parseInt(parts[3]));
					this.clientIP = parts[1];
					this.clientPort = Integer.parseInt(parts[2]);
					continue;
				}
				this.addClient(parts[0], parts[1], parts[2]);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void addServer(String serverID, String ip, String port) {
		this.servers.put(serverID, ip+":"+port);
		this.serverIDs.add(serverID);
	}
	
	private void addClient(String clientID, String ip, String port) {
		this.clients.put(clientID,  ip+":"+port);
		this.clientIDs.add(clientID);
	}
	
	private int sizeOfServers() {return serverIDs.size();}
	private int sizeOfClients() {return clientIDs.size();}
	
	//========================================================
	// Process running function part (run - listen - generate)
	//--------------------------------------------------------
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("client-"+this.clientID+" is started!");
		
		run_listenning();
		initial_connection();
		System.out.println("finished initial connection!");
		while(!pending_servers.isEmpty()) {
			// we need to confirm filenames from every server!
			System.out.println(pending_servers);
		}
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("  There are "+this.sizeOfServers()+" server processes!");
		System.out.println("  "+this.serverIDs);
		System.out.println("  There are "+this.sizeOfClients()+" client processes!");
		System.out.println("  "+this.clientIDs);
		
		
		
		System.out.println("Type 'start' if preparing finished.");
		Scanner keyboard = new Scanner(System.in);
		String word;
		while(!(word = keyboard.next()).equals("start")) {
			System.out.println("Not match, please type in again.");
		}
		
		while(finish_CS<30) {
			System.out.println("finish_CS is "+finish_CS);
			generate_opt();
			
			while(!mutex.isAllowedInCS()) {
				// keep checking whether this client is allowed to go into CS
//				System.out.println(clientID+": check requestQueue but result is false.");
				
			}
			
			execute_CS();
			
			while(!finished) {
				// keep checking whether client has received finished informing from server
				System.out.print("-");
			}
			System.out.println("");
			// remember to reset the finished as FALSE for the futher!
			finished = false;
			
			Message release = new Message("RELEASE",clientID,null,this.localClock.get_time(),null,null,null,null,clientIP,clientPort);
			mutex.receive_release(release);
			broadCast(release);
			mutex.setUnwaiting();
			finish_CS++;
		}
		
		System.out.println("I am done!");
	}
	
	//-------------------------------------------------------------------------------------
	// initial_connection used to get whole file names from servers
	private void initial_connection() {
		for(String serverID : serverIDs) {
			Message m =new Message("SERVER",clientID,"ENQUIRY",this.getClock().get_time(),null,serverID,null,null,clientIP,clientPort);
			String addr = servers.get(serverID);
			send_message(addr,m);
		}
	}
	
	//---------------------------------------------------------------------------------
	// run_listenning used for client to accept messages form other clients and servers
	private void run_listenning() {
		Thread listener = new ClientListener(this);
		listener.start();
	}
	
	private synchronized void execute_CS() {
		Message m = mutex.getHighestRequest();
		if(m.getOpt().equals("READ")) {
			System.out.println("=== execute critical section with READ! ===");
			String addr = servers.get(m.getTarget());
			send_message(addr,m);
		}else if(m.getOpt().equals("WRITE")) {
			System.out.println("=== execute critical section with WRITE! ===");
			waitingFinished = 0;
			for(String addr:servers.values()) {
				send_message(addr,m);
			}
		}else{
			System.err.println("[ERROR]: Worng type of request! "+m);
		}
	}
	
	//----------------------------------------------------------------------------------
	// generate_opt() used to periodically generates random requests on files of servers
	private void generate_opt() {
		Random rand = new Random();
		this.localClock.event();
		
		int delayTime = rand.nextInt(1000)+200;
			
		try {
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Read/Write = 30/70
		int select_range = rand.nextInt(101);
		String select_server = serverIDs.get(rand.nextInt(serverIDs.size()));
		
		String select_file = files.get(rand.nextInt(files.size()));
			
		Message m;
		int ts = this.localClock.get_time();
		if(select_range<30) {
			m = new Message("REQUEST",clientID,"READ",ts,null,select_server,select_file,null,clientIP,clientPort);
		}else {
			String newLine = "<"+clientID+", "+ts+">";
			m = new Message("REQUEST",clientID,"WRITE",ts,newLine,null,select_file,null,clientIP,clientPort);
		}
//		
//		
//		Message m;
//		int ts = this.localClock.get_time();
//		if(select_range<30) {
//			m = new Message("REQUEST",clientID,"READ",ts,null,select_server,null,null);
//		}else {
//			String newLine = "<"+clientID+", "+ts+">";
//			m = new Message("REQUEST",clientID,"WRITE",ts,newLine,null,null,null);
//		}
		this.mutex.send_request(m);
	}
	
	//==================================================================================
	
	//==================================================================================
	// Methods used to implement Lamport's Mutex Algorithm (broadCast) 
	//----------------------------------------------------------------------------------
	// broadCast(): used by Clients to broadCast their REPLY or REQUEST to other Clients
	public synchronized void broadCast(Message m) {
		System.out.println(clientID+": Sends Message - "+m);
		this.localClock.event();
		for(String cID:clientIDs) {
			if(cID.equals(clientID)) continue;
			String target = clients.get(cID);
			System.out.println("target address is "+target);
			send_message(target,m);
		}
		
		if(m.getType().equals("REQUEST")) {
			this.mutex.addRequest(m);
		}
	}
	
	//------------------------------------------------------------------------------
	// send_message(): used to send message m to specified ip:port
	public synchronized void send_message(String targetAddr, Message m) {
		String[] addr = targetAddr.split(":");
		try {
			Socket sock = new Socket(addr[0],Integer.parseInt(addr[1]));
			OutputStream out = sock.getOutputStream();
			ObjectOutputStream outStream = new ObjectOutputStream(out);
			outStream.writeObject(m);
//			out.close();
//			sock.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void deliver_message(Message m) {
		this.localClock.message(m.getTs());
		
		if(m.getType().equals("REQUEST")) {
			mutex.receive_request(m);
			// reply to sender when 
			String target = clients.get(m.getID());
			Message reply = new Message("REPLY",clientID,null,this.localClock.get_time(),null,null,null,null,clientIP,clientPort);
			System.out.println("have reply to "+m.getID());
			send_message(target, reply);
		}else if(m.getType().equals("REPLY")) {
			mutex.receive_reply(m);
		}else if(m.getType().equals("RELEASE")) {
			mutex.receive_release(m);
		}else if(m.getType().equals("SERVER")){
			mutex.receive_server(m);
		}else {
			System.out.println("[ERROR]: Wrong type of message"+m);
		}
		
	}
	
	public synchronized void setFinished() {
		System.out.println("[Client]: set finished to true!");
		finished = true;
	}
	public synchronized void getFiles(Message m) {
		
		pending_servers.remove(m.getID());
		System.out.println("Removed "+m.getID()+" from pending_servers");
		if(files==null) {
			files = m.getFileList();
		}else {
			if(!files.equals(m.getFileList())) System.err.println("Files are not same on servers!");
		}
	}
	public synchronized void getOneFinished() {
		waitingFinished++;
		if(waitingFinished==serverIDs.size()) finished = true;
	}
	
	//==============================================================================
	
	public static void main(String[] args) {
		Client c = new Client(args[0],args[1],args[2]);
		Thread t = new Thread(c);
		t.start();
	}
}
