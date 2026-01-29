package com.ontrek.mobile.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mr0xf00.easycrop.AspectRatio
import com.mr0xf00.easycrop.CircleCropShape
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.CropState
import com.mr0xf00.easycrop.CropperStyle
import com.mr0xf00.easycrop.LocalCropperStyle
import com.mr0xf00.easycrop.crop
import com.mr0xf00.easycrop.rememberImageCropper
import com.mr0xf00.easycrop.ui.CropperPreview
import com.ontrek.mobile.screens.Screen
import com.ontrek.mobile.screens.profile.components.ConnectionWearButton
import com.ontrek.mobile.screens.profile.components.FriendsDialog
import com.ontrek.mobile.screens.profile.components.UserTracksTab
import com.ontrek.mobile.screens.profile.components.ImageProfileDialog
import com.ontrek.mobile.screens.profile.components.ImageSourceDialog
import com.ontrek.mobile.screens.profile.components.MenuDialog
import com.ontrek.mobile.screens.profile.components.ProfileCard
import com.ontrek.mobile.screens.profile.components.RequestsDialog
import com.ontrek.mobile.screens.profile.components.FriendshipButton
import com.ontrek.mobile.screens.profile.ProfileViewModel.RequestsState.Companion.count
import com.ontrek.mobile.utils.ImageUtils
import com.ontrek.mobile.utils.components.BottomNavBar
import com.ontrek.mobile.utils.components.ErrorComponent
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

// Custom Material3 top bar for the cropper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropperTopBar(state: CropState) {
    TopAppBar(
        title = { Text("Crop image") },
        navigationIcon = {
            IconButton(onClick = { state.done(accept = false) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cancel"
                )
            }
        },
        actions = {
            IconButton(onClick = { state.reset() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset"
                )
            }
            IconButton(
                onClick = { state.done(accept = true) },
                enabled = !state.accepted
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Confirm"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

// Custom Image Cropper Dialog with Material3 styling
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Material3ImageCropperDialog(
    state: CropState,
    style: CropperStyle
) {
    val dialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    )

    CompositionLocalProvider(LocalCropperStyle provides style) {
        Dialog(
            onDismissRequest = { state.done(accept = false) },
            properties = dialogProperties,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column {
                    CropperTopBar(state)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clipToBounds()
                    ) {
                        CropperPreview(state = state, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    token: String,
    currentUser: String,
    clearToken: () -> Unit,
    userId: String? = null,
    username: String? = null,
    friendRequestsCount: Int = 0
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val viewModel: ProfileViewModel = viewModel()
    val scope = rememberCoroutineScope()
    var showMenuDialog by remember { mutableStateOf(false) }
    var showRequestsDialog by remember { mutableStateOf(false) }
    var showFriendsDialog by remember { mutableStateOf(false) }

    // Determine if viewing own profile or another user's profile
    val isOwner = userId == null || userId == currentUser
    val requestsState by viewModel.requestsState.collectAsState()
    val requestsCount = requestsState.count
    val friendsState by viewModel.friendsState.collectAsState()
    val friendsCount = when (val state = friendsState) {
        is ProfileViewModel.FriendsState.Success -> state.friends.size
        else -> 0
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val imageProfile by viewModel.imageProfile.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val msgToast by viewModel.msgToast.collectAsState()
    val relationshipState by viewModel.relationshipState.collectAsState()


    var previewImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var preview by remember { mutableStateOf(ByteArray(0)) }
    var modifyImageProfile by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var selectedFilename by remember { mutableStateOf<String?>(null) }

    // Easycrop image cropper
    val imageCropper = rememberImageCropper()

    // Custom style with only 1:1 aspect ratio and Material3 colors
    val cropperStyle = CropperStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        overlay = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f),
        aspects = listOf(AspectRatio(1, 1)), // Only 1:1 aspect ratio
        shapes = null, // No shape selection (rectangle only)
        autoZoom = true // Auto-zoom to fit crop region
    )

    // Temporary file for camera capture
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Function to crop image from URI and process result
    fun cropAndProcessImage(uri: android.net.Uri) {
        scope.launch {
            val result = imageCropper.crop(uri, context)
            when (result) {
                is CropResult.Success -> {
                    val croppedBitmap = result.bitmap.asAndroidBitmap()
                    val outputStream = ByteArrayOutputStream()
                    croppedBitmap.compress(
                        android.graphics.Bitmap.CompressFormat.JPEG,
                        90,
                        outputStream
                    )
                    val imageBytes = outputStream.toByteArray()

                    if (imageBytes.isNotEmpty()) {
                        selectedFilename = "profile_image.jpg"
                        preview = imageBytes
                    }
                }

                else -> {
                    // User cancelled or error occurred
                }
            }
        }
    }

    // Function to create temp file and launch camera (called after permission check)
    fun doLaunchCamera() {
        try {
            val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
            tempCameraUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            Log.e("ProfileScreen", "Error creating temp file for camera", e)
            Toast.makeText(context, "Error launching camera", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            // Correct EXIF orientation before cropping (fixes 90° rotation on physical devices)
            val correctedUri = ImageUtils.correctImageOrientation(context, tempCameraUri!!)
            cropAndProcessImage(correctedUri)
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, launch camera
            doLaunchCamera()
            tempCameraUri?.let { cameraLauncher.launch(it) }
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to take photos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Function to request camera permission and launch camera
    fun launchCamera() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, launch camera
                doLaunchCamera()
                tempCameraUri?.let { cameraLauncher.launch(it) }
            }

            else -> {
                // Request permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cropAndProcessImage(uri)
        }
    }

    // Show crop dialog when cropper is active
    val cropState = imageCropper.cropState
    if (cropState != null) {
        // Function to apply 1:1 square crop settings
        fun applySquareCrop() {
            // Set circle shape for visual reference
            cropState.shape = CircleCropShape

            // Get the full image region (initial bounds)
            val fullRegion = cropState.region

            // Step 1: Set a small 1:1 square region first (establishes 1:1 aspect for aspectLock)
            val tempSquare = androidx.compose.ui.geometry.Rect(
                left = fullRegion.center.x - 1f,
                top = fullRegion.center.y - 1f,
                right = fullRegion.center.x + 1f,
                bottom = fullRegion.center.y + 1f
            )
            cropState.region = tempSquare

            // Step 2: Enable aspectLock - now it will lock to 1:1
            cropState.aspectLock = true

            // Step 3: Expand to the largest centered 1:1 square that fits the full image
            val minSize = minOf(fullRegion.width, fullRegion.height)
            val centerX = fullRegion.center.x
            val centerY = fullRegion.center.y
            val largestSquare = androidx.compose.ui.geometry.Rect(
                left = centerX - minSize / 2,
                top = centerY - minSize / 2,
                right = centerX + minSize / 2,
                bottom = centerY + minSize / 2
            )
            cropState.region = largestSquare
        }

        // Apply 1:1 settings on initial composition
        remember(cropState) {
            applySquareCrop()
            true
        }

        // Observe aspectLock changes - when reset() is called, aspectLock becomes false
        // Re-apply 1:1 settings when this happens
        LaunchedEffect(cropState) {
            snapshotFlow { cropState.aspectLock }
                .collect { isLocked ->
                    if (!isLocked) {
                        // Reset was triggered, re-apply 1:1 settings
                        applySquareCrop()
                    }
                }
        }

        Material3ImageCropperDialog(state = cropState, style = cropperStyle)
    }

    val imageUrl = remember(imageProfile) {
        if (imageProfile is ProfileViewModel.UserImageState.Success) {
            (imageProfile as ProfileViewModel.UserImageState.Success).url
        } else null
    }

    val imageVersion = remember(imageProfile) {
        if (imageProfile is ProfileViewModel.UserImageState.Success) {
            (imageProfile as ProfileViewModel.UserImageState.Success).version
        } else 0L
    }

    LaunchedEffect(preview.contentHashCode()) {
        if (preview.isNotEmpty()) {
            previewImageBitmap =
                BitmapFactory.decodeByteArray(preview, 0, preview.size)?.asImageBitmap()
        }
    }


    LaunchedEffect(imageUrl) {
        modifyImageProfile = false
        preview = ByteArray(0)
        previewImageBitmap = null
        selectedFilename = null
    }

    LaunchedEffect(selectedFilename) {
        if (selectedFilename != null) {
            modifyImageProfile = true
        }
    }

    LaunchedEffect(Unit) {
        if (userId != null && userId != currentUser) {
            if (username != null) {
                viewModel.setOtherUserProfile(userId, username)
                viewModel.fetchRelationshipStatus(userId, username)
            }
            viewModel.fetchOtherUserImage(userId)
            viewModel.fetchUserTracks(userId, isOwner = false)
        } else {
            viewModel.fetchUserProfile()
            viewModel.fetchFriends()
            viewModel.loadFriendRequests()
            viewModel.fetchUserTracks(currentUser, isOwner = true)
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
                    Text(text = if (isOwner) "Your profile" else "Profile")
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (!isOwner) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showRequestsDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (friendRequestsCount > 0) {
                                        Badge {
                                            Text(text = friendRequestsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Friend Requests",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { showMenuDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isOwner) {
                BottomNavBar(
                    navController = navController,
                    friendRequestsCount = friendRequestsCount
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showMenuDialog) {
                MenuDialog(
                    onDismiss = { showMenuDialog = false },
                    onDeleteProfile = {
                        viewModel.deleteProfile(
                            clearToken = { clearToken() },
                        )
                        viewModel.setMsgToast("Your profile has been deleted")
                    },
                    onLogoutClick = {
                        clearToken()
                        viewModel.setMsgToast("You have been logged out")
                    }
                )
            }

            if (showImageSourceDialog) {
                ImageSourceDialog(
                    onDismiss = { showImageSourceDialog = false },
                    onCameraSelected = {
                        showImageSourceDialog = false
                        launchCamera()
                    },
                    onGallerySelected = {
                        showImageSourceDialog = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            if (modifyImageProfile) {
                ImageProfileDialog(
                    previewImageBitmap = previewImageBitmap,
                    onDismiss = {
                        modifyImageProfile = false
                        preview = ByteArray(0)
                        previewImageBitmap = null
                        selectedFilename = null
                    },
                    onConfirm = {
                        if (selectedFilename != null) {
                            viewModel.updateProfileImage(preview, selectedFilename!!)
                        }
                    }
                )
            }

            if (showRequestsDialog && isOwner) {
                RequestsDialog(
                    viewModel = viewModel,
                    onDismiss = { showRequestsDialog = false }
                )
            }

            if (showFriendsDialog && isOwner) {
                FriendsDialog(
                    viewModel = viewModel,
                    onUserClick = { userId, username ->
                        navController.navigate(Screen.UserProfile.createRoute(userId, username))
                    },
                    onDismiss = { showFriendsDialog = false }
                )
            }

            when (userProfile) {
                is ProfileViewModel.UserProfileState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ProfileViewModel.UserProfileState.Success -> {
                    val profile =
                        (userProfile as ProfileViewModel.UserProfileState.Success).userProfile

                    ProfileCard(
                        profile = profile,
                        imageUrl = imageUrl,
                        imageVersion = imageVersion,
                        imageLoadingState = imageProfile,
                        isOwner = isOwner,
                        friendsCount = friendsCount,
                        onImageClick = { if (isOwner) showImageSourceDialog = true },
                        onFriendsClick = { showFriendsDialog = true }
                    )

                    Spacer(modifier = Modifier.padding(vertical = 8.dp))

                    if (!isOwner && userId != null) {
                        FriendshipButton(
                            relationshipState = relationshipState,
                            onSendRequest = { viewModel.sendFriendRequestToUser(userId) },
                            onRemoveFriend = { viewModel.removeFriendFromProfile(userId) }
                        )

                        Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    if (isOwner) {
                        ConnectionWearButton(
                            connectionState = connectionStatus,
                            onConnectClick = {
                                viewModel.sendAuthToWearable(context, token, currentUser)
                            },
                        )

                        Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // User Tracks section
                    UserTracksTab(
                        viewModel = viewModel,
                        onTrackClick = { trackId ->
                            navController.navigate(Screen.TrackDetail.createRoute(trackId.toString()))
                        }
                    )
                }

                is ProfileViewModel.UserProfileState.Error -> {
                    val errorMsg = (userProfile as ProfileViewModel.UserProfileState.Error).message
                    ErrorComponent(errorMsg = errorMsg)
                }
            }
        }
    }
}