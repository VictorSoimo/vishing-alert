package com.vishingalert.app

import com.vishingalert.app.detector.FraudDetectionEngine
import com.vishingalert.app.model.FraudIndicator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FraudDetectionEngine
 */
class FraudDetectionEngineTest {

    private lateinit var engine: FraudDetectionEngine

    @Before
    fun setup() {
        engine = FraudDetectionEngine()
    }

    @Test
    fun testBenignTextIsNotFlagged() {
        val text = "Hello, how are you doing today? Just wanted to say hi."
        val result = engine.analyzeText(text)
        
        assertFalse("Benign text should not be flagged as suspicious", result.isSuspicious)
        assertTrue("Confidence should be low for benign text", result.confidenceScore < 0.5f)
        assertTrue("Should have no fraud indicators", result.detectedIndicators.isEmpty())
    }

    @Test
    fun testUrgentLanguageDetected() {
        val text = "This is urgent! You must act immediately or your account will be closed."
        val result = engine.analyzeText(text)
        
        assertTrue("Urgent language should be detected", 
            result.detectedIndicators.contains(FraudIndicator.URGENT_LANGUAGE))
    }

    @Test
    fun testPersonalInfoRequestDetected() {
        val text = "Please provide your social security number and credit card details to verify your account."
        val result = engine.analyzeText(text)
        
        assertTrue("Should be flagged as suspicious", result.isSuspicious)
        assertTrue("Personal info request should be detected", 
            result.detectedIndicators.contains(FraudIndicator.PERSONAL_INFO_REQUEST))
        assertTrue("Account verification should be detected", 
            result.detectedIndicators.contains(FraudIndicator.ACCOUNT_VERIFICATION))
    }

    @Test
    fun testAccountVerificationDetected() {
        val text = "We need to verify your account. There has been unusual activity detected."
        val result = engine.analyzeText(text)
        
        assertTrue("Account verification should be detected", 
            result.detectedIndicators.contains(FraudIndicator.ACCOUNT_VERIFICATION))
    }

    @Test
    fun testPaymentRequestDetected() {
        val text = "You have an overdue payment. Pay now or face legal action from the IRS."
        val result = engine.analyzeText(text)
        
        assertTrue("Should be flagged as suspicious", result.isSuspicious)
        assertTrue("Payment request should be detected", 
            result.detectedIndicators.contains(FraudIndicator.PAYMENT_REQUEST))
        assertTrue("Impersonation should be detected", 
            result.detectedIndicators.contains(FraudIndicator.IMPERSONATION))
    }

    @Test
    fun testThreateningLanguageDetected() {
        val text = "You will face legal action and arrest if you don't comply immediately."
        val result = engine.analyzeText(text)
        
        assertTrue("Threatening consequences should be detected", 
            result.detectedIndicators.contains(FraudIndicator.THREATENING_CONSEQUENCES))
    }

    @Test
    fun testImpersonationDetected() {
        val text = "This is Microsoft technical support. Your computer has a virus."
        val result = engine.analyzeText(text)
        
        assertTrue("Impersonation should be detected", 
            result.detectedIndicators.contains(FraudIndicator.IMPERSONATION))
    }

    @Test
    fun testSuspiciousLinkDetected() {
        val text = "Click here to verify your account: http://suspicious-link.com"
        val result = engine.analyzeText(text)
        
        assertTrue("Suspicious link should be detected", 
            result.detectedIndicators.contains(FraudIndicator.SUSPICIOUS_LINK))
        assertTrue("Account verification should be detected", 
            result.detectedIndicators.contains(FraudIndicator.ACCOUNT_VERIFICATION))
    }

    @Test
    fun testTimePressureDetected() {
        val text = "You must respond within 24 hours or your account will be closed."
        val result = engine.analyzeText(text)
        
        assertTrue("Time pressure should be detected", 
            result.detectedIndicators.contains(FraudIndicator.TIME_PRESSURE))
    }

    @Test
    fun testPrizeScamDetected() {
        val text = "Congratulations! You won a free gift. Click here to claim your prize."
        val result = engine.analyzeText(text)
        
        assertTrue("Prize scam should be detected", 
            result.detectedIndicators.contains(FraudIndicator.PRIZE_OR_REWARD))
        assertTrue("Suspicious link should be detected", 
            result.detectedIndicators.contains(FraudIndicator.SUSPICIOUS_LINK))
    }

    @Test
    fun testCallbackRequestDetected() {
        val text = "Please call this number back immediately: 1-800-SCAMMER"
        val result = engine.analyzeText(text)
        
        assertTrue("Callback request should be detected", 
            result.detectedIndicators.contains(FraudIndicator.CALLBACK_REQUEST))
    }

    @Test
    fun testMultipleIndicatorsIncreaseSuspicion() {
        val text = "URGENT: This is the IRS. You owe taxes. Pay now or face arrest. " +
                  "Provide your social security number and credit card within 24 hours."
        val result = engine.analyzeText(text)
        
        assertTrue("Should be flagged as highly suspicious", result.isSuspicious)
        assertTrue("Should have multiple indicators", result.detectedIndicators.size >= 3)
        assertTrue("Confidence score should be high", result.confidenceScore >= 0.7f)
    }

    @Test
    fun testAnonymizedHashGeneration() {
        val text = "Test message for hashing"
        val hash1 = engine.generateAnonymizedHash(text)
        val hash2 = engine.generateAnonymizedHash(text)
        
        assertNotNull("Hash should not be null", hash1)
        assertFalse("Hash should not be empty", hash1.isEmpty())
        assertEquals("Same text should produce same hash", hash1, hash2)
        assertEquals("SHA-256 hash should be 64 characters", 64, hash1.length)
    }

    @Test
    fun testDifferentTextProducesDifferentHash() {
        val text1 = "Message 1"
        val text2 = "Message 2"
        val hash1 = engine.generateAnonymizedHash(text1)
        val hash2 = engine.generateAnonymizedHash(text2)
        
        assertNotEquals("Different texts should produce different hashes", hash1, hash2)
    }

    @Test
    fun testCaseInsensitiveDetection() {
        val text1 = "URGENT: Verify your account NOW!"
        val text2 = "urgent: verify your account now!"
        
        val result1 = engine.analyzeText(text1)
        val result2 = engine.analyzeText(text2)
        
        assertEquals("Detection should be case-insensitive", 
            result1.detectedIndicators.size, result2.detectedIndicators.size)
    }

    @Test
    fun testEmptyTextHandling() {
        val result = engine.analyzeText("")
        
        assertFalse("Empty text should not be suspicious", result.isSuspicious)
        assertEquals("Confidence should be 0 for empty text", 0f, result.confidenceScore, 0.01f)
        assertTrue("Should have no indicators", result.detectedIndicators.isEmpty())
    }

    @Test
    fun testRealWorldVishingExample() {
        val text = "Hello, this is Amazon customer service. There has been suspicious activity " +
                  "on your account. Please verify your identity by providing your password and " +
                  "credit card information immediately, or your account will be suspended within 24 hours."
        
        val result = engine.analyzeText(text)
        
        assertTrue("Real vishing example should be detected", result.isSuspicious)
        assertTrue("Should detect impersonation", 
            result.detectedIndicators.contains(FraudIndicator.IMPERSONATION))
        assertTrue("Should detect personal info request", 
            result.detectedIndicators.contains(FraudIndicator.PERSONAL_INFO_REQUEST))
        assertTrue("Should detect account verification", 
            result.detectedIndicators.contains(FraudIndicator.ACCOUNT_VERIFICATION))
        assertTrue("Should detect time pressure", 
            result.detectedIndicators.contains(FraudIndicator.TIME_PRESSURE))
    }

    @Test
    fun testRealWorldSmishingExample() {
        val text = "URGENT: Your bank account has been locked due to suspicious activity. " +
                  "Click this link to verify your identity: http://fake-bank.com/verify"
        
        val result = engine.analyzeText(text)
        
        assertTrue("Real smishing example should be detected", result.isSuspicious)
        assertTrue("Should detect urgent language", 
            result.detectedIndicators.contains(FraudIndicator.URGENT_LANGUAGE))
        assertTrue("Should detect account verification", 
            result.detectedIndicators.contains(FraudIndicator.ACCOUNT_VERIFICATION))
        assertTrue("Should detect suspicious link", 
            result.detectedIndicators.contains(FraudIndicator.SUSPICIOUS_LINK))
    }
}
