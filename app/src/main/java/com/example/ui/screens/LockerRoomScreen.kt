package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.GameBalance
import com.example.core.ShopItem
import com.example.pixelart.PixelArtAssets
import com.example.ui.components.GbaCard
import com.example.ui.components.PixelSprite
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.DeepBlueAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.SoftGold
import com.example.ui.theme.WarmAmber
import com.example.viewmodel.AppUiState
import com.example.viewmodel.ScuolaRetroViewModel

@Composable
fun LockerRoomScreen(
    uiState: AppUiState,
    viewModel: ScuolaRetroViewModel
) {
    BackHandler { viewModel.handleGlobalBack() }

    val profile = uiState.activeProfile
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingStickerSlot by remember { mutableStateOf<Int?>(null) }

    val tabs = listOf("Bacheca Medaglie", "Il Mio Armadietto", "Spaccio Ricompense")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))
                )
            )
            .safeDrawingPadding()
            .testTag("locker_room_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.handleGlobalBack() },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("locker_back_hallway"),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlueAccent)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Torna in corridoio")
                    Spacer(Modifier.width(4.dp))
                    Text("Corridoio", style = MaterialTheme.typography.labelLarge)
                }

                Text(
                    text = "ARMADIETTO",
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 16.sp,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CreamBackground,
                    border = BorderStroke(2.dp, SoftGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Stelle", tint = SoftGold)
                        Text(
                            text = "${profile?.stars ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                    }
                }
            }

            // 3 GBA Tabs
            GbaCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(0.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CreamSurface,
                    contentColor = DeepBlueAccent
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            modifier = Modifier.testTag("locker_tab_$idx"),
                            text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> MedalBoardTab(uiState = uiState)
                        1 -> MyLockerTab(
                            uiState = uiState,
                            onSlotClick = { editingStickerSlot = it },
                            onEquipCompanion = { viewModel.equipCompanion(it) },
                            onEquipFrame = { viewModel.equipFrame(it) }
                        )
                        2 -> RewardShopTab(
                            uiState = uiState,
                            onPurchase = { viewModel.purchaseShopItem(it) }
                        )
                    }
                }
            }
        }

        // Sticker Slot Mini-Picker Modal (with "Rimuovi")
        editingStickerSlot?.let { slotIdx ->
            val ownedStickers = uiState.shopCatalog.filter {
                it.category == "sticker" && profile?.ownedItems?.contains(it.id) == true
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { editingStickerSlot = null },
                contentAlignment = Alignment.Center
            ) {
                GbaCard(
                    modifier = Modifier
                        .padding(20.dp)
                        .widthIn(max = 460.dp)
                        .clickable(enabled = false) {}
                        .testTag("sticker_picker_modal")
                ) {
                    Text(
                        text = "Scegli adesivo per Slot ${slotIdx + 1}",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlueAccent
                    )
                    Spacer(Modifier.height(10.dp))
                    if (ownedStickers.isEmpty()) {
                        Text(
                            text = "Non hai ancora adesivi! Visita lo Spaccio Ricompense per sbloccarne qualcuno con le tue stelle.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            items(ownedStickers, key = { it.id }) { st ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setLockerSticker(slotIdx, st.id)
                                            editingStickerSlot = null
                                        },
                                    color = CreamSurface,
                                    border = BorderStroke(2.dp, GameBalance.GBA_FRAME_DARK)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        PixelSprite(
                                            bitmap = PixelArtAssets.getStickerIcon(st.id),
                                            contentDescription = st.name,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(st.name, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setLockerSticker(slotIdx, "")
                                editingStickerSlot = null
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("sticker_remove_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Rimuovi")
                        }
                        Button(
                            onClick = { editingStickerSlot = null },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Chiudi")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MedalBoardTab(uiState: AppUiState) {
    val earnedIds = uiState.activeProfile?.medals?.toSet() ?: emptySet()
    // Show all active packs + any archived packs whose medal was already earned
    val visiblePacks = uiState.allPacksIncludingArchived.filter { !it.archived || it.packId in earnedIds }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(visiblePacks, key = { it.packId }) { pack ->
            val earned = pack.packId in earnedIds
            val subjColor = GameBalance.subjectColor(pack.subject)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (earned) Color(0xFFFFFBEB) else CreamSurface,
                border = BorderStroke(2.5.dp, if (earned) SoftGold else Color(0xFFCBD5E1)),
                shadowElevation = if (earned) 4.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Double-ring retro medal emblem
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(
                                if (earned) {
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                                    )
                                } else {
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                    )
                                }
                            )
                            .border(
                                width = 3.dp,
                                color = if (earned) Color(0xFF92400E) else Color(0xFF94A3B8),
                                shape = androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .border(
                                    width = 1.5.dp,
                                    color = if (earned) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (earned) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                contentDescription = if (earned) "Conquistata" else "Bloccata",
                                tint = if (earned) Color(0xFF78350F) else Color(0xFF64748B),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = pack.medalName,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = pack.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subjColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (earned) Color(0xFFD1FAE5) else Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = if (earned) "Conquistata!" else "Sconfiggi ${pack.bossName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (earned) EmeraldSuccess else InkSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyLockerTab(
    uiState: AppUiState,
    onSlotClick: (Int) -> Unit,
    onEquipCompanion: (String) -> Unit,
    onEquipFrame: (String) -> Unit
) {
    val profile = uiState.activeProfile ?: return
    val ownedSet = profile.ownedItems.toSet()
    val ownedCompanions = uiState.shopCatalog.filter { it.category == "companion" && it.id in ownedSet }
    val ownedFrames = uiState.shopCatalog.filter { it.category == "frame" && it.id in ownedSet }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 6 Sticker Slots
        item {
            Text("I 6 Slot Adesivi dell'Armadietto", style = MaterialTheme.typography.titleLarge, color = DeepBlueAccent)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (slotIdx in 0..5) {
                    val stickerId = profile.lockerStickers.getOrElse(slotIdx) { "" }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSlotClick(slotIdx) }
                            .testTag("locker_slot_$slotIdx"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (stickerId.isNotBlank()) Color(0xFFE0F2FE) else CreamSurface,
                        border = BorderStroke(2.dp, GameBalance.GBA_FRAME_DARK)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (stickerId.isNotBlank()) {
                                PixelSprite(
                                    bitmap = PixelArtAssets.getStickerIcon(stickerId),
                                    contentDescription = "Adesivo slot ${slotIdx + 1}",
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = "Aggiungi adesivo", tint = InkSecondary)
                                    Text("Slot ${slotIdx + 1}", fontSize = 11.sp, color = InkSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Companion selector
        item {
            Text("Mascotte al tuo fianco nell'Hub", style = MaterialTheme.typography.titleLarge, color = DeepBlueAccent)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onEquipCompanion("") },
                    modifier = Modifier.heightIn(min = 52.dp),
                    border = BorderStroke(if (profile.equippedCompanion.isEmpty()) 3.dp else 1.5.dp, DeepBlueAccent)
                ) {
                    Text("Nessuna")
                }
                ownedCompanions.forEach { pet ->
                    val selected = profile.equippedCompanion == pet.id
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onEquipCompanion(pet.id) },
                        color = if (selected) Color(0xFFDBEAFE) else CreamSurface,
                        border = BorderStroke(if (selected) 3.dp else 1.5.dp, if (selected) DeepBlueAccent else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PixelSprite(
                                bitmap = PixelArtAssets.getCompanionSprite(pet.id, 0),
                                contentDescription = pet.name,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(pet.name, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Frame selector
        item {
            Text("Cornice Profilo", style = MaterialTheme.typography.titleLarge, color = DeepBlueAccent)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ownedFrames.forEach { frame ->
                    val selected = profile.equippedFrame == frame.id
                    val fColor = GameBalance.frameColor(frame.id)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onEquipFrame(frame.id) },
                        color = if (selected) fColor.copy(alpha = 0.15f) else CreamSurface,
                        border = BorderStroke(if (selected) 3.dp else 2.dp, fColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(fColor)
                            )
                            Text(frame.name, style = MaterialTheme.typography.labelLarge, color = InkPrimary)
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = "Attiva", tint = fColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardShopTab(
    uiState: AppUiState,
    onPurchase: (ShopItem) -> Unit
) {
    val profile = uiState.activeProfile ?: return
    val ownedSet = profile.ownedItems.toSet()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 210.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.shopCatalog, key = { it.id }) { item ->
            val owned = item.id in ownedSet
            val canAfford = profile.stars >= item.price
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CreamSurface,
                border = BorderStroke(2.dp, if (owned) EmeraldSuccess else Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (item.category) {
                        "companion" -> PixelSprite(
                            bitmap = PixelArtAssets.getCompanionSprite(item.id, 0),
                            contentDescription = item.name,
                            modifier = Modifier.size(48.dp)
                        )
                        "sticker" -> PixelSprite(
                            bitmap = PixelArtAssets.getStickerIcon(item.id),
                            contentDescription = item.name,
                            modifier = Modifier.size(48.dp)
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CreamBackground)
                                .border(5.dp, GameBalance.frameColor(item.id), RoundedCornerShape(10.dp))
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val categoryLabel = when (item.category) {
                        "companion" -> "Mascotte"
                        "sticker" -> "Adesivo"
                        else -> "Cornice"
                    }
                    Text(categoryLabel, style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                    Button(
                        onClick = { onPurchase(item) },
                        enabled = !owned && canAfford,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("shop_buy_${item.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (owned) EmeraldSuccess else SoftGold,
                            contentColor = if (owned) Color.White else Color(0xFF451A03)
                        )
                    ) {
                        if (owned) {
                            Text("Già tuo!", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${item.price}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
