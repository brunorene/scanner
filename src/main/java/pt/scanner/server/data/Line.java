/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

/**
 *
 * @author brsantos
 */
public class Line extends LineSegment {

	public Line(CvPoint pt1, CvPoint pt2) {
		super();
		if (pt1.x() < pt2.x()) {
			setCoordinates(new Coordinate(pt1.x(), pt1.y()), new Coordinate(pt2.x(), pt2.y()));
		} else if (pt1.x() > pt2.x()) {
			setCoordinates(new Coordinate(pt2.x(), pt2.y()), new Coordinate(pt1.x(), pt1.y()));
		} else if (pt1.y() < pt2.y()) {
			setCoordinates(new Coordinate(pt1.x(), pt1.y()), new Coordinate(pt2.x(), pt2.y()));
		} else {
			setCoordinates(new Coordinate(pt2.x(), pt2.y()), new Coordinate(pt1.x(), pt1.y()));
		}
	}

	public CvPoint start() {
		return new CvPoint((int) Math.round(p0.x), (int) Math.round(p0.y));
	}

	public CvPoint end() {
		return new CvPoint((int) Math.round(p1.x), (int) Math.round(p1.y));
	}
}
