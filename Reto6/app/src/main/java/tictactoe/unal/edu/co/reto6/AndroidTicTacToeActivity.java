package tictactoe.unal.edu.co.reto6;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

public class AndroidTicTacToeActivity extends AppCompatActivity {
    static final int DIALOG_DIFFICULTY_ID = 0;
    static final int DIALOG_QUIT_ID = 1;

    // Represents the internal state of the game
    private TicTacToeGame mGame;
    private boolean mGameOver = false;

    private int mHumanWins = 0;
    private int mComputerWins = 0;
    private int mTies = 0;
    private char mGoFirst = TicTacToeGame.HUMAN_PLAYER;

    private boolean mSoundOn = true;

    // Buttons making up the board
    private BoardView mBoardView;

    // Various text displayed
    private TextView mInfoTextView;
    private TextView mHumanScoreTextView;
    private TextView mComputerScoreTextView;
    private TextView mTieScoreTextView;
    private SharedPreferences mPrefs;

    MediaPlayer mHumanMediaPlayer;
    MediaPlayer mComputerMediaPlayer;

    // Listen for touches on the board

    private OnTouchListener mTouchListener = new OnTouchListener() {

        public boolean onTouch(View v, MotionEvent event) {
            // Determine which cell was touched
            int col = (int) event.getX() / mBoardView.getBoardCellWidth();
            int row = (int) event.getY() / mBoardView.getBoardCellHeight();
            int pos = row * 3 + col;

            if (!mGameOver && setMove(TicTacToeGame.HUMAN_PLAYER, pos)) {
                mGoFirst = mGoFirst == TicTacToeGame.HUMAN_PLAYER ? TicTacToeGame.COMPUTER_PLAYER
                        : TicTacToeGame.HUMAN_PLAYER;
                if (mSoundOn) {
                    try {
                        mHumanMediaPlayer.start(); // Play the sound effect
                    } catch (Exception e) {

                    }
                }
                int winner = mGame.checkForWinner();
                if (winner == 0) {
                    mInfoTextView.setText(R.string.turn_computer);
                    turnComputer();
                } else
                    endGame(winner);
            }

            // So we aren't notified of continued events when finger is moved
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_tic_tac_toe);

        mGame = new TicTacToeGame();
        // Restore the scores from the persistent preference data source
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSoundOn = mPrefs.getBoolean("sound", true);
        String difficultyLevel = mPrefs.getString("difficulty_level",
                getResources().getString(R.string.difficulty_harder));
        if (difficultyLevel.equals(getResources().getString(
                R.string.difficulty_easy)))
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy);
        else if (difficultyLevel.equals(getResources().getString(
                R.string.difficulty_harder)))
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Harder);
        else
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Expert);

        mHumanWins = mPrefs.getInt("mHumanWins", 0);
        mComputerWins = mPrefs.getInt("mComputerWins", 0);
        mTies = mPrefs.getInt("mTies", 0);

        mInfoTextView = (TextView) findViewById(R.id.information);
        mHumanScoreTextView = (TextView) findViewById(R.id.player_score);
        mComputerScoreTextView = (TextView) findViewById(R.id.computer_score);
        mTieScoreTextView = (TextView) findViewById(R.id.tie_score);

        mBoardView = (BoardView) findViewById(R.id.board);
        mBoardView.setColor(mPrefs.getInt("board_color", 0xFFCCCCCC));
        // Listen for touches on the board
        mBoardView.setOnTouchListener(mTouchListener);
        mBoardView.setGame(mGame);

        if (savedInstanceState == null) {
            startNewGame();
        } else {
            // Restore the game's state
            mGame.setBoardState(savedInstanceState.getCharArray("board"));
            mGameOver = savedInstanceState.getBoolean("mGameOver");
            mInfoTextView.setText(savedInstanceState.getCharSequence("info"));
            mHumanWins = savedInstanceState.getInt("mHumanWins");
            mComputerWins = savedInstanceState.getInt("mComputerWins");
            mTies = savedInstanceState.getInt("mTies");
            mGoFirst = savedInstanceState.getChar("mGoFirst");

            endGame(mGame.checkForWinner());
            if (!mGameOver) {
                mInfoTextView
                        .setText(mGoFirst == TicTacToeGame.COMPUTER_PLAYER ? R.string.turn_computer
                                : R.string.turn_human);
                mBoardView.invalidate();
            }

        }
        displayScores();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putCharArray("board", mGame.getBoardState());
        outState.putBoolean("mGameOver", mGameOver);
        outState.putInt("mHumanWins", Integer.valueOf(mHumanWins));
        outState.putInt("mComputerWins", Integer.valueOf(mComputerWins));
        outState.putInt("mTies", Integer.valueOf(mTies));
        outState.putCharSequence("info", mInfoTextView.getText());
        outState.putChar("mGoFirst", mGoFirst);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mGame.setBoardState(savedInstanceState.getCharArray("board"));
        mGameOver = savedInstanceState.getBoolean("mGameOver");
        mInfoTextView.setText(savedInstanceState.getCharSequence("info"));
        mHumanWins = savedInstanceState.getInt("mHumanWins");
        mComputerWins = savedInstanceState.getInt("mComputerWins");
        mTies = savedInstanceState.getInt("mTies");
        mGoFirst = savedInstanceState.getChar("mGoFirst");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHumanMediaPlayer = MediaPlayer.create(getApplicationContext(),
                R.raw.human_move);
        mComputerMediaPlayer = MediaPlayer.create(getApplicationContext(),
                R.raw.computer_move);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHumanMediaPlayer.release();
        mComputerMediaPlayer.release();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save the current scores
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putInt("mHumanWins", mHumanWins);
        ed.putInt("mComputerWins", mComputerWins);
        ed.putInt("mTies", mTies);
        ed.commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CANCELED) {
            // Apply potentially new settings
            mSoundOn = mPrefs.getBoolean("sound", true);
            String difficultyLevel = mPrefs.getString("difficulty_level",
                    getResources().getString(R.string.difficulty_harder));
            if (difficultyLevel.equals(getResources().getString(
                    R.string.difficulty_easy))) {
                mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy);

            } else if (difficultyLevel.equals(getResources().getString(
                    R.string.difficulty_harder))) {
                mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Harder);
            } else {
                mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Expert);
            }
            mBoardView.setColor(mPrefs.getInt("board_color", 0xFFCCCCCC));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_game:
                startNewGame();
                return true;
            case R.id.ai_difficulty:
                showDialog(DIALOG_DIFFICULTY_ID);
                return true;
            case R.id.reset:
                mHumanWins = 0;
                mComputerWins = 0;
                mTies = 0;
                displayScores();
                return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case DIALOG_DIFFICULTY_ID:
                builder.setTitle(R.string.difficulty_choose);
                final CharSequence[] levels = {
                        getResources().getString(R.string.difficulty_easy),
                        getResources().getString(R.string.difficulty_harder),
                        getResources().getString(R.string.difficulty_expert) };
                // TODO: Set selected, an integer (0 to n-1), for the Difficulty
                // dialog.
                int selected = 3;
                // selected is the radio button that should be selected.
                builder.setSingleChoiceItems(levels, selected,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                dialog.dismiss(); // Close dialog
                                // TODO: Set the diff level of mGame based on which
                                // item was selected.
                                TicTacToeGame.DifficultyLevel d;
                                if (item == 0) {
                                    d = mGame.getDifficultyLevel().Easy;
                                    mGame.setDifficultyLevel(d);
                                }
                                if (item == 1) {
                                    d = mGame.getDifficultyLevel().Harder;
                                    mGame.setDifficultyLevel(d);
                                }
                                if (item == 2) {
                                    d = mGame.getDifficultyLevel().Expert;
                                    mGame.setDifficultyLevel(d);
                                }
                                // Display the selected difficulty level
                                Toast.makeText(getApplicationContext(),
                                        levels[item], Toast.LENGTH_SHORT).show();
                            }
                        });
                dialog = builder.create();
                break;
        }

        switch (id) {
            case DIALOG_QUIT_ID:
                // Create the quit confirmation dialog
                builder.setMessage(R.string.quit_question)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        AndroidTicTacToeActivity.this.finish();
                                    }
                                }).setNegativeButton(R.string.no, null);
                dialog = builder.create();
                break;
        }

        return dialog;
    }

    // Set up the game board.
    private void startNewGame() {
        mGame.clearBoard();
        mBoardView.invalidate(); // Redraw the board
        mGameOver = false;
        // Human goes first
        mInfoTextView.setText(R.string.first_human);
    }

    private void displayScores() {
        mHumanScoreTextView.setText(Integer.toString(mHumanWins));
        mComputerScoreTextView.setText(Integer.toString(mComputerWins));
        mTieScoreTextView.setText(Integer.toString(mTies));

    }


    private boolean setMove(char player, int location) {
        if (mGame.setMove(player, location)) {
            mBoardView.invalidate(); // Redraw the board
            return true;
        }
        return false;
    }

    private void endGame(int winner) {
        switch (winner) {
            case 0:
                return;
            case 1:
                mInfoTextView.setText(R.string.result_tie);
                mTies++;
                mTieScoreTextView.setText(Integer.toString(mTies));
                break;
            case 2:
                String defaultMessage = getResources().getString(R.string.result_human_wins);
                mInfoTextView.setText(mPrefs.getString("victory_message", defaultMessage));
                mHumanWins++;
                mHumanScoreTextView.setText(Integer.toString(mHumanWins));
                break;
            default:
                mInfoTextView.setText(R.string.result_computer_wins);
                mComputerWins++;
                mComputerScoreTextView.setText(Integer.toString(mComputerWins));
                break;
        }
        mGameOver = true;
    }

    private void turnComputer() {
        int move = mGame.getComputerMove();
        setMove(TicTacToeGame.COMPUTER_PLAYER, move);
        mGoFirst = mGoFirst == TicTacToeGame.HUMAN_PLAYER ? TicTacToeGame.COMPUTER_PLAYER
                : TicTacToeGame.HUMAN_PLAYER;
        mBoardView.invalidate();
        if (mSoundOn) {
            try {
                mComputerMediaPlayer.start(); // Play the sound effect
            } catch (Exception e) {
            }
        }
        int winner = mGame.checkForWinner();
        if (winner == 0) {
            mInfoTextView.setText(R.string.turn_human);
        } else
            endGame(winner);
    }
}