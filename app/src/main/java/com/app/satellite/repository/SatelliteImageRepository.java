package com.app.satellite.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.app.satellite.model.SatelliteImage;

public interface SatelliteImageRepository extends JpaRepository<SatelliteImage, Long> {

     // 이름 검색 기능
     public List<SatelliteImage> findByNameContainingIgnoreCase(String name);

     // 시퀀스 증가
     public int countByName(String name);

}
