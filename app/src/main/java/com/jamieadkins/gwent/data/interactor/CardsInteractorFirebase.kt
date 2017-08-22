package com.jamieadkins.gwent.data.interactor

import com.google.firebase.database.*
import com.jamieadkins.gwent.BuildConfig
import com.jamieadkins.gwent.card.CardFilter
import com.jamieadkins.gwent.data.CardDetails
import com.jamieadkins.gwent.data.FirebaseUtils
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*
import android.content.Context.CONNECTIVITY_SERVICE
import android.util.Log
import com.jamieadkins.gwent.base.BaseSingleObserver
import com.jamieadkins.gwent.data.SearchResult
import kotlin.Comparator
import kotlin.collections.ArrayList


/**
 * Deals with firebase.
 */

class CardsInteractorFirebase private constructor() : CardsInteractor {

    private val mDatabase = FirebaseUtils.getDatabase()
    private var mCardsReference: DatabaseReference? = null
    private val mMistakesReference: DatabaseReference
    private val mPatchReference: DatabaseReference

    private var mCardsQuery: Query? = null
    private var mCardListener: ValueEventListener? = null
    private var mLocale = "en-US"

    init {
        mPatchReference = mDatabase.getReference(PATCH_PATH)
        mPatchReference.keepSynced(true)
        mMistakesReference = mDatabase.getReference("reported-mistakes")
    }

    private fun onPatchUpdated(patch: String?) {
        patch.let {
            mCardsReference = mDatabase.getReference("card-data/" + patch)
            // Keep Cards data in cache at all times.
            mCardsReference?.keepSynced(true)
        }
    }

    private val latestPatch: Single<String>
        get() = Single.defer {
            Single.create(SingleOnSubscribe<String> { emitter ->
                val patchListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val patch = dataSnapshot.getValue(String::class.java)
                        patch?.let {
                            onPatchUpdated(patch)
                            emitter.onSuccess(patch)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError?) {

                    }
                }

                mPatchReference.addListenerForSingleValueEvent(patchListener)
            })
        }

    override fun setLocale(locale: String) {
        mLocale = locale
    }

    override fun getCards(filter: CardFilter): Single<MutableList<CardDetails>> {
        return getCards(filter, null, null, false)
    }

    override fun getCards(filter: CardFilter?, cardIds: List<String>): Single<MutableList<CardDetails>> {
        return getCards(filter, null, cardIds, false)
    }

    override fun getCards(filter: CardFilter?, query: String?, useIntelligentSearch: Boolean): Single<MutableList<CardDetails>> {
        return getCards(filter, query, null, useIntelligentSearch)
    }

    private fun getCards(filter: CardFilter?, query: String?, cardIds: List<String>?, useIntelligentSearch: Boolean): Single<MutableList<CardDetails>> {
        var source: Single<MutableList<CardDetails>> = getCards()

        if (query != null) {
            if (useIntelligentSearch) {
                source = latestPatch.flatMap { patch ->
                    onPatchUpdated(patch)
                    getSearchResult(query, patch).flatMap { result ->
                        val idList = ArrayList<String>()
                        result.value.hits?.forEach {
                            idList.add(it)
                        }

                        getCards(idList)
                    }
                }
            } else {
                source = getCards(query)
            }
        } else if (cardIds != null) {
            source = getCards(cardIds)
        }

        filter?.let {
            source = source.map { cardList ->
                val iterator = cardList.listIterator()
                while (iterator.hasNext()) {
                    val card = iterator.next()
                    if (!filter.doesCardMeetFilter(card)) iterator.remove()
                }
                cardList
            }
        }

        return source
    }

    private fun getCards(): Single<MutableList<CardDetails>> {
        return getCards(null)
    }

    private fun getCards(cardIds: List<String>): Single<MutableList<CardDetails>> {
        val singles: ArrayList<Single<CardDetails>> = ArrayList()
        cardIds.forEach { singles.add(getCard(it)) }
        return Single.merge(singles).toList()
    }

    private fun getCards(query: String?): Single<MutableList<CardDetails>> {
        return latestPatch.flatMap { patch ->
            onPatchUpdated(patch)
            mCardsQuery = mCardsReference!!.orderByChild("name/" + mLocale)

            query?.let {
                val charValue = query[query.length - 1].toInt()
                var endQuery = query.substring(0, query.length - 1)
                endQuery += (charValue + 1).toChar()

                mCardsQuery = mCardsQuery!!.startAt(query)
                        // No 'contains' query so have to fudge it.
                        .endAt(endQuery)
            }
            cardDataSnapshot.flatMap { dataSnapshot ->
                Single.create(SingleOnSubscribe<MutableList<CardDetails>> { emitter ->
                    val cardList = mutableListOf<CardDetails>()
                    for (cardSnapshot in dataSnapshot.children) {
                        val cardDetails = cardSnapshot.getValue(CardDetails::class.java)

                        if (cardDetails == null) {
                            emitter.onError(Throwable("Card doesn't exist."))
                            continue
                        }
                        cardList.add(cardDetails)
                    }

                    emitter.onSuccess(cardList)
                })
                        // IMPORTANT: Firebase forces us back onto UI thread.
                        .subscribeOn(Schedulers.io())
            }
        }
    }

    fun getSearchResult(query: String, patch: String): Single<RxDatabaseEvent<SearchResult>> {
        val key = search(query, patch)
        return Single.defer {
            Single.create(SingleOnSubscribe<RxDatabaseEvent<SearchResult>> { emitter ->
                val searchQuery = mDatabase.getReference("search/$patch/results/$key")

                val searchListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val result = dataSnapshot.getValue(SearchResult::class.java) ?: return

                        emitter.onSuccess(
                                RxDatabaseEvent(
                                        dataSnapshot.key,
                                        result,
                                        RxDatabaseEvent.EventType.ADDED
                                ))
                    }

                    override fun onCancelled(databaseError: DatabaseError?) {

                    }
                }

                searchQuery.addValueEventListener(searchListener)
            })
        }
    }

    private fun search(query: String, patch: String): String {
        val searchReference = mDatabase.getReference("search/$patch/queries")
        val key = searchReference.push().key
        val firebaseUpdates = HashMap<String, Any>()
        val queryMap = HashMap<String, Any>()
        queryMap.put("query", query)
        firebaseUpdates.put(key, queryMap)

        searchReference.updateChildren(firebaseUpdates)
        return key
    }

    private val cardDataSnapshot: Single<DataSnapshot>
        get() = Single.defer {
            Single.create(SingleOnSubscribe<DataSnapshot> { emitter ->
                mCardListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        emitter.onSuccess(dataSnapshot)
                    }

                    override fun onCancelled(databaseError: DatabaseError?) {

                    }
                }

                mCardsQuery!!.addListenerForSingleValueEvent(mCardListener)
            })
        }

    /**
     * We have a separate method for getting one card as it is significantly quicker than using a
     * card filter.

     * @param id id of the card to retrieve
     */
    override fun getCard(id: String): Single<CardDetails> {
        return latestPatch.flatMap { patch ->
            onPatchUpdated(patch)
            Single.defer {
                Single.create(SingleOnSubscribe<CardDetails> { emitter ->
                    mCardsQuery = mCardsReference!!.child(id)

                    mCardListener = object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val cardDetails = dataSnapshot.getValue(CardDetails::class.java)

                            if (cardDetails == null) {
                                emitter.onError(Throwable("Card doesn't exist."))
                                return
                            }

                            emitter.onSuccess(cardDetails)
                        }

                        override fun onCancelled(databaseError: DatabaseError?) {

                        }
                    }

                    mCardsQuery!!.addListenerForSingleValueEvent(mCardListener)
                })
            }
        }
    }

    override fun reportMistake(cardid: String, description: String): Completable {
        return Completable.defer {
            Completable.create { emitter ->
                val key = mMistakesReference.push().key
                val values = HashMap<String, Any>()
                values.put("cardId", cardid)
                values.put("description", description)
                values.put("fixed", false)

                val firebaseUpdates = HashMap<String, Any>()
                firebaseUpdates.put(key, values)

                mMistakesReference.updateChildren(firebaseUpdates)

                emitter.onComplete()
            }
        }
    }

    override fun removeListeners() {
        if (mCardsQuery != null) {
            mCardsQuery!!.removeEventListener(mCardListener!!)
        }
    }

    companion object {
        private val PATCH_PATH = "patch/" + BuildConfig.CARD_DATA_VERSION
        val instance: CardsInteractorFirebase by lazy { CardsInteractorFirebase() }
    }
}
