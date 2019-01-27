
/*

AUTHOR:      John Lu

DESCRIPTION: This file contains your agent class, which you will
             implement.

NOTES:       - If you are having trouble understanding how the shell
               works, look at the other parts of the code, as well as
               the documentation.

             - You are only allowed to make changes to this portion of
               the code. Any changes to other portions of the code will
               be lost when the tournament runs your code.
*/



import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Stack;


public class MinesweeperAI {
	// ########################## INSTRUCTIONS ##########################
	// 1) The Minesweeper Shell will pass in the board size, number of mines
	// and first move coordinates to your agent. Create any instance variables
	// necessary to store these variables.
	//
	// 2) You MUST implement the getAction() method which has a single
	// parameter,
	// number. If your most recent move is an Action.UNCOVER action, this value
	// will
	// be the number of the tile just uncovered. If your most recent move is
	// not Action.UNCOVER, then the value will be -1.
	//
	// 3) Feel free to implement any helper functions.
	//
	// ###################### END OF INSTURCTIONS #######################

	// This line is to remove compiler warnings related to using Java generics
	// if you decide to do so in your implementation.
	int rowDimension, colDimension;
	int totalMines, startX, startY, currentX, currentY, mineCounter;
	Location[][] board;
	Stack<Location> easyTargets;
	Stack<Location> flaggedTargets;
	Set<Location> suspectTargets;
	HashSet<Location> killerTargets;
	HashSet<Location> uncoveredTargets;
	HashSet<Location> coveredTargets;
	Difficulty difficulty;

	// non zero tiles uncovered tiles which have bombs in their neighbors
	Set<Location> helperTiles;
	boolean verbose = false;

	public MinesweeperAI(int rowDimension, int colDimension, int totalMines, int startX, int startY) {
		// ################### Implement Constructor (required)
		// ####################
		this.rowDimension = colDimension;
		this.colDimension = rowDimension;
		this.totalMines = totalMines;
		this.startX = startX;
		this.startY = startY;
		this.currentX = startX;
		this.currentY = startY;
		// initializes the board
		this.board = new Location[this.rowDimension + 1][this.colDimension + 1];

		// holds all covered non flagged tiles
		this.coveredTargets = new HashSet<>();

		for (int i = 1; i <= this.rowDimension; i++) {
			for (int j = 1; j <= this.colDimension; j++) {
				board[i][j] = new Location(i, j);
				coveredTargets.add(board[i][j]);
			}
		}

		// easyTargets are tiles which we will greedily uncover
		this.easyTargets = new Stack<>();

		// stores the info of flagged tiles
		this.flaggedTargets = new Stack<Location>();

		// maintains the info of suspects (covered tiles) which can be flagged
		this.suspectTargets = new HashSet<>();

		// easyTargets are tiles which contain a mine
		this.killerTargets = new HashSet<>();

		// holds uncovered targets
		this.uncoveredTargets = new HashSet<>();
		this.uncoveredTargets.add(board[startX][startY]);
		this.coveredTargets.remove(board[startX][startY]);

		// maintains the info of uncovered tiles which are not 0 and can have
		// mine neighboring them
		this.helperTiles = new HashSet<>();

		// stores the number of mines we have flagged
		this.mineCounter = 0;

		// game difficulty
		this.difficulty = findDifficulty(this.totalMines);
	}

	// ################## Implement getAction(), (required)
	// #####################

	public Action getAction(int number) {
		
		if (verbose) {
			System.out.println(
					"Easy targets (" + this.easyTargets.size() + "): " + getIterableAsString(this.easyTargets));
			System.out.println("Flagged targets size : " + this.flaggedTargets.size());
			System.out.println("Suspect targets size : " + this.suspectTargets.size());
			System.out.println(
					"Suspect targets (" + suspectTargets.size() + ") : " + getIterableAsString(this.suspectTargets));
			System.out.println(
					"Killer targets (" + this.killerTargets.size() + "): " + getIterableAsString(this.killerTargets));
			System.out.println("Helper tiles : " + getIterableAsString(this.helperTiles));
		}
		
		if(this.coveredTargets.size() == 0)
			return new Action(Intent.LEAVE);

		// if uncovered tile is non bomb and non flag
		if (number >= 0) {
			if (!this.board[this.currentX][this.currentY].isUncovered) {
				/*
				 * adding the actual number helps in keeping the logic of doing
				 * -1 for neighbors when a tile is flagged for eaxmple, a tile
				 * is flagged and neighboring tile (still uncovered) will have
				 * the value -1 (as val is initialized to 0 at first). Then
				 * later if this tile is uncovered with a number 2, then
				 * directly it will have the value 2 - 1 = 1
				 */
				this.board[this.currentX][this.currentY].val += number;
				this.board[this.currentX][this.currentY].tval = number;

				this.board[this.currentX][this.currentY].isUncovered = true;

				// add covered neighbors to easyTargets if val is 0
				if (this.board[this.currentX][this.currentY].val == 0)
					addNeighborsToEasyTargets(this.currentX, this.currentY);
				else {
					addNeighborsToSuspectTargets(this.currentX, this.currentY);
					this.helperTiles.add(this.board[this.currentX][this.currentY]);
				}
			}

		}

		// Go for the kill
		if (this.mineCounter == this.totalMines)
			this.easyTargets.addAll(coveredTargets);

		// Go for the kill
		if ((this.totalMines - this.mineCounter) == this.coveredTargets.size())
			this.killerTargets.addAll(coveredTargets);

		// print the board for debugging
		if (verbose)
			printBoard();

		// uncover the identified easy covered targets
		if (!this.easyTargets.isEmpty()) {
			Location easyTarget = this.easyTargets.pop();
			uncoverTile(easyTarget.x, easyTarget.y);
			return new Action(Intent.UNCOVER, this.currentX, this.currentY);
		}

		// uncover all the identified mines
		if (!this.killerTargets.isEmpty()) {
			Location killerTarget = killerTargets.iterator().next();
			flagTheTile(killerTarget.x, killerTarget.y);
			killerTargets.remove(killerTarget);
			return new Action(Intent.FLAG, this.currentX, this.currentY);
		}

		findTargetToFlag();

		if (!this.easyTargets.isEmpty()) {
			Location easyTarget = this.easyTargets.pop();
			uncoverTile(easyTarget.x, easyTarget.y);
			return new Action(Intent.UNCOVER, this.currentX, this.currentY);
		}

		if (!this.killerTargets.isEmpty()) {
			Location killerTarget = killerTargets.iterator().next();
			flagTheTile(killerTarget.x, killerTarget.y);
			killerTargets.remove(killerTarget);
			return new Action(Intent.FLAG, this.currentX, this.currentY);
		}

		// if true then uncover
		// else flag
		boolean[] uncoverTheTile = { true };
		Location randomTile = getRandomCoveredTile(uncoverTheTile);

		if (uncoverTheTile[0]) {
			uncoverTile(randomTile.x, randomTile.y);
			return new Action(Intent.UNCOVER, this.currentX, this.currentY);
		}

		flagTheTile(randomTile.x, randomTile.y);
		return new Action(Intent.FLAG, this.currentX, this.currentY);

	}

	// ################### Helper Functions Go Here (optional)
	// ##################
	// ...

	/**
	 * Returns game difficulty based on number of total mines
	 * 
	 * @param mines
	 * @return
	 */
	private Difficulty findDifficulty(int mines) {

		if (mines == 10)
			return Difficulty.EASY;

		if (mines == 40)
			return Difficulty.MEDIUM;

		if (mines == 99)
			return Difficulty.EXPERT;

		return Difficulty.UNKNOWN;
	}

	/**
	 * Returns a random covered tile
	 * 
	 * @param uncoverTheTile
	 * @return
	 */
	private Location getRandomCoveredTile(boolean[] uncoverTheTile) {
		ArrayList<Location> coveredTiles = new ArrayList<>(this.coveredTargets);
		Random randomGenerator = new Random();
		double randomDeciderValue = randomGenerator.nextDouble();
		int minesRemaining = this.totalMines - this.mineCounter;
		double mineProbability = minesRemaining * 1.0 / coveredTiles.size();
		if (mineProbability >= randomDeciderValue)
			uncoverTheTile[0] = false;
		else
			uncoverTheTile[0] = true;
		int tileIndex = randomGenerator.nextInt(coveredTiles.size());
		return coveredTiles.get(tileIndex);
	}

	/**
	 * This function must be called before action to uncover the Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 */
	private void uncoverTile(int x, int y) {
		Location location = this.board[x][y];
		this.currentX = location.x;
		this.currentY = location.y;

		this.uncoveredTargets.add(location);
		this.coveredTargets.remove(location);

		location.isStacked = false;
		if (this.suspectTargets.contains(location))
			this.suspectTargets.remove(location);
	}

	/**
	 * This function must be called before action to flag the Location(x,y) as a
	 * mine
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 */
	private void flagTheTile(int x, int y) {
		Location location = this.board[x][y];
		this.currentX = location.x;
		this.currentY = location.y;

		location.isUncovered = true;
		location.isFlagged = true;
		location.tval = -1;
		this.flaggedTargets.add(location);
		this.coveredTargets.remove(location);
		if (this.suspectTargets.contains(location))
			this.suspectTargets.remove(location);
		this.mineCounter++;
		List<Location> neighbors = getUnflaggedNeighborsOf(location.x, location.y);
		for (Location neighbor : neighbors) {
			neighbor.val -= 1;
			if (neighbor.val == 0) {
				addNeighborsToEasyTargets(neighbor.x, neighbor.y);
				if (this.helperTiles.contains(neighbor))
					this.helperTiles.remove(neighbor);
			}
		}
	}

	/**
	 * Add neighbors of Location(x,y) to easy targets
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 */
	private void addNeighborsToEasyTargets(int x, int y) {
		List<Location> coveredNeighbors = getCoveredNonStackedNeighborsOf(x, y);
		for (Location coveredNeighbor : coveredNeighbors) {
			this.easyTargets.push(coveredNeighbor);
			if (this.suspectTargets.contains(coveredNeighbor))
				this.suspectTargets.remove(coveredNeighbor);
			coveredNeighbor.isStacked = true;
		}
	}

	/**
	 * Add neighbors of Location(x,y) to suspect targets
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 */
	private void addNeighborsToSuspectTargets(int x, int y) {
		List<Location> coveredNeighbors = getCoveredNeighborsOf(x, y);
		for (Location coveredNeighbor : coveredNeighbors) {
			if (coveredNeighbor.isStacked)
				continue;
			this.suspectTargets.add(coveredNeighbor);
		}
	}

	/**
	 * If a number is touching the same number of squares, then the squares are
	 * all mines (can be made efficient by storing all encountered mines in a
	 * set)
	 *
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 */
	private List<Location> getSurroundingMines(Location location) {
		List<Location> neighbors = getCoveredNeighborsOf(location.x, location.y);
		if (neighbors.size() == location.val)
			return neighbors;
		return new ArrayList<>();
	}

	/**
	 * This will find out which target to uncover when its not obvious
	 */
	private void findTargetToFlag() {

		// find easy killer targets
		for (Location helperTile : helperTiles) {
			killerTargets.addAll(getSurroundingMines(helperTile));
		}

		if (!killerTargets.isEmpty())
			return;

		solve();

		if (easyTargets.isEmpty() && killerTargets.isEmpty()) {
			boolean[] flagTile = { true };
			Location mine = getProbableMine(flagTile);
			if (mine == null)
				return;
			if (flagTile[0])
				killerTargets.add(mine);
			else
				easyTargets.push(mine);
		}
	}

	private Location getProbableMine(boolean[] flagTile) {
		// creating a store of probables to flag
		Map<Location, double[]> probables = new HashMap<>();

		// our avg probability logic from here
		// initilization loop
		// double[] will store - double[0] total sum. double[1] total number of
		// contributors
		for (Location suspectTarget : this.suspectTargets) {
			if (suspectTarget.isFlagged || suspectTarget.isUncovered)
				continue;
			List<Location> neighbors = getUncoveredUnflaggedNeighborsOf(suspectTarget.x, suspectTarget.y);
			boolean skip = false;
			for (Location neighbor : neighbors)
				if (neighbor.val == 0) {
					skip = true;
					break;
				}
			if (skip)
				continue;
			probables.put(suspectTarget, new double[2]);
		}

		// actual logic of avg probability
		for (Location helperTile : helperTiles) {
			int val = helperTile.val;
			if (val == 0)
				continue;
			List<Location> neighbors = getCoveredNeighborsOf(helperTile.x, helperTile.y);
			int neighbborsCount = neighbors.size();
			if (neighbborsCount == 0)
				continue;
			for (Location neighbor : neighbors) {
				if (!probables.containsKey(neighbor))
					continue;
				double info[] = probables.get(neighbor);
				info[0] += 1.0 * val / neighbborsCount;
				info[1]++;
			}
		}

		// find the best probable having max avg probabilty from hashmap
		Location flagProbable = null, uncoverProbable = null;
		double maxChance = 0.0, minChance = 1.0;
		for (Map.Entry<Location, double[]> entry : probables.entrySet()) {
			Location loc = entry.getKey();
			double[] info = entry.getValue();
			if (info[1] == 0)
				continue;
			double avgProb = info[0] / info[1];
			if (maxChance <= avgProb) {
				flagProbable = loc;
				maxChance = avgProb;
			}

			if (minChance >= avgProb) {
				uncoverProbable = loc;
				minChance = avgProb;
			}
		}

		if (maxChance > 0.5)
			return flagProbable;
		flagTile[0] = false;
		return uncoverProbable;
	}

	/**
	 * Returns list of uncovered and flagged neighbors of Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private List<Location> getUncoveredUnflaggedNeighborsOf(int x, int y) {
		List<Location> list = new ArrayList<>();
		for (Location neighbor : getUncoveredNeighborsOf(x, y)) {
			if (neighbor.isFlagged)
				continue;
			list.add(neighbor);
		}

		return list;
	}

	/**
	 * Returns list of flagged neighbors of Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private List<Location> getUnflaggedNeighborsOf(int x, int y) {
		List<Location> list = new ArrayList<>();
		for (Location neighbor : getNeighborsOf(x, y)) {
			if (neighbor.isFlagged)
				continue;
			list.add(neighbor);
		}

		return list;
	}

	/**
	 * Returns list of covered neighbors of Location(x,y) that are not already
	 * present on stack
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private List<Location> getCoveredNonStackedNeighborsOf(int x, int y) {
		List<Location> coveredNeighbors = new ArrayList<>();
		for (Location neighbor : getNeighborsOf(x, y)) {
			if (neighbor.isUncovered || neighbor.isStacked)
				continue;
			coveredNeighbors.add(neighbor);
		}
		return coveredNeighbors;
	}

	/**
	 * Returns list of covered neighbors of Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private List<Location> getCoveredNeighborsOf(int x, int y) {
		List<Location> coveredNeighbors = new ArrayList<>();
		for (Location neighbor : getNeighborsOf(x, y)) {
			if (neighbor.isUncovered)
				continue;
			coveredNeighbors.add(neighbor);
		}
		return coveredNeighbors;
	}

	/**
	 * Returns list of uncovered neighbors of Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private List<Location> getUncoveredNeighborsOf(int x, int y) {
		List<Location> uncoveredNeighbors = new ArrayList<>();
		for (Location neighbor : getNeighborsOf(x, y)) {
			if (!neighbor.isUncovered)
				continue;
			uncoveredNeighbors.add(neighbor);
		}

		return uncoveredNeighbors;
	}

	/**
	 * Returns list of flagged neighbors of Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	@SuppressWarnings("unused")
	private List<Location> getFlaggedNeighborsOf(int x, int y) {
		List<Location> flaggedNeighbors = new ArrayList<>();
		for (Location neighbor : getNeighborsOf(x, y)) {
			if (!neighbor.isFlagged)
				continue;
			flaggedNeighbors.add(neighbor);
		}
		return flaggedNeighbors;
	}

	/**
	 * Returns all neighbors of Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private List<Location> getNeighborsOf(int x, int y) {
		List<Location> neighbors = new ArrayList<>();

		if (isLocationValid(x - 1, y - 1))
			neighbors.add(this.board[x - 1][y - 1]);
		if (isLocationValid(x - 1, y))
			neighbors.add(this.board[x - 1][y]);
		if (isLocationValid(x - 1, y + 1))
			neighbors.add(this.board[x - 1][y + 1]);
		if (isLocationValid(x + 1, y - 1))
			neighbors.add(this.board[x + 1][y - 1]);
		if (isLocationValid(x + 1, y))
			neighbors.add(this.board[x + 1][y]);
		if (isLocationValid(x + 1, y + 1))
			neighbors.add(this.board[x + 1][y + 1]);
		if (isLocationValid(x, y - 1))
			neighbors.add(this.board[x][y - 1]);
		if (isLocationValid(x, y + 1))
			neighbors.add(this.board[x][y + 1]);

		return neighbors;
	}

	/**
	 * Checks if the location(x,y) is valid
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private boolean isLocationValid(int x, int y) {
		return !(x < 1 || y < 1 || x > this.rowDimension || y > colDimension);
	}

	/**
	 * verbose utility
	 */
	private void printBoard() {
		System.out.println("============================ OUR BOARD STARTS ==============================");
		for (int i = colDimension; i > 0; i--) {
			System.out.println("");
			for (int j = 1; j <= rowDimension; j++) {
				int v = this.board[j][i].val;
				if (this.board[j][i].isFlagged) {
					System.out.print("#  ");
					continue;
				}
				if (!this.board[j][i].isUncovered)
					System.out.print(".  ");
				else
					System.out.print(v + "  ");
			}
		}

		System.out.println("\n==============================OUR BOARD ENDS ================================\n\n");
	}

	/**
	 * for verbose mode
	 * 
	 * @param o
	 * @return
	 */
	private String getIterableAsString(Iterable<Location> o) {
		String suspectTargetsString = "";
		for (Location suspectTarget : o) {
			suspectTargetsString += suspectTarget.toString();
		}
		return "[" + suspectTargetsString + "]";
	}

	static class Location {
		public int x, y, val = 0, tval = -2;
		public boolean isUncovered = false, isFlagged = false, isStacked = false;

		public Location(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public Location(int x, int y, int val) {
			this.x = x;
			this.y = y;
			this.val = val;
			this.tval = val;
		}

		@Override
		public String toString() {
			return " (" + Integer.toString(this.x) + " " + Integer.toString(this.y) + ") ";
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.toString());
		}

	}

	/**
	 * Splits the suspects tiles if they are not connected
	 * 
	 * @param suspectTiles
	 * @return list of connected tiles
	 */
	private ArrayList<ArrayList<Location>> splitSuspectTiles(Set<Location> suspectTiles) {

		ArrayList<ArrayList<Location>> splitResult = new ArrayList<>();
		ArrayList<Location> shadyTiles = new ArrayList<>(suspectTiles);
		while (shadyTiles.size() > 0) {
			LinkedList<Location> connectedTilesQueue = new LinkedList<>();
			ArrayList<Location> connectedTiles = new ArrayList<>();
			// find a starting point
			connectedTilesQueue.add(shadyTiles.get(0));
			while (!connectedTilesQueue.isEmpty()) {
				Location tile1 = connectedTilesQueue.poll();
				connectedTiles.add(tile1);
				shadyTiles.remove(tile1);
				// Find tiles connected to tile1
				for (Location tile2 : shadyTiles) {
					if (Math.abs(tile1.x - tile2.x) <= 2 && Math.abs(tile1.y - tile2.y) <= 2) {
						// Check if tile1 and tile2 is connected with the help
						// of helper tiles
						for (Location helperTile : helperTiles) {
							if (isClose(tile1, helperTile) && isClose(tile2, helperTile)) {
								if (!connectedTilesQueue.contains(tile2))
									connectedTilesQueue.add(tile2);
								break;

							}
						}
					}
				}
			}
			splitResult.add(connectedTiles);
		}
		return splitResult;

	}

	/**
	 * checks if two locations Location(x1,y1) Location(x2,y2) are close
	 * 
	 * @param p
	 * @param q
	 * @return
	 */
	private boolean isClose(Location p, Location q) {
		return (Math.abs(p.x - q.x) <= 1 && Math.abs(p.y - q.y) <= 1);
	}

	ArrayList<boolean[]> totalSolutions = null;

	/**
	 * Solves the game using backtracking
	 */
	private void solve() {

		boolean segregate = false;
		boolean[][] killerTiles;
		boolean[][] safeTiles;

		if (this.suspectTargets.size() > 8 || this.difficulty == Difficulty.EASY)
			segregate = true;

		List<ArrayList<Location>> segments = new ArrayList<ArrayList<Location>>();

		if (segregate)
			segments = splitSuspectTiles(suspectTargets);
		else {
			segments.add(new ArrayList<Location>(suspectTargets));
		}

		double bestProbability = 0;
		int totalMultiCases = 1, bestTileIndex = -1, bestSegmentIndex = -1;
		boolean safeTileFound = false;
		boolean killerTileFound = false;

		killerTiles = new boolean[this.rowDimension + 1][this.colDimension + 1];
		for (int x = 1; x <= this.rowDimension; x++) {
			for (int y = 1; y <= this.colDimension; y++) {
				killerTiles[x][y] = board[x][y].isFlagged;
			}
		}

		safeTiles = new boolean[this.rowDimension + 1][this.colDimension + 1];
		for (int x = 1; x <= this.rowDimension; x++) {
			for (int y = 1; y <= this.colDimension; y++) {
				safeTiles[x][y] = board[x][y].tval >= 0;
			}
		}

		for (int currSegment = 0; currSegment < segments.size(); currSegment++) {

			totalSolutions = new ArrayList<>();

			findSegmentSolutions(segments.get(currSegment), 0, killerTiles, safeTiles);

			if (totalSolutions.size() == 0)
				return;

			for (int i = 0; i < segments.get(currSegment).size(); i++) {
				boolean killerTile = true, safeTile = true;
				for (boolean[] solution : totalSolutions) {
					if (solution[i])
						safeTile = false;
					else
						killerTile = false;
				}

				Location block = segments.get(currSegment).get(i);

				if (killerTile) {
					killerTileFound = true;
					if (verbose)
						System.out.println("100% Sure - flag");
					if (block != null)
						killerTargets.add(block);
				} else if (safeTile) {
					safeTileFound = true;
					if (verbose)
						System.out.println("100% Sure - uncover");
					easyTargets.push(block);
				}

			}

			totalMultiCases *= totalSolutions.size();

			if (safeTileFound || killerTileFound)
				continue;
			int allMaxEmpty = -10000;
			int currIEmpty = -1;
			for (int i = 0; i < segments.get(currSegment).size(); i++) {
				int empty = 0;
				for (boolean[] sln : totalSolutions) {
					if (!sln[i])
						empty++;
				}
				if (empty > allMaxEmpty) {
					allMaxEmpty = empty;
					currIEmpty = i;
				} else if (empty == allMaxEmpty && Math.random() >= 0.5) {
					allMaxEmpty = empty;
					currIEmpty = i;
				}
			}

			double probability = (double) allMaxEmpty / (double) totalSolutions.size();

			if (probability > bestProbability) {
				bestProbability = probability;
				bestTileIndex = currIEmpty;
				bestSegmentIndex = currSegment;
			} else if (probability == bestProbability && Math.random() >= 0.5) {
				bestProbability = probability;
				bestTileIndex = currIEmpty;
				bestSegmentIndex = currSegment;
			}

		}

		if (safeTileFound || killerTileFound)
			return;

		if (bestSegmentIndex < 0 || bestTileIndex < 0)
			return;

		Location q = segments.get(bestSegmentIndex).get(bestTileIndex);
		if (killerTargets.isEmpty() && easyTargets.isEmpty())
			easyTargets.push(q);
		if (verbose)
			System.out.printf("  Guess %1.2f (%d cases)\n", bestProbability, totalMultiCases);
		return;
	}

	/**
	 * Find out surrounding true neighbors of Location(x,y) based on the board
	 * input
	 * 
	 * @param board
	 * @param x
	 * @param y
	 * @return
	 */
	private int getTrueNeighborsCounts(boolean[][] board, int x, int y) {
		int cnt = 0;
		if (isLocationValid(x - 1, y - 1) && board[x - 1][y - 1])
			cnt++;
		if (isLocationValid(x - 1, y) && board[x - 1][y])
			cnt++;
		if (isLocationValid(x - 1, y + 1) && board[x - 1][y + 1])
			cnt++;
		if (isLocationValid(x + 1, y - 1) && board[x + 1][y - 1])
			cnt++;
		if (isLocationValid(x + 1, y) && board[x + 1][y])
			cnt++;
		if (isLocationValid(x + 1, y + 1) && board[x + 1][y + 1])
			cnt++;
		if (isLocationValid(x, y - 1) && board[x][y - 1])
			cnt++;
		if (isLocationValid(x, y + 1) && board[x][y + 1])
			cnt++;
		return cnt;
	}

	/**
	 * Recursive backtracking approach to find all possible position of mines
	 * and safe tiles
	 * 
	 * @param currSegmentSuspectTiles
	 * @param currSegmentSize
	 * @param killerTiles
	 * @param safeTiles
	 */
	void findSegmentSolutions(ArrayList<Location> currSegmentSuspectTiles, int currSegmentSize, boolean[][] killerTiles,
			boolean[][] safeTiles) {

		int mineCounts = 0;
		for (int x = 1; x <= this.rowDimension; x++) {
			for (int y = 1; y <= this.colDimension; y++) {

				if (killerTiles[x][y])
					mineCounts++;

				if (board[x][y].tval < 0)
					continue;

				if (getTrueNeighborsCounts(killerTiles, x, y) > board[x][y].tval)
					return;

				if (getNeighborsCount(x, y) - getTrueNeighborsCounts(safeTiles, x, y) < board[x][y].tval)
					return;
			}
		}

		if (mineCounts > this.totalMines)
			return;

		if (currSegmentSize == currSegmentSuspectTiles.size()) {

			boolean[] solution = new boolean[currSegmentSuspectTiles.size()];
			for (int i = 0; i < currSegmentSuspectTiles.size(); i++) {
				Location block = currSegmentSuspectTiles.get(i);
				solution[i] = killerTiles[block.x][block.y];
			}
			totalSolutions.add(solution);
			return;
		}

		Location block = currSegmentSuspectTiles.get(currSegmentSize);

		killerTiles[block.x][block.y] = true;
		findSegmentSolutions(currSegmentSuspectTiles, currSegmentSize + 1, killerTiles, safeTiles);
		killerTiles[block.x][block.y] = false;

		safeTiles[block.x][block.y] = true;
		findSegmentSolutions(currSegmentSuspectTiles, currSegmentSize + 1, killerTiles, safeTiles);
		safeTiles[block.x][block.y] = false;

	}

	/**
	 * Return the neighbor count of Location(x,y)
	 * 
	 * @param x
	 *            x coordinate of the board
	 * @param y
	 *            y coordinate of the board
	 * @return
	 */
	private int getNeighborsCount(int x, int y) {
		if ((x == 1 && y == 1) || (x == this.rowDimension && y == this.colDimension))
			return 3;
		else if (x == 1 || y == 1 || x == this.rowDimension || y == this.colDimension)
			return 5;
		else
			return 8;
	}

	/**
	 * Enum of difficulty
	 */
	private enum Difficulty {
		UNKNOWN, EASY, MEDIUM, EXPERT
	}

}
