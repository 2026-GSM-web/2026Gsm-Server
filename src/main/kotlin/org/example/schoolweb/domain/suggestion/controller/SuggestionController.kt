package org.example.schoolweb.domain.suggestion.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.example.schoolweb.domain.suggestion.entity.SuggestionStatus
import org.example.schoolweb.domain.suggestion.dto.SuggestionCreateRequest
import org.example.schoolweb.domain.suggestion.dto.SuggestionResponse
import org.example.schoolweb.domain.suggestion.dto.SuggestionUpdateRequest
import org.example.schoolweb.domain.suggestion.dto.SuggestionUpdateStatusRequest
import org.example.schoolweb.global.security.CustomUserPrincipal
import org.example.schoolweb.domain.suggestion.service.SuggestionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/suggestions")
@Tag(name = "Suggestion", description = "건의사항 생성/조회/수정/삭제 및 관리자 처리")
class SuggestionController(
    private val suggestionService: SuggestionService
) {

    @PostMapping
    @Operation(summary = "건의사항 생성", description = "로그인한 사용자 본인 명의로 새 건의사항을 등록한다. 비로그인 상태로 호출하면 401이 반환된다.")
    fun create(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: SuggestionCreateRequest
    ): SuggestionResponse = suggestionService.create(principal, request)

    @GetMapping("/me")
    @Operation(summary = "내가 쓴 건의사항 목록", description = "로그인한 사용자 본인이 작성한 건의사항을 페이지 단위로 조회한다.")
    fun getMine(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        pageable: Pageable
    ): Page<SuggestionResponse> = suggestionService.getMine(principal, pageable)

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "전체 건의사항 목록 (관리자)", description = "관리자만 호출 가능. status로 필터링해서 전체 건의사항을 조회한다.")
    fun getAll(
        @Parameter(description = "필터링할 상태값. 생략하면 전체 조회") @RequestParam(required = false) status: SuggestionStatus?,
        pageable: Pageable
    ): Page<SuggestionResponse> = suggestionService.getAllForAdmin(status, pageable)

    @GetMapping("/{id}")
    @Operation(summary = "건의사항 단건 조회", description = "작성자 본인 또는 관리자만 조회 가능. 그 외에는 403이 반환된다.")
    fun getOne(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable id: Long
    ): SuggestionResponse = suggestionService.getById(principal, id)

    @PutMapping("/{id}")
    @Operation(summary = "건의사항 수정", description = "제목/내용을 수정한다. 작성자 본인 또는 관리자만 가능하며, 그 외에는 403이 반환된다.")
    fun update(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: SuggestionUpdateRequest
    ): SuggestionResponse = suggestionService.update(principal, id, request)

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "처리 상태 변경 (관리자)", description = "관리자만 호출 가능. 건의사항의 처리 상태와 답변을 갱신한다.")
    fun updateStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: SuggestionUpdateStatusRequest
    ): SuggestionResponse = suggestionService.updateStatus(id, request)

    @DeleteMapping("/{id}")
    @Operation(summary = "건의사항 삭제", description = "작성자 본인 또는 관리자만 가능하며, 그 외에는 403이 반환된다.")
    fun delete(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @PathVariable id: Long
    ) {
        suggestionService.delete(principal, id)
    }
}
