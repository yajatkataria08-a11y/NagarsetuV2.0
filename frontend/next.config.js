/** @type {import('next').NextConfig} */
const nextConfig = {
  // Phase 15: Enable standalone output for Docker container image
  output: 'standalone',
  reactStrictMode: true,
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: '**.openstreetmap.org' },
    ],
  },
};

module.exports = nextConfig;
