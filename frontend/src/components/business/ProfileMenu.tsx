"use client";

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import client from "@/lib/backend/client";
import { LoginMemberContext } from "@/stores/auth/loginMemberStore";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { use } from "react";

export default function HomeMenu() {
  const router = useRouter();
  const { isLogin, loginMember, removeLoginMember } = use(LoginMemberContext);

  async function handleLogout(e: React.MouseEvent<HTMLAnchorElement>) {
    e.preventDefault();
    try {
      // openapi-fetch 라이브러리의 표준 응답 형식에 맞춰 error 객체를 직접 구조분해합니다.
      const { error } = await client.DELETE("/api/members/logout", {
        credentials: "include",
      });

      // API 요청 실패 시 error 객체에 정보가 담겨옵니다.
      if (error) {
        // error 객체에서 메시지를 추출하여 사용자에게 알립니다.
        const errorMessage = (error as any).msg || '로그아웃에 실패했습니다.';
        alert(errorMessage);
        return;
      }

      // 에러가 없으면 성공으로 간주하고 로그아웃 처리를 진행합니다.
      removeLoginMember();
      router.replace("/");

    } catch (err) {
      // 네트워크 문제 등 예기치 않은 오류 발생 시 처리합니다.
      console.error("Logout failed:", err);
      alert("로그아웃 중 예기치 않은 오류가 발생했습니다.");
    }
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger>
        {/* 로그인 상태이고, loginMember 객체와 profileImageUrl이 모두 유효한 값일 때만 Image를 렌더링합니다. */}
        {isLogin && loginMember?.profileImageUrl && (
          <DropdownMenuLabel>
            <div className="flex gap-2 items-center">
              <div>
                <Image
                  className="w-10 h-10 rounded-full"
                  src={loginMember.profileImageUrl}
                  alt="프로필 이미지"
                  width={80}
                  height={80}
                  quality={100}
                />
              </div>
            </div>
          </DropdownMenuLabel>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        {isLogin && (
          <DropdownMenuLabel>
            <div className="text-lg">{loginMember.nickname}</div>
          </DropdownMenuLabel>
        )}
        {isLogin && (
          <DropdownMenuItem asChild>
            <Link href="" onClick={handleLogout}>
              로그아웃
            </Link>
          </DropdownMenuItem>
        )}
        {isLogin && (
          <DropdownMenuItem asChild>
            <Link href="/member/me">내정보</Link>
          </DropdownMenuItem>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
