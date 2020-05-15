package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class IO {
	public static String readFile(String filePath) {
		return readFile(new File(filePath));
	}

	public static String readFile(File file) {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String output = "";
			for (String line; (line = br.readLine()) != null;) {
				output += line + "\n";
			}
			return output;
		} catch (Exception e) {
			return null;
		}
	}
}
