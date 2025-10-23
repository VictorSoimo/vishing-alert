package com.vishingalert.app.model

/**
 * Data class representing a fraud detection result
 */
data class FraudAnalysisResult(
    val isSuspicious: Boolean,
    val confidenceScore: Float,
    val detectedIndicators: List<FraudIndicator>,
    val transcribedText: String = ""
)

/**
 * Enum class for different types of fraud indicators
 */
enum class FraudIndicator(val description: String, val weight: Float) {
    URGENT_LANGUAGE("Urgent or threatening language", 0.8f),
    PERSONAL_INFO_REQUEST("Requesting personal information", 0.9f),
    ACCOUNT_VERIFICATION("Account verification request", 0.85f),
    PAYMENT_REQUEST("Immediate payment demand", 0.9f),
    THREATENING_CONSEQUENCES("Threats of legal action or account closure", 0.85f),
    IMPERSONATION("Impersonating authority or company", 0.9f),
    SUSPICIOUS_LINK("Mentions suspicious links or downloads", 0.7f),
    TIME_PRESSURE("Creating artificial time pressure", 0.75f),
    PRIZE_OR_REWARD("Unexpected prize or reward claim", 0.7f),
    CALLBACK_REQUEST("Requesting callback to specific number", 0.65f)
}

/**
 * Data class for call information
 */
data class CallInfo(
    val phoneNumber: String,
    val timestamp: Long,
    val duration: Long = 0,
    val wasAnalyzed: Boolean = false,
    val fraudResult: FraudAnalysisResult? = null
)

/**
 * Data class for SMS information
 */
data class SmsInfo(
    val phoneNumber: String,
    val message: String,
    val timestamp: Long,
    val fraudResult: FraudAnalysisResult? = null
)
