package mb.fc.utils.gif;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GifFrame extends JFrame implements ActionListener, ChangeListener
{
	private static final long serialVersionUID = 1L;

	private GifDecoder battleGifDecoder = new GifDecoder();
	private GifDecoder walkGifDecoder = new GifDecoder();
	private GifDecoder portraitDecoder = new GifDecoder();
	private boolean hasPortrait = false;
	private ImagePanel imagePanel = new ImagePanel();
	private Hashtable<String, Integer> battleActions = new Hashtable<String, Integer>();
	private JButton leftButton;
	private JButton rightButton;
	private JButton playButton;
	private int currentIndex = 0;
	private JLabel topLabel;
	public static final Color TRANS = new Color(95, 134, 134);
	private JSpinner xOffsetSpinner;
	private JSpinner yOffsetSpinner;
	private JTabbedPane tabbedPane;
	private PortraitPanel portraitPanel;
	private boolean initialized = false;
	private String imageName;

	public GifFrame(boolean embedded)
	{
		super("Super Ugly Animator DEV 1.25");
		this.setupUI(embedded);

		// loadImages();
	}

	private void loadImages()
	{
		battleGifDecoder = new GifDecoder();
		walkGifDecoder = new GifDecoder();
		portraitDecoder = new GifDecoder();
		
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Select Battle Animation Gif");
		fc.showOpenDialog(this);
		File battleFile = fc.getSelectedFile();
		if (battleFile == null || !battleFile.getName().endsWith(".gif"))
		{
			JOptionPane.showMessageDialog(this, "ERROR: Selected file must be a .gif");
			return;
		}
		fc.setDialogTitle("Select Walking Animation Gif");
		fc.showOpenDialog(this);
		File walkFile = fc.getSelectedFile();
		if (walkFile == null || !walkFile.getName().endsWith(".gif"))
		{
			JOptionPane.showMessageDialog(this, "ERROR: Selected file must be a .gif");
			return;
		}

		fc.setDialogTitle("Select Portrait Animation Gif");
		int rc = fc.showOpenDialog(this);
		File portraitFile = fc.getSelectedFile();
		if (rc == JOptionPane.OK_OPTION && portraitFile != null && portraitFile.getName().endsWith(".gif"))
		{
			portraitDecoder.read(portraitFile.getPath());
			portraitPanel.setPortraitDecoder(portraitDecoder);
			tabbedPane.setEnabledAt(1, true);
			hasPortrait = true;
		}
		else
		{
			tabbedPane.setEnabledAt(1, false);
			hasPortrait = false;
		}
		battleActions.clear();
		currentIndex = 0;
		topLabel.setText(" Current Frame Animation: Unassigned");
		loadBattleGif(battleFile.getPath());
		loadWalkGif(walkFile.getPath());
		this.initialized = true;
		this.repaint();
	}

	private void setupUI(boolean embedded)
	{
		tabbedPane = new JTabbedPane();
		JPanel backPanel = new JPanel(new BorderLayout());
		JPanel sidePanel = new JPanel();
		sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
		sidePanel.setPreferredSize(new Dimension(150, 0));
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Stand", "Stand", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Attack", "Attack", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Winddown", "Winddown", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Dodge", "Dodge", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Spell", "Spell", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Item", "Item", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Set Special", "Special", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		playButton = createActionButton("Play Action", "play", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Export Hero", "Export Hero", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Export Enemy", "Export Enemy", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Load New Gifs", "Load", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));

		sidePanel.add(new JLabel("X coordinate offset"));
		xOffsetSpinner = new JSpinner();
		xOffsetSpinner.setPreferredSize(new Dimension(200, 30));
		xOffsetSpinner.setMaximumSize(new Dimension(150, 30));
		xOffsetSpinner.addChangeListener(this);
		xOffsetSpinner.setModel(new SpinnerNumberModel(0, -400, 400, 4));
		xOffsetSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
		sidePanel.add(xOffsetSpinner);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));

		sidePanel.add(new JLabel("Y coordinate offset"));
		yOffsetSpinner = new JSpinner();
		yOffsetSpinner.setPreferredSize(new Dimension(200, 30));
		yOffsetSpinner.setMaximumSize(new Dimension(150, 30));
		yOffsetSpinner.addChangeListener(this);
		yOffsetSpinner.setModel(new SpinnerNumberModel(0, -400, 400, 4));
		yOffsetSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
		sidePanel.add(yOffsetSpinner);
		sidePanel.add(Box.createGlue());
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));

		topLabel = new JLabel(" Current Frame Animation: Unassigned");
		topLabel.setForeground(Color.white);
		topLabel.setPreferredSize(new Dimension(0, 30));
		topLabel.setBackground(Color.DARK_GRAY);
		topLabel.setOpaque(true);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(Color.DARK_GRAY);
		leftButton = createActionButton("< --", "left", bottomPanel);
		rightButton = createActionButton("-- >", "right", bottomPanel);
		leftButton.setEnabled(false);
		rightButton.setEnabled(false);
		playButton.setEnabled(false);


		backPanel.add(sidePanel, BorderLayout.LINE_START);
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

		if (embedded)
			this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		else
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setMinimumSize(new Dimension(500, 600));
		this.pack();
		if (!embedded)
			this.setVisible(true);
	}

	public void loadBattleGif(String file)
	{
		currentIndex = 0;
		leftButton.setEnabled(false);
		File fileToLoad = new File(file);
		System.out.println(fileToLoad.exists());
		if (currentIndex + 1 != battleGifDecoder.getFrameCount())
			rightButton.setEnabled(true);
		try {
			battleGifDecoder.read(new FileInputStream(file));
			imageName = file;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occurred loading the specified file. " + e.getMessage());
			return;
		}
		imagePanel.setCurrentImage(battleGifDecoder.getFrame(0));
	}

	public void loadWalkGif(String file)
	{
		try {
			walkGifDecoder.read(new FileInputStream(new File(file)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occurred loading the specified file. " + e.getMessage());
		}
	}

	public static void main(String args[])
	{
		new GifFrame(false);
	}

	class ImagePanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		private BufferedImage currentImage;

		public ImagePanel()
		{
			this.setPreferredSize(new Dimension(200, 300));
		}


		@Override
		protected void paintComponent(Graphics g) {
			if (initialized)
			{
				g.setColor(Color.white);
				g.fillRect(0, 0, this.getWidth(), this.getHeight());

				int drawX = this.getWidth() / 5; // (this.getWidth() - currentImage.getWidth()) / 2;
				int drawY = this.getHeight() / 2;
				
				int halfWidth = currentImage.getWidth() / 2;
				int halfHeight = currentImage.getHeight() / 2;
				
				if (currentImage != null)
				{
					g.drawImage(currentImage, drawX - halfWidth + (int) xOffsetSpinner.getValue(),
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

	private JButton createActionButton(String text, String command, JPanel container)
	{
		JButton b = new JButton(text);
		b.addActionListener(this);
		b.setActionCommand(command);
		b.setAlignmentX(Component.LEFT_ALIGNMENT);
		b.setMaximumSize(new Dimension(150, 20));
		container.add(b);
		return b;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		String cmd = arg0.getActionCommand();
		if (cmd.equalsIgnoreCase("left"))
		{
			imagePanel.setCurrentImage(battleGifDecoder.getFrame(--currentIndex));
			if (currentIndex == 0)
				leftButton.setEnabled(false);
			rightButton.setEnabled(true);

			topLabel.setText(" Current Frame Animation: Unassigned");
			playButton.setEnabled(false);
			for (Map.Entry<String, Integer> s : battleActions.entrySet())
			{
				if (s.getValue() == currentIndex)
				{
					playButton.setEnabled(true);
					topLabel.setText(" Current Frame Animation: " + s.getKey());
					break;
				}
			}
		}
		else if (cmd.equalsIgnoreCase("right"))
		{
			imagePanel.setCurrentImage(battleGifDecoder.getFrame(++currentIndex));
			if (currentIndex + 1 == battleGifDecoder.getFrameCount())
				rightButton.setEnabled(false);
			leftButton.setEnabled(true);

			topLabel.setText(" Current Frame Animation: Unassigned");
			playButton.setEnabled(false);
			for (Map.Entry<String, Integer> s : battleActions.entrySet())
			{
				if (s.getValue() == currentIndex)
				{
					topLabel.setText(" Current Frame Animation: " + s.getKey());
					playButton.setEnabled(true);
					break;
				}
			}
		}
		else if (cmd.equalsIgnoreCase("play"))
		{
			Thread t = new Thread(new PlayThread());
			t.start();
		}
		else if (cmd.startsWith("Export"))
		{
			AnimationExporter.export(cmd.contains("Hero"), battleGifDecoder, walkGifDecoder, portraitDecoder, hasPortrait, 
					imageName, portraitPanel, battleActions, xOffsetSpinner, yOffsetSpinner);;
		}
		else if (cmd.startsWith("Load"))
		{
			this.loadImages();
		}
		else
		{
			battleActions.put(cmd, currentIndex);
			topLabel.setText(" Current Frame Animation: " + cmd);
			playButton.setEnabled(true);
		}
	}

	class PlayThread implements Runnable
	{
		@Override
		public void run() {
			outer: while (true)
			{
				try {
					Thread.sleep(battleGifDecoder.getDelay(currentIndex));
				} catch (InterruptedException e) {}

				if (currentIndex + 1 == battleGifDecoder.getFrameCount())
					break;

				for (Map.Entry<String, Integer> s : battleActions.entrySet())
				{
					if (s.getValue() == currentIndex + 1)
					{
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
