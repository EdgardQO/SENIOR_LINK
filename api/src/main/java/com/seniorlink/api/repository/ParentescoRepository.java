package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Parentesco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParentescoRepository extends JpaRepository<Parentesco, Short> {
    Optional<Parentesco> findByNombre(String nombre);
}