"use client";

import { useSearchParams, useRouter } from 'next/navigation';
import React, { Suspense } from 'react';

function FailComponent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const errorCode = searchParams.get('code');
  const errorMessage = searchParams.get('message');

  return (
    <div className="bg-gray-50 min-h-screen flex items-center justify-center">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-lg p-10 text-center space-y-6">
         <svg className="mx-auto h-16 w-16 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        <h1 className="text-2xl font-bold text-red-600">결제 실패</h1>
        <div className="text-left bg-gray-100 p-4 rounded-lg">
            <p><span className="font-semibold">오류 코드:</span> {errorCode}</p>
            <p><span className="font-semibold">오류 메시지:</span> {errorMessage}</p>
        </div>
        <button
          onClick={() => router.push('/toss')} // 결제 대기 목록 페이지로 이동
          className="w-full bg-gray-500 text-white font-bold py-3 px-4 rounded-lg hover:bg-gray-600 focus:outline-none focus:ring-4 focus:ring-gray-300 transition-all duration-300"
        >
          결제 다시 시도하기
        </button>
      </div>
    </div>
  );
}

export default function FailPage() {
    return (
        <Suspense fallback={<div className="flex justify-center items-center min-h-screen">Loading...</div>}>
            <FailComponent />
        </Suspense>
    )
}
