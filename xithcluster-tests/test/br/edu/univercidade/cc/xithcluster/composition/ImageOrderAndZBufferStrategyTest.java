package br.edu.univercidade.cc.xithcluster.composition;

import static br.edu.univercidade.cc.xithcluster.utils.ArraysUtils.normalize;
import static br.edu.univercidade.cc.xithcluster.utils.AssertExtention.assertPixelBufferRegion;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import br.edu.univercidade.cc.xithcluster.comparators.NearColorComparator;
import br.edu.univercidade.cc.xithcluster.composition.ColorAndAlphaBuffer.Type;
import br.edu.univercidade.cc.xithcluster.utils.ImageUtils;

public class ImageOrderAndZBufferStrategyTest {
	
	private static final int HEIGHT = 120;
	
	private static final int WIDTH = 160;
	
	private static final String[] IMAGES_FILES = {
			"resources/image1.png",
			"resources/image2.png",
			"resources/image3.png"
	};
	
	private static String COMPOSITION_RESULT_FILE = "resources/compositionResult1.png";
	
	private CompositionContext context;
	
	@Before
	public void setUp() throws Exception {
		byte[][] colorAndAlphaBuffersData = new byte[IMAGES_FILES.length][];
		
		int i = 0;
		for (String testImageFilePath : IMAGES_FILES) {
			colorAndAlphaBuffersData[i++] = ImageUtils.readImageDataAsByteArray(testImageFilePath);
		}
		
		float[][] depthBuffersData = new float[IMAGES_FILES.length][];
		i = 0;
		for (String testImageFilePath : IMAGES_FILES) {
			float value = (3 - i) * (0.5f / IMAGES_FILES.length);
			depthBuffersData[i++] = normalize(ImageUtils.readImageDataAsIntArray(testImageFilePath), 1.0f, value);
		}
		
		CompositionContext.setDefaultBufferReadOrderClass(DirectBufferReadOrder.class);
		
		context = CompositionContext.getInstance(WIDTH, HEIGHT);
		
		context.setColorAndAlphaBuffers(ColorAndAlphaBufferList.wrap(colorAndAlphaBuffersData, Type.ARGB));
		context.setDepthBuffers(DepthBufferList.wrap(depthBuffersData));
	}
	
	@Test
	public void testShouldComposeOverlapingShapesCorrectly() throws IOException {
		ImageOrderAndZBufferStrategy compositionStrategy = new ImageOrderAndZBufferStrategy();
		
		compositionStrategy.compose(context);
		
		PixelBuffer actualPixelBuffer = context.getPixelBuffer();
		int[] expectedPixelBufferData = ImageUtils.readImageDataAsIntArray(COMPOSITION_RESULT_FILE);
		PixelBuffer expectedPixelBuffer = new PixelBuffer(WIDTH, HEIGHT, expectedPixelBufferData);
		
		// DEBUG:
		// int[] actualPixelBufferData = actualPixelBuffer.toIntArray();
		// ImageUtils.dumpImageDataToFile(WIDTH, HEIGHT, actualPixelBufferData, "tmp/actual-compositionResult1.png");
		// ImageUtils.dumpImageDataToFile(WIDTH, HEIGHT, expectedPixelBufferData, "tmp/expected-compositionResult1.png");
		
		// Asserting critical pixel regions that must be equal
		NearColorComparator comparator = new NearColorComparator(10000);
		assertPixelBufferRegion(expectedPixelBuffer, actualPixelBuffer, 55, 45, 8, 8, comparator);
		assertPixelBufferRegion(expectedPixelBuffer, actualPixelBuffer, 75, 45, 8, 8, comparator);
	}
	
}
