import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Board extends JPanel {
	// Constants
	private final int CELL_SIZE = 15;
	private final int NUM_IMAGES = 13;

	private final int IMAGE_MINE = 9;
	private final int IMAGE_COVER = 10;
	private final int IMAGE_MARK = 11;
	private final int IMAGE_WRONG_MARK = 12;

	private JLabel statusBar;

	private int totalMines = 40;
	private int remainderMines;

	private int rows = 16, columns = 16;

	private Cell[][] cells;
	private Image[] img;

	private boolean inGame;
	private boolean isWon;

	private MinesweeperAI agent;
	private int startX;
	private int startY;

	public Board(JLabel statusBar, int rows, int cols, int mines) {
		this.statusBar = statusBar;
		this.rows = rows;
		this.columns = cols;
		this.totalMines = mines;
		this.img = new Image[NUM_IMAGES];
		for (int i = 0; i < NUM_IMAGES; i++) {
			String path = "img/j" + i + ".gif";
			img[i] = new ImageIcon(path).getImage();
		}

		this.setDoubleBuffered(true);
		this.newGame();
	}

	private void initCells() {
		this.cells = new Cell[rows][columns];

		for (int i = 0; i < this.rows; ++i) {
			for (int j = 0; j < this.columns; ++j) {
				this.cells[i][j] = new Cell();
			}
		}
	}

	public void newGame() {
		Random random;

		random = new Random();

		this.inGame = true;
		this.remainderMines = totalMines;

		this.initCells();
		this.statusBar.setText(Integer.toString(this.remainderMines));

		int remainder = totalMines;
		while (remainder > 0) {
			int randX = random.nextInt(this.rows);
			int randY = random.nextInt(this.columns);

			Cell cell = this.cells[randX][randY];
			if (!cell.isMine()) {
				cell.setMine(true);
				remainder--;
			}
		}

		this.setMineCounts();

		while (true) {
			int randX = random.nextInt(this.rows);
			int randY = random.nextInt(this.columns);

			Cell cell = this.cells[randX][randY];
			if (!cell.isMine() && cell.isEmpty()) {
				startX = randX;
				startY = randY;
				break;
			}
		}
		this.agent = new MinesweeperAI(this.columns, this.rows, this.totalMines, startX + 1, startY + 1);
	}

	private void setMineCounts() {

		for (int i = 0; i < this.rows; ++i) {
			for (int j = 0; j < this.columns; ++j) {
				Cell cell = this.cells[i][j];

				if (!cell.isMine()) {
					int count = countMinesAround(i, j);
					cell.setAroundMines(count);
				}
			}
		}
	}

	private int countMinesAround(int x, int y) {
		int count = 0;

		for (int i = -1; i <= 1; ++i) {
			int xIndex = x + i;
			if (xIndex < 0 || xIndex >= this.rows) {
				continue;
			}

			for (int j = -1; j <= 1; ++j) {
				int yIndex = y + j;
				if (yIndex < 0 || yIndex >= this.columns) {
					continue;
				}

				if (i == 0 && j == 0) {
					continue;
				}

				if (this.cells[xIndex][yIndex].isMine()) {
					count++;
				}
			}
		}

		return count;
	}

	public void paint(Graphics g) {
		int coveredCells = 0;

		for (int i = 0; i < this.rows; i++) {
			for (int j = 0; j < this.columns; j++) {
				Cell cell = this.cells[i][j];
				int imageType;
				int xPosition, yPosition;

				// if (cell.isCovered() && !cell.isMarked()) {
				// coveredCells++;
				// }

				// if (inGame) {
				// if (cell.isMine() && !cell.isCovered()) {
				// inGame = false;
				// }
				// }

				imageType = this.decideImageType(cell);

				xPosition = (j * CELL_SIZE);
				yPosition = (i * CELL_SIZE);

				g.drawImage(img[imageType], xPosition, yPosition, this);
			}
		}

		// if (coveredCells == 0 && inGame) {
		// inGame = false;
		// statusBar.setText("Game Won");
		// } else if (!inGame) {
		// statusBar.setText("Game Lost");
		// }
	}

	private int decideImageType(Cell cell) {
		int imageType = cell.getValue();

		if (!inGame) {
			if (cell.isCovered() && cell.isMine()) {
				cell.uncover();
				imageType = IMAGE_MINE;
			} else if (cell.isMarked()) {
				if (cell.isMine()) {
					imageType = IMAGE_MARK;
				} else {
					imageType = IMAGE_WRONG_MARK;
				}
			}
		} else {
			if (cell.isMarked()) {
				imageType = IMAGE_MARK;
			} else if (cell.isCovered()) {
				imageType = IMAGE_COVER;
			}
		}

		return imageType;
	}

	public void findEmptyCells(int x, int y, int depth) {

		for (int i = -1; i <= 1; ++i) {
			int xIndex = x + i;

			if (xIndex < 0 || xIndex >= this.rows) {
				continue;
			}

			for (int j = -1; j <= 1; ++j) {
				int yIndex = y + j;

				if (yIndex < 0 || yIndex >= this.columns) {
					continue;
				}

				if (!(i == 0 || j == 0)) {
					continue;
				}

				Cell cell = this.cells[xIndex][yIndex];
				if (checkEmpty(cell)) {
					cell.uncover();
					cell.checked();

					uncoverAroundCell(xIndex, yIndex);
					findEmptyCells(xIndex, yIndex, depth + 1);
				}
			}
		}

		if (depth == 0) {
			this.clearAllCells();
		}
	}

	private void uncoverAroundCell(int x, int y) {
		for (int i = -1; i <= 1; ++i) {
			int xIndex = x + i;

			if (xIndex < 0 || xIndex >= this.rows) {
				continue;
			}

			for (int j = -1; j <= 1; ++j) {
				int yIndex = y + j;

				if (yIndex < 0 || yIndex >= this.columns) {
					continue;
				}

				if (i == 0 || j == 0) {
					continue;
				}

				Cell cell = this.cells[xIndex][yIndex];
				if (cell.isCovered() && !cell.isEmpty()) {
					cell.uncover();
				}
			}
		}
	}

	private boolean checkEmpty(Cell cell) {
		if (!cell.isChecked()) {
			if (cell.isEmpty()) {
				return true;
			}
		}

		return false;
	}

	private void clearAllCells() {
		for (int i = 0; i < this.rows; ++i) {
			for (int j = 0; j < this.columns; ++j) {
				this.cells[i][j].clearChecked();
			}
		}
	}

	public void run() {
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {

		}
		Cell pressedCell = this.cells[startX][startY];
		pressedCell.uncover();
		while (inGame && !isWon) {
			Action actionObj = this.agent.getAction(pressedCell.getValue());
			int x = actionObj.x - 1;
			int y = actionObj.y - 1;
			pressedCell = cells[x][y];
			switch (actionObj.action) {
			case FLAG:
				pressedCell.setMark(true);
				remainderMines--;
				break;
			case LEAVE:
				if (remainderMines != 0)
					inGame = false;
				else
					isWon = true;
				break;
			case UNCOVER:
				pressedCell.uncover();
				if (pressedCell.isMine())
					inGame = false;
				break;
			case UNFLAG:
				pressedCell.setMark(false);
				remainderMines--;
				break;
			default:
				break;
			}
			repaint();
			statusBar.setText(Integer.toString(remainderMines));
			if (this.isWon)
				statusBar.setText("Game Won");
			if (!this.inGame)
				statusBar.setText("Game Lost");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {

			}
		}
	}

}
