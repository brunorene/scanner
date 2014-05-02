/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import java.util.*;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Line extends LineSegment
{

	private static final Logger log = LoggerFactory.getLogger(Line.class);

	public static Line meanLine(IplImage img, List<Line> lines)
	{
		if (lines.size() == 1)
		{
			return lines.get(0);
		}
		List<Coordinate> coords = new ArrayList<>();
		IntStream.range(0, lines.size() - 1).forEach(i -> coords.add(lines.get(i).lineIntersection(lines.get(i + 1))));
		Coordinate coord = new Coordinate(coords.stream().map(c -> c.x).reduce((a, b) -> a + b).get() / coords.size(),
				coords.stream().map(c -> c.y).reduce((a, b) -> a + b).get() / coords.size());
		float angle = lines.stream().map(l -> (float) l.angle()).reduce((a1, a2) -> a1 + a2).get() / lines.size();
		Line mean = new Line(coord, angle, 1000);
		mean.expand(img.width(), img.height());
		return mean;
	}

	public Line(Coordinate start, float angle, float length)
	{
		this(Math.round((float) start.x), Math.round((float) start.y), angle, length);
	}

	public Line(CvPoint start, float angle, float length)
	{
		this(start.x(), start.y(), angle, length);
	}

	public Line(int x1, int y1, float angle, float length)
	{
		this(x1, y1,
				Math.round(x1 + (float) Math.cos(Angle.toRadians(angle)) * length),
				Math.round(y1 + (float) Math.sin(Angle.toRadians(angle)) * length));
	}

	public Line(CvPoint pt1, CvPoint pt2)
	{
		this(pt1.x(), pt1.y(), pt2.x(), pt2.y());
	}

	public Line(int x1, int y1, int x2, int y2)
	{
		super();
		setCoordinates(new Coordinate(x1, y1), new Coordinate(x2, y2));
		normalize();
	}

	public CvPoint end()
	{
		return new CvPoint((int) Math.round(p1.x), (int) Math.round(p1.y));
	}

	public void expand(int width, int height)
	{
		Coordinate left = lineIntersection(new LineSegment(0, 0, 0, height));
		Coordinate right = lineIntersection(new LineSegment(width, 0, width, height));
		Coordinate top = lineIntersection(new LineSegment(0, 0, width, 0));
		Coordinate bottom = lineIntersection(new LineSegment(0, height, width, height));
		List<Coordinate> coords = new ArrayList<>();
		if (left != null && Utils.between(0.0, left.x, (double) width) && Utils.between(0.0, left.y, (double) height))
		{
			coords.add(left);
		}
		if (right != null && Utils.between(0.0, right.x, (double) width) && Utils.between(0.0, right.y, (double) height))
		{
			coords.add(right);
		}
		if (top != null && Utils.between(0.0, top.x, (double) width) && Utils.between(0.0, top.y, (double) height))
		{
			coords.add(top);
		}
		if (bottom != null && Utils.between(0.0, bottom.x, (double) width) && Utils.between(0.0, bottom.y, (double) height))
		{
			coords.add(bottom);
		}
		setCoordinates(coords.get(0), coords.get(1));
		normalize();
	}

	@Override
	public Coordinate lineIntersection(LineSegment line)
	{
		Coordinate c = super.lineIntersection(line);
		if (c == null)
		{
			return null;
		}
		c.x = Double.compare(c.x, -0.0) == 0 ? 0.0 : c.x;
		c.y = Double.compare(c.y, -0.0) == 0 ? 0.0 : c.y;
		return c;
	}

	public CvPoint start()
	{
		return new CvPoint((int) Math.round(p0.x), (int) Math.round(p0.y));
	}
}
