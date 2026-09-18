package com.melleeyyy.hasbi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import kotlin.random.Random

object Bus {
    val listeners = ArrayList<() -> Unit>()
    fun post() { listeners.toList().forEach { it() } }
}

class PlayerService : Service() {
    companion object {
        var instance: PlayerService? = null
        var shuffle = false
        var repeat = 0

        fun send(ctx: Context, action: String, uid: Long = -1, list: List<Long>? = null, label: String? = null, ms: Long = -1) {
            val i = Intent(ctx, PlayerService::class.java)
            i.action = action
            if (uid >= 0) i.putExtra("uid", uid)
            if (list != null) i.putIntegerArrayListExtra("queue", ArrayList(list.map { it.toInt() }))
            if (label != null) i.putExtra("label", label)
            if (ms >= 0) i.putExtra("ms", ms)
            ctx.startService(i)
        }

        fun playing(): Boolean = instance?.mp?.isPlaying == true
        fun cur(): PlayerService? = instance
    }

    lateinit var db: Db
    lateinit var prefs: SharedPreferences
    var mp: MediaPlayer? = null
    lateinit var session: MediaSession
    var queue = ArrayList<Long>()
    var queueLabel = "All songs"
    var currentUid: Long = -1
    var prepared = false
    private var wantPlay = false
    private var pendingSeek = -1L
    private lateinit var probe: Handler
    private val main = Handler(Looper.getMainLooper())
    private var started = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = Db(this)
        prefs = getSharedPreferences("hasbi", MODE_PRIVATE)
        shuffle = prefs.getBoolean("shuffle", false)
        repeat = prefs.getInt("repeat", 0)
        val ht = HandlerThread("probe"); ht.start()
        probe = Handler(ht.looper)
        session = MediaSession(this, "Hasbi")
        session.isActive = true
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { play() }
            override fun onPause() { pause() }
            override fun onSkipToNext() { next(false) }
            override fun onSkipToPrevious() { prev() }
            override fun onSeekTo(pos: Long) { mp?.seekTo(pos.toInt()) }
            override fun onStop() { pause() }
        }, Handler(Looper.getMainLooper()))
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("hasbi_playback", "Playback", NotificationManager.IMPORTANCE_LOW))
        restore()
        probeNext()
        main.post { Bus.post() }
    }

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        when (i?.action) {
            "INIT" -> {}
            "PLAY_PAUSE" -> toggle()
            "NEXT" -> next(false)
            "PREV" -> prev()
            "PLAY_UID" -> { val uid = i.getLongExtra("uid", -1); if (uid >= 0) load(uid, 0, true) }
            "SEEK" -> { val ms = i.getLongExtra("ms", -1); if (ms >= 0) mp?.seekTo(ms.toInt()) }
            "SHUFFLE" -> { shuffle = !shuffle; prefs.edit().putBoolean("shuffle", shuffle).apply(); notifyState() }
            "REPEAT" -> { repeat = (repeat + 1) % 3; prefs.edit().putInt("repeat", repeat).apply(); notifyState() }
            "PROBE" -> probeNext()
            "SET_QUEUE" -> {
                @Suppress("UNCHECKED_CAST")
                val q = i.getIntegerArrayListExtra("queue")
                val l = i.getStringExtra("label")
                if (q != null) { queue = ArrayList(q.map { it.toLong() }); queueLabel = l ?: "All songs" }
            }
            "PLAY_AT" -> {
                @Suppress("UNCHECKED_CAST")
                val q = i.getIntegerArrayListExtra("queue")
                val l = i.getStringExtra("label")
                if (q != null) { queue = ArrayList(q.map { it.toLong() }); queueLabel = l ?: "All songs" }
                val uid = i.getLongExtra("uid", -1)
                if (uid >= 0) load(uid, 0, true)
            }
        }
        return START_STICKY
    }

    fun setQueue(list: List<Long>, label: String) { queue = ArrayList(list); queueLabel = label }

    private fun restore() {
        val sort = prefs.getString("sort", "added") ?: "added"
        val ts = db.tracks()
        val sorted = sortTracks(ts, sort)
        queue = ArrayList(sorted.map { it.uid })
        val last = prefs.getLong("last", -1)
        val pos = prefs.getLong("pos", 0)
        if (last >= 0 && sorted.any { it.uid == last }) {
            load(last, pos, false)
        }
    }

    fun load(uid: Long, seekMs: Long, autoplay: Boolean) {
        val t = db.track(uid) ?: return
        currentUid = uid
        wantPlay = autoplay
        pendingSeek = seekMs
        prepared = false
        mp?.reset(); mp?.release()
        mp = MediaPlayer()
        try {
            mp?.setDataSource(t.path)
            mp?.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            mp?.setOnPreparedListener { m ->
                prepared = true
                if (pendingSeek in 0..(m.duration - 500)) m.seekTo(pendingSeek.toInt())
                if (wantPlay) m.start()
                pendingSeek = -1
                updateSession(); notifyState(); main.post { Bus.post() }
            }
            mp?.setOnCompletionListener { next(true) }
            mp?.setOnErrorListener { _, _, _ -> main.post { next(true) }; true }
            mp?.prepareAsync()
        } catch (e: Exception) {
            prepared = false
        }
        prefs.edit().putLong("last", uid).putLong("pos", seekMs).apply()
        updateSession()
        if (!started) { startAsForeground(); started = true } else notifyState()
        main.post { Bus.post() }
    }

    fun play() {
        val m = mp ?: return
        if (prepared) { m.start(); notifyState(); updateSession(); savePos(); main.post { Bus.post() } }
        else wantPlay = true
    }

    fun pause() {
        mp?.takeIf { it.isPlaying }?.pause()
        savePos()
        notifyState(); updateSession()
        main.post { Bus.post() }
    }

    fun toggle() { if (playing()) pause() else play() }

    fun seekTo(ms: Int) { mp?.seekTo(ms) }

    fun position(): Int = mp?.currentPosition ?: 0
    fun duration(): Int = if (prepared) (mp?.duration ?: 0) else (db.track(currentUid)?.dur?.toInt()?.times(1000) ?: 0)
    fun curName(): String = db.track(currentUid)?.name ?: "—"

    fun next(auto: Boolean) {
        if (!queue.contains(currentUid)) rebuildQueue()
        if (queue.isEmpty()) return
        if (auto && repeat == 2) { mp?.seekTo(0); play(); return }
        var idx = queue.indexOf(currentUid)
        if (shuffle && queue.size > 1) {
            var n = idx
            while (n == idx) n = Random.nextInt(queue.size)
            idx = n
        } else {
            idx += 1
            if (idx >= queue.size) {
                if (auto && repeat == 0) { pause(); mp?.seekTo(0); return }
                idx = 0
            }
        }
        load(queue[idx], 0, true)
    }

    fun prev() {
        if (!queue.contains(currentUid)) rebuildQueue()
        if (queue.isEmpty()) return
        if (position() > 3000) { mp?.seekTo(0); return }
        var idx = queue.indexOf(currentUid)
        if (shuffle && queue.size > 1) {
            var n = idx
            while (n == idx) n = Random.nextInt(queue.size)
            idx = n
        } else {
            idx -= 1
            if (idx < 0) idx = queue.size - 1
        }
        load(queue[idx], 0, true)
    }

    private fun rebuildQueue() {
        val sort = prefs.getString("sort", "added") ?: "added"
        queue = ArrayList(sortTracks(db.tracks(), sort).map { it.uid })
    }

    fun savePos() {
        if (currentUid >= 0) prefs.edit().putLong("last", currentUid).putLong("pos", position().toLong()).apply()
    }

    private fun updateSession() {
        val t = db.track(currentUid) ?: return
        session.setMetadata(MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, t.name)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Hasbi Music Player")
            .putString(MediaMetadata.METADATA_KEY_ALBUM, queueLabel)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration().toLong())
            .build())
        session.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP)
            .setState(if (playing()) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                position().toLong(), if (playing()) 1f else 0f)
            .build())
    }

    private fun startAsForeground() {
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(1, n)
    }

    private fun pi(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, PlayerService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val t = db.track(currentUid)
        val nb = Notification.Builder(this, "hasbi_playback")
            .setSmallIcon(R.drawable.ic_stat_note)
            .setContentTitle(t?.name ?: "Hasbi")
            .setContentText(queueLabel)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(@Suppress("DEPRECATION") Notification.Action.Builder(R.drawable.ic_prev, "Previous", pi("PREV")).build())
            .addAction(@Suppress("DEPRECATION") Notification.Action.Builder(if (playing()) R.drawable.ic_pause else R.drawable.ic_play, "Play/Pause", pi("PLAY_PAUSE")).build())
            .addAction(@Suppress("DEPRECATION") Notification.Action.Builder(R.drawable.ic_next, "Next", pi("NEXT")).build())
        nb.setStyle(Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2))
        return nb.build()
    }

    fun notifyState() {
        try { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, buildNotification()) } catch (e: Exception) {}
        updateSession()
    }

    private fun probeNext() {
        probe.post {
            val t = db.nextNoDur() ?: return@post
            var d = -1.0
            var m: MediaPlayer? = null
            try {
                m = MediaPlayer()
                m.setDataSource(t.path)
                m.prepare()
                d = m.duration / 1000.0
            } catch (e: Exception) {
                d = -1.0
            } finally {
                try { m?.release() } catch (e: Exception) {}
            }
            if (d > 0) {
                db.updateDur(t.uid, d)
                main.post { Bus.post() }
            } else {
                db.updateDur(t.uid, -2.0)
            }
            probeNext()
        }
    }

    override fun onDestroy() {
        savePos()
        mp?.release()
        mp = null
        session.release()
        instance = null
        main.post { Bus.post() }
        super.onDestroy()
    }
}

fun sortTracks(ts: List<Track>, sort: String): List<Track> = when (sort) {
    "title-asc" -> ts.sortedBy { it.name.lowercase() }
    "title-desc" -> ts.sortedByDescending { it.name.lowercase() }
    "dur-asc" -> ts.sortedBy { it.dur ?: 1e9 }
    "dur-desc" -> ts.sortedByDescending { it.dur ?: -1.0 }
    else -> ts.sortedByDescending { it.addedAt }
}
