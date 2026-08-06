package com.kinhduanpc.service;

import com.kinhduanpc.dto.TagDTO;
import com.kinhduanpc.dto.TagRequest;
import com.kinhduanpc.entity.Tag;
import com.kinhduanpc.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TagService {

    private final TagRepository tagRepo;

    @Transactional(readOnly = true)
    public List<TagDTO> findAll(String search) {
        List<Tag> tags = (search == null || search.isBlank())
            ? tagRepo.findAll()
            : tagRepo.findByNameContainingIgnoreCase(search);
        return tags.stream().map(this::toDTO).toList();
    }

    public TagDTO create(TagRequest request) {
        String slug = generateSlug(request.getName());
        
        Tag tag = Tag.builder()
            .name(request.getName())
            .slug(slug)
            .build();

        Tag saved = tagRepo.save(tag);
        return toDTO(saved);
    }

    // Mapping method
    private TagDTO toDTO(Tag tag) {
        return TagDTO.builder()
            .id(tag.getId())
            .name(tag.getName())
            .slug(tag.getSlug())
            .build();
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
            .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
            .replaceAll("[èéẹẻẽêềếệểễ]", "e")
            .replaceAll("[ìíịỉĩ]", "i")
            .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
            .replaceAll("[ùúụủũưừứựửữ]", "u")
            .replaceAll("[ỳýỵỷỹ]", "y")
            .replaceAll("[đ]", "d")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();

        // Ensure uniqueness
        String base = slug;
        int i = 1;
        while (tagRepo.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
