package com.javaguy.nhxserver.controller;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/v1/hello")
@Tag(name = "Hello", description = "Simple greeting endpoint for testing API accessibility")
public class HelloController {
    @Operation(summary = "Returns a greeting message", description = "A simple endpoint that returns 'Hello World!' to demonstrate API accessibility.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(mediaType = "text/plain",
                            schema = @Schema(type = "string", example = "Hello World!")))
    })
    @GetMapping
    public String hello() {
        return "Hello World!";
    }
}
