package com.rsupport.board.notice.api.controller;

import com.rsupport.board.common.dto.ResponseDTO;
import com.rsupport.board.notice.api.dto.NoticeCreateReqDTO;
import com.rsupport.board.notice.api.dto.NoticeResponseDTO;
import com.rsupport.board.notice.service.NoticeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공지(Notice) 관련 REST API 컨트롤러
 */
@RestController
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
                    description = "잘못된 요청입니다. (제목, 내용 등 유효성 검사 실패)",
                    content = @Content(schema = @Schema(implementation = ResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 사용자입니다",
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
        NoticeResponseDTO res = noticeService.createNotice(req);

        ResponseDTO<NoticeResponseDTO> body = ResponseDTO.success(res);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);
    }
}
