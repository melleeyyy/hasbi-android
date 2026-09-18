package com.melleeyyy.hasbi

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Track(val uid: Long, val name: String, val path: String, val dur: Double?, val addedAt: Long)
data class PlRow(val id: Long, val name: String)

class Db(ctx: Context) {
    private val h = object : SQLiteOpenHelper(ctx.applicationContext, "hasbi.db", null, 1) {
        override fun onCreate(d: SQLiteDatabase) {
            d.execSQL("CREATE TABLE tracks(uid INTEGER PRIMARY KEY, name TEXT, path TEXT, dur REAL, addedAt INTEGER)")
            d.execSQL("CREATE TABLE playlists(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)")
            d.execSQL("CREATE TABLE pl_songs(plId INTEGER, uid INTEGER, pos INTEGER)")
            d.execSQL("CREATE INDEX i_pls ON pl_songs(plId, pos)")
            d.execSQL("CREATE TABLE likes(uid INTEGER PRIMARY KEY)")
        }
        override fun onUpgrade(d: SQLiteDatabase, o: Int, n: Int) {}
    }

    private fun w(): SQLiteDatabase = h.writableDatabase

    fun nextUid(): Long {
        w().rawQuery("SELECT IFNULL(MAX(uid),0)+1 FROM tracks", null).use {
            it.moveToFirst(); return it.getLong(0)
        }
    }

    private fun rowToTrack(c: android.database.Cursor): Track = Track(
        c.getLong(0), c.getString(1), c.getString(2),
        if (c.isNull(3)) null else c.getDouble(3), c.getLong(4))

    fun tracks(): MutableList<Track> {
        val out = mutableListOf<Track>()
        w().rawQuery("SELECT uid,name,path,dur,addedAt FROM tracks", null).use {
            while (it.moveToNext()) out.add(rowToTrack(it))
        }
        return out
    }

    fun track(uid: Long): Track? {
        w().rawQuery("SELECT uid,name,path,dur,addedAt FROM tracks WHERE uid=?", arrayOf(uid.toString())).use {
            if (it.moveToFirst()) return rowToTrack(it)
        }
        return null
    }

    fun nextNoDur(): Track? {
        w().rawQuery("SELECT uid,name,path,dur,addedAt FROM tracks WHERE dur IS NULL LIMIT 1", null).use {
            if (it.moveToFirst()) return rowToTrack(it)
        }
        return null
    }

    fun insertTrack(uid: Long, name: String, path: String, addedAt: Long) {
        val cv = ContentValues()
        cv.put("uid", uid); cv.put("name", name); cv.put("path", path); cv.put("addedAt", addedAt)
        w().insertWithOnConflict("tracks", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun updateDur(uid: Long, dur: Double) {
        w().execSQL("UPDATE tracks SET dur=? WHERE uid=?", arrayOf(dur, uid.toString()))
    }

    fun deleteTrack(uid: Long) {
        w().execSQL("DELETE FROM tracks WHERE uid=?", arrayOf(uid.toString()))
        w().execSQL("DELETE FROM pl_songs WHERE uid=?", arrayOf(uid.toString()))
        w().execSQL("DELETE FROM likes WHERE uid=?", arrayOf(uid.toString()))
    }

    fun clearAll() {
        w().execSQL("DELETE FROM tracks")
        w().execSQL("DELETE FROM playlists")
        w().execSQL("DELETE FROM pl_songs")
        w().execSQL("DELETE FROM likes")
    }

    fun playlists(): List<PlRow> {
        val out = mutableListOf<PlRow>()
        w().rawQuery("SELECT id,name FROM playlists ORDER BY id", null).use {
            while (it.moveToNext()) out.add(PlRow(it.getLong(0), it.getString(1)))
        }
        return out
    }

    fun playlistName(id: Long): String? {
        w().rawQuery("SELECT name FROM playlists WHERE id=?", arrayOf(id.toString())).use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return null
    }

    fun createPlaylist(name: String): Long {
        val cv = ContentValues(); cv.put("name", name)
        return w().insert("playlists", null, cv)
    }

    fun deletePlaylist(id: Long) {
        w().execSQL("DELETE FROM playlists WHERE id=?", arrayOf(id.toString()))
        w().execSQL("DELETE FROM pl_songs WHERE plId=?", arrayOf(id.toString()))
    }

    fun plUids(id: Long): List<Long> {
        val out = mutableListOf<Long>()
        w().rawQuery("SELECT uid FROM pl_songs WHERE plId=? ORDER BY pos", arrayOf(id.toString())).use {
            while (it.moveToNext()) out.add(it.getLong(0))
        }
        return out
    }

    fun inPlaylist(id: Long, uid: Long): Boolean {
        w().rawQuery("SELECT 1 FROM pl_songs WHERE plId=? AND uid=?", arrayOf(id.toString(), uid.toString())).use {
            return it.moveToFirst()
        }
    }

    fun addToPlaylist(id: Long, uid: Long) {
        if (inPlaylist(id, uid)) return
        w().execSQL("INSERT INTO pl_songs(plId,uid,pos) SELECT ?,?,IFNULL(MAX(pos),0)+1 FROM pl_songs WHERE plId=?",
            arrayOf(id.toString(), uid.toString(), id.toString()))
    }

    fun removeFromPlaylist(id: Long, uid: Long) {
        w().execSQL("DELETE FROM pl_songs WHERE plId=? AND uid=?", arrayOf(id.toString(), uid.toString()))
    }

    fun likes(): MutableList<Long> {
        val out = mutableListOf<Long>()
        w().rawQuery("SELECT uid FROM likes", null).use {
            while (it.moveToNext()) out.add(it.getLong(0))
        }
        return out
    }

    fun isLiked(uid: Long): Boolean {
        w().rawQuery("SELECT 1 FROM likes WHERE uid=?", arrayOf(uid.toString())).use {
            return it.moveToFirst()
        }
    }

    fun toggleLike(uid: Long): Boolean {
        if (isLiked(uid)) {
            w().execSQL("DELETE FROM likes WHERE uid=?", arrayOf(uid.toString()))
            return false
        }
        w().execSQL("INSERT OR IGNORE INTO likes(uid) VALUES(?)", arrayOf(uid.toString()))
        return true
    }
}
