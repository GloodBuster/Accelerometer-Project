package com.example.accelerometerproject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MySQLiteHelper(context: Context): SQLiteOpenHelper(context, "steps.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        val createTables = "CREATE TABLE IF NOT EXISTS steps (id INTEGER PRIMARY KEY AUTOINCREMENT, steps INTEGER, date TEXT);"

        try{
            db?.execSQL(createTables)
        } catch (e: Exception) {
            println("Se jodió todo")
        }

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    fun addSteps(steps: Int) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.format(Date())

        val step = ContentValues()
        step.put("steps", steps)
        step.put("date", date)

        val db = this.writableDatabase
        db.insert("steps", null, step)
        db.close()
    }



    fun getSteps(): List<Steps> {
        val steps = ArrayList<Steps>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM steps", null)

        if(cursor.moveToFirst()) {
            do {
                val step = Steps(cursor.getInt(0), cursor.getInt(1), cursor.getString(2))
                steps.add(step)
            } while (cursor.moveToNext())

        }

        cursor.close()
        db.close()
        return steps
    }

}