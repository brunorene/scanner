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
		Double startX = lines.stream().map(l -> l.p0.x).reduce(Double::sum).get();
		Double startY = lines.stream().map(l -> l.p0.y).reduce(Double::sum).get();
		Double endX = lines.stream().map(l -> l.p1.x).reduce(Double::sum).get();
		Double endY = lines.stream().map(l -> l.p1.y).reduce(Double::sum).get();
		Line mean = new Line(new Coordinate(startX / lines.size(), startY / lines.size()), new Coordinate(endX / lines.size(), endY / lines.size()));
		return mean;
	}
	private final Set<Contour> related = new HashSet<>();

	public Line(CvPoint start, float angle, float length)
	{
		this(new Coordinate(start.x(), start.y()), angle, length);
	}

	public Line(Coordinate coord, float angle, float length)
	{
		this(new Coordinate((float) coord.x, (float) coord.y),
				new Coordinate(Math.round(coord.x + (float) Math.cos(Angle.toRadians(angle)) * length),
						Math.round(coord.y + (float) Math.sin(Angle.toRadians(angle)) * length)));
	}

	public Line(CvPoint pt1, CvPoint pt2)
	{
		this(new Coordinate(pt1.x(), pt1.y()), new Coordinate(pt2.x(), pt2.y()));
	}

	public Line(Coordinate coord1, Coordinate coord2)
	{
		super();
		setCoordinates(coord1, coord2);
		normalize();
	}

	public void addRelated(Contour c)
	{
		related.add(c);
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

	public Set<Contour> getRelated()
	{
		return related;
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

	@Override
	public String toString()
	{
		return String.format("Line(%s,%s - %s related)", getCoordinate(0), getCoordinate(1), getRelated().size());
	}
}
