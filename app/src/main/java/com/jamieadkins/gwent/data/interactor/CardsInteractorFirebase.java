package com.jamieadkins.gwent.data.interactor;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.jamieadkins.gwent.base.BaseSingleObserver;
import com.jamieadkins.gwent.card.CardFilter;
import com.jamieadkins.gwent.data.CardDetails;
import com.jamieadkins.gwent.data.FirebaseUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.CompletableSource;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.SingleSource;
import io.reactivex.schedulers.Schedulers;

/**
 * Deals with firebase.
 */

public class CardsInteractorFirebase implements CardsInteractor {
    private static final String PATCH_PATH = "card-data/latest-patch";

    private final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mCardsReference;
    private final DatabaseReference mMistakesReference;
    private final DatabaseReference mPatchReference;

    private Query mCardsQuery;
    private ValueEventListener mCardListener;

    private static CardsInteractorFirebase mInstance;

    public static CardsInteractorFirebase getInstance() {
        if (mInstance == null) {
            mInstance = new CardsInteractorFirebase();
        }

        return mInstance;
    }

    private CardsInteractorFirebase() {
        mPatchReference = mDatabase.getReference(PATCH_PATH);
        mMistakesReference = mDatabase.getReference("reported-mistakes");
    }

    private void onPatchUpdated(String patch) {
        mCardsReference = mDatabase.getReference("card-data/" + patch);
        // Keep Cards data in cache at all times.
        mCardsReference.keepSynced(true);
    }

    private Single<String> getLatestPatch() {
        return Single.defer(new Callable<SingleSource<? extends String>>() {
            @Override
            public SingleSource<? extends String> call() throws Exception {
                return Single.create(new SingleOnSubscribe<String>() {
                    @Override
                    public void subscribe(final SingleEmitter<String> emitter) throws Exception {
                        ValueEventListener patchListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String patch = dataSnapshot.getValue(String.class);
                                onPatchUpdated(patch);
                                emitter.onSuccess(patch);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        };

                        mPatchReference.addListenerForSingleValueEvent(patchListener);
                    }
                });
            }
        });
    }

    @Override
    public Observable<RxDatabaseEvent<CardDetails>> getCards(final CardFilter filter) {
        return Observable.defer(new Callable<ObservableSource<? extends RxDatabaseEvent<CardDetails>>>() {
            @Override
            public ObservableSource<? extends RxDatabaseEvent<CardDetails>> call() throws Exception {
                return Observable.create(new ObservableOnSubscribe<RxDatabaseEvent<CardDetails>>() {
                    @Override
                    public void subscribe(final ObservableEmitter<RxDatabaseEvent<CardDetails>> emitter) throws Exception {
                        getLatestPatch()
                                .observeOn(Schedulers.io())
                                .subscribe(new BaseSingleObserver<String>() {
                                    @Override
                                    public void onSuccess(String value) {
                                        mCardsQuery = mCardsReference.orderByChild("name");

                                        if (filter.getSearchQuery() != null) {
                                            String query = filter.getSearchQuery();
                                            int charValue = query.charAt(query.length() - 1);
                                            String endQuery = query.substring(0, query.length() - 1);
                                            endQuery += (char) (charValue + 1);

                                            mCardsQuery = mCardsQuery.
                                                    startAt(query)
                                                    // No 'contains' query so have to fudge it.
                                                    .endAt(endQuery);
                                        }

                                        mCardListener = new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                emitCards(dataSnapshot, emitter, filter)
                                                        .subscribeOn(Schedulers.io())
                                                        .subscribe();
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        };

                                        mCardsQuery.addListenerForSingleValueEvent(mCardListener);
                                    }
                                });
                    }
                });
            }
        });
    }

    private Completable emitCards(final DataSnapshot cardsSnapshot,
                                  final ObservableEmitter<RxDatabaseEvent<CardDetails>> emitter,
                                  final CardFilter filter) {
        return Completable.defer(new Callable<CompletableSource>() {
            @Override
            public CompletableSource call() throws Exception {
                return Completable.create(new CompletableOnSubscribe() {
                    @Override
                    public void subscribe(CompletableEmitter e) throws Exception {
                        for (DataSnapshot cardSnapshot : cardsSnapshot.getChildren()) {
                            CardDetails cardDetails = cardSnapshot.getValue(CardDetails.class);

                            if (cardDetails == null) {
                                emitter.onError(new Throwable("Card doesn't exist."));
                                emitter.onComplete();
                                return;
                            }

                            // Only add card if the card meets all the filters.
                            // Also check name and info are not null. Those are dud cards.
                            if (filter.doesCardMeetFilter(cardDetails)) {
                                emitter.onNext(
                                        new RxDatabaseEvent<CardDetails>(
                                                cardSnapshot.getKey(),
                                                cardDetails,
                                                RxDatabaseEvent.EventType.ADDED
                                        ));
                            }
                        }

                        emitter.onComplete();
                        e.onComplete();
                    }
                });
            }
        });
    }

    /**
     * We have a separate method for getting one card as it is significantly quicker than using a
     * card filter.
     *
     * @param id id of the card to retrieve
     */
    @Override
    public Single<RxDatabaseEvent<CardDetails>> getCard(final String id) {
        return Single.defer(new Callable<SingleSource<? extends RxDatabaseEvent<CardDetails>>>() {
            @Override
            public SingleSource<? extends RxDatabaseEvent<CardDetails>> call() throws Exception {
                return Single.create(new SingleOnSubscribe<RxDatabaseEvent<CardDetails>>() {
                    @Override
                    public void subscribe(final SingleEmitter<RxDatabaseEvent<CardDetails>> emitter) throws Exception {
                        getLatestPatch()
                                .observeOn(Schedulers.io())
                                .subscribe(new BaseSingleObserver<String>() {
                                    @Override
                                    public void onSuccess(String value) {
                                        mCardsQuery = mCardsReference.child(id);

                                        mCardListener = new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                CardDetails cardDetails = dataSnapshot.getValue(CardDetails.class);

                                                if (cardDetails == null) {
                                                    emitter.onError(new Throwable("Card doesn't exist."));
                                                    return;
                                                }

                                                emitter.onSuccess(
                                                        new RxDatabaseEvent<CardDetails>(
                                                                dataSnapshot.getKey(),
                                                                cardDetails,
                                                                RxDatabaseEvent.EventType.ADDED
                                                        ));
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        };

                                        mCardsQuery.addListenerForSingleValueEvent(mCardListener);
                                    }
                                });
                    }
                });
            }
        });
    }

    @Override
    public Completable reportMistake(final String cardid, final String description) {
        return Completable.defer(new Callable<CompletableSource>() {
            @Override
            public CompletableSource call() throws Exception {
                return Completable.create(new CompletableOnSubscribe() {
                    @Override
                    public void subscribe(CompletableEmitter emitter) throws Exception {
                        String key = mMistakesReference.push().getKey();
                        Map<String, Object> values = new HashMap<>();
                        values.put("cardId", cardid);
                        values.put("description", description);
                        values.put("fixed", false);

                        Map<String, Object> firebaseUpdates = new HashMap<>();
                        firebaseUpdates.put(key, values);

                        mMistakesReference.updateChildren(firebaseUpdates);

                        emitter.onComplete();
                    }
                });
            }
        });
    }

    @Override
    public void removeListeners() {
        if (mCardsQuery != null) {
            mCardsQuery.removeEventListener(mCardListener);
        }
    }
}
