package com.app.satellite.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.satellite.model.SatelliteImage;

public interface SatelliteImageRepository extends JpaRepository<SatelliteImage, Long> {


}
