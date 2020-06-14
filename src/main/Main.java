package main;

import java.io.File;

import parser.IO;
import parser.Lex;
import parser.Lexer;
import parser.Parse;
import parser.Parser;
import parser.Token;
import parser.visualizer.Visualizer;

public class Main {
	public static void main(String[] args) throws Exception {		
		
//		Print raw sourcecode
		System.out.println("\n====[ RAW ]================");
		String src = IO.readFile("./source.txt");
		System.out.println(src);

//		Print lex rule
		Lexer lexer = new Lexer(new File("./lex-rule.txt"));
		Parser parser = new Parser(new File("./parse-rule.txt"));

//		Add skip rule
		lexer.addSkip("SPACE");

//		Print lexing rule
		System.out.println("\n====[ LEXING RULES ]================");
		for (Lex rule : lexer.getLexes()) {
			System.out.println(rule);
		}

//		Lexing
		Token[] tokens = lexer.lex(src);

//		Print tokenized code
		System.out.println("\n====[ TOKNIZED ]================");
		printTokens(tokens);

//		Print parsing rule
		System.out.println("\n====[ PARSING RULES ]================");
		for (Parse rule : parser.getLexes()) {
			System.out.println(rule);
		}

//		Parsing
		Token parsedToken = parser.parse(tokens);

//		Print parsed tokens
		System.out.println("\n====[ PARSED ]================");
		printTokens(new Token[] { parsedToken });

//		Skip some tokens
		System.out.println("\n====[ SKIP ]================");

//		Unroll some recursive tokens
		Token.skipToken(parsedToken, "fields");
		Token.skipToken(parsedToken, "exp");
		Token.skipToken(parsedToken, "codelines");
		Token.skipToken(parsedToken, "codeline");

//		Remove some keywords
		Token.skipToken(parsedToken, "CLASS");
		Token.skipToken(parsedToken, "IF");
		Token.skipToken(parsedToken, "ELSE");
		Token.skipToken(parsedToken, "SPLIT");
		Token.skipToken(parsedToken, "NEW");
		Token.skipToken(parsedToken, "RETURN");
		Token.skipToken(parsedToken, "BRACKET.+");
		Token.skipToken(parsedToken, "OPERATOR_NOT");
		Token.skipToken(parsedToken, "OPERATOR_ASSIGN");
		Token.skipToken(parsedToken, "OPERATOR_REFER");

//		Print abstract semantic tree
		printTokens(new Token[] { parsedToken });

//		Start visualization
		Visualizer v = new Visualizer(parsedToken);
		v.setVisible(true);
	}

	/**
	 * 토큰들의 배열, 혹은 토큰들의 트리를 나타내는 문자열을 콘솔로 출력한다.
	 * 
	 * @param tokens 출력할 토큰들
	 */
	private static void printTokens(Token[] tokens) {
		System.out.println(_printTokens(tokens, 0));
	}

	/**
	 * 재귀적으로 토큰들의 배열, 혹은 토큰들의 트리를 나타내는 문자열을 출력한다.
	 * 
	 * @param tokens 출력할 토큰들
	 * @param depth  재귀적 출력의 깊이. 호출할 때에는 depth=0으로 하고 호출하면 된다.
	 * @return 토큰들을 나타내는 문자열
	 */
	private static String _printTokens(Token[] tokens, int depth) {
		String r = "";
		for (Token t : tokens) {
			for (int i = 0; i < depth; i++) {
				r += "    ";
			}
			r += t + "\n";
			r += _printTokens(t.getChildren(), depth + 1);
		}
		return r;
	}
}
