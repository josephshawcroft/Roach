package com.jamieadkins.gwent.data.card.repository

import com.jamieadkins.gwent.data.CardSearch
import com.jamieadkins.gwent.data.CardSearchData
import com.jamieadkins.gwent.data.card.mapper.GwentCardMapper
import com.jamieadkins.gwent.database.GwentDatabase
import com.jamieadkins.gwent.database.entity.CardWithArtEntity
import com.jamieadkins.gwent.database.entity.CategoryEntity
import com.jamieadkins.gwent.database.entity.KeywordEntity
import com.jamieadkins.gwent.domain.LocaleRepository
import com.jamieadkins.gwent.domain.card.model.GwentCard
import com.jamieadkins.gwent.domain.card.repository.CardRepository
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function5
import javax.inject.Inject

class CardRepositoryImpl @Inject constructor(
    private val database: GwentDatabase,
    private val cardMapper: GwentCardMapper,
    private val localeRepository: LocaleRepository) : CardRepository {

    private fun getCardEntities(): Observable<List<CardWithArtEntity>> {
        return database.cardDao().getCards().toObservable()
    }

    override fun searchCards(searchQuery: String): Observable<List<GwentCard>> {
        return Observable.combineLatest(
            getCardEntities(),
            database.keywordDao().getAllKeywords().toObservable(),
            database.categoryDao().getAllCategories().toObservable(),
            localeRepository.getLocale(),
            localeRepository.getDefaultLocale(),
            Function5 { cards: List<CardWithArtEntity>,
                        keywords: List<KeywordEntity>,
                        categories: List<CategoryEntity>,
                        userLocale: String,
                        defaultLocale: String ->
                val cardSearchData = CardSearchData(cards, keywords, categories)
                CardSearch.searchCards(searchQuery, cardSearchData, userLocale, defaultLocale)
            })
            .switchMap { getCards(it) }
    }

    override fun getCards(): Observable<List<GwentCard>> {
        return Observable.combineLatest(
            getCardEntities(),
            localeRepository.getLocale(),
            BiFunction { cards: List<CardWithArtEntity>, locale: String ->
                cardMapper.mapList(cards, locale)
            })
    }

    override fun getCards(cardIds: List<String>): Observable<List<GwentCard>> {
        return Observable.combineLatest(
            database.cardDao().getCards(cardIds).toObservable(),
            localeRepository.getLocale(),
            BiFunction { cards: List<CardWithArtEntity>, locale: String ->
                cardMapper.mapList(cards, locale)
            })
    }

    override fun getCard(id: String): Observable<GwentCard> {
        return Observable.combineLatest(
            database.cardDao().getCard(id).toObservable(),
            localeRepository.getLocale(),
            BiFunction { card: CardWithArtEntity, locale: String ->
                cardMapper.map(card, locale)
            })
    }
}