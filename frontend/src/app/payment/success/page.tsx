"use client";

import { Suspense, useEffect, useState } from 'react';

// Suspense로 감싸야 useSearchParams를 사용할 수 있습니다.
export default function SuccessPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <SuccessContent />
    </Suspense>
  );
}

function SuccessContent() {
  // next/navigation 대신 브라우저 표준 API를 사용하도록 수정
  const [params, setParams] = useState({
    paymentKey: '',
    orderId: '',
    amount: '',
  });

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    setParams({
      paymentKey: searchParams.get('paymentKey') || '',
      orderId: searchParams.get('orderId') || '',
      amount: searchParams.get('amount') || '',
    });
  }, []);

  return (
    <div className="bg-gray-50 min-h-screen flex items-center justify-center font-sans">
      <div className="w-full max-w-lg bg-white rounded-xl shadow-lg p-8 text-center space-y-6">
        <div className="mx-auto bg-green-100 rounded-full h-20 w-20 flex items-center justify-center">
          <svg className="h-12 w-12 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-gray-800">결제 성공</h1>
        <p className="text-gray-600">결제가 성공적으로 완료되었습니다.</p>
        
        <div className="border-t border-b border-gray-200 py-4 space-y-3 text-left bg-gray-50 rounded-lg px-6">
          <div className="flex justify-between">
            <span className="text-gray-500">주문번호:</span>
            <span className="font-mono text-gray-800">{params.orderId}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">결제금액:</span>
            <span className="font-semibold text-gray-800">{Number(params.amount).toLocaleString()}원</span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">결제키:</span>
            <span className="font-mono text-xs text-gray-800">{params.paymentKey}</span>
          </div>
        </div>

        {/* next/link 대신 표준 a 태그를 사용하도록 수정 */}
        <a href="/" className="inline-block w-full bg-blue-500 text-white font-bold py-3 px-4 rounded-lg hover:bg-blue-600 transition-all duration-300">
          홈으로 돌아가기
        </a>
      </div>
    </div>
  );
}
