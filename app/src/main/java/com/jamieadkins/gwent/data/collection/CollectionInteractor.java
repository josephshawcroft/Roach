package com.jamieadkins.gwent.data.collection;

import com.jamieadkins.commonutils.mvp.BaseInteractor;
import com.jamieadkins.gwent.data.interactor.RxDatabaseEvent;

import java.util.Map;

import io.reactivex.Observable;

/**
 * Collection manipulation class.
 */

public interface CollectionInteractor extends BaseInteractor {

    void addCardToCollection(String cardId, String variationId);

    void removeCardFromCollection(String cardId, String variationId);

    Observable<RxDatabaseEvent<Map<String, Long>>> getCollection();

    void stopCollectionUpdates();
}