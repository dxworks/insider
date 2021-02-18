package org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import org.dxworks.dxplatform.plugins.insider.dependencyAnalyser.dtos.Comment;

import java.io.File;
import java.util.List;

public class CommentService {

    @SneakyThrows
    public List<Comment> getComments() {
        ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.readValue(new File(getClass().getResource("/comments.json").getPath().substring(1)), new TypeReference<List<Comment>>() {
        });
    }
}
