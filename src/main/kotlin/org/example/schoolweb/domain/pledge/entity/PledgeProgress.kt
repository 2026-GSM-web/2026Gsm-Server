package org.example.schoolweb.domain.pledge.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

// 프론트의 공약 이행률 페이지는 진행률 바 하나로 전체 이행률(%)만 보여주므로, 공약을
// 개별적으로 관리하는 대신 값 하나만 담는 싱글턴 로우로 저장한다. 항상 id=SINGLETON_ID인
// 로우 하나만 존재하도록 서비스 계층(PledgeProgressService.findOrCreate)에서 보장한다.
@Entity
@Table(name = "pledge_progress")
class PledgeProgress(
    @Id
    val id: Long = SINGLETON_ID,

    @Column(nullable = false)
    var percentage: Int = 0,

    @UpdateTimestamp
    var updatedAt: LocalDateTime? = null
) {
    companion object {
        const val SINGLETON_ID = 1L
    }
}
