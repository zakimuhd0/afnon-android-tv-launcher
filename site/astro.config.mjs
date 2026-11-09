// Pages are pre-rendered for Kuala Lumpur (the app's default city) so search engines see real times;
// in the browser everything is recomputed for the visitor's own city and clock.
process.env.TZ = 'Asia/Kuala_Lumpur';

import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

export default defineConfig({
  site: 'https://egyjs.github.io',
  base: '/afnon-android-tv-launcher',
  trailingSlash: 'always',
  integrations: [sitemap({ i18n: { defaultLocale: 'ar', locales: { ar: 'ar', en: 'en' } } })],
});
