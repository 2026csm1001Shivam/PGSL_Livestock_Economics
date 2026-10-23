package com.gadvasu.livestockapp.ui

import android.content.Intent
import android.os.Bundle
import com.gadvasu.livestockapp.R
import com.gadvasu.livestockapp.data.Ent
import com.gadvasu.livestockapp.databinding.ActivityPoultryBinding

/** Poultry: choose Broiler (batch) or Layer (daily). */
class PoultryActivity : BaseActivity() {

    private lateinit var b: ActivityPoultryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireUser() == null) return
        b = ActivityPoultryBinding.inflate(layoutInflater)
        setContentView(b.root)
        setupTopBar(b.topbar, getString(R.string.poultry))
        b.tileBroiler.setOnClickListener { startActivity(BatchListActivity.intent(this, Ent.BROILER)) }
        b.tileLayer.setOnClickListener { startActivity(Intent(this, LayerActivity::class.java)) }
    }
}
