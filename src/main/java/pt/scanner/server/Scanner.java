/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server;

import com.googlecode.javacpp.Loader;
import static com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_core.CvContour;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_core.CvScalar.*;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.scanner.server.data.Contour;
import static pt.scanner.server.data.Contour.STORAGE;
import pt.scanner.server.data.Position;
import pt.scanner.server.data.Utils;

/**
 *
 * @author brsantos
 */
public class Scanner implements Runnable
{

    private static final CvScalar CUT_GRAY = new CvScalar(50, 0, 0, 0);
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
            // Finding Contours
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
            // remove biggest contour
            squares.sort((s1, s2) -> s1.getLineCount() - s2.getLineCount());
            squares.remove(squares.size() - 1);
            // relating mainLines
            Contour first = squares.get(0);
            squares.forEach(s -> s.joinLines());
            for (Position pos : Position.quadrants())
            {
                for (Contour c1 : squares)
                {
                    for (Contour c2 : squares)
                    {
                        if (!c1.equals(c2)
                                && c1.mainLine(pos) != null
                                && c2.mainLine(pos) != null
                                && c1.mainLine(pos).intersection(c2.mainLine(pos)) != null
                                && !c1.mainLine(pos).getRelated().contains(c2)
                                && !c2.mainLine(pos).getRelated().contains(c1)
                                && Math.abs(c1.mainLine(pos).angle() - c2.mainLine(pos).angle()) < 15)
                        {
                            cvLine(initImg, c1.mainLine(pos).start(), c1.mainLine(pos).end(), Position.colorQuadrants().get(pos), 3, CV_AA, 0);
                            cvLine(initImg, c2.mainLine(pos).start(), c2.mainLine(pos).end(), Position.colorQuadrants().get(pos), 3, CV_AA, 0);
                            c1.mainLine(pos).addRelated(c2);
                            c2.mainLine(pos).addRelated(c1);
                        }
                    }
                }
            }
            squares.forEach(s -> log.info("{}",
                    Position.quadrants().stream()
                    .collect(Collectors.toMap(
                                    q -> q,
                                    q -> Arrays.asList(s.mainLine(q)).stream().filter(l -> l != null).map(l -> l.getRelated().size()).findFirst()))));
            Utils.showImage(initImg, 640, 480);
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
