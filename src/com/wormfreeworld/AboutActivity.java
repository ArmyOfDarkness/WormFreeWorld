package com.wormfreeworld;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/*
This activity is called from two main screen buttons and displays info about either the Worm Free World Institute or the Worm Finder Game
 */
public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		Intent i = getIntent();
		int message = i.getIntExtra("message", 0);
		String text;
        String title;
		
		if (message == 0) {
            title = "About Worm Free World";
		    text = "WORMFREE WORLD INSTITUTE is a 501(c)(3) non-profit research institute. " +
				"Our people are world-class scientists dedicated towards finding cures and diagnostics for intestinal roundworm parasites, the leading cause of disease burden in hundreds of millions of children worldwide. " +
				"Cures far better than the ones we have now are urgently needed to free children from these poverty-trapping parasites.";
		} else {
            title = "About Worm Finder";
		    text = "Worm Finder is a classic memory game with a couple of twists.  Players must pick matching " +
				"cards but cannot choose the initial card.  The computer randomly chooses a card and the player has 2 seconds to " +
				"choose the match.  When playing triplet or quadruplet levels (denoted by T or Q in the level chooser), the player must choose two or three matching cards, with a 2 second limit " +
				"to find each match.\n\nYour point score is equal to the number of seconds taken to finish the game plus the number " +
				"of incorrect attempts.\n\nThe Epic Game level is a long game that plays through each of the 12 levels and keeps track of your cumulative score.";
		}

        TextView titleView = (TextView) findViewById(R.id.aboutTitle);
        titleView.setText(title);
		TextView aboutTextView = (TextView) findViewById(R.id.about);
		aboutTextView.setText(text);
	}
}
