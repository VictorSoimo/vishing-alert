package com.vishingalert.app.detector



/**
 * Central repository for social engineering threat keywords and phrases
 * Organized by attack category with configurable scoring
 */
object ThreatKeywordDatabase {

    data class ThreatKeyword(
        val phrase: String,
        val category: String,
        val severity: Float, // 0.1 to 1.0
        val description: String
    )

    private val threatKeywords = listOf(
        // Urgency & Time Pressure
        ThreatKeyword("act now", "URGENCY", 0.8f, "Urgent action request"),
        ThreatKeyword("immediately", "URGENCY", 0.8f, "Immediate action required"),
        ThreatKeyword("right away", "URGENCY", 0.75f, "Time-sensitive demand"),
        ThreatKeyword("do not delay", "URGENCY", 0.75f, "Pressure to act quickly"),
        ThreatKeyword("expires today", "URGENCY", 0.8f, "Time limit pressure"),
        ThreatKeyword("24 hours", "URGENCY", 0.7f, "Short deadline"),
        ThreatKeyword("urgent", "URGENCY", 0.7f, "Marked as urgent"),

        // Personal Information Requests
        ThreatKeyword("password", "DATA_REQUEST", 0.95f, "Password request"),
        ThreatKeyword("social security number", "DATA_REQUEST", 0.95f, "SSN request"),
        ThreatKeyword("ssn", "DATA_REQUEST", 0.95f, "SSN abbreviation"),
        ThreatKeyword("credit card", "DATA_REQUEST", 0.95f, "Credit card request"),
        ThreatKeyword("pin number", "DATA_REQUEST", 0.9f, "PIN request"),
        ThreatKeyword("account number", "DATA_REQUEST", 0.9f, "Account number request"),
        ThreatKeyword("date of birth", "DATA_REQUEST", 0.85f, "DOB request"),
        ThreatKeyword("mother's maiden name", "DATA_REQUEST", 0.85f, "Security question"),
        ThreatKeyword("verify your identity", "DATA_REQUEST", 0.85f, "Identity verification"),

        // Account Verification Attempts
        ThreatKeyword("verify account", "ACCOUNT_VERIFY", 0.9f, "Account verification attempt"),
        ThreatKeyword("confirm account", "ACCOUNT_VERIFY", 0.9f, "Account confirmation"),
        ThreatKeyword("unusual activity", "ACCOUNT_VERIFY", 0.85f, "Suspicious activity claim"),
        ThreatKeyword("unauthorized access", "ACCOUNT_VERIFY", 0.85f, "Unauthorized access claim"),
        ThreatKeyword("compromised", "ACCOUNT_VERIFY", 0.85f, "Account compromise claim"),
        ThreatKeyword("suspended", "ACCOUNT_VERIFY", 0.8f, "Account suspension threat"),
        ThreatKeyword("locked", "ACCOUNT_VERIFY", 0.8f, "Account lockout threat"),

        // Payment Demands
        ThreatKeyword("pay now", "PAYMENT", 0.95f, "Immediate payment demand"),
        ThreatKeyword("send payment", "PAYMENT", 0.9f, "Payment request"),
        ThreatKeyword("wire transfer", "PAYMENT", 0.9f, "Wire transfer request"),
        ThreatKeyword("overdue", "PAYMENT", 0.85f, "Payment overdue claim"),
        ThreatKeyword("invoice", "PAYMENT", 0.7f, "Invoice mention (contextual)"),

        // Authority Impersonation
        ThreatKeyword("this is the irs", "IMPERSONATION", 0.95f, "IRS impersonation"),
        ThreatKeyword("from the bank", "IMPERSONATION", 0.9f, "Bank employee claim"),
        ThreatKeyword("from your bank", "IMPERSONATION", 0.9f, "Bank representative claim"),
        ThreatKeyword("tech support", "IMPERSONATION", 0.85f, "Tech support claim"),
        ThreatKeyword("customer service", "IMPERSONATION", 0.6f, "Generic service claim"),
        ThreatKeyword("federal agent", "IMPERSONATION", 0.95f, "Federal agent claim"),
        ThreatKeyword("fbi", "IMPERSONATION", 0.95f, "FBI impersonation"),

        // Threatening Consequences
        ThreatKeyword("legal action", "THREAT", 0.9f, "Legal threat"),
        ThreatKeyword("arrest", "THREAT", 0.95f, "Arrest threat"),
        ThreatKeyword("jail", "THREAT", 0.95f, "Jail threat"),
        ThreatKeyword("penalty", "THREAT", 0.85f, "Penalty threat"),
        ThreatKeyword("sue", "THREAT", 0.85f, "Lawsuit threat"),
        ThreatKeyword("fine", "THREAT", 0.8f, "Fine threat"),

        // Suspicious Links/URLs
        ThreatKeyword("click here", "LINK", 0.75f, "Click link request"),
        ThreatKeyword("link", "LINK", 0.6f, "Link mention"),
        ThreatKeyword("download", "LINK", 0.7f, "Download request"),
        ThreatKeyword("http://", "LINK", 0.65f, "URL present"),
        ThreatKeyword("https://", "LINK", 0.6f, "Secure URL"),

        // Prize/Reward Scams
        ThreatKeyword("you won", "PRIZE", 0.9f, "Prize won claim"),
        ThreatKeyword("claim your prize", "PRIZE", 0.9f, "Prize claim"),
        ThreatKeyword("congratulations", "PRIZE", 0.6f, "Congratulations (contextual)"),
        ThreatKeyword("lottery", "PRIZE", 0.85f, "Lottery scam"),

        // Callback/System Interaction Requests
        ThreatKeyword("press 1", "CALLBACK", 0.7f, "Key press request"),
        ThreatKeyword("call me back", "CALLBACK", 0.65f, "Callback request"),
        ThreatKeyword("return this call", "CALLBACK", 0.65f, "Call return request"),
    )

    fun getAllKeywords(): List<ThreatKeyword> = threatKeywords

    fun getKeywordsByCategory(category: String): List<ThreatKeyword> {
        return threatKeywords.filter { it.category == category }
    }

    fun searchForThreats(text: String): List<ThreatKeyword> {
        val lowerText = text.lowercase()
        return threatKeywords.filter { keyword ->
            lowerText.contains(keyword.phrase.lowercase())
        }
    }

    fun getCategories(): Set<String> = threatKeywords.map { it.category }.toSet()
}
