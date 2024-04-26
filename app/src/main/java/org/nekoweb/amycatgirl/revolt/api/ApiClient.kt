package org.nekoweb.amycatgirl.revolt.api

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.wss
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.nekoweb.amycatgirl.revolt.models.api.authentication.SessionRequestWithFriendlyName
import org.nekoweb.amycatgirl.revolt.models.api.authentication.SessionResponse
import org.nekoweb.amycatgirl.revolt.models.api.channels.Channel
import org.nekoweb.amycatgirl.revolt.models.api.websocket.AuthenticateEvent
import org.nekoweb.amycatgirl.revolt.models.api.websocket.BaseEvent
import org.nekoweb.amycatgirl.revolt.models.api.websocket.PartialMessage
import org.nekoweb.amycatgirl.revolt.models.api.websocket.UnimplementedEvent
import org.nekoweb.amycatgirl.revolt.utilities.EventBus

object ApiClient {
    var useStaging = false
        set(value) {
            when (value) {
                true -> {
                    API_ROOT_URL = "https://revolt.chat/api/"
                    SOCKET_ROOT_URL = "wss://revolt.chat/events/?format=json&version=1"
                }

                false -> {
                    API_ROOT_URL = "https://api.revolt.chat"
                    SOCKET_ROOT_URL = "wss://ws.revolt.chat?format=json&version=1"

                }
            }
            field = value
        }
    private var SOCKET_ROOT_URL: String = "wss://ws.revolt.chat?format=json&version=1"
    private var API_ROOT_URL: String = "https://api.revolt.chat/"
    const val S3_ROOT_URL: String = "https://autumn.revolt.chat/"

    var currentSession: SessionResponse.Success? = null
    private var websocket: DefaultWebSocketSession? = null
    private val jsonDeserializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            polymorphic(BaseEvent::class) {
                defaultDeserializer { UnimplementedEvent.serializer() }
            }
        }
    }

    var cache = mutableMapOf<String, Any>()

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonDeserializer)
        }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(jsonDeserializer)
        }
    }

    fun connectToWebsocket() {
        CoroutineScope(Dispatchers.IO).launch {
            client.wss(SOCKET_ROOT_URL) {
                websocket = this@wss

                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val event: BaseEvent = receiveDeserialized()
                            Log.d("Socket", "Got Event: $event")
                            EventBus.publish(event)
                        }
                    }
                } catch (exception: Exception) {
                    Log.e("Socket", "$exception")
                }
            }
        }
    }

    suspend fun getDirectMessages(): List<Channel> {
        val res = client.get("$API_ROOT_URL/users/dms") {
            headers {
                append("X-Session-Token", currentSession?.userToken ?: "")
            }

            accept(ContentType.Application.Json)
        }.body<List<Channel>>()


        res.forEach {
            cache[it.id] = it
        }

        Log.d("Client", "Direct Messages: $res")
        Log.d("Cache", "Cache size: ${cache.size}")
        return res
    }

    suspend fun getSpecificMessageFromChannel(
        channel: Channel, messageId: String
    ): PartialMessage? {
        return try {
            val res = client.get("$API_ROOT_URL/channel/${channel.id}/messages/${messageId}") {
                headers {
                    append("X-Session-Token", currentSession?.userToken ?: "")
                }

                accept(ContentType.Application.Json)
            }.body<PartialMessage>()

            cache[res.id!!] = res

            Log.d("Cache", "Cache size: ${cache.size}")
            return res
        } catch (e: Exception) {
            Log.e("Client", "Fuck, $e")
            null
        }
    }

    suspend fun getChannelMessages(channelId: String): List<PartialMessage> {
        val channel = cache[channelId] as Channel
        Log.d("Cache", "Found Channel: $channel")
        val url = "${API_ROOT_URL}channels/${channel.id}/messages?limit=30"
        val res = client.get(url) {
            headers {
                append("X-Session-Token", currentSession?.userToken ?: "")
            }

            accept(ContentType.Application.Json)
        }.body<List<PartialMessage>>()


        Log.d("Cache", "Cache size: ${cache.size}")

        return res
    }

    suspend fun sendMessage(location: String, message: String) {
        val channel = cache[location] as Channel

        val url = "${API_ROOT_URL}channels/${channel.id}/messages"
        client.post(url) {
            headers {
                append("X-Session-Token", currentSession?.userToken ?: "")
            }

            contentType(ContentType.Application.Json)
            setBody(PartialMessage(content = message))
        }
    }

    suspend fun loginWithPassword(email: String, password: String): SessionResponse {
        val response = client.post("$API_ROOT_URL/auth/session/login") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)

            setBody(SessionRequestWithFriendlyName(email, password))
        }.body<SessionResponse>()

        if (response is SessionResponse.Success) {
            Log.d("Client", "Got response from API: $response")
            currentSession = response
            websocket?.send(
                jsonDeserializer.encodeToString(
                    AuthenticateEvent(
                        "Authenticate",
                        response.userToken
                    )
                )
            )
        } else {
            Log.d("Client", "TODO: Implement 2FA")
        }

        return response
    }

    private suspend fun removeExistingSession(sessionResponse: SessionResponse.Success) {
        client.delete("${API_ROOT_URL}auth/session/${sessionResponse.id}") {
            headers { append("X-Session-Token", currentSession?.userToken ?: "") }
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun dropSession(): Boolean {
        return try {
            removeExistingSession(currentSession!!)
            currentSession = null

            true
        } catch (error: Exception) {
            Log.e("Client", "Error whilst dropping session: $error")

            false
        }
    }
}
