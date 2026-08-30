package org.example.schoolweb.domain.pledge.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

// 공약 하나하나를 담는 항목. 전체 이행률(%)은 이 목록과 별개로 PledgeProgress 싱글턴 로우가
// 담당하므로, 여기서는 개별 공약의 진행 상태(status)만 관리한다.
@Entity
@Table(name = "pledges")
class Pledge(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 200)
    var title: String,

    @Lob
    @Column(nullable = false)
    var content: String,

    // 분야를 고정 이넘으로 두지 않고 관리자가 자유롭게 붙이는 텍스트로 둔다 (분야 목록이
    // 아직 확정되지 않았음).
    @Column(length = 100)
    var category: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PledgeStatus = PledgeStatus.IN_PROGRESS,

    // status(3단계 이넘)와 별개로, 카드에 짧게 노출할 진행 상황 문구(예: "9월 중 도입 예정").
    // 이넘만으로는 표현 못하는 세부 진행 상황을 관리자가 자유 텍스트로 채운다.
    @Column(length = 255)
    var subStatus: String? = null,

    // 프론트에 보여줄 정렬 순서. 관리자가 드래그 정렬 등으로 직접 지정한다.
    @Column(nullable = false)
    var displayOrder: Int = 0,

    @CreationTimestamp
    @Column(updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    var updatedAt: LocalDateTime? = null
)
