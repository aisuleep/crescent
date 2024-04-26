package org.nekoweb.amycatgirl.revolt.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import org.nekoweb.amycatgirl.revolt.R
import org.nekoweb.amycatgirl.revolt.api.ApiClient
import org.nekoweb.amycatgirl.revolt.models.api.User
import org.nekoweb.amycatgirl.revolt.models.api.channels.Channel
import org.nekoweb.amycatgirl.revolt.models.viewmodels.HomeViewmodel
import org.nekoweb.amycatgirl.revolt.ui.composables.PeopleListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    homeViewmodel: HomeViewmodel,
    navigateToChat: (location: String) -> Unit,
    navigateToDebug: () -> Unit,
    navigateToSettings: () -> Unit
) {

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())


    Scaffold(modifier = Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(title = {
                Text(
                    stringResource(R.string.app_directmessages_header),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }, actions = {
                IconButton(onClick = { navigateToDebug() }) {
                    Icon(
                        painterResource(R.drawable.material_symbols_adb),
                        contentDescription = "Open Debug login screen"
                    )
                }
                IconButton(onClick = { navigateToSettings() }) {
                    Icon(
                        painterResource(R.drawable.material_symbols_settings),
                        contentDescription = null
                    )
                }
            }, scrollBehavior = scrollBehavior, windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /*TODO*/ }) {
                Icon(
                    painterResource(R.drawable.material_symbols_add),
                    contentDescription = "More options"
                )
            }
        }) { innerPadding ->
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(innerPadding), contentPadding = innerPadding
        ) {
            items(homeViewmodel.channels) { channel ->
                when (channel) {
                    is Channel.DirectMessage -> {
                        val author = remember {
                            ApiClient.cache.filterIsInstance<User>().find {
                                ApiClient.currentSession?.userId != it.id && channel.recipients.contains(
                                    it.id
                                )
                            }
                        }
                        Log.d("Cache", "Found author: $author in ${ApiClient.cache}")
                        if (author != null && channel.active && author.flags != 2) {
                            PeopleListItem(
                                user = author,
                                status = author.status,
                                callback = { navigateToChat(author.id) })
                        }
                    }

                    is Channel.Group -> {
                        PeopleListItem(channel = channel, callback = { navigateToChat(channel.id) })
                    }

                    else -> {}
                }
            }
        }
    }
}