/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.main;

import com.google.common.base.Strings;
import com.vividsolutions.jts.geom.*;
import java.nio.*;
import java.util.*;
import java.util.stream.*;
import org.apache.commons.math3.util.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_core.Size;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_highgui.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_highgui.imread;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import org.slf4j.*;
import pt.scanner.server.addon.*;
import pt.scanner.server.data.*;
import pt.scanner.server.main.Scanner;
import pt.scanner.server.util.*;
import static pt.scanner.server.util.Utils.*;

/**
 *
 * @author brsantos
 */
public class Scanner implements Runnable
{

	private static final Logger log = LoggerFactory.getLogger(Scanner.class);

	public static void main(String[] args)
	{
		Scanner scanner = new Scanner("../..");
		while (true)
		{
			scanner.run();
		}
	}
	private final String root;

	public Scanner(String root)
	{
		this.root = root;
	}

	@Override
	public void run()
	{
		try
		{
			final Mat grayImg = imread(String.format("%s/calibration/background.jpg", root), CV_LOAD_IMAGE_GRAYSCALE);
			Utils.showImage(grayImg, 1000);
			Mat binImg = new Mat(grayImg.size(), CV_8UC1, BLACK);
			threshold(grayImg, binImg, 100, 255, CV_THRESH_BINARY);
			Utils.showImage(binImg, 1000);
			ByteBuffer buffer = binImg.getByteBuffer();
			IntStream.range(0, binImg.rows()).forEach(r ->
			{
				if (r < 2 || r > binImg.rows() - 3)
				{
					IntStream.range(0, binImg.cols()).forEach(c -> buffer.put(r * binImg.cols() + c, (byte) 255));
				}
				else
				{
					buffer.put(r * binImg.cols(), (byte) 255);
					buffer.put(r * binImg.cols() + binImg.cols() - 1, (byte) 255);
				}
			});
			// it is reusing the buffer - do not release
			Mat binImg2 = new Mat(binImg.rows(), binImg.cols(), CV_8UC1, new BytePointer(buffer));
			Utils.showImage(binImg2, 1000);
			/*
			 * Finding Contours
			 */
			MatVector contours = new MatVector();
			findContours(binImg2, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
			log.info("{}", contours.size());
			List<Contour> squares = LongStream.range(0, contours.size()).filter(i -> Double.compare(contourArea(contours.get(i)), 20000.0) > 0)
					.mapToObj(i -> contours.get(i))
					.sorted((c1, c2) -> Double.compare(contourArea(c1), contourArea(c2)))
					.limit(30)
					.map(c -> new Contour(c, binImg2))
					.collect(Collectors.toList());
			Mat bg = new Mat(binImg2.size(), CV_8UC1, BLACK);
			squares.forEach(c ->
			{
				log.info("contour area {}", contourArea(c.getContour()));
				MatVector v = new MatVector(1);
				v.put(0, c.getContour());
				drawContours(bg, v, 0, new Scalar(255, 255, 255, 255));
				Utils.showImage(bg, 1000);
			});
			/*
			 * Relating contours using nearness & position between lines
			 */
			squares.forEach(s -> s.joinLines());
			Position.quadrants().forEach(pos ->
			{
				squares.forEach(c1 ->
				{
					Set<Line> l1 = c1.mainLines(pos);
					if (!l1.isEmpty())
					{
						Line f1 = l1.iterator().next();
						squares.forEach(c2 ->
						{
							Set<Line> l2 = c2.mainLines(pos);
							if (!l2.isEmpty())
							{
								Line f2 = l2.iterator().next();
								if (!c1.equals(c2) && !l1.isEmpty() && !l2.isEmpty()
									&& ((f1.intersection(f2) != null && f1.angleDiff(f2) < 15)
										|| (f1.intersection(f2) == null && f1.distancePerpendicular(f2.midPoint()) < 15)))
								{
									f1.addRelated(c2);
									f2.addRelated(c1);
									l1.add(f2);
									l2.add(f1);
								}
							}
						});
					}
				});
			});
			Map<Position, Map<String, List<Line>>> meanLines = new HashMap<>();
			Map<Position, Map<String, Line>> calibrationLines = new HashMap<>();
			/*
			 * Creating final main line for each position on each contour
			 */
			Position.quadrants().forEach(pos ->
			{
				meanLines.put(pos, new HashMap<>());
				squares.stream().filter(c -> (c.mainLines(pos).size() > 0)).forEach(c ->
				{
					Line mean = Line.meanLine(c.mainLines(pos)).expandedLine(grayImg.size());
					Set<Contour> related = c.mainLines(pos).stream().map(l -> l.getRelated()).flatMap(con -> con.stream()).collect(Collectors.toSet());
					mean.getRelated().addAll(related);
					String key = related.stream().map(r -> r.getIndex()).sorted().map(r -> r.toString()).reduce((s1, s2) -> s1 + "|" + s2).get();
					if (!meanLines.get(pos).containsKey(key))
					{
						meanLines.get(pos).put(key, new ArrayList<>());
					}
					log.debug("{} {} {}", pos, c, c.mainLines(pos));
					c.mainLines(pos).clear();
					c.mainLines(pos).add(mean);
					meanLines.get(pos).get(key).add(mean);
				});
				Map<String, Line> linesPerContourSet = meanLines.get(pos).entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> Line.meanLine(e.getValue())));
				calibrationLines.put(pos, linesPerContourSet);
				linesPerContourSet.values().forEach(l -> line(grayImg, point(l.p0), point(l.p1), pos.color()));
			});
			/*
			 * Order contours using related main lines 1 - Find edge contours
			 */
			Contour topLeftContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroid().distance(new Coordinate(0, 0))
														 - c2.getCentroid().distance(new Coordinate(0, 0)))).findFirst().get();
			Contour topRightContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroid().distance(new Coordinate(grayImg.cols() - 1, 0))
														 - c2.getCentroid().distance(new Coordinate(grayImg.cols() - 1, 0)))).findFirst().get();
			Contour bottomLeftContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroid().distance(new Coordinate(0, grayImg.cols() - 1))
														 - c2.getCentroid().distance(new Coordinate(0, grayImg.cols() - 1)))).findFirst().get();
			Contour bottomRightContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroid().distance(new Coordinate(grayImg.cols() - 1, grayImg.rows() - 1))
														 - c2.getCentroid().distance(new Coordinate(grayImg.cols() - 1, grayImg.rows() - 1)))).findFirst().get();
			/*
			 * Order by region using related contours from main lines
			 */
			topLeftContour.sortRelated(0, 3, Position.RIGHT);
			topLeftContour.mainLines(Position.RIGHT).iterator().next().getRelated().forEach(c -> c.sortRelated(c.getIndex(), 1, Position.BOTTOM));
			topRightContour.sortRelated(12, 3, Position.LEFT);
			topRightContour.mainLines(Position.RIGHT).iterator().next().getRelated().forEach(c -> c.sortRelated(c.getIndex(), 1, Position.BOTTOM));
			bottomLeftContour.sortRelated(24, 1, Position.TOP);
			bottomLeftContour.mainLines(Position.TOP).iterator()
					.next()
					.getRelated()
					.stream().filter(c -> c.getIndex() == 25)
					.findFirst().get().sortRelated(25, 1, Position.RIGHT);
			bottomRightContour.sortRelated(27, 1, Position.TOP);
			bottomRightContour.mainLines(Position.TOP).iterator()
					.next()
					.getRelated()
					.stream().filter(c -> c.getIndex() == 28)
					.findFirst().get().sortRelated(28, 1, Position.LEFT);
			squares.forEach(c -> writeText(grayImg, 2.0,
										   (int) FastMath.round(c.getCentroid().x),
										   (int) FastMath.round(c.getCentroid().y),
										   new Scalar(255, 0, 255, 0), c.getIndex().toString()));
			/*
			 * Debug printing
			 */
			log.debug(calibrationLines.toString().replaceAll("\\], ", "]\n"));
			squares.forEach(s -> log.debug("{}",
										   Corner.corners().stream()
										   .filter(cn -> s.getCorner(cn) != null)
										   .collect(Collectors.toMap(cn -> cn, cn -> s.getCorner(cn)))));
			/*
			 * calibration with laser on
			 */
			IntStream.range(-1, 128)
					.mapToObj(i -> Integer.toString(i))
					.map(i -> i.equals("-1")
							  ? imread(String.format("%s/calibration/laser.jpg", root), CV_LOAD_IMAGE_GRAYSCALE)
							  : imread(String.format("%s/imagens/capt%s.jpg", root, Strings.padStart(i, 3, '0')), CV_LOAD_IMAGE_GRAYSCALE)
					).forEach(img ->
							{
								Utils.showImage(img, 1000);
								log.info("dilating");
								Mat dilatedLaser = new Mat(img.size(), CV_8UC1, BLACK);
								Mat kernel = getStructuringElement(MORPH_RECT, new Size(3, 3), new opencv_core.Point(1, 1));
								dilate(img, dilatedLaser, kernel);
								Utils.showImage(dilatedLaser, 1000);
								log.info("blurring");
								Mat blurredLaser = new Mat(img.size(), CV_8UC3, BLACK);
								Mat blurredLaser2 = new Mat(img.size(), CV_8UC3, BLACK);
								medianBlur(img, blurredLaser, 15);
								Utils.showImage(blurredLaser, 1000);
								blur(blurredLaser, blurredLaser2, new Size(15, 15));
								Utils.showImage(blurredLaser2, 1000);
								log.info("binary Image");
								Mat binLaser = new Mat(img.size(), CV_8UC1, BLACK);
								threshold(blurredLaser, binLaser, 50, 255, CV_THRESH_BINARY);
								Utils.showImage(binLaser, 1000);
								log.info("Thinning");
								Mat thinned = ZhangSuenThinning.thinning(binLaser);
								Utils.showImage(thinned, 1000);
					});
			Thread.sleep(20000);
		}
		catch (RuntimeException | InterruptedException e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
