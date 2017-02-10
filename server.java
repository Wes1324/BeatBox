import java.net.*;
import java.io.*;
import java.util.ArrayList;
import javax.swing.JCheckBox;

public class server {

	ArrayList<ObjectOutputStream> ooStreams = new ArrayList<ObjectOutputStream>();
	ArrayList<PrintWriter> printWriters = new ArrayList<PrintWriter>();

	public static void main(String [] args) {
		new server().go();
	}

	public void go() {
		try {
			ServerSocket serverSock = new ServerSocket(4998);
			while(true) {
				Socket clientSock = serverSock.accept();
				ObjectOutputStream oos = new ObjectOutputStream(clientSock.getOutputStream());
				ooStreams.add(oos);
				PrintWriter writer = new PrintWriter(clientSock.getOutputStream()); //CAN MAKE PRINTWRITERS AUTO FLUSH HERE IF WANT
				printWriters.add(writer);
				Thread clientCheckerThread = new Thread(new clientCheckerClass(clientSock));
				clientCheckerThread.start();
				System.out.println("A client has been added");
			}
		} catch(Exception ex) {
			System.out.println("receiving client failed");
			ex.printStackTrace();
		}
	}

	class clientCheckerClass implements Runnable {

		BufferedReader clientBReader;
		ObjectInputStream clientOIS;

		public clientCheckerClass(Socket clientSocket) {
			try {
				InputStreamReader clientISR = new InputStreamReader(clientSocket.getInputStream());
				clientBReader = new BufferedReader(clientISR);
				clientOIS = new ObjectInputStream(clientSocket.getInputStream());
			} catch (Exception e) {
				System.out.println("Creating client input streams failed");
				e.printStackTrace();
			}
		}

		public void run() {
			int loop = 0;
			String message;
			ArrayList<JCheckBox> receivedCheckboxList;

			try {
				while((message = clientBReader.readLine()) != null) {
					loop++;
					System.out.println("Loop number: " + loop + ". MESSAGE RECEIVED: " + message);

					receivedCheckboxList = (ArrayList<JCheckBox>) clientOIS.readObject();
					System.out.println("ARRAYLIST RECEIVED: " + receivedCheckboxList.get(0).isSelected() + " " + receivedCheckboxList.get(1).isSelected() + " " + receivedCheckboxList.get(2).isSelected());
					System.out.println("AL size = " + receivedCheckboxList.size());

					Thread distributor = new Thread(new distribute(message, receivedCheckboxList));
					distributor.start();
					System.out.println("distributor thread started in server");
				}
			} catch (Exception e) {
				System.out.println("Reading incoming messages from client failed");
				e.printStackTrace();
			}
		}
	}

	class distribute implements Runnable {

		String message2send;
		ArrayList<JCheckBox> AL2send;

		public distribute(String messageToSend, ArrayList<JCheckBox> ALToSend) {
			message2send = messageToSend;
			AL2send = ALToSend;
		}

		public void run() {
			for(int i = 0; i < printWriters.size(); i++) {
				printWriters.get(i).println(message2send);
				printWriters.get(i).flush();
			}

			for(int i = 0; i < ooStreams.size(); i++) {
				try {
					if(AL2send instanceof ArrayList) {
						ooStreams.get(i).writeObject(AL2send);
					}
				} catch (Exception e) {
					System.out.println("Writing AL to client failed");
					e.printStackTrace();
				}
			}

			System.out.println("server distributor loops done");

		}
	}
}				