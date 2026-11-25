package com.seniorlink.api.controller;
import com.seniorlink.api.repository.UsuarioRepository;
import com.seniorlink.api.service.FotoPerfilService;
import com.seniorlink.api.model.entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.OffsetDateTime;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/verificaciones")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class VerificacionController {
    private final UsuarioRepository usuarios;
    private final FotoPerfilService fotos;
    public VerificacionController(UsuarioRepository usuarios, FotoPerfilService fotos) {
        this.usuarios=usuarios; this.fotos=fotos;
    }
    public record Vista(String estado, OffsetDateTime fecha, String motivo, OffsetDateTime version) {}
    public record Revision(String estado, String motivo, OffsetDateTime version) {}
    private Vista vista(Usuario u) {
        return new Vista(u.getEstadoVerificacion(),u.getFechaVerificacion(),u.getMotivoRechazo(),u.getFotoActualizadaEn());
    }
    @GetMapping("/{dni}")
    public Vista consultar(@PathVariable String dni) {
        return vista(usuarios.findById(dni).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
    @PutMapping("/{dni}")
    @Transactional
    public Vista revisar(@PathVariable String dni,@RequestBody Revision r,Authentication auth) {
        if (dni.equals(auth.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No puedes verificar tu propia identidad");
        if (r==null || !("VERIFICADO".equals(r.estado()) || "RECHAZADO".equals(r.estado())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado inválido");
        String motivo=r.motivo()==null?null:r.motivo().trim();
        if ("RECHAZADO".equals(r.estado()) && (motivo==null || motivo.isBlank() || motivo.length()>500))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indica un motivo de hasta 500 caracteres");
        Usuario u=usuarios.bloquear(dni).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!Objects.equals(u.getFotoActualizadaEn(),r.version())) throw new ResponseStatusException(HttpStatus.CONFLICT,"La foto cambió; vuelve a revisarla");
        fotos.obtener(dni);
        u.revisarFoto(r.estado(),auth.getName(),"RECHAZADO".equals(r.estado())?motivo:null);
        usuarios.save(u);
        return vista(u);
    }
}
