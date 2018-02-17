
public class Message implements Comparable<Message>{
	String opt;
	int timeStamp;
	String newLine;
	
	public Message(String opt, String newLine, int timeStamp) {
		this.opt       = opt;
		this.timeStamp = timeStamp;
		this.newLine   = newLine;
	}

	@Override
	public int compareTo(Message m) {
		// TODO Auto-generated method stub
		if(this.timeStamp>m.timeStamp) return 1;
		else if(this.timeStamp<m.timeStamp) return -1;
		else return 0;
	}
}
