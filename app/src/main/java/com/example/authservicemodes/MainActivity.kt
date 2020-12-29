package com.example.authservicemodes

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.huawei.agconnect.auth.*
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var signFacebookBtn: LoginButton
    private lateinit var signTwitterBtn: Button
    private lateinit var signGameBtn: Button
    private lateinit var signGoogleBtn: Button

    private val GOOGLE_REQUEST_SIGNIN = 10000
    private val GOOGLE_PLAY_GAME_REQUEST_SIGNIN = 10001

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var mGoogleGameSignInClient: GoogleSignInClient? = null

    private val HUAWEIID_SIGNIN = 8000
    private val HUAWEIGAME_SIGNIN = 7000
    val PERMISSIONS_REQUEST_STORAGE = 122

    private val TAG = javaClass.simpleName

    private var uId: String? = null

    lateinit var twitterDialog: Dialog
    lateinit var twitter: Twitter
    var accToken: AccessToken? = null

    var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signFacebookBtn = findViewById(R.id.signWithFacebookBtn)
        signTwitterBtn = findViewById(R.id.loginWithTwitterBtn)
        signGameBtn = findViewById(R.id.loginWithGameBtn)
        signGoogleBtn = findViewById(R.id.loginWithGoogleBtn)

        initView()
    }

    companion object {

        fun launch(activity: AppCompatActivity) =
            activity.apply {
                startActivity(Intent(this, MainActivity::class.java))
            }
    }

    private fun initView() {
        callbackManager = CallbackManager.Factory.create()
        signFacebookBtn.setPermissions(listOf("email", "public_profile"))

        signFacebookBtn.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult?> {
            override fun onSuccess(loginResult: LoginResult?) {

                val credential =
                    FacebookAuthProvider.credentialWithToken(loginResult?.accessToken?.token)

                AGConnectAuth.getInstance().signIn(credential)
                    .addOnSuccessListener { signInResult -> // onSuccess
                        val user = signInResult.user
                        Toast.makeText(this@MainActivity, user.uid, Toast.LENGTH_LONG).show()
                        uId = user.uid

                    }
                    .addOnFailureListener {
                        // onFail
                    }
            }

            override fun onCancel() {}
            override fun onError(error: FacebookException) {}
        })


        signTwitterBtn.setOnClickListener {

        }
        signGameBtn.setOnClickListener {
            val authParams =
                HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME).createParams()
            val service = HuaweiIdAuthManager.getService(this, authParams)
            startActivityForResult(service.signInIntent, HUAWEIGAME_SIGNIN)
        }
        signGoogleBtn.setOnClickListener {

        }
    }

    private fun getRequestToken() {
        GlobalScope.launch(Dispatchers.Default) {
            val builder = ConfigurationBuilder()
                .setDebugEnabled(true)
                .setOAuthConsumerKey("")
                .setOAuthConsumerSecret("")
                .setIncludeEmailEnabled(true)
            val config = builder.build()
            val factory = TwitterFactory(config)
            twitter = factory.instance
            try {
                val requestToken = twitter.oAuthRequestToken
                withContext(Dispatchers.Main) {
                    setupTwitterWebviewDialog(requestToken.authorizationURL)
                }
            } catch (e: IllegalStateException) {
                Log.e("ERROR: ", e.toString())
            }
        }
    }

    private fun initGoogleSign() {
        val gsoBuilder: GoogleSignInOptions.Builder = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
        gsoBuilder.requestIdToken("335861146867-kih14ci4fgf1tu0th4k9kj4ujna388cd.apps.googleusercontent.com")
        val signInOptions = gsoBuilder.requestProfile().build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, signInOptions)
    }

    private fun initGoogleGameSign() {
        val gsoBuilder: GoogleSignInOptions.Builder = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN
        )
        gsoBuilder.requestIdToken("335861146867-kih14ci4fgf1tu0th4k9kj4ujna388cd.apps.googleusercontent.com")
        val signInOptions = gsoBuilder.requestProfile().build()

        mGoogleGameSignInClient = GoogleSignIn.getClient(this, signInOptions)
    }

    private fun startLoginWithGoogle(activity: Activity) {
        val lastSignedInAccount: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        if (lastSignedInAccount != null) {

            mGoogleSignInClient!!.signOut().addOnCompleteListener {

                if (it.isSuccessful) {
                    signInWithGoogle(activity)
                }
            }

        } else {
            signInWithGoogle(activity)
        }
    }

    private fun startLoginWithGooglePlayGame(activity: Activity) {
        val lastSignedInAccount: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        if (lastSignedInAccount != null) {

            mGoogleGameSignInClient!!.signOut().addOnCompleteListener {

                if (it.isSuccessful) {
                    signInWithGooglePlayGame(activity)
                }
            }

        } else {
            signInWithGooglePlayGame(activity)
        }
    }

    private fun signInWithGoogle(activity: Activity) {
        val signInIntent = mGoogleSignInClient!!.signInIntent
        activity.startActivityForResult(signInIntent, GOOGLE_REQUEST_SIGNIN)
    }

    private fun signInWithGooglePlayGame(activity: Activity) {
        val signInIntent = mGoogleGameSignInClient!!.signInIntent
        activity.startActivityForResult(signInIntent, GOOGLE_PLAY_GAME_REQUEST_SIGNIN)
    }

    private fun handleGoogleSignInResult(data: Intent) {
        try {
            val accountTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>? =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = accountTask?.getResult(ApiException::class.java)
            if (account != null) {

                val idToken = account.idToken
                if (idToken != null) {
                    loginWithGoogle(idToken)
                }

            }
        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    private fun handleGoogleGameSignInResult(data: Intent) {
        try {
            val accountTask: com.google.android.gms.tasks.Task<GoogleSignInAccount>? =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = accountTask?.getResult(ApiException::class.java)
            if (account != null) {

                val serverAuthCode = account.serverAuthCode
                if (serverAuthCode != null) {
                    loginWithGoogleGame(serverAuthCode)
                }

            }
        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    private fun loginWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.credentialWithToken(idToken)
        AGConnectAuth.getInstance().signIn(credential)
            .addOnSuccessListener { signInResult -> // onSuccess

                val user = signInResult.user
                Toast.makeText(this, user.uid, Toast.LENGTH_LONG).show()
                uId = user.uid

            }
            .addOnFailureListener {
                // onFail
            }
    }

    private fun loginWithGoogleGame(serverAuthCode: String) {
        val credential = GoogleGameAuthProvider.credentialWithToken(serverAuthCode)
        AGConnectAuth.getInstance().signIn(credential)
            .addOnSuccessListener { signInResult -> // onSuccess

                val user = signInResult.user
                Toast.makeText(this, user.uid, Toast.LENGTH_LONG).show()
                uId = user.uid

            }
            .addOnFailureListener {
                // onFail
            }
    }

    // Show twitter login page in a dialog
    @SuppressLint("SetJavaScriptEnabled")
    fun setupTwitterWebviewDialog(url: String) {
        twitterDialog = Dialog(this)
        val webView = WebView(this)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = TwitterWebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        twitterDialog.setContentView(webView)
        twitterDialog.show()
    }

    // A client to know about WebView navigations
    // For API 21 and above
    @Suppress("OverridingDeprecatedMember")
    inner class TwitterWebViewClient : WebViewClient() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request?.url.toString().startsWith("")) {
                Log.d("Authorization URL: ", request?.url.toString())
                handleUrl(request?.url.toString())

                // Close the dialog after getting the oauth_verifier
                if (request?.url.toString().contains("")) {
                    twitterDialog.dismiss()
                }
                return true
            }
            return false
        }

        // For API 19 and below
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith("")) {
                Log.d("Authorization URL: ", url)
                handleUrl(url)

                // Close the dialog after getting the oauth_verifier
                if (url.contains("")) {
                    twitterDialog.dismiss()
                }
                return true
            }
            return false
        }

        // Get the oauth_verifier
        private fun handleUrl(url: String) {
            val uri = Uri.parse(url)
            val oauthVerifier = uri.getQueryParameter("oauth_verifier") ?: ""
            GlobalScope.launch(Dispatchers.Main) {
                accToken =
                    withContext(Dispatchers.IO) { twitter.getOAuthAccessToken(oauthVerifier) }

                val usr = withContext(Dispatchers.IO) { twitter.verifyCredentials() }

                registerWithTwitter()
            }
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val accessToken = sharedPref.getString("oauth_token", "")
        val accessTokenSecret = sharedPref.getString("oauth_token_secret", "")

        val builder = ConfigurationBuilder()
        val config = builder.build()
        builder.setOAuthConsumerKey("")
            .setOAuthConsumerSecret("")
            .setOAuthAccessToken(accessToken)
            .setOAuthAccessTokenSecret(accessTokenSecret)
        val factory = TwitterFactory(config)
        val twitter = factory.instance

        try {
            withContext(Dispatchers.IO) { twitter.verifyCredentials() }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun registerWithTwitter() {

        val token = accToken?.token
        val secret = accToken?.tokenSecret

        val credential = TwitterAuthProvider.credentialWithToken(token, secret)
        AGConnectAuth.getInstance().signIn(credential)
            .addOnSuccessListener { signInResult -> // onSuccess

                val user = signInResult.user
                Toast.makeText(this, user.uid, Toast.LENGTH_LONG).show()
                uId = user.uid

            }
            .addOnFailureListener {
                // onFail
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager?.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_REQUEST_SIGNIN) {
            if (data != null) {
                handleGoogleSignInResult(data)
            }
        } else if (requestCode == GOOGLE_PLAY_GAME_REQUEST_SIGNIN) {
            if (data != null) {
                handleGoogleGameSignInResult(data)
            }
        }
    }
}