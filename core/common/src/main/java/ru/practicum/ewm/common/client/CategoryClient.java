package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.common.dto.CategoryDto;

import java.util.List;

@FeignClient(name = "category-service")
public interface CategoryClient {

    @GetMapping("/internal/categories/{categoryId}")
    CategoryDto getById(@PathVariable("categoryId") Long categoryId);

    @GetMapping("/internal/categories/by-name")
    CategoryDto getByName(@RequestParam("name") String name);

    @PostMapping("/internal/categories/batch")
    List<CategoryDto> getByIds(@RequestBody List<Long> ids);
}
