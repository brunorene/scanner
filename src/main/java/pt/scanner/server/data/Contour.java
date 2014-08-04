/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.bytedeco.javacpp.helper.opencv_core.cvDrawContours;
import org.bytedeco.javacpp.opencv_core.CvBox2D;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_core.cvSet;
import static org.bytedeco.javacpp.opencv_imgproc.CV_HOUGH_PROBABILISTIC;
import org.bytedeco.javacpp.opencv_imgproc.CvMoments;
import static org.bytedeco.javacpp.opencv_imgproc.cvBoxPoints;
import static org.bytedeco.javacpp.opencv_imgproc.cvHoughLines2;
import static org.bytedeco.javacpp.opencv_imgproc.cvMinAreaRect2;
import static org.bytedeco.javacpp.opencv_imgproc.cvMoments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static pt.scanner.server.data.Grid.WORLD_REFERENCE;

public class Contour
{

	public static final Integer MAX_CONTOUR = 30;
	public static CvMemStorage STORAGE = CvMemStorage.create();
	private static Integer globalIndex = 0;
	private static final Logger log = LoggerFactory.getLogger(Contour.class);

	public static void resetStorage()
	{
		STORAGE = CvMemStorage.create();
	}
	private final CvSeq contour;
	private Map<Corner, Point> corners;
	private final IplImage image;
	private Integer index;
	private final Map<Position, Set<Line>> intersections = new HashMap<>();
	private final Integer lineCount;
	private final Map<Position, Set<Line>> mainLines = new HashMap<>();
	private final CvBox2D minRect;
	private final CvMoments moments = new CvMoments();
	private Map<Corner, Point> realCorners;

	public Contour(CvSeq cont, IplImage img)
	{
		this.index = globalIndex++;
		contour = cont;
		image = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1);
		cvSet(this.image, CvScalar.BLACK);
		cvDrawContours(this.image, contour, CvScalar.WHITE, CvScalar.WHITE, 0, 2, 8);
		cvMoments(image, moments, 1);
		minRect = cvMinAreaRect2(contour, STORAGE);
		float[] c = new float[8];
		cvBoxPoints(minRect, c);
		float quadrantLength = Math.max(minRect.size().height() / 2f, minRect.size().width() / 2f) * 1.05f;
		Map<Position, Line> guidelines = new HashMap<>();
		Position.quadrants().stream().forEach(p -> mainLines.put(p, new LinkedHashSet<>()));
		Position.quadrants().stream().forEach(p -> guidelines.put(p, new Line(new Coordinate(getCentroid().x(),
																							 getCentroid().y()), angle()
																												 + p.getPosition(),
																			  quadrantLength)));
		CvSeq found = cvHoughLines2(image, STORAGE, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 50, 50, 10);
		lineCount = found.total();
		Position.quadrants().stream().forEach(p -> intersections.put(p, new LinkedHashSet<>()));
		IntStream.range(0, found.total())
				.mapToObj(i -> new Line(
								new CvPoint(cvGetSeqElem(found, i)).position(0),
								new CvPoint(cvGetSeqElem(found, i)).position(1)))
				.filter(l -> Double.compare(l.angle(), 0.0) != 0)
				.map(l -> l.expandedLine(img.width(), img.height()))
				.forEach(l -> Position.quadrants()
						.stream()
						.filter(q -> guidelines.get(q).intersection(l) != null).forEach(q -> intersections.get(q).add(l)));
	}

	public final float angle()
	{
		return minRect.angle() + (minRect.angle() > 45f ? -90f : (minRect.angle() < -45f ? 90f : 0));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final Contour other = (Contour) obj;
		return Objects.equals(this.index, other.index);
	}

	public final CvPoint getCentroid()
	{
		CvPoint p = new CvPoint();
		p.x((int) Math.round(moments.m10() / moments.m00()));
		p.y((int) Math.round(moments.m01() / moments.m00()));
		return p;
	}

	public final Coordinate getCentroidCoordinate()
	{
		return new Coordinate(moments.m10() / moments.m00(), moments.m01() / moments.m00());
	}

	public CvSeq getContour()
	{
		return contour;
	}

	public CvPoint getCorner(Corner p)
	{
		generateCorners();
		return corners.get(p).asCvPoint();
	}

	public Coordinate getCornerCoordinate(Corner p)
	{
		generateCorners();
		return corners.get(p).asCoordinate();
	}

	public IplImage getImage()
	{
		return image;
	}

	public Integer getIndex()
	{
		return index;
	}

	public void setIndex(Integer index)
	{
		this.index = index;
	}

	public Integer getLineCount()
	{
		return lineCount;
	}

	public Map<Corner, Point> getRealCorner(Corner c)
	{
		generateCorners();
		return realCorners;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 37 * hash + Objects.hashCode(this.index);
		return hash;
	}

	public void joinLines()
	{
		Position.quadrants().forEach(p ->
		{
			Line mean = Line.meanLine(intersections.get(p));
			if (mean != null)
			{
				mainLines.get(p).add(mean);
				mainLines.get(p).forEach(l -> l.addRelated(this));
			}
		});
		intersections.clear();
	}

	public Set<Line> mainLines(Position p)
	{
		return mainLines.get(p);
	}

	public void release()
	{
		cvReleaseImage(image);
	}

	public void sortRelated(int contourIndex, int step, Position pos)
	{
		List<Contour> contours = mainLines(pos).iterator().next().getRelated()
				.stream()
				.sorted((c1, c2) -> (int) (c1.getCentroidCoordinate().distance(getCentroidCoordinate())
										   - c2.getCentroidCoordinate().distance(getCentroidCoordinate()))).collect(Collectors.toList());
		for (Contour c : contours)
		{
			c.setIndex(contourIndex);
			contourIndex += step;
		}
	}

	@Override
	public String toString()
	{
		return String.format("Contour<%s>", index);
	}

	private void generateCorners()
	{
		if (corners == null)
		{
			corners = Corner.corners().stream()
					.filter(c -> !mainLines(c.getHorizontal()).isEmpty())
					.filter(c -> !mainLines(c.getVertical()).isEmpty())
					.collect(Collectors.toMap(c -> c,
											  c -> new Point(mainLines(c.getHorizontal()).iterator().next()
													  .intersection(mainLines(c.getVertical()).iterator().next()))));
			realCorners = Corner.corners().stream()
					.filter(c -> !mainLines(c.getHorizontal()).isEmpty())
					.filter(c -> !mainLines(c.getVertical()).isEmpty())
					.collect(Collectors.toMap(c -> c, c -> new Point(WORLD_REFERENCE.get(index).get(c))));
		}
	}
}
