/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 * Description: Faster trig functions for use when speed is needed without the need for high precision 
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.libv3.util;

public class FastMath {
	
	// trig function lookup tables, roughly 10x faster than calling math libraries
	
	// making LOOKUP_MAX larger increases the lookup table size and initialization time,
	// but increases accuracy for larger values
	// eg, atan PI/2 = 1.570796
	// 20 = 1.5208
	// 30 = 1.5375
	// 40 = 1.5458
	// 50 = 1.5508
	// 60 = 1.5541
	// 100 = 1.5608
	private final static int ATAN_LOOKUP_MAX = 50;
	private final static double ATAN_ACCURACY = .01;
	private final static double ATAN_ACCURACY_INVERSE = 1 / ATAN_ACCURACY;
	private final static double SIN_ACCURACY = .01;
	private final static double TAN_ACCURACY = .005;
	private final static double TPI = Math.PI * 2;
	private final static double PI2 = Math.PI / 2;
	private final static double PI4 = Math.PI / 4;
	private final static double PI_L = 1.2246467991473532e-16, 
			TWO_60 = 0x1000000000000000L;
	static double[] atanLookupTable = null;
	static double[] tanLookupTable = null;
	static double sinLookupTable[] = null;
	
	public static void initLookupTables() {
		if(atanLookupTable == null) {
			atanLookupTable = new double[(int) Math.ceil(ATAN_LOOKUP_MAX / ATAN_ACCURACY) + 1];
			int i = 0;
			for(double t = 0; t <= ATAN_LOOKUP_MAX + .00001; t += ATAN_ACCURACY) {
				// fix floating point errors
				atanLookupTable[i++] = Math.atan(Math.round(t * ATAN_ACCURACY_INVERSE) / ATAN_ACCURACY_INVERSE);
			}
			
			sinLookupTable = new double[(int) Math.ceil(Math.PI / SIN_ACCURACY) + 1];
			i = 0;
			for(double t = 0; t <= Math.PI + .00001; t += SIN_ACCURACY) {
				sinLookupTable[i++] = Math.sin(t);
			}
			
			// tan = sin / cos
			// [-PI/2, PI/2]
			tanLookupTable = new double[(int) Math.ceil(PI2 / TAN_ACCURACY) + 1];
			i = 0;
			for(double t = 0; t < PI2 + .00001; t += TAN_ACCURACY) {
				// fix floating point errors
				tanLookupTable[i++] = Math.tan(t);
			}
			tanLookupTable[tanLookupTable.length - 1] = Double.POSITIVE_INFINITY;
		}
	}
	
	public static double atan2(double y, double x) {
		if(atanLookupTable == null) {
			initLookupTables();
		}
		// edge case logic stolen from StrictMath
		if (x != x || y != y) {
			return Double.NaN;
		} else if (x == 1) {
			return atan(y);
		}else if (x == Double.POSITIVE_INFINITY) {
			if (y == Double.POSITIVE_INFINITY) return PI4;
			if (y == Double.NEGATIVE_INFINITY) return -PI4;
			return 0 * y;
		} else if (x == Double.NEGATIVE_INFINITY) {
			if (y == Double.POSITIVE_INFINITY) return 3 * PI4;
			if (y == Double.NEGATIVE_INFINITY) return -3 * PI4;
			return (1 / (0 * y) == Double.POSITIVE_INFINITY) ? Math.PI : -Math.PI;
		} else if (y == 0) {
			if (1 / (0 * x) == Double.POSITIVE_INFINITY) return y;
			return (1 / y == Double.POSITIVE_INFINITY) ? Math.PI : -Math.PI;
		} else if (y == Double.POSITIVE_INFINITY || y == Double.NEGATIVE_INFINITY || x == 0) {
			return y < 0 ? -PI2 : PI2;
		}
		// Safe to do y/x.
		double z = Math.abs(y / x);
		if (z > TWO_60) z = PI2 + 0.5 * PI_L;
		else if (x < 0 && z < 1 / TWO_60) z = 0;
		else z = atan(z);
		if (x > 0) return y > 0 ? z : -z;
		return y > 0 ? Math.PI - (z - PI_L) : z - PI_L - Math.PI;
	}
	
	public static double atan(double x) {
		if(atanLookupTable == null) {
			initLookupTables();
		}
		if(x > ATAN_LOOKUP_MAX || x < -ATAN_LOOKUP_MAX) {
			return x < 0 ? -PI2 : PI2;
		}
		int ti = (int) Math.round(x * ATAN_ACCURACY_INVERSE);
		return ti >= 0 ? atanLookupTable[ti] : -atanLookupTable[-ti];
	}
	
	public static double sin(double x) {
		if(atanLookupTable == null) {
			initLookupTables();
		}
		if(x > Math.PI) {
			while(x > Math.PI) x -= TPI;
		} else if(x < -Math.PI){
			while(x < -Math.PI) x += TPI;
		}
		return x < 0 
				? -sinLookupTable[(int) Math.round(-x / SIN_ACCURACY)]
				: sinLookupTable[(int) Math.round(x / SIN_ACCURACY)];
	}
	
	public static double cos(double x) {
		if(atanLookupTable == null) {
			initLookupTables();
		}
		x += PI2;
		if(x > Math.PI) {
			while(x > Math.PI) x -= TPI;
		} else if(x < -Math.PI){
			while(x < -Math.PI) x += TPI;
		}
		return x < 0 
				? -sinLookupTable[(int) Math.round(-x / SIN_ACCURACY)]
				: sinLookupTable[(int) Math.round(x / SIN_ACCURACY)];
		
	}
	
	public static double tan(double x) {
		if(atanLookupTable == null) {
			initLookupTables();
		}
		if(x >= PI2) {
			while(x >= PI2) x -= Math.PI;
		} else if(x <= -PI2){
			while(x <= -PI2) x += Math.PI;
		}
		return x < 0 
				? -tanLookupTable[(int) Math.round(Math.abs(x) / TAN_ACCURACY)]
				: tanLookupTable[(int) Math.round(Math.abs(x) / TAN_ACCURACY)];
	}
}
