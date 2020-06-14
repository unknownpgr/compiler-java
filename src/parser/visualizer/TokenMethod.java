package parser.visualizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import parser.Token;

class TokenMethod extends TokenWrapper {
	private String paramType = "";
	private Set<String> references = new HashSet<String>();

	private String modifier, type, name;

	protected TokenMethod(Token astToken, TokenWrapper parent) {
		super(astToken, parent);

		if (getToken().getRuleName().equals("function_full")) {
			Token functionDef = getToken().getChild(0);
			modifier = functionDef.getChild(0).getChild(0).getText();
			type = functionDef.getChild(0).getChild(1).getText();
			name = functionDef.getChild(0).getChild(2).getText();
			if (functionDef.getChildCount() > 1) {
				paramType = functionDef.getChild(1).getChild(0).getText();
			}
		} else {
			Token constDef = getToken().getChild(0);
			modifier = constDef.getChild(0).getText();
			type = getName() + "(Constructor)";
			name = constDef.getChild(1).getText();
			if (constDef.getChildCount() > 2) {
				paramType = constDef.getChild(2).getChild(0).getText();
			}
		}

		ArrayList<Token> stack = new ArrayList<Token>();
		stack.add(astToken);
		while (!stack.isEmpty()) {
			Token currentToken = stack.remove(0);
			if (currentToken.isNonterminal()) {
				for (Token token : currentToken.getChildren()) {
					stack.add(token);
				}
			} else if (currentToken.getRuleName().equals("IDENTIFIER")) {
				references.add(currentToken.getText());
			}
		}
	}

	public List<String> getReferences() {
		return new ArrayList<String>(references);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getModifier() {
		return modifier;
	}

	@Override
	public String toString() {
		return getName() + "(" + paramType + ")";
	}

	@Override
	protected ArrayList<TokenWrapper> getChildren() {
		return new ArrayList<TokenWrapper>();
	}
}