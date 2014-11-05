// {LICENSE}
/*
 * Copyright 2013-2014 HeroesGrave and other Paint.JAVA developers.
 * 
 * This file is part of Paint.JAVA
 * 
 * Paint.JAVA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package heroesgrave.paint.tools;

import java.awt.Point;

import heroesgrave.paint.image.Layer;
import heroesgrave.paint.image.change.edit.FloodPathChange;
import heroesgrave.paint.main.Paint;

public class FloodFill extends Tool
{

	private FloodPathChange path;
	
	public FloodFill(String name)
	{
		super(name);
	}

	@Override
	public void onPressed(Layer layer, short x, short y, int button)
	{
		path = new FloodPathChange(new Point(x, y), Paint.main.getColor(button));
		Paint.getDocument().preview(path);
	}

	@Override
	public void onReleased(Layer layer, short x, short y, int button)
	{
		Paint.getDocument().applyPreview();
		path = null;
	}

	@Override
	public void whilePressed(Layer layer, short x, short y, int button)
	{
		if(path.moveTo(x, y))
			Paint.getDocument().repaint();
	}
}