package parser.visualizer;

import java.util.ArrayList;
import java.util.HashMap;

import parser.Token;

class TokenClass extends TokenWrapper {
	private HashMap<String, TokenField> fields = new HashMap<String, TokenField>();
	private HashMap<String, TokenMethod> methods = new HashMap<String, TokenMethod>();
	private ArrayList<TokenWrapper> children = new ArrayList<TokenWrapper>();
	private String text, name;

	TokenClass(Token astToken, TokenWrapper parent) {
		super(astToken, parent);
		text = name = getToken().getChild(0).getChild(0).getText();
		for (Token token : getToken().getChildren()) {
			if (token.getRuleName().equals("field")) {
				TokenWrapper child = TokenWrapper.getTokenWrapper(token.getChild(0), this);
				children.add(child);

				if (child instanceof TokenMethod) {
					methods.put(child.getName(), (TokenMethod) child);
				}

				if (child instanceof TokenField) {
					fields.put(child.getName(), (TokenField) child);
				}
			}
		}
	}

	public TokenField[] getFileds() {
		return (TokenField[]) fields.values().toArray(new TokenField[0]);
	}

	public TokenMethod[] getMethods() {
		return (TokenMethod[]) methods.values().toArray(new TokenMethod[0]);
	}

	public boolean hasField(String name) {
		return fields.containsKey(name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return name;
	}

	@Override
	public String getModifier() {
		return null;
	}

	@Override
	public String toString() {
		return text;
	}

	@Override
	protected ArrayList<TokenWrapper> getChildren() {
		return children;
	}
}