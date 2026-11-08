package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.PerfilAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PerfilAdminRepository extends JpaRepository<PerfilAdmin, String> {
}