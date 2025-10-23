package com.vishingalert.app.detector

import com.vishingalert.app.model.FraudAnalysisResult
import com.vishingalert.app.model.FraudIndicator
import java.util.Locale

/**
 * On-device NLP-based fraud detection engine
 * Uses pattern matching and keyword analysis to detect potential vishing/smishing attempts
 */
class FraudDetectionEngine {

    companion object {
        // Threshold for considering something suspicious (0.0 to 1.0)
        private const val SUSPICION_THRESHOLD = 0.5f
        
        // Keyword patterns for different fraud indicators
        private val urgentKeywords = listOf(
            "urgent", "immediately", "right now", "emergency", "critical",
            "act now", "expires today", "limited time", "hurry"
        )
        
        private val personalInfoKeywords = listOf(
            "social security", "ssn", "credit card", "bank account",
            "password", "pin", "cvv", "date of birth", "mother's maiden",
            "account number", "routing number", "verification code"
        )
        
        private val verificationKeywords = listOf(
            "verify your account", "confirm your identity", "validate your",
            "update your information", "suspended account", "locked account",
            "unusual activity", "unauthorized access"
        )
        
        private val paymentKeywords = listOf(
            "payment required", "pay now", "settle debt", "overdue payment",
            "final notice", "legal action", "warrant", "arrest", "irs",
            "tax owed", "fine", "penalty"
        )
        
        private val threatKeywords = listOf(
            "legal action", "lawsuit", "court", "arrest warrant", "police",
            "suspend", "close your account", "terminate", "consequences"
        )
        
        private val impersonationKeywords = listOf(
            "irs", "social security administration", "medicare", "bank of",
            "microsoft", "apple", "amazon", "google", "technical support",
            "customer service", "fraud department"
        )
        
        private val linkKeywords = listOf(
            "click here", "click this link", "visit this", "download",
            "install", "bit.ly", "tinyurl", "http", "https"
        )
        
        private val timePressureKeywords = listOf(
            "within 24 hours", "by end of day", "before", "deadline",
            "last chance", "final opportunity", "expires"
        )
        
        private val prizeKeywords = listOf(
            "you won", "you've been selected", "congratulations",
            "free gift", "claim your prize", "winner", "lucky"
        )
        
        private val callbackKeywords = listOf(
            "call back", "call this number", "press 1", "reply",
            "respond immediately", "contact us"
        )
    }
    
    /**
     * Analyzes text for fraud indicators
     * @param text The text to analyze (from STT or SMS)
     * @return FraudAnalysisResult with detection details
     */
    fun analyzeText(text: String): FraudAnalysisResult {
        val normalizedText = text.lowercase(Locale.getDefault())
        val detectedIndicators = mutableListOf<FraudIndicator>()
        var totalScore = 0f
        
        // Check for each fraud indicator
        if (containsKeywords(normalizedText, urgentKeywords)) {
            detectedIndicators.add(FraudIndicator.URGENT_LANGUAGE)
            totalScore += FraudIndicator.URGENT_LANGUAGE.weight
        }
        
        if (containsKeywords(normalizedText, personalInfoKeywords)) {
            detectedIndicators.add(FraudIndicator.PERSONAL_INFO_REQUEST)
            totalScore += FraudIndicator.PERSONAL_INFO_REQUEST.weight
        }
        
        if (containsKeywords(normalizedText, verificationKeywords)) {
            detectedIndicators.add(FraudIndicator.ACCOUNT_VERIFICATION)
            totalScore += FraudIndicator.ACCOUNT_VERIFICATION.weight
        }
        
        if (containsKeywords(normalizedText, paymentKeywords)) {
            detectedIndicators.add(FraudIndicator.PAYMENT_REQUEST)
            totalScore += FraudIndicator.PAYMENT_REQUEST.weight
        }
        
        if (containsKeywords(normalizedText, threatKeywords)) {
            detectedIndicators.add(FraudIndicator.THREATENING_CONSEQUENCES)
            totalScore += FraudIndicator.THREATENING_CONSEQUENCES.weight
        }
        
        if (containsKeywords(normalizedText, impersonationKeywords)) {
            detectedIndicators.add(FraudIndicator.IMPERSONATION)
            totalScore += FraudIndicator.IMPERSONATION.weight
        }
        
        if (containsKeywords(normalizedText, linkKeywords)) {
            detectedIndicators.add(FraudIndicator.SUSPICIOUS_LINK)
            totalScore += FraudIndicator.SUSPICIOUS_LINK.weight
        }
        
        if (containsKeywords(normalizedText, timePressureKeywords)) {
            detectedIndicators.add(FraudIndicator.TIME_PRESSURE)
            totalScore += FraudIndicator.TIME_PRESSURE.weight
        }
        
        if (containsKeywords(normalizedText, prizeKeywords)) {
            detectedIndicators.add(FraudIndicator.PRIZE_OR_REWARD)
            totalScore += FraudIndicator.PRIZE_OR_REWARD.weight
        }
        
        if (containsKeywords(normalizedText, callbackKeywords)) {
            detectedIndicators.add(FraudIndicator.CALLBACK_REQUEST)
            totalScore += FraudIndicator.CALLBACK_REQUEST.weight
        }
        
        // Normalize score (max possible score if all indicators present)
        val maxPossibleScore = FraudIndicator.values().sumOf { it.weight.toDouble() }.toFloat()
        val normalizedScore = (totalScore / maxPossibleScore).coerceIn(0f, 1f)
        
        // Determine if suspicious based on threshold
        val isSuspicious = normalizedScore >= SUSPICION_THRESHOLD || detectedIndicators.size >= 3
        
        return FraudAnalysisResult(
            isSuspicious = isSuspicious,
            confidenceScore = normalizedScore,
            detectedIndicators = detectedIndicators,
            transcribedText = text
        )
    }
    
    /**
     * Checks if text contains any of the specified keywords
     */
    private fun containsKeywords(text: String, keywords: List<String>): Boolean {
        return keywords.any { keyword -> text.contains(keyword) }
    }
    
    /**
     * Generates a hash for anonymized reporting
     * @param text The text to hash
     * @return SHA-256 hash as hex string
     */
    fun generateAnonymizedHash(text: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(text.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
