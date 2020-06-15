package parser;

/**
 * Parser rule을 저장하는 클래스
 * 
 * @author 권준호
 *
 */
public class Parse {
	/**
	 * 규칙 이름
	 */
	String name;
	/**
	 * 치환할 패턴
	 */
	String[] parse;

	public Parse(String name, String[] parse) {
		this.name = name;
		this.parse = parse;
	}

	public String getRuleName() {
		return name;
	}

	@Override
	public String toString() {
		String r = name + "\t: ";
		for (String p : parse) {
			r += p + " ";
		}
		return r;
	}
}
