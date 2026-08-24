package org.example.schoolweb.domain.pledge.entity

// 프론트(lib/pledges.ts)가 쓰는 3단계 상태값. 완료 여부만 담던 기존 completed(Boolean)로는
// "시범 운영 중" 상태를 표현할 수 없어서 이넘으로 교체했다.
enum class PledgeStatus {
    IN_PROGRESS, // 진행 중
    PILOT,       // 시범 운영 중
    COMPLETED    // 완료
}
