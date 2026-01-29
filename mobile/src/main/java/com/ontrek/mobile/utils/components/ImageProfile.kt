package com.ontrek.mobile.utils.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ontrek.shared.api.profile.getImageProfile
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun ImageProfile(
    userID: String,
    colorBorder: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    size: Int = 50
) {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(userID) {
        loading = true
        imageUrl = getImageUrlForUser(userID)
        loading = false
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .then(
                if (colorBorder) {
                    Modifier.border(
                        width = 2.dp,
                        color = color,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            val stableKey = remember(imageUrl) { imageUrl!!.split("?")[0] }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .memoryCacheKey(stableKey)
                    .diskCacheKey(stableKey)
                    .crossfade(true)
                    .build(),
                contentDescription = "Immagine profilo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { loading = true },
                onSuccess = { loading = false },
                onError = { loading = false }
            )
        } else if (!loading) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = color.copy(alpha = 0.5f),
                modifier = Modifier.size(size.dp * 0.5f)
            )
        }

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.dp * 0.4f),
                strokeWidth = 2.dp,
                color = color
            )
        }
    }
}

suspend fun getImageUrlForUser(userID: String): String? {
    return suspendCancellableCoroutine { continuation ->
        getImageProfile(
            id = userID,
            onSuccess = { url ->
                continuation.resume(url)
            },
            onError = { errorMessage ->

                continuation.resume(null)
            }
        )
    }
}