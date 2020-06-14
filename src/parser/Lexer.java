package parser;

import java.io.File;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
	/**
	 * 토큰 파싱의 규칙을 담고 있는 리스트
	 */
	private ArrayList<Lex> lexes = new ArrayList<Lex>();
	/**
	 * 무시할 토큰들을 담고 있는 리스트
	 */
	private ArrayList<String> skips = new ArrayList<String>();
	/**
	 * 파싱 중 로그를 띄울지 나타내는 변수
	 */
	private boolean logging = false;

	/**
	 * @param lexerString 파싱 룰 String
	 * @throws Exception 파싱 룰이 잘못되거나 파일이 손상되었을 경우 발생
	 */
	public Lexer(String lexerString) throws Exception {
		init(lexerString);
	}

	/**
	 * 
	 * @param file 파싱 룰을 담고 있는 파일
	 * @throws Exception 파싱 룰이 잘못되거나 파일이 손상되었을 경우 발생
	 */
	public Lexer(File file) throws Exception {
		String lexerString = IO.readFile(file);
		if (lexerString == null)
			throw new Exception("Given file is empty or currupted.");
		init(lexerString);
	}

	/**
	 * Lexer의 초기화에 사용
	 * 
	 * @param lexerRule 파싱 룰 String
	 * @throws Exception 파싱 룰이 잘못되거나 파일이 손상되었을 경우 발생
	 */
	private void init(String lexerRule) throws Exception {
		for (String rule : lexerRule.split("\n")) {
			// Ignore empty line
			if (rule.length() < 2)
				continue;

			char firstChar = rule.charAt(0);

			// Ignore comments
			if (firstChar == '/' || firstChar == '#')
				continue;

			// Check rule name
			if (!Character.isUpperCase(firstChar) && !Character.isLowerCase(firstChar))
				throw new Exception("Rule name must start with alphabet.");

			// Add rule
			String[] parts = rule.split(":");
			String name = parts[0].strip();
			String regexStr = parts[1].strip();
			Pattern regex = Pattern.compile(regexStr);
			lexes.add(new Lex(name, regex));
		}
	}

	/**
	 * 무시할 토큰을 등록한다.
	 * 
	 * @param skip 무시할 토큰의 이름
	 */
	public void addSkip(String skip) {
		skips.add(skip);
	}

	/**
	 * 로드된 모든 파싱 룰을 반환한다.
	 * 
	 * @return 파싱 룰(Lex)들의 반환
	 */
	public Lex[] getLexes() {
		return (Lex[]) lexes.toArray(new Lex[0]);
	}

	/**
	 * 파싱 중 로그를 띄울지 설정한다.
	 * 
	 * @param logging true=로그 띄움, false=로그 숨김
	 */
	public void setLoggin(boolean logging) {
		this.logging = logging;
	}

	/**
	 * 만약 로그가 설정되어있다면 입력받은 문자열을 그대로 출력
	 * 
	 * @param log 출력할 문자열
	 */
	private void log(String log) {
		if (logging)
			System.out.println(log);
	}

	/**
	 * 파싱 중에 사용되는 변수. 현재 파싱 중인 룰을 나타낸다. 전역변수로 한 것은 java에서 closer지원을 하지 않기 때문.
	 */
	private Lex currentLex;
	/**
	 * 파싱할 전체 문자열. 나중에 nonterminal token을 위해서 저장한다.
	 */
	private String fullText;

	/**
	 * 주어진 문자열을 렉싱하여 토큰들의 리스트로 변환한다. 어떤 토큰에도 해당되지 않는 문자가 있으면 예외가 발생한다.
	 * 
	 * @param input 렉싱 문자열
	 * @return 토큰들의 리스트
	 * @throws Exception 어떤 토큰에도 해당되지 않는 문자가 있을 시 발생
	 */
	public Token[] lex(String input) throws Exception {
		log("Initial string = \n" + input);
		fullText = input;
		PriorityQueue<Token> pq = new PriorityQueue<Token>();

//		Replacer 함수를 정의한다. 이 함수는 입력 문자열에서 특정 부분을 토큰으로 치환하여 토큰 우선순위 큐에 삽입하고, 특정 부분만큼의 길이를 가진 # 문자열을 반환한다.
		Function<MatchResult, String> replacer = new Function<MatchResult, String>() {
			@Override
			public String apply(MatchResult t) {
				pq.add(new Token(currentLex, t, fullText));
				String replacement = "";
				for (int i = 0; i < t.group().length(); i++) {
					replacement += "#";
				}
				return replacement;
			}
		};

//		순서대로 lexing rule을 적용하여 문자열을 치환해나간다. 앞서 선언한 replacer함수가 사용된다.
		for (Lex lex : lexes) {
			currentLex = lex;
			log("Apply rule " + currentLex.getRuleName());
			Matcher m = lex.getPattern().matcher(input);

			while (m.find()) {
				input = m.replaceFirst(replacer);
				log(input);
				m = lex.getPattern().matcher(input);
			}
		}

//		모든 렉싱 규칙을 전부 적용했음에도 남아있는 문자가 있다면 예외를 발생시킨다.
		if (!input.matches("#*"))
			throw new Exception("Unregistered character detected.");

//		토큰 우선순위 큐를 배열로 변환한다. 이 과정에서, 무시해도 되는 토큰은 제외된다.
		ArrayList<Token> tokens = new ArrayList<Token>();
		while (!pq.isEmpty()) {
			Token token = pq.poll();
			boolean isSkip = false;
			for (String skip : skips) {
				if (skip.equals(token.getRuleName())) {
					isSkip = true;
					break;
				}
			}
			if (isSkip)
				continue;
			tokens.add(token);
			log(token.getText().strip());
		}

		return (Token[]) tokens.toArray(new Token[0]);
	}
}