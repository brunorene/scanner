/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author brsantos
 */
public enum Position
{

	TOP(270),
	LEFT(180),
	BOTTOM(90),
	RIGHT(0),
	TOP_LEFT(315),
	TOP_RIGHT(225),
	BOTTOM_LEFT(135),
	BOTTOM_RIGHT(45);
	private final int position;

	private Position(int position)
	{
		this.position = position;
	}

	public int getPosition()
	{
		return position;
	}

	public static List<Position> quadrants()
	{
		return Arrays.asList(RIGHT, BOTTOM, LEFT, TOP);
	}

	public static Map<Position, CvScalar> colorQuadrants()
	{
		Map<Position, CvScalar> map = new HashMap<>();
		map.put(TOP, CvScalar.BLUE);
		map.put(BOTTOM, CvScalar.RED);
		map.put(LEFT, CvScalar.GREEN);
		map.put(RIGHT, CvScalar.MAGENTA);
		return map;
	}
}
