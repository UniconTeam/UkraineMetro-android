# How to contribute

Thanks that you want to help. This is small pet project, no company big team behind it,
so every help is welcome: new translations, fixed station names,
bug fixes. Two ways to send your changes.

## Way 1: GitHub pull request (easier)

1. Fork repo and make branch from `master` with clear name, like
   `fix-kyiv-labels` or `add-pl-translation`.
2. Do your change. One PR = one thing, please not mix translation fix with
   big refactoring in same PR, it is hard to review.
3. Check that app builds (`./gradlew :app:assembleRelease`) before send.
4. Open PR with short description what you changed and why. Screenshots are
   very welcome if you touched UI.

I review when have time, usually in few days. Can ask to fix something,
this is normal, don't worry.

## Way 2: patch by email

If you don't like GitHub or have no account, send patch to
**yavarenikya@gmail.com**.

Easiest way to make patch:

```bash
git format-patch master --stdout > my-fix.patch
```

Then attach `my-fix.patch` to email and write few words what it does.
Plain text in email body is also ok for small fixes like one station name.

Please send patches made from fresh `master`, otherwise they may not apply.

## What is useful most

- Translations. App supports English, Ukrainian, Russian and Polish
  (`app/src/main/res/values*/strings.xml`). If you see bad translation,
  just fix it.
- Station names. Goverment rename stations sometimes, maps get outdated.
  If you know real official name, send fix. Good if you add link to source.
- Bugs. Before fix, good to open issue with what device and what app version
  (you can see version in Settings at bottom).

That's all. Thank you!
