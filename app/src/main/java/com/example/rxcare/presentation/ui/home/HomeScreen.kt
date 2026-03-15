package com.example.rxcare.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp as Dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.example.rxcare.domain.model.User
import com.example.rxcare.domain.model.UserRole
import com.example.rxcare.domain.model.RequestStatus
import com.example.rxcare.domain.model.Request
import com.example.rxcare.domain.model.Chat
import com.example.rxcare.presentation.viewmodel.HomeViewModel
import com.example.rxcare.presentation.viewmodel.AuthViewModel
import com.example.rxcare.presentation.ui.requests.PendingRequestsScreen
import com.example.rxcare.presentation.ui.requests.ActiveRequestsScreen
import com.example.rxcare.presentation.ui.requests.CompletedRequestsScreen
import com.example.rxcare.presentation.ui.components.WebSocketLifecycleManager
import com.example.rxcare.presentation.ui.components.StatusBadge
import com.example.rxcare.presentation.ui.components.RequestCard
import com.example.rxcare.ui.theme.*
import androidx.compose.material.icons.filled.Circle
import com.example.rxcare.presentation.ui.components.StatCard
import com.example.rxcare.presentation.ui.components.TabChip
import androidx.compose.ui.unit.dp

enum class HomeTab(val title: String) {
    PENDING("Pending"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    authViewModel: AuthViewModel,
    onNavigateToChat: (String, String, String?) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToCreatePharmacist: () -> Unit = {},
    currentUserRole: UserRole,
    currentUserName: String? = null,
    onClaimChat: (String) -> Unit = { chatId -> homeViewModel.claimChat(chatId) }
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val chats by homeViewModel.chats.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()

    var selectedTab by remember { mutableStateOf(HomeTab.PENDING) }
    var selectedStatus by remember { mutableStateOf<RequestStatus?>(null) }
    var showNewRequestSheet by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var pendingClaimChatId by remember { mutableStateOf<String?>(null) }

    // WebSocket lifecycle management
    WebSocketLifecycleManager(homeViewModel = homeViewModel)

    // Both patients and pharmacists now use the same home screen
    PatientHomeScreen(
        homeViewModel = homeViewModel,
        searchQuery = searchQuery,
        selectedStatus = selectedStatus,
        showNewRequestSheet = showNewRequestSheet,
        currentUserRole = currentUserRole,
        currentUserName = currentUserName,
        onSearchQueryChange = { homeViewModel.searchChats(it) },
        onStatusSelect = { status ->
            selectedStatus = status
            // Refetch requests when status changes, especially for pharmacists (silent loading)
            val statusString = when (status) {
                RequestStatus.PENDING -> "PENDING"
                RequestStatus.ACCEPTED -> "CLAIMED"
                RequestStatus.COMPLETED -> "COMPLETED"
                null -> null
            }
            homeViewModel.loadRequests(statusString, silent = true)
        },
        onNewRequestClick = { showNewRequestSheet = true },
        onNewRequestDismiss = { showNewRequestSheet = false },
        onNavigateToChat = onNavigateToChat,
        onNavigateToSignIn = onNavigateToSignIn,
        onNavigateToCreatePharmacist = onNavigateToCreatePharmacist,
        onClaimChat = onClaimChat,
        showClaimDialog = showClaimDialog,
        pendingClaimChatId = pendingClaimChatId,
        onClaimDialogDismiss = { showClaimDialog = false },
        onShowClaimDialog = { chatId ->
            pendingClaimChatId = chatId
            showClaimDialog = true
        }
    )

    // Claim Confirmation Dialog
    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = {
                showClaimDialog = false
                pendingClaimChatId = null
            },
            title = {
                Text("Claim Request")
            },
            text = {
                Text("Do you want to claim this request?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingClaimChatId?.let { chatId ->
                            onClaimChat(chatId)
                            // Find the chat to get participant2Id
                            val chat = chats.find { it.id == chatId }
                            chat?.let {
                                onNavigateToChat(chatId, it.participant2Id, it.status)
                            }
                        }
                        showClaimDialog = false
                        pendingClaimChatId = null
                    }
                ) {
                    Text("Claim")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showClaimDialog = false
                        pendingClaimChatId = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientHomeScreen(
    homeViewModel: HomeViewModel,
    searchQuery: String,
    selectedStatus: RequestStatus?,
    showNewRequestSheet: Boolean,
    currentUserRole: UserRole,
    currentUserName: String? = null,
    onSearchQueryChange: (String) -> Unit,
    onStatusSelect: (RequestStatus?) -> Unit,
    onNewRequestClick: () -> Unit,
    onNewRequestDismiss: () -> Unit,
    onNavigateToChat: (String, String, String?) -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToCreatePharmacist: () -> Unit = {},
    onClaimChat: (String) -> Unit = { chatId -> homeViewModel.claimChat(chatId) },
    showClaimDialog: Boolean = false,
    pendingClaimChatId: String? = null,
    onClaimDialogDismiss: () -> Unit = {},
    onShowClaimDialog: (String) -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val chats by homeViewModel.chats.collectAsState()
    val connectionStatus by homeViewModel.connectionStatus.collectAsState()
    
    
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar circle with initials
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (currentUserRole == UserRole.CLIENT) BrandPrimary else NavyPrimary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUserName?.take(2)?.uppercase() ?: "U",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Hi, ${currentUserName ?: "there"}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    // Show create pharmacist button only for pharmacists
                    if (currentUserRole == UserRole.PHARMACIST) {
                        IconButton(onClick = onNavigateToCreatePharmacist) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Create Pharmacist")
                        }
                    }
                    IconButton(onClick = onNavigateToSignIn) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB for patients
            if (currentUserRole == UserRole.CLIENT) {
                FloatingActionButton(
                    onClick = onNewRequestClick,
                    modifier = Modifier.padding(horizontal = 18.dp),
                    shape = ChipShape,
                    containerColor = BrandPrimary
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Request",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "New Request",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp)
        ) {
            // Stats row
            val pendingCount = chats.filter { it.status == "PENDING" }.size
            val claimedCount = chats.filter { it.status == "CLAIMED" }.size
            val doneCount = chats.filter { it.status == "DONE" }.size

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PENDING card
                StatCard(
                    count = pendingCount,
                    label = "PENDING",
                    color = AmberText,
                    modifier = Modifier.weight(1f)
                )

                // CLAIMED card
                StatCard(
                    count = claimedCount,
                    label = "CLAIMED",
                    color = GreenText,
                    modifier = Modifier.weight(1f)
                )

                // DONE card
                StatCard(
                    count = doneCount,
                    label = "DONE",
                    color = BlueText,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show "All" button for both patients and pharmacists
                TabChip(
                    onClick = { onStatusSelect(null) },
                    label = "All",
                    isSelected = selectedStatus == null
                )
                TabChip(
                    onClick = { onStatusSelect(RequestStatus.PENDING) },
                    label = "PENDING",
                    isSelected = selectedStatus == RequestStatus.PENDING
                )
                TabChip(
                    onClick = { onStatusSelect(RequestStatus.ACCEPTED) },
                    label = "CLAIMED",
                    isSelected = selectedStatus == RequestStatus.ACCEPTED
                )
                TabChip(
                    onClick = { onStatusSelect(RequestStatus.COMPLETED) },
                    label = "DONE",
                    isSelected = selectedStatus == RequestStatus.COMPLETED
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Use actual chat data from ViewModel with status filtering
            val filteredChats = when {
                searchQuery.isNotBlank() -> uiState.searchResults
                selectedStatus == null -> chats
                selectedStatus == RequestStatus.PENDING -> chats.filter { it.status == "PENDING" }
                selectedStatus == RequestStatus.ACCEPTED -> chats.filter { it.status == "CLAIMED" }
                selectedStatus == RequestStatus.COMPLETED -> chats.filter { it.status == "DONE" }
                else -> chats
            }

            if (filteredChats.isEmpty() && !uiState.isLoading) {
                EmptyRequestsState(
                    selectedStatus = selectedStatus,
                    searchQuery = searchQuery
                )
            } else if (uiState.isLoading) {
                // Show loading indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredChats) { chat ->
                        RequestCard(
                            chatId = chat.id,
                            status = chat.status,
                            title = chat.participant2Id.replace("patient_", "Patient ").replace("pharmacist_", "Pharmacist "),
                            subtitle = chat.lastMessage?.content ?: "No messages yet",
                            timestamp = formatTimestamp(chat.lastMessageTime),
                            currentUserRole = currentUserRole,
                            onAction = {
                                // Always navigate to chat when clicking the card
                                onNavigateToChat(chat.id, chat.participant2Id, chat.status)
                            },
                            onClaimAction = {
                                // Show claim confirmation dialog
                                if (currentUserRole == UserRole.PHARMACIST && chat.status == "PENDING") {
                                    onShowClaimDialog(chat.id)
                                }
                            }
                        )
                    }
                }
            }

            // New Request Bottom Sheet
            if (showNewRequestSheet) {
                NewRequestBottomSheet(
                    onDismiss = onNewRequestDismiss,
                    onSubmit = { description, imageUri ->
                        homeViewModel.createPrescriptionRequest(description, imageUri)
                        onNewRequestDismiss()
                    }
                )
            }

            // Show error message
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // Auto-dismiss error after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    homeViewModel.clearError()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    currentUserRole: UserRole,
    onClick: () -> Unit,
    onClaimChat: ((String) -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Participant info with avatar
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar placeholder
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = chat.participant2Id.replace("patient_", "Patient ").replace("pharmacist_", "Pharmacist "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        chat.lastMessage?.let { message ->
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when (chat.status) {
                            "PENDING" -> MaterialTheme.colorScheme.errorContainer
                            "CLAIMED" -> MaterialTheme.colorScheme.primaryContainer
                            "DONE" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = when (chat.status) {
                                "PENDING" -> "Pending"
                                "CLAIMED" -> "Active"
                                "DONE" -> "Completed"
                                else -> chat.status
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (chat.status) {
                                "PENDING" -> MaterialTheme.colorScheme.onErrorContainer
                                "CLAIMED" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "DONE" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    // Claim button for pharmacists
                    if (currentUserRole == UserRole.PHARMACIST && chat.status == "PENDING" && onClaimChat != null) {
                        Button(
                            onClick = { onClaimChat(chat.id) },
                            modifier = Modifier.heightIn(min = 32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Claim",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Footer with metadata
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(chat.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat ID: ${chat.id.take(8)}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (chat.isArchived) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewRequestBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (description: String, imageUri: String?) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Create a temporary file for camera capture
    val photoFile = remember {
        File(
            context.cacheDir,
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        )
    }

    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it.toString()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile.exists()) {
            selectedImageUri = photoUri.toString()
        }
    }

    val isSubmitEnabled = description.isNotBlank() || selectedImageUri != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) } // Remove default insets to handle keyboard manually
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()) // Make content scrollable
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New Request",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Describe your request...") },
                placeholder = { Text("e.g., I need advice about my blood pressure medication") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image upload section
            Card(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to change image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload Image (Optional)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap to select an image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSubmit(description, selectedImageUri)
                },
                enabled = isSubmitEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Request")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

@Composable
private fun EmptyRequestsState(
    selectedStatus: RequestStatus?,
    searchQuery: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Text(
            text = when {
                searchQuery.isNotBlank() -> "No requests found"
                selectedStatus == null -> "No requests yet"
                selectedStatus == RequestStatus.PENDING -> "No pending requests"
                selectedStatus == RequestStatus.ACCEPTED -> "No active requests"
                selectedStatus == RequestStatus.COMPLETED -> "No completed requests"
                else -> "No requests found"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = when {
                searchQuery.isNotBlank() -> "Try adjusting your search terms"
                selectedStatus == null -> "Create your first request to get started"
                selectedStatus == RequestStatus.PENDING -> "All requests have been accepted or completed"
                selectedStatus == RequestStatus.ACCEPTED -> "No requests are currently active"
                selectedStatus == RequestStatus.COMPLETED -> "No requests have been completed yet"
                else -> "Check back later for new requests"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
