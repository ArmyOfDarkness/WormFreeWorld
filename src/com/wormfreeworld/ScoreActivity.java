package com.wormfreeworld;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

/*
This activity fetches low scores for each level from SharedPreferences and allows user to reset scores.
 */
public class ScoreActivity extends Activity {

	SharedPreferences lowScores;
	Button resetButton;
	AlertDialog.Builder builder;
	final String[] levels = {"Level 1", "Level 2", "Level 3", "Level 4", "Level 5", "Level 6", "Level 7", "Level 8", "Level 9", "Level 10", "Level 11", "Level 12", "Epic Game"};
	final int numLevels = 13;
	int[] scores = new int[numLevels];
	ScoreAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_score);
		ListView scoreList = (ListView) findViewById(R.id.list);
		lowScores = this.getSharedPreferences("com.wormfreeworld", Context.MODE_PRIVATE);

        //defines row view for each level
		adapter = new ScoreAdapter(this, levels, scores);
		scoreList.setAdapter(adapter);
		
		getScores();
		
		resetButton = (Button) findViewById(R.id.reset);

        //confirmation for reset scores
		builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure?");
		builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {			
			public void onClick(DialogInterface dialog, int which) {
                for (String level : levels) {
                    lowScores.edit().putInt(level, 9999).commit();
                }
				getScores();
				adapter.notifyDataSetChanged();
			}
		});
		builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {			
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});		
		
		resetButton.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				builder.show();				
			}
		});
	}

	private void getScores() {
		for (int i = 0; i < numLevels; i++) {
			scores[i] = lowScores.getInt(levels[i], 9999);
		}
	}
}


