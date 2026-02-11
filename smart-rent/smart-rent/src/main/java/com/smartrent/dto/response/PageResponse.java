package com.smartrent.dto.response;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse <T> {
  Integer page;
  Integer size;
  Long totalElements;
  Integer totalPages;

  @Builder.Default
  List<T> data = Collections.emptyList();
}
