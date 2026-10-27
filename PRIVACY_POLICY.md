# Privacy Policy for Skipper

**Last updated:** October 27, 2026

Skipper ("we", "our", or "the app") respects your privacy and is committed to protecting your personal data.
This Privacy Policy describes how Skipper operates, how permissions are utilized, and our strict commitment to zero data collection.

---

## 1. Zero Data Collection & Local On-Device Operation

Skipper does not collect, store, transmit, or share any personal information, keystrokes, messages, contact details, device identifiers, or usage logs.
All automation heuristics, keyword evaluations, and UI node analyses are performed 100% locally on your device within your phone's memory.
No screen contents or user inputs ever leave your phone or are sent to any external server, cloud service, or third-party analytics provider.

---

## 2. Android Accessibility Service API Usage & Disclosure

Skipper uses the Android Accessibility Service API (`android.permission.BIND_ACCESSIBILITY_SERVICE`) strictly to deliver its core automated skipping functionality.

### A. Purpose of Accessibility Usage
- **Screen Status Inspection**: Skipper observes window content events within user-selected anonymous chat applications (e.g. AnonChat and Chatous) to detect when a partner has disconnected or when match criteria are met.
- **Automated Interaction**: Skipper dispatches simulated tap gestures on the designated "New partner" action button so you do not have to manually tap repeatedly through hundreds of empty or disconnected partner screens.
- **Safety Safeguards**: Skipper detects conversational exchanges (message timestamps, sent quotes, and read receipts) to automatically block skips whenever an active conversation is underway, protecting your active chats.

### B. Accessibility Guarantees
- The Accessibility Service operates exclusively when you enable the automation switch in Skipper.
- The service **NEVER** monitors, captures, stores, or transmits personal chat messages, typed text, passwords, financial details, or payment credentials.
- The service is never used to circumvent Android system security, prevent app uninstallation, or display unauthorized overlays or advertisements.

---

## 3. Google Play Billing

Skipper offers optional in-app subscriptions and passes through Google Play In-App Billing.
All payment transactions, credit card processing, and billing renewals are handled directly and securely by Google Play.
Skipper never accesses, processes, or stores your credit card numbers, bank accounts, or billing addresses.

---

## 4. Permissions Used

- `android.permission.BIND_ACCESSIBILITY_SERVICE`: Used solely to detect partner disconnect status and automate skipping taps.
- `android.permission.VIBRATE`: Provides subtle haptic feedback when a skip or trigger executes (if enabled).
- `android.permission.INTERNET`: Required solely for Google Play Billing subscription verification via the Google Play Store client.
- `com.android.vending.BILLING`: Allows users to manage and purchase optional PRO subscriptions through the Google Play Store.

---

## 5. Children's Privacy

Skipper does not knowingly collect or solicit any information from children under the age of 13.
The app is intended for general audience users seeking utility automation on supported chat platforms.

---

## 6. Changes to This Privacy Policy

We may periodically update our Privacy Policy to reflect app updates or regulatory requirements.
Any changes will be posted directly within this document with an updated revision date.

---

## 7. Contact Us

If you have any questions or feedback regarding this Privacy Policy or Skipper's accessibility implementation, please contact us via our GitHub repository or support email:
- **Repository**: [https://github.com/raxblackwood/Skipper](https://github.com/raxblackwood/Skipper)
- **Support**: `ramankumarat@gmail.com`
