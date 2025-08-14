"use client";

import { Suspense, useEffect, useState } from 'react';

// Suspense로 감싸야 URL 파라미터를 안전하게 읽을 수 있습니다.
export default function FailPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <FailContent />
    </Suspense>
  );
}

function FailContent() {
  // next/navigation 대신 브라우저 표준 API를 사용하도록 수정
  const [params, setParams] = useState({
    errorCode: '',
    errorMessage: '',
    orderId: '',
  });

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    setParams({
      errorCode: searchParams.get('code') || '',
      errorMessage: searchParams.get('message') || '',
      orderId: searchParams.get('orderId') || '',
    });
  }, []);

  return (
    <div className="bg-gray-50 min-h-screen flex items-center justify-center font-sans">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-lg p-8 text-center space-y-6">
        <div className="mx-auto bg-red-100 rounded-full h-20 w-20 flex items-center justify-center">
          <svg className="h-12 w-12 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-gray-800">결제 실패</h1>
        <p className="text-gray-600">결제 중 오류가 발생했습니다.</p>
        
        <div className="border-t border-b border-gray-200 py-4 space-y-3 text-left bg-gray-50 rounded-lg px-6">
          <div className="flex justify-between">
            <span className="text-gray-500">주문번호:</span>
            <span className="font-mono text-gray-800">{params.orderId}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">오류코드:</span>
            <span className="font-semibold text-red-600">{params.errorCode}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">오류메시지:</span>
            <span className="text-sm text-red-600">{params.errorMessage}</span>
          </div>
        </div>

        {/* next/link 대신 표준 a 태그를 사용하도록 수정 */}
        <a href="/toss" className="inline-block w-full bg-gray-600 text-white font-bold py-3 px-4 rounded-lg hover:bg-gray-700 transition-all duration-300">
          결제 페이지로 다시 시도
        </a>
      </div>
    </div>
  );
}
