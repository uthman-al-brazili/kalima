# Kalima workspace instructions

These instructions replace earlier workspace instructions.

## Scope

- Target Android unless the user explicitly requests Windows work.
- Preserve Windows artifacts and unrelated working-tree changes.

## Default workflow

- Inspect only affected files and nearby code using targeted `rg` searches and
  small excerpts.
- Use `low` reasoning for mechanical work, `medium` for routine changes, and
  `high` only for difficult or cross-cutting work. Reserve higher levels for
  exceptional risk or complexity.
- Keep plans and commentary brief for straightforward tasks.
- Run the smallest relevant test or compilation check after the source is stable.
- Capture verbose output in temporary logs and inspect summaries or errors only.
- Do not repeat unchanged checks or reload completed work.
- Do not browse merely to refresh saved model-selection policy.

## Delegation

Use the primary agent for routine work. Delegate only when requested or when at
least two substantial independent workstreams justify the overhead.

For delegated tasks, explicitly select model and effort:

- Luna: bounded, low-risk work.
- Sol: ambiguous, architectural, difficult, security-sensitive, or
  release-sensitive work.
- Never use Terra.

## Android verification

For ordinary app changes:

1. Inspect affected code and relevant Gradle tasks.
2. Run the smallest applicable test or compilation check.
3. Run lint or assembly only when the changed surface requires it.

After an app change, install the debug build on the connected emulator and open
the exact changed screen or state.

Do not run `verifyLockScreenRegression`, bump versions, edit release notes,
create release artifacts, commit, or tag during ordinary development.

## Releases

A release request does not authorize Git commits, tags, pushes, or website
deployment. Each requires explicit permission in the current request.

For an explicit release, read and follow `RELEASE_PROCESS.md`. Never overwrite
an existing version in `releases/`.

Before publishing on a compatible Android 14 device:

- Install the candidate and leave lock-screen learning ready.
- Ask the user to perform a real locked screen-off/screen-on test.
- Wait for explicit confirmation that it passed.

## Lock-screen invariants

Lock-screen cards must launch over the still-locked keyguard on screen-on,
including Samsung Galaxy M23 5G on Android 14.

Never remove or bypass:

- Manifest/window `showWhenLocked` declarations.
- The `ACTION_SCREEN_ON` launch path.
- The explicit locked-device safety allowance.

Do not depend on `ACTION_USER_PRESENT`.
