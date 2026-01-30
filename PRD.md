 Here is Claude's plan:                                                         
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
 Claude Watch App - Implementation Plan                                         
                                                                                
 Overview                                                                       
                                                                                
 A Samsung Galaxy Watch 7 app that lets users ask questions via voice and       
 receive scrollable text responses from Claude.                                 
                                                                                
 Tech Stack:                                                                    
 - Platform: Wear OS 5 (Android-based)                                          
 - Language: Kotlin                                                             
 - UI: Jetpack Compose for Wear OS (Material 3)                                 
 - Networking: Retrofit + OkHttp                                                
 - Speech: Android SpeechRecognizer (built-in, free)                            
                                                                                
 ---                                                                            
 Architecture Flow                                                              
                                                                                
 [User taps button] → [Microphone activates]                                    
         ↓                                                                      
 [User speaks question]                                                         
         ↓                                                                      
 [SpeechRecognizer converts to text]                                            
         ↓                                                                      
 [Text sent to Claude API via HTTPS]                                            
         ↓                                                                      
 [Response displayed in scrollable list]                                        
         ↓                                                                      
 [User scrolls with physical bezel]                                             
                                                                                
 ---                                                                            
 Project Structure                                                              
                                                                                
 wear/                                                                          
 ├── build.gradle.kts                                                           
 ├── src/main/                                                                  
 │   ├── AndroidManifest.xml                                                    
 │   ├── kotlin/com/example/claudewatch/                                        
 │   │   ├── MainActivity.kt           # Entry point                            
 │   │   ├── presentation/                                                      
 │   │   │   ├── MainScreen.kt          # Main UI composable                    
 │   │   │   └── theme/Theme.kt         # Wear OS theme                         
 │   │   ├── data/                                                              
 │   │   │   ├── ClaudeApi.kt           # Retrofit interface                    
 │   │   │   └── ClaudeRepository.kt    # API wrapper                           
 │   │   ├── viewmodel/                                                         
 │   │   │   └── ChatViewModel.kt       # State management                      
 │   │   └── speech/                                                            
 │   │       └── SpeechRecognizerHelper.kt                                      
 │   └── res/                                                                   
 │       └── values/strings.xml                                                 
                                                                                
 ---                                                                            
 Implementation Steps                                                           
                                                                                
 Step 1: Project Setup                                                          
                                                                                
 - Create new Wear OS project in Android Studio                                 
 - Configure build.gradle.kts with dependencies:                                
   - androidx.wear.compose:compose-material3:1.5.6                              
   - androidx.wear.compose:compose-foundation:1.5.6                             
   - com.squareup.retrofit2:retrofit:2.9.0                                      
   - com.squareup.retrofit2:converter-gson:2.9.0                                
   - com.squareup.okhttp3:okhttp:4.11.0                                         
 - Set minSdk = 26, targetSdk = 34                                              
                                                                                
 Step 2: Manifest Configuration                                                 
                                                                                
 Required permissions:                                                          
 <uses-permission android:name="android.permission.INTERNET" />                 
 <uses-permission android:name="android.permission.RECORD_AUDIO" />             
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />     
 Required features:                                                             
 <uses-feature android:name="android.hardware.type.watch" />                    
 <meta-data android:name="com.google.android.wearable.standalone"               
 android:value="true" />                                                        
                                                                                
 Step 3: Claude API Client                                                      
                                                                                
 Create Retrofit service:                                                       
 - Endpoint: POST https://api.anthropic.com/v1/messages                         
 - Headers: x-api-key, anthropic-version: 2023-06-01                            
 - Request body: model, max_tokens (512 for watch), messages array              
 - Parse response: extract content[0].text                                      
                                                                                
 Step 4: Speech Recognition                                                     
                                                                                
 Implement SpeechRecognizerHelper:                                              
 - Create SpeechRecognizer.createSpeechRecognizer(context)                      
 - Set up RecognitionListener callbacks                                         
 - startListening() on button tap                                               
 - Extract text from onResults() callback                                       
 - Handle errors gracefully (show retry option)                                 
                                                                                
 Step 5: Main UI Screen                                                         
                                                                                
 Build with Compose for Wear OS:                                                
 - Idle state: Large mic button in center                                       
 - Listening state: Animated indicator + "Listening..."                         
 - Loading state: Progress indicator + "Thinking..."                            
 - Response state: ScalingLazyColumn with response text                         
                                                                                
 Key UI code pattern:                                                           
 val listState = rememberScalingLazyListState()                                 
 ScreenScaffold(scrollState = listState) {                                      
     ScalingLazyColumn(state = listState) {                                     
         item { /* Response text */ }                                           
     }                                                                          
 }                                                                              
 The bezel scrolling works automatically with ScalingLazyColumn.                
                                                                                
 Step 6: ViewModel & State                                                      
                                                                                
 sealed class UiState {                                                         
     object Idle : UiState()                                                    
     object Listening : UiState()                                               
     object Loading : UiState()                                                 
     data class Response(val text: String) : UiState()                          
     data class Error(val message: String) : UiState()                          
 }                                                                              
                                                                                
 data class Message(val role: String, val content: String)                      
 - Maintain List<Message> for conversation history (keep last 10 exchanges)     
 - Send full history to Claude API for context-aware responses                  
 - Handle API calls with coroutines                                             
 - Expose state via StateFlow                                                   
                                                                                
 Step 7: API Key Storage                                                        
                                                                                
 Options (in order of security):                                                
 1. Android Keystore - Most secure, encrypted storage                           
 2. EncryptedSharedPreferences - Good balance                                   
 3. BuildConfig field - Simplest for development (NOT for production)           
                                                                                
 For initial development, use BuildConfig. Add to build.gradle.kts:             
 buildConfigField("String", "CLAUDE_API_KEY", "\"your-key-here\"")              
                                                                                
 ---                                                                            
 Key Files to Create                                                            
 ┌───────────────────────────┬────────────────────────────┐                     
 │           File            │          Purpose           │                     
 ├───────────────────────────┼────────────────────────────┤                     
 │ build.gradle.kts          │ Dependencies & config      │                     
 ├───────────────────────────┼────────────────────────────┤                     
 │ AndroidManifest.xml       │ Permissions & app metadata │                     
 ├───────────────────────────┼────────────────────────────┤                     
 │ MainActivity.kt           │ Entry point, sets content  │                     
 ├───────────────────────────┼────────────────────────────┤                     
 │ MainScreen.kt             │ UI composable with states  │                     
 ├───────────────────────────┼────────────────────────────┤                     
 │ ChatViewModel.kt          │ Business logic & state     │                     
 ├───────────────────────────┼────────────────────────────┤                     
 │ ClaudeApi.kt              │ Retrofit interface         │                     
 ├───────────────────────────┼────────────────────────────┤                     
 │ SpeechRecognizerHelper.kt │ Voice input wrapper        │                     
 └───────────────────────────┴────────────────────────────┘                     
 ---                                                                            
 Verification Plan                                                              
                                                                                
 1. Build: ./gradlew assembleDebug should complete without errors               
 2. Install: Deploy to Galaxy Watch 7 emulator or physical device               
 3. Test speech: Tap mic, speak, verify text appears in logs                    
 4. Test API: Verify Claude response displays on screen                         
 5. Test scrolling: Long response should scroll with bezel rotation             
                                                                                
 ---                                                                            
 Design Decisions                                                               
                                                                                
 - API Key: Hardcoded in BuildConfig for development (can add settings later)   
 - Conversation History: Keep recent context (last 5-10 exchanges) so follow-up 
  questions work                                                                
 - Audio Output: Text only - no text-to-speech for now   