package com.jvm.game;

import java.awt.*;

public class Particle {
    float x, y, dx, dy;
    int life;
    int maxLife;
    int size;
    Color color;

    public Particle(float x, float y) {
        this.x = x;
        this.y = y;

        this.dx = (float)(Math.random() * 4 - 2);
        this.dy = (float)(Math.random() * -3);

        this.maxLife = 30 + (int)(Math.random() * 20);
        this.life = maxLife;

        this.size = 3 + (int)(Math.random() * 3);

        this.color = new Color(
            200 + (int)(Math.random() * 55),
            200 + (int)(Math.random() * 55),
            200 + (int)(Math.random() * 55)
        );
    }

    public void update() {
        x += dx;
        y += dy;
        dy += 0.1f;
        life--;
    }

    public void draw(Graphics2D g) {
        float alpha = (float) life / maxLife;
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(color);
        g.fillOval((int)x, (int)y, size, size);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    public boolean isDead() {
        return life <= 0;
    }
}
