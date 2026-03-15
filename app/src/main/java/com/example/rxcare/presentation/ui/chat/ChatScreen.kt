package com.example.rxcare.presentation.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.rxcare.domain.model.Message
import com.example.rxcare.domain.model.MessageType
import com.example.rxcare.domain.model.UserRole
import com.example.rxcare.presentation.viewmodel.AuthViewModel
import com.example.rxcare.presentation.viewmodel.ChatViewModel
import com.example.rxcare.presentation.ui.components.StatusBadge
import com.example.rxcare.presentation.ui.components.SystemEventBubble
import com.example.rxcare.ui.theme.*
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    userId: String,
    participant2Id: String,
    onNavigateBack: () -> Unit,
    currentUser: com.example.rxcare.domain.model.User?,
    initialStatus: String? = null,
    chatViewModel: ChatViewModel = koinViewModel(parameters = { org.koin.core.parameter.parametersOf(userId) }),
) {
    var messageText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var tempImageFile by remember { mutableStateOf<File?>(null) }
    val listState = rememberLazyListState()
    val messages by chatViewModel.messages.collectAsState()
    val uiState by chatViewModel.uiState.collectAsState()
    val isUploadingImage by chatViewModel.isUploadingImage.collectAsState()
    val currentChat by chatViewModel.currentChat.collectAsState()
    val context = LocalContext.current

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            selectedImageUri = uri.toString()
            tempImageFile = tempFile
        }
    }

    LaunchedEffect(chatId) {
        initialStatus?.let { status ->
            println("ChatScreen: Loading chat $chatId with initial status: $status")
        }

        chatViewModel.selectChat(chatId, initialStatus)

        currentUser?.let { user ->
            val token = chatViewModel.getAuthToken()
            if (token.isNotEmpty()) {
                chatViewModel.ensureWebSocketConnection(user.role.name, token)
            }
        }
    }

    val chatStatus = uiState.chatStatus
    LaunchedEffect(chatStatus) {
        chatStatus?.let { status ->
            println("ChatScreen: Chat status changed to: $status")
            // You can add UI reactions here, like showing toasts or updating UI elements
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardSurface)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(NavyPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PH", // Pharmacist initials
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            currentUser?.role == UserRole.CLIENT -> {
                                                participant2Id.replace("pharmacist_", "Pharmacist ") ?: "Pharmacist"
                                            }

                                            currentUser?.role == UserRole.PHARMACIST -> {
                                                participant2Id.replace("patient_", "Patient ") ?: "Patient"
                                            }

                                            else -> "Pharmacist"
                                        },
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    chatStatus?.let { status ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when (status) {
                                                        "PENDING" -> Color.Yellow
                                                        "CLAIMED", "IN_PROGRESS" -> Color.Blue
                                                        "COMPLETED" -> Color.Green
                                                        else -> Color.Gray
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = status.replace("_", " "),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }

                                Text(
                                    text = when (currentUser?.role) {
                                        UserRole.CLIENT -> "Pharmacist"
                                        UserRole.PHARMACIST -> "Patient"
                                        else -> "Pharmacist"
                                    },
                                    style = MaterialTheme.typography.caption,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (currentUser?.role == UserRole.PHARMACIST && chatStatus == "CLAIMED") {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                chatViewModel.completeChat(chatId)
                            }
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Complete Request",
                                tint = GreenText
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            when (chatStatus) {
                "DONE" -> {
                    // Locked bar for completed chats
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurface)
                            .border(1.dp, CardBorder)
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔒",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This request is closed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHint
                            )
                        }
                    }
                }

                "PENDING" -> {
                    // Claim button bar for pharmacists, waiting state for patients
                    if (currentUser?.role == com.example.rxcare.domain.model.UserRole.PHARMACIST) {
                        ClaimButtonBar(
                            onClaim = {
                                chatViewModel.claimChat(chatId)
                            }
                        )
                    } else {
                        WaitingStateBar()
                    }
                }

                else -> {
                    // Message input bar for claimed/active chats
                    MessageInputBar(
                        messageText = messageText,
                        selectedImageUri = selectedImageUri,
                        onMessageTextChanged = { messageText = it },
                        onSendMessage = {
                            tempImageFile?.let { imageFile ->
                                if (messageText.isNotBlank()) {
                                    // Send both text and image
                                    chatViewModel.sendMessageWithImage(imageFile, messageText)
                                } else {
                                    // Send image only
                                    chatViewModel.sendImage(imageFile)
                                }
                            } ?: run {
                                // Send text only
                                if (messageText.isNotBlank()) {
                                    chatViewModel.onMessageTextChanged(messageText)
                                    chatViewModel.sendMessage()
                                }
                            }
                            messageText = ""
                            selectedImageUri = null
                            tempImageFile = null
                        },
                        onAddPhoto = {
                            imagePickerLauncher.launch("image/*")
                        },
                        onRemoveImage = {
                            selectedImageUri = null
                            tempImageFile = null
                        },
                        isUploadingImage = isUploadingImage,
                        chatStatus = chatStatus ?: ""
                    )
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 18.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Prescription attachment bubble (if exists)
                // TODO: Add prescription attachment logic

                items(messages) { message ->
                    when (message.messageType) {
                        MessageType.SYSTEM -> {
                            SystemEventBubble(
                                message = message,
                                eventType = when {
                                    message.content.contains("pharmacist_joined") -> "pharmacist_joined"
                                    message.content.contains("chat_completed") -> "chat_completed"
                                    else -> "system"
                                }
                            )
                        }

                        else -> {
                            MessageBubble(
                                message = message,
                                isFromCurrentUser = message.senderId == userId
                            )
                        }
                    }
                }
            }
        }

        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            chatViewModel.clearError()
                            chatViewModel.selectChat(chatId)
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isFromCurrentUser) 15.dp else 3.dp,
                            topEnd = if (isFromCurrentUser) 3.dp else 15.dp,
                            bottomStart = 15.dp,
                            bottomEnd = 15.dp
                        )
                    )
                    .background(
                        if (isFromCurrentUser) {
                            BrandPrimary
                        } else {
                            CardSurface
                        }
                    )
                    .border(
                        width = if (isFromCurrentUser) 0.dp else 1.dp,
                        color = if (isFromCurrentUser) Color.Transparent else CardBorder,
                        shape = RoundedCornerShape(
                            topStart = if (isFromCurrentUser) 15.dp else 3.dp,
                            topEnd = if (isFromCurrentUser) 3.dp else 15.dp,
                            bottomStart = 15.dp,
                            bottomEnd = 15.dp
                        )
                    )
                    .padding(12.dp)
            ) {
                if (message.messageType == MessageType.IMAGE && !message.imageUrl.isNullOrBlank()) {
                    message.imageUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Shared image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                } else if (message.messageType == MessageType.TEXT_WITH_IMAGE) {
                    Column {
                        // Show image first if it exists
                        message.imageUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Shared image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            if (message.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        // Show text if it exists
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isFromCurrentUser) {
                                    Color.White
                                } else {
                                    TextPrimary
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isFromCurrentUser) {
                            Color.White
                        } else {
                            TextPrimary
                        }
                    )
                }
            }

            // Timestamp
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = com.example.rxcare.ui.theme.SpaceMono),
                color = TextHint,
                fontSize = 9.sp,
                modifier = Modifier.padding(
                    horizontal = if (isFromCurrentUser) 4.dp else 12.dp,
                    vertical = 2.dp
                )
            )
        }
    }
}

@Composable
private fun MessageInputBar(
    messageText: String,
    selectedImageUri: String? = null,
    onMessageTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onAddPhoto: () -> Unit,
    onRemoveImage: () -> Unit = {},
    isUploadingImage: Boolean = false,
    chatStatus: String = "ACTIVE"
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardSurface,
        shadowElevation = 0.dp // No shadows as per design
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // Pharmacist only: "Mark as Done" chip
            // TODO: Add condition for pharmacist role
            /*
            Box(
                modifier = Modifier
                    .clip(ChipShape)
                    .background(BlueBackground.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .align(Alignment.End)
            ) {
                Text(
                    text = "Mark as Done",
                    color = BlueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            */

            // Selected image preview
            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            IconButton(
                                onClick = onRemoveImage,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove image",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo button
                IconButton(
                    onClick = onAddPhoto,
                    enabled = !isUploadingImage,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (isUploadingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = BrandPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = "Add photo",
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text input
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageTextChanged,
                    enabled = chatStatus != "COMPLETED",
                    placeholder = {
                        Text(
                            if (chatStatus == "COMPLETED") "Chat completed - no more messages" else "Type a message...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHint
                        )
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    shape = InputShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Send button
                IconButton(
                    onClick = onSendMessage,
                    enabled = (messageText.isNotBlank() || selectedImageUri != null) && chatStatus != "COMPLETED" && !isUploadingImage,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if ((messageText.isNotBlank() || selectedImageUri != null) && !isUploadingImage) {
                                BrandPrimary
                            } else {
                                CardSurface
                            },
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if ((messageText.isNotBlank() || selectedImageUri != null) && !isUploadingImage) {
                            Color.White
                        } else {
                            TextHint
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun WaitingStateBar() {
    Surface(
        shadowElevation = 12.dp,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Please wait, a pharmacist will help you shortly",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClaimButtonBar(
    onClaim: () -> Unit
) {
    Surface(
        shadowElevation = 12.dp,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Claim Request")
            }
        }
    }
}
