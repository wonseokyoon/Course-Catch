"use client";

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
// [수정] 백엔드 응답에 맞춰 reservationId 필드 추가
interface ReservationDto {
    reservationId: number; 
    courseId: number;
    courseTitle: string;
    studentId: number;
    studentName: string;
    status: string;
    price: number;
    createdDate: string;
}
interface PaymentRequestResponseDto {
  reservationId: number;
  orderId: string;
  amount: number;
  paymentKey: string | null;
  status: string;
  createdDate: string;
}
interface LoginMember {
    nickname: string | null;
}

declare global {
  interface Window {
    TossPayments: (clientKey: string) => TossPayments;
  }
}

export default function ClientPage() {
  const [pendingReservations, setPendingReservations] = useState<ReservationDto[]>([]);
  const [tossPayments, setTossPayments] = useState<TossPayments | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [statusMessage, setStatusMessage] = useState('결제 대기 목록을 불러오는 중...');
  const [currentUser, setCurrentUser] = useState<LoginMember | null>(null);
  const [payingItemId, setPayingItemId] = useState<number | null>(null);

  const TOSS_CLIENT_KEY = 'test_ck_LlDJaYngroz40d27Z0bXVezGdRpX';  
  
  const fetchPendingReservations = async () => {
    setIsLoading(true);
    setStatusMessage('결제 대기 목록을 불러오는 중...');
    try {
      const fetchOptions = {
        credentials: 'include' as RequestCredentials,
        headers: { 'Content-Type': 'application/json' }
      };

      const pendingRes = await fetch(`/api/reserve/me/pending?page=1&pageSize=10`, fetchOptions);
      if (pendingRes.status === 401) {
          setStatusMessage('로그인이 필요합니다. 다시 로그인해주세요.');
          return;
      }
      if (!pendingRes.ok) {
        throw new Error(`결제 대기 목록을 불러오는 데 실패했습니다. (상태: ${pendingRes.status})`);
      }
      
      const pendingData = await pendingRes.json();
      
      if (!pendingData.data || !pendingData.data.items || pendingData.data.items.length === 0) {
        setStatusMessage('결제할 항목이 없습니다.');
        setPendingReservations([]);
        return;
      }
      
      const reservations: ReservationDto[] = pendingData.data.items.map((item: any) => ({
        ...item,
        createdDate: item.createdDatetime
      }));

      setPendingReservations(reservations);
      if (reservations.length > 0) {
        setCurrentUser({ nickname: reservations[0].studentName });
      }
      setStatusMessage('');

    } catch (error) {
      console.error('결제 대기 목록 조회 실패:', error);
      setStatusMessage(error instanceof Error ? error.message : String(error));
    } finally {
      setIsLoading(false);
    }
  };

  const handlePaymentRequest = async (reservation: ReservationDto) => {
    if (!tossPayments) {
        alert("결제 모듈이 아직 준비되지 않았습니다. 잠시 후 다시 시도해주세요.");
        return;
    }
    setPayingItemId(reservation.reservationId);
    try {
      const fetchOptions = {
        method: 'POST',
        credentials: 'include' as RequestCredentials,
        headers: { 'Content-Type': 'application/json' }
      };
      
      // [수정] reservationId 파라미터에 reservation.reservationId를 전달
      const requestRes = await fetch(`/api/payment/request?reservationId=${reservation.reservationId}`, fetchOptions);
      if (!requestRes.ok) {
        throw new Error(`결제 요청에 실패했습니다. (상태: ${requestRes.status})`);
      }
      const paymentData: PaymentRequestResponseDto = (await requestRes.json()).data;

      await tossPayments.requestPayment('카드', {
        amount: paymentData.amount,
        orderId: paymentData.orderId,
        orderName: reservation.courseTitle,
        customerName: reservation.studentName,
        successUrl: `${window.location.origin}/toss/success`,
        failUrl: `${window.location.origin}/toss/fail`,
      });

    } catch (error) {
        console.error(`[${reservation.courseTitle}] 결제 처리 실패:`, error);
        alert(`'${reservation.courseTitle}' 결제에 실패했습니다: ${error instanceof Error ? error.message : String(error)}`);
    } finally {
        setPayingItemId(null);
    }
  };

  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://js.tosspayments.com/v1';
    script.async = true;
    document.head.appendChild(script);

    script.onload = () => {
      if (window.TossPayments) {
        setTossPayments(window.TossPayments(TOSS_CLIENT_KEY));
      }
    };

    fetchPendingReservations();

    return () => {
      if (document.head.contains(script)) {
        document.head.removeChild(script);
      }
    };
  }, []);

  return (
    <div className="bg-gray-50 min-h-screen flex justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="w-full max-w-2xl bg-white rounded-xl shadow-lg p-8 space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-800">결제 대기 목록</h1>
          {currentUser && <p className="text-gray-500 mt-2">{currentUser.nickname}님의 결제를 기다리는 수강 목록입니다.</p>}
        </div>

        <div className="flow-root">
          {isLoading ? (
             <div className="text-center text-gray-500 py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
                <p>{statusMessage}</p>
             </div>
          ) : pendingReservations.length > 0 ? (
            <ul role="list" className="-my-6 divide-y divide-gray-200">
              {pendingReservations.map((reservation) => (
                // [수정] React key를 reservation.reservationId로 변경
                <li key={reservation.reservationId} className="flex py-6">
                  <div className="flex-1 flex flex-col">
                    <div>
                      <div className="flex justify-between text-base font-medium text-gray-900">
                        <h3>{reservation.courseTitle}</h3>
                        <p className="ml-4">{reservation.price.toLocaleString()}원</p>
                      </div>
                      <p className="mt-1 text-sm text-gray-500">신청일: {new Date(reservation.createdDate).toLocaleDateString()}</p>
                    </div>
                    <div className="flex-1 flex items-end justify-between text-sm">
                      <p className="text-gray-500">상태: {reservation.status}</p>
                      <div className="flex">
                        <button
                          type="button"
                          onClick={() => handlePaymentRequest(reservation)}
                          disabled={payingItemId === reservation.reservationId}
                          className="font-medium text-white bg-blue-500 hover:bg-blue-600 px-4 py-2 rounded-md disabled:bg-gray-400 disabled:cursor-wait"
                        >
                          {payingItemId === reservation.reservationId ? '처리중...' : '결제하기'}
                        </button>
                      </div>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <div className="text-center text-gray-500 py-8">
                <p>{statusMessage || '결제할 항목이 없습니다.'}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
