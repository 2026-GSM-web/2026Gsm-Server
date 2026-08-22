package org.example.schoolweb.domain.pledge.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.example.schoolweb.domain.pledge.dto.PledgeProgressResponse
import org.example.schoolweb.domain.pledge.dto.PledgeProgressUpdateRequest
import org.example.schoolweb.domain.pledge.service.PledgeProgressService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pledge-progress")
@Tag(name = "PledgeProgress", description = "공약 이행률(퍼센트) 조회 및 관리자 수정")
class PledgeProgressController(
    private val pledgeProgressService: PledgeProgressService
) {

    @GetMapping
    @Operation(summary = "공약 이행률 조회", description = "프론트 진행률 바에 표시할 공약 이행률(%)을 조회한다. 비로그인 상태에서도 호출 가능하다.")
    fun get(): PledgeProgressResponse = pledgeProgressService.get()

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "공약 이행률 수정 (관리자)", description = "관리자만 호출 가능. 공약 이행률(%)을 0~100 사이 값으로 갱신한다.")
    fun update(
        @Valid @RequestBody request: PledgeProgressUpdateRequest
    ): PledgeProgressResponse = pledgeProgressService.update(request)
}
