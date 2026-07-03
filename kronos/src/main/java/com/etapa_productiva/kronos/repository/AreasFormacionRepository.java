package com.etapa_productiva.kronos.repository;

import com.etapa_productiva.kronos.entity.AreasFormacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreasFormacionRepository extends JpaRepository<AreasFormacion, Long> {
    
}