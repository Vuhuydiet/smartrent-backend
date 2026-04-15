package com.smartrent.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = """
        Request body for broker registration.

        **Upload flow (do this before calling /register):**
        1. Call `POST /v1/media/upload-url` three times with `purpose=BROKER_DOCUMENT, mediaType=IMAGE`.
        2. PUT each file to the returned presigned URL.
        3. Call `POST /v1/media/{mediaId}/confirm` for each upload.
        4. Submit the three confirmed mediaIds here.
        """)
public class BrokerRegisterRequest {

    @NotNull(message = "CCCD front image is required")
    @Schema(
            description = "Media ID of the confirmed CCCD (National ID) front-side image",
            example = "101",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Long cccdFrontMediaId;

    @NotNull(message = "CCCD back image is required")
    @Schema(
            description = "Media ID of the confirmed CCCD (National ID) back-side image",
            example = "102",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Long cccdBackMediaId;

    @NotNull(message = "Practising certificate image is required")
    @JsonAlias("certFrontMediaId")
    @Schema(
            description = "Media ID of the confirmed practising certificate image. Backward compatible alias: certFrontMediaId",
            example = "103",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    Long certMediaId;
}
