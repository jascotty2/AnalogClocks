/**
 * Copyright (C) 2012 Jacob Scott <jascottytechie@gmail.com> Description:
 * general number manipulation functions
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

import java.math.BigInteger;
import java.util.ArrayList;

public class Numbers {

	public static char[] baseChars = new char[]{
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
		'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
		'U', 'V', 'W', 'Y', 'Z'};
	// Binary	    Base   2
	// Ternary	    Base   3
	// Quaternary	Base   4
	// Quinary	    Base   5
	// Senary	    Base   6
	// Septenary	Base   7
	// Octal	    Base   8
	// Nonary	    Base   9
	// Decimal / Denary	    Base 10
	// Undenary	    Base 11
	// Duodenary	Base 12
	// Hexadecimal	Base 16
	// Sexagesimal	Base 60

	public static String changeBase(double num, int base) {
		if (base < 1) {
			throw new IllegalArgumentException("Base cannot be less than 1");
		} else if (base > baseChars.length) {
			throw new IllegalArgumentException(String.format("Base Greater Than %d Undefined", baseChars.length));
		}
		boolean neg = num < 0;
		num = Math.abs(num);
		// convert to new base
		StringBuilder num2 = new StringBuilder();
		int intVal = (int) Math.floor(num);
		double decVal = num - intVal;
		while (intVal > 0) {
			int ni = intVal % base;
			intVal /= base;
			if (base == 1) {
				--intVal;
			}
			num2.insert(0, baseChars[ni]);
		}
		if (neg) {
			num2.insert(0, "-");
		}
		if (decVal != 0) {
			num2.append(".");
//			// to prevent infinitely long decimals, 
//			// limit to the # from the original
			//String n = String.valueOf(num);
			//int maxDec = (n.length() - 1 - n.indexOf(".")) * 2;
			int maxDec = 12;
			while (decVal != 0 && maxDec > 0) {
				double nd = decVal * base;
				decVal = nd - Math.floor(nd);
				if (decVal < 0 || nd < 0) {
					decVal = 0;
				} else {
					num2.append(baseChars[(int) nd]);
				}
				--maxDec;
			}
		}
		return num2.length() == 0 ? "0" : num2.toString();
	}

	public static String changeBase(String num, int fromBase, int toBase) {
		if (fromBase < 1) {
			throw new IllegalArgumentException("Base From cannot be less than 1");
		} else if (toBase < 1) {
			throw new IllegalArgumentException("Base To cannot be less than 1");
		} else if (toBase > baseChars.length || fromBase > baseChars.length) {
			throw new IllegalArgumentException(String.format("Base Greater Than %d Undefined", baseChars.length));
		}
		// sanity check
		assertBase(num, fromBase);

		double b10 = 0;
		if (fromBase == 10) {
			b10 = Double.parseDouble(num);
		} else {
			// if not from base 10, convert
			int dec = num.indexOf(".");
			if (dec == -1) {
				dec = num.length();
			}
			for (char c : num.toCharArray()) {
				if (c != '.') {
					int n = 0;
					for (int i = 0; i < fromBase; ++i) {
						if (baseChars[i] == c) {
							n = i;
							break;
						}
					}
					if (n > 0) {
						b10 += n * Math.pow(fromBase, dec - 1);
					}
					dec -= 1;
				}
			}
		}// or String.valueOf((int)b10) 
		return toBase == 10 ? String.format("%.0f", b10) : changeBase(b10, toBase);
	}

	private static void assertBase(String num, int base) {
		boolean dec = false;
		for (char c : num.toCharArray()) {
			boolean good = false;
			if (c == '.') {
				// only one decimal point allowed in a number
				if (!dec) {
					dec = good = true;
				}
			} else {
				for (int i = 0; i < base && !good; ++i) {
					if (baseChars[i] == c) {
						good = true;
					}
				}
			}
			if (!good) {
				throw new IllegalArgumentException(String.format("Number \"%s\"is not in base %d", num, base));
			}
		}
	}

	public static boolean equal(double d1, double d2) {
		return Math.abs(d1 - d2) < 0.0000000000001;
	}

	public static boolean equal(double d1, double d2, double epsilon) {
		return Math.abs(d1 - d2) < epsilon;
	}
	
	private static long fibonacci[] = null; // 93 numbers is the most before a signed long overflows
	public static long getFibonacci(int sequenceIndex) {
		if (sequenceIndex > 0 && sequenceIndex <= 93) {
			if (fibonacci == null) {
				// generate first in seq.
				fibonacci = new long[93];
				fibonacci[0] = 0;
				fibonacci[1] = 1;
				for (int i = 2; i < fibonacci.length; ++i) {
					fibonacci[i] = fibonacci[i - 1] + fibonacci[i - 2];
				}
			}
			return fibonacci[sequenceIndex - 1];
		}
		return -1;
	}
	private static ArrayList<BigInteger> fibonacciLarge = new ArrayList<BigInteger>();
	/**
	 * if you're needing a number past sequence number 93
	 */
	public static BigInteger getFibonacciLarge(int sequenceIndex) {
		if(sequenceIndex <= 0) return null;
		if(sequenceIndex > fibonacciLarge.size()) {
			if(fibonacciLarge.size() < 2) {
				fibonacciLarge.add(BigInteger.valueOf(0));
				fibonacciLarge.add(BigInteger.valueOf(1));
			}
			for(int i = fibonacciLarge.size(); i < sequenceIndex; ++i) {
				BigInteger v = BigInteger.valueOf(0);
				fibonacciLarge.add(v.add(fibonacciLarge.get(i - 1)).add(fibonacciLarge.get(i - 2)));
			}
		}
		return fibonacciLarge.get(sequenceIndex - 1);
	}

	public static Boolean asBoolean(Object o) {
		return o == null ? null
				: (o instanceof Boolean ? ((Boolean) o)
				: (o instanceof Number ? ((Number) o).intValue() != 0
				: (o instanceof String ? ((String) o).equalsIgnoreCase("true") : null)));
	}

	public static Long asLong(Object o) {
		return o == null ? null
				: (o instanceof Number ? ((Number) o).longValue()
				: (o instanceof String ? Long.valueOf((String) o) : null));
	}

	public static Integer asInteger(Object o) {
		return o == null ? null
				: (o instanceof Number ? ((Number) o).intValue()
				: (o instanceof String ? Integer.valueOf((String) o) : null));
	}

	public static Short asShort(Object o) {
		return o == null ? null
				: (o instanceof Number ? ((Number) o).shortValue()
				: (o instanceof String ? Short.valueOf((String) o) : null));
	}

	public static Byte asByte(Object o) {
		return o == null ? null
				: (o instanceof Number ? ((Number) o).byteValue()
				: (o instanceof String ? Byte.valueOf((String) o) : null));
	}

	public static Double asDouble(Object o) {
		return o == null ? null
				: (o instanceof Number ? ((Number) o).doubleValue()
				: (o instanceof String ? Double.valueOf((String) o) : null));
	}

	public static Float asFloat(Object o) {
		return o == null ? null
				: (o instanceof Number ? ((Number) o).floatValue()
				: (o instanceof String ? Float.valueOf((String) o) : null));
	}

	public static Boolean asBoolean(Object o, Boolean err) {
		return o == null ? err
				: (o instanceof Boolean ? ((Boolean) o)
				: (o instanceof Number ? ((Number) o).intValue() != 0
				: (o instanceof String ? ((String) o).equalsIgnoreCase("true") : err)));
	}

	public static Long asLong(Object o, Long err) {
		return o == null ? err
				: (o instanceof Number ? ((Number) o).longValue()
				: (o instanceof String ? Long.valueOf((String) o) : err));
	}

	public static Integer asInteger(Object o, Integer err) {
		return o == null ? err
				: (o instanceof Number ? ((Number) o).intValue()
				: (o instanceof String ? Integer.valueOf((String) o) : err));
	}

	public static Short asShort(Object o, Short err) {
		return o == null ? err
				: (o instanceof Number ? ((Number) o).shortValue()
				: (o instanceof String ? Short.valueOf((String) o) : err));
	}

	public static Byte asByte(Object o, Byte err) {
		return o == null ? err
				: (o instanceof Number ? ((Number) o).byteValue()
				: (o instanceof String ? Byte.valueOf((String) o) : err));
	}

	public static Double asDouble(Object o, Double err) {
		return o == null ? err
				: (o instanceof Number ? ((Number) o).doubleValue()
				: (o instanceof String ? Double.valueOf((String) o) : err));
	}

	public static Float asFloat(Object o, Float err) {
		return o == null ? err
				: (o instanceof Number ? ((Number) o).floatValue()
				: (o instanceof String ? Float.valueOf((String) o) : err));
	}
}
