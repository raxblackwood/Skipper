# Skipper 🚀

**Skipper** is an intelligent automation app for Android designed to auto-skip partners in chat apps like AnonChat whenever a trigger condition is met — engineered with strict safeguards to prevent misclicks, loopholes, and false positives.

---

## 📱 How It Works

### The Scenario:
In apps like **anon.chat**:
1. **Screen 1 (Home)**: "START SEARCHING" button.
2. **Screen 2 (Active Chat)**: You and your partner are chatting. Top title says "New partner", with message bubbles and a message input bar at the bottom.
3. **Screen 3 (Partner Skips After Conversation)**: Chat ends after talking. "Chat was ended by your partner" appears with 3 bottom buttons: `[Report]`, `[End chat]`, `[New partner]`.
4. **Screen 4 (Instant Skip on Profile)**: When someone disconnects immediately upon seeing your profile info card (`About partner`, `Interests`) before any chatting occurs.

### 1. AnonChat Automator & On-Device AI Male-Bio Skip 🧠
- **On-Device AI Gender Intelligence**: Analyzes partner bio text in real time using multi-pattern NLP scoring (`M22`, `18M`, `M here`, `male`, `guy`, `looking for f/female`, `M Ind/city`).
- **Active Skip Trigger**: Automatically skips male partners as soon as their profile arrives on Screen 4, even if they haven't disconnected on their own.
- **Female Protection Guarantee**: If female indicators (`F20`, `female`, `girl`) or undetermined gender are detected, Skipper leaves the chat active for you.
- **Instant Disconnect Skip**: Auto-clicks "New partner" whenever someone skips on you immediately.

### 2. Chatous Male-Skipper ⚡ (New!)
- Automatically skips **Male profiles** upon matching (Screen 4).
- **Exact Detection**: Verifies presence of `"Let's Chat!"` and `"SKIP"` buttons.
- **Gender Detection**: Checks for `"Male"` / `♂` (using word boundary `\bMale\b` so `"Female"` profiles are never skipped!).
- **Safe Click**: Accurately targets the bottom `"SKIP"` button and transitions right back to search.
- When a female profile is found, Skipper does not click, allowing you to chat normally!

---

## 🛡️ Loophole Prevention (How Skipper Avoids Misclicks)

The user asked:
> *"If I'm actually talking to somebody and our trigger is 'whenever we see 'new partner', we should click on it': whenever I'm talking to somebody and 'new partner' just comes up on the screen as text (like I wrote it or the person I'm talking to wrote it), the app should not click on the dedicated pixel place."*

Skipper enforces a **4-tier safeguard pipeline** before any click action is triggered:

| Safeguard | How It Works |
| :--- | :--- |
| **1. Active Input Bar Guard** | Checks for an active `EditText` ("Write a message..."). If present, the chat is active. All triggers are immediately aborted. |
| **2. Disconnect Text Requirement** | Requires the exact status node `"Chat was ended by your partner"` (or `"Chat was ended"`) to be present on screen. |
| **3. Instant Skip vs Chat Distinction** | Differentiates Screen 4 from Screen 3. Screen 4 contains `"About partner"` / `"Interests"` / `"Chat starts"` and **no conversation bubbles/timestamps**. If conversation messages exist, Skipper does not trigger (configurable). |
| **4. Coordinate & Role Validation for Button** | Does **NOT** click any text that says "New partner". It verifies:<br>• Bounds: Must be in the lower 45% of the screen ($Y > 55\%$). Header toolbar titles ($Y < 20\%$) and message bubbles are ignored.<br>• Context: Must be accompanied by sibling action buttons (`End chat` or `Report`).<br>• Clickable target validation. |
| **5. Cooldown Debounce** | A 2-second debounce timer prevents duplicate clicks while the app transitions to searching. |

---

## 🏗️ Project Architecture & Modular Multi-App Design

Inside Skipper, there is a modular app architecture (`engine/BaseAppModule.kt`):
```
com.skipper.app
├── engine/
│   ├── BaseAppModule.kt        <-- Interface for any app section
│   ├── AppAutomationEngine.kt  <-- Routes events to matching app module
│   └── NodeMatcher.kt          <-- Safe accessibility node queries & boundary checks
├── modules/
│   ├── anonchat/
│   │   └── AnonChatModule.kt   <-- AnonChat implementation & state guards
│   └── template/
│       └── GenericAppModule.kt <-- Clean template to add Tinder, Omegle, Bumble, etc.
├── service/
│   └── SkipperAccessibilityService.kt <-- Android Accessibility Service
└── ui/
    ├── MainActivity.kt         <-- Dashboard with module cards & live trigger logs
    └── AnonChatDetailActivity.kt<-- Settings: instant-only vs all skips, delays
```

### Adding New Apps:
To add a new app section (e.g. Omegle, Bumble):
1. Create a class implementing [`BaseAppModule`](file:///e:/CODES/Miscs/Skipper/app/src/main/java/com/skipper/app/engine/BaseAppModule.kt).
2. Define target package names and trigger conditions.
3. Register it in [`AppAutomationEngine`](file:///e:/CODES/Miscs/Skipper/app/src/main/java/com/skipper/app/engine/AppAutomationEngine.kt).
4. The dashboard automatically displays a new card with independent enable/disable and configuration controls.

---

## ⚡ Ready-to-Use Built Files

Both working binaries are built and ready in this folder:

1. **[`Skipper.apk`](file:///e:/CODES/Miscs/Skipper/Skipper.apk)** (5.94 MB):
   - Native Android application running on your device via Accessibility Service.
   - Simply copy and install onto your Android phone.

2. **[`Skipper.exe`](file:///e:/CODES/Miscs/Skipper/Skipper.exe)** (13.1 MB):
   - Standalone Windows Desktop GUI app with portable [`adb.exe`](file:///e:/CODES/Miscs/Skipper/adb.exe) included.
   - Simply double-click `Skipper.exe` to run. It auto-detects your connected phone, monitors screen state, applies all safeguards, and automatically clicks "New partner" on Screen 4.

---

## 🚀 How to Run Skipper

### Option 1: Double-Click Desktop App (`Skipper.exe`)
1. Connect your phone to your PC with USB debugging enabled.
2. Double-click [`Skipper.exe`](file:///e:/CODES/Miscs/Skipper/Skipper.exe).
3. The app detects your phone and displays its model.
4. Click **"START SKIPPER"**.
5. Live trigger feed shows detection states, safeguards, and automatic clicks in real time.

### Option 2: Install Native Android App (`Skipper.apk`)
1. Transfer [`Skipper.apk`](file:///e:/CODES/Miscs/Skipper/Skipper.apk) to your phone and install it.
2. Open Skipper and tap **"Enable Accessibility Service"**.
3. Open AnonChat — Skipper runs autonomously in the background on your phone without needing a computer.
