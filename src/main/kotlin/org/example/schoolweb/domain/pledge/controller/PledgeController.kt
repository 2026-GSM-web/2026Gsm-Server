package org.example.schoolweb.domain.pledge.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.example.schoolweb.domain.pledge.dto.PledgeCreateRequest
import org.example.schoolweb.domain.pledge.dto.PledgeResponse
import org.example.schoolweb.domain.pledge.dto.PledgeUpdateRequest
import org.example.schoolweb.domain.pledge.service.PledgeService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pledges")
@Tag(name = "Pledge", description = "공약 목록 CRUD - 조회는 전체 공개, 생성/수정/삭제는 관리자 전용")
class PledgeController(
    private val pledgeService: PledgeService
) {

    @GetMapping
    @Operation(summary = "공약 목록 조회", description = "displayOrder 오름차순으로 전체 공약을 조회한다. 비로그인 상태에서도 호출 가능하다.")
    fun getAll(): List<PledgeResponse> = pledgeService.getAll()

    @GetMapping("/{id}")
    @Operation(summary = "공약 단건 조회", description = "비로그인 상태에서도 호출 가능하다.")
    fun getOne(@PathVariable id: Long): PledgeResponse = pledgeService.getById(id)

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "공약 생성 (관리자)", description = "관리자만 호출 가능.")
    fun create(
        @Valid @RequestBody request: PledgeCreateRequest
    ): PledgeResponse = pledgeService.create(request)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "공약 수정 (관리자)", description = "관리자만 호출 가능. 전체 필드를 교체한다.")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: PledgeUpdateRequest
    ): PledgeResponse = pledgeService.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "공약 삭제 (관리자)", description = "관리자만 호출 가능.")
    fun delete(@PathVariable id: Long) {
        pledgeService.delete(id)
    }
}
