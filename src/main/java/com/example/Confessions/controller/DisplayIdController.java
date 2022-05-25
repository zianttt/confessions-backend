package com.example.Confessions.controller;

import com.example.Confessions.model.DisplayId;
import com.example.Confessions.repository.DisplayIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/")
public class DisplayIdController {

    @Autowired
    private DisplayIdRepository displayIdRepository;

    @GetMapping("/displayId")
    public DisplayId getDisPlayId() {
        List<DisplayId> allDisplayId = displayIdRepository.findAll();
        // For the first post
        if (allDisplayId.size() == 0) {
            return new DisplayId(0, 0);
        }
        return allDisplayId.get(allDisplayId.size()-1);
    }

    @PostMapping("/displayId")
    public DisplayId createPost(@RequestBody DisplayId displayId) {
        return displayIdRepository.save(displayId);
    }
}
