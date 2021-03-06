package com.jamieadkins.gwent.update

import android.os.Bundle
import android.widget.Toast
import com.jamieadkins.gwent.R
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class UpdateActivity : DaggerAppCompatActivity(), UpdateContract.View {

    @Inject
    lateinit var presenter: UpdateContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        presenter.onAttach()
    }

    override fun onDestroy() {
        super.onDestroy()

        presenter.onDetach()
    }

    override fun openCardDatabase() {
        finish()
    }

    override fun showError() {
        Toast.makeText(this, R.string.updated_failed, Toast.LENGTH_SHORT).show()
    }
}
