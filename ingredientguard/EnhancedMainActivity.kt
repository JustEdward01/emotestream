package com.ingredientguard

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ingredientguard.ui.theme.IngredientGuardTheme
import com.ingredientguard.data.analyzer.EnhancedAnalysisResult
import com.ingredientguard.data.analyzer.EnhancedAnalysisState
import com.ingredientguard.data.repository.ScanHistoryItem
import com.ingredientguard.ui.screens.*
import com.ingredientguard.ui.components.PersonalAllergenWarningCard
import com.ingredientguard.viewmodel.*
import com.ingredientguard.data.repository.UserRepository
import com.ingredientguard.utils.PreferencesHelper
import java.io.File

// Enhanced Navigation States
sealed class EnhancedNavigationState {
    data object Home : EnhancedNavigationState()
    data object Scanner : EnhancedNavigationState()
    data object History : EnhancedNavigationState()
    data object Profile : EnhancedNavigationState()
    data object Settings : EnhancedNavigationState()
    data object Onboarding : EnhancedNavigationState()
    data class ScanResults(val analysisResult: EnhancedAnalysisResult) : EnhancedNavigationState()
    data class HistoryDetails(val item: ScanHistoryItem) : EnhancedNavigationState()
    data object Processing : EnhancedNavigationState()
    data class Error(val message: String) : EnhancedNavigationState()
}

class EnhancedMainActivity : ComponentActivity() {

    private lateinit var imageUri: Uri
    private lateinit var photoFile: File
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private val scannerViewModel: EnhancedScannerViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLaunchers()
        requestNotificationPermission()

        enableEdgeToEdge()
        setContent {
            IngredientGuardTheme {
                var shouldShowOnboarding by remember { mutableStateOf(true) }
                var isCheckingOnboarding by remember { mutableStateOf(true) }

                // Check if onboarding is completed
                LaunchedEffect(Unit) {
                    val userRepository = UserRepository(this@EnhancedMainActivity)
                    shouldShowOnboarding = !userRepository.isOnboardingCompleted()
                    isCheckingOnboarding = false
                }

                when {
                    isCheckingOnboarding -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    shouldShowOnboarding -> {
                        OnboardingScreen(
                            viewModel = onboardingViewModel,
                            onComplete = {
                                shouldShowOnboarding = false
                                PreferencesHelper.setFirstLaunchCompleted(this@EnhancedMainActivity)
                            }
                        )
                    }
                    else -> {
                        EnhancedAllergenScannerApp(
                            scannerViewModel = scannerViewModel,
                            onboardingViewModel = onboardingViewModel,
                            onTakePicture = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onPickFromGallery = {
                                pickImageLauncher.launch("image/*")
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setupLaunchers() {
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    scannerViewModel.analyzeImage(imageUri)
                }
            }

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    scannerViewModel.analyzeImage(it)
                }
            }

        cameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    launchCamera()
                } else {
                    Toast.makeText(this, "Permisiune refuzată!", Toast.LENGTH_SHORT).show()
                }
            }

        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted) {
                    Toast.makeText(this, "Notificările nu vor fi afișate", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun launchCamera() {
        photoFile = createImageFile()
        imageUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(imageUri)
    }

    private fun createImageFile(): File {
        val fileName = "photo_${System.currentTimeMillis()}"
        val storageDir = cacheDir
        return File.createTempFile(fileName, ".jpg", storageDir)
    }
}

@Composable
fun EnhancedAllergenScannerApp(
    scannerViewModel: EnhancedScannerViewModel,
    onboardingViewModel: OnboardingViewModel,
    onTakePicture: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    var navigationState by remember { mutableStateOf<EnhancedNavigationState>(EnhancedNavigationState.Home) }
    val scannerState = scannerViewModel.analysisState

    // React to changes from EnhancedScannerViewModel
    LaunchedEffect(scannerState) {
        navigationState = when (scannerState) {
            is EnhancedAnalysisState.Idle -> EnhancedNavigationState.Home
            is EnhancedAnalysisState.Loading -> EnhancedNavigationState.Processing
            is EnhancedAnalysisState.Success -> EnhancedNavigationState.ScanResults(scannerState.result)
            is EnhancedAnalysisState.Error -> EnhancedNavigationState.Error(scannerState.message)
        }
    }

    // Navigation functions
    val navigateToHome = {
        scannerViewModel.resetState()
        navigationState = EnhancedNavigationState.Home
    }

    val navigateToHistory = { navigationState = EnhancedNavigationState.History }
    val navigateToProfile = { navigationState = EnhancedNavigationState.Profile }
    val navigateToSettings = { navigationState = EnhancedNavigationState.Settings }

    val navigateToHistoryDetails = { item: ScanHistoryItem ->
        navigationState = EnhancedNavigationState.HistoryDetails(item)
    }

    // Render current screen with animations
    AnimatedContent(
        targetState = navigationState,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        },
        label = "screen_transition"
    ) { state ->
        when (state) {
            is EnhancedNavigationState.Home -> {
                EnhancedHomeScreen(
                    onTakePicture = onTakePicture,
                    onPickFromGallery = onPickFromGallery,
                    onViewHistory = navigateToHistory,
                    onViewProfile = navigateToProfile
                )
            }

            is EnhancedNavigationState.Processing -> {
                ProcessingScreen()
            }

            is EnhancedNavigationState.ScanResults -> {
                EnhancedScanResultsScreen(
                    result = state.analysisResult,
                    onBackToHome = navigateToHome,
                    onViewHistory = navigateToHistory
                )
            }

            is EnhancedNavigationState.History -> {
                val historyViewModel: HistoryViewModel = viewModel()
                HistoryNavigationScreen(
                    onBack = navigateToHome,
                    onItemClick = navigateToHistoryDetails,
                    viewModel = historyViewModel
                )
            }

            is EnhancedNavigationState.Profile -> {
                val profileViewModel: UserProfileViewModel = viewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = navigateToHome,
                    onNavigateToSettings = navigateToSettings
                )
            }

            is EnhancedNavigationState.Settings -> {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = navigateToProfile
                )
            }

            is EnhancedNavigationState.HistoryDetails -> {
                EnhancedHistoryDetailsScreen(
                    item = state.item,
                    onBack = navigateToHistory
                )
            }

            is EnhancedNavigationState.Error -> {
                ErrorNavigationScreen(
                    error = state.message,
                    onRetry = navigateToHome
                )
            }

            // Cazurile lipsă
            is EnhancedNavigationState.Scanner -> {
                ProcessingScreen()
            }

            is EnhancedNavigationState.Onboarding -> {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onComplete = navigateToHome
                )
            }
        }
    }
}

// Enhanced Home Screen with Profile access
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedHomeScreen(
    onTakePicture: () -> Unit,
    onPickFromGallery: () -> Unit,
    onViewHistory: () -> Unit,
    onViewProfile: () -> Unit
) {
    val userRepository = UserRepository(LocalContext.current)
    var personalAllergens by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        userRepository.getUserProfile().collect { profile ->
            personalAllergens = profile?.personalAllergens ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "AllergenScanner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AllergenScanner",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onViewProfile) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onViewHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Istoric",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personal allergen warning card
            PersonalAllergenWarningCard(personalAllergens = personalAllergens)

            // Hero Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Siguranță",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanează și Protejează-te",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Identifică ingredientele și alergenii din produsele alimentare",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onTakePicture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Scanează cu Camera",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 2.dp
                    )
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Galerie",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Alege din Galerie",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Istoric",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Vezi Istoric Scanări",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Feature Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.Speed,
                    title = "Rapid",
                    description = "Rezultate în secunde",
                    modifier = Modifier.weight(1f)
                )

                FeatureCard(
                    icon = Icons.Default.Security,
                    title = "Sigur",
                    description = "Date stocate local",
                    modifier = Modifier.weight(1f)
                )

                FeatureCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Precis",
                    description = "Recunoaștere avansată",
                    modifier = Modifier.weight(1f)
                )
            }

            // Disclaimer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "⚠️ Această aplicație te ajută să identifici alergenii, dar pentru informații medicale consultă întotdeauna un specialist.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Enhanced Results Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedScanResultsScreen(
    result: EnhancedAnalysisResult,
    onBackToHome: () -> Unit,
    onViewHistory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (result.personalAllergensDetected) {
                            "⚠️ ALERTĂ ALERGENI!"
                        } else {
                            "Rezultate Scanare"
                        }
                    )
                },
                actions = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.Home, "Acasă")
                    }
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, "Istoric")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (result.personalAllergensDetected) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Convert to legacy result for existing ResultsScreen
            val legacyResult = com.ingredientguard.data.models.AnalysisResult(
                fullText = result.fullText,
                ingredients = result.ingredients,
                allergens = result.allergens
            )

            ResultsScreen(
                result = legacyResult,
                onBack = onBackToHome
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth(),
                colors = if (result.personalAllergensDetected) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text("Scanează Alt Produs")
            }
        }
    }
}

@Composable
fun EnhancedHistoryDetailsScreen(
    item: ScanHistoryItem,
    onBack: () -> Unit
) {
    val legacyResult = com.ingredientguard.data.models.AnalysisResult(
        fullText = item.fullText,
        ingredients = item.ingredients,
        allergens = item.allergens
    )

    ResultsScreen(
        result = legacyResult,
        onBack = onBack
    )
}

@Composable
fun ProcessingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            Text(
                text = "Procesez imaginea și extrag textul...",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HistoryNavigationScreen(
    onBack: () -> Unit,
    onItemClick: (ScanHistoryItem) -> Unit,
    viewModel: HistoryViewModel
) {
    HistoryScreen(
        onBack = onBack,
        onItemClick = onItemClick,
        viewModel = viewModel
    )
}

@Composable
fun ErrorNavigationScreen(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Eroare",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Încearcă din nou")
            }
        }
    }
}