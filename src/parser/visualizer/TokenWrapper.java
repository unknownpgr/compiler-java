package parser.visualizer;

import java.util.ArrayList;

import parser.Token;

/**
 * 토큰을 사용하기 쉽도록 래핑한 클래스
 * 
 * @author 권준호
 *
 */
abstract class TokenWrapper {

	private TokenWrapper root;
	private TokenWrapper parent;
	private Token token;
	private String ruleName;

	protected TokenWrapper(Token astToken, TokenWrapper parent) {
		this.token = astToken;
		ruleName = astToken.getRuleName();
		if (parent == null) {
			root = this.parent = this;
		} else {
			root = parent.root;
			this.parent = parent;
		}
	}

	public static TokenWrapper getTokenWrapper(Token token, TokenWrapper parent) {
		String ruleName = token.getRuleName();
		if (ruleName.equals("class_full")) {
			return new TokenClass(token, parent);
		} else if (ruleName.equals("var_def_full")) {
			return new TokenField(token, parent);
		} else if (ruleName.equals("function_full") || ruleName.equals("constructor_full")) {
			return new TokenMethod(token, parent);
		} else {
			System.out.println("Unknown token : " + token.getRuleName() + ":" + token.getText());
			return null;
		}
	}

	public TokenWrapper getChild(int index) {
		return getChildren().get(index);
	}

	public int getChildCount() {
		return getChildren().size();
	}

	public boolean isLeaf() {
		return getChildren().size() == 0;
	}

	public String getRuleName() {
		return ruleName;
	}

	public Token getToken() {
		return token;
	}

	public TokenWrapper getParent() {
		return parent;
	}

	public TokenWrapper getRoot() {
		return root;
	}

	@Override
	public abstract String toString();

	public abstract String getName();

	public abstract String getType();

	public abstract String getModifier();

	protected abstract ArrayList<TokenWrapper> getChildren();
}