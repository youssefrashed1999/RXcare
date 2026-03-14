# Pharmacy Chat Application

A modern Android messaging application that connects pharmacists and clients using clean architecture and best Android development practices.

## Features

### User Roles
- **Pharmacist**: Can provide medical advice and answer medication questions
- **Client**: Can ask questions and seek advice from pharmacists

### Authentication
- Sign Up with name, email, password, and role selection
- Sign In with email and password
- Local authentication using DataStore for session management

### Home Screen
- List of all users (filtered by role)
- Search functionality to find specific users
- Filter users by role (Pharmacist/Client)
- Online status indicators

### Chat Screen
- Real-time text messaging (local implementation)
- Message timestamps
- Read/delivered indicators
- Clean message bubble UI
- User avatars and online status

## Architecture

The app follows **Clean Architecture** principles with three main layers:

### Data Layer
- **Local**: Room database for persistent storage
- **Repository**: Data access and business logic
- **Models**: Database entities and mappers

### Domain Layer
- **Models**: Business entities (User, Message, Chat)
- **Repository Interfaces**: Abstract data access contracts
- **Use Cases**: Business logic (can be added as needed)

### Presentation Layer
- **UI**: Compose screens (Auth, Home, Chat)
- **ViewModels**: State management and UI logic
- **Navigation**: Navigation Component setup

## Technology Stack

- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room for local storage
- **Async**: Coroutines and Flow
- **Navigation**: Navigation Compose
- **Dependency Injection**: Manual DI container
- **Image Loading**: Coil
- **Local Storage**: DataStore Preferences

## Project Structure

```
app/src/main/java/com/example/pharmacychat/
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAOs
│   │   ├── entity/        # Database entities
│   │   ├── mapper/        # Entity ↔ Domain mappers
│   │   └── PreferencesManager.kt
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/             # Business models
│   └── repository/        # Repository interfaces
├── presentation/
│   ├── navigation/        # Navigation setup
│   ├── ui/
│   │   ├── auth/          # Sign In/Up screens
│   │   ├── home/          # Home screen
│   │   └── chat/          # Chat screen
│   └── viewmodel/         # ViewModels
├── di/                     # Dependency injection
└── MainActivity.kt
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog | 2023.1.1 or newer
- Kotlin 1.9.0 or newer
- Android SDK 26+ (minSdk)

### Installation
1. Clone the repository
2. Open in Android Studio
3. Sync the project
4. Run on an emulator or physical device

### Usage
1. **First Time**: Create an account with your role (Pharmacist/Client)
2. **Sign In**: Use your credentials to sign in
3. **Home**: Browse users, search, and filter by role
4. **Chat**: Tap on any user to start a conversation

## Current Implementation Status

### ✅ Completed
- Clean architecture setup
- User authentication (local)
- Role-based user system
- Home screen with search and filtering
- Chat screen with messaging UI
- Message timestamps and read indicators
- Navigation between screens
- Room database integration
- DataStore for session management

### 🚧 In Progress
- Image sending and receiving functionality

### 📋 Future Enhancements
- Real-time synchronization between devices
- Push notifications
- Voice messages
- File sharing
- Video calls
- Prescription management
- Medicine database integration
- Multi-language support

## Notes

- This is a local-first implementation without Firebase
- All data is stored locally using Room database
- Authentication is handled locally using DataStore
- For production use, consider adding:
  - Proper password hashing
  - Server-side synchronization
  - Security enhancements
  - Input validation improvements

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is open source and available under the MIT License.
