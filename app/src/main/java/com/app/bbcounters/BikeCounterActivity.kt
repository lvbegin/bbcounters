package com.app.bbcounters

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.concurrent.Executors

class HistoryAdapter(private val history : MutableMap<String, Int>,
                    private val context : Context) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    val years = history.keys.sorted().toTypedArray()

    class HistoryViewHolder(val view : View) : RecyclerView.ViewHolder(view) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_counter, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val year = years.get(position)
        val number = history.get(year);
        val yearTextView = holder.view.findViewById<TextView>(R.id.historyYear);
        val numberTextView = holder.view.findViewById<TextView>(R.id.historyValue)
        yearTextView.text = year
        numberTextView.text = number.toString()
    }

    override fun getItemCount(): Int = history.count()
}

class BikeCounterActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, id : String) {
            val intent = Intent(context, BikeCounterActivity::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bike_counter)

        val id = intent.extras?.getString("id")
        if (id == null)
            return
        val history = findViewById<RecyclerView>(R.id.counterHistory)
        val linearLayoutManager = LinearLayoutManager(this)
        history?.layoutManager = linearLayoutManager
        Executors.newSingleThreadExecutor().execute {
            val historyData = DataServer().getCounterHistory(id)
            runOnUiThread {
                history?.adapter = HistoryAdapter(historyData, this)
            }
        }
    }
}