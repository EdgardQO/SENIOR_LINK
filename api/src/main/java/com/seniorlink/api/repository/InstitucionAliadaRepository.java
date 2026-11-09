package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.InstitucionAliada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstitucionAliadaRepository extends JpaRepository<InstitucionAliada, String> {
}
