package com.enterpriseai.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.enterpriseai.backend.common.ApiResponse;
import com.enterpriseai.backend.common.PageResponse;
import com.enterpriseai.backend.document.service.DocumentService;
import com.enterpriseai.backend.dto.DocumentResponse;
import com.enterpriseai.backend.dto.DocumentDetailsResponse;
import com.enterpriseai.backend.dto.DocumentTextResponse;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDetailsResponse>> upload(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        DocumentDetailsResponse response = documentService.upload(authentication.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Document uploaded successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DocumentDetailsResponse>>> list(
            Pageable pageable, 
            @RequestParam(required = false) String originalFileName,
            @RequestParam(required = false) com.enterpriseai.backend.entity.DocumentType documentType,
            Authentication authentication) {
        Page<DocumentDetailsResponse> page = documentService.list(authentication.getName(), pageable, originalFileName, documentType);
        return ResponseEntity.ok(new ApiResponse<>(true, "Documents fetched successfully", PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDetailsResponse>> get(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Document fetched successfully",
                documentService.get(authentication.getName(), id)));
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<ApiResponse<DocumentTextResponse>> getText(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Document text fetched successfully",
                documentService.getText(authentication.getName(), id)));
    }
    
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @PathVariable Long id, Authentication authentication) {
        java.io.InputStream inputStream = documentService.download(authentication.getName(), id);
        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(inputStream);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document-" + id + "\"")
                .body(resource);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<DocumentDetailsResponse>> retry(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Document processing retried",
                documentService.retryProcessing(authentication.getName(), id)));
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<ApiResponse<DocumentDetailsResponse>> reindex(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Document re-indexing triggered",
                documentService.retryProcessing(authentication.getName(), id)));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/rename")
    public ResponseEntity<ApiResponse<DocumentDetailsResponse>> rename(
            @PathVariable Long id, 
            @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body,
            Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Document renamed successfully",
                documentService.rename(authentication.getName(), id, body.get("name"))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, Authentication authentication) {
        documentService.delete(authentication.getName(), id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document deleted successfully", null));
    }
}
