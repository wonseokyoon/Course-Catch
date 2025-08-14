// ClientPage.tsx
"use client"; // 이 컴포넌트는 클라이언트 측에서 실행되어야 합니다.

import React, { useState, useEffect } from 'react';

// --- 타입 정의 ---
interface TossPayments {
  requestPayment: (paymentType: string, paymentData: PaymentData) => Promise<SuccessData | FailData>;
}
interface PaymentData {
  amount: number;
  orderId: string;
  orderName: string;
  customerName: string;
  successUrl: string;
  failUrl: string;
}
interface SuccessData {
  paymentKey: string;
  orderId: string;
  amount: number;
}
interface FailData {
  errorCode: string;
  errorMessage: string;
  orderId: string;
}
interface PaymentDto {
  merchantUid: string;
  amount: number;
  courseName: string;
}
declare global {
  interface Window {
    TossPayments: (clientKey: string) => TossPayments;
  }
}

// 컴포넌트 이름을 파일명과 일치시킵니다.
export default function ClientPage() {
  // --- 상태 관리 ---
  const [orderInfo, setOrderInfo] = useState<PaymentDto | null>(null);
  const [tossPayments, setTossPayments] = useState<TossPayments | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [statusMessage, setStatusMessage] = useState('주문 정보를 불러오는 중...');

  // --- 상수 정의 ---
  const TOSS_CLIENT_KEY = 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq';
  
  // --- 함수: 백엔드에 결제 정보 생성 요청 ---
  const fetchOrderInfo = async () => {
    try {
      // 실제로는 백엔드의 /api/payment/request API를 호출해야 합니다.
      // 현재는 테스트를 위해 성공 응답을 시뮬레이션합니다.
      const response: PaymentDto = {
        merchantUid: `order_${new Date().getTime()}`,
        amount: 35000,
        courseName: '실전! 스프링 부트와 JPA 활용',
      };
      setOrderInfo(response);
      setStatusMessage('결제 준비가 완료되었습니다.');
    } catch (error) {
      console.error('주문 정보 로딩 실패:', error);
      setStatusMessage('주문 정보를 불러오는 데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  // --- useEffect: SDK 로드 및 주문 정보 자동 요청 ---
  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://js.tosspayments.com/v1';
    script.async = true;
    document.head.appendChild(script);

    script.onload = () => {
      if (window.TossPayments) {
        const tossInstance = window.TossPayments(TOSS_CLIENT_KEY);
        setTossPayments(tossInstance);
        fetchOrderInfo(); 
      }
    };

    return () => {
      document.head.removeChild(script);
    };
  }, []);

  // --- 함수: 토스페이먼츠 결제창 호출 ---
  const handleTossPayment = async () => {
    if (!tossPayments || !orderInfo) return;
    setIsLoading(true);
    setStatusMessage('결제창을 호출하는 중...');
    try {
      const result = await tossPayments.requestPayment('카드', {
        amount: orderInfo.amount,
        orderId: orderInfo.merchantUid,
        orderName: orderInfo.courseName,
        customerName: '김토스', // 실제로는 로그인된 사용자 이름 사용
        successUrl: `${window.location.origin}/payment/success`, // 성공 시 리다이렉트 될 URL
        failUrl: `${window.location.origin}/payment/fail`,     // 실패 시 리다이렉트 될 URL
      });
      if ('paymentKey' in result) {
        await handleConfirmPayment(result);
      } else {
        setStatusMessage(`결제 실패: ${result.errorMessage}`);
      }
    } catch (error) {
      console.error('결제창 호출 오류:', error);
      setStatusMessage('결제에 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };
  
  // --- 함수: 백엔드에 최종 결제 승인 요청 ---
  const handleConfirmPayment = async (data: SuccessData) => {
      setStatusMessage('최종 결제를 승인하는 중...');
      try {
        const response = await fetch('/api/payment/confirm', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                paymentKey: data.paymentKey,
                orderId: data.orderId,
                amount: data.amount,
            }),
        });
        if (response.ok) {
            setStatusMessage('결제가 성공적으로 완료되었습니다!');
        } else {
            const errorData = await response.json();
            throw new Error(errorData.message || '결제 승인에 실패했습니다.');
        }
    } catch (error) {
        console.error('결제 승인 실패:', error);
        setStatusMessage(String(error));
    }
  }

  // --- UI 렌더링 ---
  return (
    <div className="bg-gray-50 min-h-screen flex items-center justify-center font-sans">
      <div className="w-full max-w-md bg-white rounded-xl shadow-lg p-8 space-y-6">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-800">결제하기</h1>
          <p className="text-gray-500 mt-2">주문 내용을 확인 후 결제를 진행해주세요.</p>
        </div>

        <div className="border-t border-b border-gray-200 py-6 space-y-4">
          {isLoading ? (
             <div className="text-center text-gray-500 py-8">
                <p>{statusMessage}</p>
             </div>
          ) : orderInfo ? (
            <>
              <div className="flex justify-between items-center">
                <span className="text-gray-600">주문명</span>
                <span className="font-semibold text-gray-800">{orderInfo.courseName}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600">주문번호</span>
                <span className="font-mono text-sm text-gray-500">{orderInfo.merchantUid}</span>
              </div>
              <div className="flex justify-between items-center text-xl">
                <span className="text-gray-600 font-bold">총 결제금액</span>
                <span className="font-bold text-indigo-600">{orderInfo.amount.toLocaleString()}원</span>
              </div>
            </>
          ) : (
            <div className="text-center text-red-500 py-8">
                <p>{statusMessage}</p>
            </div>
          )}
        </div>

        <div className="space-y-4">
          <button
            onClick={handleTossPayment}
            disabled={isLoading || !orderInfo}
            className="w-full bg-blue-500 text-white font-bold py-3 px-4 rounded-lg hover:bg-blue-600 focus:outline-none focus:ring-4 focus:ring-blue-300 transition-all duration-300 disabled:bg-gray-300 disabled:cursor-not-allowed"
          >
            {isLoading ? '처리 중...' : (orderInfo ? `${orderInfo.amount.toLocaleString()}원 결제하기` : '결제 정보 로딩 중...')}
          </button>
        </div>
        
        {statusMessage.includes('완료') || statusMessage.includes('실패') ? (
            <p className={`text-center font-semibold ${statusMessage.includes('완료') ? 'text-green-600' : 'text-red-600'}`}>
                {statusMessage}
            </p>
        ) : null}
      </div>
    </div>
  );
}
