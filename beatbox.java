//beatbox now connects to server
//server and beatbox have all streams they need
//sends messages and arrays to server and server receives them
//server now starts a new thread to distibute the message and AL to all clients
//beatbox now has a thread that constantly looks for new incoming messages and ALs from server
//beatbox JList has a listener so that when a message is clicked, the beat that was received with it is displayed

//NEED to flush printWriter!!! But don't flush oos.

import java.awt.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.event.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class beatbox {

    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList;
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
	JTextArea userMessage;
	DefaultListModel<String> clientMessageListModel = new DefaultListModel<String>();
	JList<String> clientMessageJList = new JList<String>(clientMessageListModel);
	JButton sendIt;
	PrintWriter writer;
	ObjectOutputStream oos;
	BufferedReader reader;
	ObjectInputStream ois;
	ArrayList<ArrayList<JCheckBox>> beatsReceived = new ArrayList<ArrayList<JCheckBox>>();

    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", 
       "Open Hi-Hat","Acoustic Snare", "Crash Cymbal", "Hand Clap", 
       "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", 
       "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", 
       "Open Hi Conga"};
    int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
    

    public static void main (String[] args) {
        new beatbox().buildGUI();
    }
  
    public void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkboxList = new ArrayList<JCheckBox>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);         
          
        JButton stop = new JButton("Stop");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

	JButton random = new JButton("random beat");
	random.addActionListener(new MyRandomListener());
        buttonBox.add(random);

	JButton clearAll = new JButton("Clear all");
	clearAll.addActionListener(new MyClearAllListener());
	buttonBox.add(clearAll);

	JButton serialize = new JButton("Save this beat");
	serialize.addActionListener(new serializeListener());
	buttonBox.add(serialize);

	JButton load = new JButton("Load");
	load.addActionListener(new loadListener());
	buttonBox.add(load);

	sendIt = new JButton("Send it");
	sendIt.addActionListener(new sendListener());
	buttonBox.add(sendIt);

//	JLabel userMessageLabel = new JLabel("Insert your message below:");
//	buttonBox.add(userMessageLabel);

	userMessage = new JTextArea(5,10);
	userMessage.setLineWrap(true);
	userMessage.setWrapStyleWord(true);
	
	buttonBox.add(userMessage);

//	JLabel otherUsersMessageLabel = new JLabel("Messages from other users will be shown below:");
//	buttonBox.add(otherUsersMessageLabel);

	clientMessageJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	clientMessageJList.addMouseListener(new myListMouseListener());

	JScrollPane scroller = new JScrollPane(clientMessageJList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	buttonBox.add(scroller);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
           nameBox.add(new Label(instrumentNames[i]));
        }
        
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);
          
        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i = 0; i < 256; i++) {                    
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);            
        } // end loop

	setUpNetworking();

        setUpMidi();

        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    } // close method

    public void setUpMidi() {
      try {
        sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequence = new Sequence(Sequence.PPQ,4);
        track = sequence.createTrack();
        sequencer.setTempoInBPM(120);
        
      } catch(Exception e) {e.printStackTrace();}
    } // close method

    public void buildTrackAndStart() {
      int[] trackList = null;
    
      sequence.deleteTrack(track);
      track = sequence.createTrack();

        for (int i = 0; i < 16; i++) {
          trackList = new int[16];
 
          int key = instruments[i];   

          for (int j = 0; j < 16; j++ ) {         
              JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
              if ( jc.isSelected()) {
                 trackList[j] = key;
              } else {
                 trackList[j] = 0;
              }                    
           } // close inner loop
         
           makeTracks(trackList);
           track.add(makeEvent(176,1,127,0,16));  
       } // close outer

       track.add(makeEvent(192,9,1,0,15));      
       try {
           sequencer.setSequence(sequence); 
	     sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);                   
           sequencer.start();
           sequencer.setTempoInBPM(120);
       } catch(Exception e) {e.printStackTrace();}
    } // close buildTrackAndStart method







	public void setUpNetworking() {
		try {
			Socket clientSocket = new Socket("127.0.0.1", 4998);
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			writer = new PrintWriter(clientSocket.getOutputStream());
			InputStreamReader ISR = new InputStreamReader(clientSocket.getInputStream());
			reader = new BufferedReader(ISR);
			ois = new ObjectInputStream(clientSocket.getInputStream());

			Thread receiver = new Thread(new receiver());
			receiver.start();

		} catch (Exception ex) {
			System.out.println("Connecting to server failed");
			ex.printStackTrace();
		}
		System.out.println("Connections established");
	}


	class receiver implements Runnable {
		public void run() {
			String incomingMessage;
			try {
				while((incomingMessage = reader.readLine()) != null) {
					clientMessageListModel.addElement(incomingMessage);
					System.out.println("Message received: " + incomingMessage);

					ArrayList<JCheckBox> receivedAL = (ArrayList<JCheckBox>) ois.readObject();
					System.out.println("AL received from server: " + receivedAL.get(0).isSelected() + " " + receivedAL.get(1).isSelected() + " " + receivedAL.get(2).isSelected() );
					beatsReceived.add(receivedAL);
					System.out.println("receivedAL added to beatsReceived. beatsReceived size = " + beatsReceived.size());
				}
			} catch(Exception e) {
				System.out.println("Reading incoming message and AL from server failed");
				e.printStackTrace();
			}
		}
	}

	class myListMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			System.out.println("mouse clicked, inside JList? Or anywhere on screen??");
			int index = clientMessageJList.getSelectedIndex();
			ArrayList<JCheckBox> selectedAL = beatsReceived.get(index);
			for(int i = 0; i < 256; i++) {
				checkboxList.get(i).setSelected(false);
				if(selectedAL.get(i).isSelected()) {
					checkboxList.get(i).setSelected(true);
				}
			}
		}
	}



	class sendListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String message = userMessage.getText();
			writer.println(message);
			writer.flush();										//Really need this! It's really important
			System.out.println("Message successfully sent to server: " + message);
			userMessage.setText("");

			if(checkboxList instanceof ArrayList) {
				try {
					oos.reset();
					oos.writeObject(checkboxList);
					System.out.println("Arraylist successfully sent to server " + checkboxList.get(0).isSelected() + " " + checkboxList.get(1).isSelected() + " " + checkboxList.get(2).isSelected());
// oos.flush();	Put this in here when trying to solve a glitch but fixed it another way and this isn't necessary
				} catch(Exception ex) {
					System.out.println("Writing AL to output stream failed");
					ex.printStackTrace();
				}
			}
		}
	}





           
    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    } // close inner class

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    } // close inner class

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
	      float tempoFactor = sequencer.getTempoFactor(); 
            sequencer.setTempoFactor((float)(tempoFactor * 1.03));
        }
     } // close inner class

     public class MyDownTempoListener implements ActionListener {
         public void actionPerformed(ActionEvent a) {
	      float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float)(tempoFactor * .97));
        }
    } // close inner class

	public class MyClearAllListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			for (int i = 0; i < 256; i++) {
				checkboxList.get(i).setSelected(false);
			}
		}            
        } // close inner class

	public class MyRandomListener implements ActionListener {
		public void actionPerformed(ActionEvent a) {
			for (int i = 0; i < 256; i++) {
				checkboxList.get(i).setSelected(false);
			}
			int numberOfTicks = (((int) (Math.random() * 255))+1);
			for (int i = 0; i < numberOfTicks; i++) {
				int random = (int)(Math.random() * 256);
				checkboxList.get(random).setSelected(true);
			}
		}            
        } // close inner class

	public class serializeListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {

			JFileChooser saver = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter("ser files","ser");
			saver.setFileFilter(filter);
			int returnValue = saver.showSaveDialog(mainPanel);
			if(returnValue == JFileChooser.APPROVE_OPTION) {
				boolean [] checkSaver = new boolean[256];
				for(int i=0; i<256; i++) {
					boolean selected = checkboxList.get(i).isSelected();
					checkSaver[i] = selected;
				}
				try {
					FileOutputStream fos = new FileOutputStream(saver.getSelectedFile());
					ObjectOutputStream oos = new ObjectOutputStream(fos);
					oos.writeObject(checkSaver);
					oos.close();
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public class loadListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			boolean [] booleanArray = null;
			JFileChooser chooser = new JFileChooser("C:/Users/Wes/Desktop");
			int returnValue = chooser.showOpenDialog(mainPanel);
			if(returnValue == JFileChooser.APPROVE_OPTION) {
				try {
					FileInputStream fis = new FileInputStream(chooser.getSelectedFile());
					ObjectInputStream ois = new ObjectInputStream(fis);
					booleanArray = ( boolean [] )ois.readObject();
					ois.close();
				} catch(Exception ex) {
					ex.printStackTrace();
				}
				for(int i = 0; i < 256; i++) {
					checkboxList.get(i).setSelected(false);
					if(booleanArray[i] == true) {
						checkboxList.get(i).setSelected(true);
					}
				}
			}
		}
	}

    public void makeTracks(int[] list) {        
       
       for (int i = 0; i < 16; i++) {
          int key = list[i];

          if (key != 0) {
             track.add(makeEvent(144,9,key, 100, i));
             track.add(makeEvent(128,9,key, 100, i+1));
          }
       }
    }
        
    public  MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);

        } catch(Exception e) {e.printStackTrace(); }
        return event;
    }

} // close class