package com.jamieadkins.gwent.card.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.jamieadkins.commonutils.bus.RefreshEvent
import com.jamieadkins.commonutils.bus.RxBus
import com.jamieadkins.commonutils.mvp3.ScrollView
import com.jamieadkins.gwent.R
import com.jamieadkins.gwent.bus.ResetFiltersEvent
import com.jamieadkins.gwent.card.detail.CardDetailsActivity
import com.jamieadkins.gwent.card.detail.CardDetailsFragment
import com.jamieadkins.gwent.domain.card.screen.CardDatabaseScreenModel
import com.jamieadkins.gwent.filter.FilterBottomSheetDialogFragment
import com.jamieadkins.gwent.filter.FilterType
import com.jamieadkins.gwent.update.UpdateActivity
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.appbar_layout.*
import kotterknife.bindView
import javax.inject.Inject

class CardDatabaseFragment :
    DaggerFragment(),
    CardDatabaseContract.View,
    SwipeRefreshLayout.OnRefreshListener,
    ScrollView {

    private val screenKey = javaClass.name

    @Inject
    lateinit var presenter: CardDatabaseContract.Presenter

    private val recyclerView by bindView<RecyclerView>(R.id.recycler_view)
    private val refreshLayout by bindView<SwipeRefreshLayout>(R.id.refreshContainer)
    private lateinit var controller: CardDatabaseController

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = CardDatabaseController(resources)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_card_list, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.search, menu)

        val searchMenuItem = menu?.findItem(R.id.action_search)
        val searchView = searchMenuItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                presenter.search(query)
                return false
            }

            override fun onQueryTextChange(query: String): Boolean {
                return false
            }
        })

        searchView?.setOnCloseListener {
            presenter.clearSearch()
            false
        }

        inflater?.inflate(R.menu.card_filters, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            title = getString(R.string.card_database)
        }

        toolbar.setTitleTextAppearance(requireContext(), R.style.GwentTextAppearance)

        refreshLayout.setColorSchemeResources(R.color.gwentAccent)
        refreshLayout.setOnRefreshListener(this)

        val layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.layoutManager = layoutManager
        val dividerItemDecoration = VerticalSpaceItemDecoration(convertDpToPixel(8f, requireContext()).toInt())
        recyclerView.addItemDecoration(dividerItemDecoration)
        recyclerView.adapter = controller.adapter

        presenter.onAttach()
    }

    override fun onDestroyView() {
        presenter.onDetach()
        super.onDestroyView()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_filter -> {
                val dialog = FilterBottomSheetDialogFragment()
                dialog.show(activity?.supportFragmentManager, dialog.javaClass.simpleName)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun showLoadingIndicator(loading: Boolean) {
        refreshLayout.isRefreshing = loading
    }

    override fun showData(data: CardDatabaseScreenModel) {
        controller.setData(data)
        recyclerView.visibility = View.VISIBLE
        recyclerView.post {
            recyclerView.scrollToPosition(0)
        }
    }

    override fun openUpdateScreen() {
        val intent = Intent(requireContext(), UpdateActivity::class.java)
        activity?.startActivity(intent)
    }

    override fun onRefresh() {
        presenter.onRefresh()
    }

    override fun scrollToTop() {
        recyclerView.smoothScrollToPosition(0)
    }

    override fun showCardDetails(cardId: String) {
        val intent = Intent(requireContext(), CardDetailsActivity::class.java)
        intent.putExtra(CardDetailsFragment.KEY_ID, cardId)
        activity?.startActivity(intent)
    }

    private fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        val px = dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        return px
    }
}
