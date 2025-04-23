package com.app.satellite.controller;

import com.app.satellite.dto.SatelliteImageDTO;
import com.app.satellite.service.SatelliteImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/satellite-images")
@Tag(name = "Satellite Image API", description = "위성 영상 관리 시스템 API")
public class SatelliteImageController {

    private final SatelliteImageService satelliteImageService;

    @Operation(summary = "TIFF 원본 파일 목록 조회")
    @GetMapping("/source-list")
    public ResponseEntity<List<SatelliteImageDTO>> getFileList() {
        return ResponseEntity.ok(satelliteImageService.getTiffSourceFilesOnly());
    }

    @Operation(summary = "메타데이터 저장")
    @PostMapping("/metadata")
    public ResponseEntity<Void> saveMetadata(@RequestBody SatelliteImageDTO dto) {
        satelliteImageService.saveMetadata(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "이름 기준 메타데이터 검색")
    @GetMapping("/metadata")
    public ResponseEntity<List<SatelliteImageDTO>> searchMetadata(
        @RequestParam(required = false) String name) {
        return ResponseEntity.ok(satelliteImageService.searchMetadata(name, null, null));
    }

    @Operation(summary = "COG 변환 및 S3 업로드 (단건)")
    @PostMapping("/upload")
    public ResponseEntity<Void> uploadConvertedImage(@RequestBody SatelliteImageDTO dto) {
        satelliteImageService.uploadConvertedImageToS3(dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "COG 변환 및 S3 업로드 (다건)")
    @PostMapping("/upload-batch")
    public ResponseEntity<Void> uploadBatch(@RequestBody List<SatelliteImageDTO> list) {
        satelliteImageService.convertBatchImagesToCog(list);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "ID로 메타데이터 조회")
    @GetMapping("/metadata/{id}")
    public ResponseEntity<SatelliteImageDTO> getMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(satelliteImageService.getMetadata(id));
    }

    @Operation(summary = "메타데이터 검색 기능 확장")
    @GetMapping("/search")
    public ResponseEntity<List<SatelliteImageDTO>> searchMetadata(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer width,
        @RequestParam(required = false) Integer bandCount
    ) {
        return ResponseEntity.ok(satelliteImageService.searchMetadata(name, width, bandCount));
    }
}
