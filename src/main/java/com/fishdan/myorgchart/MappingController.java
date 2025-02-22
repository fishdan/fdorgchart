package com.fishdan.myorgchart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MappingController {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @GetMapping("/mappings")
    public String listMappings() {
        StringBuilder mappings = new StringBuilder();
        handlerMapping.getHandlerMethods().forEach((key, value) -> {
            mappings.append(key.toString()).append(" -> ").append(value).append("\n");
        });
        return mappings.toString();
    }
}
