<<<<<<< HEAD
package com.worldmates.messenger.ui.strapi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * Bottom Sheet для вибору стікерів/гіфок з Strapi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrapiContentPicker(
    onDismiss: () -> Unit,
    onItemSelected: (String) -> Unit,
    viewModel: StrapiContentViewModel = viewModel()
) {
    val allPacks by viewModel.allPacks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val filteredPacks by viewModel.filteredPacks.collectAsState()
    val selectedPack by viewModel.selectedPack.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок з кнопками
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Стікери і GIF",
                    style = MaterialTheme.typography.titleLarge
                )

                Row {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Оновити")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Закрити")
                    }
                }
            }

            // Вкладки
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.ALL,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.ALL) },
                    text = { Text("Всі") }
                )
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.STICKERS,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.STICKERS) },
                    text = { Text("Стікери") }
                )
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.GIFS,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.GIFS) },
                    text = { Text("GIF") }
                )
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.EMOJIS,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.EMOJIS) },
                    text = { Text("Емодзі") }
                )
            }

            if (isLoading && allPacks.isEmpty()) {
                // Індикатор завантаження
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredPacks.isEmpty()) {
                // Порожній стан
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Немає контенту")
                }
            } else {
                // Список паків або вміст паку
                if (selectedPack == null) {
                    // Показуємо список паків
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPacks) { pack ->
                            StrapiPackCard(
                                pack = pack,
                                onClick = { viewModel.selectPack(pack) }
                            )
                        }
                    }
                } else {
                    // Показуємо вміст паку
                    Column {
                        // Кнопка "Назад"
                        TextButton(
                            onClick = { viewModel.selectPack(null) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("← Назад до паків")
                        }

                        // Сітка з елементами
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedPack!!.items) { item ->
                                StrapiItemCard(
                                    item = item,
                                    onClick = {
                                        onItemSelected(item.url)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Картка паку стікерів
 */
@Composable
fun StrapiPackCard(
    pack: com.worldmates.messenger.data.model.StrapiContentPack,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Перший елемент як іконка паку
            if (pack.items.isNotEmpty()) {
                AsyncImage(
                    model = pack.items.first().url,
                    contentDescription = pack.name,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = pack.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            Text(
                text = "${pack.items.size} елементів",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Картка окремого стікера/GIF
 */
@Composable
fun StrapiItemCard(
    item: com.worldmates.messenger.data.model.StrapiContentItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.url,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
=======
package com.worldmates.messenger.ui.strapi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * Bottom Sheet для вибору стікерів/гіфок з Strapi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrapiContentPicker(
    onDismiss: () -> Unit,
    onItemSelected: (String) -> Unit,
    viewModel: StrapiContentViewModel = viewModel()
) {
    val allPacks by viewModel.allPacks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val filteredPacks by viewModel.filteredPacks.collectAsState()
    val selectedPack by viewModel.selectedPack.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок з кнопками
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Стікери і GIF",
                    style = MaterialTheme.typography.titleLarge
                )

                Row {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Оновити")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Закрити")
                    }
                }
            }

            // Вкладки
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.ALL,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.ALL) },
                    text = { Text("Всі") }
                )
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.STICKERS,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.STICKERS) },
                    text = { Text("Стікери") }
                )
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.GIFS,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.GIFS) },
                    text = { Text("GIF") }
                )
                Tab(
                    selected = selectedTab == StrapiContentViewModel.ContentTab.EMOJIS,
                    onClick = { viewModel.selectTab(StrapiContentViewModel.ContentTab.EMOJIS) },
                    text = { Text("Емодзі") }
                )
            }

            if (isLoading && allPacks.isEmpty()) {
                // Індикатор завантаження
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredPacks.isEmpty()) {
                // Порожній стан
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Немає контенту")
                }
            } else {
                // Список паків або вміст паку
                if (selectedPack == null) {
                    // Показуємо список паків
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPacks) { pack ->
                            StrapiPackCard(
                                pack = pack,
                                onClick = { viewModel.selectPack(pack) }
                            )
                        }
                    }
                } else {
                    // Показуємо вміст паку
                    Column {
                        // Кнопка "Назад"
                        TextButton(
                            onClick = { viewModel.selectPack(null) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("← Назад до паків")
                        }

                        // Сітка з елементами
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedPack!!.items) { item ->
                                StrapiItemCard(
                                    item = item,
                                    onClick = {
                                        onItemSelected(item.url)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Картка паку стікерів
 */
@Composable
fun StrapiPackCard(
    pack: com.worldmates.messenger.data.model.StrapiContentPack,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Перший елемент як іконка паку
            if (pack.items.isNotEmpty()) {
                AsyncImage(
                    model = pack.items.first().url,
                    contentDescription = pack.name,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Text(
                text = pack.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            Text(
                text = "${pack.items.size} елементів",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Картка окремого стікера/GIF
 */
@Composable
fun StrapiItemCard(
    item: com.worldmates.messenger.data.model.StrapiContentItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.url,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
>>>>>>> ee7949e8573d24ecdb81dbde3aeede26ef7efb2f
