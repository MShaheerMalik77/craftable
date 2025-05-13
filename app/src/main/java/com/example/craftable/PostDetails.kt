package com.example.craftable

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.craftable.navigation.Screen
import com.google.firebase.database.*
import java.util.*

@Composable
fun PostDetails(
    post: Post,
    postId: String,
    userId: String,
    navController: NavController
) {
    val database = FirebaseDatabase.getInstance().reference
    val currentUserId = userId

    // Real-time like & likedBy state
    var postLikes by remember { mutableStateOf(post.likes) }
    var liked by remember { mutableStateOf(post.likedBy?.containsKey(currentUserId) == true) }

    // Realtime comment state
    val comments = remember { mutableStateMapOf<String, Comment>() }

    // Input state
    var newComment by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Firebase Listeners
    LaunchedEffect(postId) {
        // Listen for post like updates
        val postRef = database.child("posts").child(postId)
        postRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedPost = snapshot.getValue(Post::class.java)
                if (updatedPost != null) {
                    postLikes = updatedPost.likes
                    liked = updatedPost.likedBy?.containsKey(currentUserId) == true
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostDetails", "Failed to load post data", error.toException())
            }
        })

        // Listen for comment changes
        val commentRef = postRef.child("comments")
        commentRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                comments.clear()
                for (child in snapshot.children) {
                    val comment = child.getValue(Comment::class.java)
                    if (comment != null) {
                        comments[child.key!!] = comment
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PostDetails", "Failed to load comments", error.toException())
            }
        })
    }

    // UI layout
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Post Image
        val bitmap = try {
            val imageBytes = Base64.decode(post.imageBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            Log.e("PostDetails", "Invalid image data", e)
            null
        }

        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // Post Description
        Text(post.description, style = MaterialTheme.typography.bodyLarge)

        // Like + Save Row
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = {
                val likeRef = database.child("posts").child(postId)
                likeRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val postData = currentData.getValue(Post::class.java) ?: return Transaction.success(currentData)
                        val updatedLikes = if (!liked) postData.likes + 1 else maxOf(0, postData.likes - 1)
                        val updatedLikedBy = postData.likedBy?.toMutableMap() ?: mutableMapOf()

                        if (!liked) updatedLikedBy[currentUserId] = true
                        else updatedLikedBy.remove(currentUserId)

                        currentData.child("likes").value = updatedLikes
                        currentData.child("likedBy").value = updatedLikedBy
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                        error?.let { Log.e("Like", "Failed to like post", it.toException()) }
                    }
                })
            }) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) Color.Red else Color.Gray
                )
            }

            Text("$postLikes", color = Color.Gray)

            IconButton(onClick = { showSaveDialog = true }) {
                Icon(Icons.Filled.Bookmark, contentDescription = "Save to Board", tint = Color.Gray)
            }

            if (showSaveDialog) {
                SaveToBoardDialog(postId = postId) {
                    showSaveDialog = false
                }
            }
        }

        // Comments
        comments.forEach { (commentId, comment) ->
            YouTubeCommentCard(
                postId = postId,
                commentId = commentId,
                comment = comment,
                replyingToComment = replyingToComment,
                onReply = { replyingToComment = it }
            )
        }

        // Add Comment UI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                label = { Text(if (replyingToComment != null) "Reply" else "Comment") },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val commentRef = if (replyingToComment != null) {
                    database.child("posts").child(postId)
                        .child("comments").child(replyingToComment!!).child("replies").push()
                } else {
                    database.child("posts").child(postId).child("comments").push()
                }

                val comment = Comment(
                    userId = currentUserId,
                    text = newComment,
                    timestamp = System.currentTimeMillis()
                )

                commentRef.setValue(comment).addOnCompleteListener {
                    if (it.isSuccessful) newComment = ""
                    replyingToComment = null
                }
            }) {
                Icon(Icons.Filled.Send, contentDescription = "Send Comment")
            }
        }
    }
}
