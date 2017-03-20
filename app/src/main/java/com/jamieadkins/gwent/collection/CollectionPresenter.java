package com.jamieadkins.gwent.collection;

import android.support.annotation.NonNull;

import com.jamieadkins.gwent.card.CardFilter;
import com.jamieadkins.gwent.data.CardDetails;
import com.jamieadkins.gwent.data.Collection;
import com.jamieadkins.gwent.data.interactor.CardsInteractor;
import com.jamieadkins.gwent.data.interactor.CollectionInteractor;
import com.jamieadkins.gwent.data.interactor.RxDatabaseEvent;

import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * Listens to user actions from the UI, retrieves the data and updates the
 * UI as required.
 */

public class CollectionPresenter implements CollectionContract.Presenter {
    private final CollectionInteractor mCollectionInteractor;
    private final CardsInteractor mCardsInteractor;
    private final CollectionContract.View mCollectionView;

    public CollectionPresenter(@NonNull CollectionContract.View collectionView,
                               @NonNull CollectionInteractor collectionInteractor,
                               @NonNull CardsInteractor cardsInteractor) {
        mCollectionInteractor = collectionInteractor;

        mCardsInteractor = cardsInteractor;

        mCollectionView = collectionView;
        mCollectionView.setPresenter(this);
    }

    @Override
    public void stop() {
        mCollectionInteractor.stopCollectionUpdates();
    }

    @Override
    public Observable<RxDatabaseEvent<CardDetails>> getCards(CardFilter cardFilter) {
        return mCardsInteractor.getCards(cardFilter);
    }

    @Override
    public Single<RxDatabaseEvent<CardDetails>> getCard(String cardId) {
        return mCardsInteractor.getCard(cardId);
    }

    @Override
    public Observable<RxDatabaseEvent<Map<String, Long>>> getCollection() {
        return mCollectionInteractor.getCollection();
    }

    @Override
    public void addCard(String cardId, String variationId) {
        mCollectionInteractor.addCardToCollection(cardId, variationId);
    }

    @Override
    public void removeCard(String cardId, String variationId) {
        mCollectionInteractor.removeCardFromCollection(cardId, variationId);
    }

    @Override
    public void start() {

    }
}
