package com.example.Confessions.repository;

import com.example.Confessions.model.DisplayId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisplayIdRepository extends JpaRepository<DisplayId, Long> {


}
