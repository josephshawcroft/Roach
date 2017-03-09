package com.jamieadkins.gwent.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.jamieadkins.commonutils.ui.RecyclerViewItem;
import com.jamieadkins.gwent.R;
import com.jamieadkins.gwent.card.CardFilter;
import com.jamieadkins.gwent.card.CardFilterListener;
import com.jamieadkins.gwent.data.Faction;
import com.jamieadkins.gwent.data.Filterable;
import com.jamieadkins.gwent.data.Rarity;
import com.jamieadkins.gwent.data.Type;
import com.jamieadkins.gwent.data.interactor.RxDatabaseEvent;
import com.jamieadkins.gwent.filter.FilterBottomSheetDialogFragment;
import com.jamieadkins.gwent.filter.FilterableItem;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;

/**
 * UI fragment that shows a list of the users decks.
 */
public abstract class BaseFragment extends Fragment
        implements SwipeRefreshLayout.OnRefreshListener, CardFilterListener,
        FilterBottomSheetDialogFragment.FilterUiListener {
    private static final String STATE_CARD_FILTER = "com.jamieadkins.gwent.card.filter";
    public static final String TAG_FILTER_MENU = "com.jamieadkins.gwent.filter.menu";

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mRefreshContainer;
    private GwentRecyclerViewAdapter mAdapter;
    private boolean mLoading = false;

    private FilterBottomSheetDialogFragment mFilterMenu;
    private CardFilter mCardFilter;

    private Observer<RxDatabaseEvent<? extends RecyclerViewItem>> mObserver
            = new BaseObserver<RxDatabaseEvent<? extends RecyclerViewItem>>() {
        @Override
        public void onNext(RxDatabaseEvent<? extends RecyclerViewItem> value) {
            if (getActivity() == null) {
                return;
            }
            onDataEvent(value);
        }

        @Override
        public void onComplete() {
            setLoading(false);
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mCardFilter = (CardFilter) savedInstanceState.get(STATE_CARD_FILTER);
        } else {
            mCardFilter = initialiseCardFilter();
        }
    }

    public CardFilter initialiseCardFilter() {
        CardFilter filter = new CardFilter();
        filter.setCurrentFilterAsBase();
        return filter;
    }

    public CardFilter getCardFilter() {
        return mCardFilter;
    }

    public void setupViews(View rootView) {
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        setupRecyclerView(mRecyclerView);

        mRefreshContainer = (SwipeRefreshLayout) rootView.findViewById(R.id.refreshContainer);
        mRefreshContainer.setColorSchemeResources(R.color.gwentAccent);
        mRefreshContainer.setOnRefreshListener(this);
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        final LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(recyclerView.getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = onBuildRecyclerView();
        recyclerView.setAdapter(mAdapter);
    }

    public void onDataEvent(RxDatabaseEvent<? extends RecyclerViewItem> data) {
        switch (data.getEventType()) {
            case ADDED:
                mAdapter.addItem(data.getValue());
                break;
            case REMOVED:
                mAdapter.removeItem(data.getValue());
                break;
            case CHANGED:
                mAdapter.updateItem(data.getValue());
                break;
            case COMPLETE:
                setLoading(false);
                break;
        }
    }

    public void onLoadData() {
        setLoading(true);
    }

    public void setLoading(boolean loading) {
        mLoading = loading;
        mRefreshContainer.setRefreshing(loading);
        enableRefreshing(loading);
    }

    public void enableRefreshing(boolean enable) {
        mRefreshContainer.setEnabled(enable);
    }

    public boolean isLoading() {
        return mLoading;
    }

    public GwentRecyclerViewAdapter onBuildRecyclerView() {
        return new GwentRecyclerViewAdapter.Builder()
                .build();
    }

    public GwentRecyclerViewAdapter getRecyclerViewAdapter() {
        return mAdapter;
    }

    public Observer<RxDatabaseEvent<? extends RecyclerViewItem>> getObserver() {
        return mObserver;
    }

    @Override
    public void onRefresh() {
        getRecyclerViewAdapter().clear();
        onLoadData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mFilterMenu != null) {
            mFilterMenu.dismiss();
        }
        outState.putParcelable(STATE_CARD_FILTER, mCardFilter);
        super.onSaveInstanceState(outState);
    }

    public void setupFilterMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (query.equals("")) {
                    // Don't search for everything!
                    mCardFilter.setSearchQuery(null);
                    return false;
                }

                mCardFilter.setSearchQuery(query);
                onCardFilterUpdated();
                return false;
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mCardFilter.setSearchQuery(null);
                onCardFilterUpdated();
                return false;
            }
        });

        if (mCardFilter.getSearchQuery() != null) {
            searchView.setQuery(mCardFilter.getSearchQuery(), false);
        }

        inflater.inflate(R.menu.card_filters, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        List<FilterableItem> filterableItems = new ArrayList<>();
        String filteringOn;
        Filterable[] filterItems;
        switch (item.getItemId()) {
            case R.id.filter_reset:
                mCardFilter.clearFilters();
                onCardFilterUpdated();
                return true;
            case R.id.filter_faction:
                filteringOn = getString(R.string.faction);
                filterItems = Faction.ALL_FACTIONS;
                break;
            case R.id.filter_rarity:
                filteringOn = getString(R.string.rarity);
                filterItems = Rarity.ALL_RARITIES;
                break;
            case R.id.filter_type:
                filteringOn = getString(R.string.type);
                filterItems = Type.ALL_TYPES;
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        for (Filterable filterable : filterItems) {
            filterableItems.add(new FilterableItem(
                    filterable.getId(),
                    getString(filterable.getName()),
                    getCardFilter().get(filterable.getId())));
        }

        showFilterMenu(filteringOn, filterableItems);
        return true;
    }

    public void showFilterMenu(String filteringOn, List<FilterableItem> items) {
        mFilterMenu = FilterBottomSheetDialogFragment
                .newInstance(filteringOn, items, this);
        mFilterMenu.show(getChildFragmentManager(), TAG_FILTER_MENU);
    }

    @Override
    public void onFilterChanged(String key, boolean checked) {
        mCardFilter.put(key, checked);
        onCardFilterUpdated();
    }

    @Override
    public void onCardFilterUpdated() {
        onLoadData();
    }
}
