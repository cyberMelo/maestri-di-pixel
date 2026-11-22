package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.GameBalance
import com.example.pixelart.Facing
import com.example.pixelart.PixelArtAssets
import com.example.ui.components.GbaCard
import com.example.ui.components.PixelSprite
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HudNightBg
import com.example.ui.theme.IceWhiteText
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SoftGold
import kotlinx.coroutines.delay

@Composable
fun BootScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A5F), Color(0xFF0F172A))
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        GbaCard(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 460.dp)
                .testTag("boot_screen"),
            borderColor = SoftGold
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(2.dp, GameBalance.GBA_FRAME_DARK, RoundedCornerShape(10.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_school_hero),
                        contentDescription = "Cortile di Maestri di Pixel",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PixelSprite(
                            bitmap = PixelArtAssets.getCharacterSprite("avatar_0", Facing.DOWN, 1),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                        PixelSprite(
                            bitmap = PixelArtAssets.getCompanionSprite("pet_owl", 0),
                            contentDescription = null,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "MAESTRI DI PIXEL",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Preparazione della scuola...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = EmeraldSuccess
                )
            }
        }
    }
}

@Composable
fun ProfileSelectScreen(
    onCreateProfile: (name: String, avatarId: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableIntStateOf(0) }
    var animStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(360)
            animStep = (animStep + 1) % 3
        }
    }

    val cleanName = name.trim()
    val canStart = cleanName.length in 1..16
    val avatarNames = listOf("Blu", "Rosa", "Verde", "Viola")

    val isDark = LocalDarkTheme.current
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDark) listOf(HudNightBg, Color(0xFF1E3A5F)) else listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8), Color(0xFF7BC96F))
                )
            )
            .safeDrawingPadding()
    ) {
        val useSplitLandscape = maxWidth >= 660.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            GbaCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = if (useSplitLandscape) 860.dp else 560.dp)
                    .testTag("profile_select_screen")
            ) {
                if (useSplitLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(0.95f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.5.dp, GameBalance.GBA_FRAME_DARK, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_school_hero),
                                    contentDescription = "Illustrazione di Maestri di Pixel",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "MAESTRI DI PIXEL",
                                fontFamily = SilkscreenFontFamily,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Scrivi il tuo nome, scegli l'avatar ed esplora tutte le 6 aule e sfide della scuola!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(modifier = Modifier.weight(1.15f)) {
                            ProfileFormContent(
                                name = name,
                                onNameChange = { if (it.length <= 16) name = it },
                                selectedAvatar = selectedAvatar,
                                onSelectAvatar = { selectedAvatar = it },
                                animStep = animStep,
                                avatarNames = avatarNames,
                                compact = true,
                                canStart = canStart,
                                onStart = { onCreateProfile(cleanName, selectedAvatar) }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.5.dp, GameBalance.GBA_FRAME_DARK, RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_school_hero),
                            contentDescription = "Illustrazione di Maestri di Pixel",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Benvenuto in Maestri di Pixel!",
                        fontFamily = SilkscreenFontFamily,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Scrivi il tuo nome e scegli il tuo personaggio per esplorare la scuola.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    ProfileFormContent(
                        name = name,
                        onNameChange = { if (it.length <= 16) name = it },
                        selectedAvatar = selectedAvatar,
                        onSelectAvatar = { selectedAvatar = it },
                        animStep = animStep,
                        avatarNames = avatarNames,
                        compact = false,
                        canStart = canStart,
                        onStart = { onCreateProfile(cleanName, selectedAvatar) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileFormContent(
    name: String,
    onNameChange: (String) -> Unit,
    selectedAvatar: Int,
    onSelectAvatar: (Int) -> Unit,
    animStep: Int,
    avatarNames: List<String>,
    compact: Boolean,
    canStart: Boolean,
    onStart: () -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Il tuo nome (max 16 lettere)") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_name_input")
    )

    Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
    Text(
        text = "Scegli il tuo avatar:",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (id in 0..3) {
            val isSelected = selectedAvatar == id
            val frame = if (isSelected) animStep else 0
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectAvatar(id) }
                    .testTag("avatar_option_$id"),
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = if (isSelected) 3.dp else 2.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PixelSprite(
                        bitmap = PixelArtAssets.getCharacterSprite("avatar_$id", Facing.DOWN, frame),
                        contentDescription = "Avatar ${avatarNames[id]}",
                        modifier = Modifier.size(if (compact) 46.dp else 52.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = avatarNames[id],
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
    Button(
        onClick = onStart,
        enabled = canStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("profile_start_button"),
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = IceWhiteText)
    ) {
        Text(
            text = "Inizia l'avventura!",
            style = MaterialTheme.typography.titleLarge,
            color = IceWhiteText
        )
    }
}
