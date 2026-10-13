# Mini Games

Five offline games in one small Android app (Kotlin, framework Views and
Canvas, no libraries, no ads, no internet, no permissions):

| Game | Notes |
|---|---|
| 🔢 **Sudoku** | Easy / Medium / Hard. Every puzzle is generated on the phone and has exactly one solution. Notes (pencil marks), hints, clash highlighting, timer and best times. Your puzzle is saved if you leave. |
| 🃏 **Whot!** | Naija Whot vs 1–3 computer players. 54-card deck; specials: 1 hold on, 2 pick two, 5 pick three, 8 suspension, 14 general market, 20 Whot (ask for a shape). |
| 🟤 **Ayo** | Oware (abapa rules) vs computer or 2 players on one phone. Captures of 2 or 3, grand-slam rule, must-feed rule, first to 25. |
| ❌ **Tic-Tac-Toe** | vs computer (easy / medium / unbeatable hard) or 2 players. Sides alternate each round. |
| 🧠 **Memory Match** | 16 or 24 cards; fewest-moves record. |

## Install on your phone

Each push is built by GitHub Actions (unit tests + lint + signed debug APK).
Open **Releases → Mini Games latest debug build → MiniGames-debug.apk** on
your phone and install it. New builds install over the old one and keep your
scores (fixed debug key in `app/debug.keystore`).

## Code

```
logic/  pure Kotlin game rules + CPU players (unit tested)
ui/     small UI kit, base screen, canvas board base, stats
games/  one screen (and canvas view) per game
```
