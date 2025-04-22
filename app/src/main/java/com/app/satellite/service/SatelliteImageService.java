package com.app.satellite.service;

import com.app.satellite.dto.SatelliteImageDTO;

import java.util.List;

public interface SatelliteImageService {

    List<SatelliteImageDTO> getFileList();

    void saveMetadata(SatelliteImageDTO satelliteImageDTO);

    void convertImageToCog(SatelliteImageDTO satelliteImageDTO);

    void convertBatchImagesToCog(List<SatelliteImageDTO> satelliteImageDTOList);

    void uploadConvertedImageToS3(SatelliteImageDTO satelliteImageDTO);

    SatelliteImageDTO getMetadata(Long id);

    List<SatelliteImageDTO> getTiffSourceFilesOnly();

    List<SatelliteImageDTO> getConvertedFilesOnly();
}