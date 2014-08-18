package pt.scanner.server.addon;

import static org.bytedeco.javacpp.opencv_core.CV_8UC1;

import java.nio.*;
import java.util.*;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.slf4j.*;

/**
 * This is a JavaCV implementation of the Zhang-Suen algorithm for thinning, using a Java BitSet.
 *
 * This is a fairly straight port of the original OpenCV (C++) code by Nashruddin Amin, found here:
 * https://opencv-code.com/quick-tips/implementation-of-thinning-algorithm-in-opencv/ He also has an excellent explanation of the algorithm.
 *
 *
 * The image is first translated into a java BitSet, and then the algorithm is repeatedly run until no more deletions are possible.
 *
 * The true/false BitSet matrix used in the algorithm ---------------------- | | | | | p8 | p1 | p2 | |______|______|______| | | | | | p7 | p0 | p3 |
 * |______|______|______| | | | | | p6 | p5 | p4 | |______|______|______|
 *
 * where p0 is the current index/pixel representation of interest.
 *
 * We apply the matrix to every pixel in the image and calulate if the p0 pixel should be deleted or not.
 *
 * The calculation goes like this: Find m1, m2, A and B: m1: if iter == false -> m1 = p1 && p3 && p5 if iter == true -> m1 = p1 && p3 && p7 m2: if iter == false
 * -> m2 = p3 && p5 && p7 if iter == true -> m2 = p1 && p5 && p7 A: A = the number of 'false' => 'true' transitions, considering p1 -> p2, p2 -> p3, p3 -> p4,
 * ... , p8 -> p1 B: B = is the number of p's that are true in p1, p2, ... , p8
 *
 * Evaluate the Result: Result = (A == 1 && (B >= 2 && B <= 6) && m1 == false && m2 == false)
 *
 * If the Result is true we clear p0 (set it to false, or 0) and slide the matrix to the next index. If the Result is false we leave p0 alone and slide the
 * matrix to the next index.
 *
 * When we can iterate every pixel in the image without finding any Result (any pixels to delete), we are done!
 *
 * @author geir@digitalinferno.com
 */
public class ZhangSuenThinning
{

	private static final Logger log = LoggerFactory.getLogger(ZhangSuenThinning.class);

	/**
	 * Method for thinning the given binary image. This is a non-destructive method.
	 *
	 * @param binaryInputImage Binary image with any range
	 * @return thinned Image
	 */
	public static Mat thinning(final Mat binaryInputImage)
	{
		// The dimension (rows, cols)
		int rows = binaryInputImage.rows();
		int cols = binaryInputImage.cols();
		// Total size of image, since binary image, there should be only 1 channel per coordinate (pixel) ??
		int size = rows * cols;
		// BitSet we will use, representing the image, initially, all "bits" are false
		BitSet imageBits = new BitSet(size);
		// Set up the image bitset to reflect the binary image pixels 1::1.
		// I.e.: if image pixel contain a value, it is set to 1 (true) i the bitset,
		// else it remain at 0 (false)
		ByteBuffer bb = binaryInputImage.getByteBuffer();
		for (int index = 0; index < size; index++)
		{
			if (bb.get(index) != 0x00)
			{
				imageBits.set(index);
			}
		}
		boolean pixelsWasDeleted;
		do
		{
			pixelsWasDeleted = thinningIteration(imageBits, false, rows, cols);
			pixelsWasDeleted = thinningIteration(imageBits, true, rows, cols) || pixelsWasDeleted;
		}
		while (pixelsWasDeleted);
		// Make a copy the original image
		CvMat result = new Mat(rows, cols, CV_8UC1, binaryInputImage.data()).asCvMat();
		// ..and "set" every unset pixel in the copy that will be returned
		for (int index = 0; index < size; index++)
		{
			if (!imageBits.get(index))
			{
				result.put(index, 0.0);
			}
		}
		return new Mat(result);
	}

	/**
	 * Perform one thinning iteration.
	 *
	 * @param imageBits The binary image BitSet representation that is updated with deleted pixels for every iteration
	 * @param iter      A flag for what values to compare with; false -> "even", true -> "odd"
	 * @param rows      The number of rows in the image (aka: y, height)
	 * @param cols      The number of columns in the image (aka: x, width)
	 * @return true if any pixels were marked for deletion, false otherwise
	 */
	private static boolean thinningIteration(BitSet imageBits, boolean iter, int rows, int cols)
	{
		BitSet markerBits = new BitSet(rows * cols);
		BitSet matrix3x3 = new BitSet(9);
		// Find all pixels to be deleted
		int pixsToBeDeleted = 0;
		for (int row = 1; row < rows - 1; row++)
		{ // rows == height == y
			for (int col = 1; col < cols - 1; col++)
			{ // cols == width == x
				if (!imageBits.get((row) * cols + col))
				{
					// The pixel of interest has no value, so there is nothing to delete
					continue;
				}
				matrix3x3.clear();
				matrix3x3.set(1, imageBits.get((row - 1) * cols + col));  // p1
				matrix3x3.set(3, imageBits.get(row * cols + col + 1));  // p3
				matrix3x3.set(5, imageBits.get((row + 1) * cols + col));  // p5
				matrix3x3.set(7, imageBits.get(row * cols + col - 1));  // p7
				// We evaluate m1 and m2 first
				if (matrix3x3.cardinality() == 4)
				{
					// all bits so far is set, m1 and m2 will be true, so no need to go on with this pixel
					continue;
				}
				if (iter)
				{
					// m1 = p1 && p3 && p7, m2 = p1 && p5 && p7
					if ((matrix3x3.get(1) && matrix3x3.get(3) && matrix3x3.get(7))
						|| (matrix3x3.get(1) && matrix3x3.get(5) && matrix3x3.get(7)))
					{
						// either m1 or m2 were true, so no need to look any further
						continue;
					}
				}
				else
				{
					// m1 = p1 && p3 && p5, m2 = p3 && p5 && p7
					if ((matrix3x3.get(1) && matrix3x3.get(3) && matrix3x3.get(5))
						|| (matrix3x3.get(3) && matrix3x3.get(5) && matrix3x3.get(7)))
					{
						// either m1 or m2 were true, so no need to look any further
						continue;
					}
				}
				// m1 and m2 are ok, so set the remaining matrix values
				matrix3x3.set(2, imageBits.get((row - 1) * cols + col + 1));
				matrix3x3.set(4, imageBits.get((row + 1) * cols + col + 1));
				matrix3x3.set(6, imageBits.get((row + 1) * cols + col - 1));
				matrix3x3.set(8, imageBits.get((row - 1) * cols + col - 1));
				// Find the number of bits set for all 8 surrounding pixels
				int B = matrix3x3.cardinality();
				if (B < 2 || B > 6)
				{
					// B does not contain the right amount of bits, so no need to go on
					continue;
				}
				// To calulate A, the false->true transitions, we set p8 "in front" as well
				// (for the p8 -> p1 transition check)
				matrix3x3.set(0, matrix3x3.get(8));
				int A = 0;
				for (int idx = 0; idx < 8; idx++)
				{
					if (!matrix3x3.get(idx) && matrix3x3.get(idx + 1))
					{
						if (++A > 1)
						{
							// no need to check further, A is out of bounds
							break;
						}
					}
				}
				if (A != 1)
				{
					// no need to look any further, A is not 1
					continue;
				}
				// If we have gotten here, all preconditions have been met, and we can mark the
				// pixel at p0 as candidate for deletetion
				markerBits.set(row * cols + col);
				pixsToBeDeleted++;
			}
		}
		// Filter out all deleted pixels from the image
		markerBits.flip(0, cols * rows);
		imageBits.and(markerBits);
		return pixsToBeDeleted > 0;
	}
}
