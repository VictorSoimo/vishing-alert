# Contributing to Vishing Alert

Thank you for your interest in contributing to Vishing Alert! This document provides guidelines and instructions for contributing.

## 🎯 Project Goals

- Privacy-first fraud detection
- On-device processing (no cloud)
- Accurate fraud pattern detection
- Minimal false positives
- User-friendly interface
- Excellent code quality

## 🏗️ Development Setup

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK 26+
- Git

### Initial Setup

1. Fork the repository
2. Clone your fork:
```bash
git clone https://github.com/YOUR_USERNAME/vishing-alert.git
cd vishing-alert
```

3. Open in Android Studio
4. Sync Gradle files
5. Build the project

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 📝 Code Style

### Kotlin Guidelines

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Write self-documenting code
- Add comments for complex logic
- Maximum line length: 120 characters

### Architecture Patterns

- Use MVVM for UI components
- Services for background processing
- Repository pattern for data access
- Dependency injection where appropriate

## 🧪 Testing

### Unit Tests

- Required for all new features
- Test both happy and error paths
- Aim for >80% code coverage
- Use descriptive test names

Example:
```kotlin
@Test
fun testFraudDetectionWithMultipleIndicators() {
    // Arrange
    val text = "URGENT: Verify your account now!"
    
    // Act
    val result = engine.analyzeText(text)
    
    // Assert
    assertTrue(result.isSuspicious)
    assertTrue(result.detectedIndicators.isNotEmpty())
}
```

### Integration Tests

- Test component interactions
- Verify service lifecycle
- Test permission handling

## 🔒 Security Guidelines

### Code Review Checklist

- [ ] No hardcoded credentials or API keys
- [ ] Proper input validation
- [ ] Secure data storage
- [ ] No sensitive data in logs
- [ ] Permissions properly justified
- [ ] Privacy-preserving implementations

### Vulnerability Reporting

Report security issues privately to the maintainers. Do not create public issues for security vulnerabilities.

## 📋 Pull Request Process

### Before Submitting

1. Update documentation if needed
2. Add/update tests
3. Run all tests locally
4. Check code style
5. Update CHANGELOG.md

### PR Guidelines

- Create feature branches from `main`
- Use descriptive branch names: `feature/fraud-detection-improvement`
- Write clear commit messages
- Reference related issues
- Keep PRs focused and atomic

### Commit Messages

Follow conventional commits format:

```
feat: add new fraud indicator for prize scams
fix: correct confidence score calculation
docs: update README with new examples
test: add tests for SMS receiver
refactor: simplify fraud detection logic
```

### Review Process

1. Submit PR with description
2. Automated tests run
3. Code review by maintainers
4. Address feedback
5. Merge when approved

## 🎨 UI/UX Contributions

- Follow Material Design 3 guidelines
- Maintain consistent styling
- Test on multiple screen sizes
- Consider accessibility (a11y)
- Provide screenshots in PR

## 📚 Documentation

### What to Document

- Public APIs and functions
- Complex algorithms
- Configuration options
- Setup instructions
- Usage examples

### Documentation Style

```kotlin
/**
 * Analyzes text for fraud indicators using NLP
 * 
 * @param text The text to analyze from STT or SMS
 * @return FraudAnalysisResult with detected indicators and confidence score
 */
fun analyzeText(text: String): FraudAnalysisResult
```

## 🐛 Bug Reports

### Template

```markdown
**Description**
Clear description of the bug

**Steps to Reproduce**
1. Step one
2. Step two
3. ...

**Expected Behavior**
What should happen

**Actual Behavior**
What actually happens

**Environment**
- Android Version: 
- Device: 
- App Version:

**Logs**
Relevant log output
```

## 💡 Feature Requests

### Template

```markdown
**Problem Statement**
Describe the problem this feature solves

**Proposed Solution**
Your suggested approach

**Alternatives Considered**
Other approaches you've thought about

**Privacy Implications**
How this affects user privacy

**Additional Context**
Any other relevant information
```

## 🎖️ Recognition

Contributors will be:
- Listed in CONTRIBUTORS.md
- Credited in release notes
- Acknowledged in the app (optional)

## 📜 Code of Conduct

### Our Pledge

We pledge to make participation in our project a harassment-free experience for everyone.

### Standards

**Positive behaviors:**
- Using welcoming language
- Respecting differing viewpoints
- Accepting constructive criticism
- Focusing on what's best for the community

**Unacceptable behaviors:**
- Harassment or discriminatory language
- Trolling or insulting comments
- Personal or political attacks
- Publishing others' private information

## 📞 Getting Help

- 💬 Discussion Forum: GitHub Discussions
- 🐛 Bug Reports: GitHub Issues
- 📧 Email: [maintainer contact]
- 📚 Documentation: Wiki

## 🎯 Good First Issues

Look for issues labeled `good-first-issue` - these are great starting points for new contributors!

## 🔄 Development Workflow

1. **Pick an Issue**: Find or create an issue
2. **Discuss**: Comment on the issue to discuss approach
3. **Implement**: Create feature branch and implement
4. **Test**: Write and run tests
5. **Document**: Update relevant documentation
6. **Submit**: Create pull request
7. **Review**: Address review feedback
8. **Merge**: Maintainers merge when ready

## 📊 Project Priorities

Current focus areas:
1. Improving fraud detection accuracy
2. Reducing false positives
3. Enhancing privacy features
4. Better user experience
5. Performance optimization

## 🙏 Thank You

Every contribution, no matter how small, is valuable. Thank you for helping make Vishing Alert better!

---

*Last updated: October 2025*
