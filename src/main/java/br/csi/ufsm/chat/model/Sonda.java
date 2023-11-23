package br.csi.ufsm.chat.model;

import lombok.*;

import java.net.InetAddress;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sonda {
    private String tipoMensagem;
    private String usuario;
    private Usuario.Status status;
}
