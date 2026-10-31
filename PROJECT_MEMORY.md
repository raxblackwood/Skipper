# Skipper - Project Memory

- **Project ID**: `5435ae1b-0452-4577-b1de-9804ddb347a2`
- **Workspace**: `E:\CODES\Miscs\Skipper`
- **Status**: Active (Release Preparation Ready)

## 1. Executive Summary

Skipper is an Android utility leveraging `AccessibilityService` to automate anonymous chat workflows.
It auto-skips instantly disconnected partners and filters incoming chat profiles based on customizable keywords and gender indicators.
It includes safety guards preventing auto-skips once active conversational exchanges have occurred and integrates Google Play In-App Billing for recurring tiers.

## 2. Technical Scope & Stack

- **Stack**: Android (Kotlin, AndroidX, Material Design 3, Coroutines, SharedPreferences).
- **Automation Engine**: `AccessibilityService` node hierarchy traversal and gesture dispatch.
- **Billing & Licensing Engine**: Google Play Billing Library v8 with local test mode and cryptographic hardware-bound promo key validation.
- **Companion Engine**: Python desktop ADB script (`scripts/skipper_adb.py`, `scripts/skipper_gui.py`).
- **Target Integrations**: AnonChat (`com.anonchat.android`), Chatous (`com.chatous`).

## 3. Milestones & Current Focus

- Prepared full Google Play Store release kit and metadata documentation (`PLAY_STORE_METADATA.md`).
- Verified signed release Android App Bundle `Skipper.aab` (RSA 2048-bit, valid until 2054, targetSdk 36).
- Generated complete store listing visual assets: high-resolution icon (512x512), feature graphic (1024x500), and 4 showcase screenshots (1080x1920).
- Published comprehensive `PRIVACY_POLICY.md` strictly adhering to Google Play User Data and Accessibility Service disclosure requirements.
- Completed Data Safety questionnaire and Accessibility Service declaration answers for Google review.
- Fixed false-positive conversational exchange detection and resolved test subscription persistence.
- Fixed `applicationId` casing to `com.Skipper.app` to match Google Play Console registered package name.
- Upgraded Play Billing Library to v8.0.0 and updated BillingClient API implementations.
- Upgraded `compileSdk = 36` and `targetSdk = 36` to satisfy Google Play's required Android API level 36 target.
- Bumped `versionCode` to 6 (`v1.2.3`).
- Built clean release bundle and placed it at `builds/Skipper_v6_api36.aab`, `builds/Skipper_latest.aab`, and workspace root `Skipper.aab`.
- Streamlined and decluttered home screen UI: reduced status indicator to a discreet dynamic status dot beside top brand title, compacted trial banner, simplified app section cards, and moved trigger logs into a side navigation drawer.
- Created standalone HTML privacy policy page (`index.html`) deployed to GitHub Pages (`https://raxblackwood.github.io/Skipper/`) to eliminate Google Play automated crawler rejection of raw repository blob URLs.

## 4. Key Invariants & Safeguards

- Never auto-skip if conversational exchanges exist (presence of message timestamps, "Me" markers, or "Seen" receipts).
- Never click "New partner" nodes located in the top half of the screen; the action button strictly resides at the bottom (>60% screen height).
- Google Play Billing purchase checks must never revoke non-store test tiers (`PRO_TEST`, `PRO_PROMO`, `PRO_MANUAL`).
- Always verify accessibility node bounds before dispatching click gestures.
- Mirror core heuristic logic between Android Kotlin modules and desktop Python ADB companion scripts.
