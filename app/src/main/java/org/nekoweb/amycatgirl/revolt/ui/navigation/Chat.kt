package org.nekoweb.amycatgirl.revolt.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.nekoweb.amycatgirl.revolt.R
import org.nekoweb.amycatgirl.revolt.api.ApiClient
import org.nekoweb.amycatgirl.revolt.models.api.User
import org.nekoweb.amycatgirl.revolt.models.api.channels.Channel
import org.nekoweb.amycatgirl.revolt.models.api.websocket.PartialMessage
import org.nekoweb.amycatgirl.revolt.models.app.ChatViewmodel
import org.nekoweb.amycatgirl.revolt.ui.composables.ChatBubble
import org.nekoweb.amycatgirl.revolt.ui.composables.CustomTextField
import org.nekoweb.amycatgirl.revolt.ui.composables.ProfileImage
import org.nekoweb.amycatgirl.revolt.ui.theme.RevoltTheme
import org.nekoweb.amycatgirl.revolt.utilities.EventBus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatPage(
    cache: List<User>,
    viewmodel: ChatViewmodel,
    ulid: String = "0000000000000000000000",
    goBack: () -> Unit
) {
    val user = ApiClient.cache.filterIsInstance<User>().find { it.id == ulid }
    var messageValue by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<PartialMessage>() }
    val scope = rememberCoroutineScope()
    val currentChannel = ApiClient.cache.filterIsInstance<Channel>()
        .find { it.id == ulid || (it is Channel.DirectMessage && it.recipients.contains(ulid)) }

    LaunchedEffect(ulid) {
        viewmodel.getMessages(ulid).let {
            messages.addAll(it)
        }
    }

    LaunchedEffect(ulid) {
        EventBus.subscribe<PartialMessage> {
            if (it.channelId == currentChannel?.id) {
                messages.add(0, it)
            }
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileImage(
                            fallback = user?.username ?: "Unknown",
                            url = "${ApiClient.S3_ROOT_URL}avatars/${user?.avatar?.id}?max_side=256",
                            size = 26.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = user?.displayName
                                ?: "${user?.username ?: "Unknown"}#${user?.discriminator ?: "0000"}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Go Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        painterResource(R.drawable.material_symbols_library_add),
                        "",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                CustomTextField(
                    value = messageValue,
                    placeholder = { Text(stringResource(R.string.chat_sendmessage)) },
                    onValueChange = { messageValue = it },
                    singleLine = false,
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth(.8f)
                        .heightIn(0.dp, 100.dp)
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            ApiClient.sendMessage(ulid, messageValue)
                            messageValue = ""
                        }
                    },
                ) {
                    Icon(
                        painterResource(R.drawable.material_symbols_send),
                        "",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = innerPadding,
            reverseLayout = true
        ) {
            items(messages) { message ->
                ChatBubble(
                    message
                )
            }

        }

    }
}

@Preview
@Composable
fun ChatPagePreview() {
    RevoltTheme {
        val viewmodel = viewModel {
            ChatViewmodel()
        }
        val exampleList = List(
            1,
            init = { i -> User(id = i.toString(), username = "meow", discriminator = "000") })
        ChatPage(exampleList, viewmodel, "1") {}
    }
}