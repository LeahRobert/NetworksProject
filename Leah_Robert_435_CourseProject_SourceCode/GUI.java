
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * This class is strictly for the GUI.
 * @author Leah
 * 
 * state: denotes if the Vote button has been pressed
 * state: true if Vote button has been pressed or close window has been pressed, false otherwise
 * AYE: true if vote was Aye
 * NAY: true if vote was Nay
 * V: TRUE if vote was Aye, FALSE if vote was Nay, NULL if window was closed
 */

public class GUI {

	private volatile boolean state = false;
	private boolean AYE = false;
	private boolean NAY = false;
	private JButton VoteButton;
	private JLabel Info;
	private Vote V;
	
	public enum Vote {
	    TRUE, FALSE, NULL 
	}
	
	/**
	 * Constructor for the GUI class
	 */
	public GUI() {
		// schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 * Creates and shows the GUI
	 */
	private void createAndShowGUI() {
		// create and set up the window
		JFrame frame = new JFrame("CISC 435 Course Project");
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                state = true;
                e.getWindow().dispose();
            }
        });
		frame.setLocationRelativeTo(null);
		frame.setLayout(new BorderLayout());

		// setting up all the panels in the frame
		JPanel topPanel = new JPanel(new BorderLayout());
		JPanel radioPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel(new BorderLayout());

		JLabel label = new JLabel("Please vote by clicking on Aye or Nay, then 'Vote'. ");
		JLabel message = new JLabel (" ");
		Info = new JLabel("  ");
		Info.setForeground(Color.red);

		topPanel.add(label, BorderLayout.NORTH);
		topPanel.add(message, BorderLayout.CENTER);
		topPanel.add(Info, BorderLayout.SOUTH);

		// radio buttons
		JRadioButton ayeButton = new JRadioButton("Aye! ");
		JRadioButton nayButton = new JRadioButton("Nay! ");
		ayeButton.addActionListener(new ActionListener() {
			// method to close when button clicked
			public void actionPerformed(ActionEvent e) {
				ayeButton.setSelected(true);
				nayButton.setSelected(false);
				AYE = true;
				NAY = false;
			}
		});
		nayButton.addActionListener(new ActionListener() {
			// method to close when button clicked
			public void actionPerformed(ActionEvent e) {
				nayButton.setSelected(true);
				ayeButton.setSelected(false);
				NAY = true;
				AYE = false;
			}
		});
		radioPanel.add(ayeButton, BorderLayout.NORTH);
		radioPanel.add(nayButton, BorderLayout.SOUTH);

		// vote button
		VoteButton = new JButton("VOTE");
		VoteButton.addActionListener(new ActionListener() {
			// method to send answer when button clicked
			public void actionPerformed(ActionEvent e) {
				if (AYE == false && NAY == false) {
					Info.setText("Please select your vote to proceed. "); 
				}
				else {
					VoteButton.setEnabled(false);
					setVote();
					state = true;
					frame.dispose();
				}
			}
		});

		buttonPanel.add(VoteButton, BorderLayout.NORTH);

		// add all components to frame
		frame.add(topPanel, BorderLayout.NORTH);
		frame.add(radioPanel, BorderLayout.CENTER);
		frame.add(buttonPanel, BorderLayout.SOUTH);
		//Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * This method sets the value of Vote based on the user's input to the GUI (or lack thereof)
	 */
	public void setVote() {
		if (AYE) {
			V = Vote.TRUE;
		}
		else if (NAY) {
			V = Vote.FALSE;
		} 
		else {
			V = Vote.NULL;
		}
	}
	
	/**
	 * Accessor for the answer
	 * @return V
	 */
	public Vote getAnswer() {
		return V;
	}
	
	/**
	 * Accessor for the state of the GUI (voted on or not)
	 * @return state
	 */
	public boolean getState() {
		return state;
	}
	
	public JButton getVoteButton() {
		return VoteButton;
	}
	
	public JLabel getInfoLabel() {
		return Info;
	}
	
	public void setInfoLabel(String msg) {
		Info.setText(msg); 
	}
	
	/**
	 * Enum class for Vote
	 * @author Leah
	 *
	 */
	public class Enum {
	    Vote v;
	    /**
	     * Enum constructor
	     * @param v
	     */
	    public Enum(Vote v) {
	        this.v = v;
	    }
	}
}


