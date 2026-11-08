package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.PerfilFamiliar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilFamiliarRepository extends JpaRepository<PerfilFamiliar, String> {
}