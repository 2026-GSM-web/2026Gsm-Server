package org.example.schoolweb.domain.pledge.service

import org.example.schoolweb.domain.pledge.dto.PledgeProgressResponse
import org.example.schoolweb.domain.pledge.dto.PledgeProgressUpdateRequest
import org.example.schoolweb.domain.pledge.entity.PledgeProgress
import org.example.schoolweb.domain.pledge.repository.PledgeProgressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PledgeProgressService(
    private val pledgeProgressRepository: PledgeProgressRepository
) {

    @Transactional(readOnly = true)
    fun get(): PledgeProgressResponse = PledgeProgressResponse.from(findOrCreate())

    @Transactional
    fun update(request: PledgeProgressUpdateRequest): PledgeProgressResponse {
        val progress = findOrCreate()
        progress.percentage = request.percentage
        return PledgeProgressResponse.from(progress)
    }

    // 싱글턴 로우가 아직 없으면(최초 배포 직후 등) 0%짜리 로우를 만들어 반환한다.
    private fun findOrCreate(): PledgeProgress =
        pledgeProgressRepository.findById(PledgeProgress.SINGLETON_ID)
            .orElseGet { pledgeProgressRepository.save(PledgeProgress()) }
}
