import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // API 요청을 백엔드(localhost:8080)로 전달하기 위한 프록시 설정
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
  // 기존 이미지 설정 유지
  images: {
    dangerouslyAllowSVG: true,
    remotePatterns: [
      {
        protocol: "http",
        hostname: "img1.kakaocdn.net",
      },
      {
        protocol: "https",
        hostname: "img1.kakaocdn.net",
      },
      {
        protocol: "https",
        hostname: "placehold.co",
      },
      {
        protocol: "http",
        hostname: "k.kakaocdn.net",
      },
    ],
    contentSecurityPolicy: "default-src 'self'; img-src 'self' data: https:;",
  },
};

export default nextConfig;
