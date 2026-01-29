package com.ontrek.mobile.utils.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ontrek.shared.data.UserMinimal

@Composable
fun ProfileItem(
    user: UserMinimal,
    addMember: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {},
    onProfileClick: (() -> Unit)? = null,
    groupOwner: String = "",
    currentUserId: String = "",
    textDelete: String = "Delete Friendship",
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 1.dp,
                vertical = 4.dp,
            )
            .then(
                if (onProfileClick != null) {
                    Modifier.clickable { onProfileClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(50.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                title = textDelete,
                text = "Are you sure you want to delete @${user.username} from this list?",
                onConfirm = {
                    onClick()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false },
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp,
                ),
        ) {
            ImageProfile(
                colorBorder = groupOwner != "",
                userID = user.id,
                color = color,
                modifier = Modifier.padding(end = 10.dp),
            )

            Username(
                user.username,
                modifier = Modifier.weight(1f),
            )

            if (addMember) {
                IconButton(
                    onClick = { onClick() },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAddAlt1,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Add Member",
                    )
                }
            }  else if (groupOwner == user.id) {
                Text(
                    text = "Group Owner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                )
            } else if (currentUserId == groupOwner && groupOwner.isNotEmpty()) {
                IconButton(
                    onClick = { showDeleteDialog = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = textDelete,
                    )
                }
            }
        }
    }
}