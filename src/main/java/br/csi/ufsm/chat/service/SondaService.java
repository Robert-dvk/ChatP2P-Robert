package br.csi.ufsm.chat.service;

import br.csi.ufsm.chat.model.Sonda;
import br.csi.ufsm.chat.model.Usuario;
import br.csi.ufsm.chat.ChatClientSwing;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SondaService {
    public static final Set<Usuario> listaUsuarios = new HashSet<>();
    private ChatClientSwing chat;

    public class EnviaSonda implements Runnable{

        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket datagramSocket = new DatagramSocket();
            while (true){
                Sonda sonda = Sonda.builder().tipoMensagem("sonda").usuario(chat.meuUsuario.getNome()).status(chat.meuUsuario.getStatus()).build();
                byte[] pacote = new ObjectMapper().writeValueAsString(sonda).getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(pacote, 0, pacote.length, InetAddress.getByName("255.255.255.255"), 8084);
                datagramSocket.send(packet);
                Thread.sleep(5000);
            }
        }
    }

    public class RecebeSonda implements Runnable{

        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket datagramSocket = new DatagramSocket(8084);
            while (true){
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);
                String pacote = new String(buffer, 0, packet.getLength(), StandardCharsets.UTF_8);
                Sonda sonda = new ObjectMapper().readValue(pacote, Sonda.class);
                Usuario usuario = Usuario.builder().nome(sonda.getUsuario()).tempoAusente(System.currentTimeMillis()).status(sonda.getStatus()).endereco(packet.getAddress()).build();
                synchronized (listaUsuarios) {
                    listaUsuarios.remove(usuario);
                    listaUsuarios.add(usuario);
                }
                System.out.println(listaUsuarios);
            }
        }
    }

    public class AtualizaLista implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            while (true) {
                Thread.sleep(8000);
                synchronized (listaUsuarios) {
                    List<Usuario> listaRemover = listaUsuarios.stream().filter(u -> (System.currentTimeMillis() - u.getTempoAusente()) > 30000).toList();
                    listaRemover.stream().forEach(u -> chat.dfListModel.removeElement(u));
                }
                listaUsuarios.stream().forEach(u ->  chat.dfListModel.removeElement(u));
                listaUsuarios.stream().forEach(u ->  chat.dfListModel.addElement(u));
            }
        }
    }
    public SondaService(ChatClientSwing chat){
        this.chat = chat;
        new Thread(new RecebeSonda()).start();
        new Thread(new EnviaSonda()).start();
        new Thread(new AtualizaLista()).start();
    }
}