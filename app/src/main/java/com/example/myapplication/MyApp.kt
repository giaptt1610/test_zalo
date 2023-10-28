package com.example.myapplication

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.zing.zalo.zalosdk.oauth.ZaloSDKApplication
import java.security.MessageDigest
import android.util.Log


class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        ZaloSDKApplication.wrap(this)
        try {

            val a = getApplicationHashKey(this)
            Log.i("giang", a.toString())
        } catch (e: java.lang.Exception) {
            Log.i("giang error", "getApplicationHashKey(context)")
            throw RuntimeException(e)
        }
    }

    @Throws(Exception::class)
    fun getApplicationHashKey(ctx: Context): String? {
        val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
        for (signature in info.signatures) {
            val md = MessageDigest.getInstance("SHA")
            md.update(signature.toByteArray())
            val sig: String = Base64.encodeToString(md.digest(), Base64.DEFAULT).trim()
            if (sig.trim { it <= ' ' }.length > 0) {
                return sig
            }
        }

        return null
    }
}