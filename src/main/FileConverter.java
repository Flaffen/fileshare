package main;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileConverter {
	public static byte[] convertToByteArray(Path file) throws IOException {
		byte[] data = Files.readAllBytes(file);
		
		return data;
	}
	
	public static void putToFile(Path path, byte[] data) throws IOException {
		PrintStream ps = null;
		try {
			ps = new PrintStream(path.toFile());
			ps.write(data);
			ps.close();
		} finally {
			if (ps != null) ps.close();
		}
	}
}