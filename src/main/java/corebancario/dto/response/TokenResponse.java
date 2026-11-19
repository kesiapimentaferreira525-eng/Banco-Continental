package corebancario.dto.response;

public record TokenResponse(
        String token,
        String tipo,
        Long expiraEmSegundos
) {}
