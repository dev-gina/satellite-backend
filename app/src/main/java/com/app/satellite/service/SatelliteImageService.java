package com.app.satellite.service;

import com.app.satellite.dto.SatelliteImageDTO;
import java.util.List;

public interface SatelliteImageService {

    public void saveMetadata(SatelliteImageDTO satelliteImageDTO);

    public void uploadConvertedImageToS3(SatelliteImageDTO satelliteImageDTO);

    public void uploadConvertedImageToS3(SatelliteImageDTO satelliteImageDTO, boolean isBatch);

    public void convertBatchImagesToCog(List<SatelliteImageDTO> satelliteImageDTOList);

    public List<SatelliteImageDTO> getTiffSourceFilesOnly();

    public SatelliteImageDTO getMetadata(Long id);

    public List<SatelliteImageDTO> searchMetadata(String name, Integer width, Integer bandCount);

}
