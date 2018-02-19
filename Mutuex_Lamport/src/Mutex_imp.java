import java.util.ArrayList;
import java.util.PriorityQueue;

public class Mutex_imp {
	private Client client;
	private ArrayList<String> pending_reply; 
	// request_queue is a PQ, used to 
	private PriorityQueue<Message> requestQueue;
	private boolean inWaiting;
	
	public Mutex_imp(Client client) {
		this.client = client;
		requestQueue = new PriorityQueue<>();
		inWaiting = false;
	}
	public void setUnwaiting() {this.inWaiting = false;}
	public void addRequest(Message m) {requestQueue.add(m);}
	public void delRequest() {requestQueue.poll();}
	public Message getHighestRequest() {return requestQueue.peek();}
	
	public synchronized void receive_request(Message m) {
		this.addRequest(m);
	}
	
	public synchronized void receive_release(Message m) {
		if(m.getID().equals(requestQueue.peek().getID())) {
			this.delRequest();
		}else {
			System.err.println("release sender's request not at the head of the queue!");
		}
	}
	
	public synchronized void receive_reply(Message m) {
		if(!inWaiting) {
			System.err.println("[Reply Error]: Receive reply but not in waiting!");
			
		}else if(m.getTs()<requestQueue.peek().getTs()){
			System.err.println("[Reply Error]: Receive reply with lower ts!");
		}else {
			System.out.println(client.getID()+": get reply from "+m.getID()+" - "+m);
			pending_reply.remove(m.getID());
		}
	}
	
	public synchronized void receive_server(Message m) {
		if(m.getOpt().equals("ENQUIRY")) {
			client.getFiles(m);
		}else if(m.getOpt().equals("FINISHED-READ")) {
			client.setFinished();
		}else if(m.getOpt().equals("FINISHED-WRITE")) {
			client.getOneFinished();
		}else {
			System.err.println("ERROR: SERVER Type with wrong opt!");
		}
	}
	
	public synchronized void send_request(Message m) {
		if(!inWaiting) {
			inWaiting = true;
			pending_reply = new ArrayList<>(client.getOtherClients());
			client.broadCast(m);
		}
	}
	
	public synchronized boolean isAllowedInCS() {
		// if there are no replies in pending from all other clients, means it can go into critical section if it is highest.
//		System.out.println("In isAllowedInCS: ");
//		System.out.println("  highest request from "+requestQueue.peek().getID());
//		System.out.println("  pending replies are "+pending_reply);
		if(requestQueue.peek().getID().equals(client.getID()) && pending_reply.isEmpty())
			return true;
		if(!pending_reply.isEmpty()) System.out.println("needs reply "+requestQueue.size());
		if(!requestQueue.peek().getID().equals(client.getID())) System.out.println("not highest! "+requestQueue.peek().getID()+" "+requestQueue.size());
		return false;
	}
}
