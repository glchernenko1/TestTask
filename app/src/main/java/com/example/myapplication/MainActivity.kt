package com.example.myapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.VolumeShaper
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*


private var player: MediaPlayer? = null
private var shaper: VolumeShaper? = null
private var mediaPath1: Uri? = null
private var mediaPath2: Uri? = null
private var play = false
private var сrossFade = 2
var REQUEST_CODE_GET_MP3_1 = 100
var REQUEST_CODE_GET_MP3_2 = 101
private lateinit var thumbView: View


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        thumbView = LayoutInflater.from(this).inflate(R.layout.layout_seekbar_thumb, null, false)
        val sk = findViewById<SeekBar>(R.id.seekBar)
        sk.thumb = getThumb(0)

        sk.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                seekBar.thumb = getThumb(progress)
                сrossFade = progress + 2
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        val playPause = findViewById<ImageButton>(R.id.PlayPause)
        playPause.setOnClickListener {
            if (!play) {
                if (mediaPath1 != null && mediaPath2 != null) {
                    play = true
                    playMusic(mediaPath1)
                    playPause.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
                } else
                    showToast("url невалидный")
            } else {
                play = false
                playPause.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
                stopMusic()
            }
        }


        val button1 = findViewById<ImageButton>(R.id.button1)
        button1.setOnClickListener() {
            openMp3(REQUEST_CODE_GET_MP3_1)
        }

        val button2 = findViewById<ImageButton>(R.id.button2)
        button2.setOnClickListener() {
            openMp3(REQUEST_CODE_GET_MP3_2)
        }

    }

    fun getThumb(progress: Int): Drawable? {
        (thumbView.findViewById(R.id.tvProgress) as TextView).text = (progress + 2).toString() + ""
        thumbView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(
            thumbView.measuredWidth,
            thumbView.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        thumbView.layout(0, 0, thumbView.measuredWidth, thumbView.measuredHeight)
        thumbView.draw(canvas)
        return BitmapDrawable(resources, bitmap)
    }


    fun shaperConfig(second: Int, isStart: Boolean = true): VolumeShaper.Configuration {

        var volumes = floatArrayOf(1f, 0f)

        if (isStart)
            volumes = floatArrayOf(0f, 1f)

        return VolumeShaper.Configuration.Builder()
            .setDuration(1000 * second.toLong())
            .setCurve(floatArrayOf(0f, 1f), volumes)
            .setInterpolatorType(VolumeShaper.Configuration.INTERPOLATOR_TYPE_LINEAR)
            .build()
    }

    fun stopMusic() {
        if (player!!.isPlaying) {
            player!!.stop()
        }
        player?.release()
        player = null
    }

    private fun openMp3(code: Int) {
        val intent = Intent()
        intent.type = "audio/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            intent, code
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_GET_MP3_1 && resultCode == RESULT_OK && data != null)
            mediaPath1 = data.data
        if (requestCode == REQUEST_CODE_GET_MP3_2 && resultCode == RESULT_OK && data != null)
            mediaPath2 = data.data
        else super.onActivityResult(requestCode, resultCode, data)
    }

    fun showToast(massage: String) {
        Toast.makeText(applicationContext, massage, Toast.LENGTH_LONG).show()
    }

    fun playMusic(input: Uri?) {
        if (play && input != null) {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(applicationContext, input)
                prepare()
            }

            val start = shaperConfig(сrossFade)
            val end = shaperConfig(сrossFade, false)

            val length = player!!.duration

            if ((length - сrossFade * 1000.toLong()) / 2 > 0) {
                shaper = player!!.createVolumeShaper(start)
                shaper!!.apply(VolumeShaper.Operation.PLAY)
                player!!.start()

                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (play) shaper!!.replace(end, VolumeShaper.Operation.PLAY, true)
                    }
                }, length - сrossFade * 1000.toLong())


                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        if (play) {
                            stopMusic()
                            if (input == mediaPath1)
                                playMusic(mediaPath2)
                            else
                                playMusic(mediaPath1)
                        }

                    }
                }, length.toLong())
            } else
                showToast("Короткое аудио для данного кроссфейда")


        } else
            if (input == null)
                showToast("url невалидный")
    }


}