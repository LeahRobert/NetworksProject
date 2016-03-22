import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

/**
 * The main class for the program
 * @author Leah
 *
 * N: Number of peers in the network
 * C: Chair ID of the Chair Peer
 * peerList: list of Peers (excluding the chair)
 * Localhost: host of the local IP
 * chair: Peer object for the Chair Peer
 */

public class Main {

	private static int N;
	private static int C;
	private static ArrayList<Peer> peerList;
	private static java.net.InetAddress Localhost;
	private static Peer chair;

	/**
	 * Method to start the program
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// get arguments from command line/Eclipse run configurations
		String nText = args[0];
		String cText = args[1];

		// set global variables
		N = Integer.parseInt(nText);
		C = Integer.parseInt(cText);
		Localhost = java.net.InetAddress.getLocalHost();
		
		System.out.println("Number of peers: " + N + " Chair Id: " + C);	// ------ TAKE THIS LINE OUT LATER
		System.out.println("--------------------------------------------------");

		peerList = new ArrayList<Peer>();
		ArrayList<Integer> portList = getAvailablePorts();

		// create peer with Chair Id, add to list
		chair = new Peer(C, Localhost, portList.get(0));
		Thread t = new Thread(chair);
		t.start();

		// create all other peers
		for (int i = 1; i < N; i++) {
			if (i == C) {
				int j = N + 1;
				createPeers(j, portList.get(i));
			} else {
				createPeers(i, portList.get(i));
			}
		}
	}

	/**
	 * Creates a Peer object given an id and a port number
	 * @param id
	 * @param port
	 * @throws IOException
	 */
	private static void createPeers(int id, int port) throws IOException {
		Peer p = new Peer(id, Localhost, port);
		peerList.add(p);
		Thread t = new Thread(p);
		t.start();
	}

	/**
	 * Method to get the Chair Peer object
	 * @return Chair Peer object
	 */
	public static Peer getChairPeer() {
		return chair;
	}

	/**
	 * Method to get the Chair Id
	 * @return Chair Id
	 */
	public static int getChairId() {
		return C;
	}

	/**
	 * Method to get the list of all peers in the network (at the start)
	 * @return list of peers
	 */
	public static ArrayList<Peer> getPeerList() {
		return peerList;
	}

	/**
	 * Method to get available port numbers
	 * @return list of available ports
	 * @throws IOException
	 */
	private static ArrayList<Integer> getAvailablePorts() throws IOException {
		ArrayList<Integer> portList = new ArrayList<Integer>();
		for (int i = 0; i < N; i++) {
			ServerSocket s = new ServerSocket(0);
			portList.add(s.getLocalPort());
			System.out.println(s.getLocalPort());
			s.close();
		}
		return portList;
	}

}
