package com.ontrek.mobile.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.ontrek.mobile.data.PreferencesViewModel
import com.ontrek.mobile.data.NotificationViewModel
import com.ontrek.mobile.screens.profile.ProfileScreen
import com.ontrek.mobile.screens.group.GroupsScreen
import com.ontrek.mobile.screens.group.detail.GroupDetailsScreen
import com.ontrek.mobile.screens.search.SearchFriendsScreen
import com.ontrek.mobile.screens.track.TrackScreen
import com.ontrek.mobile.screens.track.detail.TrackDetailScreen

@Composable
fun NavigationStack(navController: NavHostController) {
    val preferencesViewModel: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory)
    val notificationViewModel: NotificationViewModel = viewModel()

    val friendRequestsCount by notificationViewModel.requestsCount.collectAsState()

    LaunchedEffect(Unit) {
        notificationViewModel.loadFriendRequests()
    }

    NavHost(
        navController = navController,
        startDestination = TopLevelScreen.Tracks.route,
    ) {

        navigation(route = TopLevelScreen.Profile.route, startDestination = Screen.Profile.route) {
            composable(route = Screen.Profile.route) {
                ProfileScreen(
                    navController = navController,
                    token = preferencesViewModel.tokenState.value ?: "",
                    currentUser = preferencesViewModel.currentUserState.value ?: "",
                    clearToken = {
                        preferencesViewModel.clearToken()
                    },
                    friendRequestsCount = friendRequestsCount
                )
            }
            composable(route = Screen.UserProfile.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                val username = backStackEntry.arguments?.getString("username") ?: ""
                ProfileScreen(
                    navController = navController,
                    token = preferencesViewModel.tokenState.value ?: "",
                    currentUser = preferencesViewModel.currentUserState.value ?: "",
                    clearToken = {
                        preferencesViewModel.clearToken()
                    },
                    userId = userId,
                    username = username,
                    friendRequestsCount = friendRequestsCount
                )
            }
        }

        navigation(route = TopLevelScreen.Tracks.route, startDestination = Screen.Tracks.route) {
            composable(route = Screen.Tracks.route) {
                TrackScreen(
                    navController = navController,
                    friendRequestsCount = friendRequestsCount
                )
            }
            composable(route = Screen.TrackDetail.route) { backStackEntry ->
                val trackId = backStackEntry.arguments?.getString("trackId")?.toInt() ?: 0
                TrackDetailScreen(
                    trackId = trackId,
                    navController = navController,
                    currentUser = preferencesViewModel.currentUserState.value ?: "",
                    friendRequestsCount = friendRequestsCount
                )
            }
        }

        navigation(route = TopLevelScreen.Groups.route, startDestination = Screen.Groups.route) {
            composable(route = Screen.Groups.route) {
                GroupsScreen(
                    navController = navController,
                    friendRequestsCount = friendRequestsCount
                )
            }
            composable(route = Screen.GroupDetails.route) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toInt() ?: 0
                GroupDetailsScreen(
                    groupId = groupId,
                    navController = navController,
                    currentUser = preferencesViewModel.currentUserState.value ?: "",
                    friendRequestsCount = friendRequestsCount
                )
            }
        }

        navigation(route = TopLevelScreen.Search.route, startDestination = Screen.Search.route) {
            composable(route = Screen.Search.route) {
                SearchFriendsScreen(
                    navController = navController,
                    friendRequestsCount = friendRequestsCount
                )
            }
        }
    }
}