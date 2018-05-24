package com.jamieadkins.gwent.base

import android.util.Log
import io.reactivex.observers.DisposableObserver

abstract class BaseDisposableObserver<T> : DisposableObserver<T>() {

    override fun onError(e: Throwable) {
        // Do nothing.
    }

    override fun onComplete() {
        Log.e(javaClass.simpleName, "onComplete")
    }
}
