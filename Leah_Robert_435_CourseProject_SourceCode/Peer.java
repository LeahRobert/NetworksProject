import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


/**
 * This class implements a peer.
 * @author Leah
 *
 * PeerId: the current peer's id
 * Left: the peer that the current peer listens to
 * Right: the peer that the current peer sends to
 * IP: Localhost IP address
 * Port: current peers' port number
 * PeerList: list of all peers in network (minus Chair)
 * ss: ServerSocket used throughout program
 * Broadcast: true if current peer has received the broadcast, otherwise false
 * State: true if current peer has voted
 * Vote: the current peer's vote on the issue (TRUE, FALSE, NULL)
 */

public class Peer implements Runnable {

	private int PeerId;
	private Peer Left;
	private Peer Right;
	private java.net.InetAddress IP;
	private int Port;
	private ArrayList<Peer> PeerList;
	private ServerSocket ss;
	private boolean Broadcast = false;
	private boolean State = false;
	private GUI.Vote V;

	/**
	 * Constructor for Peer
	 * @param id
	 * @param ip
	 * @param port
	 * @throws IOException
	 */
	public Peer(int id, java.net.InetAddress ip, int port) throws IOException {
		this.PeerId = id;
		this.IP = ip;
		this.Port = port;
	}

	/**
	 * Since Peer is a thread, this is what is run first
	 * If the current peer is the Chair of the network, it sends out the vote file
	 * If it is not the Chair of the network, it waits until it can read and download the file
	 */
	@Override
	public synchronized void run() {
		System.out.println("Port for peer with ID " + PeerId + " is: " + Port);
		PeerList = Main.getPeerList();
		int chairId = Main.getChairId();
		Peer chair = Main.getChairPeer();

		// BROADCAST THAT THERE WILL BE A VOTE
		if (PeerId == chairId) {
			try {
				System.out.println("Broadcasting message to peers... ");
				broadcast();
			} catch (IOException e) {
				System.out.println("Message could not be transmitted. ");
			}
		} 
		else {
			boolean broadcastRead = false;
			while (!broadcastRead) {
				try {
					readFromSocketOne();
					broadcastRead = true;
					Broadcast = true;
				} catch (IOException e) {
					System.out.println("Message was not read by Peer " + PeerId);
					System.out.println("Peer " + PeerId + " trying to read again. ");
				}
			}
		}

		// WAIT UNTIL ALL PEERS HAVE GOTTEN BROADCASTED MESSAGE
		boolean wait = false;
		while (!wait) {
			wait = waitForBroadcast();
		}

		// SET RING TOPOLOGY - REQUEST NETWORK ACCESS
		if (!(PeerId == chairId)) {
			// set ring topology: if current peer is not the chair, request access to chair's network
			System.out.println("Peer " + PeerId + " requesting access to network... ");
			requestNetworkAccess();
		}

		// SLEEP FOR 2 SECONDS
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		showRing();

		// SEND FILE THROUGH RING TOPOLOGY
		if (PeerId == chairId) {
			try {
				System.out.println("Chair with ID " + PeerId + " sending file... ");
				sendFile(chair.getRightPeer().getPort());
			} catch (IOException e) {
				System.out.println("File not transmitted. ");
				e.printStackTrace();
			}
		} else {
			boolean fileRead = false;
			System.out.println("Peer with ID " + PeerId + " reading file... ");
			while (!fileRead) {
				try {
					readFileFromSocket();
					fileRead = true;
				} catch (IOException e) {
				}
			}
		}

		// SLEEP FOR 2 SECONDS
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} 

		// SEND VOTE THROUGH RING TOPOLOGY
		// chair peer must send out the voting message
		String answer = "Aye 0 Nay 0";
		String results = "";
		if (PeerId == chairId) {
			try {
				System.out.println("Chair with ID " + PeerId + " sending vote... ");
				writeToSocket(answer, this.getRightPeer().getPort());
			} catch (IOException e) {
				System.out.println("Voting message was not sent by Chair. ");
				e.printStackTrace();
			}
		}
		boolean voteRead = false;
		System.out.println("Peer with ID " + PeerId + " reading vote... ");
		while (!voteRead) {
			try {
				results = readFromSocketTwo();
				voteRead = true;
			} catch (IOException e) {
			}
		}

		// SLEEP FOR 2 SECONDS
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		// CALCULATE RESULTS
		String result = "";
		if (PeerId == chairId) {
			result = calculateResults(results);
		}

		// SEND RESULT THROUGH RING TOPOLOGY
		if (PeerId == chairId) {
			try {
				System.out.println("Chair with ID " + PeerId + " sending result... ");
				writeResults(result, this.getRightPeer().getPort());
			} catch (IOException e) {
				System.out.println("Result was not sent by Chair. ");
			}
		}
		boolean resultRead = false;
		String finalResult = "";
		System.out.println("Peer with ID " + PeerId + " reading result... ");
		while (!resultRead) {
			try {
				finalResult = readResults();
				resultRead = true;
			} catch (IOException e) {
			}
		}

		// PRINT FINAL RESULT 
		System.out.println("--------------------------------------------------");
		System.out.println("Peer with ID " + PeerId + "\nHere is the final result: " + finalResult);
		System.out.println("--------------------------------------------------");
		System.exit(1);

	}

	/**
	 * Writes the results (output) to the specified port number
	 * @param output
	 * @param port
	 * @throws IOException
	 */
	private void writeResults(String output, int port) throws IOException {
		Peer chair = Main.getChairPeer();
		if (!this.getRightPeer().equals(chair)) {
			ss.close();
		}
		ss = new ServerSocket(port);
		byte[] byteArray2 = new byte[1024];
		while(true) {
			Socket s = ss.accept();
			byte[] byteArray = output.getBytes(StandardCharsets.US_ASCII);
			OutputStream out = s.getOutputStream();
			out.write(byteArray, 0, byteArray.length);
			out.flush();
			InputStream in = s.getInputStream();
			in.read(byteArray2, 0, byteArray2.length);
			String input = new String(byteArray2);
			input = cutString(input);
			out.flush();
			out.close();
			in.close();
			s.close();
			//confirm Peer received message by receiving same message back
			if (input.equalsIgnoreCase("received2")) break;
		}
	}

	/**
	 * This method is called when something is expected over the network
	 * @throws IOException
	 */
	private String readResults() throws IOException {
		int chairId = Main.getChairId();
		Socket s = new Socket(IP, Port);
		byte[] byteArray = new byte[256];
		InputStream in = s.getInputStream();
		in.read(byteArray, 0, byteArray.length);
		String input = new String(byteArray);
		input = cutString(input);
		OutputStream out = s.getOutputStream();
		String confirm = "received2";
		out.write(confirm.getBytes());
		out.close();
		in.close();
		s.close();
		// write the results to the next peer
		if (this.getRightPeer().getPeerId() != chairId) {
			writeResults(input, this.getRightPeer().getPort());
		}
		return input;
	}

	/**
	 * This method takes in the output it needs to send to the next peer in the network
	 * @param output
	 * @throws IOException
	 */
	private void writeToSocket(String output, int port) throws IOException {
		Peer chair = Main.getChairPeer();
		if (!this.getRightPeer().equals(chair)) {
			ss.close();
		}
		// TIMER FOR GETTING RESPONSE
		VoteTimerTask timerTask = new VoteTimerTask();
		Timer timer = new Timer(true);
		// COLLECTS VOTE FROM PEER
		State = false;
		GUI gui = new GUI();
		timer.scheduleAtFixedRate(timerTask, 0, 10 * 1000);
		while (!State) {
			State = gui.getState();
			if (State) {
				timer.cancel();
			}
			if (timerTask.isCompleted && !State) {
				V = GUI.Vote.NULL;
				break;
			}
		}
		// IF STATE HAS CHANGED OR VOTE TIMER RUNS OUT
		if (State || timerTask.isCompleted) {
			V = gui.getAnswer();
			String newOutput = updateMessage(output);
			ss = new ServerSocket(port);
			byte[] byteArray2 = new byte[1024];
			while(true) {
				Socket s = ss.accept();
				byte[] byteArray = newOutput.getBytes(StandardCharsets.US_ASCII);
				OutputStream out = s.getOutputStream();
				out.write(byteArray, 0, byteArray.length);
				out.flush();
				InputStream in = s.getInputStream();
				in.read(byteArray2, 0, byteArray2.length);
				String input = new String(byteArray2);
				input = cutString(input);
				out.flush();
				out.close();
				in.close();
				s.close();
				//confirm Peer received message by receiving same message back
				if (input.equalsIgnoreCase("received2")) break;
			}
		}
	}

	/**
	 * This method is called when something is expected over the network
	 * @throws IOException
	 */
	private String readFromSocketTwo() throws IOException {
		int chairId = Main.getChairId();
		Socket s = new Socket(IP, Port);
		byte[] byteArray = new byte[256];
		InputStream in = s.getInputStream();
		in.read(byteArray, 0, byteArray.length);
		String input = new String(byteArray);
		input = cutString(input);
		OutputStream out = s.getOutputStream();
		String confirm = "received2";
		out.write(confirm.getBytes());
		out.close();
		in.close();
		s.close();
		// send message to next peer
		if (PeerId != chairId) {
			writeToSocket(input, this.getRightPeer().getPort());
		}
		return input;
	}

	/**
	 * This method sends the voting file over the network
	 * @param port
	 * @throws IOException
	 */
	private void sendFile(int port) throws IOException {
		//send file to all peers in PeerList
		ss = new ServerSocket(port);
		File myFile = new File("voteQuestion.txt");
		Socket s = ss.accept();
		byte[] byteArray = new byte[(int)myFile.length()];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
		bis.read(byteArray, 0, byteArray.length);
		OutputStream out = s.getOutputStream();
		out.write(byteArray, 0, byteArray.length);
		out.flush();
		bis.close();
		s.close();
	}

	/**
	 * This method is called when a file is expected to be sent through the network
	 * It reads/downloads the file from the chair peer
	 * @return number of bytes read
	 * @throws IOException
	 */
	private void readFileFromSocket() throws IOException {
		Peer chair = Main.getChairPeer();
		Socket s = new Socket(IP, Port);
		byte[] byteArray = new byte[1024];
		InputStream in = s.getInputStream();
		// naming the downloaded file
		File downloadedFile = new File("voteQuestion-downloaded" + PeerId + ".txt");
		FileOutputStream out = new FileOutputStream(downloadedFile);
		BufferedOutputStream bos = new BufferedOutputStream(out);
		int bytesRead = in.read(byteArray, 0, byteArray.length);
		bos.write(byteArray, 0, bytesRead);
		bos.close();
		in.close();
		s.close();
		// send file to next peer
		if (!this.getRightPeer().equals(chair)) {
			sendFile(this.getRightPeer().getPort());
		}
	}

	/**
	 * Method to send the file to all peers
	 * Puts the file in the Chair's output buffer
	 * @throws IOException
	 */
	private void broadcast() throws IOException {
		//send message to all peers in PeerList
		for (int i = 0; i < PeerList.size(); i++) {
			Peer p = PeerList.get(i);
			int port = p.getPort();
			ss = new ServerSocket(port); 
			String output = "There is going to be a vote!";
			byte[] byteArray2 = new byte[1024];
			while(true) {
				Socket s = ss.accept();
				byte[] byteArray = output.getBytes(StandardCharsets.US_ASCII);
				OutputStream out = s.getOutputStream();
				out.write(byteArray, 0, byteArray.length);
				InputStream in = s.getInputStream();
				in.read(byteArray2, 0, byteArray2.length);
				String input = new String(byteArray2);
				input = cutString(input);
				out.flush();
				s.close();
				//confirm Peer received message by receiving same message back
				if (input.equalsIgnoreCase("received")) break;
			}
			ss.close();
		}
	}

	/**
	 * This method is called when something is expected over the network
	 * It reads this information and displays the GUI for the current peer to vote
	 * @throws IOException
	 */
	private String readFromSocketOne() throws IOException {
		Socket s = new Socket(IP, Port);
		byte[] byteArray = new byte[1024];
		InputStream in = s.getInputStream();
		in.read(byteArray, 0, byteArray.length);
		String input = new String(byteArray);
		input = cutString(input);
		// send a message to confirm Peer received it
		OutputStream out = s.getOutputStream();
		String confirm = "received";
		out.write(confirm.getBytes());
		out.close();
		s.close();
		return input;
	}

	/**
	 * Method to join the network in a ring topology
	 */
	private void requestNetworkAccess() {
		Peer chair = Main.getChairPeer();
		if (chair.getRightPeer() == null || chair.getRightPeer().equals(chair)) {
			// no nodes in ring yet
			// create loop between current node and chair node
			chair.setLeftPeer(this);
			chair.setRightPeer(this);
			this.setRightPeer(chair);
			this.setLeftPeer(chair);
		}
		else {
			// nodes already in loop/ring
			// add this node to the end, making chair node its right peer
			Peer p = chair;
			while (p.getRightPeer() != chair) {
				p = p.getRightPeer();
			}
			p.setRightPeer(this);
			this.setLeftPeer(p);
			this.setRightPeer(chair);
			chair.setLeftPeer(this);
		}
	}

	/**
	 * Tabulates the final result based on parameter results
	 * @param results
	 * @return "Quorum was not reached." if number of votes does not equal the number of peers
	 * 		or the number of Aye's is equal to the number of Nay's
	 * @return "Aye!" if number of aye votes is greater than the number of nay votes
	 * @return "Nay!" if number of nay votes is greater than the number of aye votes
	 */
	private String calculateResults(String results) {
		String[] string = results.split(" ");
		int aye = Integer.parseInt(string[1]);
		int nay = Integer.parseInt(string[3]);
		if ((aye + nay) != (PeerList.size() + 1)) {
			return "Quorum was not reached.";
		}
		// tie vote
		if (aye == nay) {
			return "Quorum was not reached.";
		}
		if (aye > nay) {
			return "Aye!";
		} else {
			return "Nay!";
		}
	}

	/**
	 * Waits until all peers have received the Chair's broadcast
	 * @return wait
	 */
	private boolean waitForBroadcast() {
		boolean wait = true;
		for (int i = 0; i < PeerList.size(); i++) {
			wait = wait & PeerList.get(i).getBroadcast();
		}
		return wait;
	}

	/**
	 * Cuts off all unecessary \0 characters in a string
	 * @param string
	 * @return shorter string
	 */
	private String cutString(String string) {
		String result = string.replaceAll("\0", "");
		return result;
	}

	/**
	 * Updates the parameter input based on the current peers' Vote
	 * @param input 
	 * @return output
	 */
	private String updateMessage(String input) {
		String output;
		String[] string = input.split(" ");
		if (V == GUI.Vote.TRUE) { 
			System.out.println();
			int aye = Integer.parseInt(string[1]);
			aye = aye + 1;
			string[1] = String.valueOf(aye);
			output = concatString(string);
		} else if (V == GUI.Vote.FALSE) {
			int nay = Integer.parseInt(string[3]);
			nay = nay + 1;
			string[3] = String.valueOf(nay);
			output = concatString(string);
		} else {
			output = input;
		}
		return output;
	}

	private String concatString(String[] string) {
		String output = "";
		for (int i = 0; i < string.length; i++) {
			output = output + string[i] + " ";
		}
		return output;
	}

	/**
	 * Method to set the current peer's predecessor
	 * @param leftPeer
	 */
	public void setLeftPeer(Peer leftPeer) {
		this.Left = leftPeer;
	}

	/**
	 * Method to set the current peer's successor
	 * @param rightPeer
	 */
	public void setRightPeer(Peer rightPeer) {
		this.Right = rightPeer;
	}

	/**
	 * Accessor for the Left peer
	 * @return leftPeer
	 */
	public Peer getLeftPeer() {
		return this.Left;
	}

	/**
	 * Accessor for the Right peer
	 * @return rightPeer
	 */
	public Peer getRightPeer() {
		return this.Right;
	}

	/**
	 * Accessor for Peer Id
	 * @return PeerId
	 */
	public int getPeerId() {
		return this.PeerId;
	}

	/**
	 * Accessor for Port Number
	 * @return Port
	 */
	public int getPort() {
		return this.Port;
	}

	/**
	 * Accessor for Broadcast boolean
	 * @return Broadcast
	 */
	public boolean getBroadcast() {
		return this.Broadcast;
	}
	
	/**
	 * Method to print the Ring
	 */
	public void showRing() {
		Peer chair = Main.getChairPeer();
		String show = "Ring Topology (just for proof of ring):\n";
		show = show + chair.getPeerId() + " --> ";
		Peer p = chair.getRightPeer();
		while (p != chair) {
			show = show + p.getPeerId() + " --> ";
			p = p.getRightPeer();
		}
		show = show + chair.getPeerId();
		System.out.println(show);
	}

	/**
	 * This class implements the voting timer.
	 * @author Leah
	 *
	 */
	public class VoteTimerTask extends TimerTask {

		public boolean isCompleted = false;
		
		@Override
		public void run() {
			try {
				Thread.sleep(10000);
				isCompleted = true;
				System.out.println("Timed out!");
				this.cancel();
			} catch (InterruptedException e) {
			}	
		}
	}
}