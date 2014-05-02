/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import java.util.Arrays;
import java.util.List;

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
}
