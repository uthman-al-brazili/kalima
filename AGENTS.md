# Kalima workspace instructions

## Scope

Application work targets Android only unless the user explicitly requests
Windows work. Preserve all Windows source and release artifacts. Preserve
unrelated user changes in the working tree.

## Low-token default

Use the development fast path unless the user explicitly requests a release,
exhaustive verification, or work whose risk clearly requires more scrutiny.

- Inspect only affected files and nearby code. Prefer targeted `rg` searches and
  small excerpts over repository-wide reads or complete logs.
- Use the lowest adequate reasoning effort when it can be selected: `low` for
  mechanical lookups, `medium` for routine changes, `high` for non-obvious or
  cross-cutting work, and `xhigh` only for especially difficult, high-impact,
  security-sensitive, or release-critical work. Reserve `max` for exceptional
  quality-first work.
- Do not create a detailed plan for a straightforward change. Keep commentary
  to a short starting update, material findings or blockers, and a concise final
  report.
- During implementation, run the smallest relevant check after the source is
  stable. Do not repeat unchanged checks or the complete verification gate.
- Capture verbose command output in a temporary log and inspect only summaries
  and relevant errors. Diagnose failures before retrying.
- Keep context lean. After a completed milestone, retain the objective,
  decisions, changed files, verification results, and remaining work; do not
  reload or restate completed work.

## Delegation and model choice

Use the primary agent alone for routine discovery, focused edits, ordinary
testing, log inspection, and documentation. Use subagents only when the user
explicitly requests them or when the task contains at least two substantial,
independent workstreams whose parallel execution will materially reduce elapsed
time. Coordination overhead must not exceed the expected benefit.

For every delegated subtask, explicitly select its model and reasoning effort.
Never use Terra. Use Luna for concrete, bounded, low-risk work with clear
acceptance criteria. Use Sol for ambiguous, cross-cutting, architectural,
difficult, security-sensitive, or release-sensitive work. Select the lowest
reasoning effort expected to satisfy the subtask and escalate only after the
scope, risk, ambiguity, or failed verification justifies it.

Do not browse or fetch documentation merely to refresh this saved model policy.

## Development verification

For an ordinary Android change:

1. Inspect the affected code and existing Gradle task names before running
   Gradle.
2. Run the smallest tests or compilation check that validates the changed
   surface.
3. Run Android lint or APK assembly during development only when needed to
   validate the affected build, resource, manifest, packaging, or integration
   surface, or when the user explicitly requests it.

After every Kalima app change, install the updated debug build on the connected
Android emulator, open the app, and navigate to the exact screen or state that
was changed so the user can test it immediately.

Do not run `verifyLockScreenRegression` for ordinary development changes unless
the user explicitly requests it. Run it as part of the explicit release workflow
when preparing to publish a new app version.

Do not bump versions, edit release notes, create release artifacts, commit, or
tag as part of an ordinary development change.

## Explicit release workflow

Treat work as a release only when the user explicitly asks to release, package,
version, or publish it. Once the source is stable:

Creating a commit or tag, or pushing commits or tags, always requires the
user's explicit permission in the current request. A request to prepare,
package, release, or publish the app does not by itself grant Git permission.
Without that permission, prepare and verify the complete release state locally,
then stop before any commit, tag, or push.

1. Confirm the requested version does not already exist in `releases/`; stop
   rather than overwrite an existing artifact.
2. Increment Android `versionCode` and `versionName` in
   `app/build.gradle.kts`.
3. Add a dated section at the top of `CHANGELOG.md` summarizing user-visible
   changes.
4. Update the versioned GitHub release and APK links plus the English and
   Portuguese download labels in `website/src/App.tsx`, and include that website
   source state in the release commit. Do not change promotional artwork,
   screenshots, or the artwork manifest unless the user explicitly requests it.
5. In one Gradle invocation where practical, run the relevant unit tests,
   Android lint, `verifyLockScreenRegression`, and assembly of the requested
   distributable release artifact. Do not build or copy a separate debug APK
   as a release backup.
6. Only with the user's explicit Git permission, commit the complete intended
   source state and create annotated tag `v<version>`.
7. Create `releases/kalima-<version>-source.zip` from that tag.
8. Update `releases/SHA256SUMS.txt` with the source archive checksum and the
   checksum of any distributable artifact retained for the release.
9. Publish the GitHub release and its retained assets. Prepare and verify the
   website build locally, but never deploy `website/` or create a public preview
   unless the user explicitly authorizes that website publication in the
   current request. A general request to publish an app release does not grant
   website-deployment permission.

## Lock-screen regression contract

Lock-screen cards must launch over the still-locked keyguard when the display
turns on, including on a Samsung Galaxy M23 5G running Android 14. They must not
wait for `ACTION_USER_PRESENT`. Never remove or bypass the manifest/window
`showWhenLocked` declarations, the `ACTION_SCREEN_ON` launch path, or the
explicit locked-device safety allowance. For a release, when a compatible
Android 14 device is connected, install the candidate APK and exercise a real
screen-off/screen-on cycle.
