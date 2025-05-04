package com.decade.practice.composable

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.decade.practice.R
import com.decade.practice.activity.ConversationRoute
import com.decade.practice.model.domain.Chat
import com.decade.practice.model.domain.ChatIdentifier
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.presentation.Dialog
import com.decade.practice.theme.ApplicationTheme
import com.decade.practice.utils.ONE_HOUR_SECONDS
import com.decade.practice.utils.ONE_MINUTE_SECONDS
import java.time.Instant

@Composable
fun Online(navController: NavController, dialog: Dialog) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
      val partner = dialog.conversation.partner
      val owner = LocalUser.current
      val chat = Chat(ChatIdentifier.from(owner, partner), owner)
      val conversation = Conversation(chat, partner, owner)
      Box(modifier = Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
            navController.navigate(ConversationRoute(conversation))
      }) {
            val diff = Instant.now().epochSecond - dialog.onlineAt
            AsyncImage(
                  model = partner.avatar.uri,
                  placeholder = painterResource(id = R.drawable.avatar_placeholder),
                  contentDescription = "Partner Avatar",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
            )
            if (diff < ONE_HOUR_SECONDS) {
                  Badge(
                        containerColor = Color(0xFF70c1b3),
                        contentColor = Color.Black,
                        modifier = Modifier
                              .offset(y = 45.dp, x = 45.dp)
                              .border(
                                    2.dp,
                                    color = MaterialTheme.colorScheme.background,
                                    shape = CircleShape
                              )
                              .padding(2.dp)
                  ) {
                        if (diff >= ONE_MINUTE_SECONDS)
                              Text(text = ((diff + ONE_MINUTE_SECONDS - 1) / ONE_MINUTE_SECONDS).toString() + "m")
                  }
            }
      }
      Text(
            text = partner.name, style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Medium
            )
      )
}

@Composable
fun OnlineList(
      modifier: Modifier = Modifier,
      navController: NavController = rememberNavController(),
      dialogList: List<Dialog>,
) {
      val lazyListState = rememberLazyListState()
      LazyRow(
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = modifier.fillMaxWidth()
      ) {
            items(items = dialogList, key = { it.conversation.partner.id }) { dialog ->
                  Online(navController, dialog)
            }
      }
}

@Preview(showBackground = true, device = "id:pixel")
@Composable
fun OnlinePreview() {
      ApplicationTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                  OnlineList(dialogList = List(10) {
                        mockDialog()
                  })
            }
      }
}
