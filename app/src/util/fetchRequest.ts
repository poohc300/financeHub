const BASE_URL = "http://localhost:8080"; // API 기본 URL

/**
 * 공통 fetch 유틸리티 함수
 * @param {string} endpoint - API 엔드포인트 (예: "/dashboard/data")
 * @param {string} method - HTTP 메서드 ("GET", "POST", "PUT", "PATCH")
 * @param {Object} [body] - 요청 본문 (JSON 데이터, 옵션)
 * @returns {Promise<T>} - DTO 타입을 반환하는 Promise
 */
export async function fetchRequest<T>(endpoint: string, method: string = "GET", body?: unknown): Promise<T> {
    const options: RequestInit = {
        method,
        headers: {
            "Content-Type": "application/json", // 공통 헤더
        },
    };

    if (body) {
        options.body = JSON.stringify(body); // 요청 본문 추가
    }

    try {
        const response = await fetch(`${BASE_URL}${endpoint}`, options);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json(); // 응답 데이터를 DTO 타입으로 반환
    } catch (error) {
        console.error("Fetch error:", error);
        throw error; // 에러를 호출한 곳으로 전달
    }
}
