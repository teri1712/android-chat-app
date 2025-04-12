package com.decade.practice.view.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.decade.practice.R
import com.decade.practice.view.theme.ApplicationTheme
import com.decade.practice.view.theme.LocalTheme

@Composable
fun Setting(onProfileClick: () -> Unit = {}, onLogoutClick: () -> Unit = {}) = Column {
    val user = LocalUser.current
    Row {
        Box {
            AsyncImage(
                model = user.avatar.uri,
                placeholder = painterResource(id = R.drawable.avatar_placeholder),
                contentDescription = "Partner avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
        ) {
            Text(
                text = user.name, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = user.gender,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Medium

            )
        }
    }

    SettingItem("Profile", R.drawable.profile)
    SettingItem("Logout", R.drawable.logout, onLogoutClick)
    ThemeMode()
}

@Composable
private fun ThemeMode() = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    val theme = LocalTheme.current
    val icon = if (theme.light) R.drawable.light_mode else R.drawable.dark_mode
    val content = if (theme.light) "Light mode" else "Dark mode"
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            "Theme mode",
            modifier = Modifier
                .size(20.dp)
        )
        Text(content, style = MaterialTheme.typography.titleMedium)
    }

    Switch(
        modifier = Modifier.scale(0.8f),
        checked = theme.light,
        onCheckedChange = {
            theme.light = !theme.light
        },
        thumbContent = {
            Icon(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )

}

@Composable
private fun SettingItem(name: String, icon: Int, onClick: () -> Unit = {}) =
    TextButton(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RectangleShape,
        colors = ButtonDefaults.textButtonColors().copy(
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        contentPadding = PaddingValues(0.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                name,
                modifier = Modifier
                    .size(20.dp)
            )
            Text(name, style = MaterialTheme.typography.titleMedium)
        }
    }


@Preview(showBackground = true, device = "id:pixel")
@Composable
private fun SettingScreenPreview() {
    ApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Setting()
        }
    }
}
