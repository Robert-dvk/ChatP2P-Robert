package br.csi.ufsm.chat;

import br.csi.ufsm.chat.model.Usuario;
import br.csi.ufsm.chat.model.Mensagem;
import br.csi.ufsm.chat.service.SondaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import static br.csi.ufsm.chat.model.Usuario.Status.*;

/**
 *
 * User: Rafael
 * Date: 13/10/14
 * Time: 10:28
 *
 */
public class ChatClientSwing extends JFrame {

    public Usuario meuUsuario;
    private final String endBroadcast = "255.255.255.255";
    private JList listaChat;
    public DefaultListModel dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private Set<Usuario> chatsAbertos = new HashSet<>();
    private Socket socket;

    private PainelChatPVT painel;

    @SneakyThrows
    public ChatClientSwing() throws UnknownHostException {
        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(DISPONIVEL);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(NAO_PERTURBE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(VOLTO_LOGO);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);
        menuBar.add(menu);
        this.setJMenuBar(menuBar);
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            if (e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu popupMenu =  new JPopupMenu();
                final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                JMenuItem item = new JMenuItem("Fechar");
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PainelChatPVT painel = (PainelChatPVT) tabbedPane.getTabComponentAt(tab);
                        tabbedPane.remove(tab);
                        chatsAbertos.remove(painel.getUsuario());
                    }
                });
                popupMenu.add(item);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuario: ");
        this.meuUsuario = new Usuario(nomeUsuario, DISPONIVEL, InetAddress.getLocalHost());
        new SondaService(this);
        new Thread(new Sessao()).start();
        Thread.sleep(5000);
        this.painel = new PainelChatPVT(meuUsuario);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Chat P2P - Redes de Computadores");
        setVisible(true);
    }
    private JComponent criaLista() {
        dfListModel = new DefaultListModel();
        SondaService.listaUsuarios.stream().forEach(usuario -> dfListModel.addElement(usuario));
        listaChat = new JList(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            @SneakyThrows
            public void mouseClicked(MouseEvent evt) {
            JList list = (JList) evt.getSource();
            if (evt.getClickCount() == 2) {
                int index = list.locationToIndex(evt.getPoint());
                Usuario user = (Usuario) list.getModel().getElementAt(index);
                socket = new Socket(user.getEndereco(), 8085);
                if (chatsAbertos.add(user)) {
                    tabbedPane.add(user.toString(), painel);
                }
            }
            }
        });
        return listaChat;
    }


    class PainelChatPVT extends JPanel {

        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;

        PainelChatPVT(Usuario usuario) {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            campoEntrada.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JTextField) e.getSource()).setText("");
                    areaChat.append(meuUsuario.getNome() + "> " + e.getActionCommand() + "\n");
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }

        public Usuario getUsuario() {
            return usuario;
        }
    }

    public class Sessao implements Runnable {
        private ServerSocket serverSocket;

        @SneakyThrows
        public Sessao(){
            this.serverSocket = new ServerSocket(8085);
        }

        @SneakyThrows
        @Override
        public void run() {
            Socket soc = this.serverSocket.accept();
            byte[] buffer = new byte[1024];
            int size = soc.getInputStream().read(buffer);
            String msg = new String(buffer, 0, size);
            Mensagem mensagem = new ObjectMapper().readValue(msg, Mensagem.class);
            Usuario user = mensagem.getUser();
            PainelChatPVT painel = new PainelChatPVT(user);
            tabbedPane.add(user.toString(), painel);
            painel.areaChat.append(user.getNome()+"> "+ mensagem.getMensagem()+"\n");
            new Thread(new RecebeMsg(soc, painel)).start();
        }
        public class RecebeMsg implements Runnable {
            private Socket socket;
            PainelChatPVT painel;

            public RecebeMsg(Socket socket, PainelChatPVT painel){
                this.socket = socket;
                this.painel = painel;
            }
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    byte[] buffer = new byte[1024];
                    int size = socket.getInputStream().read(buffer);
                    String msg = new String(buffer, 0, size);
                    Mensagem mensagem = new ObjectMapper().readValue(msg, Mensagem.class);
                    Usuario user = mensagem.getUser();
                    painel.areaChat.append(user.getNome() + "> " + mensagem.getMensagem() + "\n");
                }
            }
        }
    }
    public static void main(String[] args) throws UnknownHostException {
        new ChatClientSwing();
    }
}
