package com.example.rxcare.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rxcare.domain.model.UserRole
import com.example.rxcare.presentation.ui.auth.CreatePharmacistScreen
import com.example.rxcare.presentation.ui.auth.SignInScreen
import com.example.rxcare.presentation.ui.auth.SignUpScreen
import com.example.rxcare.presentation.ui.chat.ChatScreen
import com.example.rxcare.presentation.ui.home.HomeScreen
import com.example.rxcare.presentation.ui.splash.SplashScreen
import com.example.rxcare.presentation.viewmodel.AuthViewModel
import com.example.rxcare.presentation.viewmodel.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RxCareNavigation(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = koinViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignIn.route) {
            SignInScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToSignIn = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = koinViewModel(parameters = { 
                parametersOf(currentUser?.id ?: "")
            })
            HomeScreen(
                homeViewModel = homeViewModel,
                authViewModel = authViewModel,
                onNavigateToChat = { chatId, participant2Id, status ->
                    navController.navigate(Screen.Chat.createRoute(chatId, participant2Id, status))
                },
                onNavigateToSignIn = {
                    authViewModel.signOut()
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToCreatePharmacist = {
                    navController.navigate(Screen.CreatePharmacist.route)
                },
                currentUserRole = currentUser?.role ?: UserRole.CLIENT,
                currentUserName = currentUser?.name
            )
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
            val participant2Id = backStackEntry.arguments?.getString("participant2Id") ?: ""
            val initialStatus = backStackEntry.arguments?.getString("initialStatus")
            ChatScreen(
                chatId = chatId,
                userId = currentUser?.id ?: "",
                participant2Id = participant2Id,
                onNavigateBack = {
                    navController.popBackStack()
                },
                currentUser = currentUser,
                initialStatus = initialStatus
            )
        }

        composable(Screen.CreatePharmacist.route) {
            CreatePharmacistScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.CreatePharmacist.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object Home : Screen("home")
    object Chat : Screen("chat/{chatId}?participant2Id={participant2Id}&initialStatus={initialStatus}") {
        fun createRoute(chatId: String, participant2Id: String, initialStatus: String? = null) = 
            "chat/$chatId?participant2Id=$participant2Id${if (initialStatus != null) "&initialStatus=$initialStatus" else ""}"
    }
    object CreatePharmacist : Screen("create_pharmacist")
}
