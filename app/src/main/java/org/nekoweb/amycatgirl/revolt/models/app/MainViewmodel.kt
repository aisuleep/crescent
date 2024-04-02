package org.nekoweb.amycatgirl.revolt.models.app

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nekoweb.amycatgirl.revolt.api.ApiClient
import org.nekoweb.amycatgirl.revolt.models.websocket.PartialMessageEvent
import org.nekoweb.amycatgirl.revolt.models.websocket.ReadyEvent
import org.nekoweb.amycatgirl.revolt.utilities.EventBus

class MainViewmodel : ViewModel() {
    private val client = ApiClient()
    private val listener = GlobalEventListener()
    var messageList = mutableStateListOf<PartialMessageEvent>()
        private set

    init {
        viewModelScope.launch {
            client.connectToWebsocket(listener)
            EventBus.subscribe<ReadyEvent> { ev -> println("Got Ready Event! $ev") }
        }

        viewModelScope.launch {
            EventBus.subscribe<PartialMessageEvent> { ev ->
                println("Got Message Event! $ev")
                messageList.add(ev)
            }
        }
    }
}