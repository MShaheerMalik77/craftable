package com.example.craftable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.craftable.navigation.Screen
import com.example.craftable.ui.theme.CraftableTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            CraftableTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "auth"
                ) {
                    composable("auth") { AuthGate(navController) }
                    composable(Screen.Login.route) { LoginScreen(navController) }
                    composable(Screen.Register.route) { RegisterScreen(navController) }
                    composable(Screen.Dashboard.route) { DashboardScreen(navController) }
                    composable(Screen.MakePost.route) { MakePostScreen(navController) }
                    composable(Screen.Profile.route) { ProfileScreen(navController) }
                    composable("boardDetail/{boardName}") { backStackEntry ->
                        val boardName = backStackEntry.arguments?.getString("boardName") ?: ""
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@composable
                        BoardDetailScreen(boardName, userId, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun AuthGate(navController: androidx.navigation.NavHostController) {
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(0)
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(0)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
