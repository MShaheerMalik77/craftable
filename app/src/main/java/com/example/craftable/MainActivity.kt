package com.example.craftable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.craftable.navigation.Screen
import com.example.craftable.ui.theme.CraftableTheme
import com.example.craftable.LoginScreen
import com.example.craftable.RegisterScreen
import com.example.craftable.DashboardScreen
import com.example.craftable.MakePostScreen
import com.example.craftable.ProfileScreen
import com.google.firebase.FirebaseApp



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = Screen.Login.route) {
                composable(Screen.Login.route) { LoginScreen(navController) }
                composable(Screen.Register.route) { RegisterScreen(navController) }
                composable(Screen.Dashboard.route) { DashboardScreen(navController) }
                composable(Screen.MakePost.route) { MakePostScreen(navController) }
                composable(Screen.Profile.route) { ProfileScreen(navController) }
            }
        }
    }
}
