package com.smartrent.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DuplicateCheckRequest {
    String title;
    String description;
    Double price;
    Double area;
    String productType;
    String provinceCode;
    Integer districtId;
    String address;
    List<String> imageUrls;
}
