package com.vishingalert.app.detector


import android.util.Log

/**
 * Core threat detection engine for analyzing transcribed text
 * Implements pattern matching and threat scoring algorithm
 */
class ThreatDetectionEngine {

    data class ThreatAnalysisResult(
        val isThreat: Boolean,
        val threatLevel: Float, // 0.0 to 1.0
        val detectedKeywords: List<ThreatKeywordDatabase.ThreatKeyword>,
        val threatSummary: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "ThreatDetectionEngine"
        private const val THREAT_THRESHOLD = 0.5f
        private const val COMBINED_THRESHOLD = 0.6f
    }

    /**
     * Analyze transcribed text for threat indicators
     * Returns threat analysis with confidence score
     */
    fun analyzeText(text: String): ThreatAnalysisResult {
        if (text.trim().isEmpty()) {
            return ThreatAnalysisResult(
                isThreat = false,
                threatLevel = 0.0f,
                detectedKeywords = emptyList(),
                threatSummary = "No text to analyze"
            )
        }

        val detectedKeywords = ThreatKeywordDatabase.searchForThreats(text)

        if (detectedKeywords.isEmpty()) {
            return ThreatAnalysisResult(
                isThreat = false,
                threatLevel = 0.0f,
                detectedKeywords = emptyList(),
                threatSummary = "No threats detected"
            )
        }

        // Calculate threat level using weighted average
        val threatLevel = calculateThreatLevel(detectedKeywords)
        val isThreat = threatLevel >= THREAT_THRESHOLD

        val threatSummary = buildThreatSummary(detectedKeywords, threatLevel)

        Log.d(TAG, "Threat Analysis: isThreat=$isThreat, level=$threatLevel, keywords=${detectedKeywords.size}")

        return ThreatAnalysisResult(
            isThreat = isThreat,
            threatLevel = threatLevel,
            detectedKeywords = detectedKeywords,
            threatSummary = threatSummary
        )
    }

    /**
     * Calculate overall threat level using weighted average
     * Higher weights for more critical threat indicators
     */
    private fun calculateThreatLevel(keywords: List<ThreatKeywordDatabase.ThreatKeyword>): Float {
        if (keywords.isEmpty()) return 0.0f

        // Weight multiplier for high-severity keywords
        val weightedSum = keywords.sumOf { keyword ->
            when {
                keyword.severity >= 0.9f -> keyword.severity * 1.2 // High severity multiplier
                keyword.severity >= 0.8f -> keyword.severity * 1.1 // Medium-high multiplier
                else -> keyword.severity.toDouble()
            }
        }

        val baseScore = (weightedSum / keywords.size).toFloat()

        // Bonus for multiple threat categories (compound threats are more suspicious)
        val categoryCount = keywords.map { it.category }.distinct().size
        val categoryBonus = when {
            categoryCount >= 3 -> 0.15f // Multiple threat types = higher score
            categoryCount >= 2 -> 0.10f
            else -> 0.0f
        }

        return (baseScore + categoryBonus).coerceIn(0.0f, 1.0f)
    }

    /**
     * Build human-readable threat summary
     */
    private fun buildThreatSummary(
        keywords: List<ThreatKeywordDatabase.ThreatKeyword>,
        threatLevel: Float
    ): String {
        val topKeywords = keywords
            .sortedByDescending { it.severity }
            .take(3)
            .map { it.description }

        val threatLevelText = when {
            threatLevel >= 0.9f -> "CRITICAL"
            threatLevel >= 0.7f -> "HIGH"
            threatLevel >= 0.5f -> "MEDIUM"
            else -> "LOW"
        }

        return "[$threatLevelText] Detected: ${topKeywords.joinToString(", ")}"
    }

    /**
     * Get threat category breakdown
     */
    fun getThreadByCategoryBreakdown(text: String): Map<String, Int> {
        val keywords = ThreatKeywordDatabase.searchForThreats(text)
        return keywords.groupingBy { it.category }.eachCount()
    }
}
