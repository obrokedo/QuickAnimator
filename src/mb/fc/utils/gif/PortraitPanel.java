package mb.fc.utils.gif;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PortraitPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	private GifDecoder portraitDecoder;
	private ImagePanel imagePanel;
	private JButton leftButton;
	private JButton rightButton;
	private int currentIndex = 0;
	private Hashtable<String, Integer> battleActions = new Hashtable<String, Integer>();
	private JLabel actionLabel;
	private int yPortraitSplit = 0;

	public PortraitPanel()
	{
		this.setLayout(new BorderLayout());
		imagePanel = new ImagePanel();

		JPanel sidePanel = new JPanel();
		sidePanel.setPreferredSize(new Dimension(150, 0));
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Start Talk", "Talk", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Start Blink", "Blink", sidePanel);
		sidePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		createActionButton("Start Idle", "Idle", sidePanel);

		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.PAGE_AXIS));
		actionLabel = new JLabel(" Current Frame Animation: Unassigned");
		actionLabel.setForeground(Color.white);
		actionLabel.setPreferredSize(new Dimension(0, 30));
		actionLabel.setBackground(Color.DARK_GRAY);
		actionLabel.setOpaque(true);

		System.out.println("CONSTRUCTOR");
		this.add(actionLabel, BorderLayout.PAGE_START);
		this.add(sidePanel, BorderLayout.LINE_START);
		this.add(imagePanel, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setBackground(Color.DARK_GRAY);
		leftButton = createActionButton("< --", "left", bottomPanel);
		rightButton = createActionButton("-- >", "right", bottomPanel);
		leftButton.setEnabled(false);
		this.add(bottomPanel, BorderLayout.PAGE_END);
	}

	public void setPortraitDecoder(GifDecoder portraitDecoder) {
		System.out.println("SET PORTRAIT");
		actionLabel.setText(" Current Frame Animation: Unassigned");
		this.battleActions.clear();
		this.portraitDecoder = portraitDecoder;
		currentIndex = 0;
		yPortraitSplit = portraitDecoder.getFrame(currentIndex).getHeight() / 2;
		imagePanel.setCurrentImage(portraitDecoder.getFrame(currentIndex));
	}

	class ImagePanel extends JPanel implements MouseListener
	{
		private static final long serialVersionUID = 1L;

		private BufferedImage currentImage;

		public ImagePanel()
		{
			this.setPreferredSize(new Dimension(200, 300));
			this.addMouseListener(this);
		}


		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.white);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());

			int x = this.getWidth() / 2 - currentImage.getWidth() / 2;
			int y = (this.getHeight() - currentImage.getHeight()) / 2;
			if (currentImage != null)
			{
				g.drawImage(currentImage, x, y, this);

				g.setColor(Color.blue);
				g.drawLine(x, y + yPortraitSplit, x + currentImage.getWidth(), y + yPortraitSplit);
			}

		}



		public void setCurrentImage(BufferedImage currentImage) {
			this.currentImage = currentImage;
			this.repaint();
		}


		@Override
		public void mouseClicked(MouseEvent arg0) {
			int y = (this.getHeight() - currentImage.getHeight()) / 2;
			int clickY = arg0.getY();
			if (clickY > y && clickY < y + currentImage.getHeight())
			{
				yPortraitSplit = clickY - y;
				this.repaint();
			}
		}


		@Override
		public void mouseEntered(MouseEvent arg0) {

		}


		@Override
		public void mouseExited(MouseEvent arg0) {

		}


		@Override
		public void mousePressed(MouseEvent arg0) {

		}


		@Override
		public void mouseReleased(MouseEvent arg0) {

		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equalsIgnoreCase("left"))
		{
			imagePanel.setCurrentImage(portraitDecoder.getFrame(--currentIndex));
			if (currentIndex == 0)
				leftButton.setEnabled(false);
			rightButton.setEnabled(true);

			actionLabel.setText(" Current Frame Animation: Unassigned");
			for (Map.Entry<String, Integer> s : battleActions.entrySet())
			{
				if (s.getValue() == currentIndex)
				{
					actionLabel.setText(" Current Frame Animation: " + s.getKey());
					break;
				}
			}
		}
		else if (cmd.equalsIgnoreCase("right"))
		{
			imagePanel.setCurrentImage(portraitDecoder.getFrame(++currentIndex));
			if (currentIndex + 1 == portraitDecoder.getFrameCount())
				rightButton.setEnabled(false);
			leftButton.setEnabled(true);

			actionLabel.setText(" Current Frame Animation: Unassigned");
			for (Map.Entry<String, Integer> s : battleActions.entrySet())
			{
				if (s.getValue() == currentIndex)
				{
					actionLabel.setText(" Current Frame Animation: " + s.getKey());

					break;
				}
			}
		}
		else
		{
			battleActions.put(cmd, currentIndex);
			actionLabel.setText(" Current Frame Animation: " + cmd);
		}

		System.out.println(actionLabel + " " + actionLabel.getText());
		actionLabel.repaint();
		this.repaint();
	}

	private JButton createActionButton(String text, String command, JPanel container)
	{
		JButton b = new JButton(text);
		b.addActionListener(this);
		b.setActionCommand(command);
		b.setMaximumSize(new Dimension(150, 25));
		container.add(b);
		return b;
	}

	public Hashtable<String, Integer> getBattleActions() {
		return battleActions;
	}

	public int getyPortraitSplit() {
		return yPortraitSplit;
	}
}
