# Kalima website

A static React and Vite website for Kalima. It includes English and Brazilian Portuguese content, real app screenshots, privacy and support pages, and no analytics or remote fonts.

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

The production website is always published to the existing Cloudflare Pages
project at `https://kalima-h1f.pages.dev/`:

```powershell
pnpm deploy:cloudflare
```

Do not publish this website through ChatGPT Sites or another hosting provider.

## App screenshots

The website keeps separate, real Android captures in `public/screens/en/` and
`public/screens/pt-BR/`. Portuguese visitors automatically receive the pt-BR
set. Do not reuse screenshots from an older app release.

For every Android UI release:

1. Install the candidate APK and capture the four named screens in both app
   languages.
2. Replace the website images and any matching Uptodown listing images.
3. Record the candidate APK SHA-256, capture date, device, and image SHA-256
   values in `screenshots-manifest.json`. This internal record is not published
   or displayed on the website.
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

After connecting a final domain, update the `og:image` value in `index.html` to the absolute deployed URL for the best social sharing compatibility.
