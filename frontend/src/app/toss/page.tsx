import React from 'react';
// import 경로에 파일 확장자를 명시하여 모듈을 찾을 수 있도록 수정합니다.
import ClientPage from './ClientPage';

export default function TossPage() {
  // /toss 경로로 접속하면 이 페이지가 보입니다.
  // 실제 화면 내용은 ClientPage 컴포넌트가 담당합니다.
  return <ClientPage />;
}
