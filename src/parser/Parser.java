package parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

public class Parser {
	/**
	 * 파싱 규칙을 담고 있는 리스트
	 */
	private ArrayList<Parse> parses = new ArrayList<Parse>();

	/**
	 * 파일에서 파싱 규칙을 로드
	 * 
	 * @param parserFile 파싱 규칙을 서술한 파일
	 * @throws Exception 파싱 규칙이 잘못되거나 파일을 여는 데 문제가 있을 경우 발생
	 */
	public Parser(File parserFile) throws Exception {
		init(IO.readFile(parserFile));
	}

	/**
	 * 파싱 문자열에서 파싱 규칙을 로드
	 * 
	 * @param parserString 파싱 규칙을 서술한 파일의 경로
	 * @throws Exception 파싱 규칙이 잘못되거나 파일을 여는 데 문제가 있을 경우 발생
	 */
	public Parser(String parserString) throws Exception {
		init(parserString);
	}

	/**
	 * 파서를 초기화
	 * 
	 * @param parserString 파서 정보를 담고 있는 문자열
	 * @throws Exception 파싱 규칙이 잘못될 경우 발생
	 */
	private void init(String parserString) throws Exception {
		for (String parserRuleString : parserString.split("\n")) {
			// Ignore empty line
			if (parserRuleString.length() < 2)
				continue;

			char firstChar = parserRuleString.charAt(0);

			// Ignore comments
			if (firstChar == '/' || firstChar == '#')
				continue;

			// Check rule name
			if (!Character.isLowerCase(firstChar) && !Character.isUpperCase(firstChar))
				throw new Exception("Rule name must start with alphabet.");

			// Add rule
			String[] parts = parserRuleString.split(":");
			String name = parts[0].trim();
			String[] tokenNames = parts[1].trim().split(" ");
			parses.add(new Parse(name, tokenNames));
		}
	}

	/**
	 * 파싱 규칙들의 배열을 반환한다.
	 * 
	 * @return 파싱 규칙들의 배열
	 */
	public Parse[] getLexes() {
		return (Parse[]) parses.toArray(new Parse[0]);
	}

	/**
	 * 주어진 토큰 리스트에서 주어진 파싱 룰을 적용할 수 있는 가장 왼쪽의 경우에 파싱 룰을 적용한다.
	 * 
	 * @param tokens 파싱 룰을 적용할 토큰 리스트
	 * @param parse  적용할 파싱 룰
	 * @return 파싱 룰을 적용했는지의 여부
	 */
	private static boolean replaceFirst(ArrayList<Token> tokens, Parse parse) {
		String[] pattern = parse.parse;
		int patternLength = pattern.length;

		for (int i = 0; i < tokens.size(); i++) {
			boolean find = true;
			for (int j = 0; j < patternLength; j++) {
				if (!tokens.get(i + j).getRuleName().equals(pattern[j])) {
					find = false;
					break;
				}
			}
			if (find) {
				// Replace existing tokens
				Token replace = new Token(parse);
				for (int j = 0; j < patternLength; j++) {
					Token removed = tokens.remove(i);
					replace.addChild(removed);
				}
				tokens.add(i, replace);
				return true;
			}
		}
		return false;
	}

	/**
	 * 더이상 불가능할 때까지, 왼쪽에서 오른쪽으로, 우선순위가 높은 룰부터 낮은 룰 순으로 파싱 규칙을 적용한다.
	 * 사실 원래대로라면 왼쪽에서 오른쪽이 아니라 최대한 많은 문자열이 매칭되는 순으로 파싱을 진행하여야 한다.
	 * 그러나 제대로 된 컴파일러를 구현하는 것이 목적이 아니라, 과제로 제출할 간단한 컴파일러가 목표이므로 생략한다.
	 * 
	 * @param tokens 파싱할 토큰들의 배열
	 * @return 루트 토큰
	 * @throws Exception 만약 파싱 룰을 모두 적용했을 때 단 하나의 토큰만 남는 경우가 아니라면 발생
	 */
	public Token parse(Token[] tokens) throws Exception {
		ArrayList<Token> temp = new ArrayList<Token>(Arrays.asList(tokens));
		int l = parses.size();
		for (int i = 0; i < l; i++) {
			if (replaceFirst(temp, parses.get(i)))
				i = 0;
		}
		if (temp.size() > 1) {
			String errorMessage = "Unparsable token pattern exists : ";
			for (Token token : temp) {
				errorMessage += token.getRuleName() + " ";
			}
			throw new Exception(errorMessage);
		}
		return temp.get(0);
	}
}