package com.app.bbcounters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class CustomPagerAdapter(private val context : Activity, private val  data : Array<DisplayedCountersData>) : RecyclerView.Adapter<CustomPagerAdapter.ViewHolder>() {
    class ViewHolder(val view : View) : RecyclerView.ViewHolder(view)

    var clickX  : Int = 0
    var clickY  : Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pager_layout, parent, false)
        return CustomPagerAdapter.ViewHolder(view)
    }

    override fun getItemCount() = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageView = holder.view.findViewById<ImageView>(R.id.image_device)
        if (data[position].picture != null)
            imageView.setImageBitmap(data[position].picture)
        else
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
        holder.view.setOnClickListener {
            BikeCounterActivity.startActivity(context, data[position].name)
        }
        holder.view.setOnLongClickListener {
            val x = this@CustomPagerAdapter.clickX
            val y = this@CustomPagerAdapter.clickY
            graphShortcutCallback(context, data, position, x, y, Gravity.LEFT + Gravity.TOP)
        }
        holder.view.setOnTouchListener( object : View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                if (p1?.action == MotionEvent.ACTION_DOWN)
                {
                    clickX = p1?.x?.toInt() ?: 0
                    clickY = p1?.y?.toInt() ?: 0
                }
                return false
            }
        })
    }
}

class PictureActivity : AppCompatActivity() {

    private lateinit var  data : Array<DisplayedCountersData>
    private var index : Int = -1
    companion object {
        private const val dataName = "data"
        private const val indexName = "index"
        private const val xName = "x"
        private const val yName = "y"
        fun startActivity(context: Context, data : Array<DisplayedCountersData>, index : Int, x : Float, y : Float) {
            val intent = Intent(context, PictureActivity::class.java)
            intent.putExtra(dataName, data.toCollection(ArrayList()))
            intent.putExtra(indexName, index)
            intent.putExtra(xName, x)
            intent.putExtra(yName, y)

            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)

        // IntentCompat used because of a bug in Android APO 33
        val dataFromIntent  = IntentCompat.getParcelableArrayListExtra(intent, dataName, DisplayedCountersData::class.java)?.toTypedArray()
        val indexFromIntent = intent.extras?.getInt(indexName)
        val xFromIntent = intent.extras?.getFloat(xName)
        val yFromIntent = intent.extras?.getFloat(yName)
        if (dataFromIntent == null || indexFromIntent == null || xFromIntent == null || yFromIntent == null) {
            finish()
            return
        }
        val anim: Animation = ScaleAnimation(0f, 1f, 0f, 1f, xFromIntent, yFromIntent)
        anim.duration = 250

        window.decorView.findViewById<View>(android.R.id.content).startAnimation(anim)

        data = dataFromIntent
        index = indexFromIntent
        val deviceStore = DeviceStore(this, DataServer())
        val countDownLatch = CountDownLatch(1)
        Executors.newSingleThreadExecutor().execute {
            dataFromIntent.forEach { it.retrieveBitmap(deviceStore) }
            countDownLatch.countDown()
        }
        countDownLatch.await()

        val pager = findViewById<ViewPager2>(R.id.view_pager)
        pager.adapter = CustomPagerAdapter(this, dataFromIntent)
        pager.setCurrentItem(index, false)
    }
}