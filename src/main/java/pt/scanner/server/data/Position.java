/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import static pt.scanner.server.util.Utils.BLUE;
import static pt.scanner.server.util.Utils.CYAN;
import static pt.scanner.server.util.Utils.GREEN;
import static pt.scanner.server.util.Utils.RED;
import java.util.*;
import org.bytedeco.javacpp.opencv_core.Scalar;

/**
 *
 * @author brsantos
 */
public enum Position
{

	TOP(270),
	LEFT(180),
	BOTTOM(90),
	RIGHT(0);
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

	// BGR
	public Scalar color()
	{
		switch (this)
		{
			case TOP:
				return RED;
			case BOTTOM:
				return BLUE;
			case LEFT:
				return CYAN;
			default:
				return GREEN;
		}
	}
}
