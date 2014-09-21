/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.vividsolutions.jts.geom.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;
import org.apache.commons.math3.util.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.RNG;
import org.bytedeco.javacpp.opencv_core.RotatedRect;
import org.bytedeco.javacpp.opencv_core.Scalar;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import org.bytedeco.javacpp.opencv_imgproc.Moments;
import org.slf4j.*;
import static pt.scanner.server.data.Grid.WORLD_REFERENCE;
import pt.scanner.server.util.*;
import static pt.scanner.server.util.Utils.BLACK;
import static pt.scanner.server.util.Utils.point;

public class Contour
{

	public static final Integer MAX_CONTOUR = 30;
	private final static AtomicInteger globalIndex = new AtomicInteger();
	private static final Logger log = LoggerFactory.getLogger(Contour.class);
	private final Mat contour;
	private Map<Corner, Coordinate> corners;
	private final Mat image;
	private Integer index;
	private final Map<Position, Set<Line>> intersections = new HashMap<>();
	private final Integer lineCount;
	private final Map<Position, Set<Line>> mainLines = new HashMap<>();
	private final RotatedRect minRect;
	private final Moments moments;
	private Map<Corner, Coordinate> realCorners;
	private final RNG rng = new RNG();

	public Contour(Mat cont, Mat img)
	{
		this.index = globalIndex.getAndIncrement();
		contour = cont;
		image = img;
		moments = moments(contour);
		minRect = minAreaRect(contour);
		float quadrantLength = FastMath.max(minRect.size().height() / 2f, minRect.size().width() / 2f) * 1.05f;
		Map<Position, Line> guidelines = new HashMap<>();
		Position.quadrants().stream().forEach(p -> mainLines.put(p, new LinkedHashSet<>()));
		Position.quadrants().stream().forEach(p -> guidelines.put(p, new Line(getCentroid(), angle() + p.getPosition(), quadrantLength)));
		Mat lines = new Mat(image.size(), CV_8UC4, BLACK);
		// create auxiliary image with contour
		Mat bg = new Mat(image.size(), CV_8UC1, BLACK);
		opencv_core.MatVector v = new opencv_core.MatVector(1);
		v.put(0, contour);
		drawContours(bg, v, 0, new opencv_core.Scalar(255, 255, 255, 255));
		HoughLinesP(bg, lines, 1, FastMath.PI / 180, 30, 2, 20);
		CvMat source = lines.asCvMat();
		Position.quadrants().stream().forEach(p -> intersections.put(p, new LinkedHashSet<>()));
		lineCount = source.cols();
		IntStream.range(0, source.cols())
				.mapToObj(i -> new Line(source.get(0, i, 0), source.get(0, i, 1), source.get(0, i, 2), source.get(0, i, 3)))
				.map(l -> l.expandedLine(image.size()))
				.forEach(l -> Position.quadrants()
						.stream()
						.filter(q -> guidelines.get(q).intersection(l) != null).forEach(q -> intersections.get(q).add(l)));
		if (log.isDebugEnabled())
		{
			Mat debugImg = new Mat(image.size(), CV_8UC3, BLACK);
			cvtColor(image, debugImg, CV_GRAY2BGR);
			Position.quadrants().stream().forEach(q ->
			{
				intersections.get(q).stream().forEach(l -> line(debugImg, point(l.p0), point(l.p1), q.color()));
				line(debugImg, point(guidelines.get(q).p0), point(guidelines.get(q).p1), q.color());
			});
			MatVector vec = new MatVector(1);
			vec.put(0, contour);
			drawContours(debugImg, vec, 0, new Scalar(255, 255, 255, 255));
			Utils.showImage(debugImg, 100);
		}
		log.debug("Contour {} constructed - {} lines", index, lines.cols());
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
		return Objects.equals(this.index, other.getIndex());
	}

	public final Coordinate getCentroid()
	{
		return new Coordinate(moments.m10() / moments.m00(), moments.m01() / moments.m00());
	}

	public Mat getContour()
	{
		return contour;
	}

	public Coordinate getCorner(Corner p)
	{
		generateCorners();
		return corners.get(p);
	}

	public Mat getImage()
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

	public Map<Corner, Coordinate> getRealCorner(Corner c)
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

	public void sortRelated(int contourIndex, int step, Position pos)
	{
		List<Contour> contours = mainLines(pos).iterator().next().getRelated()
				.stream()
				.sorted((c1, c2) -> (int) (c1.getCentroid().distance(getCentroid()) - c2.getCentroid().distance(getCentroid())))
				.collect(Collectors.toList());
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
					.filter(c -> !mainLines(c.getHorizontal()).isEmpty() && !mainLines(c.getVertical()).isEmpty()
								 && mainLines(c.getHorizontal()).iterator().next().intersection(mainLines(c.getVertical()).iterator().next()) != null)
					.collect(Collectors.toMap(c -> c,
											  c -> new Coordinate(mainLines(c.getHorizontal()).iterator().next()
													  .intersection(mainLines(c.getVertical()).iterator().next()))));
			realCorners = Corner.corners().stream()
					.filter(c -> !mainLines(c.getHorizontal()).isEmpty())
					.filter(c -> !mainLines(c.getVertical()).isEmpty())
					.collect(Collectors.toMap(c -> c, c -> new Coordinate(WORLD_REFERENCE.get(index).get(c))));
		}
	}
}
