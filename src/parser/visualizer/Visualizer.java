package parser.visualizer;

import java.awt.CardLayout;
import java.awt.Container;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import parser.Token;

public class Visualizer extends JFrame {

	private TokenTreeModel tokenTreeModel;
	private JTextArea textArea;
	private JTree tree;
	private JPanel panelRight = new JPanel();
	private JTable table = new JTable();
	private JTextArea sourceCode = new JTextArea();
	private CardLayout cardLayout = new CardLayout();

	public Visualizer(Token root) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {
		// JFrame setting
		setSize(960, 720);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// Set splitPane
		JSplitPane jspMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		JSplitPane jspSub = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		// Set right panel layout
		panelRight.setLayout(cardLayout);
		JScrollPane scrollPane = new JScrollPane(table);
		panelRight.add(scrollPane);
		panelRight.add(sourceCode);

		// Tree model setting
		tokenTreeModel = new TokenTreeModel(root);		
		tree = new JTree(tokenTreeModel);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				event(e.getPath());
			}
		});

		// Text area setting
		textArea = new JTextArea();
		textArea.setText("Use : ");
		textArea.setEditable(false);

		// Split pane setting at the end
		jspMain.setLeftComponent(jspSub);
		jspMain.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		jspMain.setEnabled(false);
		jspSub.setEnabled(false);
		jspMain.setRightComponent(add(panelRight));
		jspSub.setTopComponent(tree);
		jspSub.setBottomComponent(textArea);
		Container cp = getContentPane();
		cp.add(jspMain);
		jspMain.setDividerLocation(getSize().width / 2);
		jspSub.setDividerLocation(getSize().height * 3 / 4);
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

		// 선�?�?� 노드
		TokenWrapper tokenWrapper = (TokenWrapper) selPath.getLastPathComponent();

		// 만약 선�?�?� 노드가 �?�래스�?� 경우
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