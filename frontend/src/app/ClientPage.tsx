"use client";
import { Button } from "@/components/ui/button";
// 경로를 상대 경로로 수정하여 오류를 해결합니다.
import { LoginMemberContext } from "../stores/auth/loginMemberStore";
import { use } from "react";

export default function Page() {
  const { isLogin, loginMember } = use(LoginMemberContext);

  return (
    <>
      {/* 로그인이 되어있지 않은 경우, 카카오 로그인 버튼을 보여줍니다. */}
      {!isLogin && (
        <div className="flex flex-grow justify-center items-center">
          <Button>
            {/* 로그인 후 리다이렉트 될 URL은 실제 환경에 맞게 수정해주세요. */}
            <a href="http://localhost:8080/oauth2/authorization/kakao?redirectUrl=http://localhost:3000">
              카카오 로그인
            </a>
          </Button>
        </div>
      )}

      {/* 로그인이 되어있는 경우, 환영 메시지와 함께 결제 페이지로 이동하는 버튼을 보여줍니다. */}
      {isLogin && (
        <div className="flex flex-col flex-grow justify-center items-center space-y-4">
          <div className="text-2xl font-bold">{loginMember.nickname}님, 환영합니다.</div>
          <p className="text-gray-500">수강 신청을 완료하려면 결제를 진행해주세요.</p>
          <Button size="lg" asChild className="bg-blue-500 hover:bg-blue-600 text-white">
            {/* '/toss' 경로는 실제 결제 페이지가 있는 곳으로 지정해야 합니다. */}
            <a href="/toss">결제 페이지로 이동</a>
          </Button>
        </div>
      )}
    </>
  );
}
