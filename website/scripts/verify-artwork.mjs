import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const websiteRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
const screensRoot = join(websiteRoot, 'public', 'screens');
const manifestPath = join(websiteRoot, 'artwork-manifest.json');

const manifest = JSON.parse(readFileSync(manifestPath, 'utf8'));
if (!/^\d{4}-\d{2}-\d{2}$/.test(manifest.createdAt)
  || !/^\d+\.\d+\.\d+$/.test(manifest.releaseVersion)
  || !/^[a-f0-9]{64}$/.test(manifest.apkSha256)
  || manifest.kind !== 'edited promotional artwork') {
  throw new Error('Artwork manifest must record its date, release version, APK SHA-256, and asset kind.');
}

for (const locale of ['en', 'pt-BR']) {
  const images = manifest.locales?.[locale];
  if (!images || Object.keys(images).length !== 3) {
    throw new Error(`Artwork manifest must contain exactly three ${locale} images.`);
  }
  for (const [fileName, expectedHash] of Object.entries(images)) {
    if (!fileName.endsWith('.webp')) {
      throw new Error(`${locale}/${fileName} must use the WebP format.`);
    }
    const image = readFileSync(join(screensRoot, locale, fileName));
    if (image.subarray(0, 4).toString('ascii') !== 'RIFF'
      || image.subarray(8, 12).toString('ascii') !== 'WEBP') {
      throw new Error(`${locale}/${fileName} is not a valid WebP image.`);
    }
    const actualHash = createHash('sha256')
      .update(image)
      .digest('hex');
    if (actualHash !== expectedHash) {
      throw new Error(`${locale}/${fileName} does not match its recorded artwork checksum.`);
    }
  }
}

console.log(`Verified the ${manifest.createdAt} English and pt-BR promotional artwork for Kalima ${manifest.releaseVersion}.`);
