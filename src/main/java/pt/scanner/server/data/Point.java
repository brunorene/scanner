/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.vividsolutions.jts.geom.Coordinate;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import static org.bytedeco.javacpp.opencv_core.cvPoint;

public class Point
{

	private float x;
	private float y;

	public Point(Coordinate c)
	{
		this.x = (float) c.x;
		this.y = (float) c.y;
	}

	public Point(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	public Coordinate asCoordinate()
	{
		return new Coordinate(x, y);
	}

	public CvPoint asCvPoint()
	{
		return cvPoint(Math.round(x), Math.round(y));
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null || getClass() != obj.getClass())
		{
			return false;
		}
		final Point other = (Point) obj;
		if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x))
		{
			return false;
		}
		return Float.floatToIntBits(this.y) == Float.floatToIntBits(other.y);
	}

	public float getX()
	{
		return x;
	}

	public void setX(float x)
	{
		this.x = x;
	}

	public float getY()
	{
		return y;
	}

	public void setY(float y)
	{
		this.y = y;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 17 * hash + Float.floatToIntBits(this.x);
		hash = 17 * hash + Float.floatToIntBits(this.y);
		return hash;
	}
}
