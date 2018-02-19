import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Comparable<Message>, Serializable{
	private String type; // REQUEST - REPLY - RELEASE - SERVER
	private String ID;   // sender ID
	private String opt;  // READ - WRITE - ENQUIRY
	private int timeStamp;
	private String command;  // <ClientID, timeStamp>
	private String serverID; // target server
	private String file;     // target file on server
	private ArrayList<String> files; // list of filenames on servers
	private String IP;  // IP of sender
	private int port;   // port of sender
	
	public Message(String type, String ID, String opt, int timeStamp, String command, String serverID, String file, ArrayList<String> files, String IP, int port) {
		this.type      = type;
		this.ID	       = ID;
		this.opt       = opt;
		this.timeStamp = timeStamp;
		this.command   = command;
		this.serverID  = serverID;
		this.file      = file;
		this.files	   = files;
		this.IP	       = IP;
		this.port      = port;
	}

	@Override
	public int compareTo(Message m) {
		// TODO Auto-generated method stub
		if(this.timeStamp>m.timeStamp) return 1;
		else if(this.timeStamp<m.timeStamp) return -1;
		else return 0;
	}
	
	public String getType() {return type;}
	public String getID() {return ID;}
	public String getOpt() {return opt;}
	public int getTs() {return timeStamp;}
	public String getCom() {return command;}
	public String getTarget() {return serverID;}
	public String getFile() {return file;}
	public ArrayList<String> getFileList() {return files;}
	public String getIP() {return IP;}
	public int getPort() {return port;}
	
	public String toString() {
		return "Message from "+ID+" "+type+" "+opt+" timeStamp:"+timeStamp+" "+command+" "+serverID+" "+file+" "+files+" "+IP+":"+port;
	}
}
