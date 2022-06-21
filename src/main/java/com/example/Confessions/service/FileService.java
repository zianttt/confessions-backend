package com.example.Confessions.service;

import java.io.IOException;
import java.util.stream.Stream;

import com.example.Confessions.model.File;
import com.example.Confessions.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
public class FileService {

    @Autowired
    private FileRepository uploadFileRepository;

    public File store(MultipartFile file, long submitId) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        File uploadFile = new File(fileName, file.getContentType(), file.getBytes(), submitId);

        return uploadFileRepository.save(uploadFile);
    }

    public File getFile(String id) {
        return uploadFileRepository.findById(id).get();
    }

    public Stream<File> getAllFiles() {
        return uploadFileRepository.findAll().stream();
    }
}