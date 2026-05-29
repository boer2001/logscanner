# AI Agent Guide for LogScanner

## Project Overview
LogScanner is an Android inventory management app built with modern Jetpack Compose and Room database. It enables barcode scanning, local logging, and server synchronization for lumber/log tracking with QR code integration.

## Architecture

### Layered Structure
The project follows **Clean Architecture** with explicit data/UI separation:

```
com.lumberyard.logscanner/
├── MainActivity.kt           # Single activity  
├── data/                     # Repository pattern + data sources
│   ├── local/              # Room database (LogEntity, ScanEntity)
│   ├── remote/             # Retrofit API client
│   └── repository/         # LogRepository (abstraction layer)
└── ui/                       # Composable screens + ViewModels
    ├── screens/            # Feature screens + screen-specific VMs
    ├── navigation/         # Navigation3 with sealed Route interface
    └── theme/              # Material3 theming
```

### Dependency Injection Pattern
**No DI framework used** (not Hilt). Manual setup in `MainActivity`:
- Database singleton: `AppDatabase.getDatabase(context)` with `@Volatile` INSTANCE
- Retrofit client: Built inline with `OkHttpClient.Builder()`
- Repository: Instantiated with DAOs + ApiService
- ViewModels: Created via custom `ViewModelProvider.Factory` objects in composables

When adding new features requiring shared instances, follow this pattern rather than adding Hilt.

### Data Layer
**Repository Pattern**: `LogRepository` abstracts DB + API:
```kotlin
// Exposes Flow-based streams for reactive UI
val allLogs: Flow<List<LogEntity>>
val allScans: Flow<List<ScanEntity>>

// Suspend functions for state-changing ops
suspend fun insertScan(scan: ScanEntity)
suspend fun syncData()  // Orchestrates upload + download
```

**Database**: Room with two DAOs
- `LogDao`: All-get() methods return Flow for reactivity
- `ScanDao`: Track sync status (unsynced scans before server upload)
- Database version 1, no migrations yet

**API**: Retrofit interface with Moshi serialization
- Endpoint: `https://api.lumberyard.com/`
- Key endpoints: `GET /logs`, `POST /scans`
- Response wrapper: `SyncResponse(success, message, syncedIds)`

### Huawei/Non-GMS Compatibility
- **Scanning**: Dual-strategy using `BarcodeAnalyzer`. Uses HMS Scan Kit (`com.huawei.hms:scanplus`) on Huawei devices for optimized performance and ML Kit (bundled version) as a fallback/default.
- **Location Services**: Play Services location dependency removed (to support non-GMS devices like Huawei).
- **Repositories**: Huawei Maven repository added to `settings.gradle.kts`.

## UI Layer

### Navigation3 (Sealed Type-Safe Routes)
- Routes defined as sealed interface `Route : NavKey` with `@Serializable` data objects
- Five top-level routes: Scanner, History, Inventory, Sync, StockTake
- Composable-first: `NavDisplay` + `entryProvider` DSL in `AppShell`
- Navigator utility for programmatic navigation

### Screen Patterns
All screens follow ViewModel + Compose state pattern:
```kotlin
@Composable
fun scannerScreen(viewModel: ScannerViewModel, onNavigate: () -> Unit)
```

Screen-specific ViewModels extend `ViewModel` and access repository via constructor injection. Example: `ScannerViewModel(repository)` created in MainActivity's entryProvider.

### State Management
- **Flow-based**: Repository exposes `Flow<List<T>>` for lists
- **MutableStateFlow**: Used in ViewModels for mutable state (e.g., `_isSyncing`)
- **collectAsState()**: Compose integration in screens

## Key Technologies & Versions

| Tech        | Version    | Purpose                       |
|-------------|------------|-------------------------------|
| Kotlin      | 2.3.21     | Language                      |
| ComposeUI   | 2024.09.00 | UI framework                  |
| Room        | 2.7.0      | Local DB                      |
| Retrofit    | 2.12.0     | HTTP client                   |
| Coroutines  | 1.10.2     | Async/concurrency             |
| KSP         | 2.3.9      | Code generation (Room, Moshi) |
| Camera      | 1.5.0      | Barcode scanning              |
| ML Kit      | 17.3.0     | QR/barcode detection          |
| Navigation3 | 1.0.1      | Typed navigation              |

## Build & Development

### Build Command
```bash
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK (unsigned)
./gradlew assemble           # Build both
./gradlew test               # Run local unit tests
./gradlew connectedAndroidTest  # Run instrumented tests
```

### Gradle Structure
- `build.gradle.kts` (root): Plugin management only
- `app/build.gradle.kts`: All dependencies, AGP = 9.2.1
- `gradle/libs.versions.toml`: Centralized version catalog (best practice)

### Key Build Properties
```
SDK Level: compileSDK=34, minSDK=24, targetSDK=34
Compose Enabled: buildFeatures { compose = true }
Java Target: VERSION_11
Code Generation: KSP enabled for Room + Moshi codegen
```

### Database Seeding
Default test data inserted on first app startup in `MainActivity.onCreate()` via `CoroutineScope(Dispatchers.IO)` if `logDao.getLogCount() == 0`.

## Common Workflows

### Adding a New Screen
1. Create screen function in `ui/screens/` as `@Composable`
2. Create corresponding `ScreenViewModel(repository: LogRepository)` in same file/folder
3. Add route to `Route` sealed interface with `@Serializable`
4. Register in `MainActivity.AppShell` entryProvider DSL
5. Add NavigationBarItem in bottom nav if top-level

### Adding a Database Entity
1. Create data class in `data/local/` annotated with `@Entity`
2. Create corresponding DAO interface with suspend functions + Flow getters
3. Add to `@Database` entities list in `AppDatabase`
4. Increment `version` and add migration (or use `fallbackToDestructiveMigration` for dev)

### Making API Changes
1. Update `ApiService` interface with new `@GET/@POST/@PUT/@DELETE` methods
2. Update `SyncResponse` or create new response DTOs (Moshi will serialize)
3. Add corresponding to suspend function to `LogRepository`
4. Call from ViewModel and observe Flow state in UI

### Handling Permissions
Camera permission for barcode scanner handled via `Accompanist` permissions:
```kotlin
PermissionBox(permissions = listOf(Manifest.permission.CAMERA)) { granted ->
    if (granted) setupCamera() 
}
```

## Testing Setup

- Unit tests: JUnit4 + Coroutines test utilities (`runBlockingTest`)
- Instrumented tests: AndroidJUnit with Espresso + Compose test API
- Test runner: `androidx.test.runner.AndroidJUnitRunner`
- No Mockito/Robolectric yet—tests are minimal; expand as needed

## Code Style & Conventions

- **Kotlin Code Style**: "official" per gradle.properties
- **Naming**: camelCase for functions, PascalCase for classes/objects
- **Sealed Types**: Used for typesafe Route navigation (follow this pattern for domain models)
- **Coroutines**: ViewModels use `viewModelScope` to ensure cancellation; Repository uses suspend
- **Imports**: Package structure mirrors feature → avoid star imports
- **Comments**: Minimal, let code speak; only document non-obvious logic

## Integration Points & External Concerns

- **API Base URL**: Hardcoded in MainActivity as `https://api.lumberyard.com/`—move to config/BuildConfig for multienvironment support
- **Location Services**: Play Services location dependency removed (to support non-GMS devices like Huawei)
- **Network**: OkHttpClient logs network calls via `logging-interceptor` (add interceptor in MainActivity as needed)
- **Storage**: Datastore Preferences available for key-value config (not yet used)

## Debugging Tips

- Enable Network Logging: Pass `HttpLoggingInterceptor` to OkHttpClient builder
- Database: Use Android Studio Device File Explorer to inspect SQLite DB at `/data/data/com.lumberyard.logscanner/databases/`
- Compose Preview: Use `@Preview` on composables; compile with `debugImplementation(libs.androidx.compose.ui.tooling)`
- Sync Issues: Check `scanDao.getUnsyncedScans()` state in repository; logs not marked as synced until server responds

## Performance Notes

- **Database**: Room queries are synchronous on caller's dispatcher; repository returns Flow to avoid blocking main thread
- **Camera**: Uses `ProcessCameraProvider` for lifecycle-safe initialization; barcode analysis runs on background executor
- **Memory**: Navigated screens are kept in backstack; consider `disposable` flags for large cached screens
- **Compose Recomposition**: Avoid passing collections directly; use `remember` or extract to ViewModel state

## Future Expansion Points

- Switch to Hilt DI for cleaner multiscreen ViewModels
- Add DataStore for sync timestamps (currently hardcoded to 0L)
- Implement Room migration strategy for versioned schema changes
- Add local error logging/analytics
- Implement proper date-based sync logic with last_updated tracking

