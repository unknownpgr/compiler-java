package parser.visualizer;

import java.util.ArrayList;

import parser.Token;

class TokenField extends TokenWrapper {

	String modifier, type, name;

	protected TokenField(Token astToken, TokenWrapper parent) {
		super(astToken, parent);
		Token varDef = getToken().getChild(0);
		modifier = varDef.getChild(0).getText();
		type = varDef.getChild(1).getText();
		name = varDef.getChild(2).getText();
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
		return getName() + ":" + getType();
	}

	@Override
	protected ArrayList<TokenWrapper> getChildren() {
		return new ArrayList<TokenWrapper>();
	}
}