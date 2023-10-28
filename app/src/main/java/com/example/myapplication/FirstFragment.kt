package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentFirstBinding
import com.zing.zalo.zalosdk.oauth.LoginVia
import com.zing.zalo.zalosdk.oauth.OAuthCompleteListener
import com.zing.zalo.zalosdk.oauth.OauthResponse
import com.zing.zalo.zalosdk.oauth.ZaloOpenAPICallback
import com.zing.zalo.zalosdk.oauth.ZaloSDK
import com.zing.zalo.zalosdk.oauth.model.ErrorResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.loginZaloButton.setOnClickListener {
            val extInfo = JSONObject()
//            val codeVerifier = "IE2G1SO-_RK7UnFJMZ_mijk7HKPnweC932ovBz-qrd4a3kwLAdrbWHr1zw"
//            val codeChallenge = "H1wjSjN7nxtPzE-kEKkIqAxDdE28hIL57i5rLw0mT0I"

            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)

            val listener: OAuthCompleteListener = object : OAuthCompleteListener() {
                override fun onGetOAuthComplete(response: OauthResponse) {
                    val oauthCode = response.oauthCode
                    Log.i("giang", "onGetOAuthComplete, oauthCode:${oauthCode}")
                    ZaloSDK.Instance.getAccessTokenByOAuthCode(activity, oauthCode, codeVerifier, withZOGraphCallBack())
                }

                override fun onAuthenError(errorResponse: ErrorResponse?) {
                    val error: MutableMap<String, Any?> = HashMap()
                    error["errorCode"] = errorResponse?.errorCode
                    error["errorMessage"] = errorResponse?.errorMsg
                    error["errorDescription"] = errorResponse?.errorDescription
                    error["errorReason"] = errorResponse?.errorReason
                    val data: Map<String, Any> = HashMap()
                    val map: MutableMap<String, Any?> = HashMap()
                    map["isSuccess"] = false
                    map["error"] = error
                    map["data"] = data
//                    result.success(map)

                    Log.i("giang", "map:${map}")
                }
            }
            ZaloSDK.Instance.authenticateZaloWithAuthenType(this.activity, LoginVia.APP_OR_WEB,codeChallenge, extInfo, listener )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Throws(Exception::class)
    private fun withZOGraphCallBack(): ZaloOpenAPICallback {
        return ZaloOpenAPICallback { response: JSONObject? ->
            try {
                if (response == null) {
                    val error: MutableMap<String, Any?> = HashMap()
                    error["errorCode"] = -9999
                    error["errorMessage"] = "Other error: cannot get response"
                    val map: MutableMap<String, Any?> = HashMap()
                    map["isSuccess"] = false
                    map["error"] = error
//                    result.success(map)

                    Log.i("giang", "success1, ${map}")
                } else {
                    val data: Map<String, Any?> = AppHelper.jsonToMap(response)
                    val errorCode = data["error"] as Int
                    val isSuccess = errorCode == 0
                    if (isSuccess) {
                        val map: MutableMap<String, Any?> = HashMap()
                        map["isSuccess"] = true
                        val newData = data.filterKeys { key -> key != "error" && key != "message" && key != "extCode" }
                        map["data"] = newData
//                        result.success(map)

                        Log.i("giang", "success2, ${map}")
                    } else {
                        val map: MutableMap<String, Any?> = HashMap()

                        val error: MutableMap<String, Any?> = HashMap()
                        error["errorCode"] = errorCode
                        error["errorMessage"] = data["message"]
                        error["errorExtCode"] = data["extCode"]

                        map["isSuccess"] = errorCode == 0
                        map["error"] = error
//                        result.success(map)
                        Log.i("giang", "success3, ${map}")
                    }
                }
            } catch (e: Exception) {
                val error: MutableMap<String, Any?> = HashMap()
                error["errorCode"] = -9997
                error["errorMessage"] = e.message
                val map: MutableMap<String, Any?> = HashMap()
                map["isSuccess"] = false
                map["error"] = error
//                result.success(map)
                Log.i("giang", "error, ${map}")
            }
        }
    }

    private fun trimSpecialChars(str: String): String {
        return str.trim().replace("+", "-")
            .replace("/", "_").replace("=", "").replace("\\n", "")
    }

    fun generateCodeVerifier(): String? {
        return try {
            val secureRandom = SecureRandom()
            val codeVerifier = ByteArray(32)
            secureRandom.nextBytes(codeVerifier)
            val encoded = Base64.encodeToString(codeVerifier, Base64.DEFAULT)
            trimSpecialChars(encoded)
        } catch (e: Exception) {
            null
        }
    }

    fun generateCodeChallenge(verifyCode: String?): String? {
        return try {
            val bytes = verifyCode?.toByteArray(charset("US-ASCII"))
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(bytes!!, 0, bytes.size)
            val digest = messageDigest.digest()
            val encoded = Base64.encodeToString(digest, Base64.DEFAULT)
            trimSpecialChars(encoded)
        } catch (e: Exception) {
            null
        }
    }

}

private object AppHelper {
    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    fun getHashKey(@NonNull context: Context): String {
        try {
            val packageManager = context.packageManager
            val info = packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                return Base64.encodeToString(md.digest(), Base64.DEFAULT)
            }
            return ""
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return ""
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return ""
        }
    }

    @Throws(JSONException::class)
    fun jsonToMap(json: JSONObject): Map<String, Any?> {
        var map: Map<String, Any?> = HashMap()
        if (json !== JSONObject.NULL) {
            map = fromMap(json)
        }
        return map
    }

    @Throws(JSONException::class)
    private fun fromMap(jsonObj: JSONObject): Map<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            var value = jsonObj[key]
            if (value is JSONArray) {
                value = fromList(value)
            } else if (value is JSONObject) {
                value = fromMap(value)
            }
            map[key] = value
        }
        return map
    }

    @Throws(JSONException::class)
    private fun fromList(array: JSONArray): List<Any> {
        val list: MutableList<Any> = ArrayList()
        for (i in 0 until array.length()) {
            var value = array[i]
            if (value is JSONArray) {
                value = fromList(value)
            } else if (value is JSONObject) {
                value = fromMap(value)
            }
            list.add(value)
        }
        return list
    }
}

