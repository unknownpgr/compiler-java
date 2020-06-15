package parser;

import java.util.regex.Pattern;

/**
 * 렉싱 규칙을 저장하는 클래스. 정규표현식과 토큰 이름의 튜플 (Patter,Name)이다.
 * 
 * @author 권준호
 *
 */
public class Lex {
	/**
	 * 매칭할 정규표현식
	 */
	private Pattern pattern;
	/**
	 * 규칙 이름
	 */
	private String name;

	/**
	 * @param name    토큰의 이름
	 * @param pattern 토큰을 나타내는 정규표현식
	 */
	Lex(String name, Pattern pattern) {
		this.name = name;
		this.pattern = pattern;
	}

	/**
	 * 이 토큰의 이름을 반환한다.
	 * 
	 * @return 토큰의 이름
	 */
	public String getRuleName() {
		return name;
	}

	/**
	 * 이 토큰을 나타내는 정규표현식을 반환한다.
	 * 
	 * @return 토큰을 나타내는 정규표현식
	 */
	public Pattern getPattern() {
		return pattern;
	}

	@Override
	public String toString() {
		return name + "\t: " + pattern.pattern();
	}
}
