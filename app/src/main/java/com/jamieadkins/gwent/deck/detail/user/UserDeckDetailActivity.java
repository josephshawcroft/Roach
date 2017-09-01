package com.jamieadkins.gwent.deck.detail.user;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;

import com.jamieadkins.gwent.R;
import com.jamieadkins.gwent.card.list.CardListFragment;
import com.jamieadkins.gwent.deck.detail.DeckDetailActivity;

public class UserDeckDetailActivity extends DeckDetailActivity {
    private static final String STATE_DECK_BUILDER_OPEN = "com.jamieadkins.com.gwent.deck.open";
    private static final String TAG_BOTTOM_FRAGMENT = "com.jamieadkins.com.gwent.deck.builder";

    private boolean mDeckBuilderOpen = false;
    private UserDeckDetailFragment baseFragment;
    private Fragment bottomFragment;
    private FloatingActionButton mAddCardButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAddCardButton = findViewById(R.id.deck_add);
        mAddCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDeckBuilderMenu();
            }
        });
        if (savedInstanceState != null) {
            mDeckBuilderOpen = savedInstanceState.getBoolean(STATE_DECK_BUILDER_OPEN);
            if (mDeckBuilderOpen) {
                mAddCardButton.hide();
                getSupportActionBar().setHomeAsUpIndicator(VectorDrawableCompat.create(getResources(), R.drawable.ic_close, getTheme()));
            }
            bottomFragment = getSupportFragmentManager().findFragmentByTag(TAG_BOTTOM_FRAGMENT);
        } else {
            bottomFragment = new CardListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.contentContainer2, bottomFragment, TAG_BOTTOM_FRAGMENT)
                    .hide(bottomFragment)
                    .commit();
        }

        if (mDeckBuilderOpen) {
            openDeckBuilderMenu();
        } else {
            closeDeckBuilderMenu();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDeckBuilderOpen) {
                    onBackPressed();
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_DECK_BUILDER_OPEN, mDeckBuilderOpen);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeDeckBuilderMenu();
    }

    protected void closeDeckBuilderMenu() {
        mDeckBuilderOpen = false;
        mAddCardButton.show();
        getSupportActionBar().setHomeAsUpIndicator(VectorDrawableCompat.create(getResources(), R.drawable.ic_arrow_back, getTheme()));
    }

    protected void openDeckBuilderMenu() {
        mDeckBuilderOpen = true;
        mAddCardButton.hide();
        getSupportActionBar().setHomeAsUpIndicator(VectorDrawableCompat.create(getResources(), R.drawable.ic_close, getTheme()));

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down, R.anim.slide_in_up, R.anim.slide_out_down)
                .show(bottomFragment)
                .hide(fragment)
                .addToBackStack(null)
                .commit();
    }
}
