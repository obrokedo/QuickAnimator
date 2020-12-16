package mb.fc.utils.gif;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;
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
import javax.swing.JOptionPane;
import javax.swing.JSpinner;

public class AnimationExporter {	
	  public static void generateCombatantSpriteSheet(GifDecoder battleGifDecoder, GifDecoder walkGifDecoder, GifDecoder portraitDecoder, 
			  boolean hasPortrait, String imageName, PortraitPanel portraitPanel, boolean promoted) throws IOException {
	    String directory = "Unpromoted";
	    String prefix = "Un";
	    if (promoted) {
	      directory = "Promoted";
	      prefix = "Pro";
	    } 
	    Dimension fullImage = new Dimension(battleGifDecoder.getFrameCount() * (battleGifDecoder.getFrameSize()).width, 
				(int) battleGifDecoder.getFrameSize().getHeight());
		BufferedImage bim = new BufferedImage(
				fullImage.width + walkGifDecoder.getFrameSize().width * 8
						+ (hasPortrait ? portraitDecoder.getFrameSize().width * portraitDecoder.getFrameCount() : 0),
				fullImage.height, BufferedImage.TYPE_INT_ARGB);

		ArrayList<String> spriteSheetContents = new ArrayList<>();
		spriteSheetContents.add(
				"<img name=\"" + imageName.substring(imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".png")
						+ "\" w=\"" + fullImage.width + "\" h=\"" + fullImage.height + "\">\n");
		spriteSheetContents.add("\t<definitions>\n");
		spriteSheetContents.add("\t\t<dir name=\"/\">\n");
		spriteSheetContents.add("\t\t\t<dir name=\"" + directory + "\">\n");
		Graphics g = bim.createGraphics();
		for (int i = 0; i < battleGifDecoder.getFrameCount(); i++) {
			spriteSheetContents
					.add("\t\t\t\t<spr name=\"Frame" + i + "\" x=\"" + i * battleGifDecoder.getFrameSize().width
							+ "\" y=\"0\" w=\"" + battleGifDecoder.getFrameSize().width + "\" h=\""
							+ battleGifDecoder.getFrameSize().height + "\"/>\n");
			g.drawImage(battleGifDecoder.getFrame(i), i * battleGifDecoder.getFrameSize().width, 0, null);
		}

		for (int i = 0; i < 6; i++) {
			g.drawImage(transformColorToTransparency(walkGifDecoder.getFrame(i), GifFrame.TRANS),
					battleGifDecoder.getFrameCount() * battleGifDecoder.getFrameSize().width
							+ i * walkGifDecoder.getFrameSize().width,
					0, null);

			int width = battleGifDecoder.getFrameCount() * battleGifDecoder.getFrameSize().width
					+ i * walkGifDecoder.getFrameSize().width;

			spriteSheetContents.add("\t\t\t\t<spr name=\"Walk" + i + "\" x=\"" + width + "\" y=\"0\" w=\""
					+ walkGifDecoder.getFrameSize().width + "\" h=\"" + walkGifDecoder.getFrameSize().height
					+ "\"/>\n");
		}

		if (hasPortrait) {
			for (int i = 0; i < portraitDecoder.getFrameCount(); i++) {
				int placeX = battleGifDecoder.getFrameCount() * battleGifDecoder.getFrameSize().width
						+ 8 * walkGifDecoder.getFrameSize().width + i * portraitDecoder.getFrameSize().width;
				spriteSheetContents.add("\t\t\t\t<spr name=\"PortraitTop" + i + "\" x=\"" + placeX + "\" y=\"0\" w=\""
						+ portraitDecoder.getFrameSize().width + "\" h=\"" + portraitPanel.getyPortraitSplit()
						+ "\"/>\n");
				spriteSheetContents.add("\t\t\t\t<spr name=\"PortraitBottom" + i + "\" x=\"" + placeX + "\" y=\""
						+ portraitPanel.getyPortraitSplit() + "\" w=\"" + portraitDecoder.getFrameSize().width
						+ "\" h=\"" + (portraitDecoder.getFrameSize().height - portraitPanel.getyPortraitSplit())
						+ "\"/>\n");
				g.drawImage(portraitDecoder.getFrame(i), placeX, 0, null);
			}
		}

		spriteSheetContents.add("\t\t\t</dir>\n");
		spriteSheetContents.add("\t\t</dir>\n");
		spriteSheetContents.add("\t</definitions>\n");
		spriteSheetContents.add("</img>\n");

		g.dispose();

		Path path = Paths.get(imageName.replace(".gif", ".sprites"));
		File outputfile = new File(imageName.replace(".gif", ".png"));
		Files.write(path, spriteSheetContents, StandardCharsets.UTF_8);
		ImageIO.write(bim, "png", outputfile);
	}

	private static void generateCombatantAnimations(GifDecoder battleGifDecoder,
			GifDecoder portraitDecoder, boolean hasPortrait, String imageName, PortraitPanel portraitPanel,
			Hashtable<String, Integer> battleActions, JSpinner xOffsetSpinner, JSpinner yOffsetSpinner, boolean promoted)
			throws IOException {
		String directory = "Unpromoted";
	    String prefix = "Un";
	    if (promoted) {
	      directory = "Promoted";
	      prefix = "Pro";
	    } 
		ArrayList<String> animStrings = new ArrayList<String>();
		animStrings.add("<animations spriteSheet=\""
				+ imageName.substring(imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".sprites")
				+ "\" ver=\"1.2\">");
		addWalkAnimations(animStrings, directory, prefix);

		HashSet<Integer> keyFrames = new HashSet<Integer>(battleActions.values());
		for (Map.Entry<String, Integer> e : battleActions.entrySet()) {
	      animStrings.add("<anim name=\"" + prefix + (String)e.getKey() + "\" loops=\"0\">");
			int count = 0;
			int index = e.getValue();

			while (true) {
				animStrings.add("<cell index=\"" + count + "\" delay=\"" + battleGifDecoder.getDelay(index) + "\">");
		        animStrings.add(
		            "<spr name=\"/" + directory + "/Frame" + index + "\" x=\"" + ((Integer)xOffsetSpinner.getValue()).intValue() + 
		            "\" y=\"" + ((Integer)yOffsetSpinner.getValue()).intValue() + "\" z=\"0\"/>");
				animStrings.add("</cell>");

				count++;
				index++;

				if (keyFrames.contains(index) || index == battleGifDecoder.getFrameCount())
					break;
			}

			animStrings.add("</anim>");
		}

		if (hasPortrait) {
			keyFrames = new HashSet<>(portraitPanel.getBattleActions().values());
			for (Map.Entry<String, Integer> e : portraitPanel.getBattleActions().entrySet()) {
				animStrings.add("<anim name=\"" + prefix + "Port" + (String)e.getKey() + "\" loops=\"0\">");
				int count = 0;
				int index = e.getValue();

				while (true) {
					animStrings.add("<cell index=\"" + count + "\" delay=\"" + portraitDecoder.getDelay(index) + "\">");
			          if (((String)e.getKey()).equalsIgnoreCase("Idle")) {
			            animStrings.add(
			                "<spr name=\"/" + directory + "/PortraitTop" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
			            animStrings.add("<spr name=\"/" + directory + "/PortraitBottom" + index + 
			                "\" x=\"0\" y=\"0\" z=\"0\"/>");
			          } else if (((String)e.getKey()).equalsIgnoreCase("Blink")) {
			            animStrings.add(
			                "<spr name=\"/" + directory + "/PortraitTop" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
			          } else if (((String)e.getKey()).equalsIgnoreCase("Talk")) {
			            animStrings.add("<spr name=\"/" + directory + "/PortraitBottom" + index + 
			                "\" x=\"0\" y=\"0\" z=\"0\"/>");
			          }

					animStrings.add("</cell>");

					count++;
					index++;

					if (keyFrames.contains(index) || index == portraitDecoder.getFrameCount())
						break;
				}

				animStrings.add("</anim>");
			}
		}

		animStrings.add("</animations>");

		Path animPath = Paths.get(imageName.replace(".gif", ".anim"));
		Files.write(animPath, animStrings, StandardCharsets.UTF_8);
		JOptionPane.showMessageDialog(null, "Animation successfully written to " + animPath.toString());
	}
	
	public static void exportCombatant(GifDecoder battleGifDecoder, GifDecoder walkGifDecoder, 
			GifDecoder portraitDecoder, boolean hasPortrait, String imageName, PortraitPanel portraitPanel, 
			Hashtable<String, Integer> battleActions, JSpinner xOffsetSpinner, JSpinner yOffsetSpinner, boolean promoted)
	{		 
		try {
			generateCombatantSpriteSheet(battleGifDecoder, walkGifDecoder, portraitDecoder, hasPortrait, imageName, portraitPanel, promoted);
			generateCombatantAnimations(battleGifDecoder, portraitDecoder, hasPortrait, imageName, 
					portraitPanel, battleActions, xOffsetSpinner, yOffsetSpinner, promoted);
			
			
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "An error occurred while trying to save the animation:"
					+ e.getMessage(), "Error saving animation", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static void addWalkAnimations(ArrayList<String> anims, String directory, String prefix) {
	    anims.add("<anim name=\"" + prefix + "Up\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk4\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk5\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("</anim>");
	    anims.add("<anim name=\"" + prefix + "Down\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk0\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk1\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("</anim>");
	    anims.add("<anim name=\"" + prefix + "Left\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk2\" x=\"0\" y=\"0\" z=\"0\" flipH=\"1\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk3\" x=\"0\" y=\"0\" z=\"0\" flipH=\"1\"/>");
	    anims.add("</cell>");
	    anims.add("</anim>");
	    anims.add("<anim name=\"" + prefix + "Right\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk2\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "/Walk3\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("</anim>");
	  }
	
	public static Image transformColorToTransparency(final BufferedImage im, final Color color)
	   {
	      final ImageFilter filter = new RGBImageFilter()
	      {
	         // the color we are looking for (white)... Alpha bits are set to opaque
	         public int markerRGB = color.getRGB(); // | 0xFFFFFFFF;

	         @Override
			public final int filterRGB(final int x, final int y, final int rgb)
	         {
	            if ((rgb | 0xFF000000) == markerRGB)
	            {
	               // Mark the alpha bits as zero - transparent
	               return 0x00FFFFFF & rgb;
	            }
	            else
	            {
	               // nothing to do
	               return rgb;
	            }
	         }
	      };

	      final ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
	      return Toolkit.getDefaultToolkit().createImage(ip);
	   }
	
	private static void generateWeaponSpriteSheet(GifDecoder weaponGifDecoder, String imageName)
			throws IOException {
		Dimension fullImage = new Dimension(weaponGifDecoder.getFrameCount() * weaponGifDecoder.getFrameSize().width,
				(int) weaponGifDecoder.getFrameSize().getHeight());
		BufferedImage bim = new BufferedImage(
				fullImage.width, fullImage.height, BufferedImage.TYPE_INT_ARGB);

		ArrayList<String> spriteSheetContents = new ArrayList<String>();
		spriteSheetContents.add(
				"<img name=\"" + imageName.substring(imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".png")
						+ "\" w=\"" + fullImage.width + "\" h=\"" + fullImage.height + "\">\n");
		spriteSheetContents.add("\t<definitions>\n");
		spriteSheetContents.add("\t\t<dir name=\"/\">\n");
		spriteSheetContents.add("\t\t\t<dir name=\"weapon\">\n");
		Graphics g = bim.createGraphics();
		for (int i = 0; i < weaponGifDecoder.getFrameCount(); i++) {
			spriteSheetContents
					.add("\t\t\t\t<spr name=\"Frame" + i + "\" x=\"" + i * weaponGifDecoder.getFrameSize().width
							+ "\" y=\"0\" w=\"" + weaponGifDecoder.getFrameSize().width + "\" h=\""
							+ weaponGifDecoder.getFrameSize().height + "\"/>\n");
			g.drawImage(weaponGifDecoder.getFrame(i), i * weaponGifDecoder.getFrameSize().width, 0, null);
		}
		
		spriteSheetContents.add("\t\t\t</dir>\n");
		spriteSheetContents.add("\t\t</dir>\n");
		spriteSheetContents.add("\t</definitions>\n");
		spriteSheetContents.add("</img>\n");

		g.dispose();

		Path path = Paths.get(imageName.replace(".gif", ".sprites").replaceAll("weapongifs", "weaponanim"));
		File outputfile = new File(imageName.replace(".gif", ".png").replaceAll("weapongifs", "weaponanim"));
		Files.write(path, spriteSheetContents, StandardCharsets.UTF_8);
		ImageIO.write(bim, "png", outputfile);
	}
	
	private static void generateWeaponAnimations(GifDecoder weaponGifDecoder, String imageName)
			throws IOException {
		ArrayList<String> animStrings = new ArrayList<String>();
		animStrings.add("<animations spriteSheet=\""
				+ imageName.substring(imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".sprites")
				+ "\" ver=\"1.2\">");
		
		animStrings.add("<anim name=\"attack\" loops=\"1\">");
		for (int i = 0; i < weaponGifDecoder.getFrameCount(); i++) {
			animStrings.add("<cell index=\"0\" delay=\"1\">");
			animStrings.add("<spr name=\"/weapon/Frame" + i + "\" x=\"0\" y=\"0\" z=\"0\"/>");
			animStrings.add("</cell>");
		}
		animStrings.add("</anim>");
		

		animStrings.add("</animations>");

		Path animPath = Paths.get(imageName.replace(".gif", ".anim").replaceAll("weapongifs", "weaponanim"));
		Files.write(animPath, animStrings, StandardCharsets.UTF_8);
		JOptionPane.showMessageDialog(null, "Animation successfully written to " + animPath.toString());
	}
	
	public static void exportWeapon(GifDecoder weaponGifDecoder, String imageName)
	{		 
		try {
			generateWeaponSpriteSheet(weaponGifDecoder, imageName);
			generateWeaponAnimations(weaponGifDecoder, imageName);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "An error occurred while trying to save the animation:"
					+ e.getMessage(), "Error saving animation", JOptionPane.ERROR_MESSAGE);
		}
	}
	
}
