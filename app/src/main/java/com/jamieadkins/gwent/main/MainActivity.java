package com.jamieadkins.gwent.main;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.jamieadkins.gwent.CardListActivityFragment;
import com.jamieadkins.gwent.R;
import com.jamieadkins.gwent.base.GwentApiActivity;
import com.jamieadkins.gwent.deck.ui.DeckListFragment;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

public class MainActivity extends GwentApiActivity {

    @Override
    public void initialiseContentView() {
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BottomBar bottomBar = (BottomBar) findViewById(R.id.bottomBar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment fragment;
                switch (tabId) {
                    case R.id.tab_decks:
                        fragment = new DeckListFragment();
                        fragmentTransaction.replace(R.id.contentContainer, fragment, "decks");
                        break;
                    case R.id.tab_collection:
                        fragment = new CardListActivityFragment();
                        fragmentTransaction.replace(R.id.contentContainer, fragment, "collection");
                        break;
                    case R.id.tab_news:
                        fragment = new CardListActivityFragment();
                        fragmentTransaction.replace(R.id.contentContainer, fragment, "news");
                        break;
                }

                fragmentTransaction.commit();
            }
        });
    }
}
