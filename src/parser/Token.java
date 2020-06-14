package parser;

import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.MatchResult;

/**
 * 분석된 토큰을 저장하는 클래스. 원래는 그렇게 하면 안 되지만, 개발의 편의를 위하여 토큰과 Parse tree의 Nonterminal을
 * 함께 구현한다.
 * 
 * @author 권준호
 *
 */
public class Token implements Comparable<Token> {
	private Lex lex;
	private Parse parse;
	private String fullText;
	private String text;
	private int start = -1;
	private int end;
	private ArrayList<Token> children = new ArrayList<Token>();

	/**
	 * 이 토큰에 해당하는 규칙과 정규표현식 매칭 결과를 가져온다.
	 * 
	 * @param lex
	 * @param mr
	 */
	public Token(Lex lex, MatchResult mr, String fullText) {
		this.lex = lex;
		this.text = mr.group().replace("#", "");
		this.start = mr.start();
		this.end = mr.end();
		this.fullText = fullText;
	}

	public Token(Parse parse) {
		this.parse = parse;
	}

	/**
	 * 현재 토큰의 raw text를 반환한다.
	 * 
	 * @return 현재 토큰의 raw text
	 */
	public String getText() {
		if (text != null)
			return text.trim();
		else
			return fullText.substring(start, end).trim();
	}

	/**
	 * 현재 토큰에 해당하는 생성 규칙을 반환한다.
	 * 
	 * @return 현재 토큰에 해당하는 생성 규칙
	 */
	public Lex getLex() {
		return lex;
	}

	/**
	 * 현재 토큰의 이름, 즉 현재 토큰의 생성 규칙의 이름을 반환한다.
	 * 
	 * @return 현재 토큰의 이름
	 */
	public String getRuleName() {
		if (lex != null)
			return lex.getRuleName();
		if (parse != null)
			return parse.getRuleName();
		System.err.println("Both lex and parse are null.");
		return null;
	}

	/**
	 * 새로운 자식 노드를 추가한 후, 노드 정보를 업데이트한다.
	 * 
	 * @param token
	 */
	public void addChild(Token token) {
//		Add child
		children.add(token);

//		Update token range
		if (start < 0)
			start = token.start;
		else
			start = Math.min(start, token.start);
		end = Math.max(end, token.end);

//		Set full text
		if (fullText == null)
			fullText = token.fullText;
	}

	/**
	 * 토큰의 index번째 child를 반환
	 * 
	 * @param index 몇 번째 자식인지
	 * @return 토큰의 index번째 child
	 */
	public Token getChild(int index) {
		return children.get(index);
	}

	/**
	 * 토큰의 자식 토큰들을 모두 담은 배열을 반환
	 * 
	 * @return 자식 토큰들의 배열
	 */
	public Token[] getChildren() {
		return (Token[]) children.toArray(new Token[0]);
	}

	/**
	 * 자식 토큰의 개수를 반환
	 * 
	 * @return 자식 토큰의 개수
	 */
	public int getChildCount() {
		return children.size();
	}

	@Override
	public int compareTo(Token o) {
		if (o.start > start)
			return -1;
		if (o.start < start)
			return 1;
		return 0;
	}

	@Override
	public String toString() {
		if (lex != null)
			return text + "\t: " + getRuleName() + "[" + start + "," + end + "]";
		if (parse != null)
			return getRuleName() + "[" + start + "," + end + "]";
		System.err.println("Both lex and parse are null.");
		return null;
	}

	/**
	 * 이 토큰이 Terminal인지 반환
	 * 
	 * @return 이 토큰이 Terminal인지 여부
	 */
	public boolean isTerminal() {
		return lex != null;
	}

	/**
	 * 이 토큰이 Nonterminal인지 반환
	 * 
	 * @return 이 토큰이 Nonterminal인지 반환
	 */
	public boolean isNonterminal() {
		return parse != null;
	}

	/**
	 * 특정 토큰을 무시한다. 예컨대 a(b(b(c)))에서 b를 무시하면 a(c)로 된다.
	 * 
	 * @param tree skipToken을 적용할 트리 구조의 토큰
	 * @param skip 무시할 토큰 이름, 혹은 정규표현식
	 * @return skipToken이 적용된 tree
	 */
	public static Token skipToken(Token tree, String skip) {
		ArrayList<Token> newChildren = new ArrayList<Token>();

//		재귀적으로 skipToken을 적용한 후, 만약 skip해야 할 child라면 child의 child를 child로 옮긴다.
		for (Token child : tree.children) {
			skipToken(child, skip);
			if (child.getRuleName().matches(skip)) {
				for (Token grandChild : child.children) {
					newChildren.add(grandChild);
				}
			} else {
				newChildren.add(child);
			}
		}

		tree.children = newChildren;
		return tree;
	}
}