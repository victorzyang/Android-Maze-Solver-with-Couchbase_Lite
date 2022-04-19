package com.example.assignment2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import androidx.core.content.ContextCompat;
import android.util.Log;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDictionary;
import com.couchbase.lite.Query;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.Document;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Expression;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.CouchbaseLite;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.Result;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.URLEndpoint;

// import com.couchbase.lite.ReplicatorType;
import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.DataSource;
import android.util.Log;
import com.couchbase.lite.internal.CouchbaseLiteInternal.*;
import java.net.URI;
import java.net.URISyntaxException;

import com.couchbase.lite.Database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG =  MainActivity.class.getSimpleName();
    public static final String DATABASE = "DATABASE";

    // Scale of the game (number of rows and columns)
    private  static final int NUM_ROWS = 13;
    private  static final int NUM_COLS = 10;

    private  Button solveMazeButton;
    private Button viewDatabaseButton;

    Button buttons[][] = new Button[NUM_ROWS][NUM_COLS]; //add buttons to this array

    //have another double array of ints to determine whether buttons should become start, destination, wall or blank cells
    String buttonCells[][] = new String[NUM_ROWS][NUM_COLS];

    //coordinates of the starting cell
    int startX = 0;
    int startY = 0;

    //coordinates of the goal cell
    int goalX = 0;
    int goalY = 0;

    Button startButton;

    boolean startCellSelected = false;
    boolean destinationCellSelected = false;

    ThreadWithControl mThread; //ThreadWithControl object, this is a thread

    ButtonCell buttonCell;

    private Context cntx = this; //needed for database
    Database database; //Database instance used for inserting and reading database data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // One-off initialization
        CouchbaseLite.init(cntx);
        Log.i(TAG,"Initialized CBL");

        // Step 1 (from online doc): Create a database (this step looks ok)
        Log.i(TAG, "Starting DB");
        final DatabaseConfiguration cfg = new DatabaseConfiguration(); //Does this need to be final?
        cfg.setDirectory(cntx.getFilesDir().getAbsolutePath()); //Do I need to have this line?
        database = null;
        try {
            Log.i(TAG, "Creating Database");
            database = new Database(  "mazes", cfg);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        // Adding buttons with UI Threads
        TableLayout gameLayout = (TableLayout) findViewById(R.id.gameTable); //layout of the maze

        for(int r = 0; r < NUM_ROWS; r++){

            TableRow tableRow = new TableRow(MainActivity.this); //create a new table row
            tableRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.MATCH_PARENT,
                    1.0f
            ));
            gameLayout.addView(tableRow); //add table row to gameLayout

            for(int c = 0; c < NUM_COLS; c++){ //add a new button in each column of the row
                /* If I wanted to add more cells per row, I would increase
                the number of iterations here to the desired amount of cells.

                For example, if I wanted to add two more cells, I would change the for loop
                range to NUM_COLS+2 */

                final Button button = new Button(MainActivity.this); //create a new button

                button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.empty));
                button.setText("E"); //initialize each button to empty

                final int finalC = c;
                final int finalR = r;

                button.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        if(startCellSelected == false){ //if no start cell has been selected yet
                            if(buttonCells[finalR][finalC] == "destination"){ //if clicked cell is the destination cell
                                button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.empty));
                                button.setText("E");
                                destinationCellSelected = false;
                                buttonCells[finalR][finalC] = "empty"; //set the cell to an empty cell
                            }else{
                                button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.start));
                                button.setText("S");
                                startCellSelected = true;
                                buttonCells[finalR][finalC] = "start"; //set the cell to the start cell
                                startX = finalC;
                                startY = finalR;
                                startButton = buttons[finalR][finalC];
                            }
                        }else if(destinationCellSelected == false){ //if no destination cell has been selected yet
                            if(buttonCells[finalR][finalC] == "start"){ //if clicked cell is the start cell
                                button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.empty));
                                button.setText("E");
                                startCellSelected = false;
                                buttonCells[finalR][finalC] = "empty"; //set the cell to an empty cell
                            }else{
                                button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.destination));
                                button.setText("D");
                                destinationCellSelected = true;
                                buttonCells[finalR][finalC] = "destination"; //set the cell to the destination cell
                                goalX = finalC;
                                goalY = finalR;
                            }
                        }else{
                            if(buttonCells[finalR][finalC] == "empty"){ //if clicked cell is an empty cell
                                button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.wall));
                                button.setText("W");
                                buttonCells[finalR][finalC] = "wall"; //set the cell to a wall cell
                            }else{
                                if(buttonCells[finalR][finalC] == "start"){ //if clicked cell is the start cell
                                    startCellSelected = false;
                                }else if(buttonCells[finalR][finalC] == "destination"){ //if clicked cell is the destination cell
                                    destinationCellSelected = false;
                                }

                                button.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.empty));
                                button.setText("E");
                                buttonCells[finalR][finalC] = "empty"; //set the cell to an empty cell
                            }
                        }

                    }
                });

                TableRow.LayoutParams params = new TableRow.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT,
                        TableLayout.LayoutParams.MATCH_PARENT,
                        1.0f
                );

                params.setMargins(4,4,4,4);
                button.setLayoutParams(params);
                button.setPadding(2,2,2,2);

                tableRow.addView(button); //adds button to table row
                buttons[r][c] = button; //adds button to buttons array
                buttonCells[r][c] = "empty"; //initialize each button in array to empty
            }

        }

        solveMazeButton = (Button) findViewById(R.id.button_solve_maze);
        solveMazeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Solve Button clicked");

                // Step 2 (from online doc): create document (i.e. a record) in the database. (I have this done in the ButtonCell class actually)
                MutableDocument mazeDoc = new MutableDocument(); //document ID is randomly generated by database

                for(int r = 0; r < NUM_ROWS; r++){
                    for(int c = 0; c < NUM_COLS; c++){
                        buttons[r][c].setEnabled(false); //disables all buttons
                    }
                }

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); //disables screen orientation

                buttonCell = new ButtonCell(buttons, buttonCells, NUM_ROWS, NUM_COLS, startX, startY, goalX, goalY, MainActivity.this, database);

                Log.i(TAG, "Starting X is " + startX + " and starting Y is " + startY);

                //pass the two arrays to ThreadWithControl
                mThread = new ThreadWithControl(buttonCell, startX, startY);

                mThread.start(); //'start()' is a built in Thread method

            }
        });

        //button for viewing all mazes in the Maze table
        viewDatabaseButton = findViewById(R.id.button_view_data);
        viewDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Maybe take a look at fetching?

                try {
                    ResultSet rs = QueryBuilder
                            .select(
                                    SelectResult.expression(Meta.id),
                                    SelectResult.property("maze"))
                            .from(DataSource.database(database))
                            .where(Expression.property("document_type").equalTo(Expression.string("maze")))
                            .execute(); //query for getting all maze documents
                    StringBuffer buffer = new StringBuffer();
                    Log.i(TAG, "Selecting all mazes from Database");
                    for (Result result : rs) {
                        String mazesDictToString = "{Id: ";
                        mazesDictToString += result.getString("id"); //document ID is randomly generated by database
                        mazesDictToString += ", # of Rows: ";
                        mazesDictToString += result.getDictionary("maze").getInt("numRows");
                        mazesDictToString += ", # of Columns: ";
                        mazesDictToString += result.getDictionary("maze").getInt("numCols");
                        mazesDictToString += ", Start (X): ";
                        mazesDictToString += result.getDictionary("maze").getInt("startX");
                        mazesDictToString += ", Start (Y): ";
                        mazesDictToString += result.getDictionary("maze").getInt("startY");
                        mazesDictToString += ", Goal (X): ";
                        mazesDictToString += result.getDictionary("maze").getInt("goalX");
                        mazesDictToString += ", Goal (Y): ";
                        mazesDictToString += result.getDictionary("maze").getInt("goalY");
                        mazesDictToString += ", Walls (X): [";
                        for (int w = 0; w < result.getDictionary("maze").getArray("wallsX").count(); w++) {
                            mazesDictToString += result.getDictionary("maze").getArray("wallsX").getInt(w);
                            if(w != result.getDictionary("maze").getArray("wallsX").count()-1){
                                mazesDictToString += ", ";
                            }
                        }
                        mazesDictToString += "], Walls (Y): [";
                        for (int w = 0; w < result.getDictionary("maze").getArray("wallsY").count(); w++) {
                            mazesDictToString += result.getDictionary("maze").getArray("wallsY").getInt(w);
                            if(w != result.getDictionary("maze").getArray("wallsY").count()-1){
                                mazesDictToString += ", ";
                            }
                        }
                        mazesDictToString += "], Does Solution Exist? ";
                        mazesDictToString += result.getDictionary("maze").getBoolean("solutionExists");
                        mazesDictToString += "}";
                        buffer.append("Mazes: " + mazesDictToString + "\n\n");
                    }

                    //Show all data
                    showMessage("Database Data", buffer.toString());
                } catch (CouchbaseLiteException e) {
                    Log.e("Sample", e.getLocalizedMessage());
                }

            }
        });

    }

    //method for displaying an alert showing data from a database table
    private void showMessage(String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }
}