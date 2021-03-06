package com.jamieadkins.gwent.card.list

import com.jamieadkins.gwent.di.FragmentScoped
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class CardDatabaseModule {

    @FragmentScoped
    @ContributesAndroidInjector()
    internal abstract fun view(): CardDatabaseFragment

    @FragmentScoped
    @Binds
    internal abstract fun view(activity: CardDatabaseFragment): CardDatabaseContract.View

    @FragmentScoped
    @Binds
    internal abstract fun presenter(presenter: CardDatabasePresenter): CardDatabaseContract.Presenter
}