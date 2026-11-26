package com.busticket.desktop;

import java.awt.*;
import java.sql.Connection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class BusTicketApp {
    static final Color INK = new Color(28, 45, 57);
    static final Color TEAL = new Color(0, 116, 110);
    static final Color PALE = new Color(241, 246, 244);
    static final Color MUTED = new Color(105, 122, 130);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Connection connection = Database.connect();
                connection.setAutoCommit(false);
                new MainWindow(connection).setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to Oracle. Check the connection settings and run schema.sql.\n\n" + e.getMessage(),
                        "Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    static JButton button(String text) {
        JButton b = new JButton(text); b.setBackground(TEAL); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBorder(new EmptyBorder(11, 17, 11, 17)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    static JLabel label(String text) { JLabel l=new JLabel(text); l.setForeground(INK); l.setFont(new Font("Segoe UI",Font.PLAIN,14)); return l; }
    static JPanel card() { JPanel p=new JPanel(); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(18,20,18,20)); return p; }
}
