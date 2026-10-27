# Skipper - Project Memory

- **Project ID**: `5435ae1b-0452-4577-b1de-9804ddb347a2`
- **Workspace**: `E:\CODES\Miscs\Skipper`
- **Status**: Active

## 1. Executive Summary

Skipper is an Android utility leveraging `AccessibilityService` to automate anonymous chat workflows.
It auto-skips instantly disconnected partners and filters incoming chat profiles based on customizable keywords and gender indicators.
It also includes safety guards preventing auto-skips once active conversational exchanges have occurred.

## 2. Technical Scope & Stack

- **Stack**: Android (Kotlin, AndroidX, Material Design 3, Coroutines, SharedPreferences).
- **Automation Engine**: `AccessibilityService` node hierarchy traversal and gesture dispatch.
- **Billing & Licensing Engine**: Google Play Billing Library with local test mode and cryptographic hardware-bound promo key validation.
- **Companion Engine**: Python desktop ADB script (`scripts/skipper_adb.py`, `scripts/skipper_gui.py`).
- **Target Integrations**: AnonChat (`com.anonchat.android`), Chatous (`com.chatous`).

## 3. Milestones & Current Focus

- Fixed false-positive conversational exchange detection: eliminated flawed heuristic that misidentified multi-line bios and interest tags as chat bubbles.
- Chat exchange verification now strictly checks real message timestamps (`\d{1,2}:\d{2}\s*(?:AM|PM)`), read receipts (`"Seen"`), quote badges (`"Me"`), or total absence of profile cards on ended screens.
- Fixed recurring trial expiration bug: prevented `BillingManager` from overwriting test activations and promo unlocks to `false` when Google Play returns zero active purchases.
- Added permanent Test PRO mode allowing uninterrupted unlimited skipping for development and testing without burning trial skips.
- Adjusted bottom action button search boundary to `>60%` display height for universal device compatibility.
- Synchronized desktop companion GUI safeguards.

## 4. Key Invariants & Safeguards

- Never auto-skip if conversational exchanges exist (presence of message timestamps, "Me" markers, or "Seen" receipts).
- Never click "New partner" nodes located in the top half of the screen; the action button strictly resides at the bottom (>60% screen height).
- Google Play Billing purchase checks must never revoke non-store test tiers (`PRO_TEST`, `PRO_PROMO`, `PRO_MANUAL`).
- Always verify accessibility node bounds before dispatching click gestures.
- Mirror core heuristic logic between Android Kotlin modules and desktop Python ADB companion scripts.
