package main;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import parser.Token;

public class Visualizer extends JFrame {
	TokenTreeModel tokenTreeModel;
	JTextArea textArea;
	JTree tree;
	JPanel panelRight;

	public Visualizer(Token root) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {
		// JFrame setting
		setLayout(new GridLayout());

		setSize(960, 720);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		// Create main panels
		JPanel panelLeft = new JPanel();
		add(panelLeft);
		panelRight = new JPanel();
		add(panelRight);

		// Set right panel layout
		panelRight.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 16));
		panelRight.setLayout(new BorderLayout());

		// Set left panel layout
		panelLeft.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;

		// Tree model setting
		tokenTreeModel = new TokenTreeModel(root);
		tree = new JTree(tokenTreeModel);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				event(e.getPath());
			}
		});
		c.gridy = 0;
		c.weighty = 3;
		c.insets = new Insets(16, 16, 0, 8);
		panelLeft.add(tree, c);

		// Text area setting
		textArea = new JTextArea();
		textArea.setText("Use : ");
		textArea.setEditable(false);
		c.gridy = 1;
		c.weighty = 1;
		c.insets = new Insets(16, 16, 16, 8);
		panelLeft.add(textArea, c);
	}

	private void event(TreePath selPath) {
		if (selPath == null) {
			System.out.println("SELECTED PAHT IS NULL");
			return;
		}

		TokenWrapper tokenWrapper = (TokenWrapper) selPath.getLastPathComponent();

		if (tokenWrapper instanceof TokenClass) {
			String[] columns = { "Name", "Type", "Access" };
			// Set table
			Object[][] rows = new Object[tokenWrapper.getChildCount()][];
			for (int i = 0; i < tokenWrapper.getChildCount(); i++) {
				TokenWrapper child = tokenWrapper.getChild(i);
				Object[] row = { child.name, child.type, child.modifier };
				rows[i] = row;
			}
			DefaultTableModel tableModel = new DefaultTableModel(rows, columns);
			JTable table = new JTable(tableModel);

			panelRight.removeAll();
			JScrollPane scrollPane = new JScrollPane(table);
			panelRight.add(scrollPane);
		} else if (tokenWrapper instanceof TokenMethod) {
			JTextArea sourceCode = new JTextArea();
			panelRight.removeAll();
			panelRight.add(sourceCode);
			sourceCode.setText(tokenWrapper.token.getText().replace("\t", "    "));

			ArrayList<String> fields = new ArrayList<String>();
			for (TokenWrapper token : tokenWrapper.root.children) {
				if (token instanceof TokenVariable) {
					fields.add(token.name);
				}
			}

			Set<String> usedField = new HashSet<String>();
			ArrayList<Token> stack = new ArrayList<Token>();
			stack.add(tokenWrapper.token);
			while (!stack.isEmpty()) {
				Token currentToken = stack.remove(0);
				if (currentToken.isNonterminal()) {
					for (Token token : currentToken.getChildren()) {
						stack.add(token);
					}
				} else if (currentToken.getRuleName().equals("IDENTIFIER")) {
					for (String fieldName : fields) {
						if (fieldName.equals(currentToken.getText())) {
							usedField.add(currentToken.getText());
							break;
						}
					}
				}
			}

			String usedFieldString = "Use :\n";
			for (String field : usedField) {
				usedFieldString += field + "\n";
			}
			textArea.setText(usedFieldString);
		} else if (tokenWrapper instanceof TokenVariable) {
			

		} else {
			System.err.println("Unregistered token");
			panelRight.removeAll();
			textArea.setText("");
		}
	}
}

class TokenWrapper {

	protected TokenWrapper root;
	protected TokenWrapper parent;
	protected Token token;
	protected String text;
	protected ArrayList<TokenWrapper> children = new ArrayList<TokenWrapper>();
	protected String ruleName;

	String name;
	String type;
	String modifier;

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
			return new TokenVariable(token, parent);
		} else if (ruleName.equals("function_full") || ruleName.equals("constructor_full")) {
			return new TokenMethod(token, parent);
		} else {
			System.out.println("Unknown token : " + token.getRuleName() + ":" + token.getText());
			return null;
		}
	}

	@Override
	public String toString() {
		return text;
	}

	public TokenWrapper getChild(int index) {
		return children.get(index);
	}

	public int getChildCount() {
		return children.size();
	}

	public boolean isLeaf() {
		return children.size() == 0;
	}
}

class TokenClass extends TokenWrapper {
	TokenClass(Token astToken, TokenWrapper parent) {
		super(astToken, parent);
		text = name = token.getChild(0).getChild(0).getText();
		for (Token token : token.getChildren()) {
			if (token.getRuleName().equals("field")) {
				children.add(TokenWrapper.getTokenWrapper(token.getChild(0), this));
			}
		}
	}
}

class TokenVariable extends TokenWrapper {
	protected TokenVariable(Token astToken, TokenWrapper parent) {
		super(astToken, parent);
		Token varDef = token.getChild(0);
		modifier = varDef.getChild(0).getText();
		type = varDef.getChild(1).getText();
		name = varDef.getChild(2).getText();
		text = name + ":" + type;
	}
}

class TokenMethod extends TokenWrapper {
	String paramType = "";

	protected TokenMethod(Token astToken, TokenWrapper parent) {
		super(astToken, parent);

		if (token.getRuleName().equals("function_full")) {
			Token functionDef = token.getChild(0);
			modifier = functionDef.getChild(0).getChild(0).getText();
			type = functionDef.getChild(0).getChild(1).getText();
			name = functionDef.getChild(0).getChild(2).getText();
			if (functionDef.getChildCount() > 1) {
				paramType = functionDef.getChild(1).getChild(0).getText();
			}
		} else {
			Token constDef = token.getChild(0);
			modifier = constDef.getChild(0).getText();
			type = name + "(Constructor)";
			name = constDef.getChild(1).getText();
			if (constDef.getChildCount() > 2) {
				paramType = constDef.getChild(2).getChild(0).getText();
			}
		}
		text = name + "(" + paramType + ")";
	}
}

class TokenTreeModel implements TreeModel {

	TokenWrapper tr;

	public TokenTreeModel(Token token) {
		tr = TokenWrapper.getTokenWrapper(token, null);
	}

	@Override
	public Object getRoot() {
		return tr;
	}

	@Override
	public Object getChild(Object parent, int index) {
		return ((TokenWrapper) parent).getChild(index);
	}

	@Override
	public int getChildCount(Object parent) {
		return ((TokenWrapper) parent).getChildCount();
	}

	@Override
	public boolean isLeaf(Object node) {
		return ((TokenWrapper) node).isLeaf();
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {

	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return 0;
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
	}
}