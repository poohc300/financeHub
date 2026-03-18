/**
 * 외부 링크를 새 브라우저 탭에서 엽니다.
 * PWA/모바일 환경에서 target="_blank" 단독 사용 시 앱을 벗어나는 문제를 방지합니다.
 */
export function openLink(url: string) {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
}
