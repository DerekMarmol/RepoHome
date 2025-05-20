package com.petter.application.ui.utils

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petter.application.ui.components.MainLayout
import com.petter.application.ui.screens.auth.LoginScreen
import com.petter.application.ui.screens.auth.RegisterScreen
import com.petter.application.ui.screens.community.CommunityScreen
import com.petter.application.ui.screens.community.CommunityDetailScreen
import com.petter.application.ui.screens.community.CommunityMembersScreen
import com.petter.application.ui.screens.community.CommunityPostDetailScreen
import com.petter.application.ui.screens.community.CommunityRulesScreen
import com.petter.application.ui.screens.community.CommunitySettingsScreen
import com.petter.application.ui.screens.community.CreateCommunityPostScreen
import com.petter.application.ui.screens.community.CreateCommunityScreen
import com.petter.application.ui.screens.community.EditCommunityPostScreen
import com.petter.application.ui.screens.community.EditCommunityScreen
import com.petter.application.ui.screens.feed.CreatePostScreen
import com.petter.application.ui.screens.feed.FeedScreen
import com.petter.application.ui.screens.feed.PostDetailScreen
import com.petter.application.ui.screens.marketplace.CreateProductScreen
import com.petter.application.ui.screens.marketplace.EditProductScreen
import com.petter.application.ui.screens.marketplace.MarketplaceScreen
import com.petter.application.ui.screens.marketplace.ProductDetailScreen
import com.petter.application.ui.screens.notifications.ChatScreen
import com.petter.application.ui.screens.notifications.ChatDetailScreen
import com.petter.application.ui.screens.notifications.NewChatScreen
import com.petter.application.ui.screens.profile.ProfileScreen
import com.petter.application.ui.screens.settings.SettingsScreen
import com.petter.application.ui.screens.splash.SplashScreen
import com.petter.application.ui.screens.veterinary.VetDirectoryScreen

sealed class Screen(val route: String) {
    // Pantallas de autenticación
    object Login : Screen("login")
    object Register : Screen("register")
    object SplashScreen : Screen("splashScreen")

    // Pantallas principales
    object Feed : Screen("feed")
    object CreatePost : Screen("create_post")
    object PostDetail : Screen("post/{postId}") {
        fun createRoute(postId: String) = "post/$postId"
    }

    // Pantallas de perfil
    object Profile : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }

    // Pantallas de comunidad
    object Community : Screen("community")
    object CommunityDetail : Screen("community/{communityId}") {
        fun createRoute(communityId: String) = "community/$communityId"
    }
    object CreateCommunity : Screen("create_community")
    object EditCommunity : Screen("edit_community/{communityId}") {
        fun createRoute(communityId: String) = "edit_community/$communityId"
    }
    object CommunityMembers : Screen("community/{communityId}/members") {
        fun createRoute(communityId: String) = "community/$communityId/members"
    }
    object CommunitySettings : Screen("community/{communityId}/settings") {
        fun createRoute(communityId: String) = "community/$communityId/settings"
    }
    object CommunityRules : Screen("community/{communityId}/rules") {
        fun createRoute(communityId: String) = "community/$communityId/rules"
    }
    object CreateCommunityPost : Screen("community/{communityId}/create_post") {
        fun createRoute(communityId: String) = "community/$communityId/create_post"
    }
    object CommunityPostDetail : Screen("community_post/{postId}") {
        fun createRoute(postId: String) = "community_post/$postId"
    }
    object EditCommunityPost : Screen("edit_community_post/{postId}") {
        fun createRoute(postId: String) = "edit_community_post/$postId"
    }

    // Pantallas de marketplace
    object Marketplace : Screen("marketplace")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object CreateProduct : Screen("create_product")
    object EditProduct : Screen("edit_product/{productId}") {
        fun createRoute(productId: String) = "edit_product/$productId"
    }

    // Pantallas de chat
    object Chat : Screen("chat")
    object ChatDetail : Screen("chat_detail/{conversationId}") {
        fun createRoute(conversationId: String) = "chat_detail/$conversationId"
    }
    object NewChat : Screen("chat_new")
    object StoriesViewer : Screen("stories_viewer/{userId}") {
        fun createRoute(userId: String) = "stories_viewer/$userId"
    }

    // Pantalla veterinaria y configuración
    object VetDirectory : Screen("vet_directory")
    object Settings : Screen("settings")
}

@Composable
fun PetterNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {
        // Pantallas de autenticación
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }

        composable(Screen.SplashScreen.route) {
            SplashScreen(navController)
        }

        // Pantallas principales con MainLayout
        composable(Screen.Feed.route) {
            MainLayout(navController) {
                FeedScreen(navController)
            }
        }

        composable(Screen.CreatePost.route) {
            MainLayout(navController) {
                CreatePostScreen(navController)
            }
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            MainLayout(navController) {
                PostDetailScreen(navController, postId)
            }
        }

        // Pantallas de perfil
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MainLayout(navController) {
                ProfileScreen(navController, userId)
            }
        }

        // Pantallas de comunidad
        composable(Screen.Community.route) {
            MainLayout(navController) {
                CommunityScreen(navController)
            }
        }

        composable(Screen.CreateCommunity.route) {
            MainLayout(navController) {
                CreateCommunityScreen(navController)
            }
        }

        composable(
            route = Screen.CommunityDetail.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunityDetailScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.EditCommunity.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                EditCommunityScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunityMembers.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunityMembersScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunitySettings.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunitySettingsScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunityRules.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunityRulesScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CreateCommunityPost.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CreateCommunityPostScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunityPostDetail.route,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            MainLayout(navController) {
                CommunityPostDetailScreen(postId = postId, navController = navController)
            }
        }

        composable(
            route = Screen.EditCommunityPost.route,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            MainLayout(navController) {
                EditCommunityPostScreen(postId = postId, navController = navController)
            }
        }

        // Pantallas de marketplace
        composable(Screen.Marketplace.route) {
            MainLayout(navController) {
                MarketplaceScreen(navController)
            }
        }

        composable(Screen.CreateProduct.route) {
            MainLayout(navController) {
                CreateProductScreen(navController)
            }
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            MainLayout(navController) {
                ProductDetailScreen(navController, productId)
            }
        }

        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            MainLayout(navController) {
                EditProductScreen(navController = navController, productId = productId)
            }
        }

        // Pantallas de chat
        composable(Screen.Chat.route) {
            MainLayout(navController) {
                ChatScreen(navController)
            }
        }

        composable(Screen.NewChat.route) {
            MainLayout(navController) {
                NewChatScreen(navController)
            }
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            MainLayout(navController) {
                ChatDetailScreen(navController, conversationId)
            }
        }

        composable(
            route = Screen.StoriesViewer.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MainLayout(navController) {
                // StoriesViewerScreen(navController, userId) // Por implementar
            }
        }

        // Pantalla veterinaria
        composable(Screen.VetDirectory.route) {
            MainLayout(navController) {
                VetDirectoryScreen(navController)
            }
        }

        // Pantalla de configuración
        composable(Screen.Settings.route) {
            MainLayout(navController) {
                SettingsScreen(navController)
            }
        }
    }
}

// Función alternativa que acepta NavHostController como parámetro
@Composable
fun PetterNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {
        // Pantallas de autenticación
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }

        composable(Screen.SplashScreen.route) {
            SplashScreen(navController)
        }

        // Pantallas principales con MainLayout
        composable(Screen.Feed.route) {
            MainLayout(navController) {
                FeedScreen(navController)
            }
        }

        composable(Screen.CreatePost.route) {
            MainLayout(navController) {
                CreatePostScreen(navController)
            }
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            MainLayout(navController) {
                PostDetailScreen(navController, postId)
            }
        }

        // Pantallas de perfil
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MainLayout(navController) {
                ProfileScreen(navController, userId)
            }
        }

        // Pantallas de comunidad
        composable(Screen.Community.route) {
            MainLayout(navController) {
                CommunityScreen(navController)
            }
        }

        composable(Screen.CreateCommunity.route) {
            MainLayout(navController) {
                CreateCommunityScreen(navController)
            }
        }

        composable(
            route = Screen.CommunityDetail.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunityDetailScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.EditCommunity.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                EditCommunityScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunityMembers.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunityMembersScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunitySettings.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunitySettingsScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunityRules.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CommunityRulesScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CreateCommunityPost.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val communityId = backStackEntry.arguments?.getString("communityId") ?: ""
            MainLayout(navController) {
                CreateCommunityPostScreen(communityId = communityId, navController = navController)
            }
        }

        composable(
            route = Screen.CommunityPostDetail.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType },
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            MainLayout(navController) {
                CommunityPostDetailScreen(postId = postId, navController = navController)
            }
        }

        composable(
            route = Screen.EditCommunityPost.route,
            arguments = listOf(
                navArgument("communityId") { type = NavType.StringType },
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            MainLayout(navController) {
                EditCommunityPostScreen(postId = postId, navController = navController)
            }
        }

        // Pantallas de marketplace
        composable(Screen.Marketplace.route) {
            MainLayout(navController) {
                MarketplaceScreen(navController)
            }
        }

        composable(Screen.CreateProduct.route) {
            MainLayout(navController) {
                CreateProductScreen(navController)
            }
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            MainLayout(navController) {
                ProductDetailScreen(navController, productId)
            }
        }

        composable(
            route = Screen.EditProduct.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            MainLayout(navController) {
                EditProductScreen(navController = navController, productId = productId)
            }
        }

        // Pantallas de chat
        composable(Screen.Chat.route) {
            MainLayout(navController) {
                ChatScreen(navController)
            }
        }

        composable(Screen.NewChat.route) {
            MainLayout(navController) {
                NewChatScreen(navController)
            }
        }

        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            MainLayout(navController) {
                ChatDetailScreen(navController, conversationId)
            }
        }

        composable(
            route = Screen.StoriesViewer.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            MainLayout(navController) {
                // StoriesViewerScreen(navController, userId) // Por implementar
            }
        }

        // Pantalla veterinaria
        composable(Screen.VetDirectory.route) {
            MainLayout(navController) {
                VetDirectoryScreen(navController)
            }
        }

        // Pantalla de configuración
        composable(Screen.Settings.route) {
            MainLayout(navController) {
                SettingsScreen(navController)
            }
        }
    }
}