package com.wormfreeworld;

import android.app.Activity;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

/*
This activity sets up the main screen buttons and listeners
 */
public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);       
        
        Button newButton = (Button) findViewById(R.id.bNew);
        Button aboutButton = (Button) findViewById(R.id.bAbout);
        Button linkButton = (Button) findViewById(R.id.bLink);
        Button instrButton = (Button) findViewById(R.id.instructions);
        Button scoreButton = (Button) findViewById(R.id.lowscores);
        ImageButton facebook = (ImageButton) findViewById(R.id.facebookLink);
        ImageButton twitter = (ImageButton) findViewById(R.id.twitterLink);
        
        final Spinner levelChooser = (Spinner) findViewById(R.id.levelChooser);
        ArrayAdapter<CharSequence> levelAdapter = ArrayAdapter.createFromResource(this, R.array.levels, android.R.layout.simple_spinner_item);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelChooser.setAdapter(levelAdapter);
        levelChooser.getBackground().setColorFilter(new LightingColorFilter(0xee77ee, 0x000000));
        
        final Spinner cardChooser = (Spinner) findViewById(R.id.cardChooser);
        ArrayAdapter<CharSequence> cardAdapter = ArrayAdapter.createFromResource(this, R.array.cardSets, android.R.layout.simple_spinner_item);
        cardAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cardChooser.setAdapter(cardAdapter);
        cardChooser.getBackground().setColorFilter(new LightingColorFilter(0xee77ee, 0x000000));
        
        newButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, GameActivity.class);
                int level = levelChooser.getSelectedItemPosition() + 1;
                int cardSet = cardChooser.getSelectedItemPosition();
                i.putExtra("level", level);
                i.putExtra("cardset", cardSet);
                startActivity(i);
            }
        });
        
        aboutButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, AboutActivity.class);
				int message = 0;
				i.putExtra("message", message);
				startActivity(i);
			}
		});
        
        linkButton.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				Intent webLink = new Intent(android.content.Intent.ACTION_VIEW);
				webLink.setData(Uri.parse("http://www.wormfreeworld.org"));
				startActivity(webLink);
			}
		});
        
        facebook.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent facebookLink = new Intent(android.content.Intent.ACTION_VIEW);
				facebookLink.setData(Uri.parse("http://www.facebook.com/pages/Wormfree-World/271474386196491"));
				startActivity(facebookLink);
			}
		});

        twitter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent twitterLink = new Intent(Intent.ACTION_VIEW);
                twitterLink.setData(Uri.parse("http://www.twitter.com/WFWIns"));
                startActivity(twitterLink);
            }
        });
        
        instrButton.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, AboutActivity.class);
				int message = 1;
				i.putExtra("message", message);
				startActivity(i);
			}
		});
        
        scoreButton.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				Intent i = new Intent(MainActivity.this, ScoreActivity.class);
				startActivity(i);
			}
		});
    }
}
