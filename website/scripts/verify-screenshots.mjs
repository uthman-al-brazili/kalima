import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const websiteRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
const screensRoot = join(websiteRoot, 'public', 'screens');
const manifestPath = join(websiteRoot, 'screenshots-manifest.json');

const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
if (!/^\d{4}-\d{2}-\d{2}$/.test(manifest.capturedAt) || !/^[a-f0-9]{64}$/.test(manifest.apkSha256)) {
  throw new Error('Screenshot manifest must record a capture date and candidate APK SHA-256.');
}

for (const locale of ['en', 'pt-BR']) {
  const images = manifest.locales?.[locale];
  if (!images || Object.keys(images).length < 4) {
    throw new Error(`Screenshot manifest must contain at least four ${locale} images.`);
  }
  for (const [fileName, expectedHash] of Object.entries(images)) {
    const actualHash = createHash('sha256')
      .update(readFileSync(join(screensRoot, locale, fileName)))
      .digest('hex');
    if (actualHash !== expectedHash) {
      throw new Error(`${locale}/${fileName} does not match its recorded screenshot checksum.`);
    }
  }
}

console.log(`Verified the ${manifest.capturedAt} English and pt-BR Android screenshot sets.`);
