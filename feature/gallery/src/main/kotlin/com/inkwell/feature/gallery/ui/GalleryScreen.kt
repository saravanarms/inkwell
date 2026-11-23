package com.inkwell.feature.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkwell.core.model.CanvasSpec
import com.inkwell.data.project.storage.ProjectHeader
import com.inkwell.data.repository.ProjectRepository
import kotlinx.coroutines.launch
import java.io.File

/**
 * Inkwell Project Gallery Screen.
 * Displays user artwork projects, enables cold-start thumbnail viewing,
 * and launches new or existing canvases.
 */
@Composable
fun GalleryScreen(
    repository: ProjectRepository,
    onOpenProject: (File) -> Unit,
    onCanvasCreated: (title: String, spec: CanvasSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<ProjectHeader>>(emptyList()) }
    var showNewDialog by remember { mutableStateOf(false) }

    fun refreshProjects() {
        scope.launch {
            projects = repository.listProjects()
        }
    }

    LaunchedEffect(Unit) {
        refreshProjects()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141418))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inkwell",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Gallery (${projects.size} Artworks)",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // "+ New Canvas" Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3D5AFE))
                        .clickable { showNewDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ New Canvas",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Project Grid
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🎨", fontSize = 48.sp)
                        Text(
                            text = "No artworks created yet",
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap '+ New Canvas' to begin painting",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects, key = { it.projectId }) { header ->
                        ProjectCard(
                            header = header,
                            onClick = { onOpenProject(header.file) },
                            onDuplicate = {
                                scope.launch {
                                    repository.duplicateProject(header.file)
                                    refreshProjects()
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    repository.deleteProject(header.file)
                                    refreshProjects()
                                }
                            }
                        )
                    }
                }
            }
        }

        // New Canvas Dialog
        if (showNewDialog) {
            NewCanvasDialog(
                onDismiss = { showNewDialog = false },
                onCanvasCreated = { title, spec ->
                    showNewDialog = false
                    onCanvasCreated(title, spec)
                }
            )
        }
    }
}
