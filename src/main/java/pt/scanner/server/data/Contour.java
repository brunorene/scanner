/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import static com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_core.CvBox2D;
import com.googlecode.javacv.cpp.opencv_core.CvMemStorage;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import com.googlecode.javacv.cpp.opencv_imgproc.CvMoments;
import com.vividsolutions.jts.geom.Coordinate;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Contour implements Comparable<Contour>
{

	private static final Logger log = LoggerFactory.getLogger(Contour.class);
	public static CvMemStorage STORAGE = CvMemStorage.create();

	public static void resetStorage()
	{
		STORAGE = CvMemStorage.create();
	}
	private final CvSeq contour;
	private final List<Point> corners = new ArrayList<>();
	private final Map<Position, Line> guidelines = new HashMap<>();
	private final IplImage image;
	private final Map<Position, List<Line>> lines = new HashMap<>();
	private final Map<Position, Line> mainLines = new HashMap<>();
	private final CvBox2D minRect;
	private final CvMoments moments = new CvMoments();

	public Contour(CvSeq cont, IplImage img)
	{
		contour = cont;
		image = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1);
		cvSet(this.image, CvScalar.BLACK);
		cvDrawContours(this.image, contour, CvScalar.WHITE, CvScalar.WHITE, 0, 2, 8);
		cvMoments(image, moments, 1);
		Position.quadrants().stream().forEach(p -> lines.put(p, new ArrayList<>()));
		minRect = cvMinAreaRect2(contour, STORAGE);
		float[] c = new float[8];
		cvBoxPoints(minRect, c);
		IntStream.range(0, c.length / 2).forEach(i -> corners.add(new Point(c[i * 2], c[i * 2 + 1])));
		float quadrantLength = Math.max(minRect.size().height() / 2f, minRect.size().width() / 2f) * 1.05f;
		Position.quadrants().stream().forEach(p -> guidelines.put(p, new Line(getCentroid().x(), getCentroid().y(), angle() + (float) p.getPosition(), quadrantLength)));
	}

	public void addLine(Line l)
	{
		l.expand(getImage().width(), getImage().height());
		for (Entry<Position, Line> e : guidelines.entrySet())
		{
			if (e.getValue().intersection(l) != null)
			{
				lines.get(e.getKey()).add(l);
				mainLines.put(e.getKey(), Line.meanLine(image, lines.get(e.getKey())));
				break;
			}
		}
	}

	public final float angle()
	{
		if (minRect.angle() > 45f)
		{
			return minRect.angle() - 90f;
		}
		if (minRect.angle() < -45f)
		{
			return minRect.angle() + 90f;
		}
		return minRect.angle();
	}

	@Override
	public int compareTo(Contour o)
	{
		return Double.compare(cvContourArea(contour, CV_WHOLE_SEQ, 1), cvContourArea(o.getContour(), CV_WHOLE_SEQ, 1));
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

	public List<Point> getCorners()
	{
		return corners;
	}

	public Line getGuideline(Position pos)
	{
		return guidelines.get(pos);
	}

	public IplImage getImage()
	{
		return image;
	}

	public List<Line> getLines(Position pos)
	{
		return lines.get(pos);
	}

	public Point nextCorner(Point curr)
	{
		return corners.get((corners.indexOf(curr) + 1) % corners.size());
	}

	public void release()
	{
		cvReleaseImage(image);
	}
}
