package com.example.craftable

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.craftable.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val database = FirebaseDatabase.getInstance().reference
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(true) {
        database.child("posts").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val postList = mutableListOf<Post>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)?.copy(id = postSnapshot.key ?: "")
                    if (post != null) postList.add(post)
                }
                posts = postList.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Craftable",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.MakePost.route) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Make Post", tint = Color(0xFF2E7D32))
                    }

                    var profileBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

                    LaunchedEffect(Unit) {
                        currentUserId?.let { uid ->
                            val userRef = database.child("users").child(uid).child("profilePic")
                            userRef.get().addOnSuccessListener { snapshot ->
                                val base64 = snapshot.value as? String
                                base64?.let {
                                    val bytes = Base64.decode(it, Base64.DEFAULT)
                                    profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }
                            }
                        }
                    }

                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap!!.asImageBitmap(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Transparent, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black, CircleShape)
                            )
                        }
                    }
                }
            )
        },
        containerColor = Color(0xFFF1F8E9)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(posts) { post ->
                PostCard(post = post, navController = navController, currentUserId = currentUserId, database = database)
            }
        }
    }
}

@Composable
fun PostCard(post: Post, navController: NavController, currentUserId: String?, database: DatabaseReference) {
    val postId = post.id
    val imageBytes = Base64.decode(post.imageBase64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
    val formattedTime = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date(post.timestamp))
    var liked by remember { mutableStateOf(post.likedBy?.containsKey(currentUserId) == true) }
    var newComment by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
            Text(post.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(post.description, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            Text("Posted on $formattedTime", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(onClick = {
                    liked = !liked
                    val likeRef = database.child("posts").child(postId)
                    likeRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val postData = currentData.getValue(Post::class.java) ?: return Transaction.success(currentData)
                            val updatedLikes = if (liked) postData.likes + 1 else maxOf(0, postData.likes - 1)
                            val updatedLikedBy = postData.likedBy?.toMutableMap() ?: mutableMapOf()
                            currentUserId?.let {
                                if (liked) updatedLikedBy[it] = true else updatedLikedBy.remove(it)
                            }
                            currentData.child("likes").value = updatedLikes
                            currentData.child("likedBy").value = updatedLikedBy
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
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
                Text("${post.likes}", color = Color.Gray)

                IconButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Filled.Bookmark, contentDescription = "Save to Board", tint = Color.Gray)
                }
            }

            if (showSaveDialog) {
                SaveToBoardDialog(postId = post.id) { showSaveDialog = false }
            }

            post.comments?.forEach { (commentId, comment) ->
                YouTubeCommentCard(
                    postId = postId,
                    commentId = commentId,
                    comment = comment,
                    replyingToComment = replyingToComment,
                    onReply = { replyingToComment = it }
                )
            }

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
                    if (newComment.isNotBlank()) {
                        val commentRef = if (replyingToComment != null) {
                            database.child("posts").child(postId)
                                .child("comments").child(replyingToComment!!)
                                .child("replies").push()
                        } else {
                            database.child("posts").child(postId).child("comments").push()
                        }

                        val comment = Comment(
                            userId = FirebaseAuth.getInstance().currentUser?.uid,
                            text = newComment.trim(),
                            timestamp = System.currentTimeMillis()
                        )

                        commentRef.setValue(comment).addOnSuccessListener {
                            newComment = "" // ✅ Clears the input field after posting
                            replyingToComment = null
                        }.addOnFailureListener {
                            Log.e("Comment", "Failed to post comment", it)
                        }
                    }
                }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send Comment")
                }
            }
        }
    }
}
@Composable
fun YouTubeCommentCard(
    postId: String,
    commentId: String,
    comment: Comment,
    replyingToComment: String?,
    onReply: (String?) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var likedComment by remember { mutableStateOf(comment.likedBy?.containsKey(currentUserId) == true) }

    Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp)) {
        UserProfile(userId = comment.userId)
        val commentTime = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date(comment.timestamp))
        Text("• $commentTime", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 44.dp))

        Text(comment.text ?: "", fontSize = 14.sp, modifier = Modifier.padding(start = 44.dp, top = 4.dp))


        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 44.dp, top = 4.dp)) {
            IconButton(onClick = {
                likedComment = !likedComment
                val commentLikeRef = database.child("posts").child(postId).child("comments").child(commentId)
                commentLikeRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val commentData = currentData.getValue(Comment::class.java)
                            ?: return Transaction.success(currentData)
                        val updatedLikes = if (likedComment) commentData.likes + 1 else maxOf(0, commentData.likes - 1)
                        val updatedLikedBy = commentData.likedBy?.toMutableMap() ?: mutableMapOf()
                        currentUserId?.let {
                            if (likedComment) updatedLikedBy[it] = true
                            else updatedLikedBy.remove(it)
                        }
                        currentData.child("likes").value = updatedLikes
                        currentData.child("likedBy").value = updatedLikedBy
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        error?.let { Log.e("CommentLike", "Failed to like comment", it.toException()) }
                    }
                })
            }) {
                Icon(Icons.Filled.ThumbUp, contentDescription = "Like Comment", tint = if (likedComment) Color.Blue else Color.Gray)
            }
            Text("${comment.likes}", fontSize = 12.sp, modifier = Modifier.padding(end = 16.dp))

            Text(
                "Reply",
                modifier = Modifier
                    .clickable { onReply(commentId) }
                    .padding(horizontal = 8.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        comment.replies?.forEach { (_, reply) ->
            val replyTime = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date(reply.timestamp))
            Column(modifier = Modifier.padding(start = 64.dp, top = 4.dp)) {
                UserProfile(userId = reply.userId, imageSize = 30.dp)
                Text("• $replyTime", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 36.dp))
                Text(reply.text ?: "", fontSize = 13.sp, modifier = Modifier.padding(start = 36.dp, top = 2.dp))

            }
        }
    }
}

@Composable
fun UserProfile(userId: String?, modifier: Modifier = Modifier, imageSize: Dp = 36.dp) {
    var username by remember { mutableStateOf("null") }
    var profileBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(userId) {
        userId?.let {
            val userRef = FirebaseDatabase.getInstance().reference.child("users").child(it)
            userRef.child("username").get().addOnSuccessListener { snapshot ->
                username = snapshot.value as? String ?: "null"
            }
            userRef.child("profilePic").get().addOnSuccessListener { snapshot ->
                val base64 = snapshot.value as? String
                base64?.let {
                    val bytes = Base64.decode(it, Base64.DEFAULT)
                    profileBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (profileBitmap != null) {
            Image(
                bitmap = profileBitmap!!.asImageBitmap(),
                contentDescription = "Profile Picture",
                modifier = Modifier.size(imageSize).padding(end = 8.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(imageSize)
                    .background(Color.Black, shape = CircleShape)
                    .padding(end = 8.dp)
            )
        }
        Text(text = username, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
@Composable
fun SaveToBoardDialog(
    postId: String,
    onDismiss: () -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var boardList by remember { mutableStateOf(listOf<String>()) }
    var showNewBoardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current // ✅ Moved to top of the composable

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            database.child("users").child(currentUserId).child("boards")
                .get()
                .addOnSuccessListener { snapshot ->
                    boardList = snapshot.children.mapNotNull { it.key }
                }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Save to Board", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                boardList.forEach { board ->
                    Button(
                        onClick = {
                            currentUserId?.let { userId ->
                                database.child("users").child(userId).child("boards").child(board)
                                    .child("posts").child(postId).setValue(true)

                                // ✅ Safe Toast call
                                Toast.makeText(context, "Saved to $board", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(board)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showNewBoardDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Board")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Board")
                }
            }
        }
    }

    if (showNewBoardDialog) {
        NewBoardDialog(onDismiss = { showNewBoardDialog = false })
    }
}

@Composable
fun NewBoardDialog(onDismiss: () -> Unit) {
    val database = FirebaseDatabase.getInstance().reference
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var boardName by remember { mutableStateOf("") }
    var boardDescription by remember { mutableStateOf("") }
    val context = LocalContext.current // ✅ Moved to top of the composable

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create New Board", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = boardName,
                    onValueChange = { boardName = it },
                    label = { Text("Board Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = boardDescription,
                    onValueChange = { boardDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (boardName.isNotEmpty()) {
                            currentUserId?.let { userId ->
                                database.child("users").child(userId).child("boards").child(boardName)
                                    .setValue(mapOf("description" to boardDescription))

                                // ✅ Safe Toast call
                                Toast.makeText(context, "Board Created", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        } else {
                            // ✅ Safe Toast call
                            Toast.makeText(context, "Board name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create")
                }
            }
        }
    }
}
