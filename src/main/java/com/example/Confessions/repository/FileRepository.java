package com.example.Confessions.repository;

import com.example.Confessions.model.File;
import com.example.Confessions.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, String> {

}