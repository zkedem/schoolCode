import java.util.*; 
import java.util.concurrent.*;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets; 
import java.nio.file.*; 
public class Sudoku{
	private JFrame frame = new JFrame();
	private JSplitPane splitPane;
	//Deals with the grid
	private JPanel gridPanel = new JPanel();
	private JPanel gridContainer = new JPanel();
	private HashMap<Coordinate, SudokuButton> gridButtons = new HashMap<Coordinate, SudokuButton>();
	private SudokuButton currentGridButton = null;
	private boolean pressable = false;
	//Control Panel
	private JPanel controlSuper = new JPanel();
	private JPanel control = new JPanel();
    //Contains the puzzle generator
	private JPanel generatorContainer = new JPanel();
	private JButton generatePuzzleButton = new JButton("Generate Puzzle.");
	private JComboBox<String> difficultiesBox = new JComboBox<String>(new String[] {"Easy", "Medium", "Hard", "Evil"});
	private JTextField seedInput = new JTextField("Seed");
	private String seed;
	private String difficulty;
	//Sudoku Puzzles
    private SudokuPuzzleGenerated myPuzzle;
	private SudokuPuzzlePlayable puzzle;
	//Deals with the choices for value of each spot on grid
	private JPanel numberBox = new JPanel(); 
	private JComboBox numberChoice = new JComboBox<String>(new String [] {"Value:", "1", "2", "3", "4", "5", "6", "7", "8", "9"});
	private JCheckBox showValues = new JCheckBox("Show possible values?");
	private boolean showingValues = false;
	// Deals with timer
	private JPanel timerBox = new JPanel();
	private JLabel timerLabel = new JLabel("00:00");
	private javax.swing.Timer timer;
	private int secondsElapsed;
	private int minutesElapsed;	
	//Deals with solving
	private JButton solve = new JButton("Solve.");
	private boolean solving = false;
	//Deals with history(undo/redo)
	private ArrayList<Move> history = new ArrayList<Move>();
	private int historyPosition = 0;
	private JPanel undoRedo = new JPanel();
	public JButton undo = new JButton("Undo");
	public JButton redo = new JButton("Redo");
	private boolean undoing;
	//Deals with saving and loading files
	private FileNameExtensionFilter filter = new FileNameExtensionFilter(".sudoku files", "sudoku");
	private JFileChooser fileChooser = new JFileChooser(){{
		setFileFilter(filter);
		File workingDirectory = new File(System.getProperty("user.dir"));
		setCurrentDirectory(workingDirectory);
	}};
	private JButton loadButton = new JButton("Load."){
		{
			addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						loadFile();
					}
			});
		}
	};
	private JButton saveButton = new JButton("Save"){{
		addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						saveFile();
					}
			});
	}};
	//Deals with isPaused and unisPaused game
	private JButton pauseButton = new JButton("PAUSE"){{
		setVisible(false);
		addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					pausePlay();
				}
		});
	}};
	private boolean isPaused = false;
	//Misc
	private JLabel winnerLabel = new JLabel("You won!");
	private JLabel seedLabel = new JLabel();
	private javax.swing.Timer pauseTimer;
    private JButton giveUp = new JButton("Cede Defeat.");
	
	public Sudoku(){
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setupGrid();
		setupGenerator();
		//control.setLayout()//new BoxLayout(control, BoxLayout.PAGE_AXIS));
		control.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTH;
		c.insets = new Insets(10, 5, 0, 5);
		c.ipadx = 10;
		c.ipady = 5;
		c.gridx = 0;
		c.gridy = 0;
		setupTimer();
		control.add(timerBox, c);
		c.gridy ++;		
		control.add(generatorContainer, c);
		c.gridy ++;
		control.add(loadButton,c);
		c.gridy ++;
		setupSolver();
		control.add(solve, c);
		c.gridy ++;
		setupNumberBox();
		control.add(numberBox, c);
		c.gridy ++;
		control.add(giveUp, c);
		giveUp.setVisible(false);
		c.gridy ++;
		setupUndoRedo();
		control.add(undoRedo, c);
		c.gridy ++;
		saveButton.setVisible(false);
		control.add(saveButton, c);
		c.gridy ++;
		control.add(winnerLabel, c);
		winnerLabel.setVisible(false);
		winnerLabel.setFont(new Font("Arial", Font.BOLD, 30));
		controlSuper.add(control);
		setupPauseTimer();
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlSuper, gridContainer);
		frame.getContentPane().add(splitPane);
		frame.pack();
        frame.setVisible(true);
	}
	public static void main(String args[]){
        Sudoku s = new Sudoku();
    }
	/** Deals with the pressing of the buttons on the sudoku grid. */
	public void press(SudokuButton b){
		if(pressable){
			if (currentGridButton != null){
				currentGridButton.setBackground(null);
			}
			if(currentGridButton == b){
				currentGridButton.setBackground(null);
				currentGridButton = null;
				//numberBox.setVisible(false);
			}else{
				currentGridButton = b;
				b.setBackground(Color.green);
				//numberBox.setVisible(true);
				if(!((String) numberChoice.getSelectedItem()).equals("Value:") && !((String) numberChoice.getSelectedItem()).equals(currentGridButton.getText())){
					numberChoice.setSelectedItem("Value:");
				}
				//numberChoices.
			}
			//checkForWin();
		}
		
	}
	/** Checks if the player has won and deals with the winning. */
	private void checkForWin(){
		if(puzzle.getKnownCount() == 81){
			pressable = false;
			for(Coordinate coord: gridButtons.keySet()){
				gridButtons.get(coord).setBackground(Color.green);
			}
			timer.stop();
			numberBox.setVisible(false);
			winnerLabel.setVisible(true);
			undoRedo.setVisible(false);
			giveUp.setVisible(false);
			saveButton.setVisible(false);
			pauseButton.setVisible(false);
			
		}
	}
    /**Loads the permanent value sof a Sudoku puzzle onto the grid. */
	public void loadPuzzleData(){
		Integer[][] myData = myPuzzle.data;
		for(int x = 0; x < 9; x ++){
			for(int y = 0; y < 9; y++){
				if(myData[y][x] != 0){
					gridButtons.get(new Coordinate(x, y)).setText(String.valueOf(myData[y][x]));
					gridButtons.get(new Coordinate(x, y)).permanent();
				}
			}
		}
		generatePuzzleButton.setVisible(false);
		difficultiesBox.setVisible(false);
		seedInput.setVisible(false);
		generatorContainer.add(new JLabel("<HTML>Difficulty: " + difficultiesBox.getSelectedItem() + "<br/>Seed: " + seedInput.getText() + "<br/>Difficulty-Rating: " + myPuzzle.getDifficultyRating() + "</HTML>"));
		
    }
	/** Sets up the puzzle generator.*/
	private void setupGenerator(){
		generatorContainer.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		generatorContainer.add(difficultiesBox, gbc);
		gbc.gridx = 1;
		generatorContainer.add(seedInput, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
		generatorContainer.add(generatePuzzleButton, gbc);
		generatorContainer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.black));
		generatorContainer.setSize(new Dimension(200, 200));
		
		generatePuzzleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			    myPuzzle = new SudokuPuzzleGenerated((String) difficultiesBox.getSelectedItem(), seedInput.getText());
			    seed = seedInput.getText();
				difficulty = (String) difficultiesBox.getSelectedItem();
				loadPuzzleData();
				puzzle = new SudokuPuzzlePlayable(myPuzzle.data);
				pressable = true;
				startTimer();		
				solve.setVisible(false);
				undoRedo.setVisible(true);
				giveUp.setVisible(true);
				loadButton.setVisible(false);
				saveButton.setVisible(true);
				numberBox.setVisible(true);
				pauseButton.setVisible(true);
				giveUp.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						Integer[][] d = myPuzzle.getSolution();
						for(Coordinate coord : gridButtons.keySet()){
							SudokuButton sb = gridButtons.get(coord);
							sb.setText(String.valueOf(d[sb.coord.y][sb.coord.x]));
							sb.permanent();
						}
						timer.stop();
						undoRedo.setVisible(false);
						numberBox.setVisible(false);
						solve.setVisible(false);
						if(currentGridButton != null){
							currentGridButton.setBackground(null);
						}
					    checkForWin();
					    giveUp.setVisible(false);
						saveButton.setVisible(false);
						JOptionPane.showMessageDialog(frame, "HA! Once again, a simple human can't solve a simple Sudoku. Puny worthless human, once again teh machines rule supreme!");

					   
					}
				});
			}
		});
		generatePuzzleButton.setToolTipText("Press to generate a puzzle of the given difficulty from the given seed. ");
	}
	/** Sets up the Sudoku grid.*/
	private void setupGrid(){
		gridPanel.setLayout(new GridLayout(9,9));
        for(int y = 0; y < 9; y++){
			for(int x = 0; x < 9; x ++){
			    SudokuButton b = new SudokuButton(new Coordinate(x, y),this);
				gridPanel.add(b);
				gridButtons.put(b.coord, b);
				
			}
		}
		
		gridPanel.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5, Color.blue));
        gridPanel.setPreferredSize(new Dimension(500, 500));
        gridContainer.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;
		gridContainer.add(gridPanel, c);
		
		c.weightx = 0;
		c.anchor = GridBagConstraints.NORTHEAST;
		
		gridContainer.add(pauseButton, c);
		gridContainer.setPreferredSize(new Dimension(700, 700));
	}
	/** Sets up the value box for each grid position. */
	private void setupNumberBox(){
		numberBox.setLayout(new GridBagLayout());
		numberBox.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.black));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridy = 0;
		//numberBox.add(numberChoice, gbc);
		numberChoice.addActionListener (new ActionListener () {
			public void actionPerformed(ActionEvent e) {
				setGridSpot((String) numberChoice.getSelectedItem(), false);
			}
		});
	        
		gbc.gridy = 1;
		numberBox.add(showValues, gbc);
		showValues.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					showingValues = true;
					updatePossibleValues();
				} else {
					showingValues = false;
					updatePossibleValues();
				}
			}
		});
		gbc.gridy = 2;
		numberBox.setVisible(false);
	}
	/** Changes the value of a spot on the grid.*/
	public void setGridSpot(String value,  boolean isUndoRedo){
		if(pressable){
			if(!value.equals("Value:") && !value.equals("0")){
				if(puzzle.isValidPlay(currentGridButton.coord, Integer.valueOf(value))){
					if(!undoing){
						addToHistory(currentGridButton.coord, Integer.valueOf(value));
					}
					puzzle.fillValue(currentGridButton.coord, Integer.valueOf(value));
					currentGridButton.setText(value);
				}else{
					SudokuButton b = currentGridButton;
					b.setBackground(Color.RED);
					b.setText(value);
					pressable = false;
					pauseTimer.start();
				}
			}else{
				if(!undoing){
					addToHistory(currentGridButton.coord, 0);
				}
				puzzle.fillValue(currentGridButton.coord, 0);
				currentGridButton.setText("");
				
			}
			checkForWin();
			updatePossibleValues();
		}
	}
	/** Adds the last move to the undo/redo history and deals with changes. */
	private void addToHistory(Coordinate coord, int value){
			if(!gridButtons.get(coord).getText().equals("")){
				if(value != Integer.valueOf(gridButtons.get(coord).getText())){
					history.add(new Move(coord, value, Integer.valueOf(gridButtons.get(coord).getText())));
					historyPosition++;
				}
			}else{
				if(value != 0){
					history.add(new Move(coord, value, 0));
					historyPosition++;
				}
			}
			for(int i = historyPosition; i < history.size(); i ++){
				history.remove(i);
			}
	
	}
	/** Sets up the timer.*/
	private void setupTimer(){
		timerBox.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.black));
		timerLabel.setFont(new Font("Arial", Font.BOLD, 30));
		timerBox.add(timerLabel);
		timerBox.setVisible(false);		
	}
	/** Starts the timer for completing a puzzle. */
	private void startTimer(){
		timerBox.setVisible(true);
		timer = new javax.swing.Timer(1000, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				secondsElapsed++;
				if(secondsElapsed == 60){
					secondsElapsed = secondsElapsed % 60;
					minutesElapsed++;
				}
				timerLabel.setText(String.format("%2s", String.valueOf(minutesElapsed)).replace(' ', '0') + ":" + String.format("%2s", String.valueOf(secondsElapsed)).replace(' ', '0'));
			}			 
		});	
		timer.start();
	}
	/** Sets up the solving functionality. */
	private void setupSolver(){
		solve.setToolTipText("Press to enter the information into the grid then, then solve.");
		solve.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(!solving){
					solving = true;
					pressable = true;
					generatorContainer.setVisible(false);
					loadButton.setVisible(false);
					puzzle = new SudokuPuzzlePlayable();
					solve.setToolTipText("Press to solve with the given puzzle. ");
					undoRedo.setVisible(true);
				}else{
					
					SudokuPuzzle s = new SudokuPuzzle(puzzle.getData());
					s.solve();
					Integer[][] d = s.getData();
					for(Coordinate coord : gridButtons.keySet()){
						SudokuButton sb = gridButtons.get(coord);
						sb.setText(String.valueOf(d[sb.coord.y][sb.coord.x]));
						sb.permanent();
					}
					undoRedo.setVisible(false);
					//numberBox.setVisible(false);
					solve.setVisible(false);
					currentGridButton.setBackground(null);
				}
			}
		});
	}
	/** Sets up the undo and redo buttons.*/
	private void setupUndoRedo(){
		undoRedo.setLayout(new GridBagLayout());
		GridBagConstraints g = new GridBagConstraints();
		g.fill = GridBagConstraints.HORIZONTAL;
		g.gridx = 0;
		g.gridy = 0;
		undo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(historyPosition > 0){
					historyPosition--;
					Move move = history.get(historyPosition);
					SudokuButton lastbutton = currentGridButton;
					currentGridButton = gridButtons.get(move.coord);
					undoing = true;
					setGridSpot(String.valueOf(move.lastValue), true);
					currentGridButton = lastbutton;
					undoing = false;		
				}
			}
		});
		undoRedo.add(undo, g);
		
		g.gridx = 1;
		redo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(historyPosition < history.size()){
					Move move = history.get(historyPosition);
					SudokuButton lastbutton = currentGridButton;
					currentGridButton = gridButtons.get(move.coord);
					undoing = true;
					setGridSpot(String.valueOf(move.value), true);
					currentGridButton = lastbutton;
					historyPosition++;
					undoing = false;
				}
			}
		});
		undoRedo.add(redo, g);
		undoRedo.setVisible(false);
	}
	/**Moves the selected spot on grid.*/
	public void moveSelected(String move){
		Coordinate coord = currentGridButton.coord;
		int x = coord.x, y = coord.y, xInc = 0, yInc = 0;
		if(move.equals("Left")){
			xInc = -1;
		}else if(move.equals("Right")){
			xInc = 1;
		}else if(move.equals("Up")){
			yInc = -1;
		}else{
			yInc = 1;
		}
		x += xInc;
		y += yInc;
		if(x < 0 && xInc == -1) x = 8;
		if(x > 8 && xInc == 1) x = 0;
		if(y < 0 && yInc == -1) y = 8;
		if(y > 8 && yInc == 1) y = 0;
		while(gridButtons.get(new Coordinate(x, y)).isPermanent()){
			x += xInc;
			y += yInc;
			if(x < 0 && xInc == -1) x = 8;
			if(x > 8 && xInc == 1) x = 0;
			if(y < 0 && yInc == -1) y = 8;
			if(y > 8 && yInc == 1) y = 0;
			
		}
		press(gridButtons.get(new Coordinate(x, y)));
	}
	/** Sets up the pause timer for incorrect responses.*/
	private void setupPauseTimer(){
		pauseTimer = new javax.swing.Timer(3000, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				currentGridButton.setBackground(null);
				currentGridButton.setText("");
				numberChoice.setSelectedItem("Value:");
				pressable = true;
			}			 
		});	
		pauseTimer.setRepeats(false);
		//timer.start();
	}
	/** Loads a saved game from a file.*/
	private void loadFile(){
		int returnVal = fileChooser.showOpenDialog(frame);
		undoing = true;
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			ArrayList<String> lines;
			try{
			    Scanner sc = new Scanner(new File(fileChooser.getSelectedFile().getName()));
			    lines = new ArrayList<String>();
			    while (sc.hasNextLine()) {
				lines.add(sc.nextLine());
			    }

			    String[] arr = lines.toArray(new String[0]);
			    
			    String [] gData = arr[0].split("-");
			    difficultiesBox.setSelectedItem(gData[0]);
			    seedInput.setText(gData[1]);
			    generatePuzzleButton.doClick();
			    for(int i = 1; i < 10; i ++){
					String[] newLine = arr[i].replace("_","0").split(",");
						
					for(int k = 0; k < 9; k++){
						SudokuButton b = gridButtons.get(new Coordinate(k, i - 1));
						if(!b.isPermanent()){
							currentGridButton = b;
							setGridSpot(newLine[k], true);
						}
					}
			    }
			    historyPosition = Integer.valueOf(arr[10]) + 1;
			    String[] copiedHistory = arr[11].substring(1, arr[11].length() - 1).replace("), ", "):").split(":");
			    minutesElapsed = Integer.valueOf(arr[12]);
			    secondsElapsed = Integer.valueOf(arr[13]);
			    if(copiedHistory.length > 1) {
					for (String move : copiedHistory) {
						history.add(new Move(move));
					}
				}

			 }catch (IOException e){ 
			        e.printStackTrace(); 
			 } 
		}
		undoing = false;
	}
	/** Saves game data to a file. */
	private void saveFile(){
		fileChooser.setSelectedFile(new File(difficulty + "-" + seed + ".sudoku"));
		int returnVal = fileChooser.showSaveDialog(frame);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			try{
				FileWriter fileWriter = new FileWriter(fileChooser.getSelectedFile().getName());
				PrintWriter printWriter = new PrintWriter(fileWriter);
				printWriter.print(difficulty + "-" + seed + "\n");
				printWriter.print(puzzle.dataForFile());
				printWriter.print(historyPosition + "\n");
				printWriter.print(history + "\n");
				printWriter.print(minutesElapsed + "\n");
				printWriter.print(secondsElapsed);
				printWriter.close();
			}catch(IOException e){
				
			}	
		}
	}
	/** Updates possible values on grid. */
	private void updatePossibleValues(){
		for(Coordinate coord : gridButtons.keySet()){
			SudokuButton sb = gridButtons.get(coord);
			if(sb.getText().equals("")){
				if(showingValues){
					sb.possibleValues.setText(String.valueOf(puzzle.getPossibleValues(sb.coord)));
				}else{
					sb.possibleValues.setText("");
				}
			}else{
				sb.possibleValues.setText("");
			}
			sb.updateToolTip();

		}
	}
	/** Pauses and unpauses the game.*/
	private void pausePlay(){
		isPaused = !isPaused;
		if(isPaused){
			timer.stop();
			numberBox.setVisible(false);
			giveUp.setVisible(false);
			undoRedo.setVisible(false);
			saveButton.setVisible(false);
			gridPanel.setVisible(false);
			pauseButton.setText("PLAY");
		}else{
			timer.start();
			numberBox.setVisible(true);
			giveUp.setVisible(true);
			undoRedo.setVisible(true);
			saveButton.setVisible(true);
			gridPanel.setVisible(true);
			pauseButton.setText("PAUSE");
			
		}
		
	}
}


