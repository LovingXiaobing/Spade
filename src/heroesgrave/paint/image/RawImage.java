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

package heroesgrave.paint.image;

import heroesgrave.utils.math.MathUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public class RawImage
{
	public enum MaskMode
	{
		ADD, SUB, XOR, AND
	}
	
	private int[] buffer;
	public final int width, height;
	
	private boolean[] mask;
	
	public RawImage(int width, int height)
	{
		this.width = width;
		this.height = height;
		this.buffer = new int[width * height];
	}
	
	public RawImage(int width, int height, int[] buffer)
	{
		this.width = width;
		this.height = height;
		if(buffer.length != width * height)
			throw new IllegalArgumentException("Buffer length must be `width*height`");
		this.buffer = buffer;
	}
	
	public RawImage(int width, int height, int[] buffer, boolean[] mask)
	{
		this.width = width;
		this.height = height;
		if(buffer.length != width * height || buffer.length != mask.length)
			throw new IllegalArgumentException("Buffer length must be `width*height`");
		this.buffer = buffer;
		this.mask = mask;
	}
	
	// Drawing functions
	
	public void drawLine(int x1, int y1, final int x2, final int y2, final int c)
	{
		final int dx = Math.abs(x2 - x1);
		final int dy = Math.abs(y2 - y1);
		final int sx = (x1 < x2) ? 1 : -1;
		final int sy = (y1 < y2) ? 1 : -1;
		int err = dx - dy;
		do
		{
			drawPixelChecked(x1, y1, c);
			final int e2 = 2 * err;
			if(e2 > -dy)
			{
				err = err - dy;
				x1 = x1 + sx;
			}
			if(e2 < dx)
			{
				err = err + dx;
				y1 = y1 + sy;
			}
		}
		while(!(x1 == x2 && y1 == y2));
		drawPixelChecked(x2, y2, c);
	}
	
	public void drawRect(int x1, int y1, int x2, int y2, final int c)
	{
		// Do the clamping once at the start so we don't have to perform checks when drawing the pixel.
		x1 = MathUtils.clamp(x1, 0, width);
		x2 = MathUtils.clamp(x2, 0, width);
		y1 = MathUtils.clamp(y1, 0, height);
		y2 = MathUtils.clamp(y2, 0, height);
		
		if(mask == null)
		{
			// top
			final int ix = y1 * width;
			Arrays.fill(buffer, ix + x1, ix + x2 + 1, c);
			
			// bottom
			final int jx = y2 * width;
			Arrays.fill(buffer, jx + x1, jx + x2 + 1, c);
			
			for(int i = y1 + 1; i < y2; i++)
			{
				setPixel(x1, i, c);
				setPixel(x2, i, c);
			}
		}
		else
		{
			// top
			final int ix = y1 * width;
			for(int k = ix + x1; k < ix + x2 + 1; k++)
			{
				if(mask[k])
					buffer[k] = c;
			}
			
			// bottom
			final int jx = y2 * width;
			for(int k = jx + x1; k < jx + x2 + 1; k++)
			{
				if(mask[k])
					buffer[k] = c;
			}
			
			for(int i = y1 + 1; i < y2; i++)
			{
				drawPixel(x1, i, c);
				drawPixel(x2, i, c);
			}
		}
	}
	
	public void fillRect(int x1, int y1, int x2, int y2, int c)
	{
		x1 = MathUtils.clamp(x1, 0, width);
		x2 = MathUtils.clamp(x2, 0, width);
		y1 = MathUtils.clamp(y1, 0, height);
		y2 = MathUtils.clamp(y2, 0, height);
		
		if(mask == null)
		{
			for(; y1 <= y2; y1++)
			{
				final int k = y1 * width;
				Arrays.fill(buffer, k + x1, k + x2 + 1, c);
			}
		}
		else
		{
			for(; y1 <= y2; y1++)
			{
				final int offset = y1 * width;
				for(int k = offset + x1; k < offset + x2 + 1; k++)
				{
					if(mask[k])
						buffer[k] = c;
				}
			}
		}
	}
	
	public void drawPixel(int x, int y, int c)
	{
		final int loc = index(x, y);
		if(mask == null || mask[loc])
		{
			buffer[loc] = c;
		}
	}
	
	public void drawPixelChecked(int x, int y, int c)
	{
		if(x < 0 || y < 0 || x >= width || y >= height)
			return;
		final int loc = index(x, y);
		if(mask == null || mask[loc])
		{
			buffer[loc] = c;
		}
	}
	
	// Mask Manipulation
	
	public void clearMask(MaskMode mode)
	{
		switch(mode)
		{
			case ADD:
				Arrays.fill(mask, true);
				break;
			case SUB:
				Arrays.fill(mask, false);
				break;
			case XOR:
				for(int i = 0; i < mask.length; i++)
					mask[i] = !mask[i];
				break;
			case AND:
				break;
		}
	}
	
	public void maskRect(int x1, int y1, int x2, int y2, MaskMode mode)
	{
		x1 = MathUtils.clamp(x1, 0, width);
		x2 = MathUtils.clamp(x2, 0, width);
		y1 = MathUtils.clamp(y1, 0, height);
		y2 = MathUtils.clamp(y2, 0, height);
		
		switch(mode)
		{
			case ADD:
				for(; y1 <= y2; y1++)
				{
					final int k = y1 * width;
					Arrays.fill(mask, k + x1, k + x2 + 1, true);
				}
				break;
			case SUB:
				for(; y1 <= y2; y1++)
				{
					final int k = y1 * width;
					Arrays.fill(mask, k + x1, k + x2 + 1, false);
				}
				break;
			case XOR:
				for(; y1 <= y2; y1++)
				{
					final int offset = y1 * width;
					for(int k = offset + x1; k < offset + x2 + 1; k++)
					{
						mask[k] = !mask[k];
					}
				}
				break;
			case AND:
				// Before Rectangle
				final int offset = y1 * width + x1;
				Arrays.fill(mask, 0, offset, false);
				
				// After Rectangle
				final int offset2 = y2 * width + x2 + 1;
				Arrays.fill(mask, offset2, mask.length, false);
				
				// Within Rectangle
				final int offstep = x1 + width - x2;
				final int onstep = x2 - x1;
				
				for(int i = offset; i < offset2; i += offstep)
					Arrays.fill(mask, i, onstep, false);
				break;
		}
	}
	
	public void setMaskEnabled(boolean enabled)
	{
		if(enabled)
		{
			if(mask == null)
				mask = new boolean[buffer.length];
		}
		else if(mask != null)
		{
			mask = null;
		}
	}
	
	public void toggleMask()
	{
		if(mask == null)
		{
			mask = new boolean[buffer.length];
		}
		else
		{
			mask = null;
		}
	}
	
	public boolean[] copyMask()
	{
		return mask == null ? null : Arrays.copyOf(mask, mask.length);
	}
	
	public boolean[] getMask()
	{
		return mask;
	}
	
	public void setMask(boolean[] mask)
	{
		this.mask = mask;
	}
	
	// Buffer Manipulation
	
	public void clear(int c)
	{
		Arrays.fill(buffer, c);
	}
	
	public void setPixel(int x, int y, int c)
	{
		buffer[index(x, y)] = c;
	}
	
	public int getPixel(int x, int y)
	{
		return buffer[index(x, y)];
	}
	
	private int index(int x, int y)
	{
		return y * width + x;
	}
	
	public int[] copyBuffer()
	{
		return Arrays.copyOf(buffer, buffer.length);
	}
	
	public int[] getBuffer()
	{
		return buffer;
	}
	
	public void setBuffer(int[] buffer)
	{
		this.buffer = buffer;
	}
	
	// Create a RawImage which has direct access to the pixels of the BufferedImage.
	// This could be quite unreliable.
	public static RawImage unwrapBufferedImage(BufferedImage image)
	{
		return new RawImage(image.getWidth(), image.getHeight(), ((DataBufferInt) image.getRaster().getDataBuffer()).getData());
	}
	
	public static RawImage fromBufferedImage(BufferedImage image)
	{
		return new RawImage(image.getWidth(), image.getHeight(), image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth()));
	}
	
	public BufferedImage toBufferedImage()
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, buffer, 0, width);
		return image;
	}
	
	public static RawImage copyOf(RawImage image)
	{
		if(image.mask == null)
			return new RawImage(image.width, image.height, image.copyBuffer());
		return new RawImage(image.width, image.height, image.copyBuffer(), image.copyMask());
	}
	
	public void copyFrom(RawImage image, boolean withMask)
	{
		if(image == this)
		{
			return;
		}
		if(this.buffer.length != image.buffer.length)
			throw new RuntimeException("Cannot copy from a different sized RawImage");
		System.arraycopy(image.buffer, 0, buffer, 0, buffer.length);
		if(withMask)
		{
			if(image.mask == null)
				this.mask = null;
			else if(this.mask == null)
				this.mask = Arrays.copyOf(image.mask, image.mask.length);
			else
				System.arraycopy(image.mask, 0, mask, 0, mask.length);
		}
	}
	
	public void copyMaskFrom(RawImage image)
	{
		if(image == this)
		{
			return;
		}
		if(this.mask.length != image.mask.length)
			throw new RuntimeException("Cannot copy from a different sized RawImage");
		System.arraycopy(image.mask, 0, mask, 0, mask.length);
	}
	
	public void dispose()
	{
		buffer = null;
		mask = null;
	}
}
