package com.gadvasu.livestockapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.data.FarmRepository
import com.gadvasu.livestockapp.databinding.FragmentHomeBinding

/** Home: Poultry, Livestock, Goatery, Fishery tiles. */
class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        b.tilePoultry.setOnClickListener { startActivity(Intent(ctx, PoultryActivity::class.java)) }
        b.tileLivestock.setOnClickListener { startActivity(Intent(ctx, LivestockActivity::class.java)) }
        b.tileGoatery.setOnClickListener { startActivity(BatchListActivity.intent(ctx, Ent.GOATERY)) }
        b.tileFishery.setOnClickListener { startActivity(BatchListActivity.intent(ctx, Ent.FISHERY)) }
    }

    override fun onResume() {
        super.onResume()
        val who = FarmRepository.user()?.profile?.let { it.name.ifBlank { it.value } } ?: ""
        b.tvWelcome.text = if (who.isEmpty()) getString(R.string.welcome) else getString(R.string.welcome_name, who)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
