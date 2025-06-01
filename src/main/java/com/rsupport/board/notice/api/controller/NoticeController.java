package com.rsupport.board.notice.api.controller;

import com.rsupport.board.common.dto.ResponseDTO;
import com.rsupport.board.notice.api.dto.*;
import com.rsupport.board.notice.service.NoticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 공지(Notice) 관련 REST API 컨트롤러
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/v1/notices")
@Tag(name = "Notice", description = "공지 관련 API")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeService noticeService;

    @Operation(summary = "공지 생성", description = "공지를 생성합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201", // http status 201 created 반환
                    description = "공지 등록이 완료되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoticeResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "[BAD_REQUEST] 제목, 내용 등 유효성 검사 실패한 경우",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "[NOT_FOUND] 존재하지 않는 사용자인 경우",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class))
            )
    })
    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResponseDTO<NoticeResponseDTO>> createNotice(
            @Valid @ModelAttribute NoticeCreateReqDTO req
    ) {
        log.info("POST /v1/notices?{}", req);

        NoticeResponseDTO res = noticeService.createNotice(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseDTO.success(res));
    }


    @Operation(summary = "공지 목록 조회", description = "공지 목록을 조회합니다. \n최신 등록일 순으로 정렬되며, 제목+내용 / 내용 / 등록일자 로 검색가능합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", // http status 200 ok 반환
                    description = "공지 목록이 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoticeListItemDTO.class))
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<ResponseDTO<NoticeListResDTO>> getNoticeList(
            @Valid @ModelAttribute NoticeListReqDTO searchConditions,
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC, size = 20) Pageable pageable
    ) {
        log.info("GET /v1/notices?{}, pageable={}", searchConditions, pageable);

        NoticeListResDTO res = noticeService.getNoticeList(searchConditions, pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseDTO.success(res));
    }

    @Operation(summary = "공지 상세 조회", description = "선택한 공지를 조회합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", // http status 200 ok 반환
                    description = "공지가 조회되었습니다.",
                    content = @Content(schema = @Schema(implementation = NoticeListItemDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "[BAD_REQUEST] userId나 noticeId가 누락된 경우",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "[NOT_FOUND] 존재하지 않는 공지이거나 사용자인 경우",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class))
            )
    })
    @GetMapping(value = "/{noticeId}", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<ResponseDTO<NoticeResponseDTO>> getNotice(
            @NotNull @RequestParam Long userId,
            @PathVariable Long noticeId
    ) {
        log.info("GET /v1/notices/{}?{}", noticeId, userId);

        NoticeResponseDTO res = noticeService.getNotice(userId, noticeId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ResponseDTO.success(res));
    }
}
