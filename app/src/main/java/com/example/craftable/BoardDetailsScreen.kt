package com.example.craftable
import  com.example.craftable.PostDetails
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.craftable.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.craftable.Post

@Composable
fun BoardDetailScreen(
    boardName: String,
    userId: String,
    navController: NavController
) {
    val database = FirebaseDatabase.getInstance().reference
    var postIds by remember { mutableStateOf(listOf<String>()) }
    val postData = remember { mutableStateMapOf<String, Post>() }

    // Fetch post IDs
    LaunchedEffect(boardName, userId) {
        database.child("users").child(userId).child("boards").child(boardName).child("posts")
            .get()
            .addOnSuccessListener { snapshot ->
                val ids = snapshot.children.mapNotNull { it.key }
                postIds = ids

                // Fetch each post's data
                ids.forEach { postId ->
                    database.child("posts").child(postId).get()
                        .addOnSuccessListener { postSnapshot ->
                            val post = postSnapshot.getValue(Post::class.java)
                            if (post != null) {
                                postData[postId] = post
                            }
                        }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Board: $boardName",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (postIds.isEmpty()) {
            Text("No posts available yet.", fontStyle = FontStyle.Italic)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(postIds) { postId ->
                    val post = postData[postId]
                    if (post != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PostDetails(
                                postId = postId,
                                post = post,
                                userId = userId,
                                navController = navController
                            )
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text("Loading post...", fontStyle = FontStyle.Italic)
                            }
                        }
                    }
                }
            }
        }
    }
}
