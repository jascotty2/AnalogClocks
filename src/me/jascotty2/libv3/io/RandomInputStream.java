package me.jascotty2.libv3.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomInputStream extends InputStream {

	final RandomAccessFile raf;

	protected RandomInputStream(RandomAccessFile file) {
		raf = file;
	}

	@Override
	public int read() throws IOException {
		return raf.read();
	}

}
