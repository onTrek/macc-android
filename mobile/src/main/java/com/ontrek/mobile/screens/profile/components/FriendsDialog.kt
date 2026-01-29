package com.ontrek.mobile.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ontrek.mobile.screens.profile.ProfileViewModel
import com.ontrek.mobile.utils.components.EmptyComponent
import com.ontrek.mobile.utils.components.ErrorComponent
import com.ontrek.mobile.utils.components.ProfileItem
import com.ontrek.shared.data.UserMinimal

@Composable
fun FriendsDialog(
    viewModel: ProfileViewModel,
    onUserClick: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val friendsState by viewModel.friendsState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your Friends",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                when (friendsState) {
                    is ProfileViewModel.FriendsState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    is ProfileViewModel.FriendsState.Empty -> {
                         EmptyComponent(
                            fillMaxSize = false,
                            title = "No Friends",
                            description = "You have no friends yet.",
                            icon = Icons.Default.PersonSearch,
                        )
                    }

                    is ProfileViewModel.FriendsState.Error -> {
                         val errorState = friendsState as ProfileViewModel.FriendsState.Error
                        ErrorComponent(
                            errorMsg = errorState.message,
                        )
                    }

                    is ProfileViewModel.FriendsState.Success -> {
                        val friends = (friendsState as ProfileViewModel.FriendsState.Success).friends

                        if (friends.isEmpty()) {
                             EmptyComponent(
                                fillMaxSize = false,
                                title = "No Friends",
                                description = "You have no friends yet.",
                                icon = Icons.Default.PersonSearch,
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .heightIn(max = 400.dp)
                            ) {
                                items(friends) { friend ->
                                    ProfileItem(
                                        user = UserMinimal(
                                            id = friend.id,
                                            username = friend.username,
                                        ),
                                        onClick = { viewModel.removeFriend(friend.id) },
                                        onProfileClick = {
                                            onUserClick(friend.id, friend.username)
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
