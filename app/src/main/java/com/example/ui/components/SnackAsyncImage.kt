package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.InternalStorageHelper
import java.io.File

@Composable
fun SnackAsyncImage(
    imagePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    contentScale: ContentScale = ContentScale.Fit, // Fit ensures logo edges remain visible
    fallbackIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Default.Fastfood,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(32.dp)
        )
    }
) {
    val context = LocalContext.current

    val imageModel: Any? = when {
        imagePath.isNullOrBlank() -> null
        imagePath.startsWith("res:sample_chips") -> R.drawable.sample_chips
        imagePath.startsWith("res:sample_cookies") -> R.drawable.sample_cookies
        imagePath.startsWith("res:sample_drink") -> R.drawable.sample_drink
        imagePath.startsWith("res:ic_app_icon") -> R.drawable.ic_app_icon
        imagePath.startsWith("res:") -> {
            val resName = imagePath.removePrefix("res:")
            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (resId != 0) resId else null
        }
        imagePath.startsWith("content://") || imagePath.startsWith("file://") -> {
            Uri.parse(imagePath)
        }
        else -> {
            InternalStorageHelper.resolveImageFile(context, imagePath)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            fallbackIcon()
        }
    }
}
