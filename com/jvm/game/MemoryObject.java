package com.jvm.game;

import java.awt.*;

public class MemoryObject {
    public float x, y;
    public int size, sides;
    public float speed;
    public String type;
    public Color color;

    public int age = 0;
    public boolean isTenured = false;
    public int hitsRequired = 1;
    public boolean isBoss = false;

    public MemoryObject(int x, String type, float speedMultiplier) {
        this.x = x;
        this.y = -50;
        this.type = type;

        if (type.equals("int")) {
            this.sides = 3;
            this.color = new Color(0, 255, 150);
            this.speed = 4.5f * speedMultiplier;
            this.size = 30;
        } else if (type.equals("String")) {
            this.sides = 5;
            this.color = new Color(0, 150, 255);
            this.speed = 3.5f * speedMultiplier;
            this.size = 40;
        } else {
            this.sides = 6;
            this.color = new Color(155, 89, 182);
            this.speed = 2.5f * speedMultiplier;
            this.size = 50;
        }
    }

    public void update() {
        y += speed;
        age++;

        if (age > 180 && !isTenured) {
            isTenured = true;
            hitsRequired = 2;
            this.color = color.darker().darker();
        }
    }

    public void draw(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[] xP = new int[sides];
        int[] yP = new int[sides];
        for (int i = 0; i < sides; i++) {
            xP[i] = (int) (x + size/2 + (size/2) * Math.cos(i * 2 * Math.PI / sides - Math.PI / 2));
            yP[i] = (int) (y + size/2 + (size/2) * Math.sin(i * 2 * Math.PI / sides - Math.PI / 2));
        }

        if (isBoss) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
            g2.fillOval((int)x - 15, (int)y - 15, size + 30, size + 30);
        }

        g2.setColor(color);
        g2.fillPolygon(xP, yP, sides);
        g2.setColor(isTenured ? Color.RED : Color.WHITE);
        g2.setStroke(new BasicStroke(isTenured ? 3 : 1));
        g2.drawPolygon(xP, yP, sides);

        g2.setFont(new Font("Monospaced", Font.BOLD, isBoss ? 16 : 12));
        String label = isBoss ? "BOSS_LEAK" : (isTenured ? "OLD_GEN" : type);
        g2.drawString(label, x, y - 10);

        if (isBoss) {
            int barWidth = size;
            int maxHits = 20;
            double pct = Math.max(0, (double) hitsRequired / maxHits);
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect((int)x, (int)y - 26, barWidth, 8);
            g2.setColor(Color.RED);
            g2.fillRect((int)x, (int)y - 26, (int)(barWidth * pct), 8);
        } else if (hitsRequired > 1) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2.drawString("HP:" + hitsRequired, (int)x, (int)y + size + 14);
        }
    }

    public boolean isOffScreen() {
        return y > 560;
    }
}
