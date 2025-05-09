package com.example.craftable.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object MakePost : Screen("make_post")
    object Profile : Screen("profile")
}