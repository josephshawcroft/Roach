package com.jamieadkins.gwent.domain.deck

import com.jamieadkins.gwent.domain.card.model.GwentCardColour
import com.jamieadkins.gwent.domain.card.repository.CardRepository
import com.jamieadkins.gwent.domain.deck.repository.DeckRepository
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

class RemoveCardFromDeckUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository
) {

    fun removeCard(deckId: String, cardId: String): Completable {
        val countInDeck = deckRepository.getDeckOnce(deckId).map { it.cards[cardId] ?: 0 }
        val cardTier = cardRepository.getCard(cardId).firstOrError().map { it.colour }
        return Single.zip(countInDeck, cardTier, BiFunction { inDeck: Int, tier: GwentCardColour -> Pair(inDeck, tier) })
            .flatMapCompletable {
                val count = it.first
                val newCount = count - 1
                if (count > 0) updateCardCount(deckId, cardId, newCount) else Completable.complete()
            }
    }

    private fun updateCardCount(deckId: String, cardId: String, count: Int): Completable {
        return deckRepository.updateCardCount(deckId, cardId, count)
    }
}