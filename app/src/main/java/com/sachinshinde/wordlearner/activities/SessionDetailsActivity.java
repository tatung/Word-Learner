package com.sachinshinde.wordlearner.activities;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sachinshinde.wordlearner.R;
import com.sachinshinde.wordlearner.fragments.WordListFragment;
import com.sachinshinde.wordlearner.module.Session;
import com.sachinshinde.wordlearner.utils.NetworkUtils;
import com.sachinshinde.wordlearner.utils.SessionsUtil;
import com.sachinshinde.wordlearner.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SessionDetailsActivity extends AppCompatActivity implements WordListFragment.OnFragmentInteractionListener, WordListFragment.OnFragmentLongPressedInteractionListener {

    private Session currentSession;
    private AlertDialog dialog;
    private TextToSpeech ttobj;
    WordListFragment todoFragment, masteredFragment;
    private boolean mIsPremium;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        String sessionName = getIntent().getStringExtra(TestWordsActivity.SESSION_NAME);
        currentSession = SessionsUtil.getSession(sessionName);

        ab.setTitle(sessionName);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        if (viewPager != null) {
            setupViewPager(viewPager);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);


        mIsPremium = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext())
                .getString("re", "ezq").equalsIgnoreCase("qze");
        if(mIsPremium) {
            findViewById(R.id.adView).setVisibility(View.GONE);
        }else{

            AdView mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            findViewById(R.id.adView).setVisibility(View.VISIBLE);

        }

        ttobj = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
            }
        });

        ttobj.setLanguage(Locale.US);

        ttobj.setSpeechRate(0.8f);

        findViewById(R.id.fabStartSession).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SessionDetailsActivity.this, TestWordsActivity.class);
                intent.putExtra(TestWordsActivity.SESSION_NAME, currentSession.getSessionName());
                intent.putExtra(TestWordsActivity.SESSION_TYPE, viewPager.getCurrentItem() == 0 ? TestWordsActivity.SESSION_ALL : TestWordsActivity.SESSION_MASTERED);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
                finish();
            }
        });

        findViewById(R.id.fabStartSession).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                Toast.makeText(SessionDetailsActivity.this, "Start Session of " + (viewPager.getCurrentItem() == 0 ? "TODO" : "Mastered") + " Words.", Toast.LENGTH_SHORT).show();

                return true;
            }
        });


    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        currentSession = SessionsUtil.getSession(currentSession.getSessionName());
        try {
            todoFragment.setAdapter(currentSession.getToRevise());
            masteredFragment.setAdapter(currentSession.getMastered());
        }catch (Exception ex){}
    }

    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        todoFragment = WordListFragment.newInstance(currentSession.getToRevise(), 0);
        masteredFragment = WordListFragment.newInstance(currentSession.getMastered(), 1);
        adapter.addFragment(todoFragment, "TODO");
        adapter.addFragment(masteredFragment, "Mastered");
        viewPager.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                startActivity(new Intent(SessionDetailsActivity.this, SessionsListActivity.class));
                return true;
            case R.id.action_add_words:
                Intent intent = new Intent(SessionDetailsActivity.this, AddWordsToSessionActivity.class);
                intent.putExtra(TestWordsActivity.SESSION_NAME, currentSession.getSessionName());
                startActivityForResult(intent, 10921);
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String word) {
        performItemClick(word);
    }

    @Override
    public boolean onFragmentLongPressInteraction(View view, String word, int type) {
        showPopupMenu(view, word, type);
        return true;
    }

    private void showPopupMenu(View view, final String word,final int type) {

        // Create a PopupMenu, giving it the clicked view for an anchor
        PopupMenu popup = new PopupMenu(SessionDetailsActivity.this, view);


        // Inflate our menu resource into the PopupMenu's Menu
        popup.getMenuInflater().inflate(R.menu.word_actions, popup.getMenu());

        MenuItem moveTo = popup.getMenu().findItem(R.id.action_moveto);
        moveTo.setTitle(type == 0 ? "Move to Mastered" : "Move to TODO");

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_delete:
                        if(type == 0){
                            todoFragment.getAdapter().mList.remove(word);
                            currentSession.setToRevise(todoFragment.getAdapter().getList());
                            SessionsUtil.saveSession(currentSession);
                            todoFragment.setAdapter(todoFragment.getAdapter().mList);
                        } else {
                            masteredFragment.getAdapter().mList.remove(word);
                            currentSession.setMastered(masteredFragment.getAdapter().getList());
                            SessionsUtil.saveSession(currentSession);
                            masteredFragment.setAdapter(masteredFragment.getAdapter().mList);
                        }
                        return true;
                    case R.id.action_moveto:
                        if(type == 0){
                            todoFragment.getAdapter().mList.remove(word);
                            masteredFragment.getAdapter().mList.add(word);
                            currentSession.setToRevise(todoFragment.getAdapter().mList);
                            currentSession.setMastered(masteredFragment.getAdapter().mList);
                            SessionsUtil.saveSession(currentSession);
                            todoFragment.setAdapter(todoFragment.getAdapter().mList);
                            masteredFragment.setAdapter(masteredFragment.getAdapter().mList);
                        } else {
                            masteredFragment.getAdapter().mList.remove(word);
                            todoFragment.getAdapter().mList.add(word);
                            currentSession.setMastered(masteredFragment.getAdapter().mList);
                            currentSession.setToRevise(todoFragment.getAdapter().mList);
                            SessionsUtil.saveSession(currentSession);
                            todoFragment.setAdapter(todoFragment.getAdapter().mList);
                            masteredFragment.setAdapter(masteredFragment.getAdapter().mList);
                        }

                        return true;
                }
                return false;
            }
        });

        // Finally show the PopupMenu
        popup.show();
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
        startActivity(new Intent(SessionDetailsActivity.this, SessionsListActivity.class));
        super.onBackPressed();
    }



    public void performItemClick(final String word) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(SessionDetailsActivity.this);

        builder.setView(Utils.getProgressView(SessionDetailsActivity.this, "Loading..."));
        new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... strings) {
                if (Utils.hasWord(strings[0])) {
                    Log.d("WordLearner", "Loading from memory");
                    return Utils.getWordJSON(strings[0]);
                }
                Log.d("WordLearner", "Loading from my dictionary");
                return Utils.saveWord(strings[0], NetworkUtils.GET(Utils.FINAL_URL_SINGLE + strings[0].trim().toLowerCase(Locale.US)));
            }

            @Override
            protected void onPostExecute(String s) {

                builder.setView(null);

                View mView = Utils.getMeaningsView(s, SessionDetailsActivity.this);
                if (mView != null) {

                    View buttonBar = LayoutInflater.from(SessionDetailsActivity.this).inflate(R.layout.button_bar, null);
                    FrameLayout fl = (FrameLayout) buttonBar.findViewById(R.id.flButtonBarContainer);
                    fl.addView(mView);

                    builder.setView(buttonBar);

                    final AlertDialog mNewDialog = builder.create();

                    buttonBar.findViewById(R.id.bEdit).setVisibility(View.GONE);

                    buttonBar.findViewById(R.id.bSpeak).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                                ttobj.speak(word, TextToSpeech.QUEUE_FLUSH, null, String.valueOf(System.currentTimeMillis()));
                            } else {
                                ttobj.speak(word, TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }
                    });


                    ((ImageButton) buttonBar.findViewById(R.id.bCancelorDelete)).setImageResource(R.drawable.ic_delete);
                    buttonBar.findViewById(R.id.bCancelorDelete).setVisibility(View.GONE);


                    buttonBar.findViewById(R.id.bDone).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mNewDialog.cancel();
                        }
                    });

                    mNewDialog.show();

                } else {
                    Toast.makeText(SessionDetailsActivity.this, "Oops! An error occurred. Try Again.", Toast.LENGTH_LONG).show();
                    dialog.cancel();
                    Utils.deleteWord(word);
                    return;
                }

                dialog.cancel();
                super.onPostExecute(s);
            }
        }.execute(word);
        dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_session_details, menu);


        return true;
    }

}
