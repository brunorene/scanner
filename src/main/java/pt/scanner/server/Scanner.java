/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bytedeco.javacpp.Loader;
import static org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar.BLACK;
import static org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar.MAGENTA;
import static org.bytedeco.javacpp.helper.opencv_imgproc.cvFindContours;
import static org.bytedeco.javacpp.opencv_core.CV_AA;
import static org.bytedeco.javacpp.opencv_core.CV_WHOLE_SEQ;
import org.bytedeco.javacpp.opencv_core.CvContour;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvInRangeS;
import static org.bytedeco.javacpp.opencv_core.cvLine;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvScalar;
import static org.bytedeco.javacpp.opencv_highgui.CV_LOAD_IMAGE_COLOR;
import static org.bytedeco.javacpp.opencv_highgui.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.CV_LINK_RUNS;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RETR_LIST;
import static org.bytedeco.javacpp.opencv_imgproc.cvContourArea;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.scanner.server.data.Contour;
import static pt.scanner.server.data.Contour.STORAGE;
import pt.scanner.server.data.Line;
import pt.scanner.server.data.Position;
import pt.scanner.server.data.Utils;
import static pt.scanner.server.data.Utils.writeText;

/**
 *
 * @author brsantos
 */
public class Scanner implements Runnable
{

	private static final CvScalar CUT_GRAY = cvScalar(50, 0, 0, 0);
	private static final Logger log = LoggerFactory.getLogger(Scanner.class);

	public static void main(String[] args)
	{
		Scanner scanner = new Scanner("/home/brsantos/MEOCloud/Scanner3D");
		scanner.run();
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
			IplImage initImg = cvLoadImage(String.format("%s/calibration/background.jpg", root), CV_LOAD_IMAGE_COLOR);
			IplImage grayImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvCvtColor(initImg, grayImg, CV_BGR2GRAY);
			IplImage binImg = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvInRangeS(grayImg, BLACK, CUT_GRAY, binImg);
			CvSeq contours = new CvSeq();
			/*
			 * Finding Contours
			 */
			cvFindContours(binImg, STORAGE, contours, Loader.sizeof(CvContour.class), CV_RETR_LIST, CV_LINK_RUNS);
			List<Contour> squares = new LinkedList<>();
			while (contours != null && !contours.isNull())
			{
				if (Double.compare(cvContourArea(contours, CV_WHOLE_SEQ, 1), 20000.0) > 0)
				{
					squares.add(new Contour(contours, binImg));
				}
				contours = contours.h_next();
			}
			/*
			 * Remove biggest contour - area below rotating table
			 */
			squares.sort((s1, s2) -> s1.getLineCount() - s2.getLineCount());
			squares.remove(squares.size() - 1);
			squares.sort((s1, s2) -> s1.getIndex() - s2.getIndex());
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
					Line mean = Line.meanLine(c.mainLines(pos)).expandedLine(initImg.width(), initImg.height());
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
				linesPerContourSet.values().forEach(l -> cvLine(initImg, l.start(), l.end(), Position.colorQuadrants().get(pos), 3, CV_AA, 0));
			});
			/*
			 * Order contours using related main lines 1 - Find edge contours
			 */
			Contour topLeftContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroidCoordinate().distance(new Coordinate(0, 0))
														 - c2.getCentroidCoordinate().distance(new Coordinate(0, 0)))).findFirst().get();
			Contour topRightContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroidCoordinate().distance(new Coordinate(initImg.width() - 1, 0))
														 - c2.getCentroidCoordinate().distance(new Coordinate(initImg.width() - 1, 0)))).findFirst().get();
			Contour bottomLeftContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroidCoordinate().distance(new Coordinate(0, initImg.height() - 1))
														 - c2.getCentroidCoordinate().distance(new Coordinate(0, initImg.height() - 1)))).findFirst().get();
			Contour bottomRightContour = squares
					.stream()
					.sorted((c1, c2) -> (int) Math.round(c1.getCentroidCoordinate().distance(new Coordinate(initImg.width() - 1, initImg.height() - 1))
														 - c2.getCentroidCoordinate().distance(new Coordinate(initImg.width() - 1, initImg.height() - 1)))).findFirst().get();
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
			squares.forEach(c -> writeText(initImg, 2.0, c.getCentroid().x(), c.getCentroid().y(), MAGENTA, c.getIndex().toString()));
//			Utils.showImage(initImg, 640, 480, 40000);
			/*
			 * Debug printing
			 */
//			log.debug(calibrationLines.toString().replaceAll("\\], ", "]\n"));
//			squares.forEach(s -> log.debug("{}",
//										   Corner.corners().stream()
//										   .collect(Collectors.toMap(cn -> cn, cn -> s.getCorner(cn)))));
			
			/*
			 * calibration with laser on
			 */
			IplImage initLaser = cvLoadImage(String.format("%s/calibration/laser.jpg", root), CV_LOAD_IMAGE_COLOR);
			IplImage grayLaser = cvCreateImage(cvGetSize(initImg), IPL_DEPTH_8U, 1);
			cvCvtColor(initLaser, grayLaser, CV_BGR2GRAY);
			IplImage binLaser = cvCreateImage(cvGetSize(initLaser), IPL_DEPTH_8U, 1);
			cvInRangeS(grayLaser, BLACK, CUT_GRAY, binLaser);
			Utils.showImage(binLaser, 640, 480, 40000);
			
			
			/*
			 * Releasing memory
			 */
			squares.forEach(Contour::release);
			cvReleaseImage(initImg);
			cvReleaseImage(binImg);
			STORAGE.release();
		}
		catch (RuntimeException e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
