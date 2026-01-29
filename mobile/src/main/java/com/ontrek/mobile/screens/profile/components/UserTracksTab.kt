package com.ontrek.mobile.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ontrek.mobile.screens.profile.ProfileViewModel
import com.ontrek.mobile.screens.track.components.TrackItem
import com.ontrek.mobile.utils.components.EmptyComponent
import com.ontrek.mobile.utils.components.ErrorComponent


@Composable
fun UserTracksTab(
    viewModel: ProfileViewModel,
    onTrackClick: (Int) -> Unit = { },
) {
    val userTracksState by viewModel.userTracksState.collectAsState()


    Column(
        modifier = Modifier.padding(vertical = 5.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tracks",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(
            thickness = DividerDefaults.Thickness,
            color = DividerDefaults.color,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (userTracksState) {
            is ProfileViewModel.UserTracksState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is ProfileViewModel.UserTracksState.Empty -> {
                EmptyComponent(
                    fillMaxSize = false,
                    title = "No Tracks",
                    description = "This user has not created any tracks yet.",
                    icon = Icons.Outlined.Route,
                )
            }

            is ProfileViewModel.UserTracksState.Error -> {
                val errorState = userTracksState as ProfileViewModel.UserTracksState.Error
                ErrorComponent(
                    errorMsg = errorState.message,
                )
            }

            is ProfileViewModel.UserTracksState.Success -> {
                val tracks = (userTracksState as ProfileViewModel.UserTracksState.Success).tracks

                if (tracks.isEmpty()) {
                    EmptyComponent(
                        fillMaxSize = false,
                        title = "No Tracks",
                        description = "This user has not created any tracks yet.",
                        icon = Icons.Outlined.Route,
                    )
                } else {
                    Column {
                        tracks.forEach { track ->
                            TrackItem(
                                track = track,
                                onItemClick = { onTrackClick(track.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
