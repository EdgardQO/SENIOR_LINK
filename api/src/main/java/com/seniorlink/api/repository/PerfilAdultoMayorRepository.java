package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.PerfilAdultoMayor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilAdultoMayorRepository extends JpaRepository<PerfilAdultoMayor, String> {
}