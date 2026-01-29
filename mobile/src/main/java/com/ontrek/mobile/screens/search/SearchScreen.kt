package com.ontrek.mobile.screens.search

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import com.ontrek.mobile.screens.track.components.TrackItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ontrek.mobile.screens.Screen
import com.ontrek.mobile.utils.components.BottomNavBar
import com.ontrek.mobile.utils.components.EmptyComponent
import com.ontrek.mobile.utils.components.ErrorComponent
import com.ontrek.mobile.utils.components.ProfileItem
import com.ontrek.shared.data.Track
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFriendsScreen(
    navController: NavHostController,
    friendRequestsCount: Int = 0
) {
    val viewModel: SearchViewModel = viewModel()
    val msgToast by viewModel.msgToast.collectAsState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    
    val tabs = listOf("Users", "Tracks")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var query by remember { mutableStateOf("") }
    
    val userSearchState by viewModel.userSearchState.collectAsState()
    val trackSearchState by viewModel.trackSearchState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Re-trigger search when tab changes
    LaunchedEffect(pagerState.currentPage, query) {
        if (query.isNotEmpty()) {
            when (pagerState.currentPage) {
                0 -> viewModel.searchUsers(query)
                1 -> viewModel.searchTracks(query)
            }
        }
    }

    if (msgToast.isNotEmpty()) {
        LaunchedEffect(msgToast) {
            Toast.makeText(context, msgToast, Toast.LENGTH_SHORT).show()
            viewModel.clearMsgToast()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Search")
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { 
            BottomNavBar(
                navController = navController,
                friendRequestsCount = friendRequestsCount
            ) 
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar at the top
            OutlinedTextField(
                value = query,
                onValueChange = { newQuery ->
                    query = newQuery
                    when (pagerState.currentPage) {
                        0 -> viewModel.searchUsers(newQuery)
                        1 -> viewModel.searchTracks(newQuery)
                    }
                },
                label = { 
                    Text(
                        if (pagerState.currentPage == 0) "Search for username" 
                        else "Search for track title"
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp)
                    .padding(top = 8.dp, bottom = 4.dp),
                shape = RoundedCornerShape(30.dp)
            )
            
            // Tabs below the search bar
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }
            
            // Swipeable pager for results
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp)
                ) {
                    when (page) {
                        0 -> UsersResultsContent(
                            query = query,
                            searchState = userSearchState,
                            onUserClick = { userId, username ->
                                navController.navigate(Screen.UserProfile.createRoute(userId, username))
                            }
                        )
                        1 -> TracksResultsContent(
                            query = query,
                            searchState = trackSearchState,
                            currentUser = currentUser,
                            onTrackClick = { track ->
                                navController.navigate(Screen.TrackDetail.createRoute(track.id.toString()))
                            },
                            onToggleSave = { track -> 
                                viewModel.toggleTrackSave(track) 
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersResultsContent(
    query: String,
    searchState: SearchViewModel.UserSearchState,
    onUserClick: (String, String) -> Unit
) {
    when (searchState) {
        is SearchViewModel.UserSearchState.Loading -> {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        is SearchViewModel.UserSearchState.Error -> {
            ErrorComponent(errorMsg = searchState.message)
        }

        is SearchViewModel.UserSearchState.Success -> {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(searchState.users) { user ->
                    ProfileItem(
                        user = user,
                        onProfileClick = { onUserClick(user.id, user.username) }
                    )
                }
            }
        }

        SearchViewModel.UserSearchState.Empty -> {
            EmptyComponent(
                title = if (query.isEmpty()) "Search for users" else "No results found",
                description = if (query.isEmpty()) {
                    "Type a username to search for users."
                } else {
                    "Try searching with a different username."
                },
                icon = if (query.isEmpty()) {
                    Icons.Default.Search
                } else {
                    Icons.Default.SearchOff
                },
            )
        }
    }
}

@Composable
private fun TracksResultsContent(
    query: String,
    searchState: SearchViewModel.TrackSearchState,
    currentUser: com.ontrek.shared.data.Profile?,
    onTrackClick: (Track) -> Unit,
    onToggleSave: (Track) -> Unit
) {
    when (searchState) {
        is SearchViewModel.TrackSearchState.Loading -> {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        is SearchViewModel.TrackSearchState.Error -> {
            ErrorComponent(errorMsg = searchState.message)
        }

        is SearchViewModel.TrackSearchState.Success -> {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(searchState.tracks) { track ->
                    val isCreatedByMe = currentUser != null && track.owner == currentUser.username
                    TrackItem(
                        track = track,
                        isCreatedByMe = isCreatedByMe,
                        isSavedByMe = track.saved,
                        onToggleSave = { onToggleSave(track) },
                        onItemClick = { onTrackClick(track) }
                    )
                }
            }
        }

        SearchViewModel.TrackSearchState.Empty -> {
            EmptyComponent(
                title = if (query.isEmpty()) "Search for tracks" else "No results found",
                description = if (query.isEmpty()) {
                    "Type a track title to search."
                } else {
                    "Try searching with a different title."
                },
                icon = if (query.isEmpty()) {
                    Icons.Default.Search
                } else {
                    Icons.Default.SearchOff
                },
            )
        }
    }
}