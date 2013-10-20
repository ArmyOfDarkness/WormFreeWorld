package com.wormfreeworld;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/*
Adapter to create row views containing level names and stored scores
 */
public class ScoreAdapter extends ArrayAdapter<String> {
	
	private Context context;
	private String[] levels;
	private int[] scores;

	public ScoreAdapter(Context context, String[] levels, int [] scores) {
		super(context, R.layout.scorerow, levels);
	    this.context = context;
	    this.levels = levels;
	    this.scores = scores;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.scorerow, parent, false);
		TextView level = (TextView) rowView.findViewById(R.id.scoreLevel);
		TextView score = (TextView) rowView.findViewById(R.id.score);
		level.setText(levels[position]);
		score.setText(Integer.toString(scores[position]));
		return rowView;
	}
	
}
