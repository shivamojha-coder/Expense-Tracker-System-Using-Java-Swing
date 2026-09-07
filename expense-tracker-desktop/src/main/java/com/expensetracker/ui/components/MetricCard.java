package com.expensetracker.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public final class MetricCard extends JPanel {
    private final JLabel valueLabel = new JLabel("—");

    public MetricCard(String title, String supportingText) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));
        setPreferredSize(new Dimension(210, 118));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setForeground(new Color(100, 116, 139));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 26f));
        valueLabel.setForeground(new Color(15, 23, 42));

        JLabel supportingLabel = new JLabel(supportingText);
        supportingLabel.setForeground(new Color(100, 116, 139));
        supportingLabel.setFont(supportingLabel.getFont().deriveFont(12f));

        add(titleLabel);
        add(javax.swing.Box.createVerticalStrut(10));
        add(valueLabel);
        add(javax.swing.Box.createVerticalGlue());
        add(supportingLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }
}