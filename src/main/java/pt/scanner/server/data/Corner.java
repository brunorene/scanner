/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import java.util.Arrays;
import java.util.List;
import static pt.scanner.server.data.Position.BOTTOM;
import static pt.scanner.server.data.Position.LEFT;
import static pt.scanner.server.data.Position.RIGHT;
import static pt.scanner.server.data.Position.TOP;

public enum Corner
{

	TOP_LEFT(TOP, LEFT),
	TOP_RIGHT(TOP, RIGHT),
	BOTTOM_LEFT(BOTTOM, LEFT),
	BOTTOM_RIGHT(BOTTOM, RIGHT);
	private final Position horizontal;
	private final Position vertical;

	private Corner(Position h, Position v)
	{
		this.horizontal = h;
		this.vertical = v;
	}

	public Position getHorizontal()
	{
		return horizontal;
	}

	public Position getVertical()
	{
		return vertical;
	}

	public static List<Corner> corners()
	{
		return Arrays.asList(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT);
	}
}
