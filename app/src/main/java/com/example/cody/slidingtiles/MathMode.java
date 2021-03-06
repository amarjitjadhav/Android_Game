package com.example.cody.slidingtiles;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.Toast;

import java.util.ArrayList;

public class MathMode extends AppCompatActivity {

    //Get from Base App
    private String playerName;
    //Firebase stuff
    DatabaseHandler db = new DatabaseHandler();
    boolean highScoreReached = false;
    //Board Resources
    int tileMatrix[][] = new int [5][5];
    float xTileDistance = 0;
    float yTileDistance = 0;
    private float ySubmittedTile;
    private float xSubmittedTile;
    private int axisLock;   // 1 = Vertical solution; 2 = Horizontal solution
    int currentScore = 0;
    private ArrayList<String> solutionList;

    //UI Elements
    Button emptyTileButton;
    GridLayout board;
    ViewGroup submissionHistoryWindow;

    // Timer variables
    //private Button startButton;
    private Button pauseButton;
    private TextView timerValue;
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;

    //Popup window
    final Context context = this;

    //Helper Classes
    MathSolutionHandler equationHandler = new MathSolutionHandler();
    BoardGenerator boardGen = new BoardGenerator();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_mode);

        // Get player name
        playerName = ((BaseApp)this.getApplicationContext()).playerName;
        // Timer implementation
        timerValue = (TextView) findViewById(R.id.timerValue);
        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);

        pauseButton = (Button) findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                timeSwapBuff += timeInMilliseconds;
                customHandler.removeCallbacks(updateTimerThread);


                // -------------------------- dialouge popup -------------------------//
                // custom dialog
                final Dialog dialog = new Dialog(context);
                dialog.getWindow().setGravity(Gravity.CENTER);
                dialog.setContentView(R.layout.popup);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.setCanceledOnTouchOutside(false);


                Button resumeButton = (Button) dialog.findViewById(R.id.resume);
                Button closeButton = (Button) dialog.findViewById(R.id.exit);

                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // -------------------------- inside dialog ---------------------------- //
                        dialog.dismiss();
                        // custom dialog
                        final Dialog dialog1 = new Dialog(context);
                        dialog1.getWindow().setGravity(Gravity.CENTER);
                        dialog1.setContentView(R.layout.popup_player_score);
                        dialog1.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                        dialog1.setCanceledOnTouchOutside(false);

                        //dialog.setTitle("Title.");
                        TextView playerID = (TextView) dialog1.findViewById(R.id.player_name) ;
                        playerID.setText(playerName);
                        TextView gameScore = (TextView) dialog1.findViewById(R.id.player_score);
                        gameScore.setText("Your Score: " + currentScore);
                        Button closeButton1 = (Button) dialog1.findViewById(R.id.exit1 );
                        LinearLayout validSubmission = dialog1.findViewById(R.id.validSubmissionHistory);
                        for(String equation : solutionList){
                            TextView addDisplay = new TextView(context);
                            addDisplay.setText(equation);
                            validSubmission.addView(addDisplay,0);
                        }

                        //Submit score
                        db.pushToMathMode(playerName, currentScore);

                        closeButton1.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View view) {
                                dialog1.dismiss();
                                finish();
                                //System.exit(0);

                            }
                        });

                        dialog1.show();
                        // -------------------------- inside dialog end---------------------------- //

                    }
                });
                resumeButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        startTime = SystemClock.uptimeMillis();
                        customHandler.postDelayed(updateTimerThread, 0);
                        dialog.dismiss();

                    }
                });

                dialog.show();
                // -------------------------- dialogue popup end ---------------------//
            }
        });

        //Create a 2-D array of the board
        tileMatrix = boardGen.generateMathModeBoard();
//        boardGen.shuffleBoard(tileMatrix);
        solutionList = new ArrayList<>();

        //Move the contents of the 2-D array to the UI
        board = findViewById(R.id.board);
        displayBoardMatrixUI(board);

        //Find and active the shuffle button
        Button btnShuffle = findViewById(R.id.btnShuffle);
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            DemandShuffle(tileMatrix);
            }
        });

        //Find the submission history window
        submissionHistoryWindow = findViewById(R.id.submissionHistory);

    }

    // Timer code
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            timerValue.setText("" + mins + ":"
                    + String.format("%02d", secs) + ":"
                    + String.format("%03d", milliseconds));
            customHandler.postDelayed(this, 0);
        }
    };


    // Takes a 2-d array and maps it to UI elements
    protected void displayBoardMatrixUI(GridLayout board) {
        Button tile;
        int tileCount = 0;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                tile = (Button) board.getChildAt(tileCount);
                switch(tileMatrix[i][j]) {
                    case -1:
                        tile.setText(" ");
                        emptyTileButton = tile;
                        break;
                    case 10:
                        tile.setText("=");
                        break;
                    case 11:
                        tile.setText("+");
                        break;
                    case 12:
                        tile.setText("-");
                        break;
                    case 13:
                        tile.setText("*");
                        break;
                    case 14:
                        tile.setText("/");
                        break;
                    default:
                        tile.setText(Integer.toString(tileMatrix[i][j]));
                        break;
                }
                tileCount ++;
            }
        }
    }

    // Function that determines how far apart tile are.
    // The distance is dependent on screen size.
    // This should be called in the moveTile() method.
    // Calculates distances using tiles located in the lower right corner of the board.
    private void obtainTileDistance() {
        View xButton = findViewById(R.id.xButton);
        View yButton = findViewById(R.id.yButton);
        View lowerRightButton = findViewById(R.id.lowerRightButton);

        xTileDistance = Math.abs(xButton.getX() - lowerRightButton.getX());
        yTileDistance = Math.abs(yButton.getY() - lowerRightButton.getY());
    }

    // General onTouchEvent used to submit player solutions
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int xPos = (int) event.getX();
        int yPos = (int) event.getY();

        int action = event.getAction();

        // While the user's finger is on the screen, lets Record their submission.
        // When the user lifts up their finger, that signals the end of their submission
        if (action == MotionEvent.ACTION_UP) {
            TextView submission = new TextView(this);
            if (equationHandler.getCountOfSubmittedTiles() != 0) {
                int score = equationHandler.solve();
                if (score == -1) {          // Invalid equation
                    submission.setTextColor(Color.RED);
//                } else if(score == 0 ) {
//                    submission.setTextColor(Color.BLUE);
                } else if(score == -2 ) {   // Incorrect format
                    submission.setTextColor(Color.YELLOW);
                } else if (score == -3) {   // Already Used
                    submission.setTextColor(Color.DKGRAY);
                } else {
                    submission.setTextColor(Color.GREEN);
                    updateScore(score);
                    if (!highScoreReached && db.checkForNewMathHighScore(currentScore)) {
                        Toast.makeText(this, "New High Score!", Toast.LENGTH_LONG).show();
                        highScoreReached = true;
                    }
                    solutionList.add(equationHandler.getEquationString());
                }
                submission.setTextSize(20);
                submission.setBackgroundColor(Color.GRAY);
                submission.setText(equationHandler.getEquationString());
                submissionHistoryWindow.addView(submission, 0);
                equationHandler.resetHandler();
            }
            return true;
        } else {
            Button tile = (Button) findViewAt(board, xPos, yPos);
            if (tile != null && (equationHandler.getCountOfSubmittedTiles() != 5)) {
                // If this is the first time we are calling this, lets get the tile distances
                if (xTileDistance == 0) {
                    obtainTileDistance();
                }
                // The following logic only allows horizontal or vertical solutions
                int tileCount = equationHandler.getCountOfSubmittedTiles();
                if (tileCount == 0) {
                    //This if the first submitted tile, lets get the x,y coords
                    xSubmittedTile = tile.getX();
                    ySubmittedTile = tile.getY();
                    equationHandler.addTile(tile);
                }else if(tileCount == 1) {
                    //This is the second tile we are attempting to add.
                    //Make sure this tile is next to the first tile
                    //Also depending on its location (above/below or left/right) lock future submissions to either horizontal or vertical
                    if ((Math.abs(tile.getY() - ySubmittedTile) == yTileDistance) && (tile.getX() == xSubmittedTile)) {
                        //Vertical Submission
                        axisLock = 1;
                        ySubmittedTile = tile.getY();
                        equationHandler.addTile(tile);
                    }else if ((Math.abs(tile.getX() - xSubmittedTile) == xTileDistance) && (tile.getY() == ySubmittedTile)) {
                        //Horizontal Submission
                        axisLock = 2;
                        xSubmittedTile = tile.getX();
                        equationHandler.addTile(tile);
                    }
                } else {
                    //We are attempting to add tiles 3-5
                    if (axisLock == 1) {    //Vertical Submission
                        //Check to see if this solution is directly above or below the previous tile
                        if ((Math.abs(tile.getY() - ySubmittedTile) == yTileDistance) && (tile.getX() == xSubmittedTile)) {
                            ySubmittedTile = tile.getY();
                            equationHandler.addTile(tile);
                        }
                    }else {                 //Horizontal Submission
                        if ((Math.abs(tile.getX() - xSubmittedTile) == xTileDistance) && (tile.getY() == ySubmittedTile)) {
                            xSubmittedTile = tile.getX();
                            equationHandler.addTile(tile);
                        }
                    }
                }
            }
            return true;
        }
    }

    // Finds view within a Gridlayout
    // x & y are coordinates relative to layout.
    // Code Courtesy of Luke, with slight modifications by Joseph Venetucci
    // https://stackoverflow.com/a/36037991
    private View findViewAt(GridLayout viewGroup, int x, int y) {
        for(int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof GridLayout) {
                View foundView = findViewAt((GridLayout) child, x, y);
                if (foundView != null && foundView.isShown()) {
                    return foundView;
                }
            } else {
                int[] location = new int[2];
                child.getLocationOnScreen(location);
                Rect rect = new Rect(location[0], location[1], location[0] + child.getWidth(), location[1] + child.getHeight());
                if (rect.contains(x, y)) {
                    return child;
                }
            }
        }
        return null;
    }

    // Switches a tiles position with the empty tile
    // A valid move is if the tile to be moved and the empty tile:
    // 1) Differ by xTileDistance|yTileDistance units in either the x or y plane,
    // 2) Have the same value in the remaining plane.
    public void moveTile(View tile) {
        // If this is the first time we are calling this, lets get the tile distances
        if (xTileDistance == 0) {
            obtainTileDistance();
        }

        float currentX = tile.getX();
        float currentY = tile.getY();

        float emptyY = emptyTileButton.getY();
        float emptyX = emptyTileButton.getX();

        if (((Math.abs(currentX - emptyX) == xTileDistance) && (currentY == emptyY)) || ((Math.abs(currentY - emptyY) == yTileDistance) && (currentX == emptyX))) {
            //Code that moves the TextViews
            tile.animate().x(emptyX).y(emptyY);
            emptyTileButton.animate().x(currentX).y(currentY);
        }
    }

    // Force he board to be shuffled and redisplayed.
    public void DemandShuffle(int tileMatrix[][]){
        boardGen.shuffleBoard(tileMatrix);
        displayBoardMatrixUI(board);
    }

    // Updates the score accordingly
    private void updateScore(int score){
        currentScore += score;
        TextView playerScore = findViewById(R.id.currentScoreTextView);
        playerScore.setText(String.valueOf(currentScore));
    }
}


