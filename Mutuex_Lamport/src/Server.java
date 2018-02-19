import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable{
	private LamportClock localClock;
	private String serverIP;
	private int serverPort;
	private String serverID;
	private ArrayList<String> files;
	
	public Server(String serverID, String configFile_server, String configFile_file) {
		this.serverID = serverID;
		this.files = new ArrayList<String>();
		this.configServer(configFile_server);
		this.configFiles(configFile_file);
	}
	
	private void configServer(String file) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = r.readLine())!=null) {
				String[] parts = line.split(" ");
				if(serverID.equals(parts[0])) {
					this.localClock = new LamportClock(Integer.parseInt(parts[3]));
					this.serverIP = parts[1];
					this.serverPort = Integer.parseInt(parts[2]);
					break;
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void configFiles(String file) {
		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line = null;
			while((line = r.readLine())!=null) {
				File newFile = new File(line);
				files.add(line);
				
				if(newFile.exists() == false) {
					try {
						newFile.createNewFile();
					} catch(IOException e) {
						e.printStackTrace();
						System.err.println("Cannot creat this file");
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getID() {return serverID;}
	public int getPort() {return serverPort;}
	public LamportClock getClock() {return localClock;}
	public ArrayList<String> getFiles() {return files;}
	
	@Override
	public void run() {
		run_listenning();
		// TODO Auto-generated method stub
		System.out.println(files);
	}
	
	public void run_listenning() {
		Thread listener = new ServerListener(this);
		listener.start();
	}
	
	private synchronized String read(String file) {
		String lastLine = "";
		// use RandomAccessFile class to quickly get the last line in file.
		File f = new File(file);
		StringBuilder builder = new StringBuilder();
		try {
			RandomAccessFile rfile = new RandomAccessFile(f,"r");
			long fileLength = file.length()-1;
			rfile.seek(fileLength);
			for(long pointer = fileLength; pointer>=0;pointer--) {
				rfile.seek(pointer);
				char c;
				c = (char)rfile.read();
				if(c=='\n') {
					break;
				}
				builder.append(c);
			}
			lastLine = builder.reverse().toString();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lastLine;
	}
	
	private synchronized void write(String file, String command) {
		try {
		    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
		    out.println(command);
		    out.close();
		} catch (IOException e) {
		    //exception handling left as an exercise for the reader
		}
	}
	
	public synchronized void deliver_message(Message m) {
		this.localClock.message(m.getTs());
		
		String opt = m.getOpt();
		
		String r2IP = m.getIP();
		int r2Port  = m.getPort();
		if(opt.equals("READ")) {
			String command = read(m.getFile());
			Message rm = new Message("SERVER",serverID,"FINISHED-READ",this.localClock.get_time(),command,serverID,m.getFile(),null,serverIP,serverPort);
			send_message(rm, r2IP, r2Port);
		}else if(opt.equals("WRITE")) {
			write(m.getFile(),m.getCom());
			String command = "Writing finished!";
			Message rm = new Message("SERVER",serverID,"FINISHED-WRITE",this.localClock.get_time(),command,serverID,m.getFile(),null,serverIP,serverPort);
			send_message(rm, r2IP, r2Port);
		}else if(opt.equals("ENQUIRY")){
			Message rm = new Message("SERVER",serverID,"ENQUIRY",this.localClock.get_time(),null,serverID,m.getFile(),files,serverIP,serverPort);
			send_message(rm, r2IP, r2Port);
			System.out.println("has reply to client for an enquiry");
		}else{
			System.err.println("ERROR: Wrong operation type - "+opt);
		}
	}
	
	public synchronized void send_message(Message m, String IP, int Port) {
		try {
			Socket sock = new Socket(IP,Port);
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
	
	public static void main(String[] args) {
		Server s = new Server(args[0],args[1],args[2]);
		Thread t = new Thread(s);
		t.start();
	}
	
}
