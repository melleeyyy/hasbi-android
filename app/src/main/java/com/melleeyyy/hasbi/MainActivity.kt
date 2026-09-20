package com.melleeyyy.hasbi

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max

class MainActivity : Activity() {
    lateinit var db: Db
    lateinit var prefs: SharedPreferences
    var view = "songs"
    var plCtx: Long? = null
    var sortMode = "added"
    var searchQ = ""
    var npOpen = false
    var sheetOpen = false
    val h = Handler(Looper.getMainLooper())

    lateinit var root: FrameLayout
    lateinit var backBtn: ImageView
    lateinit var sortBtn: ImageView
    lateinit var tabThumb: View
    lateinit var tabSongs: TextView
    lateinit var tabPlaylists: TextView
    lateinit var search: EditText
    lateinit var clearBtn: ImageView
    lateinit var content: FrameLayout
    lateinit var songsScroll: ScrollView
    lateinit var songsList: LinearLayout
    lateinit var songsEmpty: LinearLayout
    lateinit var plScroll: ScrollView
    lateinit var plList: LinearLayout
    lateinit var plEmpty: LinearLayout
    lateinit var detScroll: ScrollView
    lateinit var detHero: LinearLayout
    lateinit var detList: LinearLayout
    lateinit var tabsWrap: FrameLayout
    lateinit var searchWrap: LinearLayout
    lateinit var fab: FrameLayout
    lateinit var mini: FrameLayout
    lateinit var miniBar: View
    lateinit var miniName: TextView
    lateinit var miniSub: TextView
    lateinit var miniLike: ImageView
    lateinit var miniPrev: ImageView
    lateinit var miniPlay: ImageView
    lateinit var miniNext: ImageView
    lateinit var np: FrameLayout
    lateinit var npLabel: TextView
    lateinit var npLike: ImageView
    lateinit var npQueue: ImageView
    lateinit var npPl: ImageView
    lateinit var disc: DiscView
    lateinit var skipBadge: TextView
    lateinit var npTitle: TextView
    lateinit var npSub: TextView
    lateinit var seek: SeekBar
    lateinit var curTime: TextView
    lateinit var totTime: TextView
    lateinit var shuffleBtn: ImageView
    lateinit var prevBtn: ImageView
    lateinit var playBtn: ImageView
    lateinit var nextBtn: ImageView
    lateinit var repeatBtn: ImageView
    lateinit var dim: View
    lateinit var panel: LinearLayout
    lateinit var sheetBody: LinearLayout

    val SORTS = listOf("added" to "Recently added", "title-asc" to "Title A-Z",
        "title-desc" to "Title Z-A", "dur-asc" to "Shortest first", "dur-desc" to "Longest first")

    private val busListener: () -> Unit = { runOnUiThread { updateControls(); renderAll() } }

    private val tick = object : Runnable {
        override fun run() {
            updateTick()
            h.postDelayed(this, 500)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        db = Db(this)
        prefs = getSharedPreferences("hasbi", MODE_PRIVATE)
        sortMode = prefs.getString("sort", "added") ?: "added"
        view = if (b?.getBoolean("pl", false) == true) "playlists" else "songs"
        buildUi()
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 5)
        Bus.listeners.add(busListener)
        PlayerService.send(this, "INIT")
        h.postDelayed(tick, 500)
    }

    override fun onDestroy() {
        Bus.listeners.remove(busListener)
        h.removeCallbacks(tick)
        super.onDestroy()
    }

    fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun tv(text: String, sp: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text; t.textSize = sp; t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun icon(res: Int, sizeDp: Int, tint: Int, pad: Int = 11): ImageView {
        val v = ImageView(this)
        v.setImageResource(res)
        v.setColorFilter(tint)
        val p = dp(pad)
        v.setPadding(p, p, p, p)
        v.layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply { gravity = Gravity.CENTER }
        return v
    }

    private fun circleClick(child: View): View {
        child.background = ripple(GradientDrawable().apply { setColor(0x00000000); shape = GradientDrawable.OVAL })
        return child
    }

    private fun buildUi() {
        root = FrameLayout(this)
        val mainCol = LinearLayout(this)
        mainCol.orientation = LinearLayout.VERTICAL
        root.addView(mainCol, FrameLayout.LayoutParams(-1, -1))

        // header
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(8), dp(10), dp(8), dp(6))
        mainCol.addView(header, LinearLayout.LayoutParams(-1, -2))
        backBtn = icon(R.drawable.ic_back, 44, C.TEXT)
        backBtn.visibility = View.INVISIBLE
        backBtn.setOnClickListener { plCtx = null; renderAll() }
        header.addView(backBtn, LinearLayout.LayoutParams(dp(44), dp(44)))
        val brand = LinearLayout(this)
        brand.orientation = LinearLayout.VERTICAL
        val brandRow = LinearLayout(this)
        brandRow.orientation = LinearLayout.HORIZONTAL
        brandRow.gravity = Gravity.CENTER_VERTICAL
        val bName = tv("Hasbi", 24f, C.TEXT, true)
        val bLine = View(this)
        bLine.background = GradientDrawable().apply { setColor(C.ACCENT2); cornerRadius = dp(2).toFloat() }
        brandRow.addView(bName, LinearLayout.LayoutParams(-2, -2))
        brandRow.addView(bLine, LinearLayout.LayoutParams(dp(42), dp(2)).apply { leftMargin = dp(8); topMargin = dp(8) })
        val bSub = tv("MUSIC PLAYER", 8.5f, C.ACCENT, true)
        bSub.letterSpacing = 0.55f
        brand.addView(brandRow, LinearLayout.LayoutParams(-2, -2))
        brand.addView(bSub, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(8) })
        sortBtn = icon(R.drawable.ic_sort, 44, C.TEXT)
        sortBtn.setOnClickListener { showSortMenu() }
        header.addView(sortBtn, LinearLayout.LayoutParams(dp(44), dp(44)))

        // tabs
        tabsWrap = FrameLayout(this)
        tabsWrap.background = surfaceBg(dp(20).toFloat())
        val tl = LinearLayout.LayoutParams(-1, dp(42))
        tl.setMargins(dp(16), dp(12), dp(16), 0)
        mainCol.addView(tabsWrap, tl)
        tabThumb = View(this)
        tabThumb.background = GradientDrawable().apply {
            setColor(0x420229FF.toInt())
            cornerRadius = dp(17).toFloat()
        }
        tabsWrap.addView(tabThumb, FrameLayout.LayoutParams(0, dp(34)).apply { marginStart = dp(4); topMargin = dp(4) })
        tabThumb.post { tabThumb.layoutParams.width = (tabsWrap.width - dp(8)) / 2; tabThumb.requestLayout() }
        val tabsRow = LinearLayout(this)
        tabsWrap.addView(tabsRow, FrameLayout.LayoutParams(-1, -1))
        tabSongs = tv("Songs", 14f, C.TEXT, true)
        tabSongs.gravity = Gravity.CENTER
        tabPlaylists = tv("Playlists", 14f, C.MUTED, true)
        tabPlaylists.gravity = Gravity.CENTER
        tabsRow.addView(tabSongs, LinearLayout.LayoutParams(0, -1, 1f))
        tabsRow.addView(tabPlaylists, LinearLayout.LayoutParams(0, -1, 1f))
        tabSongs.setOnClickListener { switchTab("songs") }
        tabPlaylists.setOnClickListener { switchTab("playlists") }

        // search
        searchWrap = LinearLayout(this)
        searchWrap.orientation = LinearLayout.HORIZONTAL
        searchWrap.gravity = Gravity.CENTER_VERTICAL
        searchWrap.background = surfaceBg(dp(14).toFloat())
        searchWrap.setPadding(dp(16), 0, dp(4), 0)
        val sl = LinearLayout.LayoutParams(-1, -2)
        sl.setMargins(dp(16), dp(10), dp(16), dp(6))
        mainCol.addView(searchWrap, sl)
        val si = icon(R.drawable.ic_search, 34, C.MUTED, 6)
        searchWrap.addView(si, LinearLayout.LayoutParams(dp(34), dp(34)))
        search = EditText(this)
        search.hint = "Search songs or playlists..."
        search.setTextColor(C.TEXT)
        search.setHintTextColor(C.MUTED)
        search.background = null
        search.textSize = 15f
        search.imeOptions = EditorInfo.IME_ACTION_DONE
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchQ = p0?.toString()?.trim()?.lowercase() ?: ""
                clearBtn.visibility = if (searchQ.isEmpty()) View.INVISIBLE else View.VISIBLE
                renderAll()
            }
            override fun afterTextChanged(p0: Editable?) {}
        })
        searchWrap.addView(search, LinearLayout.LayoutParams(0, dp(42), 1f))
        clearBtn = icon(R.drawable.ic_close, 32, C.MUTED, 6)
        clearBtn.visibility = View.INVISIBLE
        clearBtn.setOnClickListener { search.setText("") }
        searchWrap.addView(clearBtn, LinearLayout.LayoutParams(dp(32), dp(32)))

        // content
        content = FrameLayout(this)
        mainCol.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        songsScroll = ScrollView(this)
        songsList = LinearLayout(this)
        songsList.orientation = LinearLayout.VERTICAL
        val sp = dp(6)
        songsList.setPadding(sp, sp, sp, dp(180))
        songsScroll.addView(songsList, ViewGroup.LayoutParams(-1, -2))
        content.addView(songsScroll, FrameLayout.LayoutParams(-1, -1))
        songsEmpty = emptyState(R.drawable.ic_music, "Your library is empty",
            "Add audio files from your device to start listening", "Add Songs") { pickFiles() }

        plScroll = ScrollView(this)
        plScroll.visibility = View.GONE
        plList = LinearLayout(this)
        plList.orientation = LinearLayout.VERTICAL
        plList.setPadding(sp, sp, sp, dp(180))
        plScroll.addView(plList, ViewGroup.LayoutParams(-1, -2))
        content.addView(plScroll, FrameLayout.LayoutParams(-1, -1))
        plEmpty = emptyState(R.drawable.ic_playlist, "No playlists yet",
            "Group your favourite songs into playlists", "New Playlist") { newPlaylistSheet(null) }

        detScroll = ScrollView(this)
        detScroll.visibility = View.GONE
        val detCol = LinearLayout(this)
        detCol.orientation = LinearLayout.VERTICAL
        detHero = LinearLayout(this)
        detHero.orientation = LinearLayout.HORIZONTAL
        val hl = LinearLayout.LayoutParams(-1, -2)
        hl.setMargins(sp, dp(10), sp, dp(8))
        detCol.addView(detHero, hl)
        detList = LinearLayout(this)
        detList.orientation = LinearLayout.VERTICAL
        detCol.addView(detList, LinearLayout.LayoutParams(-1, -2))
        detScroll.addView(detCol, ViewGroup.LayoutParams(-1, -2))
        content.addView(detScroll, FrameLayout.LayoutParams(-1, -1))

        // FAB
        fab = FrameLayout(this)
        fab.background = ripple(gradBg2(dp(28).toFloat()))
        val fi = icon(R.drawable.ic_add, 26, C.TEXT, 10)
        fab.addView(fi, FrameLayout.LayoutParams(-1, -1))
        fab.setOnClickListener { pickFiles() }
        root.addView(fab, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply {
            rightMargin = dp(18); bottomMargin = dp(104)
        })

        // mini player
        mini = FrameLayout(this)
        mini.visibility = View.GONE
        mini.background = GradientDrawable().apply { setColor(0xE6111116.toInt()); cornerRadius = dp(18).toFloat() }
        mini.clipToOutline = true
        mini.setPadding(dp(10), dp(8), dp(4), dp(8))
        root.addView(mini, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            leftMargin = dp(10); rightMargin = dp(10); bottomMargin = dp(10)
        })
        miniBar = View(mini.context)
        miniBar.background = GradientDrawable().apply { setColor(C.ACCENT2) }
        mini.addView(miniBar, FrameLayout.LayoutParams(0, dp(3), Gravity.TOP or Gravity.START))
        val miniRow = LinearLayout(this)
        miniRow.orientation = LinearLayout.HORIZONTAL
        miniRow.gravity = Gravity.CENTER_VERTICAL
        mini.addView(miniRow, FrameLayout.LayoutParams(-1, -2))
        val art = FrameLayout(this)
        art.background = gradBg(dp(12).toFloat())
        val ai = icon(R.drawable.ic_music, 20, C.TEXT, 8)
        art.addView(ai, FrameLayout.LayoutParams(-1, -1))
        miniRow.addView(art, LinearLayout.LayoutParams(dp(44), dp(44)))
        val meta = LinearLayout(this)
        meta.orientation = LinearLayout.VERTICAL
        miniName = tv("—", 13.5f, C.TEXT, true)
        miniName.maxLines = 1
        miniName.ellipsize = TextUtils.TruncateAt.END
        miniSub = tv("Hasbi", 11f, C.MUTED)
        miniSub.maxLines = 1
        meta.addView(miniName, LinearLayout.LayoutParams(-1, -2))
        meta.addView(miniSub, LinearLayout.LayoutParams(-1, -2))
        miniRow.addView(meta, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })
        miniLike = icon(R.drawable.ic_heart_o, 36, C.MUTED, 8)
        miniPrev = icon(R.drawable.ic_prev, 36, C.TEXT, 8)
        miniPlay = icon(R.drawable.ic_play, 38, C.TEXT, 9)
        miniNext = icon(R.drawable.ic_next, 36, C.TEXT, 8)
        listOf(miniLike, miniPrev, miniPlay, miniNext).forEach {
            miniRow.addView(circleClick(it), LinearLayout.LayoutParams(dp(40), dp(40)))
        }
        art.setOnClickListener { openNP() }
        meta.setOnClickListener { openNP() }
        miniLike.setOnClickListener { likeCurrent() }
        miniPrev.setOnClickListener { PlayerService.send(this, "PREV") }
        miniPlay.setOnClickListener { PlayerService.send(this, "PLAY_PAUSE") }
        miniNext.setOnClickListener { PlayerService.send(this, "NEXT") }

        // now playing
        np = FrameLayout(this)
        np.setBackgroundColor(C.BG2)
        np.visibility = View.GONE
        root.addView(np, FrameLayout.LayoutParams(-1, -1))
        buildNp()

        // sheet
        dim = View(this)
        dim.setBackgroundColor(0x99020409.toInt())
        dim.visibility = View.GONE
        dim.setOnClickListener { dismissSheet() }
        root.addView(dim, FrameLayout.LayoutParams(-1, -1))
        panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setBackgroundColor(C.PANEL)
        panel.setPadding(dp(14), dp(8), dp(14), dp(24))
        panel.visibility = View.GONE
        val pbg = GradientDrawable()
        pbg.setColor(C.PANEL)
        pbg.cornerRadii = floatArrayOf(dp(26).toFloat(), dp(26).toFloat(), dp(26).toFloat(), dp(26).toFloat(), 0f, 0f, 0f, 0f)
        panel.background = pbg
        val handle = View(this)
        handle.background = GradientDrawable().apply { setColor(0x38FFFFFF); cornerRadius = dp(2).toFloat() }
        val hp = LinearLayout.LayoutParams(dp(42), dp(4))
        hp.gravity = Gravity.CENTER_HORIZONTAL
        panel.addView(handle, hp)
        sheetBody = LinearLayout(this)
        sheetBody.orientation = LinearLayout.VERTICAL
        panel.addView(sheetBody, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        root.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

        setContentView(root)
        root.post { np.translationY = root.height.toFloat() }
    }

    private fun buildNp() {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.setPadding(dp(20), dp(12), dp(20), dp(28))
        np.addView(col, FrameLayout.LayoutParams(-1, -1))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val c0 = icon(R.drawable.ic_chevron_down, 44, C.TEXT)
        c0.setOnClickListener { closeNP() }
        header.addView(c0, LinearLayout.LayoutParams(dp(44), dp(48)))
        val hmid = LinearLayout(this)
        hmid.orientation = LinearLayout.VERTICAL
        hmid.gravity = Gravity.CENTER_HORIZONTAL
        val pl = tv("PLAYING FROM", 9.5f, C.MUTED, true)
        pl.letterSpacing = 0.35f
        pl.gravity = Gravity.CENTER
        npLabel = tv("All songs", 13f, C.TEXT, true)
        npLabel.gravity = Gravity.CENTER
        npLabel.maxLines = 1
        npLabel.ellipsize = TextUtils.TruncateAt.END
        hmid.addView(pl, LinearLayout.LayoutParams(-1, -2))
        hmid.addView(npLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
        header.addView(hmid, LinearLayout.LayoutParams(0, -2, 1f))
        npLike = icon(R.drawable.ic_heart_o, 40, C.TEXT, 9)
        npPl = icon(R.drawable.ic_playlist, 40, C.TEXT, 9)
        npQueue = icon(R.drawable.ic_queue, 40, C.TEXT, 9)
        listOf(npLike, npPl, npQueue).forEach { header.addView(circleClick(it), LinearLayout.LayoutParams(dp(40), dp(48))) }
        npLike.setOnClickListener { likeCurrent() }
        npPl.setOnClickListener {
            val s = PlayerService.cur() ?: return@setOnClickListener
            if (s.currentUid < 0) { toast("Play a song first"); return@setOnClickListener }
            playlistPickerSheet(s.currentUid)
        }
        npQueue.setOnClickListener { queueSheet() }
        col.addView(header, LinearLayout.LayoutParams(-1, -2))

        val spacer = View(this)
        col.addView(spacer, LinearLayout.LayoutParams(0, 0, 1f))

        val discWrap = FrameLayout(this)
        val size = (resources.displayMetrics.widthPixels * 0.54f).toInt()
        val lp = LinearLayout.LayoutParams(size, size)
        lp.gravity = Gravity.CENTER_HORIZONTAL
        disc = DiscView(this)
        discWrap.addView(disc, FrameLayout.LayoutParams(-1, -1))
        skipBadge = tv("+10s", 16f, C.TEXT, true)
        skipBadge.background = GradientDrawable().apply {
            setColor(0xD004060B.toInt()); cornerRadius = dp(22).toFloat()
        }
        skipBadge.setPadding(dp(20), dp(10), dp(20), dp(10))
        skipBadge.visibility = View.GONE
        discWrap.addView(skipBadge, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        col.addView(discWrap, lp)
        discWrap.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> discWrap.tag = floatArrayOf(e.x, e.y)
                MotionEvent.ACTION_UP -> {
                    val s = PlayerService.cur()
                    val d = discWrap.tag as? FloatArray
                    if (s != null && d != null && abs(e.x - d[0]) < dp(24) && abs(e.y - d[1]) < dp(24)) {
                        val back = e.x < discWrap.width / 2
                        val ms = (s.position() + if (back) -10000 else 10000).coerceIn(0, max(1, s.duration()))
                        s.seekTo(ms)
                        skipBadge.text = if (back) "\u221210s" else "+10s"
                        skipBadge.visibility = View.VISIBLE
                        skipBadge.alpha = 1f
                        skipBadge.animate().alpha(0f).setStartDelay(500).setDuration(300).withEndAction { skipBadge.visibility = View.GONE }
                    }
                    true
                }
                else -> {}
            }
            true
        }

        npTitle = tv("—", 21f, C.TEXT, true)
        npTitle.gravity = Gravity.CENTER
        npTitle.maxLines = 1
        npTitle.ellipsize = TextUtils.TruncateAt.END
        val tl2 = LinearLayout.LayoutParams(-1, -2)
        tl2.topMargin = dp(18)
        col.addView(npTitle, tl2)
        npSub = tv("Local file", 13f, C.MUTED)
        npSub.gravity = Gravity.CENTER
        col.addView(npSub, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })

        val seekRow = LinearLayout(this)
        seekRow.orientation = LinearLayout.HORIZONTAL
        seekRow.gravity = Gravity.CENTER_VERTICAL
        col.addView(seekRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        curTime = tv("0:00", 12f, C.MUTED)
        curTime.gravity = Gravity.CENTER
        seekRow.addView(curTime, LinearLayout.LayoutParams(dp(42), -2))
        seek = SeekBar(this)
        seek.max = 1000
        val seekThumb = GradientDrawable()
        seekThumb.shape = GradientDrawable.OVAL
        seekThumb.setColor(-1)
        seekThumb.setSize(dp(12), dp(12))
        seek.thumb = seekThumb
        seek.progressTintList = android.content.res.ColorStateList.valueOf(C.ACCENT2)
        seek.thumbTintList = android.content.res.ColorStateList.valueOf(-1)
        seek.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0x24FFFFFF)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (!fromUser) return
                val s = PlayerService.cur() ?: return
                val d = s.duration()
                if (d > 0) { s.seekTo(p * d / 1000); curTime.text = fmt(p * d / 1000.0 / 1000.0) }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        seekRow.addView(seek, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10); rightMargin = dp(10) })
        totTime = tv("0:00", 12f, C.MUTED)
        totTime.gravity = Gravity.CENTER
        seekRow.addView(totTime, LinearLayout.LayoutParams(dp(42), -2))

        val ctl = LinearLayout(this)
        ctl.orientation = LinearLayout.HORIZONTAL
        ctl.gravity = Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
        col.addView(ctl, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        shuffleBtn = icon(R.drawable.ic_shuffle, 46, C.MUTED, 10)
        prevBtn = icon(R.drawable.ic_prev, 46, C.TEXT, 10)
        nextBtn = icon(R.drawable.ic_next, 46, C.TEXT, 10)
        repeatBtn = icon(R.drawable.ic_repeat, 46, C.MUTED, 10)
        playBtn = ImageView(this)
        playBtn.setImageResource(R.drawable.ic_play)
        playBtn.setColorFilter(C.BG)
        playBtn.background = ripple(GradientDrawable().apply { setColor(-1); shape = GradientDrawable.OVAL })
        shuffleBtn.setOnClickListener { PlayerService.send(this, "SHUFFLE"); toast(if (PlayerService.shuffle) "Shuffle on" else "Shuffle off") }
        prevBtn.setOnClickListener { PlayerService.send(this, "PREV") }
        nextBtn.setOnClickListener { PlayerService.send(this, "NEXT") }
        playBtn.setOnClickListener { PlayerService.send(this, "PLAY_PAUSE") }
        repeatBtn.setOnClickListener {
            PlayerService.send(this, "REPEAT")
            toast(when (PlayerService.repeat) { 1 -> "Repeat all songs"; 2 -> "Repeat current song"; else -> "Repeat off" })
        }
        ctl.addView(circleClick(shuffleBtn), LinearLayout.LayoutParams(dp(46), dp(52)))
        ctl.addView(prevBtn, LinearLayout.LayoutParams(dp(50), dp(52)))
        ctl.addView(playBtn, LinearLayout.LayoutParams(dp(74), dp(74)).apply { leftMargin = dp(10); rightMargin = dp(10) })
        ctl.addView(nextBtn, LinearLayout.LayoutParams(dp(50), dp(52)))
        ctl.addView(circleClick(repeatBtn), LinearLayout.LayoutParams(dp(46), dp(52)))

        val spacer2 = View(this)
        col.addView(spacer2, LinearLayout.LayoutParams(0, 0, 0.75f))

        val npGesture = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                val dx = e2.x - (e1?.x ?: e2.x)
                val dy = e2.y - (e1?.y ?: e2.y)
                if (abs(dx) > abs(dy) * 1.4 && abs(dx) > dp(90)) {
                    animateSongChange(dx < 0)
                    PlayerService.send(this@MainActivity, if (dx < 0) "NEXT" else "PREV")
                    return true
                }
                if (dy > dp(130) && abs(dy) > abs(dx)) { closeNP(); return true }
                return false
            }
        })
        col.setOnTouchListener { _, e -> npGesture.onTouchEvent(e); true }
    }

    private fun animateSongChange(left: Boolean) {
        val d = dp(56).toFloat()
        listOf(disc, npTitle).forEach { v ->
            v.translationX = if (left) d else -d
            v.alpha = 0.3f
            v.animate().translationX(0f).alpha(1f).setDuration(280)
                .setInterpolator(AccelerateDecelerateInterpolator()).start()
        }
    }

    private fun likeCurrent() {
        val s = PlayerService.cur() ?: return
        if (s.currentUid < 0) { toast("Play a song first"); return }
        val liked = db.toggleLike(s.currentUid)
        toast(if (liked) "Added to Liked songs" else "Removed from Liked songs")
        updateControls(); renderAll()
    }

    fun openNP() {
        npOpen = true
        np.visibility = View.VISIBLE
        np.translationY = root.height.toFloat()
        np.animate().translationY(0f).setDuration(360).setInterpolator(AccelerateDecelerateInterpolator()).start()
    }

    fun closeNP() {
        npOpen = false
        np.animate().translationY(root.height.toFloat()).setDuration(340)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { np.visibility = View.GONE }.start()
    }

    private fun switchTab(v: String) {
        if (view == v) return
        view = v
        tabThumb.animate().translationX(if (v == "playlists") tabThumb.width + dp(8).toFloat() * 0f + (tabsWrap.width - tabThumb.width - dp(8)).toFloat() else 0f).setDuration(250).start()
        tabSongs.setTextColor(if (v == "songs") C.TEXT else C.MUTED)
        tabPlaylists.setTextColor(if (v == "playlists") C.TEXT else C.MUTED)
        renderAll()
    }

    private fun queueForContext(): Pair<List<Long>, String> {
        val ts = db.tracks()
        return when (plCtx) {
            -1L -> Pair(sortTracks(ts, sortMode).filter { db.isLiked(it.uid) }.map { it.uid }, "Liked songs")
            null -> Pair(sortTracks(ts, sortMode).map { it.uid }, "All songs")
            else -> {
                val name = db.playlistName(plCtx!!) ?: "Playlist"
                Pair(sortTracks(ts.filter { db.inPlaylist(plCtx!!, it.uid) }, sortMode).map { it.uid }, name)
            }
        }
    }

    private fun playContext(uid: Long) {
        val (q, label) = queueForContext()
        PlayerService.send(this, "PLAY_AT", uid = uid, list = q, label = label)
        openNP()
    }

    private fun emptyState(iconRes: Int, title: String, sub: String, btn: String, onClick: () -> Unit): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.setPadding(dp(22), dp(44), dp(22), dp(44))
        val bg = GradientDrawable()
        bg.setColor(0x00000000)
        bg.setStroke(dp(2), 0x1FFFFFFF, dp(8).toFloat(), dp(6).toFloat())
        bg.cornerRadius = dp(24).toFloat()
        box.background = bg
        val ic = FrameLayout(this)
        ic.background = surfaceBg(dp(38).toFloat())
        val iv = icon(iconRes, 30, C.ACCENT, 6)
        ic.addView(iv, FrameLayout.LayoutParams(-1, -1))
        val icp = LinearLayout.LayoutParams(dp(76), dp(76))
        icp.gravity = Gravity.CENTER_HORIZONTAL
        box.addView(ic, icp)
        val t = tv(title, 17f, C.TEXT, true)
        t.gravity = Gravity.CENTER
        box.addView(t, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        val s = tv(sub, 13f, C.MUTED)
        s.gravity = Gravity.CENTER
        box.addView(s, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        val b = TextView(this)
        b.text = btn
        b.textSize = 14.5f
        b.typeface = Typeface.DEFAULT_BOLD
        b.setTextColor(-1)
        b.background = ripple(gradBg(dp(24).toFloat()))
        b.setPadding(dp(26), dp(12), dp(26), dp(12))
        b.setOnClickListener { onClick() }
        box.addView(b, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(22); gravity = Gravity.CENTER_HORIZONTAL })
        return box
    }

    fun pretty(n: String): String = n.replace('_', ' ').replace(Regex("\\s+"), " ").trim()

    fun makeRow(t: Track, inPlaylist: Boolean): View {
        val s = PlayerService.cur()
        val cur = s?.currentUid == t.uid
        val playing = s != null && PlayerService.playing()
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(12), dp(8), dp(4), dp(8))
        row.background = ripple(surfaceBg(dp(16).toFloat(), cur))
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(6)
        row.layoutParams = lp
        if (cur) {
            val g = GradientDrawable()
            g.setColor(0x240229FF.toInt())
            g.cornerRadius = dp(16).toFloat()
            g.setStroke(1, 0x8C0229FF.toInt())
            row.background = ripple(g)
        }
        val art = FrameLayout(this)
        art.background = gradBg(dp(12).toFloat())
        val note = icon(R.drawable.ic_music, 20, -1, 8)
        val eq = EqView(this)
        eq.visibility = if (cur && playing) View.VISIBLE else View.INVISIBLE
        art.addView(note, FrameLayout.LayoutParams(-1, -1))
        art.addView(eq, FrameLayout.LayoutParams(dp(22), dp(16), Gravity.CENTER))
        row.addView(art, LinearLayout.LayoutParams(dp(48), dp(48)))

        val meta = LinearLayout(this)
        meta.orientation = LinearLayout.VERTICAL
        val name = tv(pretty(t.name), 15f, if (cur) C.ACCENT2 else C.TEXT, true)
        name.maxLines = 1
        name.ellipsize = TextUtils.TruncateAt.END
        val sub = tv(if (t.dur != null && t.dur > 0) fmt(t.dur) else "Local file", 12f, C.MUTED)
        meta.addView(name, LinearLayout.LayoutParams(-1, -2))
        meta.addView(sub, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
        row.addView(meta, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(10) })

        val liked = db.isLiked(t.uid)
        val like = icon(if (liked) R.drawable.ic_heart else R.drawable.ic_heart_o, 36, if (liked) C.ACCENT2 else C.MUTED, 8)
        like.setOnClickListener {
            val now = db.toggleLike(t.uid)
            toast(if (now) "Added to Liked songs" else "Removed from Liked songs")
            updateControls(); renderAll()
        }
        val more = icon(R.drawable.ic_more, 36, C.MUTED, 8)
        more.setOnClickListener { songMenuSheet(t.uid, inPlaylist) }
        row.addView(circleClick(like), LinearLayout.LayoutParams(dp(38), dp(38)))
        row.addView(circleClick(more), LinearLayout.LayoutParams(dp(38), dp(38)))
        row.setOnClickListener { playContext(t.uid) }
        return row
    }

    fun renderAll() {
        content.removeView(songsEmpty)
        content.removeView(plEmpty)
        val detail = plCtx != null
        backBtn.visibility = if (detail) View.VISIBLE else View.INVISIBLE
        sortBtn.visibility = if (!detail && view == "songs") View.VISIBLE else View.INVISIBLE
        tabsWrap.visibility = if (detail) View.GONE else View.VISIBLE
        searchWrap.visibility = View.VISIBLE
        songsScroll.visibility = if (detail || view == "songs") if (detail) View.GONE else View.VISIBLE else View.GONE
        plScroll.visibility = if (!detail && view == "playlists") View.VISIBLE else View.GONE
        detScroll.visibility = if (detail) View.VISIBLE else View.GONE
        if (detail) renderDetail() else if (view == "songs") renderSongs() else renderPlaylists()
    }

    private fun renderSongs() {
        songsList.removeAllViews()
        val ts = sortTracks(db.tracks(), sortMode).filter { searchQ.isEmpty() || it.name.lowercase().contains(searchQ) }
        (content.indexOfChild(songsEmpty)).let { if (it >= 0) content.removeView(songsEmpty) }
        if (db.tracks().isEmpty()) {
            val lp = FrameLayout.LayoutParams(-1, -2)
            lp.setMargins(dp(16), dp(16), dp(16), 0)
            content.addView(songsEmpty, 0, lp)
        }
        ts.forEach { songsList.addView(makeRow(it, false)) }
    }

    private fun renderPlaylists() {
        plList.removeAllViews()
        val pls = db.playlists().filter { searchQ.isEmpty() || it.name.lowercase().contains(searchQ) }
        var row: LinearLayout? = null
        fun ensureRow() {
            if (row == null || row!!.childCount == 2) {
                row = LinearLayout(this)
                row!!.orientation = LinearLayout.HORIZONTAL
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.bottomMargin = dp(12)
                plList.addView(row, lp)
            }
        }
        fun card(name: String, count: String, heart: Boolean, dashed: Boolean, onClick: () -> Unit): View {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            val bg = if (dashed) GradientDrawable().apply {
                setColor(0x00000000); setStroke(dp(2), 0x38FFFFFF); cornerRadius = dp(18).toFloat()
            } else surfaceBg(dp(18).toFloat())
            card.background = ripple(bg)
            card.setPadding(dp(12), dp(12), dp(12), dp(12))
            val art = FrameLayout(this)
            if (!dashed) art.background = if (heart) gradBg2(dp(13).toFloat()) else gradBg(dp(13).toFloat())
            val iv = icon(if (heart) R.drawable.ic_heart else if (name == "New Playlist") R.drawable.ic_add else R.drawable.ic_playlist, 30, if (dashed) C.MUTED else -1, 10)
            art.addView(iv, FrameLayout.LayoutParams(-1, -1))
            card.addView(art, LinearLayout.LayoutParams(-1, dp(120)))
            val n = tv(name, 14.5f, C.TEXT, true)
            n.maxLines = 1; n.ellipsize = TextUtils.TruncateAt.END
            card.addView(n, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
            val c = tv(count, 12f, C.MUTED)
            card.addView(c, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
            card.setOnClickListener { onClick() }
            return card
        }
        ensureRow()
        val likedCount = db.likes().count { db.track(it) != null }
        row!!.addView(card("Liked Songs", "$likedCount song${if (likedCount == 1) "" else "s"}", true, false) { plCtx = -1; renderAll() },
            LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(6) })
        ensureRow()
        row!!.addView(card("New Playlist", "Create one", false, true) { newPlaylistSheet(null) },
            LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        pls.forEach { p ->
            ensureRow()
            val uids = db.plUids(p.id)
            val side = row!!.childCount == 0
            row!!.addView(card(p.name, "${uids.size} song${if (uids.size == 1) "" else "s"}", false, false) { plCtx = p.id; renderAll() },
                LinearLayout.LayoutParams(0, -2, 1f).apply { if (side) rightMargin = dp(6) else leftMargin = dp(6) })
        }
        if (row != null && row!!.childCount == 1) {
            row!!.addView(LinearLayout(this), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(6) })
        }
    }

    private fun renderDetail() {
        val id = plCtx!!
        detHero.removeAllViews()
        detList.removeAllViews()
        if (id == -1L) {
            val uids = db.likes().filter { db.track(it) != null }
            detHero.addView(heroView(R.drawable.ic_heart, "Liked Songs", "${uids.size} song${if (uids.size == 1) "" else "s"}", true, null))
            uids.mapNotNull { db.track(it) }.let { sortTracks(it, sortMode) }.forEach { detList.addView(makeRow(it, true)) }
        } else {
            val name = db.playlistName(id) ?: run { plCtx = null; renderAll(); return }
            val uids = db.plUids(id).mapNotNull { db.track(it) }
            detHero.addView(heroView(R.drawable.ic_playlist, name, "${uids.size} song${if (uids.size == 1) "" else "s"}", false, id))
            sortTracks(uids, sortMode).forEach { detList.addView(makeRow(it, true)) }
        }
    }

    private fun heroView(iconRes: Int, name: String, count: String, liked: Boolean, plId: Long?): View {
        val h = LinearLayout(this)
        h.orientation = LinearLayout.VERTICAL
        h.setPadding(dp(16), dp(16), dp(16), dp(16))
        val bg = GradientDrawable()
        bg.setColor(0x240229FF.toInt())
        bg.cornerRadius = dp(22).toFloat()
        bg.setStroke(1, 0x4D0229FF.toInt())
        h.background = bg
        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        val art = FrameLayout(this)
        art.background = if (liked) gradBg2(dp(18).toFloat()) else gradBg(dp(18).toFloat())
        val iv = icon(iconRes, 40, -1, 14)
        art.addView(iv, FrameLayout.LayoutParams(-1, -1))
        top.addView(art, LinearLayout.LayoutParams(dp(84), dp(84)))
        val meta = LinearLayout(this)
        meta.orientation = LinearLayout.VERTICAL
        val n = tv(name, 19f, C.TEXT, true)
        n.maxLines = 1; n.ellipsize = TextUtils.TruncateAt.END
        meta.addView(n, LinearLayout.LayoutParams(-1, -2))
        val c = tv(count, 13f, C.MUTED)
        meta.addView(c, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        top.addView(meta, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14); gravity = Gravity.CENTER_VERTICAL })
        h.addView(top, LinearLayout.LayoutParams(-1, -2))
        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL
        actions.gravity = Gravity.CENTER_VERTICAL
        val playAll = TextView(this)
        playAll.text = "Play All"
        playAll.textSize = 13f
        playAll.typeface = Typeface.DEFAULT_BOLD
        playAll.setTextColor(-1)
        playAll.background = ripple(gradBg(dp(20).toFloat()))
        playAll.setPadding(dp(16), dp(10), dp(16), dp(10))
        playAll.setOnClickListener {
            val (q, label) = queueForContext()
            if (q.isNotEmpty()) { PlayerService.send(this, "PLAY_AT", uid = q[0], list = q, label = label); openNP() }
            else toast("Nothing to play yet")
        }
        actions.addView(playAll, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        if (plId != null && plId > 0) {
            val addBtn = TextView(this)
            addBtn.text = "Add Songs"
            addBtn.textSize = 13f
            addBtn.typeface = Typeface.DEFAULT_BOLD
            addBtn.setTextColor(C.TEXT)
            addBtn.background = ripple(surfaceBg(dp(20).toFloat()))
            addBtn.setPadding(dp(16), dp(10), dp(16), dp(10))
            addBtn.setOnClickListener { songPickerSheet(plId) }
            actions.addView(addBtn, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
            val delBtn = TextView(this)
            delBtn.text = "Delete"
            delBtn.textSize = 13f
            delBtn.typeface = Typeface.DEFAULT_BOLD
            delBtn.setTextColor(C.DANGER)
            delBtn.background = ripple(surfaceBg(dp(20).toFloat()))
            delBtn.setPadding(dp(16), dp(10), dp(16), dp(10))
            delBtn.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete playlist?")
                    .setMessage("Songs stay in your library.")
                    .setPositiveButton("Delete") { _, _ ->
                        db.deletePlaylist(plId); plCtx = null; renderAll(); toast("Playlist deleted") }
                    .setNegativeButton("Cancel", null).show()
            }
            actions.addView(delBtn, LinearLayout.LayoutParams(-2, -2))
        }
        h.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        return h
    }

    fun showSheet(build: (LinearLayout) -> Unit) {
        sheetBody.removeAllViews()
        build(sheetBody)
        dim.visibility = View.VISIBLE
        dim.alpha = 0f
        dim.animate().alpha(1f).setDuration(220).start()
        panel.visibility = View.VISIBLE
        panel.translationY = panel.height.toFloat() + dp(40)
        panel.animate().translationY(0f).setDuration(300).start()
        sheetOpen = true
    }

    fun dismissSheet() {
        dim.animate().alpha(0f).setDuration(200).withEndAction { dim.visibility = View.GONE }.start()
        panel.animate().translationY(panel.height.toFloat() + dp(40)).setDuration(260)
            .withEndAction { panel.visibility = View.GONE }.start()
        sheetOpen = false
    }

    private fun sheetTitle(text: String): TextView {
        val t = tv(text, 11f, C.MUTED, true)
        t.letterSpacing = 0.18f
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        sheetBody.addView(t, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        return t
    }

    private fun sheetItem(label: String, iconRes: Int, danger: Boolean = false, onClick: () -> Unit) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(12), dp(11), dp(12), dp(11))
        row.background = ripple(surfaceBg(dp(13).toFloat(), false))
        val iv = icon(iconRes, 20, if (danger) C.DANGER else C.MUTED, 1)
        row.addView(iv, LinearLayout.LayoutParams(dp(20), dp(20)))
        val t = tv(label, 15f, if (danger) C.DANGER else C.TEXT)
        row.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14) })
        row.setOnClickListener { dismissSheet(); onClick() }
        sheetBody.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
    }

    fun songMenuSheet(uid: Long, inPlaylist: Boolean) {
        val t = db.track(uid) ?: return
        val liked = db.isLiked(uid)
        showSheet {
            sheetTitle(pretty(t.name))
            sheetItem("Play", R.drawable.ic_play) { playContext(uid) }
            sheetItem(if (liked) "Remove from Liked songs" else "Add to Liked songs",
                if (liked) R.drawable.ic_heart else R.drawable.ic_heart_o) {
                db.toggleLike(uid); renderAll(); updateControls()
            }
            sheetItem("Add to playlist", R.drawable.ic_playlist) { playlistPickerSheet(uid) }
            if (inPlaylist && plCtx != null && plCtx != -1L)
                sheetItem("Remove from this playlist", R.drawable.ic_close, true) {
                    db.removeFromPlaylist(plCtx!!, uid); renderAll()
                }
            if (inPlaylist && plCtx == -1L)
                sheetItem("Remove from Liked songs", R.drawable.ic_close, true) {
                    db.toggleLike(uid); renderAll()
                }
            sheetItem("Remove from library", R.drawable.ic_trash, true) {
                AlertDialog.Builder(this)
                    .setTitle("Remove from library?")
                    .setMessage("\"${t.name}\" will be deleted from your device storage.")
                    .setPositiveButton("Remove") { _, _ ->
                        val s = PlayerService.cur()
                        if (s?.currentUid == uid) PlayerService.send(this, "NEXT")
                        db.deleteTrack(uid)
                        java.io.File(t.path).delete()
                        renderAll()
                        toast("Removed from library")
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }
    }

    fun playlistPickerSheet(uid: Long) {
        showSheet {
            sheetTitle("Add to playlist")
            db.playlists().forEach { p ->
                val inPl = db.inPlaylist(p.id, uid)
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(dp(12), dp(11), dp(12), dp(11))
                row.background = ripple(surfaceBg(dp(13).toFloat(), false))
                val iv = icon(R.drawable.ic_playlist, 20, C.MUTED, 1)
                row.addView(iv, LinearLayout.LayoutParams(dp(20), dp(20)))
                val t = tv(p.name, 15f, C.TEXT)
                row.addView(t, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14) })
                if (inPl) {
                    val c = icon(R.drawable.ic_check, 20, C.ACCENT2, 1)
                    row.addView(c, LinearLayout.LayoutParams(dp(20), dp(20)))
                }
                row.setOnClickListener {
                    if (inPl) { db.removeFromPlaylist(p.id, uid); toast("Removed from \"${p.name}\"") }
                    else { db.addToPlaylist(p.id, uid); toast("Added to \"${p.name}\"") }
                    dismissSheet(); renderAll()
                }
                sheetBody.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
            }
            sheetItem("New playlist", R.drawable.ic_add) { newPlaylistSheet(uid) }
        }
    }

    fun newPlaylistSheet(uidToAdd: Long?) {
        showSheet {
            sheetTitle("New playlist")
            val et = EditText(this)
            et.hint = "Playlist name"
            et.setTextColor(C.TEXT)
            et.setHintTextColor(C.MUTED)
            et.textSize = 15f
            et.background = surfaceBg(dp(13).toFloat())
            et.setPadding(dp(14), dp(11), dp(14), dp(11))
            sheetBody.addView(et, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
            val b = TextView(this)
            b.text = "Create Playlist"
            b.textSize = 14.5f
            b.typeface = Typeface.DEFAULT_BOLD
            b.setTextColor(-1)
            b.gravity = Gravity.CENTER
            b.background = ripple(gradBg(dp(24).toFloat()))
            b.setPadding(dp(16), dp(12), dp(16), dp(12))
            sheetBody.addView(b, LinearLayout.LayoutParams(-1, -2))
            fun create() {
                val name = et.text.toString().trim().ifEmpty { "My Playlist" }
                val id = db.createPlaylist(name)
                if (uidToAdd != null) db.addToPlaylist(id, uidToAdd)
                dismissSheet(); renderAll()
                toast("Playlist \"$name\" created")
            }
            b.setOnClickListener { create() }
            et.setOnEditorActionListener { _, _, _ -> create(); true }
        }
    }

    fun songPickerSheet(plId: Long) {
        val name = db.playlistName(plId) ?: return
        showSheet {
            sheetTitle("Add songs · $name")
            sortTracks(db.tracks(), sortMode).forEach { t ->
                val inPl = db.inPlaylist(plId, t.uid)
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(dp(12), dp(10), dp(12), dp(10))
                row.background = ripple(surfaceBg(dp(13).toFloat(), false))
                val iv = icon(R.drawable.ic_music, 20, C.MUTED, 1)
                row.addView(iv, LinearLayout.LayoutParams(dp(20), dp(20)))
                val n = tv(pretty(t.name), 15f, C.TEXT)
                n.maxLines = 1; n.ellipsize = TextUtils.TruncateAt.END
                row.addView(n, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14) })
                if (t.dur != null && t.dur > 0) {
                    val d = tv(fmt(t.dur), 12f, C.MUTED)
                    row.addView(d, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
                }
                val tag = FrameLayout(this)
                val chk = if (inPl) icon(R.drawable.ic_check, 20, C.ACCENT2, 1) else View(this)
                tag.addView(chk, FrameLayout.LayoutParams(-1, -1))
                row.addView(tag, LinearLayout.LayoutParams(dp(20), dp(20)).apply { leftMargin = dp(10) })
                row.setOnClickListener {
                    if (inPl) db.removeFromPlaylist(plId, t.uid) else db.addToPlaylist(plId, t.uid)
                    renderAll()
                }
                sheetBody.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
            }
        }
    }

    fun queueSheet() {
        val s = PlayerService.cur()
        if (s == null || s.queue.isEmpty()) { toast("Nothing in the queue"); return }
        showSheet {
            sheetTitle("Playing from · ${s.queueLabel}")
            s.queue.forEachIndexed { i, uid ->
                val t = db.track(uid) ?: return@forEachIndexed
                val cur = uid == s.currentUid
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.setPadding(dp(12), dp(10), dp(12), dp(10))
                row.background = ripple(surfaceBg(dp(13).toFloat(), false))
                val idx = tv(if (cur) "▶" else "${i + 1}", 13f, if (cur) C.ACCENT2 else C.MUTED)
                idx.gravity = Gravity.CENTER
                row.addView(idx, LinearLayout.LayoutParams(dp(26), -2))
                val n = tv(pretty(t.name), 15f, if (cur) C.ACCENT2 else C.TEXT)
                n.maxLines = 1; n.ellipsize = TextUtils.TruncateAt.END
                row.addView(n, LinearLayout.LayoutParams(0, -2, 1f))
                if (t.dur != null && t.dur > 0) {
                    val d = tv(fmt(t.dur), 12f, C.MUTED)
                    row.addView(d, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(8) })
                }
                val more = icon(R.drawable.ic_more, 18, C.MUTED, 4)
                more.setOnClickListener {
                    dismissSheet(); songMenuSheet(uid, false)
                }
                row.addView(more, LinearLayout.LayoutParams(dp(26), dp(26)).apply { leftMargin = dp(6) })
                row.setOnClickListener { dismissSheet(); PlayerService.send(this, "PLAY_UID", uid = uid) }
                sheetBody.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(2) })
            }
        }
    }

    private fun showSortMenu() {
        val popup = PopupWindow(this)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.setBackgroundDrawable(GradientDrawable().apply { setColor(C.PANEL); cornerRadius = dp(15).toFloat() })
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(6), dp(6), dp(6), dp(6))
        SORTS.forEach { (id, label) ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(dp(12), dp(12), dp(12), dp(12))
            row.background = ripple(surfaceBg(dp(11).toFloat(), false))
            val t = tv(label, 14f, C.TEXT)
            row.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
            if (sortMode == id) {
                val c = icon(R.drawable.ic_check, 18, C.ACCENT2, 1)
                row.addView(c, LinearLayout.LayoutParams(dp(18), dp(18)))
            }
            row.setOnClickListener {
                sortMode = id
                prefs.edit().putString("sort", id).apply()
                popup.dismiss(); renderAll()
            }
            col.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
        popup.contentView = col
        popup.width = dp(220)
        popup.height = -2
        popup.showAsDropDown(sortBtn, -dp(160), dp(4))
    }

    private fun pickFiles() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "audio/*"
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(i, 7)
    }

    override fun onActivityResult(code: Int, res: Int, data: Intent?) {
        super.onActivityResult(code, res, data)
        if (code != 7 || res != RESULT_OK) return
        val uris = ArrayList<Uri>()
        val clip = data?.clipData
        if (clip != null) for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri)
        else data?.data?.let { uris.add(it) }
        if (uris.isEmpty()) return
        toast("Adding ${uris.size} file${if (uris.size == 1) "" else "s"}...")
        Thread {
            var added = 0
            val firstUid = db.nextUid()
            uris.forEach { u ->
                try {
                    var name = "Song"
                    contentResolver.query(u, null, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx >= 0) name = c.getString(idx)
                        }
                    }
                    name = name.substringBeforeLast('.')
                    val uid = db.nextUid()
                    val f = java.io.File(filesDir, "track_$uid")
                    contentResolver.openInputStream(u)?.use { input ->
                        f.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@forEach
                    db.insertTrack(uid, name, f.absolutePath, System.currentTimeMillis())
                    added++
                } catch (e: Exception) { }
            }
            runOnUiThread {
                if (added > 0) {
                    renderAll()
                    PlayerService.send(this, "PROBE")
                    toast("$added song${if (added == 1) "" else "s"} added to library")
                    val s = PlayerService.cur()
                    if (s != null && s.currentUid < 0L) {
                        val (q, label) = queueForContext()
                        val target = firstUid
                        if (q.contains(target)) PlayerService.send(this, "PLAY_AT", uid = target, list = q, label = label)
                    }
                } else toast("No audio files found")
            }
        }.start()
    }

    private fun updateControls() {
        val s = PlayerService.cur()
        val playing = s != null && PlayerService.playing()
        playBtn.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        playBtn.setColorFilter(C.BG)
        miniPlay.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        disc.setSpin(playing)
        val cur = s?.currentUid ?: -1L
        val liked = if (cur >= 0) db.isLiked(cur) else false
        npLike.setImageResource(if (liked) R.drawable.ic_heart else R.drawable.ic_heart_o)
        npLike.setColorFilter(if (liked) C.ACCENT2 else C.TEXT)
        miniLike.setImageResource(if (liked) R.drawable.ic_heart else R.drawable.ic_heart_o)
        miniLike.setColorFilter(if (liked) C.ACCENT2 else C.MUTED)
        shuffleBtn.setImageResource(R.drawable.ic_shuffle)
        shuffleBtn.setColorFilter(if (PlayerService.shuffle) C.ACCENT2 else C.MUTED)
        repeatBtn.setImageResource(if (PlayerService.repeat == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat)
        repeatBtn.setColorFilter(if (PlayerService.repeat > 0) C.ACCENT2 else C.MUTED)
        if (cur >= 0) {
            val t = db.track(cur)
            mini.visibility = View.VISIBLE
            npTitle.text = pretty(t?.name ?: "—")
            miniName.text = pretty(t?.name ?: "—")
            npLabel.text = s?.queueLabel ?: "All songs"
            miniSub.text = s?.queueLabel ?: "Hasbi"
            npSub.text = if (t?.dur != null && t.dur > 0) "Local file · ${fmt(t.dur)}" else "Local file"
        }
        updateTick()
    }

    private fun updateTick() {
        val s = PlayerService.cur() ?: return
        val pos = s.position()
        val dur = s.duration()
        if (!npOpen && s.currentUid < 0) return
        curTime.text = fmt(pos / 1000.0)
        if (dur > 0) {
            totTime.text = fmt(dur / 1000.0)
            seek.progress = (pos * 1000 / dur).coerceIn(0, 1000)
            disc.progress = pos.toFloat() / dur
            miniBar.layoutParams.width = (mini.width * pos / dur).coerceIn(0, mini.width)
            miniBar.requestLayout()
        }
    }

    override fun onBackPressed() {
        if (sheetOpen) { dismissSheet(); return }
        if (npOpen) { closeNP(); return }
        if (plCtx != null) { plCtx = null; renderAll(); return }
        moveTaskToBack(true)
    }
}
