package com.wormfreeworld;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.facebook.*;
import com.facebook.widget.LoginButton;
import com.facebook.widget.WebDialog;

import java.util.*;

/*
This activity creates the Worm Finder game
 */
public class GameActivity extends Activity {
	private ImageButton turnover, last;
	private ImageButton match, match2, match3;
	private int matchesLeft, attempts, level;
	private final int maxLevel = 12;
	private int rows, columns;
	private Thread thread;
	private Handler handler;
	private Handler timer;
	private ArrayList<Drawable> cardList;
	private Chronometer chrono;
	private TextView attemptView, showLevel, matchGoal;
    private final List<ImageButton> buttons = new ArrayList<ImageButton>();
    private final List<ImageButton> buttonsLeft = new ArrayList<ImageButton>();
	private boolean isPaused;
	private boolean isClickable;
    private boolean triplets = false, quadruplets = false;
	private TableLayout gameTable;
	private Context context;
	private OnClickListener buttonListener;
    private boolean isStarted, isEnded;
	private Button startButton;
    private int timeScore, attemptScore, totalScore;
    private String currentLevel = null;
    private SharedPreferences lowScores;
    private int currentLow;
    private boolean epicMode = false;
    private int epicScore = 0;
    private int cardSet;

    //for Facebook integration
	private Button shareButton;
    private LoginButton authButton;
	private UiLifecycleHelper uiHelper;
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions", "publish_stream");
    private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
    private boolean pendingPublishReauthorization = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        Intent i = getIntent();
        level = i.getIntExtra("level", 1);
        cardSet = i.getIntExtra("cardset", 0);
        epicMode = false;
        setContentView(R.layout.gametable);
        gameTable = (TableLayout) findViewById(R.id.gameTable);
		context = gameTable.getContext();
		final TextView score = (TextView) findViewById(R.id.score);
		chrono = (Chronometer) findViewById(R.id.chrono);
		attemptView = (TextView) findViewById(R.id.tvNumAttempts);
		lowScores = this.getSharedPreferences("com.wormfreeworld", Context.MODE_PRIVATE);
		showLevel = (TextView) findViewById(R.id.tvLevelNum);
		matchGoal = (TextView) findViewById(R.id.tvMatchGoal);
		
        startButton = (Button) findViewById(R.id.bStart);
        startButton.getBackground().setColorFilter(new LightingColorFilter(0x0000FF, 0x000000));

        shareButton = (Button) findViewById(R.id.shareButton);
        authButton = (LoginButton) findViewById(R.id.authButton);

        if (savedInstanceState != null) {
            pendingPublishReauthorization =
                    savedInstanceState.getBoolean(PENDING_PUBLISH_KEY, false);
        }

        //save match variables on user tap on buttons depending on doublets, triplets, quadruplets
        //increase # attempts on first tap after new turnover card
        buttonListener = new OnClickListener() {
			public void onClick(View button) {
				if (!isClickable) {return;}
				
				if (quadruplets) {
					if (match != null) {
						if (match2 != null) {
							match3 = (ImageButton) button;
							isClickable = false;
						} else {
							match2 = (ImageButton) button;
						}
					} else {
						match = (ImageButton) button;
						attempts++;
						attemptView.setText(Integer.toString(attempts));
					}
				} else if (triplets) {
					if (match != null) {
						match2 = (ImageButton) button;
						isClickable = false;
					} else {
						match = (ImageButton) button;
						attempts++;
				    	attemptView.setText(Integer.toString(attempts));
					}					
				} else {
			    	isClickable = false;
			    	match = (ImageButton) button;
			    	attempts++;
			    	attemptView.setText(Integer.toString(attempts));
				}				
		    	
		    	synchronized(thread) {
		    		thread.notify();
				}
            }
        };                

        //handle action on tapping start button
        //either start game, pause, resume, next level, or return to MainActivity depending on conditions
        startButton.setOnClickListener(new View.OnClickListener() {			
			public void onClick(View v) {
				if (isEnded) {
					if (epicMode) {
						if (level > maxLevel) {
							finish();
							return;
						}
						setupGame();
						startButton.setText("Start");
						return;
					} else {
                        /*
						finish();
						startActivity(getIntent());
						return;*/
                        setupGame();
                        return;
					}
				}
				if (!isStarted) {
					isStarted = true;
					startButton.setText("Pause");
					startGame();
					return;
				}
				if (isPaused) {
					startButton.setText("Pause");
				}
				else startButton.setText("Resume");
				isPaused = !isPaused;
			}
		});

        //share new low score accomplishment to facebook
        shareButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
                publishStory();
            }
		});

        //handle UI change messages from game thread
        handler = new Handler() {
    		@Override
    		public void handleMessage(Message msg) {
    			if (msg.what == 0) { //make cards invisible if successful match
    				ImageButton ib = (ImageButton) msg.obj;
    				ib.setVisibility(View.INVISIBLE);
    			} else if (msg.what == 1) { //turn card over to reveal picture
    				ImageButton ib = (ImageButton) msg.obj;
    				Drawable color = (cardList.get(ib.getId() - 1));
    				ib.setImageDrawable(color);    				
    			} else if (msg.what == 2) { //turn card facedown
    				ImageButton ib = (ImageButton) msg.obj;
    				ib.setImageResource(R.drawable.ic_grey);
    			} else if (msg.what == 3) { //make all cards visible and face up after end of game
    				for (ImageButton butt : buttons) {
    					butt.setVisibility(View.VISIBLE);
    					Drawable color = (cardList.get(butt.getId() - 1));
    					butt.setImageDrawable(color);
    				}
                    //calculate score and display message and sharing options if new low score
    				totalScore = timeScore + attemptScore;
    				score.setText("Score - " + totalScore);
    				if (totalScore < currentLow) {
    					lowScores.edit().putInt(currentLevel, totalScore).commit();
    					Toast.makeText(getApplicationContext(), "New low! Share on Facebook!", Toast.LENGTH_SHORT).show();
    					//share on Facebook
    					Session session = Session.getActiveSession();
    					authButton.setVisibility(View.VISIBLE);
    					shareButton.setVisibility(View.VISIBLE);
    				}
    				if (epicMode) {
    					epicScore += totalScore;
    					if (level == maxLevel) {
    						score.append("\nEpic Game Score - " + epicScore);
    						if (epicScore < lowScores.getInt("Epic Game", 9999)) {
    							lowScores.edit().putInt("Epic Game", epicScore).commit();
    							Toast.makeText(getApplicationContext(), "New Epic Game low!", Toast.LENGTH_SHORT).show();
    						}
                            level += 1;
                            startButton.setText("Main Menu");
    					} else {
                            score.append("\nCumulative Epic Score - " + epicScore);
    						level += 1;
    						startButton.setText("Next Level");
    					}
    				} else {
    					startButton.setText("Play Again");
    				}
   				
    			}
    		}
    	};
    	
    	setupGame();
    }

    //setup game table depending on user selections in MainActivity
    private void setupGame() {
    	triplets = quadruplets = false;    	
    	switch (level) {
        case 1: rows = 4;
        		columns = 4;
        		break;
        case 2: rows = 4;
				columns = 3;
				triplets = true;
				break;
        case 3: rows = 4;
				columns = 3;
				quadruplets = true;
				break;
        case 4: rows = 5;
				columns = 4;
        		break;
        case 5: rows = 5;
				columns = 3;
				triplets = true;
				break;	
        case 6: rows = 4;
				columns = 4;
				quadruplets = true;
				break;
        case 7: rows = 6;
        		columns = 4;
        		break;
        case 8: rows = 6;
				columns = 4;
				triplets = true;
        		break;
        case 9: rows = 5;
        		columns = 4;
        		quadruplets = true;
        		break;
        case 10: rows = 6;
        		columns = 5;        		
        		break;
        case 11: rows = 6;
        		columns = 5;
        		triplets = true;
        		break;
        case 12: rows = 6;
        		columns = 4;
        		quadruplets = true;
        		break;   
        case 13: epicMode = true;
				rows = 4;
				columns = 4;
				level = 1;
				break;
        }
    	currentLevel = "Level " + Integer.toString(level);
        matchesLeft = rows * columns / (quadruplets ? 4 : triplets ? 3 : 2);
        currentLow = lowScores.getInt(currentLevel, 9999);
        isStarted = false;
        isEnded = false;
        authButton.setVisibility(View.INVISIBLE);
        shareButton.setVisibility(View.INVISIBLE);
        getButtons();
        chrono.setBase(SystemClock.elapsedRealtime());
        startButton.setText("Start");
        attemptView.setText("0");
        showLevel.setText(Integer.toString(level));
        matchGoal.setText(quadruplets ? "quadruplets" : triplets ? "triplets" : "doublets");
        final TextView score = (TextView) findViewById(R.id.score);
        score.setText("Current low score - " + currentLow);
        if (epicMode) score.append("\nCumulative Epic Score - " + epicScore);
    }

    //start timer, get random set of cards for game table and call game thread
    private void startGame() {
    	isEnded = false;
    	attempts = 0;
    	attemptView.setText(Integer.toString(attempts));

        //start clock
    	chrono.setBase(SystemClock.elapsedRealtime());
    	chrono.start();

        //pause and resume clock on pause button clicks
    	timer = new Handler() {
    		public void handleMessage(Message msg) {
    			if (msg.what == 0) {
        			chrono.stop();
        		} else if (msg.what == 1) {
        			long difference = (long) msg.obj;
        			chrono.setBase(chrono.getBase() + difference);
        			chrono.start();
        		}
    		}    		
    	};

        //set up cards and call game thread
    	setCards(cardSet);    	
    	turnover();
    }

    private void turnover() {
	    final Random rand = new Random();	    
	    
	    buttonsLeft.addAll(buttons);

        //run game on separate thread so wait commands won't lock up UI thread
	    thread = new Thread() {
	    	@Override
	     	public void run() {
	            while (matchesLeft > 0) {
	        	
                    //pause loop
                    if (isPaused) {
                        long clickTime = SystemClock.elapsedRealtime();
                        while (isPaused) {
                            Message stopTime = new Message();
                            stopTime.what = 0;
                            timer.sendMessage(stopTime);
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                        long difference = SystemClock.elapsedRealtime() - clickTime;
                        Message startTime = new Message();
                        startTime.what = 1;
                        startTime.obj = difference;
                        timer.sendMessage(startTime);
                    }
	        	
                    //pick random card and turn over
                    do {
                        int random = rand.nextInt((2 + (quadruplets ? 2 : triplets ? 1 : 0)) * matchesLeft);
                        turnover = buttonsLeft.get(random);
                    } while (turnover == last);  //make sure the same card isn't picked twice in a row
                    last = turnover;

                    match = null;
                    match2 = null;
                    match3 = null;

                    revealCard(turnover);

                    //wait 2s for user attempt
                    isClickable = true;
                    try {
                        synchronized(this) {
                            wait(2000);
                        }
                    } catch(InterruptedException e) {}

                    //check user choice for match, update View on UI thread
		        	if (match != null) {
			        	if ((cardList.get(match.getId() - 1)) == (cardList.get(turnover.getId() - 1))
			        		&& match != turnover) {
			        		if (quadruplets) {
			        			revealCard(match);
			        			try {
			        				synchronized(this) {
			        					wait(2000);
			        				}
			        			} catch(InterruptedException e) {}

			        			if (match2 != null) {
			        				revealCard(match2);
                                    if ((cardList.get(match2.getId() - 1)) == (cardList.get(turnover.getId() - 1))
                                            && match2 != match && match2 != turnover) {
                                        try {
                                            synchronized(this) {
                                                wait(2000);
                                            }
                                        } catch(InterruptedException e) {}

                                        if (match3 != null) {
                                            if ((cardList.get(match2.getId() - 1)) == (cardList.get(turnover.getId() - 1))
                                                    && (cardList.get(match3.getId() - 1)) == (cardList.get(turnover.getId() - 1))
                                                    && match2 != turnover && match2 != match && match3 != match2 && match3 != match && match3 != turnover) {
                                                buttonsLeft.remove(turnover);
                                                buttonsLeft.remove(match);
                                                buttonsLeft.remove(match2);
                                                buttonsLeft.remove(match3);
                                                loseCard(turnover, match, match2, match3);
                                                matchesLeft--;
                                                match = null;
                                                match2 = null;
                                                match3 = null;
                                            } else {
                                                isClickable = false;
                                                revealCard(match3);
                                                try {
                                                    synchronized(this) {
                                                        wait(1000);
                                                    }
                                                } catch(InterruptedException e) {}

                                                obscureCard(match, match2, match3);
                                                match = null;
                                                match2 = null;
                                                match3 = null;
                                            }
                                        } else {
                                            obscureCard(match, match2);
                                            match = null;
                                            match2 = null;
                                        }
                                    } else {
                                        isClickable = false;
                                        try {
                                            synchronized(this) {
                                                wait(1000);
                                            }
                                        } catch(InterruptedException e) {}

                                        obscureCard(match, match2);
                                        match = null;
                                        match2 = null;
                                    }
			        			} else {
			        				obscureCard(match);
			        				match = null;
			        			}
			        			isClickable = false;
			        		} else if (triplets) {
			        			revealCard(match);
			        			try {
			        				synchronized(this) {
			        					wait(2000);
			        				}
			        			} catch(InterruptedException e) {}
			        			if (match2 != null) {
			        				if ((cardList.get(match2.getId() - 1)) == (cardList.get(turnover.getId() - 1))
			    			        		&& match2 != turnover && match2 != match) {
			        					buttonsLeft.remove(turnover);
			        					buttonsLeft.remove(match);
			        					buttonsLeft.remove(match2);
			        					loseCard(turnover, match, match2);
			        					matchesLeft--;
			        					match = null;
			        					match2 = null;
			        				} else {
                                        isClickable = false;
			        					revealCard(match2);
			        					try {
			        						synchronized(this) {
			        							wait(1000);
			        						}
			        					} catch(InterruptedException e) {}
			        					obscureCard(match, match2);
			        					match = null;
			        					match2 = null;
			        				}
			        			} else {
			        				obscureCard(match);
			        				match = null;
			        			}
			        			isClickable = false;
			        		} else {			        		
				        		buttonsLeft.remove(turnover);
				        	    buttonsLeft.remove(match);			        		
				        		loseCard(turnover, match);
				        	    matchesLeft--;	
				        	    match = null;
			        		}
		        		} else {
		        			revealCard(match);			        					        			
		        			try {
		        				synchronized(this) {
		        				    wait(1000);
		        				}
		        			} catch(InterruptedException e) {}
		        			obscureCard(match);
			        		match = null;
		        	    }
			        	}	        	
			        	isClickable = false;
			        	obscureCard(turnover);		        	
		        			        			
	        	}
	        	isEnded = true;
	        	chrono.stop();
	        	timeScore = (int) ( SystemClock.elapsedRealtime() - chrono.getBase() )/ 1000;
	        	attemptScore = attempts - (rows * columns)/(quadruplets ? 4 : triplets ? 3 : 2);

                //send message to display all cards
	        	Message msg = new Message();
	        	msg.what = 3;
	        	handler.sendMessage(msg);
	        }
	    };
	    thread.start();
    }

    //make cards disappear on successful math
    private void loseCard(ImageButton... cards) {
    	for (ImageButton card : cards) {
            Message msg = new Message();
            msg.what = 0;
            msg.obj = card;
            handler.sendMessage(msg);
        }
    }

    //turn card face up
    private void revealCard(ImageButton card) {
    	Message msg = new Message();
		msg.what = 1;
		msg.obj = card;
		handler.sendMessage(msg);	
    }

    //turn card face down
    private void obscureCard(ImageButton... cards) {
    	for (ImageButton card : cards) {
            Message msg = new Message();
            msg.what = 2;
            msg.obj = card;
            handler.sendMessage(msg);
        }
    }

    //choose random set of cards from user choice of card set
    private void setCards(int cardSet) {
    	cardList = new ArrayList<Drawable>();
    	Random picker = new Random();
    	List<Drawable> allCards = new ArrayList<Drawable>(15);
    	switch (cardSet) {
    		case 0: //worm cardset
                allCards.add(getResources().getDrawable(R.drawable.blackworm));
                allCards.add(getResources().getDrawable(R.drawable.brownworm));
                allCards.add(getResources().getDrawable(R.drawable.redworm));
                allCards.add(getResources().getDrawable(R.drawable.greenworm));
                allCards.add(getResources().getDrawable(R.drawable.lightblueworm));
                allCards.add(getResources().getDrawable(R.drawable.yellowworm));
                allCards.add(getResources().getDrawable(R.drawable.orangeworm));
                allCards.add(getResources().getDrawable(R.drawable.purpleworm));
                allCards.add(getResources().getDrawable(R.drawable.pinkworm));
                allCards.add(getResources().getDrawable(R.drawable.turqoiseworm));
                allCards.add(getResources().getDrawable(R.drawable.brightpinkworm));
                allCards.add(getResources().getDrawable(R.drawable.lightgreenworm));
                allCards.add(getResources().getDrawable(R.drawable.rainbowworm));
                allCards.add(getResources().getDrawable(R.drawable.goldworm));
                allCards.add(getResources().getDrawable(R.drawable.darkblueworm));
                break;
    		case 1: //solid color cardset
                allCards.add(getResources().getDrawable(R.drawable.ic_black));
                allCards.add(getResources().getDrawable(R.drawable.ic_brown));
                allCards.add(getResources().getDrawable(R.drawable.ic_red));
                allCards.add(getResources().getDrawable(R.drawable.ic_green));
                allCards.add(getResources().getDrawable(R.drawable.ic_bluegrey));
                allCards.add(getResources().getDrawable(R.drawable.ic_yellow));
                allCards.add(getResources().getDrawable(R.drawable.ic_orange));
                allCards.add(getResources().getDrawable(R.drawable.ic_purple));
                allCards.add(getResources().getDrawable(R.drawable.ic_pink));
                allCards.add(getResources().getDrawable(R.drawable.ic_turqoise));
                allCards.add(getResources().getDrawable(R.drawable.ic_darkred));
                allCards.add(getResources().getDrawable(R.drawable.ic_lime));
                allCards.add(getResources().getDrawable(R.drawable.ic_indigo));
                allCards.add(getResources().getDrawable(R.drawable.ic_gold));
                allCards.add(getResources().getDrawable(R.drawable.ic_lavender));
                break;
    	}

        //randomly choose a number of cards from cardset equal to the number of matches needed in the game
    	for (int i = 1; i <= matchesLeft; i++) { 
    		int card = picker.nextInt(16-i);
    		cardList.add(allCards.get(card));
    		allCards.remove(card);
    	}   	

        //multiply list of cards depending on whether finding doublets, triplets, or quadruplets
    	if (quadruplets) {
    		ArrayList<Drawable> cardList2 = new ArrayList<Drawable>(cardList);
    		cardList.addAll(cardList2);
    		cardList.addAll(cardList2);
    		cardList.addAll(cardList2);
    	} else if (triplets) {
    		ArrayList<Drawable> cardList2 = new ArrayList<Drawable>(cardList);
    		cardList.addAll(cardList2);
    		cardList.addAll(cardList2);
    	} else {
    		cardList.addAll(cardList);
    	}
        //shuffle cards so they appear in random order in table
    	Collections.shuffle(cardList);
    }
   
    //set up buttons for game depending on level chosen by user
    private void getButtons() {
    	gameTable.removeAllViews();
    	buttons.clear();
		for (int row = 1; row <= rows; row++) {
			TableRow tableRow = new TableRow(context);
			tableRow.setHorizontalGravity(Gravity.CENTER);
			for (int column = 1; column <= columns; column++) {
				ImageButton button = new ImageButton(context);
				//button.setPadding(5, 5, 5, 5);
				button.setImageResource(R.drawable.ic_grey);
				button.setId((row-1)*columns + column);
				button.setBackgroundDrawable(null);
                button.setPadding(10,10,10,10);
				button.setClickable(true);
				button.setOnClickListener(buttonListener);
				tableRow.addView(button);
				buttons.add(button);
			}
			gameTable.addView(tableRow);
		}
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }
    
    private Session.StatusCallback callback = new Session.StatusCallback() {
    	public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};
	
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		if (pendingPublishReauthorization && state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
            pendingPublishReauthorization = false;
            publishStory();
		}
	}

    //publish facebook story upon new low score
    private void publishStory() {
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            String fbLevel = "";
            if (level == 13) {
                fbLevel = "Epic Mode";
            } else {
                fbLevel = currentLevel;
            }
            Bundle postParams = new Bundle();
            postParams.putString("name", "Worm Free World");
            postParams.putString("caption", "I just achieved a personal best score of " + totalScore + " on " + fbLevel + " in Worm Finder!");
            postParams.putString("description", "Wormfree World is a non-profit dedicated to finding cures and diagnostics for intestinal roundworm parasites.");
            postParams.putString("link", "http://wfwins.org");
            postParams.putString("picture", "http://nmraccelerator.com/wfwnewlogo100x100.png");
            WebDialog feedDialog = (
                    new WebDialog.FeedDialogBuilder(context, session, postParams))
                    .setOnCompleteListener(new WebDialog.OnCompleteListener() {

                        public void onComplete(Bundle values, FacebookException error) {
                            if (error == null) {
                                // When the story is posted, echo the success
                                // and the post Id.
                                final String postId = values.getString("post_id");
                                if (postId != null) {
                                    Toast.makeText(getApplicationContext(), "Posted accomplishment!",Toast.LENGTH_SHORT).show();
                                } else {
                                    // User clicked the Cancel button
                                    Toast.makeText(getApplicationContext(), "Publish cancelled", Toast.LENGTH_SHORT).show();
                                }
                            } else if (error instanceof FacebookOperationCanceledException) {
                                // User clicked the "x" button
                                Toast.makeText(getApplicationContext(), "Publish cancelled", Toast.LENGTH_SHORT).show();
                            } else {
                                // Generic, ex: network error
                                Toast.makeText(getApplicationContext(), "Error posting story", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }).build();
            feedDialog.show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PENDING_PUBLISH_KEY, pendingPublishReauthorization);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
        //pause game if running
        if (isStarted && !isEnded) {
            if (!isPaused) startButton.performClick();
        }
    }

    @Override
    public void onDestroy() {
        uiHelper.onDestroy();
        super.onDestroy();
    }

    //check game status so user doesn't accidentally exit game in progress by pressing physical back button
    @Override
    public void onBackPressed() {
        if (isEnded) {
            finish();
            return;
        } else if (isStarted) {
                if (!isPaused) startButton.performClick();
            } else {
                finish();
                return;
            }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setMessage("Are you sure?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}