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
- **Companion Engine**: Python desktop ADB script (`scripts/skipper_adb.py`, `scripts/skipper_gui.py`).
- **Target Integrations**: AnonChat (`com.anonchat.android`), Chatous (`com.chatous`).

## 3. Milestones & Current Focus

- Added intelligent text exchange detection in `AnonChatModule.kt` to ensure conversations with any sent or received messages are never auto-skipped.
- Corrected button resolution in AnonChat to enforce strictly bottom-screen bounds (>65% height), eliminating inadvertent clicks on top toolbar header and bio.
- Removed legacy confirmation dialog delayed runner that was previously targeting top header elements.
- Implemented non-skip keyword vetoing system alongside customizable skip lists.
- Built release APK and Google Play AAB bundle.
- Deployed directly to connected phone and verified accessibility service activation.

## 4. Key Invariants & Safeguards

- Never auto-skip if conversational exchanges exist (presence of message timestamps, "Me" markers, or "Seen" receipts).
- Never click "New partner" nodes located in the top half of the screen; the action button strictly resides at the bottom (>65% screen height).
- Always verify accessibility node bounds before dispatching click gestures.
- Mirror core heuristic logic between Android Kotlin modules and desktop Python ADB companion scripts.
