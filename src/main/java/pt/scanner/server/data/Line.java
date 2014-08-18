/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;


import com.vividsolutions.jts.algorithm.*;
import com.vividsolutions.jts.geom.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.apache.commons.math3.util.*;
import org.bytedeco.javacpp.opencv_core.Size;
import org.slf4j.*;
import pt.scanner.server.util.*;

public class Line extends LineSegment
{

	private final static AtomicInteger globalIndex = new AtomicInteger();
	private static final Logger log = LoggerFactory.getLogger(Line.class);

	public static Line meanLine(Collection<Line> lines)
	{
		if (lines.isEmpty())
		{
			return null;
		}
		if (lines.size() == 1)
		{
			return lines.iterator().next();
		}
		double x0 = lines.stream().mapToDouble(l -> l.p0.x).sum();
		double y0 = lines.stream().mapToDouble(l -> l.p0.y).sum();
		double x1 = lines.stream().mapToDouble(l -> l.p1.x).sum();
		double y1 = lines.stream().mapToDouble(l -> l.p1.y).sum();
		Line mean = new Line(new Coordinate(x0 / lines.size(), y0 / lines.size()),
							 new Coordinate(x1 / lines.size(), y1 / lines.size()));
		return mean;
	}
	private final Integer index;
	private final Set<Contour> related = new HashSet<>();

	public Line(Coordinate start, float angle, float length)
	{
		this(start,
			 new Coordinate(start.x + FastMath.cos(Angle.toRadians(angle)) * length,
							start.y + FastMath.sin(Angle.toRadians(angle)) * length));
	}

	public Line(double x1, double y1, double x2, double y2)
	{
		super(x1, y1, x2, y2);
		index = globalIndex.getAndIncrement();
		normalize();
	}

	public Line(Coordinate p1, Coordinate p2)
	{
		super(p1, p2);
		index = globalIndex.getAndIncrement();
		normalize();
	}

	public void addRelated(Contour c)
	{
		related.add(c);
	}

	@Override
	public double angle()
	{
		return Angle.toDegrees(super.angle());
	}

	public double angleDiff(Line l2)
	{
		double currAngle = FastMath.abs(angle() - l2.angle());
		double currAngle2 = FastMath.abs(180 - FastMath.abs(angle() - l2.angle()));
		return FastMath.min(currAngle, currAngle2);
	}

	public Line expandedLine(Size size)
	{
		Coordinate left = lineIntersection(new Line(0, 0, 0, size.height()));
		Coordinate right = lineIntersection(new Line(size.width(), 0, size.width(), size.height()));
		Coordinate top = lineIntersection(new Line(0, 0, size.width(), 0));
		Coordinate bottom = lineIntersection(new Line(0, size.height(), size.width(), size.height()));
		TreeSet<Coordinate> coords = new TreeSet<>();
		if (left != null && Utils.between(0.0, (double) FastMath.round(left.x),
										  (double) size.width()) && Utils.between(0.0, (double) FastMath.round(left.y), (double) size.height()))
		{
			coords.add(new Coordinate(FastMath.round(left.x), FastMath.round(left.y)));
		}
		if (right != null && Utils.between(0.0, (double) FastMath.round(right.x), (double) size.width()) && Utils.between(0.0, (double) FastMath.round(right.y), (double) size.height()))
		{
			coords.add(new Coordinate(FastMath.round(right.x), FastMath.round(right.y)));
		}
		if (top != null && Utils.between(0.0, (double) FastMath.round(top.x), (double) size.width()) && Utils.between(0.0, (double) FastMath.round(top.y), (double) size.height()))
		{
			coords.add(new Coordinate(FastMath.round(top.x), FastMath.round(top.y)));
		}
		if (bottom != null && Utils.between(0.0, (double) FastMath.round(bottom.x), (double) size.width()) && Utils.between(0.0, (double) FastMath.round(bottom.y), (double) size.height()))
		{
			coords.add(new Coordinate(FastMath.round(bottom.x), FastMath.round(bottom.y)));
		}
		Line newLine = new Line(coords.first(), coords.last());
		return newLine;
	}

	public Integer getIndex()
	{
		return index;
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

	@Override
	public String toString()
	{
		return String.format("|%s,%s,%s,%s|%s|", p0.x, p0.y, p1.x, p1.y, getLength());
	}
}
