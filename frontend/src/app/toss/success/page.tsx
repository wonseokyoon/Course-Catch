"use client";

import React, { useEffect, useState } from 'react';

// Next.js 라우터 훅 대신 표준 웹 API를 사용하도록 수정했습니다.
export default function SuccessPage() {
  const [statusMessage, setStatusMessage] = useState("결제를 승인하는 중입니다...");
  const [isSuccess, setIsSuccess] = useState<boolean | null>(null);

  useEffect(() => {
    const confirmPayment = async () => {
      // useSearchParams 훅 대신 URLSearchParams를 사용하여 쿼리 파라미터를 직접 파싱합니다.
      const params = new URLSearchParams(window.location.search);
      const paymentKey = params.get("paymentKey");
      const orderId = params.get("orderId");
      const amount = params.get("amount");

      if (!paymentKey || !orderId || !amount) {
        setStatusMessage("결제 승인에 필요한 정보가 URL에 없습니다.");
        setIsSuccess(false);
        return;
      }

      try {
        const fetchOptions = {
          method: 'POST',
          credentials: 'include' as RequestCredentials,
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ paymentKey, orderId, amount: Number(amount) }),
        };

        // 백엔드에 최종 결제 승인 요청
        const response = await fetch("/api/payment/confirm", fetchOptions);
        const responseData = await response.json();

        if (response.ok) {
          setStatusMessage("결제가 성공적으로 완료되었습니다!");
          setIsSuccess(true);
        } else {
          // 백엔드에서 보낸 에러 메시지를 표시
          setStatusMessage(`결제 승인 실패: ${responseData.msg || '알 수 없는 오류가 발생했습니다.'}`);
          setIsSuccess(false);
        }
      } catch (error) {
        console.error("결제 승인 네트워크 오류:", error);
        setStatusMessage(`결제 승인 중 오류가 발생했습니다: ${error instanceof Error ? error.message : String(error)}`);
        setIsSuccess(false);
      }
    };

    confirmPayment();
  }, []); // 컴포넌트가 마운트될 때 한 번만 실행

  const handleGoHome = () => {
    // useRouter 훅 대신 window.location을 사용하여 페이지를 이동합니다.
    window.location.href = '/';
  };

  return (
    <div className="bg-gray-50 min-h-screen flex items-center justify-center">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-lg p-10 text-center space-y-6">
        {isSuccess === null ? (
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto"></div>
        ) : isSuccess ? (
          <svg className="mx-auto h-16 w-16 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        ) : (
          <svg className="mx-auto h-16 w-16 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        )}
        <h1 className={`text-2xl font-bold ${isSuccess ? 'text-gray-800' : 'text-red-600'}`}>
          {isSuccess ? "결제 완료" : "결제 실패"}
        </h1>
        <p className="text-gray-600">{statusMessage}</p>
        <button
          onClick={handleGoHome} // 메인 페이지로 이동
          className="w-full bg-blue-500 text-white font-bold py-3 px-4 rounded-lg hover:bg-blue-600 focus:outline-none focus:ring-4 focus:ring-blue-300 transition-all duration-300"
        >
          홈으로 돌아가기
        </button>
      </div>
    </div>
  );
}
