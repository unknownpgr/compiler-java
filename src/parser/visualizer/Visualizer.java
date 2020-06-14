package parser.visualizer;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import parser.IO;
import parser.Lex;
import parser.Lexer;
import parser.Parse;
import parser.Parser;
import parser.Token;

public class Visualizer extends JFrame {

	/**
	 * Tree 아래의 text 영역
	 */
	private JTextArea textArea;
	/**
	 * 자바 파일 구조를 보여 줄 tree model
	 */
	private JTree tree = new JTree();
	/**
	 * 
	 */
	private JPanel panelRight = new JPanel();
	private JTable table = new JTable();
	private JTextArea sourceCode = new JTextArea();
	private CardLayout cardLayout = new CardLayout();

	public Visualizer() {
		// JFrame setting
		setSize(960, 720);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// Right panel layout setting
		panelRight.setLayout(cardLayout);
		JScrollPane scrollPane = new JScrollPane(table);
		panelRight.add(scrollPane);
		panelRight.add(sourceCode);

		// Text area setting
		textArea = new JTextArea();
		textArea.setText("Use : ");
		textArea.setEditable(false);

		// Tree model setting
		tree.setModel(null);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				event(e.getPath());
			}
		});

		// Split pane setting
		JSplitPane jspMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		JSplitPane jspSub = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		jspMain.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		jspMain.setLeftComponent(jspSub);
		jspMain.setRightComponent(add(panelRight));
		jspMain.setEnabled(false);
		jspMain.setDividerLocation(getSize().width / 2);

		jspSub.setTopComponent(tree);
		jspSub.setBottomComponent(textArea);
		jspSub.setEnabled(false);
		jspSub.setDividerLocation(getSize().height * 3 / 4);

		Container cp = getContentPane();
		cp.add(jspMain);

		// Menu setting
		JMenuBar menuBar = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem open = new JMenuItem("Open");
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Get file
				JFileChooser chooser = new JFileChooser("./");
				int returnValue = chooser.showOpenDialog(null);
				File file = null;
				if (returnValue == JFileChooser.APPROVE_OPTION)
					file = chooser.getSelectedFile();

				// Check if file is proper
				if (file == null)
					return;
				if (!file.exists()) {
					JOptionPane.showMessageDialog(null, "선택하신 파일이 존재하지 않습니다.", "File selection error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Try parse and display
				String filePath = file.getPath();
				try {
					Token token = parse(filePath);
					TokenTreeModel tokenTreeModel = new TokenTreeModel(token);
					tree.setModel(tokenTreeModel);
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(null, "올바른 JAVA 소스 파일이 아닙니다.", "File type error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		file.add(open);
		menuBar.add(file);
		setJMenuBar(menuBar);
	}

	public Token parse(String sourcFilePath) throws Exception {
		// Print raw sourcecode
		System.out.println("\n====[ RAW ]================");
		String src = IO.readFile(sourcFilePath);
		System.out.println(src);

		// Print lex rule
		Lexer lexer = new Lexer(new File("./lex-rule.txt"));
		Parser parser = new Parser(new File("./parse-rule.txt"));

		// Add skip rule
		lexer.addSkip("SPACE");

		// Print lexing rule
		System.out.println("\n====[ LEXING RULES ]================");
		for (Lex rule : lexer.getLexes()) {
			System.out.println(rule);
		}

		// Lexing
		Token[] tokens = lexer.lex(src);

		// Print tokenized code
		System.out.println("\n====[ TOKNIZED ]================");
		printTokens(tokens);

		// Print parsing rule
		System.out.println("\n====[ PARSING RULES ]================");
		for (Parse rule : parser.getLexes()) {
			System.out.println(rule);
		}

		// Parsing
		Token parsedToken = parser.parse(tokens);

		// Print parsed tokens
		System.out.println("\n====[ PARSED ]================");
		printTokens(new Token[] { parsedToken });

		// Skip some tokens
		System.out.println("\n====[ SKIP ]================");

		// Unroll some recursive tokens
		Token.skipToken(parsedToken, "fields");
		Token.skipToken(parsedToken, "exp");
		Token.skipToken(parsedToken, "codelines");
		Token.skipToken(parsedToken, "codeline");

		// Remove some keywords
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

		// Print abstract semantic tree
		printTokens(new Token[] { parsedToken });
		return parsedToken;
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

	/**
	 * Tree에서 특정 노드가 선택되었을 때 호출
	 * 
	 * @param selPath 선택된 노드를 담고 있는 Path
	 */
	private void event(TreePath selPath) {
		if (selPath == null) {
			System.out.println("SELECTED PAHT IS NULL");
			return;
		}

		// 선택한 노드
		TokenWrapper tokenWrapper = (TokenWrapper) selPath.getLastPathComponent();

		// 만약 선택한 노드가 클래스였을 경우
		if (tokenWrapper instanceof TokenClass) {
			String[] columns = { "Name", "Type", "Access" };
			// Set table
			Object[][] rows = new Object[tokenWrapper.getChildCount()][];
			for (int i = 0; i < tokenWrapper.getChildCount(); i++) {
				TokenWrapper child = tokenWrapper.getChild(i);
				Object[] row = { child.getName(), child.getType(), child.getModifier() };
				rows[i] = row;
			}
			DefaultTableModel tableModel = new DefaultTableModel(rows, columns);
			table.setModel(tableModel);
			textArea.setText("");
			cardLayout.first(panelRight);
		}

		// 만약 선택된 노드가 메서드였을 경우
		else if (tokenWrapper instanceof TokenMethod) {
			sourceCode.setText(tokenWrapper.getToken().getText().replace("\t", "    "));
			String usedFieldString = "Use :\n";
			TokenMethod tokenMethod = (TokenMethod) tokenWrapper;
			for (String field : tokenMethod.getReferences()) {
				TokenClass root = (TokenClass) tokenMethod.getRoot();
				if (root.hasField(field))
					usedFieldString += field + "\n";
			}
			textArea.setText(usedFieldString);
			cardLayout.last(panelRight);
		}

		// 만약 선택된 노드가 변수(Field)였을 경우
		else if (tokenWrapper instanceof TokenField) {
			String[] columns = { "Name", "Method" };

			// Set table
			ArrayList<TokenMethod> usingMethods = new ArrayList<TokenMethod>();
			for (TokenMethod method : ((TokenClass) tokenWrapper.getRoot()).getMethods()) {
				if (method.getReferences().contains(tokenWrapper.getName()))
					usingMethods.add(method);
			}
			Object[][] rows = new Object[usingMethods.size()][];
			for (int i = 0; i < usingMethods.size(); i++) {
				Object[] row = { tokenWrapper.getName(), usingMethods.get(i) };
				rows[i] = row;
			}
			DefaultTableModel tableModel = new DefaultTableModel(rows, columns);
			table.setModel(tableModel);
			textArea.setText("");
			cardLayout.first(panelRight);
		} else {
			System.err.println("Unregistered token");
			textArea.setText("");
		}
	}
}

class TokenTreeModel implements TreeModel {

	private TokenWrapper tr;

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