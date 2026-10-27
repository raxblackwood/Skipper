# Skipper - Google Play Store Release Kit & Metadata

This document contains everything required to complete your Google Play Console store listing, policy declarations, in-app subscription setup, and bundle upload.

---

## 1. Store Listing Metadata

### App Title (30 characters max)
```text
Skipper: Smart Auto-Clicker
```
*(Character count: 27 / 30)*

### Short Description (80 characters max)
```text
Smart auto-skip and profile filter for anonymous chat apps. 100% on-device.
```
*(Character count: 75 / 80)*

### Full Description (4000 characters max)
```text
Skipper is an intelligent, privacy-first automation utility designed to streamline your anonymous chat experience.
Say goodbye to staring at disconnected partner screens and manually tapping "New partner" hundreds of times a day.

⚡ KEY FEATURES

• Instant Disconnect Skipping:
Automatically detects when a chat partner immediately disconnects on profile (Screen 4) and instantly taps "New partner" with zero lag.

• On-Device AI Bio & Keyword Filter:
Intelligently scans introduction cards and bios for keywords, age indicators, and gender markers.
Automatically skips non-matching profiles in milliseconds without you having to type a word.

• Protected Keywords & Never-Skip Rules:
Add preferred keywords (e.g. "female", "girl", "gamer") that immediately veto skips, ensuring you never miss a match you want to talk to.

• Conversation Safe Safeguards:
Smart heuristics inspect message timestamps, quote badges, and read receipts.
As soon as even a single message is exchanged, auto-skip is automatically disabled so your ongoing conversation is never interrupted.

• Multi-App Support:
Tailored automation engines built specifically for AnonChat and Chatous, with customizable cooldowns and touch delay timers.

• Complete Privacy & Transparency:
All pattern matching and heuristics execute 100% locally on your device.
Skipper requires zero accounts, zero sign-ins, and collects zero personal data.

---

🛡️ ACCESSIBILITY SERVICE API DISCLOSURE

Skipper uses Android's AccessibilityService API (`android.permission.BIND_ACCESSIBILITY_SERVICE`) strictly to provide its core automation functions:

1. Screen Status Detection:
Skipper observes window state events in user-selected chat apps to determine when a partner has disconnected or when profile criteria trigger a skip.

2. Automated Tapping:
Skipper emulates touch gestures on the designated "New partner" action button so you don't have to perform repetitive physical taps.

3. Privacy Guarantees:
• The service runs only when you toggle automation ON in the Skipper app.
• Skipper NEVER collects, reads, records, or transmits your personal chat messages, typed text, passwords, or personal data.
• All processing occurs 100% locally on your phone.

---

💎 PREMIUM PASSES & SUBSCRIPTIONS

Skipper includes a free trial of 10 skips so you can test all features.
Unlock unlimited automatic skips with flexible subscription passes:
• Daily VIP Pass
• Weekly Pro Pass
• Monthly Unlimited Pass

Take control of your anonymous chat experience today with Skipper!
```

---

## 2. Store Assets Summary

All required visual assets have been generated and prepared in the `store_assets/` folder:

| Asset Type | Dimension | Format | File Path |
|---|---|---|---|
| **App Icon** | 512 x 512 px | 32-bit PNG | `store_assets/icon_512.png` |
| **Feature Graphic** | 1024 x 500 px | PNG | `store_assets/feature_graphic_1024x500.png` |
| **Screenshot 1** | 1080 x 1920 px | PNG | `store_assets/screenshots/01_instant_skip.png` |
| **Screenshot 2** | 1080 x 1920 px | PNG | `store_assets/screenshots/02_ai_bio_filter.png` |
| **Screenshot 3** | 1080 x 1920 px | PNG | `store_assets/screenshots/03_chat_protection.png` |
| **Screenshot 4** | 1080 x 1920 px | PNG | `store_assets/screenshots/04_custom_rules.png` |

---

## 3. Subscriptions Configuration (Google Play Console)

In Google Play Console, navigate to **Monetize** -> **Subscriptions** -> **Create subscription**.
Create the three product tiers configured in the code:

### Tier 1: Daily VIP
- **Product ID**: `skipper_pro_daily`
- **Name**: Daily VIP Pass
- **Description**: 24-hour unlimited automated skipping pass.
- **Base Plan Type**: Auto-renewing (1 day) or Prepaid.
- **Suggested Price**: $0.49 / ₹29

### Tier 2: Weekly Pro
- **Product ID**: `skipper_pro_weekly`
- **Name**: Weekly Pro Pass
- **Description**: 7-day unlimited automated skipping pass.
- **Base Plan Type**: Auto-renewing (1 week).
- **Suggested Price**: $1.99 / ₹99

### Tier 3: Monthly Unlimited (Recommended)
- **Product ID**: `skipper_pro_monthly`
- **Name**: Monthly Unlimited Pass
- **Description**: 30-day all-access automated skipping and AI filter pass.
- **Base Plan Type**: Auto-renewing (1 month).
- **Suggested Price**: $4.99 / ₹249

---

## 4. Google Play Console Policy Answers

### A. Privacy Policy URL
Enter the public GitHub privacy policy link:
`https://github.com/raxblackwood/Skipper/blob/main/PRIVACY_POLICY.md`

### B. Accessibility Service Declaration (Crucial for Approval)
When Google Play Console asks for the Accessibility Service declaration, use the following exact responses:

1. **Which accessibility capabilities does your app use?**
   - Select: `Can retrieve window content`
   - Select: `Can perform gestures`

2. **Why does your app need the Accessibility API?**
   - Answer:
     ```text
     Skipper is an automation utility designed to assist users with repetitive touch actions in anonymous chat applications. The AccessibilityService API is used strictly to detect when a chat partner has disconnected (by inspecting on-screen status text) and to automate the tap on the "New partner" action button. This eliminates the need for users to perform hundreds of repetitive physical taps. No personal user data, keystrokes, or messages are ever collected, stored, or transmitted.
     ```

3. **Does your app collect or share user data using the Accessibility API?**
   - Select: `No, this app does not collect or share user data using the Accessibility API.`

4. **Prominent In-App Disclosure Confirmation:**
   - Select: `Yes, the app displays a prominent in-app disclosure explaining the use of the Accessibility API before the user enables the permission.`

5. **Demo Video URL (Optional but highly recommended):**
   - Upload a short 30-second unlisted YouTube video showing the app opening, displaying the prominent disclosure dialog, enabling the accessibility service in Android Settings, and executing a skip.

### C. Data Safety Questionnaire
- **Does your app collect or share any of the required user data types?**
  - Select: `No` (Skipper collects zero personal data).
- **Is all user data encrypted in transit?**
  - Select: `Yes` (All billing communication with Google Play is encrypted via HTTPS).
- **Do you provide a way for users to request data deletion?**
  - Select: `Yes` (Users can clear all local settings and logs at any time via the in-app "Clear Logs" and app data reset).

### D. Target Audience & Content Rating
- **Target Age**: 18 and older (since the companion chat apps are for mature audiences).
- **App Category**: Tools / Productivity.
- **Ads**: Select `No, my app does not contain ads`.

---

## 5. Bundle Upload & Release Steps

1. **Locate the Release Bundle**:
   - The signed, release-ready app bundle is located in your workspace root at:
     `E:\CODES\Miscs\Skipper\Skipper.aab`
   - Certificate: RSA 2048-bit (valid until year 2054).
   - Version Code: `1`
   - Version Name: `1.0.0`
   - Target SDK: `35` (Android 15 ready).

2. **Upload to Google Play Console**:
   - Open [Google Play Console](https://play.google.com/console).
   - Select or Create App -> App name: `Skipper: Smart Auto-Clicker` -> Default language: English -> App -> Free -> Declarations.
   - Go to **Release** -> **Testing** -> **Closed testing** (or Internal testing).
   - Click **Create new release** -> Upload `Skipper.aab`.
   - Release name: `1.0.0 (Initial Release)`.
   - Release notes:
     ```text
     Initial release of Skipper:
     • Instant auto-skip for disconnected chat partners.
     • On-device AI bio and keyword filtering.
     • Conversation-safe protection for active chats.
     • Google Play In-App Billing integration.
     ```
   - Click **Save** -> **Review release** -> **Start rollout**.
