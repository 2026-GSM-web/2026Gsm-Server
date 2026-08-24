package org.example.schoolweb.domain.pledge.service

import org.example.schoolweb.domain.pledge.dto.PledgeCreateRequest
import org.example.schoolweb.domain.pledge.dto.PledgeResponse
import org.example.schoolweb.domain.pledge.dto.PledgeUpdateRequest
import org.example.schoolweb.domain.pledge.entity.Pledge
import org.example.schoolweb.domain.pledge.repository.PledgeRepository
import org.example.schoolweb.global.exception.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PledgeService(
    private val pledgeRepository: PledgeRepository
) {

    @Transactional(readOnly = true)
    fun getAll(): List<PledgeResponse> =
        pledgeRepository.findAllByOrderByDisplayOrderAscIdAsc().map { PledgeResponse.from(it) }

    @Transactional(readOnly = true)
    fun getById(id: Long): PledgeResponse = PledgeResponse.from(findOrThrow(id))

    @Transactional
    fun create(request: PledgeCreateRequest): PledgeResponse {
        val pledge = pledgeRepository.save(
            Pledge(
                title = request.title,
                content = request.content,
                category = request.category,
                status = request.status,
                displayOrder = request.displayOrder
            )
        )
        return PledgeResponse.from(pledge)
    }

    @Transactional
    fun update(id: Long, request: PledgeUpdateRequest): PledgeResponse {
        val pledge = findOrThrow(id)
        pledge.title = request.title
        pledge.content = request.content
        pledge.category = request.category
        pledge.status = request.status
        pledge.displayOrder = request.displayOrder
        return PledgeResponse.from(pledge)
    }

    @Transactional
    fun delete(id: Long) {
        pledgeRepository.delete(findOrThrow(id))
    }

    private fun findOrThrow(id: Long): Pledge =
        pledgeRepository.findById(id)
            .orElseThrow { NotFoundException("공약을 찾을 수 없습니다. id=$id") }
}
