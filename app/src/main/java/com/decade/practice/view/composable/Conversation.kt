package com.decade.practice.view.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.decade.practice.R
import com.decade.practice.model.Conversation
import com.decade.practice.model.User
import com.decade.practice.utils.ONE_HOUR_SECONDS
import com.decade.practice.utils.ONE_MINUTE_SECONDS
import com.decade.practice.view.activity.ConversationRoute
import com.decade.practice.view.theme.ApplicationTheme
import com.decade.practice.view.viewmodel.Dialog
import com.decade.practice.view.viewmodel.Message
import com.decade.practice.view.viewmodel.announcementOf
import java.time.Instant

val LocalUser = compositionLocalOf { mockUser }

@Composable
fun Conversation(
    conversation: Conversation,
    newest: Message,
    onlineAt: Long,
    navController: NavController
) {
    val partner = conversation.partner
    val mine = conversation.owner.id == newest.sender
    val seen = newest.seen
    val diffOnline = Instant.now().epochSecond - onlineAt

    val messageText = conversation.announcementOf(newest)
    val nameWeight = if (!seen && !mine) FontWeight.Bold else FontWeight.Medium
    val contentWeight = if (!seen && !mine) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier
            .clickable {
                navController.navigate(ConversationRoute(conversation))
            }
            .padding(vertical = 10.dp, horizontal = 5.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box {
            SideAvatar(partner = partner)
            if (diffOnline < ONE_HOUR_SECONDS)
                Badge(
                    containerColor = Color(0xFF70c1b3),
                    contentColor = Color.Black,
                    modifier = Modifier
                        .offset(y = 40.dp, x = 40.dp)
                        .border(
                            2.dp,
                            color = MaterialTheme.colorScheme.background,
                            shape = CircleShape
                        )
                        .padding(2.dp)
                ) {
                    if (diffOnline >= ONE_MINUTE_SECONDS)
                        Text(text = ((diffOnline + ONE_MINUTE_SECONDS - 1) / ONE_MINUTE_SECONDS).toString() + "m")
                }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = partner.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = nameWeight
                )
            )
            Text(
                text = messageText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = contentWeight
                )
            )
        }
        if (seen && mine) {
            SideSeen(partner = partner)
        }
    }
}

@Composable
private fun SideAvatar(partner: User) {
    AsyncImage(
        model = partner.avatar.uri,
        placeholder = painterResource(id = R.drawable.avatar_placeholder),
        contentDescription = "avatar",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(55.dp)
            .clip(CircleShape)
    )
}


@Composable
private fun SideSeen(partner: User) {
    if (LocalInspectionMode.current) {
        Image(
            bitmap = ImageBitmap.imageResource(id = R.drawable.nami),
            contentDescription = "seen",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .size(15.dp)
                .clip(CircleShape)
        )
    } else {
        AsyncImage(
            model = partner.avatar.uri,
            placeholder = painterResource(id = R.drawable.avatar_placeholder),
            contentDescription = "seen",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .size(15.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun ConversationList(
    navController: NavController = rememberNavController(),
    conversationList: List<Dialog> = List(12) {
        val conversation = mockConversation()
        object : Dialog {
            override val conversation: Conversation
                get() = conversation
            override val onlineAt: Long
                get() = Instant.now().epochSecond - (0..1).random() * ONE_MINUTE_SECONDS
            override val newest: Message
                get() = mockTextMessage(conversation).also {
                    it.seenAt = System.currentTimeMillis()
                }

            override fun goOnline() {}

            override fun goOffline() {
            }
        }
    },
    loading: Boolean = true,
    endListAction: () -> Unit = {}
) {
    val lazyListState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            visibleItems.isNotEmpty() && visibleItems.last().index >= layoutInfo.totalItemsCount - 5 && conversationList.isNotEmpty()
        }
    }
    if (nearEnd) {
        endListAction()
    }
    LazyColumn(state = lazyListState) {
        items(items = conversationList, key = { it.conversation.identifier.hashCode() }) { dialog ->
            DisposableEffect(dialog) {
                dialog.goOnline()
                onDispose {
                    dialog.goOffline()
                }
            }
            if (dialog.conversation.partner.role == "ROLE_USER") {
                Conversation(dialog.conversation, dialog.newest, dialog.onlineAt, navController)
            } else {
                //TODO
            }
        }

        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "id:pixel")
@Composable
fun ConversationPreview() {
    ApplicationTheme {
        ConversationList()
    }
}


