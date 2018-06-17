/**
 * Copyright (C) 2011 Jacob Scott <jascottytechie@gmail.com> Description:
 * methods for reading/writing files
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
package me.jascotty2.libv3_2.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileIO {

	/**
	 * this is a quote within a csv string <br /> covers “ ” (8220 - 8221)
	 */
	protected final static char nullQuo = (char) 65533;
	//protected final static String nullQuo = String.valueOf((char) 226) + (char) 128 + String.valueOf((char) 156);
	/**
	 * recognized delimeters
	 */
	protected final static ArrayList<Character> delimeters = new ArrayList<Character>() {
		{
			// fields can be tab, comma, or semicolin -delimited
			add('\t');
			add(',');
			add(';');
		}
	};

	public static List<String[]> loadCSVFile(File toLoad) throws FileNotFoundException, IOException {
		List<String[]> ret = new LinkedList<String[]>();
		if (toLoad.exists() && toLoad.isFile() && toLoad.canRead()) {
			char delim = getDelim(toLoad);
			String line;
			//FileReader fstream = new FileReader(toLoad.getAbsolutePath());
			//BufferedReader in = new BufferedReader(fstream);
			FileInputStream fstream = new FileInputStream(toLoad.getAbsolutePath());
			InputStreamReader inr = new InputStreamReader(fstream, "UTF8");
			BufferedReader in = new BufferedReader(inr);
			
			try {
				if (delim == 0) {
					while ((line = in.readLine()) != null) {
						ret.add(new String[]{line});
					}
				} else {
					boolean inStr = false;
					ArrayList<String> fields = new ArrayList<String>();
					StringBuilder temp = new StringBuilder();
					while ((line = in.readLine()) != null) {
						if(inStr) {
							temp.append("\n");
						} else if(!ret.isEmpty() || fields.size() > 0) {
							// terminating a line
							fields.add(temp.toString());
							temp.delete(0, Integer.MAX_VALUE);
							ret.add(fields.toArray(new String[fields.size()]));
							fields.clear();
						}
						char lastChar = 0;
						for(char c : line.toCharArray()) {
							if(c == '"') {
								inStr = !inStr;
								if(lastChar == '"') {
									// double-quote is a quoted quote
									temp.append('"');
								}
							} else if(!inStr && c == delim) {
								// terminating a field
								fields.add(temp.toString());
								temp.delete(0, Integer.MAX_VALUE);
							} else {
								if (c == nullQuo) {
									temp.append('"');
								} else {
									temp.append(c);
								}
							}
							lastChar = c;
						}
					}
					if(temp.length() > 0) {
						fields.add(temp.toString());
					}
					if(!fields.isEmpty()) {
						ret.add(fields.toArray(new String[fields.size()]));
					}
						
						/*
						if (line.contains("\"")) {
							// need to parse the strings..
							ArrayList<String> fields = new ArrayList<String>();
							boolean inStr = false;
							int start = 0;
							char c;
							for (int i = 0; i < line.length() && (c = line.charAt(i)) != 0; ++i) {
								if (c == '"') {
									inStr = !inStr;
								} else if (!inStr && c == delim) {
									String field = line.substring(start, i);
									if (!field.isEmpty() && field.charAt(0) == '"') {
										field = field.substring(1, field.length() - 1);
									}
									fields.add(field.replace("\"\"", "\"").replace(nullQuo, '"'));
									start = i + 1;
								}
							}
							if (start < line.length()) {
								String field = line.substring(start, line.length());
								if (!field.isEmpty() && field.charAt(0) == '"') {
									field = field.substring(1);
								}
								// just in case is missing the terminating quote
								if (!field.isEmpty() && field.charAt(field.length() - 1) == '"') {
									field = field.substring(0, field.length() - 1);
								}
								fields.add(field.replace("\"\"", "\"").replace(nullQuo, '"'));
							}
							ret.add(fields.toArray(new String[0]));
						/*
						if (line.contains("\"")) {
							// need to parse the strings..
							ArrayList<String> fields = new ArrayList<String>();
							boolean inStr = false;
							int start = 0;
							char c;
							for (int i = 0; i < line.length() && (c = line.charAt(i)) != 0; ++i) {
								if (c == '"') {
									inStr = !inStr;
								} else if (!inStr && c == delim) {
									String field = line.substring(start, i);
									if (!field.isEmpty() && field.charAt(0) == '"') {
										field = field.substring(1, field.length() - 1);
									}
									fields.add(field.replace("\"\"", "\"").replace(nullQuo, '"'));
									start = i + 1;
								}
							}
							if (start < line.length()) {
								String field = line.substring(start, line.length());
								if (!field.isEmpty() && field.charAt(0) == '"') {
									field = field.substring(1);
								}
								// just in case is missing the terminating quote
								if (!field.isEmpty() && field.charAt(field.length() - 1) == '"') {
									field = field.substring(0, field.length() - 1);
								}
								fields.add(field.replace("\"\"", "\"").replace(nullQuo, '"'));
							}
							ret.add(fields.toArray(new String[0]));
						} else {
							// simple substitution :)
							ret.add(line.replace(nullQuo, '"').split(String.valueOf(delim)));
						} */
				}
			} finally {
				in.close();
				fstream.close();
			}
		}
		return ret;
	}

	private static char getDelim(File toLoad) throws IOException {
		FileReader fstream = new FileReader(toLoad);
		int inC;
		char delim = 0;
		try {
			boolean inStr = false;
			while ((inC = fstream.read()) != -1) {
				if (((char) inC) == '"') {
					inStr = !inStr;
				} else if (!inStr && delimeters.contains((Character) (char) inC)) {
					delim = (char) inC;
					break;
				}
			}
			fstream.close();
		} catch (IOException e) {
			fstream.close();
			throw (e);
		}
		return delim;
	}

	public static List<String> loadFile(File toLoad) throws FileNotFoundException, IOException {
		ArrayList<String> ret = new ArrayList<String>();
		if (toLoad != null && toLoad.exists() && toLoad.isFile() && toLoad.canRead()) {
			FileReader fstream = new FileReader(toLoad.getAbsolutePath());
			BufferedReader in = new BufferedReader(fstream);
			try {
				String line;
				while ((line = in.readLine()) != null) {
					ret.add(line);
				}
			} finally {
				in.close();
				fstream.close();
			}
		}
		return ret;
	}

	public static String loadFileFully(File toLoad) throws FileNotFoundException, IOException {
		StringBuilder s = new StringBuilder();
		if (toLoad != null && toLoad.exists() && toLoad.isFile() && toLoad.canRead()) {
			FileReader fstream = new FileReader(toLoad.getAbsolutePath());
			BufferedReader in = new BufferedReader(fstream);
			try {
				char buff[] = new char[2048];
				int len;
				while ((len = in.read(buff)) > 0) {
					s.append(buff, 0, len);
				}
			} finally {
				in.close();
				fstream.close();
			}
		}
		return s.toString();
	}

	public static List<String> loadFileUTF8(File toLoad) throws FileNotFoundException, IOException {
		ArrayList<String> ret = new ArrayList<String>();
		if (toLoad != null && toLoad.exists() && toLoad.isFile() && toLoad.canRead()) {
			//FileReader fstream = new FileReader(toLoad.getAbsolutePath());
			//BufferedReader in = new BufferedReader(fstream);
			FileInputStream fstream = new FileInputStream(toLoad.getAbsolutePath());
			InputStreamReader inr = new InputStreamReader(fstream, "UTF8");
			BufferedReader in = new BufferedReader(inr);
			try {
				boolean line1past = false;
				String line;
				while ((line = in.readLine()) != null) {
					// sometimes utf8 has a weird bit sitting around
					if (!line1past && (line1past = true) && line.length() > 1 && (int) line.charAt(0) == 65279) {
						ret.add(line.substring(1));
					} else {
						ret.add(line);
					}
				}
			} finally {
				in.close();
				inr.close();
				fstream.close();
			}
		}
		return ret;
	}

	public static String loadFileFullyUTF8(File toLoad) throws FileNotFoundException, IOException {
		StringBuilder s = new StringBuilder();
		if (toLoad != null && toLoad.exists() && toLoad.isFile() && toLoad.canRead()) {
			FileInputStream fstream = new FileInputStream(toLoad.getAbsolutePath());
			InputStreamReader inr = new InputStreamReader(fstream, "UTF8");
			BufferedReader in = new BufferedReader(inr);
			try {
				char buff[] = new char[2048];
				boolean line1past = false;
				int len;
				while ((len = in.read(buff)) > 0) {
					if (!line1past && (line1past = true) && (int) buff[0] == 65279) {
						if(len > 1) s.append(buff, 1, len);
					} else {
						s.append(buff, 0, len);
					}
				}
			} finally {
				in.close();
				fstream.close();
			}
//			try {
//				char buff[] = new char[2048];
//				int len;
//				while ((len = in.read(buff)) > 0) {
//					s.append(buff, 0, len);
//				}
//			} finally {
//				in.close();
//				fstream.close();
//			}
		}
		return s.toString();
	}
	
	public static String headCharacters(File toLoad, long chars) throws FileNotFoundException, IOException {
		StringBuilder s = new StringBuilder();
		if (toLoad != null && toLoad.exists() && toLoad.isFile() && toLoad.canRead()) {
			FileInputStream fstream = new FileInputStream(toLoad.getAbsolutePath());
			InputStreamReader inr = new InputStreamReader(fstream, "UTF8");
			BufferedReader in = new BufferedReader(inr);
			try {
				char buff[] = new char[2048];
				boolean line1past = false;
				int len;
				while (chars > 0 && (len = in.read(buff)) > 0) {
					if (!line1past && (line1past = true) && (int) buff[0] == 65279) {
						if(len > 1) s.append(buff, 1, (int) Math.min(chars, len--));
					} else {
						s.append(buff, 0, (int) Math.min(chars, len));
					}
					chars = len > chars ? 0 : chars - len;
				}
			} finally {
				in.close();
				fstream.close();
			}
		}
		return s.toString();
	}
	
	public static String headLines(File toLoad, int lines) throws FileNotFoundException, IOException {
		StringBuilder s = new StringBuilder();
		if (toLoad != null && toLoad.exists() && toLoad.isFile() && toLoad.canRead()) {
			FileInputStream fstream = new FileInputStream(toLoad.getAbsolutePath());
			InputStreamReader inr = new InputStreamReader(fstream, "UTF8");
			BufferedReader in = new BufferedReader(inr);
			try {
				boolean line1past = false;
				String line;
				while (--lines >= 0 && (line = in.readLine()) != null) {
					// sometimes utf8 has a weird bit sitting around
					if (!line1past && (line1past = true) && line.length() > 1 && (int) line.charAt(0) == 65279) {
						s.append(line.substring(1)).append("\n");
					} else {
						s.append(line).append("\n");
					}
				}
			} finally {
				in.close();
				fstream.close();
			}
		}
		return s.toString();
	}
	
	public static void saveFile(File toSave, String data) throws IOException {
		if (toSave == null || data == null) {
			return;
		}
		if (!toSave.exists()) {
			// first check if directory exists, then create the file
			File dir = new File(toSave.getAbsolutePath().substring(0, toSave.getAbsolutePath().lastIndexOf(File.separatorChar)));
			dir.mkdirs();
			toSave.createNewFile();
		}
		FileWriter fstream = new FileWriter(toSave.getAbsolutePath());
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(data);
		out.close();
		fstream.close();
	}

	public static void saveAppendFile(File toSave, String data) throws IOException {
		if (!toSave.exists()) {
			// first check if directory exists, then create the file
			File dir = new File(toSave.getAbsolutePath().substring(0, toSave.getAbsolutePath().lastIndexOf(File.separatorChar)));
			dir.mkdirs();
			toSave.createNewFile();
		}
		FileWriter fstream = new FileWriter(toSave.getAbsolutePath(), true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(data);
		out.close();
		fstream.close();
	}

	public static void saveFile(File toSave, String[] lines) throws IOException {
		if (!toSave.exists()) {
			// first check if directory exists, then create the file
			File dir = new File(toSave.getAbsolutePath().substring(0, toSave.getAbsolutePath().lastIndexOf(File.separatorChar)));
			dir.mkdirs();
			toSave.createNewFile();
		}
		FileWriter fstream = new FileWriter(toSave.getAbsolutePath());
		BufferedWriter out = new BufferedWriter(fstream);
		for (int i = 0; i < lines.length; ++i) {
			out.write(lines[i]);
			if (i + 1 < lines.length) {
				out.newLine();
			}
		}
		out.close();
		fstream.close();
	}

	public static void saveFile(File toSave, ArrayList<String> lines) throws IOException {
		if (!toSave.exists()) {
			// first check if directory exists, then create the file
			File dir = new File(toSave.getAbsolutePath().substring(0, toSave.getAbsolutePath().lastIndexOf(File.separatorChar)));
			dir.mkdirs();
			toSave.createNewFile();
		}
		FileWriter fstream = new FileWriter(toSave.getAbsolutePath());
		BufferedWriter out = new BufferedWriter(fstream);

		Iterator<String> toWrite = lines.iterator();
		while (toWrite.hasNext()) {
			out.write(toWrite.next());
			if (toWrite.hasNext()) {
				out.newLine();
			}
		}
		out.close();
		fstream.close();
	}

	public static void saveFileUTF8(File toSave, String data) throws IOException {
		if (!toSave.exists()) {
			// first check if directory exists, then create the file
			File dir = new File(toSave.getAbsolutePath().substring(0, toSave.getAbsolutePath().lastIndexOf(File.separatorChar)));
			dir.mkdirs();
			toSave.createNewFile();
		}
		FileOutputStream fstream = new FileOutputStream(toSave);
		OutputStreamWriter out = new OutputStreamWriter(fstream, StandardCharsets.UTF_8);
		out.write(data);
		out.close();
		fstream.close();
	}
	
	public static void saveCSVFile(File toSave, ArrayList<String[]> lines) throws IOException {
		if (!toSave.exists()) {
			// first check if directory exists, then create the file
			File dir = new File(toSave.getAbsolutePath().substring(0, toSave.getAbsolutePath().lastIndexOf(File.separatorChar)));
			dir.mkdirs();
			toSave.createNewFile();
		}
		FileWriter fstream = new FileWriter(toSave);
		BufferedWriter out = new BufferedWriter(fstream);
		Iterator<String[]> toWrite = lines.iterator();
		String line[];
		while (toWrite.hasNext()) {
			if ((line = toWrite.next()) == null) {
				continue;
			}
			for (int i = 0; i < line.length; ++i) {
				boolean str = line[i].contains("\"");
				if (!str) {
					for (Character c : delimeters) {
						if (line[i].contains(String.valueOf(c))) {
							str = true;
							break;
						}
					}
				}
				if (str) {
					out.write("\"" + line[i].replace("\"", "\"\"") + "\"");
				} else {
					out.write(line[i].replace('"', nullQuo));
				}
				// slower:
//				if (Pattern.matches(fpRegex, line[i])) { // (fpRegex as defined from Double documentation)
//					out.write(line[i]);
//				} else {
//					out.write("\"" + line[i].replace("\"", "\"\"") + "\"");
//				}
				if (i + 1 < line.length) {
					out.write(",");
				}
			}
			if (toWrite.hasNext()) {
				out.newLine();
			}
		}
		out.close();
		fstream.close();
	}

	public static File getJarFile(Class jarClass) {
		return new File(jarClass.getProtectionDomain().getCodeSource().getLocation().getPath().
			replace("%20", " ").replace("%25", "%"));
	}

	/**
	 * parses the given filename string for the file extension
	 *
	 * @param filename string to parse
	 * @return extension, beginning with the dot (eg. ".jar")
	 */
	public static String getExtension(File file) {
		return getExtension(file.getAbsolutePath());
	}

	/**
	 * parses the given filename string for the file extension
	 *
	 * @param filename string to parse
	 * @return extension, beginning with the dot (eg. ".jar")
	 */
	public static String getExtension(String filename) {
		if (filename != null) {
			int dot = filename.lastIndexOf(".");
			if (dot > 0 && dot > filename.lastIndexOf(File.separator)) {
				return filename.substring(dot);
			}
		}
		return "";
	}

	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			File d = destFile.getParentFile();
			if(!d.exists()) {
				d.mkdirs();
			}
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	public static void move(File source, File dest) throws SecurityException {
		if(source.isFile()) {
			if(dest.exists()) {
				dest.delete();
			}
			source.renameTo(dest);
		} else {
			// dest is assumed a folder
			if(dest.exists() && !dest.isDirectory()) {
				dest.delete();
			}
			if(!dest.exists()) {
				dest.mkdirs();
			}
			for(File f : source.listFiles()) {
				File d = new File(dest, f.getName());
				if(f.isDirectory()) {
					move(f, d);
				} else {
					if(d.exists()) {
						d.delete();
					}
					f.renameTo(d);
				}
			}
			source.delete();
		}
	}
	
	public static boolean filesEqual(File f1, File f2) throws IOException {
		FileInputStream fis1 = null, fis2 = null;
		try {
			fis1 = new FileInputStream(f1);
			fis2 = new FileInputStream(f2);
			if (f1.length() == f2.length()) {
				int n = 0;
				byte[] b1;
				byte[] b2;
				while ((n = Math.min(fis1.available(), 2048)) > 0) {
					b1 = new byte[n];
					b2 = new byte[n];
					int res1 = fis1.read(b1);
					int res2 = fis2.read(b2);
					if (!Arrays.equals(b1, b2)) {
						return false;
					}
				}
				return true;
			}
		} finally {
			if (fis1 != null) {
				fis1.close();
			}
			if (fis2 != null) {
				fis2.close();
			}
		}
		return false;
	}

	public static enum OVERWRITE_CASE {

		NEVER, IF_NEWER, ALWAYS
	}

	public static void extractResource(String path, File writeTo, Class jarClass) throws Exception {
		extractResource(path, writeTo, jarClass, OVERWRITE_CASE.ALWAYS);
	}

	/**
	 * extract a file from the jar archive
	 *
	 * @param path path to the resource, relative to the jar root
	 * @param writeTo path to write to. if doesn't exist, will create directory
	 * if there is not a matching file extension
	 * @param jarClass class in the jar with the resource to extract
	 * @param overwrite what cases should the file be overwriten, if it exists
	 * @throws Exception
	 */
	public static void extractResource(String path, File writeTo, Class jarClass, OVERWRITE_CASE overwrite) throws Exception {
		if (!writeTo.exists()) {
			if (!getExtension(path).equalsIgnoreCase(getExtension(writeTo))) {
				writeTo.mkdirs();

			} else {
				// ensure parent dirs exist
				writeTo.getParentFile().mkdirs();
			}
		}
		if (writeTo.isDirectory()) {
			String fname = new File(path).getName();
			writeTo = new File(writeTo, fname);
		}
		// check if the file exists and is newer than the JAR
		File jarFile = getJarFile(jarClass);
		if (writeTo.exists()) {
			if (overwrite == OVERWRITE_CASE.NEVER) {
				return;
			} else if (overwrite == OVERWRITE_CASE.IF_NEWER
				&& writeTo.lastModified() >= jarFile.lastModified()) {
				return;
			}
		}

		Exception err = null;

		OutputStream output = null;
		InputStream input = null;
		try {
			// need to jump through hoops to ensure we can still pull messages from a JAR
			// file after it's been reloaded...
			URL res = jarClass.getResource(path.startsWith("/") ? path : "/" + path);
			if (res == null) {
				throw new java.io.FileNotFoundException("Could not find '" + path + "' in " + jarFile.getAbsolutePath());
			}
			URLConnection resConn = res.openConnection();
			resConn.setUseCaches(false);
			input = resConn.getInputStream();

			if (input == null) {
				throw new java.io.IOException("can't get input stream from " + res);
			} else {
				output = new FileOutputStream(writeTo);
				byte[] buf = new byte[8192];
				int len;
				while ((len = input.read(buf)) > 0) {
					output.write(buf, 0, len);
				}
			}
		} catch (Exception ex) {
			err = ex;
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (Exception e) {
			}
			try {
				if (output != null) {
					output.close();
				}
			} catch (Exception e) {
			}
		}
		if (err != null) {
			throw err;
		}
	}

	public static String loadFileFromResource(String path, Class jarClass) throws IOException, UnsupportedCharsetException {
		File jarFile = getJarFile(jarClass);
		Exception err = null;
		StringBuilder output = new StringBuilder();
		InputStream input = null;
		try {
			// need to jump through hoops to ensure we can still pull messages from a JAR
			// file after it's been reloaded...
			URL res = jarClass.getResource(path.startsWith("/") ? path : "/" + path);
			if (res == null) {
				throw new java.io.FileNotFoundException("Could not find '" + path + "' in " + jarFile.getAbsolutePath());
			}
			URLConnection resConn = res.openConnection();
			resConn.setUseCaches(false);
			input = resConn.getInputStream();

			if (input == null) {
				throw new java.io.IOException("can't get input stream from " + res);
			} else {
				byte[] buf = new byte[8192];
				Charset c = null;
				int len;
				while ((len = input.read(buf)) > 0) {
					if(c==null) {
						c = detectCharset(buf);
					}
					output.append(new String(buf, 0, len, c));
				}
			}
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (Exception e) {
			}
		}
		return output.toString();
	}
	
    public static Charset detectCharset(File f) throws UnsupportedCharsetException {
		return detectCharset(f, new String[]{"UTF-8", "windows-1253", "ISO-8859-7"});
	}
	
    public static Charset detectCharset(File f, String[] charsets) throws UnsupportedCharsetException {

		// iterate through sets to test, and return the first that is ok
        for (String charsetName : charsets) {
			final Charset c = Charset.forName(charsetName);
			if(isCharset(f, c)) {
				return c;
			}
        }

        return null;
    }
	
    private static boolean isCharset(File f, Charset charset) {
		boolean fileOk = false;
        try {
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(f));

            CharsetDecoder decoder = charset.newDecoder();
            decoder.reset();

            byte[] buffer = new byte[8192];
			if(input.read(buffer) != -1) {
				try {
					decoder.decode(ByteBuffer.wrap(buffer));
					fileOk = true;
				} catch (CharacterCodingException e) {
				}
			}
            input.close();
        } catch (Exception e) {
        }
		return fileOk;
    }
	
    public static Charset detectCharset(byte[] data) throws UnsupportedCharsetException {
		return detectCharset(data, new String[]{"UTF-8", "windows-1253", "ISO-8859-7"});
	}

    public static Charset detectCharset(byte[] data, String[] charsets) throws UnsupportedCharsetException {

		// iterate through sets to test, and return the first that is ok
        for (String charsetName : charsets) {
			final Charset c = Charset.forName(charsetName);
			if(isCharset(data, c)) {
				return c;
			}
        }

        return null;
    }
	
    private static boolean isCharset(byte[] data, Charset charset) {
		try {
            CharsetDecoder decoder = charset.newDecoder();
            decoder.reset();
			decoder.decode(ByteBuffer.wrap(data));
			return true;
		} catch (CharacterCodingException e) {
		}
		return false;
	}
	
	public static enum ITERATION {

		NONE, CLASS, PACKAGE, FULL
	}

	public static List<String> getClassNamesFromPackage(String packageName) throws IOException, URISyntaxException, ClassNotFoundException {
		return getClassNamesFromPackage(packageName, ITERATION.NONE);
	}

	public static List<String> getClassNamesFromPackage(String packageName, ITERATION iterate) throws IOException, URISyntaxException, ClassNotFoundException {
		return getClassNamesFromPackage(null, packageName, iterate);
	}

	public static List<String> getClassNamesFromPackage(File sourceJar, String packageName, ITERATION iterate) throws IOException, URISyntaxException, ClassNotFoundException {
		// http://stackoverflow.com/questions/1456930/how-do-i-read-all-classes-from-a-java-package-in-the-classpath
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL packageURL;
		ArrayList<String> names = new ArrayList<String>();

		if (packageName.contains("/")) {
			// load as a file
			packageURL = classLoader.getResource(packageName);

			// todo - if there is an error, step backwards to find the first avaliable package
			if (packageURL == null && packageName.contains("/")) {
				// added - check to see if maybe trying to load a file?
				final int i = packageName.lastIndexOf('/');
				packageName = packageName.substring(0, i) + "." + packageName.substring(i + 1);
				packageURL = classLoader.getResource(packageName);
			}
		} else {
			packageName = packageName.replace(".", "/");
			packageURL = classLoader.getResource(packageName);

			if (sourceJar == null && packageURL == null) {
				throw new IOException("Cannot open resource '" + packageName + "'");
			}

		}

		if (sourceJar == null && packageURL == null) {
			throw new IOException("Cannot open resource '" + packageName + "'");
			//} else if (packageURL.getProtocol().equals("file") || ) {
			// cannot do this..
		} else if (sourceJar != null || packageURL.getProtocol().equals("jar")) {
			// this can also be used to load jar from resources
			String jarFileName;
			JarFile jf;
			Enumeration<JarEntry> jarEntries;
			String entryName;

			// build jar file name, then loop through zipped entries
			jarFileName = sourceJar != null ? sourceJar.getAbsolutePath() : URLDecoder.decode(packageURL.getFile(), "UTF-8");
			// changed - support for resource jar files, too
			if (jarFileName.startsWith("file:/")) {
				jarFileName = jarFileName.substring(5);
			}
			if (jarFileName.startsWith("/")) {
				jarFileName = jarFileName.substring(1);
			}
			if (jarFileName.contains("!")) {
				jarFileName = jarFileName.substring(0, jarFileName.indexOf("!"));
			}

			jf = new JarFile(jarFileName);
			jarEntries = jf.entries();
			// in case of multiple sub-classes, keep track of what classes have been searched
			ArrayList<String> loaded = new ArrayList<String>();
			while (jarEntries.hasMoreElements()) {
				entryName = jarEntries.nextElement().getName();
				if (entryName.startsWith(packageName) && entryName.length() > packageName.length() && entryName.toLowerCase().endsWith(".class")) {
					if (entryName.contains(".")) {
						entryName = entryName.substring(packageName.length() + 1, entryName.lastIndexOf('.'));
					}
					// iteration test
					if (!entryName.contains("/") || (iterate == ITERATION.PACKAGE || iterate == ITERATION.FULL)) {

						if (entryName.contains("$")) { // added - sub-package test
							// added - iteration
							if (iterate == ITERATION.CLASS || iterate == ITERATION.FULL) {
								entryName = entryName.substring(0, entryName.indexOf('$')).replace('/', '.');
								if (!loaded.contains(entryName)) {
									loaded.add(entryName);
									try {
										Class c = Class.forName(packageName.replace('/', '.') + "." + entryName);
										for (Class c2 : c.getDeclaredClasses()) {
											names.add(entryName + "." + c2.getSimpleName());
										}
									} catch (Throwable t) {
									}
								}
							}
						} else {
							names.add(entryName.replace('/', '.'));
						}
					}
				}
			}
		} else {
			// hits here if running in IDE

			// loop through files in classpath
			URI uri = new URI(packageURL.toString());
			File folder = new File(uri.getPath());
			// won't work with path which contains blank (%20)
			// File folder = new File(packageURL.getFile()); 
			File[] contenuti = folder.listFiles();
			// in case of multiple sub-classes, keep track of what classes have been searched
			ArrayList<String> loaded = new ArrayList<String>();
			String entryName;
			for (File actual : contenuti) {
				entryName = actual.getName();
				if (entryName.contains(".")) { // added - folder check
					entryName = entryName.substring(0, entryName.lastIndexOf('.'));
					if (entryName.contains("$")) { // added - sub-package test
						// added - iteration
						if (iterate == ITERATION.CLASS || iterate == ITERATION.FULL) {
							entryName = entryName.substring(0, entryName.indexOf('$'));
							if (!loaded.contains(entryName)) {
								loaded.add(entryName);
								Class c = Class.forName(packageName.replace('/', '.') + "." + entryName);
								for (Class c2 : c.getDeclaredClasses()) {
									names.add(entryName + "." + c2.getSimpleName());
								}
							}
						}
					} else {
						names.add(entryName);
					}
				} else if (iterate == ITERATION.PACKAGE || iterate == ITERATION.FULL) {
					// added - iteration
					for (String sub : getClassNamesFromPackage(packageName + "/" + entryName, iterate)) {
						names.add(entryName + "." + sub);
					}
				}
			}
		}
		return names;
	}
}
