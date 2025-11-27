package PlateRecognitionApp.plakass.utils

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF = "APP_SESSION"
    private const val KEY_TOKEN = "token"

    var token: String? = null

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        token = sp.getString(KEY_TOKEN, null)
    }

    fun saveToken(context: Context, value: String) {
        token = value
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_TOKEN, value).apply()
    }

    fun logout(context: Context) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        sp.edit().clear().apply()
        token = null
    }
}
