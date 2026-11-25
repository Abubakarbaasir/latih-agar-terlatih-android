package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AiQuestionGenerator
import com.example.data.NahwuRepository
import com.example.domain.model.Question
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signOut
import com.example.ui.screens.*
import com.example.ui.theme.LatihNahwuTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private const val ROUTE_PLAYER_HOME = "player_home"
private const val ROUTE_GAME_PLAY = "game_play"
private const val ROUTE_EVALUATION = "evaluation"
private const val ROUTE_ADMIN_DASHBOARD = "admin_dashboard"
private const val ROUTE_ADMIN_MANUAL = "admin_manual"
private const val ROUTE_ADMIN_AI = "admin_ai"
private const val ROUTE_ADMIN_EXCEL = "admin_excel"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureAppCheck(this, intent)

        setContent {
            LatihNahwuTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LatihAgarTerlatihRoot()
                }
            }
        }
    }
}

@Composable
private fun LatihAgarTerlatihRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isSigningIn = false
                    authError = null
                },
                onUnauthenticated = {
                    isSigningIn = false
                },
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            isLoading = isSigningIn,
            errorMessage = authError,
            onGoogleSignIn = {
                isSigningIn = true
                authError = null
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isSigningIn = false
                        authError = null
                    },
                    onAuthError = { msg ->
                        isSigningIn = false
                        authError = msg
                    },
                    scope = scope,
                    onAuthCancelled = {
                        isSigningIn = false
                    }
                )
            },
            onDismissError = { authError = null }
        )
    } else {
        key(user.uid) {
            AuthenticatedAppHost(
                uid = user.uid,
                onSignOut = {
                    signOut(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedAppHost(
    uid: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context, uid) { NahwuRepository(context) }
    val aiGenerator = remember { AiQuestionGenerator() }

    val playerViewModel: PlayerViewModel = viewModel(
        key = "player_$uid",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PlayerViewModel(repository = repository, currentUserId = uid) as T
        }
    )

    val adminViewModel: AdminViewModel = viewModel(
        key = "admin_$uid",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AdminViewModel(repository = repository, aiGenerator = aiGenerator, currentUserId = uid) as T
        }
    )

    val navController = rememberNavController()
    var questionBeingEdited by remember { mutableStateOf<Question?>(null) }

    val userProfile by playerViewModel.myProfile.collectAsStateWithLifecycle()
    val adminRecord by playerViewModel.myAdminRecord.collectAsStateWithLifecycle()
    val categoriesState by playerViewModel.categoriesState.collectAsStateWithLifecycle()
    val difficultyConfig by playerViewModel.difficultyConfig.collectAsStateWithLifecycle()
    val gameState by playerViewModel.gameState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ROUTE_PLAYER_HOME
    ) {
        composable(ROUTE_PLAYER_HOME) {
            PlayerHomeScreen(
                userProfile = userProfile,
                adminRecord = adminRecord,
                categoriesState = categoriesState,
                difficultyConfig = difficultyConfig,
                gameState = gameState,
                onRetryCategories = playerViewModel::retryCategories,
                onSelectCategory = { cat ->
                    playerViewModel.startCategorySession(cat) {
                        navController.navigate(ROUTE_GAME_PLAY) {
                            launchSingleTop = true
                        }
                    }
                },
                onDismissError = playerViewModel::clearError,
                onOpenAdminDashboard = {
                    if (adminRecord?.active == true) {
                        navController.navigate(ROUTE_ADMIN_DASHBOARD) {
                            launchSingleTop = true
                        }
                    }
                },
                onSignOut = onSignOut
            )
        }

        composable(ROUTE_GAME_PLAY) {
            GamePlayScreen(
                gameState = gameState,
                onSelectToken = playerViewModel::selectToken,
                onSelectLetter = playerViewModel::selectLetter,
                onApplyVowel = playerViewModel::applyVowelMark,
                onToggleShadda = playerViewModel::toggleShadda,
                onClearLetter = playerViewModel::clearSelectedLetterMarks,
                onResetToken = playerViewModel::resetSelectedToken,
                onAppendSharafLetter = playerViewModel::appendSharafLetter,
                onRemoveSharafLetter = playerViewModel::removeAddedSharafLetter,
                onCheckAnswer = {
                    playerViewModel.checkAnswer {
                        navController.navigate(ROUTE_EVALUATION) {
                            launchSingleTop = true
                        }
                    }
                },
                onOpenHistoryItem = { item ->
                    playerViewModel.openHistoryEvaluation(item) {
                        navController.navigate(ROUTE_EVALUATION) {
                            launchSingleTop = true
                        }
                    }
                },
                onEndSession = {
                    playerViewModel.endSession {
                        navController.popBackStack(ROUTE_PLAYER_HOME, inclusive = false)
                    }
                }
            )
        }

        composable(ROUTE_EVALUATION) {
            EvaluationResultScreen(
                question = gameState.currentQuestion,
                evaluation = gameState.currentEvaluation,
                onNextQuestion = {
                    playerViewModel.nextQuestion()
                    navController.popBackStack(ROUTE_GAME_PLAY, inclusive = false)
                },
                onBackToGame = {
                    navController.popBackStack(ROUTE_GAME_PLAY, inclusive = false)
                }
            )
        }

        composable(ROUTE_ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                adminViewModel = adminViewModel,
                adminRecord = adminRecord,
                onBackToPlayer = {
                    navController.popBackStack(ROUTE_PLAYER_HOME, inclusive = false)
                },
                onOpenManualEditor = { q ->
                    questionBeingEdited = q
                    navController.navigate(ROUTE_ADMIN_MANUAL) {
                        launchSingleTop = true
                    }
                },
                onOpenAiGenerator = {
                    navController.navigate(ROUTE_ADMIN_AI) {
                        launchSingleTop = true
                    }
                },
                onOpenExcelImporter = {
                    navController.navigate(ROUTE_ADMIN_EXCEL) {
                        launchSingleTop = true
                    }
                },
                onSignOut = onSignOut
            )
        }

        composable(ROUTE_ADMIN_MANUAL) {
            AdminManualEditorScreen(
                adminViewModel = adminViewModel,
                existingQuestion = questionBeingEdited,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_ADMIN_AI) {
            AdminAiGeneratorScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_ADMIN_EXCEL) {
            AdminExcelImportScreen(
                adminViewModel = adminViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
