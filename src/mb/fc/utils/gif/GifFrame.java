package mb.fc.utils.gif;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class GifFrame extends JFrame implements ActionListener, ChangeListener {
	private enum AnimationType {
		SPELL,
		NPC,
		COMBATANT
	}
	
	private static final long serialVersionUID = 1L;

	private GifDecoder battleGifDecoder = new GifDecoder();
	private GifDecoder walkGifDecoder = new GifDecoder();
	private GifDecoder portraitDecoder = new GifDecoder();
	private boolean hasPortrait = false;
	private ImagePanel imagePanel = new ImagePanel();
	private Hashtable<String, Integer> battleActions = new Hashtable<>();
	private JButton leftButton;
	private JButton rightButton;
	private JButton playButton;
	private int currentIndex = 0;
	private JLabel topLabel;
	private JSpinner xOffsetSpinner;
	private JSpinner yOffsetSpinner;
	private JTabbedPane tabbedPane;
	private PortraitPanel portraitPanel;
	private boolean initialized = false;
	private String imageName;
	private JFileChooser fc = new JFileChooser();
	private JMenuItem exportMI = null;
	private JPanel combatantPanel, spellPanel;
	private AnimationType type = null;
	private JPanel backPanel = null;
	public static Color COMBAT_TRANS;
	public static Color WALK_TRANS;

  private JCheckBox promotedCheckbox;
  
	public GifFrame(boolean embedded) {
		super("Super Ugly Animator DEV 1.26");
		this.setupUI(embedded);

		// loadImages();
	}
	
	private File loadGifFromFC(String description, boolean allowNone) {		
		fc.setFileFilter(new FileNameExtensionFilter("Gif containing " + description, new String[] { "gif" }));
		fc.setDialogTitle("Select " + description + " gif");
		fc.setSelectedFile(null);
		fc.showOpenDialog(this);		
		File file = fc.getSelectedFile();
		if ((!allowNone && file == null) || (file != null && !file.getName().endsWith(".gif"))) {
			JOptionPane.showMessageDialog(this, "ERROR: Selected file must be a .gif");
			return null;
		}
		
		return file;
	}

	private void loadImages(boolean battle, boolean walk, boolean portrait) {
		battleGifDecoder = new GifDecoder();
		walkGifDecoder = new GifDecoder();
		portraitDecoder = new GifDecoder();

		this.initialized = false;
		exportMI.setEnabled(false);
		
		File battleFile = null;
		File walkFile = null;
		File portraitFile = null;
		backPanel.remove(combatantPanel);
		backPanel.remove(spellPanel);
		
		if (battle) {
			battleFile = loadGifFromFC("Battle Animations", false);
			if (battleFile == null)
				return;
		}
		if (walk) {
			walkFile = loadGifFromFC("Walking Animations", false);
			if (walkFile == null)
				return;
		}
		if (portrait)
			portraitFile = loadGifFromFC("Portrait Animations", true);

		if (portraitFile != null) {
			portraitDecoder.read(portraitFile.getPath());
			portraitPanel.setPortraitDecoder(portraitDecoder);
			tabbedPane.setEnabledAt(1, true);
			hasPortrait = true;
		} else {
			tabbedPane.setEnabledAt(1, false);
			hasPortrait = false;
		}
		
		battleActions.clear();
		currentIndex = 0;
		topLabel.setText(" Current Frame Animation: Unassigned");
		
		if (walk)
			loadWalkGif(walkFile.getPath());
		
		// If all we have is walk animations then just export directly
		if (walk && !battle && portraitFile == null) {
			tabbedPane.setEnabledAt(0, false);	
			int rc = JOptionPane.showConfirmDialog(this, "This NPC has no portrait so there is nothing to modify.\nWould you like to export it now?", 
					"Export NPC", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null);
			if (rc == JOptionPane.YES_OPTION)
				AnimationExporter.exportNPC(walkGifDecoder, imageName, portraitDecoder, false, portraitPanel);
			
			return;
		} else {
			tabbedPane.setEnabledAt(0, true);
			if (battle) {
				loadBattleGif(battleFile.getPath());
				this.initialized = true;
			}			
			
			if (type == AnimationType.COMBATANT) {
				combatantPanel = setupCombatantPanel();
				backPanel.add(combatantPanel, BorderLayout.LINE_START);
			}
			else if (type == AnimationType.SPELL) {
				spellPanel = setupSpellPanel();
				backPanel.add(spellPanel, BorderLayout.LINE_START);
			}
			exportMI.setEnabled(true);
		}
		
		this.repaint();
	}
	
	private JMenuItem addMenuItem(String desc, String cmd, JMenu fileMenu) {
		JMenuItem mi = new JMenuItem(desc);
		mi.setActionCommand(cmd);
		mi.addActionListener(this);
		fileMenu.add(mi);
		return mi;
	}

	private void setupUI(boolean embedded) {
		JMenuBar menuBar = new JMenuBar();		
		
		JMenu fileMenu = new JMenu("File");
		addMenuItem("Import Combatant", "Import Combatant", fileMenu);
		addMenuItem("Import Spell", "Import Spell", fileMenu);
		addMenuItem("Import NPC", "Import NPC", fileMenu);
		fileMenu.addSeparator();
		addMenuItem("Load Combatant", "Load Combatant", fileMenu);
		addMenuItem("Load Spell", "Load Spell", fileMenu);
		addMenuItem("Load NPC", "Load NPC", fileMenu);
		fileMenu.addSeparator();
		exportMI = addMenuItem("Export", "Export", fileMenu);
		exportMI.setEnabled(false);
		menuBar.add(fileMenu);
		this.setJMenuBar(menuBar);
		
		tabbedPane = new JTabbedPane();
		backPanel = new JPanel(new BorderLayout());

		topLabel = new JLabel(" Current Frame Animation: Unassigned");
		topLabel.setForeground(Color.white);
		topLabel.setPreferredSize(new Dimension(0, 30));
		topLabel.setBackground(Color.DARK_GRAY);
		topLabel.setOpaque(true);

		xOffsetSpinner = new JSpinner();
		xOffsetSpinner.setPreferredSize(new Dimension(200, 30));
		xOffsetSpinner.setMaximumSize(new Dimension(150, 30));
		xOffsetSpinner.addChangeListener(this);
		xOffsetSpinner.setModel(new SpinnerNumberModel(0, -400, 400, 4));
		xOffsetSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
		yOffsetSpinner = new JSpinner();
		yOffsetSpinner.setPreferredSize(new Dimension(200, 30));
		yOffsetSpinner.setMaximumSize(new Dimension(150, 30));
		yOffsetSpinner.addChangeListener(this);
		yOffsetSpinner.setModel(new SpinnerNumberModel(0, -400, 400, 4));
		yOffsetSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		
	    promotedCheckbox = new JCheckBox("Is promoted");
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(Color.DARK_GRAY);
		leftButton = createActionButton("< --", "left", bottomPanel);
		rightButton = createActionButton("-- >", "right", bottomPanel);
		leftButton.setEnabled(false);
		rightButton.setEnabled(false);
		
	
		// backPanel.add(sidePanel, BorderLayout.LINE_START);
		backPanel.add(imagePanel, BorderLayout.CENTER);
		backPanel.add(bottomPanel, BorderLayout.PAGE_END);
		backPanel.add(topLabel, BorderLayout.PAGE_START);
		tabbedPane.add(backPanel);
		tabbedPane.setTitleAt(0, "Animation Definitions");

		portraitPanel = new PortraitPanel();
		tabbedPane.add(portraitPanel);
		tabbedPane.setTitleAt(1, "Portrait Definitions");
		tabbedPane.setEnabledAt(1, false);
		this.setContentPane(tabbedPane);

		spellPanel = setupSpellPanel();
		combatantPanel = setupCombatantPanel();
		
		if (embedded)
			this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		else
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setMinimumSize(new Dimension(500, 675));
		this.pack();
		if (!embedded)
			this.setVisible(true);
	}
	
	private JPanel setupSpellPanel() {
		JPanel sidePanel = new JPanel();
		sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
		sidePanel.setPreferredSize(new Dimension(150, 0));
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
		createActionButton("Set Level 1", "1", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Level 2", "2", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Level 3", "3", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Level 4", "4", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
	    createActionButton("Set Custom", "Custom", sidePanel);
	    sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
	    playButton = createActionButton("Play Action", "play");
		playButton.setEnabled(false);
	    sidePanel.add(playButton);
		sidePanel.add(Box.createGlue());
	
		return sidePanel;
	}
	
	private JPanel setupCombatantPanel() {
		JPanel sidePanel = new JPanel();
		sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
		sidePanel.setPreferredSize(new Dimension(150, 0));
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
		createActionButton("Set Stand", "Stand", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Attack", "Attack", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Winddown", "Winddown", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Dodge", "Dodge", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set DodgeWinddown", "DodgeWinddown", sidePanel);
    	sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Spell", "Spell", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Item", "Item", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Special", "Special", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
	    createActionButton("Set Custom", "Custom", sidePanel);
	    sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
	    playButton = createActionButton("Play Action", "play");
		playButton.setEnabled(false);
	    sidePanel.add(playButton);	
	    sidePanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    sidePanel.add(promotedCheckbox);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));

		sidePanel.add(new JLabel("X coordinate offset"));
		sidePanel.add(xOffsetSpinner);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		sidePanel.add(new JLabel("Y coordinate offset"));
		sidePanel.add(yOffsetSpinner);
		sidePanel.add(Box.createGlue());
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		return sidePanel;
	}

	public void loadBattleGif(String file) {
		currentIndex = 0;
		leftButton.setEnabled(false);
		File fileToLoad = new File(file);
		if (currentIndex + 1 != battleGifDecoder.getFrameCount())
			rightButton.setEnabled(true);
		try {
			battleGifDecoder.read(new FileInputStream(file));
			this.COMBAT_TRANS = new Color(battleGifDecoder.getImage().getRGB(0, 0));
			imageName = file;
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occurred loading the specified file. " + e.getMessage());
			return;
		}
		imagePanel.setCurrentImage(battleGifDecoder.getFrame(0));
	}

	public void loadWalkGif(String file) {
		try {
			walkGifDecoder.read(new FileInputStream(new File(file)));
			WALK_TRANS = new Color(walkGifDecoder.getImage().getRGB(0, 0));
			// This gets clobbered by battle image load for combatants
			imageName = file;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occurred loading the specified file. " + e.getMessage());
		}
	}

	public static void main(String args[]) {
		new GifFrame(false);
	}

	class ImagePanel extends JPanel {
		private static final long serialVersionUID = 1L;

		private BufferedImage currentImage;

		public ImagePanel() {
			this.setPreferredSize(new Dimension(200, 300));
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (initialized) {
				g.setColor(Color.white);
				g.fillRect(0, 0, this.getWidth(), this.getHeight());

				int drawX = this.getWidth() / 5; // (this.getWidth() - currentImage.getWidth()) / 2;
				int drawY = this.getHeight() / 2;

				int halfWidth = currentImage.getWidth() / 2;
				int halfHeight = currentImage.getHeight() / 2;

				if (currentImage != null) {
					g.drawImage(AnimationExporter.transformColorToTransparency(currentImage, COMBAT_TRANS), 
							drawX - halfWidth + (int) xOffsetSpinner.getValue(),
							drawY - halfHeight + (int) yOffsetSpinner.getValue(), this);
				}

				g.setColor(Color.red);
				g.drawRect(drawX + 20, drawY - 48, 40, 40);
				g.drawRect(drawX + 196, drawY - 48, 40, 40);

				// g.drawRect(drawX + 18, this.getHeight() / 2, 40, 40);

				g.setColor(Color.blue);

				g.drawLine(drawX + 196, drawY, drawX + 236, drawY);
				// g.drawRect(this.getWidth() / 2 - 25, this.getHeight() / 2, 50, 50);
			}
		}

		public void setCurrentImage(BufferedImage currentImage) {
			this.currentImage = currentImage;
			this.repaint();
		}
	}
	
	private JButton createActionButton(String text, String command) {
		JButton b = new JButton(text);
		b.addActionListener(this);
		b.setActionCommand(command);
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		b.setMaximumSize(new Dimension(150, 20));
		return b;
	}

	private JButton createActionButton(String text, String command, JPanel container) {
		JButton b = createActionButton(text, command);
		container.add(b);
		return b;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();
		if (cmd.equalsIgnoreCase("left")) {
			imagePanel.setCurrentImage(battleGifDecoder.getFrame(--currentIndex));
			if (currentIndex == 0)
				leftButton.setEnabled(false);
			rightButton.setEnabled(true);

			topLabel.setText(" Current Frame Animation: Unassigned");
			playButton.setEnabled(false);
			for (Map.Entry<String, Integer> s : battleActions.entrySet()) {
				if (s.getValue() == currentIndex) {
					playButton.setEnabled(true);
					topLabel.setText(" Current Frame Animation: " + s.getKey());
					break;
				}
			}
		} else if (cmd.equalsIgnoreCase("right")) {
			imagePanel.setCurrentImage(battleGifDecoder.getFrame(++currentIndex));
			if (currentIndex + 1 == battleGifDecoder.getFrameCount())
				rightButton.setEnabled(false);
			leftButton.setEnabled(true);

			topLabel.setText(" Current Frame Animation: Unassigned");
			playButton.setEnabled(false);
			for (Map.Entry<String, Integer> s : battleActions.entrySet()) {
				if (s.getValue() == currentIndex) {
					topLabel.setText(" Current Frame Animation: " + s.getKey());
					playButton.setEnabled(true);
					break;
				}
			}
		} else if (cmd.equalsIgnoreCase("play")) {
			Thread t = new Thread(new PlayThread());
			t.start();
		} else if (cmd.startsWith("Export")) {
			switch (type) {
			case COMBATANT:
				AnimationExporter.exportCombatant(battleGifDecoder, walkGifDecoder, portraitDecoder, hasPortrait, 
				          imageName, portraitPanel, battleActions, xOffsetSpinner, yOffsetSpinner, promotedCheckbox.isSelected());
				break;
			case NPC:
				AnimationExporter.exportNPC(walkGifDecoder, imageName, portraitDecoder, hasPortrait, portraitPanel);
				break;
			case SPELL:
				AnimationExporter.exportSpell(battleGifDecoder, imageName, battleActions, xOffsetSpinner, yOffsetSpinner);
				break;
			default:
				break;
				
			}
			
		} else if (cmd.equalsIgnoreCase("Import Combatant")) {
			type = AnimationType.COMBATANT;
			loadImages(true, true, true);
	    } else if (cmd.equalsIgnoreCase("Import NPC")) {
	    	type = AnimationType.NPC;
			loadImages(false, true, true);
	    } else if (cmd.equalsIgnoreCase("Import Spell")) {
	    	type = AnimationType.SPELL;
			loadImages(true, false, false);
	    }else if (cmd.equalsIgnoreCase("Custom")) {
	      String animName = JOptionPane.showInputDialog(this, "Enter the name of the animation");
	      if (animName != null) {
	        battleActions.put(animName, Integer.valueOf(currentIndex));
	        topLabel.setText(" Current Frame Animation: " + animName);
	        playButton.setEnabled(true);
	      } 
		} else {
			battleActions.put(cmd, currentIndex);
			topLabel.setText(" Current Frame Animation: " + cmd);
			playButton.setEnabled(true);
		}
	}

	class PlayThread implements Runnable {
		@Override
		public void run() {
			outer: while (true) {
				try {
					Thread.sleep(battleGifDecoder.getDelay(currentIndex));
				} catch (InterruptedException e) {
				}

				if (currentIndex + 1 == battleGifDecoder.getFrameCount())
					break;

				for (Map.Entry<String, Integer> s : battleActions.entrySet()) {
					if (s.getValue() == currentIndex + 1) {
						break outer;
					}
				}

				actionPerformed(new ActionEvent(this, 0, "right"));
			}
		}

	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		this.repaint();
	}
}
