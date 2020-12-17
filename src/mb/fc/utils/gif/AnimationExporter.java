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
	  private static void generateCombatantSpriteSheet(GifDecoder battleGifDecoder, GifDecoder walkGifDecoder, 
			  	GifDecoder portraitDecoder,  boolean hasPortrait, String imageName, PortraitPanel portraitPanel, 
			  		String directory) throws IOException {	    
	    Dimension battleImageDims = null;
	    if (battleGifDecoder != null)
	    	battleImageDims = new Dimension(battleGifDecoder.getFrameCount() * (battleGifDecoder.getFrameSize()).width, 
					(int) battleGifDecoder.getFrameSize().getHeight());
	    else
	    	battleImageDims = new Dimension(0, 0);
	    
	    int walkImageWidth = 0;
	    if (walkGifDecoder != null)
	    	walkImageWidth = walkGifDecoder.getFrameSize().width * 8;
	    
		BufferedImage bim = new BufferedImage(
				battleImageDims.width + walkImageWidth
						+ (hasPortrait ? portraitDecoder.getFrameSize().width * portraitDecoder.getFrameCount() : 0),
				(battleImageDims.height != 0 ? battleImageDims.height : walkGifDecoder.getFrameSize().height), BufferedImage.TYPE_INT_ARGB);

		ArrayList<String> spriteSheetContents = new ArrayList<>();
		spriteSheetContents.add(
				"<img name=\"" + imageName.substring(imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".png")
						+ "\" w=\"" + bim.getWidth() + "\" h=\"" + bim.getHeight() + "\">\n");
		spriteSheetContents.add("\t<definitions>\n");
		spriteSheetContents.add("\t\t<dir name=\"/\">\n");
		// Only add this directory if there is actually something to add. This will be 0 length for spells and npcs
		if (directory.length() > 0) {
			spriteSheetContents.add("\t\t\t<dir name=\"" + directory.substring(0, directory.length() - 1) + "\">\n");
		}
		Graphics g = bim.createGraphics();
		
		
		// Add battle animations
		if (battleGifDecoder != null) {
			for (int i = 0; i < battleGifDecoder.getFrameCount(); i++) {
				spriteSheetContents
						.add("\t\t\t\t<spr name=\"Frame" + i + "\" x=\"" + i * battleGifDecoder.getFrameSize().width
								+ "\" y=\"0\" w=\"" + battleGifDecoder.getFrameSize().width + "\" h=\""
								+ battleGifDecoder.getFrameSize().height + "\"/>\n");
				g.drawImage(transformColorToTransparency(battleGifDecoder.getFrame(i), GifFrame.COMBAT_TRANS), i * battleGifDecoder.getFrameSize().width, 0, null);
			}
		}
		
		// Add walking animations
		if (walkGifDecoder != null) {
			for (int i = 0; i < 6; i++) {
				g.drawImage(transformColorToTransparency(walkGifDecoder.getFrame(i), GifFrame.WALK_TRANS),
						battleImageDims.width + i * walkGifDecoder.getFrameSize().width,
						0, null);
	
				int walkX = battleImageDims.width + i * walkGifDecoder.getFrameSize().width;
	
				spriteSheetContents.add("\t\t\t\t<spr name=\"Walk" + i + "\" x=\"" + walkX + "\" y=\"0\" w=\""
						+ walkGifDecoder.getFrameSize().width + "\" h=\"" + walkGifDecoder.getFrameSize().height
						+ "\"/>\n");
			}
		}

		// Add portrait animations
		if (hasPortrait) {
			for (int i = 0; i < portraitDecoder.getFrameCount(); i++) {
				int placeX = battleImageDims.width
						+ walkImageWidth + i * portraitDecoder.getFrameSize().width;
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

		// Only add this directory if there is actually something to add. This will be 0 length for spells and npcs
		if (directory.length() > 0)
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
	  
	private static void exportAnimations(ArrayList<String> animStrings, String imageName) throws IOException {
		animStrings.add(0, "<animations spriteSheet=\""
				+ imageName.substring(imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".sprites")
				+ "\" ver=\"1.2\">");
		animStrings.add("</animations>");

		Path animPath = Paths.get(imageName.replace(".gif", ".anim"));
		Files.write(animPath, animStrings, StandardCharsets.UTF_8);
		JOptionPane.showMessageDialog(null, "Animation successfully written to " + animPath.toString());
	}

	private static void addPortraitAnimations(GifDecoder portraitDecoder, boolean hasPortrait,
			PortraitPanel portraitPanel, String directory, String prefix, ArrayList<String> animStrings) {
		if (hasPortrait) {
			HashSet<Integer> keyFrames = new HashSet<>(portraitPanel.getBattleActions().values());
			for (Map.Entry<String, Integer> e : portraitPanel.getBattleActions().entrySet()) {
				animStrings.add("<anim name=\"" + prefix + "Port" + (String)e.getKey() + "\" loops=\"0\">");
				int count = 0;
				int index = e.getValue();

				while (true) {
					animStrings.add("<cell index=\"" + count + "\" delay=\"" + portraitDecoder.getDelay(index) + "\">");
			          if (((String)e.getKey()).equalsIgnoreCase("Idle")) {
			            animStrings.add(
			                "<spr name=\"/" + directory + "PortraitTop" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
			            animStrings.add("<spr name=\"/" + directory + "PortraitBottom" + index + 
			                "\" x=\"0\" y=\"0\" z=\"0\"/>");
			          } else if (((String)e.getKey()).equalsIgnoreCase("Blink")) {
			            animStrings.add(
			                "<spr name=\"/" + directory + "PortraitTop" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
			          } else if (((String)e.getKey()).equalsIgnoreCase("Talk")) {
			            animStrings.add("<spr name=\"/" + directory + "PortraitBottom" + index + 
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
	}

	private static void addCombatAnimations(GifDecoder battleGifDecoder, Hashtable<String, Integer> battleActions,
			JSpinner xOffsetSpinner, JSpinner yOffsetSpinner, String directory, String prefix,
			ArrayList<String> animStrings) {
		HashSet<Integer> keyFrames = new HashSet<Integer>(battleActions.values());
		for (Map.Entry<String, Integer> e : battleActions.entrySet()) {
	      animStrings.add("<anim name=\"" + prefix + (String)e.getKey() + "\" loops=\"0\">");
			int count = 0;
			int index = e.getValue();			
			while (true) {
				animStrings.add("<cell index=\"" + count + "\" delay=\"" + battleGifDecoder.getDelay(index) + "\">");
		        animStrings.add(
		            "<spr name=\"/" + directory + "Frame" + index + "\" x=\"" + ((Integer)xOffsetSpinner.getValue()).intValue() + 
		            "\" y=\"" + ((Integer)yOffsetSpinner.getValue()).intValue() + "\" z=\"0\"/>");
				animStrings.add("</cell>");

				count++;
				index++;

				if (keyFrames.contains(index) || index == battleGifDecoder.getFrameCount())
					break;
			}

			animStrings.add("</anim>");
		}
	}
	
	public static void exportCombatant(GifDecoder battleGifDecoder, GifDecoder walkGifDecoder, 
			GifDecoder portraitDecoder, boolean hasPortrait, String imageName, PortraitPanel portraitPanel, 
			Hashtable<String, Integer> battleActions, JSpinner xOffsetSpinner, JSpinner yOffsetSpinner, boolean promoted)
	{		 
		String directory = "Unpromoted/";
	    String prefix = "Un";
	    if (promoted) {
	      directory = "Promoted/";
	      prefix = "Pro";
	    } 
	    
		try {
			generateCombatantSpriteSheet(battleGifDecoder, walkGifDecoder, portraitDecoder, hasPortrait, 
					imageName, portraitPanel, directory);
			
			ArrayList<String> animStrings = new ArrayList<String>();		
			addWalkAnimations(animStrings, directory, prefix);
			addCombatAnimations(battleGifDecoder, battleActions, xOffsetSpinner, yOffsetSpinner, directory, prefix,
					animStrings);
			addPortraitAnimations(portraitDecoder, hasPortrait, portraitPanel, directory, prefix, animStrings);
			exportAnimations(animStrings, imageName);
			
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "An error occurred while trying to save the animation:"
					+ e.getMessage(), "Error saving animation", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void exportSpell(GifDecoder battleGifDecoder,
			String imageName, Hashtable<String, Integer> battleActions, JSpinner xOffsetSpinner, 
				JSpinner yOffsetSpinner) {
		try {
			generateCombatantSpriteSheet(battleGifDecoder, null, null, false, 
					imageName, null, "");
			ArrayList<String> animStrings = new ArrayList<String>();
			addCombatAnimations(battleGifDecoder, battleActions, xOffsetSpinner, yOffsetSpinner, "", "",
					animStrings);
			exportAnimations(animStrings, imageName);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "An error occurred while trying to save the animation:"
					+ e.getMessage(), "Error saving animation", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static void exportNPC(GifDecoder walkGifDecoder, String imageName, GifDecoder portraitDecoder, 
			boolean hasPortrait, PortraitPanel portraitPanel) {
		try {
			generateCombatantSpriteSheet(null, walkGifDecoder, portraitDecoder, hasPortrait, 
					imageName, portraitPanel, "");
			ArrayList<String> animStrings = new ArrayList<String>();		
			addWalkAnimations(animStrings, "", "");
			addPortraitAnimations(portraitDecoder, hasPortrait, portraitPanel, "", "", animStrings);
			exportAnimations(animStrings, imageName);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "An error occurred while trying to save the animation:"
					+ e.getMessage(), "Error saving animation", JOptionPane.ERROR_MESSAGE);
		}
	}

	private static void addWalkAnimations(ArrayList<String> anims, String directory, String prefix) {
	    anims.add("<anim name=\"" + prefix + "Up\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk4\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk5\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("</anim>");
	    anims.add("<anim name=\"" + prefix + "Down\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk0\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk1\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("</anim>");
	    anims.add("<anim name=\"" + prefix + "Left\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk2\" x=\"0\" y=\"0\" z=\"0\" flipH=\"1\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk3\" x=\"0\" y=\"0\" z=\"0\" flipH=\"1\"/>");
	    anims.add("</cell>");
	    anims.add("</anim>");
	    anims.add("<anim name=\"" + prefix + "Right\" loops=\"0\">");
	    anims.add("<cell index=\"0\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk2\" x=\"0\" y=\"0\" z=\"0\"/>");
	    anims.add("</cell>");
	    anims.add("<cell index=\"1\" delay=\"1\">");
	    anims.add("<spr name=\"/" + directory + "Walk3\" x=\"0\" y=\"0\" z=\"0\"/>");
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
	
	private static void exportWeapon(GifDecoder weaponGifDecoder, String imageName)
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
