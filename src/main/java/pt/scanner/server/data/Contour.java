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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Contour
{

    private static final Logger log = LoggerFactory.getLogger(Contour.class);
    public static CvMemStorage STORAGE = CvMemStorage.create();

    public static void resetStorage()
    {
        STORAGE = CvMemStorage.create();
    }
    private final CvSeq contour;
    private final List<Point> corners = new ArrayList<>();
    private final IplImage image;
    private final Map<Position, Line> mainLines = new HashMap<>();
    private final CvBox2D minRect;
    private final CvMoments moments = new CvMoments();
    private final Integer lineCount;
    private final Map<Position, Set<Line>> lines = new HashMap<>();

    public Contour(CvSeq cont, IplImage img)
    {
        contour = cont;
        image = cvCreateImage(cvGetSize(img), IPL_DEPTH_8U, 1);
        cvSet(this.image, CvScalar.BLACK);
        cvDrawContours(this.image, contour, CvScalar.WHITE, CvScalar.WHITE, 0, 2, 8);
        cvMoments(image, moments, 1);
        minRect = cvMinAreaRect2(contour, STORAGE);
        float[] c = new float[8];
        cvBoxPoints(minRect, c);
        IntStream.range(0, c.length / 2).forEach(i -> corners.add(new Point(c[i * 2], c[i * 2 + 1])));
        float quadrantLength = Math.max(minRect.size().height() / 2f, minRect.size().width() / 2f) * 1.05f;
        Map<Position, Line> guidelines = new HashMap<>();
        Position.quadrants().stream().forEach(p -> guidelines.put(p, new Line(new Coordinate(getCentroid().x(), getCentroid().y()), angle() + (float) p.getPosition(), quadrantLength)));
        CvSeq found = cvHoughLines2(image, STORAGE, CV_HOUGH_PROBABILISTIC, 1, Math.PI / 180, 50, 50, 10);
        lineCount = found.total();
        Position.quadrants().stream().forEach(p -> lines.put(p, new HashSet<>()));
        IntStream.range(0, found.total())
                .mapToObj(i -> new Line(
                                new CvPoint(cvGetSeqElem(found, i)).position(0),
                                new CvPoint(cvGetSeqElem(found, i)).position(1)))
                .filter(l -> Double.compare(l.angle(), 0.0) != 0)
                .map(l -> l.expandedLine(img.width(), img.height()))
                .forEach(l -> Position.quadrants()
                        .stream()
                        .filter(q -> guidelines.get(q).intersection(l) != null).forEach(q -> lines.get(q).add(l)));
    }

    public Integer getLineCount()
    {
        return lineCount;
    }

    public void joinLines()
    {
        Position.quadrants().forEach(p -> mainLines.put(p, Line.meanLine(lines.get(p))));
    }

    public final float angle()
    {
        return minRect.angle() + (minRect.angle() > 45f ? -90f : (minRect.angle() < -45f ? 90f : 0));
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

    public IplImage getImage()
    {
        return image;
    }

    public Line mainLine(Position p)
    {
        return mainLines.get(p);
    }

    public Point nextCorner(Point curr)
    {
        return corners.get((corners.indexOf(curr) + 1) % corners.size());
    }

    public void release()
    {
        cvReleaseImage(image);
    }

    @Override
    public String toString()
    {
        return String.format("Contour %s", getCentroidCoordinate());
    }
}
