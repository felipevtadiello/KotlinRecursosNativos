package com.example.recursosnativos

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.*

data class FormData(
    val name: String,
    val email: String,
    val comment: String,
    val imagePath: String?
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "formdata.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE FormData (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "email TEXT," +
                    "comment TEXT," +
                    "image_path TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS FormData")
        onCreate(db)
    }

    fun insertFormData(formData: FormData): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("name", formData.name)
            put("email", formData.email)
            put("comment", formData.comment)
            put("image_path", formData.imagePath)
        }
        return db.insert("FormData", null, values)
    }

    fun getAllFormData(): List<FormData> {
        val formDataList = mutableListOf<FormData>()
        val db = readableDatabase
        val cursor: Cursor = db.query("FormData", null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val email = cursor.getString(cursor.getColumnIndexOrThrow("email"))
                val comment = cursor.getString(cursor.getColumnIndexOrThrow("comment"))
                val imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"))
                formDataList.add(FormData(name, email, comment, imagePath))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return formDataList
    }
}
