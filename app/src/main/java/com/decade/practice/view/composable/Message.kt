package com.decade.practice.view.composable

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.decade.practice.R
import com.decade.practice.model.IconEvent
import com.decade.practice.model.ImageEvent
import com.decade.practice.model.TextEvent
import com.decade.practice.model.User
import com.decade.practice.model.placeHolder
import com.decade.practice.utils.formatTime
import com.decade.practice.view.theme.ApplicationTheme
import com.decade.practice.view.viewmodel.Message
import com.decade.practice.view.viewmodel.OwnerMessage
import com.decade.practice.view.viewmodel.PartnerMessage
import com.decade.practice.view.viewmodel.Position
import com.decade.practice.view.viewmodel.RollInFormater
import com.decade.practice.view.viewmodel.SendState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


val LocalConversation = compositionLocalOf { mockConversation() }

@Composable
fun LazyItemScope.Message(message: Message) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
        .padding(vertical = 0.1.dp)
        .fillMaxWidth()
        .animateItem(),
) {
    var displayInfo by remember { mutableStateOf(false) }
    val partner = LocalConversation.current.partner
    if (message.fixedDisplayTime || displayInfo) {
        Text(
            text = formatTime(message.receiveTime),
            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = Modifier.padding(vertical = 3.dp)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                displayInfo = !displayInfo
            }) {
        if (partner.id == message.sender) {
            LeftMessage(message as PartnerMessage)
        } else {
            RightMessage(message as OwnerMessage)
        }
    }
    if (message.sender != partner.id) {
        val sendState = (message as OwnerMessage).sendState
        if (sendState != SendState.Sent)
            return@Column
        if (displayInfo || message.isLastSent)
            Text(
                sendState.toString(),
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .align(alignment = Alignment.End)
            )
    }
}

@Composable
fun LeftMessage(message: PartnerMessage) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
    if (message.position == Position.Bottom || message.position == Position.Single) {
        val partner = LocalConversation.current.partner
        AsyncImage(
            model = partner.avatar.uri,
            placeholder = painterResource(id = R.drawable.avatar_placeholder),
            contentDescription = "Partner Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
        )
    } else {
        Spacer(modifier = Modifier.size(30.dp))
    }
    if (message.iconEvent != null) {
        IconMessage(message.iconEvent)
    } else {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = when (message.position) {
                            Position.Single, Position.Top -> 20.dp
                            else -> 3.dp
                        },
                        topEnd = 20.dp,
                        bottomEnd = 20.dp,
                        bottomStart = when (message.position) {
                            Position.Single, Position.Bottom -> 20.dp
                            else -> 3.dp
                        }
                    )
                )

        ) {
            if (message.textEvent != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    TextMessage(message.textEvent)
                }
            }
            if (message.imageEvent != null) {
                ImageMessage(message.imageEvent)

            }
        }
    }

}

@Composable
fun dotOffset(initialDelay: Long) = produceState(initialValue = 0.dp) {
    delay(initialDelay)
    while (true) {
        delay(1000)
        value = (-5).dp
        delay(250)
        value = 0.dp
    }
}

@Composable
fun LazyItemScope.TypeMessage() = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .padding(top = 40.dp)
        .animateItem()
) {
    val partner = LocalConversation.current.partner
    AsyncImage(
        model = partner.avatar.uri,
        placeholder = painterResource(id = R.drawable.avatar_placeholder),
        contentDescription = "Partner Avatar",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .padding(end = 5.dp)
            .size(30.dp)
            .clip(CircleShape)
    )

    val actualOffset1 by dotOffset(initialDelay = 0)
    val actualOffset2 by dotOffset(initialDelay = 250)
    val actualOffset3 by dotOffset(initialDelay = 500)

    val offset1 by animateDpAsState(targetValue = actualOffset1, animationSpec = tween(250))
    val offset2 by animateDpAsState(targetValue = actualOffset2, animationSpec = tween(250))
    val offset3 by animateDpAsState(targetValue = actualOffset3, animationSpec = tween(250))

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                    bottomStart = 20.dp
                )
            )

    ) {
        Row(
            modifier = Modifier
                .background(color = Color(0, 0, 0, 30))
                .padding(vertical = 5.dp, horizontal = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Canvas(
                modifier = Modifier
                    .height(25.dp)
                    .width(8.dp)
                    .offset(y = offset1)
            ) {
                drawCircle(
                    color = Color(0, 0, 0, 60),
                    radius = 3.dp.toPx(),
                )
            }

            Canvas(
                modifier = Modifier
                    .height(25.dp)
                    .width(8.dp)
                    .offset(y = offset2)
            ) {
                drawCircle(
                    color = Color(0, 0, 0, 60),
                    radius = 3.dp.toPx(),
                )
            }
            Canvas(
                modifier = Modifier
                    .height(25.dp)
                    .width(8.dp)
                    .offset(y = offset3)
            ) {
                drawCircle(
                    color = Color(0, 0, 0, 60),
                    radius = 3.dp.toPx(),
                )
            }
        }
    }
}


@Composable
fun RightMessage(message: OwnerMessage) = Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.CenterEnd,
) {
    if (message.iconEvent != null) {
        IconMessage(message.iconEvent)
    } else {

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = when (message.position) {
                            Position.Single, Position.Top -> 20.dp
                            else -> 3.dp
                        },
                        bottomEnd = when (message.position) {
                            Position.Single, Position.Bottom -> 20.dp
                            else -> 3.dp
                        },
                        bottomStart = 20.dp
                    )
                )
        ) {
            if (message.textEvent != null) {
                Surface(color = MaterialTheme.colorScheme.primary) {
                    TextMessage(message.textEvent)
                }
            }
            if (message.imageEvent != null) {
                ImageMessage(message.imageEvent)

            }
        }

    }
}

@Composable
fun TextMessage(textEvent: TextEvent) {
    val content = textEvent.content
    Text(
        text = content,
        modifier = Modifier
            .padding(vertical = 5.dp, horizontal = 15.dp)
    )
}

@Composable
fun IconMessage(iconEvent: IconEvent) {
    Icon(
        imageVector = ImageVector.vectorResource(id = iconEvent.resourceId),
        contentDescription = "Like",
        modifier = Modifier
            .padding(
                vertical = 10.dp,
                horizontal = 15.dp
            )
            .size(30.dp),
        tint = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ImageMessage(image: ImageEvent) {

    val width = image.width
    val height = image.height
    val density = LocalDensity.current
    val widthDp = with(density) { width.toDp() }
    val heightDp = with(density) { height.toDp() }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    AsyncImage(
        model = image.uri,
        placeholder = BitmapPainter(placeHolder(width, height).asImageBitmap()),
        contentDescription = "Image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .heightIn(min = 200.dp, max = 250.dp)
            .widthIn(min = 200.dp, max = (screenWidth * 7 / 10))
            .size(width = widthDp, height = heightDp)

    )
}

@Composable
fun JumpToBottom(onClick: () -> Unit, modifier: Modifier) {
    Button(onClick = onClick, shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Icon(imageVector = ImageVector.vectorResource(id = R.drawable.arrow_down), contentDescription = "Scroll Down")
        Text(text = "Jump To Bottom")
    }
}

@Composable
fun MessageList(
    modifier: Modifier = Modifier,
    messageList: List<Message>,
    listState: LazyListState = rememberLazyListState(),
    loading: Boolean = true,
    typing: Boolean = true,
    endListAction: () -> Unit = {}
) = Box(modifier = modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {

    val jumpButtonDisplayed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >= 5
        }
    }
    val nearEnd by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            visibleItems.isEmpty() || visibleItems.last().index >= layoutInfo.totalItemsCount - 5 && messageList.isNotEmpty() && messageList.last() != Message
        }
    }
    if (nearEnd) {
        endListAction()
    }
    val partner = LocalConversation.current.partner


    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), reverseLayout = true) {
        if (typing) {
            item {
                TypeMessage()
            }
        }
        items(items = messageList, key = { it.id }) { message ->
            if (partner.id != message.sender && message.isLastSeen) {
                SideSeen(partner)
            }
            Message(message)
        }
        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
    if (jumpButtonDisplayed) {

        val coroutineScope = rememberCoroutineScope()
        JumpToBottom(
            onClick = {
                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            }, modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

    }
}


@Composable
private fun SideSeen(partner: User) = Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
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


@Preview(showBackground = true, device = "id:pixel")
@Composable
fun MessagePreview() {
    val messageList = listOf(
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current, mine = true),
        mockImageMessage(LocalConversation.current, mine = true),
        mockTextMessage(LocalConversation.current, mine = true).also {
            it.isLastSeen = true
        },
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockIconMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current),
        mockTextMessage(LocalConversation.current, mine = true),
        mockTextMessage(LocalConversation.current, mine = true),
        mockIconMessage(LocalConversation.current, mine = true),
        mockTextMessage(LocalConversation.current, mine = true).also {
            (it as OwnerMessage).sendState = SendState.Sent
            (it as OwnerMessage).isLastSent = true
        },
        mockTextMessage(LocalConversation.current, mine = true),

        ).asReversed()
    LaunchedEffect(Unit) {

        delay(3000)

        val formater = RollInFormater()
        formater.format(messageList)
    }
    ApplicationTheme {
        var typing by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1500)
                typing = !typing
            }
        }
        MessageList(messageList = messageList, typing = typing)
    }
}
