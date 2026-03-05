# Vishing Alert 🛡️

A privacy-focused mobile app for detecting SMS (smishing) and call-based (vishing) fraud using on-device ML processing.

## 🎯 Overview

Vishing Alert is an Android application that protects users from phone and SMS fraud by analyzing communications in real-time using on-device machine learning. The app never sends your data to the cloud, ensuring complete privacy.

### Key Features

- **🔐 Privacy-First**: All processing done on-device - no cloud, no data sharing
- **📞 Vishing Detection**: Real-time analysis of phone calls for fraud indicators
- **💬 Smishing Detection**: Automatic scanning of incoming SMS messages
- **🧠 On-Device NLP**: Advanced natural language processing without internet
- **🔊 Speech-to-Text**: Real-time transcription using Android's built-in STT
- **🔒 Anonymized Reporting**: SHA-256 hashing for privacy-preserving threat intelligence
- **⚡ Real-Time Alerts**: Instant notifications when fraud is detected

## 🏗️ Architecture

### Core Components

1. **Fraud Detection Engine** (`FraudDetectionEngine.kt`)
   - Pattern-based NLP analysis
   - 10 different fraud indicator categories
   - Confidence scoring algorithm
   - Privacy-preserving hash generation

2. **Speech-to-Text Handler** (`SpeechToTextHandler.kt`)
   - On-device speech recognition
   - Real-time transcription during calls
   - Partial results for early detection

3. **Call Monitoring Service** (`CallMonitoringService.kt`)
   - Foreground service for continuous monitoring
   - Integrates STT with fraud detection
   - Real-time alert generation

4. **SMS Receiver** (`SmsReceiver.kt`)
   - Intercepts incoming SMS messages
   - Analyzes for smishing indicators
   - Instant threat notifications

### Fraud Indicators

The app detects the following fraud patterns:

1. **Urgent Language** - "act now", "immediately", "emergency"
2. **Personal Info Requests** - SSN, credit card, passwords
3. **Account Verification** - "verify your account", "unusual activity"
4. **Payment Demands** - "pay now", "overdue", "final notice"
5. **Threatening Language** - "legal action", "arrest warrant"
6. **Impersonation** - IRS, banks, tech support
7. **Suspicious Links** - URL shorteners, unknown domains
8. **Time Pressure** - "within 24 hours", "expires today"
9. **Prize Scams** - "you won", "claim your prize"
10. **Callback Requests** - "call this number back"

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 26 (Android 8.0) or higher
- Gradle 8.0+

### Building the App

1. Clone the repository:
```bash
git clone https://github.com/VictorSoimo/vishing-alert.git
cd vishing-alert
```

2. Open the project in Android Studio

3. Sync Gradle files:
```bash
./gradlew build
```

4. Run the app:
```bash
./gradlew installDebug
```

Or use Android Studio's Run button.

### Running Tests

Execute the unit tests:
```bash
./gradlew test
```

View test results:
```bash
./gradlew test --info
```

## 📱 Usage

### First Launch

1. Grant required permissions:
   - Phone State Access
   - Call Log
   - Microphone
   - SMS Read/Receive
   - Notifications

2. Enable monitoring from the main screen

3. The app will now protect you automatically

### Monitoring

- **Green Status**: Monitoring active, you're protected
- **Gray Status**: Monitoring inactive
- **Statistics**: View calls monitored and threats detected
- **Alerts**: Receive instant notifications when fraud is detected

## 🔒 Privacy & Security

### On-Device Processing

- **No Cloud**: All analysis happens locally on your device
- **No Data Sharing**: Your calls and messages never leave your phone
- **No Account Required**: No sign-up, no tracking
- **No Network Needed**: Works completely offline

### Anonymized Reporting

When enabled, threat patterns are hashed using SHA-256 before sharing:
- Original content is never transmitted
- Only cryptographic hashes are shared
- Hash-based pattern matching protects the community
- Completely anonymous and privacy-preserving

## 🛠️ Technical Details

### Technologies Used

- **Language**: Kotlin
- **UI**: Material Design 3
- **STT**: Android SpeechRecognizer API
- **Storage**: SharedPreferences
- **Architecture**: MVVM with Services
- **Concurrency**: Kotlin Coroutines

### Permissions Required

- `READ_PHONE_STATE` - Detect incoming calls
- `READ_CALL_LOG` - Access call metadata
- `RECORD_AUDIO` - Capture call audio for STT
- `RECEIVE_SMS` - Intercept incoming SMS
- `READ_SMS` - Read message content
- `POST_NOTIFICATIONS` - Show fraud alerts
- `FOREGROUND_SERVICE` - Continuous monitoring

### Limitations

**Call Audio Capture**: Due to Android security restrictions, capturing live call audio requires:
- Android 10+ Call Screening Service
- System-level permissions (root/OEM partnership)
- Or Accessibility Service implementation

The current implementation provides the architecture and can be extended with device-specific solutions.

## 🧪 Testing

The app includes comprehensive unit tests:

- Fraud detection accuracy tests
- Hash generation validation
- Case-insensitive pattern matching
- Real-world vishing/smishing examples
- Edge case handling

Test coverage focuses on the fraud detection engine to ensure accurate threat identification.

## 📊 Project Structure

```
vishing-alert/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/vishingalert/app/
│   │   │   │   ├── detector/           # ML and STT components
│   │   │   │   ├── model/              # Data models
│   │   │   │   ├── receiver/           # Broadcast receivers
│   │   │   │   ├── service/            # Background services
│   │   │   │   ├── util/               # Utilities
│   │   │   │   └── MainActivity.kt     # Main UI
│   │   │   ├── res/                    # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Unit tests
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Guidelines

1. Follow Kotlin coding conventions
2. Add unit tests for new features
3. Update documentation
4. Maintain privacy-first principles
5. Test on multiple Android versions

## 📄 License

This project is open source and available under the MIT License.

## ⚠️ Disclaimer

This app provides fraud detection assistance but should not be your only line of defense. Always:
- Verify caller identity through official channels
- Never share personal information over the phone
- Report suspicious calls to authorities
- Stay informed about common scam tactics

## 🔮 Future Enhancements

- [ ] Machine learning model training with TensorFlow Lite
- [ ] Community threat intelligence sharing
- [ ] Caller ID reputation database
- [ ] Multi-language support
- [ ] Advanced audio analysis with frequency detection
- [ ] Integration with phone carriers' spam protection

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Review existing documentation
- Check the FAQ in the wiki

---

**Stay Safe. Stay Protected. Stay Private.** 🛡️
