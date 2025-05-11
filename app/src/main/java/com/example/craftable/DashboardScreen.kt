package com.example.craftable

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.craftable.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }

    LaunchedEffect(Unit) {
        val database = FirebaseDatabase.getInstance().reference
        val postsRef = database.child("posts")
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedPosts = mutableListOf<Post>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    val id = postSnapshot.key
                    post?.copy(id = id ?: "")?.let { loadedPosts.add(it) }
                }
                posts = loadedPosts.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Dashboard", "Failed to load posts", error.toException())
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Craftable", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFE8F5E9)),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.MakePost.route) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Make Post", tint = Color(0xFF2E7D32))
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile", tint = Color(0xFF2E7D32))
                    }
                }
            )
        },
        containerColor = Color(0xFFF1F8E9)
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(posts) { post ->
                var liked by remember { mutableStateOf(false) }
                var showDialog by remember { mutableStateOf(false) }
                var selectedBoard by remember { mutableStateOf<String?>(null) }
                var newComment by remember { mutableStateOf("") }

                val postId = post.id
                val database = FirebaseDatabase.getInstance().reference

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(post.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(8.dp))

                        val imageBitmap = remember(post.imageBase64) {
                            try {
                                val bytes = Base64.decode(post.imageBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (e: Exception) {
                                Log.e("ImageDecode", "Failed to decode image", e)
                                null
                            }
                        }

                        imageBitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(post.description, fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { /* Comment input toggle if needed */ }) {
                                Icon(Icons.Default.Comment, contentDescription = "Comment", tint = Color.Gray)
                            }

                            IconButton(onClick = {
                                liked = !liked
                                val likeRef = database.child("posts").child(postId).child("likes")
                                likeRef.runTransaction(object : Transaction.Handler {
                                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                                        var currentLikes = currentData.getValue(Int::class.java) ?: 0
                                        currentLikes = if (liked) currentLikes + 1 else maxOf(0, currentLikes - 1)
                                        currentData.value = currentLikes
                                        return Transaction.success(currentData)
                                    }

                                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                                        if (error != null) Log.e("Like", "Like failed", error.toException())
                                    }
                                })
                            }) {
                                Icon(
                                    if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = Color.Red
                                )
                            }

                            IconButton(onClick = { showDialog = true }) {
                                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color(0xFF2E7D32))
                            }
                        }

                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = { showDialog = false },
                                title = { Text("Save to Board") },
                                text = {
                                    Column {
                                        listOf("Tables", "Chairs", "Shelves").forEach { board ->
                                            TextButton(onClick = {
                                                selectedBoard = board
                                                showDialog = false
                                            }) {
                                                Text(board)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { showDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        selectedBoard?.let {
                            Text("Saved to $it board", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()

                        Text("Comments:", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))

                        post.comments?.values?.forEach { comment ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                comment.text?.let { text ->
                                    Text(text, fontSize = 14.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            label = { Text("Add a comment...") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (newComment.isNotBlank()) {
                                    val commentRef = database.child("posts").child(postId).child("comments").push()
                                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                                    val comment = Comment(text = newComment, userId = currentUserId)
                                    commentRef.setValue(comment)
                                    newComment = ""
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Post Comment")
                        }
                    }
                }
            }
        }
    }
}
