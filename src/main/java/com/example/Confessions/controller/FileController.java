package com.example.Confessions.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.Confessions.dto.DataHandler;
import com.example.Confessions.model.File;
import com.example.Confessions.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.Confessions.message.ResponseFile;
import com.example.Confessions.message.ResponseMessage;

import javax.servlet.http.HttpServletRequest;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/")
public class FileController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private FileService storageService;

    @PostMapping("/files/{submitId}")
    public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file, @PathVariable Long submitId) {

        String message = "";

        try {

            String filePath = request.getServletContext().getRealPath("/");
            file.transferTo(new java.io.File(filePath));



            storageService.store(file, submitId);
            message = "Uploaded the file successfully: " + file.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            message = "Could not upload the file: " + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<ResponseFile>> getListFiles() {
        List<ResponseFile> files = storageService.getAllFiles().map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/files/")
                    .path(dbFile.getId())
                    .toUriString();

            return new ResponseFile(
                    dbFile.getName(),
                    fileDownloadUri,
                    dbFile.getType(),
                    dbFile.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(files);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        File uploadFile = storageService.getFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + uploadFile.getName() + "\"")
                .body(uploadFile.getData());
    }

    @PostMapping("/files/ids")
    public ResponseEntity<List<ResponseFile>> getFilesBySubmitIds(@RequestBody DataHandler submitIdsDetails) {
        List<Long> submitIdsNumbers = submitIdsDetails.getSubmitIds();
        List<File> files = storageService.getAllFiles().collect(Collectors.toList());
        List<File> selectedFiles = new ArrayList<>();
        for (File file: files ) {
            if (submitIdsNumbers.contains(file.getSubmitId()))
                selectedFiles.add(file);
        }

        List<ResponseFile> retFiles = selectedFiles.stream().map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/v1/files/")
                    .path(dbFile.getId())
                    .toUriString();

            return new ResponseFile(
                    dbFile.getName(),
                    fileDownloadUri,
                    dbFile.getType(),
                    dbFile.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(retFiles);
    }
}