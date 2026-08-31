# Kalima website

A static React and Vite website for Kalima. It includes English and Brazilian Portuguese content, edited promotional artwork based on the Android experience, privacy and support pages, and no analytics or remote fonts.

## Local development

```powershell
pnpm install
pnpm dev
```

## Production build

```powershell
pnpm build
```

The deployable output is generated in `dist/`.

## Publishing

Publishing is manual and requires the user's explicit approval. For review,
run the website only on localhost; do not create a public preview deployment.
After approval, the production target is the existing Cloudflare Pages project
at `https://kalima-h1f.pages.dev/`:

```powershell
pnpm deploy:cloudflare
```

Do not publish this website through ChatGPT Sites or another hosting provider.

Every published Android release must update the versioned release URL, APK URL,
and English and Portuguese download labels in `src/App.tsx`, and include those
website changes in the release commit. Do not change promotional artwork,
screenshots, or `artwork-manifest.json` unless the user explicitly requests it.
After the GitHub release assets are publicly available, run the production
build locally. Deploy it to Cloudflare and verify the live download button only
when the user explicitly authorizes that website publication in the current
request. Never infer deployment permission from a general app-release request.

## Promotional artwork

The website keeps separate English and pt-BR promotional artwork in
`public/screens/en/` and `public/screens/pt-BR/`. Portuguese visitors
automatically receive the pt-BR set. These website-specific compositions place
real app captures inside a simulated phone and add only the website's geometric
background details. They must remain faithful to the shipped Android experience.

The website serves promotional screenshots as lossless WebP. Keep future
website raster artwork in WebP unless a platform integration specifically
requires another format. Regenerate the compositions from the source captures
with FFmpeg available on `PATH`:

```powershell
.\scripts\render-website-screens.ps1
```

For every Android UI release:

1. Review the three promotional images in both app languages against the
   candidate APK and current product claims.
2. Replace the website images and the matching Uptodown listing images.
3. Record the release version, candidate APK SHA-256, artwork date, and image
   SHA-256 values in `artwork-manifest.json`. This internal record is not
   published or displayed on the website.
4. Run `pnpm check`. It fails if an image differs from its recorded checksum or
   if either language set is incomplete.

## Cloudflare Pages settings

- Framework preset: React (Vite)
- Root directory: `website`
- Build command: `pnpm build`
- Build output directory: `dist`
- Production branch: `main`
- Node.js version: `22.16.0` (also pinned in `.node-version`)

No Worker, database, or paid Cloudflare feature is required. Cloudflare Pages serves the SPA routes through `index.html` because the build intentionally has no top-level `404.html`.

When a current social-preview image is available, add it to `index.html` with an absolute deployed URL for the best sharing compatibility. Never reuse artwork for a removed feature.
