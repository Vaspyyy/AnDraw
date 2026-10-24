package dev.vaspyyy.andraw

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterBAndW
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.vaspyyy.andraw.ui.theme.AnDrawTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AppScreen { HOME, TRACE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnDrawTheme {
                AnDrawApp()
            }
        }
    }
}

@Composable
private fun AnDrawApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("andraw_preferences", Context.MODE_PRIVATE)
    }
    var savedUri by remember {
        mutableStateOf(preferences.getString("last_reference_uri", null))
    }
    val imageUri = remember(savedUri) { savedUri?.let(Uri::parse) }
    var screen by remember { mutableStateOf(AppScreen.HOME) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Copy the selected image into app-private storage. Some providers (including
            // cloud/photo-picker providers) return URIs that later stop being readable.
            val copiedUri = runCatching {
                val referenceDir = File(context.filesDir, "references").apply { mkdirs() }
                val target = File(referenceDir, "last-reference-image")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: error("The selected image could not be opened.")
                Uri.fromFile(target).toString()
            }.getOrNull()
            if (copiedUri != null) {
                savedUri = copiedUri
                preferences.edit().putString("last_reference_uri", savedUri).apply()
                screen = AppScreen.TRACE
            }
        }
    }

    BackHandler(enabled = screen == AppScreen.TRACE) {
        screen = AppScreen.HOME
    }

    if (screen == AppScreen.TRACE && imageUri != null) {
        TraceScreen(
            imageUri = imageUri,
            onBack = { screen = AppScreen.HOME },
            onChooseAnother = { imagePicker.launch(arrayOf("image/*")) }
        )
    } else {
        HomeScreen(
            hasSavedReference = imageUri != null,
            onImport = { imagePicker.launch(arrayOf("image/*")) },
            onResume = {
                if (imageUri != null) screen = AppScreen.TRACE
                else imagePicker.launch(arrayOf("image/*"))
            }
        )
    }
}

@Composable
private fun HomeScreen(
    hasSavedReference: Boolean,
    onImport: () -> Unit,
    onResume: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("AnDraw", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    "YOUR LITTLE ART NOOK",
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "Made for your next favorite drawing",
                tint = colors.tertiary,
                modifier = Modifier.size(23.dp)
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Let’s draw\nsomething lovely.",
            fontSize = 34.sp,
            lineHeight = 39.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "A little space for big anime-art energy.",
            fontSize = 16.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF6DFEA),
                            Color(0xFFEAE4F8),
                            Color(0xFFE0EEE7)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Surface(
                    color = Color.White.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        "✦  TRACE MODE  ✦",
                        color = Color(0xFF704B69),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
                Spacer(Modifier.height(15.dp))
                Text(
                    "Your reference.\nYour paper. Your pace.",
                    fontSize = 24.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF392B3A)
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    "Line up fanart over the live camera, lock it in, and trace at your own pace.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF59495B)
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(9.dp))
                    Text("Choose anime fanart", fontWeight = FontWeight.SemiBold)
                }
                if (hasSavedReference) {
                    Spacer(Modifier.height(9.dp))
                    FilledTonalButton(
                        onClick = onResume,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text("Continue last reference")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "A setup that just works",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Spacer(Modifier.height(12.dp))
        TipCard(
            icon = Icons.Outlined.CenterFocusStrong,
            title = "Use your phone stand",
            body = "Point the rear camera straight down. Keep the phone and paper still, then line up the reference."
        )
        Spacer(Modifier.height(10.dp))
        TipCard(
            icon = Icons.Outlined.Opacity,
            title = "Try Ink only",
            body = "For dark line art on a white background, Ink only removes bright pixels so your paper stays visible."
        )
        Spacer(Modifier.height(10.dp))
        TipCard(
            icon = Icons.Outlined.Lock,
            title = "Lock before you trace",
            body = "Move, pinch, and rotate your reference first. Lock alignment to stop accidental shifts while drawing."
        )

        Spacer(Modifier.height(18.dp))
        Text(
            "Made for late-night sketches and favorite characters.",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun TipCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    body,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TraceScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onChooseAnother: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val alignmentPreferences = remember(context) {
        context.getSharedPreferences("andraw_preferences", Context.MODE_PRIVATE)
    }
    val referenceKey = remember(imageUri) {
        "reference_" + imageUri.toString().hashCode().toString()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isLocked by remember(imageUri) { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showGuides by remember(imageUri) {
        mutableStateOf(alignmentPreferences.getBoolean(referenceKey + "_guides", true))
    }
    var inkOnly by remember(imageUri) {
        mutableStateOf(alignmentPreferences.getBoolean(referenceKey + "_ink_only", false))
    }
    var grayscale by remember(imageUri) {
        mutableStateOf(alignmentPreferences.getBoolean(referenceKey + "_grayscale", false))
    }
    var opacity by remember(imageUri) {
        mutableStateOf(alignmentPreferences.getFloat(referenceKey + "_opacity", 0.48f))
    }
    var screenBrightness by remember(imageUri) {
        mutableStateOf(alignmentPreferences.getFloat(referenceKey + "_brightness", 0.90f))
    }
    var scale by remember(imageUri) {
        mutableStateOf(alignmentPreferences.getFloat(referenceKey + "_scale", 1f))
    }
    var rotation by remember(imageUri) {
        mutableStateOf(alignmentPreferences.getFloat(referenceKey + "_rotation", 0f))
    }
    var imageOffset by remember(imageUri) {
        mutableStateOf(
            Offset(
                alignmentPreferences.getFloat(referenceKey + "_offset_x", 0f),
                alignmentPreferences.getFloat(referenceKey + "_offset_y", 0f)
            )
        )
    }
    var isLoading by remember(imageUri, inkOnly) { mutableStateOf(true) }
    var loadError by remember(imageUri, inkOnly) { mutableStateOf<String?>(null) }
    var referenceBitmap by remember(imageUri, inkOnly) { mutableStateOf<ImageBitmap?>(null) }

    val originalScreenBrightness = remember(activity) {
        activity?.window?.attributes?.screenBrightness ?: -1f
    }
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            camera?.cameraControl?.enableTorch(false)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (activity != null) {
                val params = activity.window.attributes
                params.screenBrightness = originalScreenBrightness
                activity.window.attributes = params
            }
        }
    }
    LaunchedEffect(screenBrightness, activity) {
        if (activity != null) {
            val params = activity.window.attributes
            params.screenBrightness = screenBrightness.coerceIn(0.25f, 1f)
            activity.window.attributes = params
        }
    }
    LaunchedEffect(
        imageUri, showGuides, inkOnly, grayscale, opacity, screenBrightness,
        scale, rotation, imageOffset
    ) {
        alignmentPreferences.edit()
            .putBoolean(referenceKey + "_guides", showGuides)
            .putBoolean(referenceKey + "_ink_only", inkOnly)
            .putBoolean(referenceKey + "_grayscale", grayscale)
            .putFloat(referenceKey + "_opacity", opacity)
            .putFloat(referenceKey + "_brightness", screenBrightness)
            .putFloat(referenceKey + "_scale", scale)
            .putFloat(referenceKey + "_rotation", rotation)
            .putFloat(referenceKey + "_offset_x", imageOffset.x)
            .putFloat(referenceKey + "_offset_y", imageOffset.y)
            .apply()
    }

    LaunchedEffect(camera, isTorchOn) {
        camera?.cameraControl?.enableTorch(isTorchOn)
    }
    DisposableEffect(camera) {
        onDispose { camera?.cameraControl?.enableTorch(false) }
    }

    LaunchedEffect(imageUri, inkOnly) {
        isLoading = true
        loadError = null
        referenceBitmap = withContext(Dispatchers.IO) {
            runCatching { decodeReferenceBitmap(context, imageUri, inkOnly) }.getOrNull()
        }
        if (referenceBitmap == null) {
            loadError = "Couldn't read this image. Choose it again from your files."
        }
        isLoading = false
    }

    val grayscaleFilter = remember(grayscale) {
        if (grayscale) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF211E27))
    ) {
        if (hasCameraPermission) {
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                onCameraReady = { camera = it; cameraError = null },
                onError = { cameraError = it }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF211E27)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.CenterFocusStrong,
                        contentDescription = null,
                        tint = Color(0xFFF4DCE8),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Your paper is waiting",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "AnDraw needs camera access to show your paper underneath the reference.",
                        color = Color(0xFFE2D9E3),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Allow camera")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "If you denied access permanently, enable Camera in Android app settings.",
                        color = Color(0xFFBDB2C0),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (referenceBitmap != null) {
            val gestureModifier = if (isLocked) {
                Modifier
            } else {
                Modifier.pointerInput(imageUri, isLocked) {
                    detectTransformGestures { _, pan, zoomChange, rotationChange ->
                        scale = (scale * zoomChange).coerceIn(0.35f, 5f)
                        imageOffset = imageOffset + pan
                        rotation += rotationChange
                    }
                }
            }
            androidx.compose.foundation.Image(
                bitmap = referenceBitmap!!,
                contentDescription = "Anime fanart reference overlay",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                colorFilter = grayscaleFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(gestureModifier)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = imageOffset.x
                        translationY = imageOffset.y
                        rotationZ = rotation
                        alpha = opacity
                    }
            )
        }

        if (showGuides) AlignmentGuides()

        if (isLoading) {
            Surface(
                color = Color(0xDDFFF8F5),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Preparing your reference…", color = Color(0xFF40313E))
                }
            }
        }
        if (loadError != null) {
            Surface(
                color = Color(0xF7FFF8F5),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(loadError!!, color = Color(0xFF40313E), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onChooseAnother) { Text("Choose another image") }
                }
            }
        }
        if (cameraError != null && hasCameraPermission) {
            Surface(
                color = Color(0xEFFFF8F5),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            ) {
                Text(
                    "Camera could not start: " + cameraError,
                    color = Color(0xFF40313E),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xF7FFF9FC))
                        .padding(start = 5.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to home")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tracing studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            if (isLocked) "Alignment locked" else "Pinch, drag & rotate to line up",
                            color = Color(0xFF766673),
                            fontSize = 11.sp
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { isLocked = !isLocked },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = if (isLocked) "Unlock alignment" else "Lock alignment"
                        )
                    }
                    IconButton(onClick = { showControls = false }) {
                        Icon(Icons.Outlined.VisibilityOff, contentDescription = "Hide controls")
                    }
                }

                Surface(
                    color = Color(0xF7FFF9FC),
                    shape = RoundedCornerShape(26.dp),
                    tonalElevation = 4.dp,
                    shadowElevation = 7.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Opacity,
                                contentDescription = null,
                                tint = Color(0xFF8F587B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Reference opacity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                (opacity * 100).toInt().toString() + "%",
                                fontSize = 12.sp,
                                color = Color(0xFF766673)
                            )
                        }
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.10f..1f
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Brightness6,
                                contentDescription = null,
                                tint = Color(0xFF8F587B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Screen brightness", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                (screenBrightness * 100).toInt().toString() + "%",
                                fontSize = 12.sp,
                                color = Color(0xFF766673)
                            )
                        }
                        Slider(
                            value = screenBrightness,
                            onValueChange = { screenBrightness = it },
                            valueRange = 0.25f..1f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ToolButton(
                                label = if (inkOnly) "Ink only" else "Original",
                                icon = Icons.Outlined.Palette,
                                selected = inkOnly,
                                onClick = {
                                    inkOnly = !inkOnly
                                    scale = 1f
                                    rotation = 0f
                                    imageOffset = Offset.Zero
                                }
                            )
                            ToolButton(
                                label = "B&W",
                                icon = Icons.Outlined.FilterBAndW,
                                selected = grayscale,
                                onClick = { grayscale = !grayscale }
                            )
                            ToolButton(
                                label = "Guides",
                                icon = Icons.Outlined.GridOn,
                                selected = showGuides,
                                onClick = { showGuides = !showGuides }
                            )
                            ToolButton(
                                label = "Torch",
                                icon = Icons.Outlined.FlashOn,
                                selected = isTorchOn,
                                enabled = camera != null,
                                onClick = { isTorchOn = !isTorchOn }
                            )
                            ToolButton(
                                label = "Recenter",
                                icon = Icons.Outlined.RestartAlt,
                                selected = false,
                                onClick = {
                                    scale = 1f
                                    rotation = 0f
                                    imageOffset = Offset.Zero
                                }
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            if (isLocked) {
                                "Ready to trace. Unlock only when you need to realign."
                            } else {
                                "Match the image to your paper, then lock alignment."
                            },
                            color = Color(0xFF766673),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else {
            FilledTonalIconButton(
                onClick = { showControls = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(14.dp)
                    .size(48.dp)
            ) {
                Icon(Icons.Outlined.Visibility, contentDescription = "Show controls")
            }
            Surface(
                color = Color(0xDDFFF9FC),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    if (isLocked) "Alignment locked  •  Tap eye to adjust"
                    else "Trace view  •  Tap eye for controls",
                    color = Color(0xFF51424E),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(62.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (enabled) Color(0xFF61515E) else Color(0xFFA99BA4),
            maxLines = 1
        )
    }
}

@Composable
private fun AlignmentGuides() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val inset = 17.dp.toPx()
        val dash = 8.dp.toPx()
        val stroke = 1.3.dp.toPx()
        val guideColor = Color(0xFF5F4358).copy(alpha = 0.72f)
        drawRect(
            color = guideColor,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
            )
        )
        drawLine(
            color = guideColor.copy(alpha = 0.48f),
            start = Offset(size.width / 2f, inset),
            end = Offset(size.width / 2f, size.height - inset),
            strokeWidth = stroke
        )
        drawLine(
            color = guideColor.copy(alpha = 0.48f),
            start = Offset(inset, size.height / 2f),
            end = Offset(size.width - inset, size.height / 2f),
            strokeWidth = stroke
        )
    }
}

@Composable
private fun CameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraReady: (Camera) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val latestOnCameraReady by rememberUpdatedState(onCameraReady)
    val latestOnError by rememberUpdatedState(onError)
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    AndroidView(
        factory = { viewContext ->
            val previewView = PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val providerFuture = ProcessCameraProvider.getInstance(viewContext)
            providerFuture.addListener({
                try {
                    val cameraProvider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                    provider = cameraProvider
                    latestOnCameraReady(boundCamera)
                } catch (exception: Exception) {
                    latestOnError(exception.localizedMessage ?: "Could not start the rear camera.")
                }
            }, ContextCompat.getMainExecutor(viewContext))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(lifecycleOwner, context) {
        onDispose {
            provider?.unbindAll()
        }
    }
}

private fun decodeReferenceBitmap(
    context: Context,
    uri: Uri,
    inkOnly: Boolean
): ImageBitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    } ?: return null

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 2400 || bounds.outHeight / sampleSize > 2400) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val source = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    } ?: return null

    if (!inkOnly) return source.asImageBitmap()

    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    for (index in pixels.indices) {
        val pixel = pixels[index]
        val originalAlpha = android.graphics.Color.alpha(pixel)
        val red = android.graphics.Color.red(pixel)
        val green = android.graphics.Color.green(pixel)
        val blue = android.graphics.Color.blue(pixel)
        val luminance = (299 * red + 587 * green + 114 * blue) / 1000
        val darkness = 255 - luminance
        val inkAlpha = (((darkness - 12) * 255) / 243).coerceIn(0, 255)
        val alpha = inkAlpha * originalAlpha / 255
        pixels[index] = if (alpha < 4) {
            android.graphics.Color.TRANSPARENT
        } else {
            (alpha shl 24) or 0x002A2028
        }
    }

    val inkBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    inkBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    source.recycle()
    return inkBitmap.asImageBitmap()
}
