package com.example.craftable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.craftable.navigation.Screen
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Reply


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Craftable",
                        color = Color(0xFF2E7D32), // dark green
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFE8F5E9) // light green background
                ),
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Screen.MakePost.route) }
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Make Post",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(Screen.Profile.route) }
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color(0xFF2E7D32)
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFFF1F8E9) // even softer green background for the whole page
    ) { padding ->

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            items((1..20).toList()) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Image Post $index",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFA5D6A7),
                                            Color(0xFF81C784)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Image Placeholder",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "A brief description about the post goes here, inspiring creativity!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { /* Open comments */ }) {
                                Icon(
                                    Icons.Default.Comment,
                                    contentDescription = "Comment",
                                    tint = Color.Gray
                                )
                            }
                            IconButton(onClick = { /* Like/Heart */ }) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Heart",
                                    tint = Color.Red
                                )
                            }
                            IconButton(onClick = { /* Save to board */ }) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = Color(0xFF2E7D32)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Comment list
                        Text("Comments:", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        val sampleComments =
                            listOf("Love this!", "So inspiring!", "How did you make it?")
                        sampleComments.forEach { comment ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(comment, fontSize = 14.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(onClick = { /* Upvote */ }) {
                                        Icon(
                                            Icons.Default.ThumbUp,
                                            contentDescription = "Upvote",
                                            tint = Color.Gray
                                        )
                                    }
                                    IconButton(onClick = { /* Downvote */ }) {
                                        Icon(
                                            Icons.Default.ThumbDown,
                                            contentDescription = "Downvote",
                                            tint = Color.Gray
                                        )
                                    }
                                    IconButton(onClick = { /* Reply */ }) {
                                        Icon(
                                            Icons.Default.Reply,
                                            contentDescription = "Reply",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
