/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.scanner.server.data;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static pt.scanner.server.data.Contour.MAX_CONTOUR;
import static pt.scanner.server.data.Corner.BOTTOM_LEFT;
import static pt.scanner.server.data.Corner.BOTTOM_RIGHT;
import static pt.scanner.server.data.Corner.TOP_LEFT;
import static pt.scanner.server.data.Corner.TOP_RIGHT;

/**
 *
 * @author brsantos
 */
public class Grid
{

	public final static List<Map<Corner, Coordinate>> WORLD_REFERENCE = new ArrayList<>();
	
	static
	{
		for (int i = 0; i < MAX_CONTOUR; i++)
		{
			// LEFT SIDE
			WORLD_REFERENCE.addAll(IntStream.range(0, 12).mapToObj(idx ->
			{
				Map<Corner, Coordinate> intersections = new HashMap<>();
				int dx = (idx % 3) * 6;
				int dz = (idx / 3) * 6;
				intersections.put(TOP_LEFT, new Coordinate(18 - dx, 0, 24 - dz));
				intersections.put(TOP_RIGHT, new Coordinate(14 - dx, 0, 24 - dz));
				intersections.put(BOTTOM_LEFT, new Coordinate(18 - dx, 0, 20 - dz));
				intersections.put(BOTTOM_RIGHT, new Coordinate(14 - dx, 0, 20 - dz));
				return intersections;
			}).collect(Collectors.toList()));
			// RIGHT SIDE
			WORLD_REFERENCE.addAll(IntStream.range(0, 12).mapToObj(idx ->
			{
				Map<Corner, Coordinate> intersections = new HashMap<>();
				int dy = (idx % 3) * 6;
				int dz = (idx / 3) * 6;
				intersections.put(TOP_RIGHT, new Coordinate(0, 18 - dy, 24 - dz));
				intersections.put(TOP_LEFT, new Coordinate(0, 14 - dy, 24 - dz));
				intersections.put(BOTTOM_RIGHT, new Coordinate(0, 18 - dy, 20 - dz));
				intersections.put(BOTTOM_LEFT, new Coordinate(0, 14 - dy, 20 - dz));
				return intersections;
			}).collect(Collectors.toList()));
			// BOTTOM SIDE
			Map<Corner, Coordinate> intersections = new HashMap<>();
			intersections.put(TOP_LEFT, new Coordinate(28, 2, 0));
			intersections.put(TOP_RIGHT, new Coordinate(24, 2, 0));
			intersections.put(BOTTOM_LEFT, new Coordinate(28, 6, 0));
			intersections.put(BOTTOM_RIGHT, new Coordinate(24, 6, 0));
			WORLD_REFERENCE.add(intersections); // 24
			intersections = new HashMap<>();
			intersections.put(TOP_LEFT, new Coordinate(22, 2, 0));
			intersections.put(TOP_RIGHT, new Coordinate(18, 2, 0));
			intersections.put(BOTTOM_LEFT, new Coordinate(22, 6, 0));
			intersections.put(BOTTOM_RIGHT, new Coordinate(18, 6, 0));
			WORLD_REFERENCE.add(intersections); // 25
			intersections = new HashMap<>();
			intersections.put(TOP_LEFT, new Coordinate(22, 8, 0));
			intersections.put(TOP_RIGHT, new Coordinate(18, 8, 0));
			intersections.put(BOTTOM_LEFT, new Coordinate(22, 12, 0));
			intersections.put(BOTTOM_RIGHT, new Coordinate(18, 12, 0));
			WORLD_REFERENCE.add(intersections); // 26
			intersections = new HashMap<>();
			intersections.put(TOP_RIGHT, new Coordinate(2, 28, 0));
			intersections.put(TOP_LEFT, new Coordinate(2, 24, 0));
			intersections.put(BOTTOM_RIGHT, new Coordinate(6, 28, 0));
			intersections.put(BOTTOM_LEFT, new Coordinate(6, 24, 0));
			WORLD_REFERENCE.add(intersections); // 27
			intersections = new HashMap<>();
			intersections.put(TOP_LEFT, new Coordinate(2, 18, 0));
			intersections.put(TOP_RIGHT, new Coordinate(2, 22, 0));
			intersections.put(BOTTOM_LEFT, new Coordinate(6, 18, 0));
			intersections.put(BOTTOM_RIGHT, new Coordinate(6, 22, 0));
			WORLD_REFERENCE.add(intersections); // 28
			intersections.put(TOP_LEFT, new Coordinate(8, 18, 0));
			intersections.put(TOP_RIGHT, new Coordinate(8, 22, 0));
			intersections.put(BOTTOM_LEFT, new Coordinate(12, 18, 0));
			intersections.put(BOTTOM_RIGHT, new Coordinate(12, 22, 0));
			WORLD_REFERENCE.add(intersections); // 29
		}
	}
}
