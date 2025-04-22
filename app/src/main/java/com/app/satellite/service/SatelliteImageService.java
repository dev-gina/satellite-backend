package com.app.satellite.service;

import com.app.satellite.dto.SatelliteImageDTO;

import java.util.List;

public interface SatelliteImageService {

    public void convertImageToCog(SatelliteImageDTO satelliteImageDTO);

    public void convertBatchImagesToCog(List<SatelliteImageDTO> satelliteImageDTOList);

    public SatelliteImageDTO getMetadata(Long id);

    public List<SatelliteImageDTO> getFileList();

    public void saveMetadata(SatelliteImageDTO satelliteImageDTO);
}
