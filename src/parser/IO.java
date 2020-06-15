package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class IO {
	/**
	 * 파일을 읽어 String으로 반환한다.
	 * @param filePath 파일의 경로
	 * @return 파일의 내용
	 */
	public static String readFile(String filePath) {
		return readFile(new File(filePath));
	}

	/**
	 * 파일을 읽어 String으로 반환한다.
	 * @param file 읽을 파일 객체
	 * @return 파일의 내용
	 */
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
