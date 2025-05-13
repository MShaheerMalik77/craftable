package com.example.craftable

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.craftable.Comment
import com.example.craftable.Post
import com.example.craftable.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.util.Base64
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val userEmail = FirebaseAuth.getInstance().currentUser?.email
    var selectedTab by remember { mutableStateOf(0) }
    var pfpUri by remember { mutableStateOf<Uri?>(null) }
    var username by remember { mutableStateOf("Loading...") }
    var showEditDialog by remember { mutableStateOf(false) }
    var profilePicBase64 by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            pfpUri = uri

            // Convert selected image to Base64 and upload to Firebase
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

            userId?.let { uid ->
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("profilePic")
                    .setValue(base64Image)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    // Fetch username from database
    LaunchedEffect(userId) {
        userId?.let {
            val ref = FirebaseDatabase.getInstance().getReference("users").child(it).child("username")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    username = snapshot.getValue(String::class.java) ?: "Unknown"
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // Fetch profile picture from database
        userId?.let {
            val ref = FirebaseDatabase.getInstance().getReference("users").child(it).child("profilePic")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    profilePicBase64 = snapshot.getValue(String::class.java)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    if (showEditDialog) {
        var newUsername by remember { mutableStateOf(username) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Username") },
            text = {
                OutlinedTextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("New Username") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    userId?.let {
                        FirebaseDatabase.getInstance().getReference("users")
                            .child(it).child("username").setValue(newUsername)
                    }
                    username = newUsername
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = Color(0xFF2E7D32)) },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFFE8F5E9))
            )
        },
        containerColor = Color(0xFFF1F8E9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA5D6A7))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profilePicBase64 != null) {
                    val imageBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                } else {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(100.dp))
                }
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Change Picture",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF66BB6A), CircleShape)
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                username,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                modifier = Modifier.clickable { showEditDialog = true }
            )
            Text(userEmail ?: "No email", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF2E7D32)
            ) {
                listOf("Boards", "Comments", "Pins", "Posts").forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> BoardsTab(userId, navController)
                1 -> CommentsTab(userId)
                2 -> PinsTab(userId)
                3 -> PostsTab(userId)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate(Screen.Login.route) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BoardsTab(userId: String?, navController: NavController) {
    // Define a data class for a board
    data class Board(val name: String, val description: String?)

    var boards by remember { mutableStateOf(listOf<Board>()) }

    LaunchedEffect(userId) {
        if (userId != null) {
            val ref = FirebaseDatabase.getInstance().getReference("users/$userId/boards")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val boardList = snapshot.children.map { boardSnapshot ->
                        val name = boardSnapshot.key ?: return@map null
                        val description = boardSnapshot.child("description").getValue(String::class.java)
                        Board(name, description)
                    }.filterNotNull()
                    boards = boardList
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(boards) { board ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("boardDetail/${board.name}")
                    },
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(board.name, fontWeight = FontWeight.SemiBold)
                    board.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}



@Composable
fun CommentsTab(userId: String?) {
    var userComments by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    LaunchedEffect(userId) {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        postsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = mutableListOf<Pair<String, String>>()
                for (postSnap in snapshot.children) {
                    val postTitle = postSnap.child("title").getValue(String::class.java) ?: "Unknown Post"
                    val commentsSnap = postSnap.child("comments")
                    for (commentSnap in commentsSnap.children) {
                        val comment = commentSnap.getValue(Comment::class.java)
                        comment?.let {
                            if (it.userId == userId) {
                                val commentText = it.text ?: "(no text)"
                                comments.add(postTitle to commentText)
                            }
                        }
                    }
                }
                userComments = comments
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors here if needed
            }
        })
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(userComments) { (postTitle, commentText) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("On: $postTitle", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(commentText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun PinsTab(userId: String?) {
    var pins by remember { mutableStateOf(listOf<Post>()) }

    LaunchedEffect(userId) {
        val pinsRef = FirebaseDatabase.getInstance().getReference("pins/$userId")
        pinsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pinnedPosts = mutableListOf<Post>()
                for (pinSnap in snapshot.children) {
                    val post = pinSnap.getValue(Post::class.java)
                    post?.let { pinnedPosts.add(it) }
                }
                pins = pinnedPosts
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(pins) { pinnedPost ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(pinnedPost.title, fontWeight = FontWeight.Bold)
                    Text(pinnedPost.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PostsTab(userId: String?) {
    var userPosts by remember { mutableStateOf(listOf<Post>()) }

    LaunchedEffect(userId) {
        val postsRef = FirebaseDatabase.getInstance().getReference("posts")
        postsRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val postsList = mutableListOf<Post>()
                for (postSnap in snapshot.children) {
                    val post = postSnap.getValue(Post::class.java)
                    post?.let { postsList.add(it) }
                }
                userPosts = postsList
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(userPosts) { userPost ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(userPost.title, fontWeight = FontWeight.Bold)
                    Text(userPost.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

