/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: '**.openstreetmap.org' },
    ],
  },
};

module.exports = nextConfig;
