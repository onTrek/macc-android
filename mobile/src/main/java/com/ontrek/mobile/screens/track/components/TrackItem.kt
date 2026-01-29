package com.ontrek.mobile.screens.track.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ontrek.shared.data.Track

import androidx.compose.material.icons.filled.AccountCircle

@Composable
fun TrackItem(
    track: Track,
    isCreatedByMe: Boolean = false,
    isSavedByMe: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onItemClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            // Colonna per titolo e distanza
            Column(
                modifier = Modifier.weight(1f)
            ) {
                TitleTrack(
                    title = track.title,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Distanza del percorso
                Text(
                    text = "${track.stats.km} km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                if (isCreatedByMe) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Created by me",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (onToggleSave != null && !isCreatedByMe) {
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            imageVector = if (isSavedByMe) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isSavedByMe) "Unsave" else "Save",
                            tint = if (isSavedByMe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}