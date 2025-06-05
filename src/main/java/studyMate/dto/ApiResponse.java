package studyMate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;


@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
}
