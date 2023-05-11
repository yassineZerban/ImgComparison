package com.ImgComparison;
import java.awt.Color;

import java.awt.Graphics2D;
import net.coobird.thumbnailator.Thumbnails;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.ArrayList;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

public class ImageUtil {
	static BufferedImage smallerImage = null;
	static BufferedImage largerImage = null;
	static BufferedImage resImage = null;
    static boolean sameContent;



	/*public static ComparisonState compareImages(BufferedImage image1, BufferedImage image2, int tolerance) throws IOException {
	    BufferedImage largerImage;
	    BufferedImage smallerImage;
	 // comparaison de la taille des images, s'il ne sont pas égaux, on fait qqchose...
      
	  boolean compareSizes =  cmpSize(image1, image2);
	    	if (compareSizes == true ) {
	    		//même dimensions, il faut comparer le contenu
	    		boolean sameContent = compareBlocks(image1, image2, tolerance);
	    		if (sameContent == true) {
	    			// même contenu, comparer le ALPHA
	    			boolean compareAlpha = compareAlpha(image1, image2);
	    			if (compareAlpha == true) {
	    				System.out.println("même contenu, même dimensions,même alpha");
	    				return ComparisonState.IDENTICAL;
	    			}
	    			else {
	    				System.out.println("même contenu, même dimensions,différent alpha");
	    				return ComparisonState.IDENTICAL_DIFFERENT_ALPHA;
	    			}
	    		} else {
	    			System.out.println("pas le même contenu.");
	    			return ComparisonState.NOT_IDENTICAL;
	    		}
	    		
	    	} else {
	    	    largerImage = image1.getWidth() > image2.getWidth() ? image1 : image2;
	    	    smallerImage = image1.getWidth() <= image2.getWidth() ? image1 : image2;
	    		BufferedImage resImg = resizeImageT(smallerImage, largerImage);
	    		boolean sameContent = compareBlocks(smallerImage, resImg, tolerance);
	    		if (sameContent == true) {
	    			boolean compareAlpha = compareAlpha(smallerImage, resImg);
	    			if (compareAlpha == true) {
	    				System.out.println("même contenu, différentes dimensions, même alpha");
	    				return ComparisonState.IDENTICAL_DIFFERENT_DIMENSIONS;
	    			} 
	    			else {
	    				System.out.println("même contenu, différentes dimensions, différent alphas");
	    				return ComparisonState.IDENTICAL_DIFFERENT_DIMENSIONS_AND_ALPHA;
	    			}
	    		} else {
	    			System.out.println("pas le même contenu.[different dims]");
	    			return ComparisonState.NOT_IDENTICAL;
	    		}

	    	}


	   
	}
	public static boolean compareBlocks(BufferedImage image1, BufferedImage image2, int tolerance) {
	    int blockWidth = 32;
	    int blockHeight = 32;

	    int numBlocksX = image1.getWidth() / blockWidth;
	    int numBlocksY = image1.getHeight() / blockHeight;

	    // Calculate average color difference

	    for (int bx = 0; bx < numBlocksX; bx++) {
	        for (int by = 0; by < numBlocksY; by++) {
	            int x = bx * blockWidth;
	            int y = by * blockHeight;
	            Rectangle blockRect = new Rectangle(x, y, blockWidth, blockHeight);

	            boolean blockMatch = compareBlock(image1, image2, blockRect, tolerance);
	            if (!blockMatch) {
	                return false;
	            }
	        }
	    }

	    return true;
	}*/
	public static ComparisonState compareImages(BufferedImage image1, BufferedImage image2, int tolerance, boolean checkContentOnly) {
	    BufferedImage largerImage;
	    BufferedImage smallerImage;
	    final Logger LOGGER =  LogManager.getLogger( ImageUtil.class.getName());

        // Utilisation du logger
 
	    // comparaison de la taille des images, s'il ne sont pas égaux, on fait qqchose...
	    boolean compareSizes = cmpSize(image1, image2);

	    if (compareSizes) {
	    	// le cas où les deux images ont les mêmes dimensions
	        LOGGER.info("Images have the same dimensions, checking content.");
	        sameContent = compareBlocksParallel(image1, image2, tolerance);
	        if (sameContent) {
	            // même contenu, comparer le ALPHA
	            LOGGER.info("Contents are the same, checking if checkContentOnly(toggleButton) is checked.");

	        	if (checkContentOnly) {
	                LOGGER.info("checkContentOnly is checked, returning comparison results.");
	        		return ComparisonState.IDENTICAL;
	        	} else {
	        		//System.out.println("alpha not ignored. comparing");
	                LOGGER.info("checkContentOnly is not checked, comparing alphas.");

		            boolean sameAlpha = compareAlpha(image1, image2);
		            if (sameAlpha) {
		                LOGGER.info("Alphas are the same, returning comparison results.");
		                return ComparisonState.IDENTICAL;
		            } else {
		                LOGGER.info("Alphas are not the same, returning comparison results.");
		                return ComparisonState.IDENTICAL_DIFFERENT_ALPHA;
		            
	        	}
}
	        } else {
	            LOGGER.info("Images are not authentic. Content is different, returning comparison results.");
	            return ComparisonState.NOT_IDENTICAL;
	        }

	    } else {
	        LOGGER.info("Images have the different dimensions, scaling larger image into the smaller one.");

	    	// le cas où les images n'ont pas les même dimensions
	        largerImage = image1.getWidth() > image2.getWidth() ? image1 : image2;
	        smallerImage = image1.getWidth() <= image2.getWidth() ? image1 : image2;
	        BufferedImage resImg = resizeImageT(smallerImage, largerImage);
	         sameContent = compareBlocksParallel(smallerImage, resImg, tolerance);
	        if (sameContent) {
		        LOGGER.info("Images now have the same dimensions, checking content.");

	        	if (checkContentOnly) {
	                LOGGER.info("checkContentOnly is checked, returning comparison results.");
	        		return ComparisonState.IDENTICAL;
	        	} else {
	            boolean sameAlpha = compareAlpha(smallerImage, resImg);
                LOGGER.info("checkContentOnly not is checked, comparing alphas.");

	            if (sameAlpha) {
	                LOGGER.info("Alphas are the same, returning comparison results. | result : "+ComparisonState.IDENTICAL_DIFFERENT_DIMENSIONS);

	                return ComparisonState.IDENTICAL_DIFFERENT_DIMENSIONS;
	            } else {
	                LOGGER.info("Alphas are not the same, returning comparison results. | result : "+ComparisonState.IDENTICAL_DIFFERENT_DIMENSIONS);

	                return ComparisonState.IDENTICAL_DIFFERENT_DIMENSIONS_AND_ALPHA;
	            }
	        	}
	        } else {
                LOGGER.info("Images are not the same, returning comparison results. | result : "+ComparisonState.IDENTICAL_DIFFERENT_DIMENSIONS);
	            return ComparisonState.NOT_IDENTICAL;
	        }
	    }
	}

	public static boolean compareBlocksParallel(BufferedImage image1, BufferedImage image2, int tolerance) {
	    int blockWidth = 64;
	    int blockHeight = 64;

	    int numBlocksX = image1.getWidth() / blockWidth;
	    int numBlocksY = image1.getHeight() / blockHeight;
	    	
	    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	    List<Future<Boolean>> futures = new ArrayList<>();

	    // Calculate average color difference for each block in parallel
	    for (int bx = 0; bx < numBlocksX; bx++) {
	        for (int by = 0; by < numBlocksY; by++) {
	            int x = bx * blockWidth;
	            int y = by * blockHeight;
	            Rectangle blockRect = new Rectangle(x, y, blockWidth, blockHeight);

	            Future<Boolean> future = executor.submit(() -> compareBlock(image1, image2, blockRect, tolerance));
	            futures.add(future);

	        }
	    }

	    executor.shutdown();

	    // Wait for all futures to complete and check if any of them returned false
	    for (Future<Boolean> future : futures) {
	        try {
				if (!future.get()) {
				    return false;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }

	    return true;
	}

	private static boolean compareBlock(BufferedImage image1, BufferedImage image2, Rectangle rect, int tolerance) {
	    int x = rect.x;
	    int y = rect.y;
	    int width = rect.width;
	    int height = rect.height;

	    // Calculate average color for block in image 1
	    double sumR1 = 0, sumG1 = 0, sumB1 = 0;
	    for (int i = x; i < x + width; i++) {
	        for (int j = y; j < y + height; j++) {
	            Color c1 = new Color(image1.getRGB(i, j));
	            sumR1 += c1.getRed();
	            sumG1 += c1.getGreen();
	            sumB1 += c1.getBlue();
	        }
	    }
	    int numPixels = width * height;
	    int avgR1 = (int) (sumR1 / numPixels);
	    int avgG1 = (int) (sumG1 / numPixels);
	    int avgB1 = (int) (sumB1 / numPixels);

	    // Calculate average color for block in image 2
	    double sumR2 = 0, sumG2 = 0, sumB2 = 0;
	    for (int i = x; i < x + width; i++) {
	        for (int j = y; j < y + height; j++) {
	            Color c2 = new Color(image2.getRGB(i, j));
	            sumR2 += c2.getRed();
	            sumG2 += c2.getGreen();
	            sumB2 += c2.getBlue();
	        }
	    }
	    int avgR2 = (int) (sumR2 / numPixels);
	    int avgG2 = (int) (sumG2 / numPixels);
	    int avgB2 = (int) (sumB2 / numPixels);

	    // Calculate color difference
	    int diffR = Math.abs(avgR1 - avgR2);
	    int diffG = Math.abs(avgG1 - avgG2);
	    int diffB = Math.abs(avgB1 - avgB2);
	    int totalDiff = diffR + diffG + diffB;

	    return (totalDiff <= tolerance);
	}


	public static boolean comparePixels(int pixel1, int pixel2, int tolerance) {
	    int diffColor = getPixelColorDifference(pixel1, pixel2);
	    return diffColor <= tolerance;
	}

	public static int getPixelColorDifference(int pixel1, int pixel2) {
	    int r1 = (pixel1 >> 16) & 0xff;
	    int g1 = (pixel1 >> 8) & 0xff;
	    int b1 = (pixel1) & 0xff;

	    int r2 = (pixel2 >> 16) & 0xff;
	    int g2 = (pixel2 >> 8) & 0xff;
	    int b2 = (pixel2) & 0xff;

	    int diffR = Math.abs(r1 - r2);
	    int diffG = Math.abs(g1 - g2);
	    int diffB = Math.abs(b1 - b2);
	    return (diffR + diffG + diffB) / 3;
	}

	public static boolean compareAlphas(BufferedImage image1, BufferedImage image2) {
	    largerImage = image1.getWidth() > image2.getWidth() ? image1 : image2;
	    smallerImage = image1.getWidth() <= image2.getWidth() ? image1 : image2;
	    largerImage = resizeImage(largerImage, smallerImage.getWidth(), smallerImage.getHeight());
		    boolean isAlpha = true;
		    for (int x = 0; x < smallerImage.getWidth(); x++) {
		        for (int y = 0; y < smallerImage.getHeight(); y++) {
		            int alpha1 = (smallerImage.getRGB(x, y) >> 24) & 0xff;
		            int alpha2 = (largerImage.getRGB(x, y) >> 24) & 0xff;
		            if (alpha1 != alpha2) {
		                isAlpha = false;
		                System.out.println("x: "+x+" y: "+y);
		               // System.out.println("alpha2: "+y);

		                break;
		            }
		        }
		        if (!isAlpha) {
		            break;
		        }
		    }
		  return isAlpha;
	}
	public static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return null;
	}


    private static boolean cmpSize(BufferedImage image1, BufferedImage image2) {
    	int width1 = image1.getWidth();
    	int width2 = image2.getWidth();
    	int height1 = image1.getHeight();
    	int height2 = image2.getHeight();
    	return width1== width2 && height1 == height2;
    }

    private static BufferedImage resizeImageT(BufferedImage smallerImage, BufferedImage largerImage) {
    	//	boolean compareSizes = cmpSize(image1,image2);
    
    	    BufferedImage resizedLargeImage= null;
    	    try {
				 resizedLargeImage = Thumbnails.of(largerImage)
				        .size(smallerImage.getWidth(), smallerImage.getHeight())
				        .asBufferedImage();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	    return resizedLargeImage;
    }
    
    private static boolean compareAlpha(BufferedImage image1, BufferedImage image2) {
        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                int argb1 = image1.getRGB(x, y);
                int argb2 = image2.getRGB(x, y);
                if ((argb1 & 0xFF000000) != (argb2 & 0xFF000000)) {
                    return false;
                }
            }
        }
        return true;
    }




	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
	    int w = img.getWidth(); 
	    int h = img.getHeight(); 
	    BufferedImage dimg = new BufferedImage(newW, newH, img.getType()); 
	    Graphics2D g = dimg.createGraphics(); 
	    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); 
	    g.drawImage(img, 0, 0, newW, newH, 0, 0, w, h, null); 
	    g.dispose(); 
	    return dimg; 
	}


	

	public static BufferedImage resizeImage(BufferedImage image, int width, int height) {
	    BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2d = resizedImage.createGraphics();
	    g2d.drawImage(image.getScaledInstance(width, height, Image.SCALE_DEFAULT), 0, 0, null);
	    g2d.dispose();
	    return resizedImage;
	}
    
public void ouvrirFichierImage(File selectedFile) {
   JFileChooser fileChooser = new JFileChooser();
   FileNameExtensionFilter filter = new FileNameExtensionFilter(
           "Images", "jpg", "jpeg", "png", "gif", "bmp");
   fileChooser.setFileFilter(filter);
   int result = fileChooser.showOpenDialog(null);
   if (result == JFileChooser.APPROVE_OPTION) {
       String extension = getExtension(selectedFile.getName());
       if (extension != null && filter.accept(selectedFile)) {
           // Le fichier sélectionné est une image avec une extension valide
           System.out.println("ce fichier est une image");
           // Utiliser l'objet BufferedImage ici
       } else {
           // Le fichier sélectionné n'est pas une image avec une extension valide
           // Gérer cette erreur ici
       	System.out.println("Ce fichier n'est pas une image.");
       }
   }
}
public static BufferedImage convertFileToImage(File file) {
   BufferedImage image = null;
   try {
       image = ImageIO.read(file);
   } catch (IOException e) {
       e.printStackTrace();
   }
   return image;
}

public static BufferedImage scale(BufferedImage bi, double scaleValue) { 
      AffineTransform tx = new AffineTransform(); 
      tx.scale(scaleValue, scaleValue); 
      AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR); 
      BufferedImage biNew = new BufferedImage( (int) (bi.getWidth() * scaleValue), 
              (int) (bi.getHeight() * scaleValue), 
              bi.getType()); 
      return op.filter(bi, biNew); 

}
}