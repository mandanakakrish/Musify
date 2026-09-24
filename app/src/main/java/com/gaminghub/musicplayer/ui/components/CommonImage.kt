package com.gaminghub.musicplayer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gaminghub.musicplayer.R

@Composable
fun SongImage(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    AppAsyncImage(
        model = model,
        defaultRes = R.drawable.song,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment
    )
}

@Composable
fun ArtistImage(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    AppAsyncImage(
        model = model,
        defaultRes = R.drawable.artist,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment
    )
}

@Composable
fun PlaylistImage(
    model: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    AppAsyncImage(
        model = model,
        defaultRes = R.drawable.cover,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment
    )
}

@Composable
fun AppAsyncImage(
    model: Any?,
    @DrawableRes defaultRes: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    val context = LocalContext.current
    val placeholderPainter = painterResource(id = defaultRes)

    val validModel = when (model) {
        null -> null
        is String -> if (model.isBlank()) null else model
        else -> model
    }

    val prefs = androidx.compose.runtime.remember(context) {
        context.getSharedPreferences("Musify_settings", android.content.Context.MODE_PRIVATE)
    }
    val useLessData = prefs.getBoolean("use_less_data", false)

    val processedModel = if (useLessData && validModel is String) {
        var str = validModel
        if (str.contains("maxresdefault")) {
            str = str.replace("maxresdefault", "hqdefault")
        }
        if (str.contains("=w1200-h1200")) {
            str = str.replace("=w1200-h1200", "=w240-h240")
        }
        if (str.contains("s1200-p")) {
            str = str.replace("s1200-p", "s240-p")
        }
        if (str.contains("/s1200/")) {
            str = str.replace("/s1200/", "/s240/")
        }
        str
    } else {
        validModel
    }

    if (processedModel == null) {
        Image(
            painter = placeholderPainter,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment
        )
    } else {
        val requestBuilder = ImageRequest.Builder(context)
            .data(processedModel)
            .crossfade(true)
        if (useLessData) {
            requestBuilder.size(240, 240)
        }
        AsyncImage(
            model = requestBuilder.build(),
            placeholder = placeholderPainter,
            error = placeholderPainter,
            fallback = placeholderPainter,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment
        )
    }
}
