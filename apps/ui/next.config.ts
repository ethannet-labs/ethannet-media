import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: [
    "@ethannet/messages",
    "@ethannet/navigation",
    "@ethannet/schema",
  ],
};

export default nextConfig;
