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

    // 1. 단 건 변환
    @PostMapping("/convert")
    public ResponseEntity<Void> convertSingleImage(@RequestBody SatelliteImageDTO satelliteImageDTO) {
        satelliteImageService.convertImageToCog(satelliteImageDTO);
        return ResponseEntity.ok().build();  
    }
    
    // 2. 다 건 변환
    @PostMapping("/convert-batch")
    public ResponseEntity<Void> convertBatchImages(@RequestBody List<SatelliteImageDTO> satelliteImageDTOList) {
        satelliteImageService.convertBatchImagesToCog(satelliteImageDTOList);
        return ResponseEntity.ok().build();  
    }

    // 3. 파일 목록 조회 (S3에서 파일 목록 가져오기)
    @GetMapping("/list")
    public ResponseEntity<List<SatelliteImageDTO>> getFileList() {
        List<SatelliteImageDTO> fileList = satelliteImageService.getFileList();
        return ResponseEntity.ok(fileList);
    }

    // 4. 메타 데이터 저장
    @PostMapping("/metadata")
    public ResponseEntity<Void> saveMetadata(@RequestBody SatelliteImageDTO satelliteImageDTO) {
        satelliteImageService.saveMetadata(satelliteImageDTO);
        return ResponseEntity.ok().build();
    }

    // 5. 메타 데이터 조회
    @GetMapping("/metadata/{id}")
    public ResponseEntity<SatelliteImageDTO> getMetadata(@PathVariable Long id) {
        SatelliteImageDTO satelliteImageDTO = satelliteImageService.getMetadata(id);
        return ResponseEntity.ok(satelliteImageDTO);
    }
}
