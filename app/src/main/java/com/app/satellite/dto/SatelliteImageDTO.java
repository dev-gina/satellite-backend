package com.app.satellite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SatelliteImageDTO {
    private String name;
    private int width;
    private int height;
    private int bandCount;
    private String remoteUrl;
    private String userName;
    private int sequence;
    private String cogPath;
}
