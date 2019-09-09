package com.groundzero.networkcallcaching

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

class MainActivity : AppCompatActivity() {

    /**
     * Specifying the cache size
     */
    private val cacheSize = (5 * 1024 * 1024).toLong()

    /**
     * Creating Cache variable for our client
     */
    private val myCache: Cache by lazy {
        Cache(applicationContext.cacheDir, cacheSize)
    }

    /**
     * Adding interception to our client to monitor response
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        this.level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Adding interceptor to manage cache depending on internet connection
     */
    private val controlInterceptor: Interceptor = Interceptor { chain ->
        var request = chain.request()
        request = if (NetworkUtils.hasNetwork(baseContext)!!) {
            request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
        } else {
            request.newBuilder().header(
                "Cache-Control",
                "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7
            ).build()
        }
        chain.proceed(request)
    }

    /**
     * Creating client and adding 2 interceptors
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(myCache)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(controlInterceptor)
            .build()
    }

    /**
     * Creating retrofit instance that takes for an input a
     * base url, gson for data serialization and addint previous
     * created okHttpClient to it
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://ron-swanson-quotes.herokuapp.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    /**
     * Setting our GET request that will be sent to server
     */
    private interface JokesApi {
        @GET("v2/quotes/")
        fun getRandomJoke(): Call<ArrayList<String>>
    }

    /**
     * Creating an instance of previous API
     */
    private val api: JokesApi by lazy {
        retrofit.create(JokesApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getQuote()
        new_quote_button.setOnClickListener { getQuote() }
    }

    /**
     * Making a call request to a server
     */
    private fun getQuote() {
        api.getRandomJoke().enqueue(object : Callback<ArrayList<String>> {
            override fun onResponse(
                call: Call<ArrayList<String>>,
                response: Response<ArrayList<String>>
            ) {
                if (response.isSuccessful)
                    quote_text.text = response.body()!![0]
            }

            override fun onFailure(call: Call<ArrayList<String>>, t: Throwable) {
                quote_text.text = getString(R.string.error_fetching_data)
            }
        })
    }
}