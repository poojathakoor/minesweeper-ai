import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class Main extends JFrame {
	private final int CELL_WIDTH = 15;
	private final int CELL_HEIGHT = 15;
	private final int ROWS = 16;
	private final int COLUMNS = 30;
	private final int MINES = 99;

	private JLabel statusbar;

	public Main() {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(CELL_WIDTH * (COLUMNS + 1), CELL_HEIGHT * (ROWS + 1));
		setLocationRelativeTo(null);
		setTitle("Minesweeper");

		statusbar = new JLabel("");
		add(statusbar, BorderLayout.SOUTH);
		Board board = new Board(statusbar, ROWS, COLUMNS, MINES);
		add(board);

		setResizable(false);
		setVisible(true);
		board.run();
	}

	public static void main(String[] args) {
		new Main();
	}
}
