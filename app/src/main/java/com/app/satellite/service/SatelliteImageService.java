package com.app.satellite.service;

import com.app.satellite.dto.SatelliteImageDTO;
import java.util.List;

public interface SatelliteImageService {

    void saveMetadata(SatelliteImageDTO satelliteImageDTO);

    void uploadConvertedImageToS3(SatelliteImageDTO satelliteImageDTO);

    void convertBatchImagesToCog(List<SatelliteImageDTO> satelliteImageDTOList);

    List<SatelliteImageDTO> getTiffSourceFilesOnly();

    SatelliteImageDTO getMetadata(Long id);
}
