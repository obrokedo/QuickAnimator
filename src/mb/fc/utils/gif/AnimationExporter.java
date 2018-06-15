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
	public static void export(boolean hero,  GifDecoder battleGifDecoder, GifDecoder walkGifDecoder, GifDecoder portraitDecoder, boolean hasPortrait, String imageName, 
			PortraitPanel portraitPanel, Hashtable<String, Integer> battleActions, JSpinner xOffsetSpinner, JSpinner yOffsetSpinner)
	{
		 Dimension fullImage = new Dimension(battleGifDecoder.getFrameCount() * battleGifDecoder.getFrameSize().width, (int) battleGifDecoder.getFrameSize().getHeight());
		 BufferedImage bim = new BufferedImage(fullImage.width + walkGifDecoder.getFrameSize().width * 8 + (hasPortrait ? portraitDecoder.getFrameSize().width * portraitDecoder.getFrameCount(): 0),
				 fullImage.height, BufferedImage.TYPE_INT_ARGB);

		  ArrayList<String> spriteSheetContents = new ArrayList<String>();
		  spriteSheetContents.add("<img name=\"" + imageName.substring(
				  imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".png") + "\" w=\"" + fullImage.width + "\" h=\"" + fullImage.height + "\">\n");
		  spriteSheetContents.add("\t<definitions>\n");
		  spriteSheetContents.add("\t\t<dir name=\"/\">\n");
		  spriteSheetContents.add("\t\t\t<dir name=\"Unpromoted\">\n");
		  Graphics g = bim.createGraphics();
		  for (int i = 0; i < battleGifDecoder.getFrameCount(); i++)
		  {
			  spriteSheetContents.add("\t\t\t\t<spr name=\"Frame" + i + "\" x=\"" + i * battleGifDecoder.getFrameSize().width + "\" y=\"0\" w=\""
					  + battleGifDecoder.getFrameSize().width + "\" h=\"" + battleGifDecoder.getFrameSize().height + "\"/>\n");
			  g.drawImage(battleGifDecoder.getFrame(i), i * battleGifDecoder.getFrameSize().width, 0, null);
		  }

		  for (int i = 0; i < 6; i++)
		  {
			  g.drawImage(transformColorToTransparency(walkGifDecoder.getFrame(i), GifFrame.TRANS), battleGifDecoder.getFrameCount() * battleGifDecoder.getFrameSize().width +
					  i * walkGifDecoder.getFrameSize().width, 0, null);

			  int width = battleGifDecoder.getFrameCount() * battleGifDecoder.getFrameSize().width +
					  i * walkGifDecoder.getFrameSize().width;

			  spriteSheetContents.add("\t\t\t\t<spr name=\"Walk" + i + "\" x=\"" + width + "\" y=\"0\" w=\""
					  + walkGifDecoder.getFrameSize().width + "\" h=\"" + walkGifDecoder.getFrameSize().height + "\"/>\n");
		  }

		  if (hasPortrait)
		  {
			  for (int i = 0; i < portraitDecoder.getFrameCount(); i++)
			  {
				  int placeX = battleGifDecoder.getFrameCount() * battleGifDecoder.getFrameSize().width +
						  8 * walkGifDecoder.getFrameSize().width + i * portraitDecoder.getFrameSize().width;
				  spriteSheetContents.add("\t\t\t\t<spr name=\"PortraitTop" + i + "\" x=\"" + placeX + "\" y=\"0\" w=\""
						  + portraitDecoder.getFrameSize().width + "\" h=\"" + portraitPanel.getyPortraitSplit() + "\"/>\n");
				  spriteSheetContents.add("\t\t\t\t<spr name=\"PortraitBottom" + i + "\" x=\"" + placeX + "\" y=\"" + portraitPanel.getyPortraitSplit() + "\" w=\""
						  + portraitDecoder.getFrameSize().width + "\" h=\"" + (portraitDecoder.getFrameSize().height - portraitPanel.getyPortraitSplit()) + "\"/>\n");
				  g.drawImage(portraitDecoder.getFrame(i), placeX, 0, null);
			  }
		  }

		  spriteSheetContents.add("\t\t\t</dir>\n");
		  spriteSheetContents.add("\t\t</dir>\n");
		  spriteSheetContents.add("\t</definitions>\n");
		  spriteSheetContents.add("</img>\n");

		  g.dispose();

		  ArrayList<String> animStrings = new ArrayList<String>();
		  animStrings.add("<animations spriteSheet=\"" + imageName.substring(
				  imageName.lastIndexOf(File.separator) + 1).replace(".gif", ".sprites") + "\" ver=\"1.2\">");
		  addWalkAnimations(animStrings);


		  HashSet<Integer> keyFrames = new HashSet<Integer>(battleActions.values());
		  for (Map.Entry<String,Integer> e : battleActions.entrySet())
		  {
			  animStrings.add("<anim name=\"Un" + e.getKey() + "\" loops=\"0\">");

			  int count = 0;
			  int index = e.getValue();

			  while (true)
			  {
				  animStrings.add("<cell index=\"" + count + "\" delay=\"" + battleGifDecoder.getDelay(index) + "\">");
				  if (!hero)
					  animStrings.add("<spr name=\"/Unpromoted/Frame" + index + "\" x=\"" + (int) xOffsetSpinner.getValue() + "\" y=\"" + (int) yOffsetSpinner.getValue() + "\" z=\"0\"/>");
				  else
					  animStrings.add("<spr name=\"/Unpromoted/Frame" + index + "\" x=\"" + (int) xOffsetSpinner.getValue() + "\" y=\"" + (int) yOffsetSpinner.getValue() + "\" z=\"0\"/>");
				  animStrings.add("</cell>");

				  count++;
				  index++;

				  if (keyFrames.contains(index) || index == battleGifDecoder.getFrameCount())
					  break;
			  }

			  animStrings.add("</anim>");
		  }

		  if (hasPortrait)
		  {
			  keyFrames = new HashSet<Integer>(portraitPanel.getBattleActions().values());
			  for (Map.Entry<String,Integer> e : portraitPanel.getBattleActions().entrySet())
			  {
				  animStrings.add("<anim name=\"UnPort" + e.getKey() + "\" loops=\"0\">");

				  int count = 0;
				  int index = e.getValue();

				  while (true)
				  {
					  animStrings.add("<cell index=\"" + count + "\" delay=\"" + portraitDecoder.getDelay(index) + "\">");
					  if (e.getKey().equalsIgnoreCase("Idle"))
					  {
						  animStrings.add("<spr name=\"/Unpromoted/PortraitTop" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
						  animStrings.add("<spr name=\"/Unpromoted/PortraitBottom" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
					  }
					  else if (e.getKey().equalsIgnoreCase("Blink"))
					  {
						  animStrings.add("<spr name=\"/Unpromoted/PortraitTop" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
					  }
					  else if (e.getKey().equalsIgnoreCase("Talk"))
					  {
						  animStrings.add("<spr name=\"/Unpromoted/PortraitBottom" + index + "\" x=\"0\" y=\"0\" z=\"0\"/>");
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

		  Path path = Paths.get(imageName.replace(".gif", ".sprites"));
		  Path animPath = Paths.get(imageName.replace(".gif", ".anim"));

		  File outputfile = new File(imageName.replace(".gif", ".png"));
			try {
				Files.write(path, spriteSheetContents, StandardCharsets.UTF_8);
				Files.write(animPath, animStrings, StandardCharsets.UTF_8);
				ImageIO.write(bim, "png", outputfile);
				JOptionPane.showMessageDialog(null, "Animation successfully written to " + path.toString());
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "An error occurred while trying to save the animation:"
						+ e.getMessage(), "Error saving animation", JOptionPane.ERROR_MESSAGE);
			}
	}

	private static void addWalkAnimations(ArrayList<String> anims)
	{
		anims.add("<anim name=\"UnUp\" loops=\"0\">");
		anims.add("<cell index=\"0\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk4\" x=\"0\" y=\"0\" z=\"0\"/>");
		anims.add("</cell>");
		anims.add("<cell index=\"1\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk5\" x=\"0\" y=\"0\" z=\"0\"/>");
		anims.add("</cell>");
		anims.add("</anim>");
		anims.add("<anim name=\"UnDown\" loops=\"0\">");
		anims.add("<cell index=\"0\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk0\" x=\"0\" y=\"0\" z=\"0\"/>");
		anims.add("</cell>");
		anims.add("<cell index=\"1\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk1\" x=\"0\" y=\"0\" z=\"0\"/>");
		anims.add("</cell>");
		anims.add("</anim>");
		anims.add("<anim name=\"UnLeft\" loops=\"0\">");
		anims.add("<cell index=\"0\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk2\" x=\"0\" y=\"0\" z=\"0\" flipH=\"1\"/>");
		anims.add("</cell>");
		anims.add("<cell index=\"1\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk3\" x=\"0\" y=\"0\" z=\"0\" flipH=\"1\"/>");
		anims.add("</cell>");
		anims.add("</anim>");
		anims.add("<anim name=\"UnRight\" loops=\"0\">");
		anims.add("<cell index=\"0\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk2\" x=\"0\" y=\"0\" z=\"0\"/>");
		anims.add("</cell>");
		anims.add("<cell index=\"1\" delay=\"1\">");
		anims.add("<spr name=\"/Unpromoted/Walk3\" x=\"0\" y=\"0\" z=\"0\"/>");
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
}
