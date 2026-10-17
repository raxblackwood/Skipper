# 🚀 Google Play Store Publishing Guide for Skipper

This guide contains everything you need to publish **Skipper** on the Google Play Console and pass review smoothly on your first attempt.

---

## 📦 1. App Bundle (Ready to Upload)
- **File to upload:** **[`Skipper.aab`](file:///e:/CODES/Miscs/Skipper/Skipper.aab)** (Android App Bundle, 4.33 MB)
- **Signed with:** Dedicated release keystore `app/skipper.jks` (Key alias: `skipper`)

---

## 📝 2. Store Listing Details

### App Title (Max 30 characters)
```
Skipper - Auto Match Skipper
```

### Short Description (Max 80 characters)
```
Smart automated skip assistant for AnonChat & Chatous with on-device AI.
```

### Full Description (Formatted with Play Store HTML tags)
```html
<b>Skipper</b> is an intelligent, privacy-first automation assistant designed to eliminate repetitive manual clicking in supported chat applications.

Whether you're tired of manually tapping "New partner" after an instant disconnect, or want to automatically filter matches, Skipper handles the repetitive tasks smoothly and reliably.

<b>⚡ KEY FEATURES:</b>

• <b>AnonChat Automator:</b> Automatically clicks "New partner" whenever a connection is ended immediately, saving you hundreds of repetitive taps.
• <b>On-Device AI Bio Intelligence:</b> Evaluates partner bio tags in real-time (M22, 18M, looking for F) and automatically skips male profiles even if they don't disconnect.
• <b>Chatous Male-Skipper:</b> Instantly skips male profiles upon matching on Screen 4, while preserving female connections so you can chat naturally.
• <b>Multi-Tier Loophole Safeguards:</b> Intelligent screen inspection prevents accidental misclicks while you are actively typing or chatting.
• <b>100% Offline & Private:</b> Operates completely on your device without transmitting any screen data, keystrokes, or messages to external servers.

---

<b>🛡️ ACCESSIBILITY SERVICE API DISCLOSURE:</b>
Skipper uses Android's AccessibilityService API solely to perform its core automation features:
1. Detecting screen state transitions and partner disconnection indicators within designated apps (AnonChat, Chatous).
2. Emulating touch events on the "New partner" and "SKIP" action buttons on your behalf.

Skipper does NOT collect, store, record, or transmit keystrokes, private chat messages, passwords, or personal identity information. All processing occurs 100% locally on your smartphone.
```

---

## 📋 3. Accessibility Service Declaration Form (CRITICAL)

When filling out the **App Content &rarr; Accessibility Service declaration** in Google Play Console, use these exact answers:

1. **Why does your app use the Accessibility Service API?**
   - Select: **"Tool / Utility / Automation / Repetitive Input Assistance"**

2. **Describe the user-facing feature(s) that use the AccessibilityService API:**
   > *"Skipper uses the Accessibility Service API to detect match states and automate clicking the 'New partner' and 'SKIP' buttons in AnonChat and Chatous. When a partner disconnects immediately or when an unwanted profile match occurs, Skipper simulates the tap on the corresponding action button on the user's behalf to eliminate repetitive manual tapping. The service is active only in the user's selected applications."*

3. **Does your app collect or share user data using the Accessibility API?**
   - Select: **"No, my app does not collect or share any personal or sensitive user data using the Accessibility API."**

4. **Demo Video URL:**
   - Record a 30-second screen capture of Skipper skipping a profile on AnonChat or Chatous, upload it as an unlisted YouTube video, and paste the link here.

---

## 🌐 4. Privacy Policy URL (Free on GitHub Pages)

Google Play requires a live Privacy Policy URL. You can host [`privacy_policy.html`](file:///e:/CODES/Miscs/Skipper/privacy_policy.html) for free via your GitHub repo:

1. Open your repository: [https://github.com/raxblackwood/Skipper](https://github.com/raxblackwood/Skipper)
2. Go to **Settings &rarr; Pages**.
3. Under **Build and deployment &rarr; Branch**, select **`main`** and **`/ (root)`**, then click **Save**.
4. Your official Privacy Policy link will be:
   ```
   https://raxblackwood.github.io/Skipper/privacy_policy.html
   ```
5. Paste this URL into the **Privacy Policy** section of Google Play Console.
