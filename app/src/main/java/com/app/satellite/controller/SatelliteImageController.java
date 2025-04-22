package com.app.satellite.controller;

import com.app.satellite.dto.SatelliteImageDTO;
import com.app.satellite.service.SatelliteImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/satellite-images")
public class SatelliteImageController {

    private final SatelliteImageService satelliteImageService;

    // 1. 원본 파일 목록 조회
    @GetMapping("/source-list")
    public ResponseEntity<List<SatelliteImageDTO>> getFileList() {
        return ResponseEntity.ok(satelliteImageService.getTiffSourceFilesOnly());
    }

    // 2. 메타 데이터 저장
    @PostMapping("/metadata")
    public ResponseEntity<Void> saveMetadata(@RequestBody SatelliteImageDTO dto) {
        satelliteImageService.saveMetadata(dto);
        return ResponseEntity.ok().build();
    }

    // 3. 변환 + 업로드 (단건)
    @PostMapping("/upload")
    public ResponseEntity<Void> uploadConvertedImage(@RequestBody SatelliteImageDTO dto) {
        satelliteImageService.uploadConvertedImageToS3(dto);
        return ResponseEntity.ok().build();
    }

    // 4. 변환 + 업로드 ()
    @PostMapping("/upload-batch")
    public ResponseEntity<Void> uploadBatch(@RequestBody List<SatelliteImageDTO> list) {
        satelliteImageService.convertBatchImagesToCog(list);
        return ResponseEntity.ok().build();
    }

    // 5. 메타 데이터 조회
    @GetMapping("/metadata/{id}")
    public ResponseEntity<SatelliteImageDTO> getMetadata(@PathVariable Long id) {
        return ResponseEntity.ok(satelliteImageService.getMetadata(id));
    }
}
