package com.example.rocketreserver

import android.content.Context
import android.os.Looper
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

private var instance: ApolloClient? = null

fun apolloClient(context: Context): ApolloClient {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "only the main thread can get instance of apolloclient"
    }

    if (instance != null) {
        return instance!!
    }

    val okHttpClient = okHttpInstance(context)

    instance = ApolloClient.builder()
        .serverUrl("https://apollo-fullstack-tutorial.herokuapp.com")
        .subscriptionTransportFactory(WebSocketSubscriptionTransport.Factory("wss://apollo-fullstack-tutorial.herokuapp.com/graphql",okHttpClient))
        .okHttpClient(okHttpClient)
        .build()

    return instance!!
}

fun okHttpInstance(context: Context): OkHttpClient {
    return OkHttpClient.Builder().addInterceptor(AuthenticationInterceptor(context)).build()
}


private class AuthenticationInterceptor(val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            chain.request().newBuilder()
                .addHeader("Authorization", User.getToken(context) ?: "")
                .build()
        return chain.proceed(request)
    }

}
