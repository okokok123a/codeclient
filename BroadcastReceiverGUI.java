import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class BroadcastReceiverGUI extends JFrame {
    JTextArea thanhGhi;
    JButton nutLangNghe, nutTaiTep;
    DefaultListModel<String> danhSach;
    JList<String> danhSachTep;
    Map<String, ThongTinMayChu> thongTinFile;
    Map<String, String[]> danhSachTaiKhoan = new HashMap<>();

    public BroadcastReceiverGUI() {
        setTitle("Client");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        thanhGhi = new JTextArea();
        nutLangNghe = new JButton("Bat dau lang nghe Broadcast");
        nutTaiTep = new JButton("Tai file da chon");
        danhSach = new DefaultListModel<>();
        danhSachTep = new JList<>(danhSach);
        thongTinFile = new HashMap<>();

        thanhGhi.setEditable(false);

        JScrollPane cuonThanhGhi = new JScrollPane(thanhGhi);
        JScrollPane cuonDanhSach = new JScrollPane(danhSachTep);

        JPanel bangDieuKhien = new JPanel();
        nutLangNghe.addActionListener(e -> batDauLangNghe());
        nutTaiTep.addActionListener(e -> taiFile());

        bangDieuKhien.add(nutLangNghe);
        bangDieuKhien.add(nutTaiTep);

        add(bangDieuKhien, BorderLayout.NORTH);
        add(cuonDanhSach, BorderLayout.CENTER);
        JPanel khuVucThanhGhi = new JPanel(new BorderLayout());
        khuVucThanhGhi.setPreferredSize(new Dimension(800, 150));
        khuVucThanhGhi.add(cuonThanhGhi, BorderLayout.CENTER);
        add(khuVucThanhGhi, BorderLayout.SOUTH);
    }

    private void batDauLangNghe() {
        if (danhSachTep.getModel().getSize() > 0) {
            thanhGhii("Da bat lang nghe roi!");
            return;
        }
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(12345)) {
                socket.setBroadcast(true);
                byte[] buffer = new byte[1024];
                while (true) {
                    DatagramPacket goiTin = new DatagramPacket(buffer, buffer.length);
                    socket.receive(goiTin);
                    xuLyThongDiep(new String(goiTin.getData(), 0, goiTin.getLength()));
                }
            } catch (IOException e) {
                thanhGhii("Loi khi lang nghe: " + e.getMessage());
            }
        }).start();
        thanhGhii("Dang lang nghe broadcast...");
    }

    private void xuLyThongDiep(String thongDiep) {
        String[] phan = thongDiep.split(";");
        if (phan.length == 4) {
            String hienThi = phan[0] + " - " + phan[1] + " bytes from " + phan[2] + ":" + phan[3];
            ThongTinMayChu thongTin = new ThongTinMayChu(phan[0], Long.parseLong(phan[1]), phan[2], Integer.parseInt(phan[3]));
            if (!thongTinFile.containsKey(hienThi)) {
                thongTinFile.put(hienThi, thongTin);
                SwingUtilities.invokeLater(() -> {
                    danhSach.addElement(hienThi);
                    thanhGhii("Phat hien file: " + hienThi);
                });
            }
        }
    }

    private void taiFile() {
        String daChon = danhSachTep.getSelectedValue();
        if (daChon == null) {
            thanhGhii("Hay chon mot file de tai.");
            return;
        }

        ThongTinMayChu thongTin = thongTinFile.get(daChon);
        String key = thongTin.diaChiIP + ":" + thongTin.cuaSo;
        String username, password;

        if (danhSachTaiKhoan.containsKey(key)) {
            String[] info = danhSachTaiKhoan.get(key);
            username = info[0];
            password = info[1];
        } else {
            JPanel panel = new JPanel(new GridLayout(2, 2));
            JTextField userField = new JTextField();
            JPasswordField passField = new JPasswordField();
            panel.add(new JLabel("Username:"));
            panel.add(userField);
            panel.add(new JLabel("Password:"));
            panel.add(passField);
            int result = JOptionPane.showConfirmDialog(this, panel, "Đăng nhập", JOptionPane.OK_CANCEL_OPTION);
            if (result != JOptionPane.OK_OPTION) {
                thanhGhii("Đã huỷ đăng nhập.");
                return;
            }
            username = userField.getText();
            password = new String(passField.getPassword());
        }

        new Thread(() -> {
            try (Socket ketNoiTCP = new Socket(thongTin.diaChiIP, thongTin.cuaSo);
                 OutputStream os = ketNoiTCP.getOutputStream();
                 PrintWriter writer = new PrintWriter(os, true);
                 InputStream is = ketNoiTCP.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

                writer.println(username);
                writer.println(password);
                String phanHoi = reader.readLine();
                if ("AUTH_FAIL".equals(phanHoi)) {
                    thanhGhii("Sai username hoac password.");
                    danhSachTaiKhoan.remove(key);
                    return;
                }

                danhSachTaiKhoan.put(key, new String[]{username, password});
                writer.println(thongTin.tenFile);

                JFileChooser hopChon = new JFileChooser();
                hopChon.setSelectedFile(new File(thongTin.tenFile));
                if (hopChon.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File fileLuu = hopChon.getSelectedFile();
                    try (FileOutputStream fos = new FileOutputStream(fileLuu)) {
                        byte[] buffer = new byte[4096];
                        int soByteDoc;
                        while ((soByteDoc = is.read(buffer)) != -1)
                            fos.write(buffer, 0, soByteDoc);
                        thanhGhii("Tai thanh cong: " + fileLuu.getAbsolutePath());
                    }
                } else {
                    thanhGhii("Da huy luu file.");
                }

            } catch (IOException e) {
                thanhGhii("Loi khi tai file: " + e.getMessage());
            }
        }).start();
    }

    private void thanhGhii(String thongDiep) {
        SwingUtilities.invokeLater(() -> {
            thanhGhi.append(thongDiep + "\n");
            thanhGhi.setCaretPosition(thanhGhi.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BroadcastReceiverGUI().setVisible(true));
    }

    static class ThongTinMayChu {
        String tenFile, diaChiIP;
        long kichThuoc;
        int cuaSo;

        ThongTinMayChu(String tenFile, long kichThuoc, String diaChiIP, int cuaSo) {
            this.tenFile = tenFile;
            this.kichThuoc = kichThuoc;
            this.diaChiIP = diaChiIP;
            this.cuaSo = cuaSo;
        }
    }
}
