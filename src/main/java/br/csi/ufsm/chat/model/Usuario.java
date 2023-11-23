package br.csi.ufsm.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetAddress;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {
    private String nome;
    private Long tempoAusente;
    private Status status;
    private InetAddress endereco;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return endereco.equals(usuario.endereco);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endereco);
    }

    public Usuario(String nome, Status status, InetAddress endereco) {
        this.nome = nome;
        this.status = status;
        this.endereco = endereco;
    }

    public enum Status{
        DISPONIVEL, NAO_PERTURBE, VOLTO_LOGO
    }

    public String toString() {
        return this.getNome() + " (" + getStatus().toString() + ")";
    }
}