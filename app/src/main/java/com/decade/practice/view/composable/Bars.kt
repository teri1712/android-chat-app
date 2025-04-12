package com.decade.practice.view.composable

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.decade.practice.R
import com.decade.practice.model.Online
import com.decade.practice.utils.ONE_HOUR_SECONDS
import com.decade.practice.utils.ONE_MINUTE_SECONDS
import com.decade.practice.view.theme.ApplicationTheme
import java.time.Instant

@Composable
fun RowScope.InputField(value: String, onChange: (String) -> Unit) = Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .height(35.dp)
            .clip(RoundedCornerShape(20.dp))
            .weight(1f)

    ) {
    Box(modifier = Modifier.padding(vertical = 5.dp, horizontal = 15.dp), contentAlignment = Alignment.CenterStart){
        BasicTextField(
            value = value, onValueChange = onChange,
            textStyle = TextStyle(color = LocalContentColor.current),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = "Message", style = MaterialTheme.typography.bodyLarge
                    )
                }
                innerTextField()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

}

@Composable
fun InputButton(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit = {}
) {
    var actualSize by remember {
        mutableStateOf(40.dp)
    }
    LaunchedEffect(text) {
        when (text.length) {
            0 -> {
                actualSize = 40.dp
            }

            else -> {
                actualSize = 0.dp
            }
        }
    }
    val size by animateDpAsState(
        targetValue = actualSize,
        animationSpec = tween(200)
    )
    IconButton(
        onClick = onClick, modifier = Modifier
            .size(size)
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = icon),
            contentDescription = "",
            modifier = Modifier
                .padding(9.dp)
        )
    }
}

@Composable
fun InputBar(
    onSubmitText: (text: String) -> Unit = {},
    onSubmitImage: (uri: Uri) -> Unit = {},
    onSubmitIcon: (resourceId: Int) -> Unit = {},
    onTextChanged: (text: String) -> Unit = {}
) = Row(
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {

        var text by remember { mutableStateOf("") }
        val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                onSubmitImage(uri)
            }
        }
        InputButton(icon = R.drawable.picture, text = text, onClick = {
            launcher.launch("image/*")
        })
        InputField(value = text, onChange = { value ->
            text = value
            onTextChanged(value)
        })

        if (text.isEmpty()) {
            IconButton(onClick = { onSubmitIcon(R.drawable.blue_like_button_icon) }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.blue_like_button_icon),
                    contentDescription = "Like",
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            IconButton(onClick = {
                onSubmitText(text)
                text = ""
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.send),
                    contentDescription = "Like",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}


//@Preview(showBackground = true, device = "id:pixel")
@Composable
fun InputFieldPreview() {
    ApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            InputBar()
        }
    }
}


@Composable
fun ChatInfoBar(navController: NavController, online: Online) =
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .drawBehind {
                val gradHeight = 5.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(0.1f),
                            Color.Transparent
                        ),
                        startY = size.height - gradHeight,
                        endY = size.height
                    ), topLeft = Offset(0f, size.height - gradHeight),
                    size = Size(size.width, gradHeight)
                )
            }
            .padding(bottom = 5.dp)
    ) {
        val partner = LocalConversation.current.partner

        IconButton(onClick = {
            navController.popBackStack()
        }) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.back),
                contentDescription = "Back",
                modifier = Modifier.size(25.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        val diff = Instant.now().epochSecond - online.at
        Box {
            AsyncImage(
                model = partner.avatar.uri,
                placeholder = painterResource(id = R.drawable.avatar_placeholder),
                contentDescription = "Partner avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            if (diff < ONE_HOUR_SECONDS) {
                Badge(
                    containerColor = Color(0xFF70c1b3),
                    contentColor = Color.Black,
                    modifier = Modifier
                        .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                        .offset(y = 27.dp, x = 27.dp)
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
        Column(
            modifier = Modifier
                .padding(start = 15.dp)
                .weight(1f)
        ) {
            Text(text = partner.name, style = MaterialTheme.typography.titleMedium)
            if (diff < ONE_HOUR_SECONDS) {
                val status = if (diff >= ONE_MINUTE_SECONDS)
                    ((diff + ONE_MINUTE_SECONDS - 1) / ONE_MINUTE_SECONDS).toString() + "m ago"
                else
                    ""
                Text(
                    text = "Online $status", style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    )
                )
            }
        }
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.info),
                contentDescription = "Infomation",
                modifier = Modifier.size(25.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }


@Preview(showBackground = true, device = "id:pixel")
@Composable
fun ChatInfoBarPreview() {
    ApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),

            ) {
            ChatInfoBar(rememberNavController(), mockOnline().copy(at = Instant.now().epochSecond - 2 * ONE_MINUTE_SECONDS))
        }
    }
}
@Preview(showBackground = true, device = "id:pixel")
@Composable
fun InputBarPreview() {
    ApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),

            ) {
            InputBar()
        }
    }
}

@Composable
private fun CircleProgress(number: Int, reached: Boolean, modifier: Modifier = Modifier) {
    val color = if (reached) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        color = color, modifier =
            modifier
                .size(30.dp)
                .clip(CircleShape)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number.toString())
        }
    }
}

@Composable
fun SignUpProgress(currentProgress: Float = 0.3f) = Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(30.dp)
        .padding(horizontal = 5.dp)
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val paddingDiff = with(LocalDensity.current) {
        20.dp.toPx()
    }
    val maxHeightRect = with(LocalDensity.current) {
        8.dp.toPx()
    }
    val rectYOffset = with(LocalDensity.current) {
        15.dp.toPx()
    } - maxHeightRect / 2
    val animateProgress by animateFloatAsState(targetValue = currentProgress, tween(250), label = "progress")

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.CenterStart)
            .padding(horizontal = 5.dp)
    ) {
        val maxWidth = size.width - paddingDiff
        drawRect(
            color = primaryColor,
            topLeft = Offset(0f, rectYOffset),
            size = Size(
                width = maxWidth * animateProgress,
                height = maxHeightRect
            )
        )
    }

    CircleProgress(number = 1, currentProgress >= 0, modifier = Modifier.align(Alignment.CenterStart))
    CircleProgress(number = 2, currentProgress >= 1, modifier = Modifier.align(Alignment.CenterEnd))
}

//@Preview(showBackground = true, device = "id:pixel")
@Composable
private fun ProgressPreview() {
    ApplicationTheme {
        SignUpProgress()
    }
}