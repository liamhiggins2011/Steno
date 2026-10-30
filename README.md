# ✍️ Steno

### Text expansion for Android that **can't** spy on you — because it has no internet permission at all.

Type `;addr` → your full address. Type `brb` → "be right back, give me five."
Type `;sig` → your whole email signature. In **any** app. Instantly. For free.

The catch? There isn't one. Steno has **zero** network code, **zero** ads,
**zero** trackers, and **zero** accounts. Your snippets physically cannot leave
your phone, and you can read the source to prove it.

![platform: Android](https://img.shields.io/badge/platform-Android-3ddc84)
![min SDK 26](https://img.shields.io/badge/minSdk-26-blue)
![internet permission: none](https://img.shields.io/badge/internet%20permission-NONE-brightgreen)
![ads/trackers: 0](https://img.shields.io/badge/ads%20%26%20trackers-0-brightgreen)
![100% Kotlin](https://img.shields.io/badge/100%25-Kotlin-7f52ff)

---

## Why people are switching

Text expanders are one of those tools you can't live without once you have one —
but the good ones want **$40 a year** and sync your most personal text (home
address, phone number, canned replies) through *their* cloud.

Steno flips that:

| | The subscription apps | **Steno** |
|---|---|---|
| Price | ~$40 / year | **Free, forever** |
| Where your snippets live | their servers | **only your phone** |
| Internet access | required | **impossible** (no permission) |
| Ads / trackers | often | **none** |
| Source code | closed | **open — audit it yourself** |
| Works in every app | sometimes | **yes, system-wide** |

If your keyboard can't see it, it can't leak it. Steno expands text through
Android's accessibility system, so it works in **every** editable field — chat
apps, email, browsers, forms — without replacing your keyboard.

---

## ⚡ 60-second setup

1. Install the APK.
2. Open Steno, add a shortcut or two.
3. Flip on the accessibility toggle when prompted. Done.

Now type your trigger anywhere and watch it expand.

## ✨ What it does

- **Longest match wins** — `;addr2` beats `;addr`, so you can nest shortcuts.
- **Smart boundaries** — symbol triggers (`;`, `/`, `:`) fire anywhere; plain
  words like `brb` only fire at a word boundary, so they never go off mid-word.
- **Dynamic placeholders** baked into any expansion:

  | Type this | Get this |
  |-----------|----------|
  | `{date}`  | today's date |
  | `{time}`  | the current time |
  | `{nl}`    | a line break |
  | `{cursor}`| where your cursor lands afterward |

  > e.g. a shortcut `;meet` → `Let's meet on {date}. Agenda:{nl}{cursor}`
  > drops you on a fresh line with the date already filled in.

---

## 🔒 Privacy you can verify, not just trust

Open `app/src/main/AndroidManifest.xml`. There is **no `INTERNET`
permission**. That's not a policy promise — it's a hard wall enforced by
Android itself. The app is *structurally incapable* of sending your data
anywhere. Everything lives in a local Room database on-device.

The one permission Steno does use — accessibility — is the mechanism that lets
it read and rewrite the field you're typing in. It still never touches the
network.

## 🧱 How it's built (for the curious)

Three clean layers, so the tricky part is testable in isolation:

| Layer | Role | Key files |
|-------|------|-----------|
| **Engine** | A pure, Android-free matcher — rewrites the field based on the caret. Unit-tested on the plain JVM (**7/7 green**). | `engine/Expander.kt` |
| **Service** | `AccessibilityService` that watches fields and injects expansions (InputMethod API on API 34+). | `engine/StenoAccessibilityService.kt` |
| **UI + data** | Jetpack Compose shortcut manager over a Room database. | `ui/*`, `data/*` |

**Stack:** 100% Kotlin · Jetpack Compose · Room (KSP) · `minSdk 26` /
`targetSdk 35` · Java/Kotlin 17.

## 🛠️ Build it yourself

```bash
./gradlew assembleDebug     # debug APK
./gradlew test              # run the engine unit tests
./gradlew assembleRelease   # signed release (needs your own keystore/)
```

Release signing reads from a **gitignored** `keystore/keystore.properties` —
no signing secrets in the repo. Create `local.properties` locally with
`sdk.dir=/path/to/Android/Sdk`.

---

**Steno** · `com.hirephaze.steno` · v0.1.0 — early, working, and honest.
Found a bug or want a feature? Open an issue. ⭐ it if offline-first tools are
your thing.

## License

[MIT](LICENSE) © 2026 Liam Higgins — free to use, fork, and build on.
