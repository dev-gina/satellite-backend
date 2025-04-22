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

    // 1. 파일 목록 조회 (S3에서 파일 목록 가져오기)
    @GetMapping("/source-list")
    public ResponseEntity<List<SatelliteImageDTO>> getFileList() {
        List<SatelliteImageDTO> fileList = satelliteImageService.getTiffSourceFilesOnly();
        return ResponseEntity.ok(fileList);
    }

    // 2. 메타 데이터 저장
    @PostMapping("/metadata")
    public ResponseEntity<Void> saveMetadata(@RequestBody SatelliteImageDTO satelliteImageDTO) {
        satelliteImageService.saveMetadata(satelliteImageDTO);
        return ResponseEntity.ok().build();
    }

    // 3-1. 단 건 변환
    @PostMapping("/convert")
    public ResponseEntity<Void> convertSingleImage(@RequestBody SatelliteImageDTO satelliteImageDTO) {
        satelliteImageService.convertImageToCog(satelliteImageDTO);
        return ResponseEntity.ok().build();  
    }

    // 3-2. 다 건 변환
    @PostMapping("/convert-batch")
    public ResponseEntity<Void> convertBatchImages(@RequestBody List<SatelliteImageDTO> satelliteImageDTOList) {
        satelliteImageService.convertBatchImagesToCog(satelliteImageDTOList);
        return ResponseEntity.ok().build();  
    }

    // 4. 변환 결과 S3에 업로드
    @PostMapping("/upload")
    public ResponseEntity<Void> uploadConvertedImage(@RequestBody SatelliteImageDTO satelliteImageDTO) {
        satelliteImageService.uploadConvertedImageToS3(satelliteImageDTO);
        return ResponseEntity.ok().build();
    }

    // 5. 메타 데이터 조회
    @GetMapping("/metadata/{id}")
    public ResponseEntity<SatelliteImageDTO> getMetadata(@PathVariable Long id) {
        SatelliteImageDTO satelliteImageDTO = satelliteImageService.getMetadata(id);
        return ResponseEntity.ok(satelliteImageDTO);
    }

    @GetMapping("/result-list")
    public List<SatelliteImageDTO> getResultList() {
        return satelliteImageService.getConvertedFilesOnly();
    }
}