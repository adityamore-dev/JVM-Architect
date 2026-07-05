package com.jvm.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GameWindow extends JPanel implements Runnable {
    private enum State { MENU, PLAYING, PAUSED, GAMEOVER }
    private volatile State currentState = State.MENU;
    private static final long serialVersionUID = 1L;
    private Thread gameThread;
    private volatile boolean isRunning = false;
    private volatile MemoryObject boss = null;

    private final CopyOnWriteArrayList<MemoryObject> objects = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

    private final AtomicInteger score = new AtomicInteger(0);
    private final AtomicInteger health = new AtomicInteger(100);
    private final AtomicInteger combo = new AtomicInteger(0);
    private volatile int highScore = 0;
    private volatile int level = 1;
    private volatile boolean jitMode = false;
    private volatile int jitTimer = 0;

    private Point lassoStart, lassoEnd;

    private final Rectangle startBtn = new Rectangle(300, 250, 200, 50);
    private final Rectangle exitBtn = new Rectangle(300, 320, 200, 50);
    private final Rectangle pauseBtn = new Rectangle(680, 20, 100, 35);

    public GameWindow() {
        this.setPreferredSize(new Dimension(800, 600));
        this.setFocusable(true);
        loadHighScore();

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                requestFocusInWindow();
                if (currentState == State.MENU) {
                    if (startBtn.contains(p)) resetGame();
                    else if (exitBtn.contains(p)) System.exit(0);
                } else if (currentState == State.PLAYING) {
                    if (pauseBtn.contains(p)) currentState = State.PAUSED;
                    else lassoStart = p;
                } else if (currentState == State.PAUSED && pauseBtn.contains(p)) {
                    currentState = State.PLAYING;
                } else if (currentState == State.GAMEOVER) {
                    currentState = State.MENU;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentState == State.PLAYING && lassoStart != null) {
                    handleParallelGC(e.getPoint());
                    lassoStart = null;
                    lassoEnd = null;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentState == State.PLAYING) lassoEnd = e.getPoint();
            }
        };
        this.addMouseListener(ma);
        this.addMouseMotionListener(ma);

        KeyAdapter ka = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    if (currentState == State.PLAYING) currentState = State.PAUSED;
                    else if (currentState == State.PAUSED) currentState = State.PLAYING;
                } else if (code == KeyEvent.VK_P) {
                    if (currentState == State.PLAYING) currentState = State.PAUSED;
                    else if (currentState == State.PAUSED) currentState = State.PLAYING;
                } else if (code == KeyEvent.VK_R) {
                    if (currentState == State.GAMEOVER || currentState == State.PAUSED) resetGame();
                } else if (code == KeyEvent.VK_ENTER) {
                    if (currentState == State.MENU) resetGame();
                    else if (currentState == State.GAMEOVER) currentState = State.MENU;
                }
            }
        };
        this.addKeyListener(ka);
    }

    private void handleParallelGC(Point end) {
        Rectangle lasso = new Rectangle(
            Math.min(lassoStart.x, end.x), Math.min(lassoStart.y, end.y),
            Math.max(1, Math.abs(lassoStart.x - end.x)), Math.max(1, Math.abs(lassoStart.y - end.y))
        );

        boolean hit = false;
        for (MemoryObject obj : objects) {
            if (lasso.intersects(new Rectangle((int)obj.x, (int)obj.y, obj.size, obj.size))) {
                obj.hitsRequired--;
                hit = true;
                if (obj.hitsRequired <= 0) {
                    objects.remove(obj);
                    for (int i = 0; i < 10; i++) {
                        particles.add(new Particle(obj.x, obj.y));
                    }
                    score.addAndGet(jitMode ? 20 : 10);
                    combo.incrementAndGet();
                    health.updateAndGet(h -> Math.min(100, h + 1));
                }
            }
        }

        if (boss != null && lasso.intersects(new Rectangle((int)boss.x, (int)boss.y, boss.size, boss.size))) {
            boss.hitsRequired--;
            hit = true;
            if (boss.hitsRequired <= 0) {
                for (int i = 0; i < 60; i++) {
                    particles.add(new Particle(boss.x + boss.size / 2f, boss.y + boss.size / 2f));
                }
                score.addAndGet(jitMode ? 200 : 100);
                combo.incrementAndGet();
                health.updateAndGet(h -> Math.min(100, h + 10));
                boss = null;
            }
        }

        if (!hit) combo.set(0);
        if (combo.get() >= 15 && !jitMode) triggerJIT();
    }

    private void triggerJIT() {
        jitMode = true;
        jitTimer = 300;
    }

    private void resetGame() {
        score.set(0);
        health.set(100);
        combo.set(0);
        level = 1;
        jitMode = false;
        boss = null;
        objects.clear();
        particles.clear();
        currentState = State.PLAYING;
        if (gameThread == null || !isRunning) startGame();
    }

    public synchronized void startGame() {
        isRunning = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double ns = 1000000000 / 60.0;
        double delta = 0;
        int spawnCounter = 0;

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;

            if (delta >= 1) {
                if (currentState == State.PLAYING) {
                    updateLogic();
                    if (++spawnCounter >= Math.max(15, 40 - (level * 2))) {
                        spawnObject();
                        spawnCounter = 0;
                    }
                }
                delta--;
            }
            repaint();

            try {
                Thread.sleep(1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void spawnObject() {
        String[] types = {"int", "String", "Object"};
        objects.add(new MemoryObject(new Random().nextInt(720), types[new Random().nextInt(3)], 1.0f + (level * 0.12f)));
    }

    private void updateLogic() {
        if (health.get() <= 0) {
            saveHighScore();
            currentState = State.GAMEOVER;
            return;
        }

        if (jitMode) {
            jitTimer--;
            if (jitTimer <= 0) jitMode = false;
        }

        for (MemoryObject obj : objects) {
            obj.update();
            if (obj.isOffScreen()) {
                objects.remove(obj);
                health.addAndGet(obj.isTenured ? -20 : -10);
                combo.set(0);
            }
        }

        for (Particle p : particles) {
            p.update();
            if (p.isDead()) particles.remove(p);
        }

        if (score.get() > level * 1000) {
            level++;
            if (level % 3 == 0 && boss == null) {
                MemoryObject b = new MemoryObject(300, "Object", 0.5f);
                b.size = 120;
                b.hitsRequired = 20;
                b.isBoss = true;
                boss = b;
            }
        }

        if (boss != null) {
            boss.update();
            if (boss.isOffScreen()) {
                health.addAndGet(-50);
                boss = null;
            }
        }
    }

    private void saveHighScore() {
        if (score.get() > highScore) {
            highScore = score.get();
            try (PrintWriter out = new PrintWriter("highscore.dat")) {
                out.println(highScore);
            } catch (Exception e) {
                System.err.println("Failed to save high score: " + e.getMessage());
            }
        }
    }

    private void loadHighScore() {
        File f = new File("highscore.dat");
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            highScore = Integer.parseInt(br.readLine());
        } catch (Exception e) {
            System.err.println("Failed to load high score, defaulting to 0: " + e.getMessage());
            highScore = 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Color topColor = jitMode ? new Color(60, 0, 0) : new Color(15, 15, 35);
        g2.setPaint(new GradientPaint(0, 0, topColor, 0, 600, new Color(40, 40, 90)));
        g2.fillRect(0, 0, 800, 600);

        if (currentState == State.MENU) drawMenu(g2);
        else if (currentState == State.PLAYING || currentState == State.PAUSED) drawPlay(g2);
        else if (currentState == State.GAMEOVER) drawGameOver(g2);
    }

    private void drawMenu(Graphics2D g2) {
        g2.setColor(Color.CYAN);
        g2.setFont(new Font("Verdana", Font.BOLD, 45));
        g2.drawString("JVM ARCHITECT GAME", 140, 180);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 20));
        g2.drawString("BEST HEAP SCORE: " + highScore, 280, 220);
        drawButton(g2, startBtn, "START RUNTIME", Color.GREEN);
        drawButton(g2, exitBtn, "EXIT SYSTEM", Color.RED);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.drawString("ENTER: start   ESC/P: pause   R: restart", 250, 400);
    }

    private void drawGenerationBands(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fillRect(0, 0, 800, 180);
        g2.setColor(new Color(255, 255, 255, 20));
        g2.fillRect(0, 180, 800, 200);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRect(0, 380, 800, 220);

        g2.setColor(new Color(255, 255, 255, 100));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.drawString("YOUNG GEN (EDEN)", 630, 15);
        g2.drawString("SURVIVOR SPACE", 630, 195);
        g2.drawString("OLD GEN (TENURED)", 630, 395);

        g2.setStroke(new BasicStroke(1));
        g2.drawLine(0, 180, 800, 180);
        g2.drawLine(0, 380, 800, 380);
    }

    private void drawRuntimePanel(Graphics2D g2) {
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;

        int panelX = 20, panelY = 515, panelW = 300, panelH = 70;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 10, 10);
        g2.setColor(new Color(255, 255, 255, 200));
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 10, 10);

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.drawString("REAL JVM HEAP (Runtime.getRuntime)", panelX + 10, panelY + 18);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.drawString(String.format("USED %d MB / MAX %d MB", used / (1024 * 1024), max / (1024 * 1024)),
                panelX + 10, panelY + 36);

        int barX = panelX + 10, barY = panelY + 46, barW = panelW - 20, barH = 12;
        double pct = max > 0 ? (double) used / max : 0;
        g2.setColor(Color.GRAY);
        g2.fillRect(barX, barY, barW, barH);
        g2.setColor(pct > 0.8 ? Color.RED : (pct > 0.5 ? Color.ORANGE : Color.GREEN));
        g2.fillRect(barX, barY, (int) (barW * Math.min(1.0, pct)), barH);
        g2.setColor(Color.WHITE);
        g2.drawRect(barX, barY, barW, barH);
    }

    private void drawPlay(Graphics2D g2) {
        drawGenerationBands(g2);

        int s = score.get();
        int h = health.get();
        int c = combo.get();

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2.drawString("SCORE: " + s, 20, 35);
        g2.drawString("COMBO: x" + c, 20, 60);
        g2.drawString("LEVEL: " + level, 20, 85);
        if (jitMode) {
            g2.setColor(Color.YELLOW);
            g2.drawString("JIT OPTIMIZATION ACTIVE!", 280, 35);
        }
        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(580, 60, 200, 15, 10, 10);
        g2.setColor(h > 30 ? Color.GREEN : Color.RED);
        g2.fillRoundRect(580, 60, Math.max(0, h * 2), 15, 10, 10);

        if (lassoStart != null && lassoEnd != null) {
            g2.setColor(new Color(0, 255, 255, 80));
            g2.fillRect(Math.min(lassoStart.x, lassoEnd.x), Math.min(lassoStart.y, lassoEnd.y),
                        Math.abs(lassoStart.x - lassoEnd.x), Math.abs(lassoStart.y - lassoEnd.y));
        }

        for (MemoryObject obj : objects) obj.draw(g2);
        if (boss != null) boss.draw(g2);
        for (Particle p : particles) p.draw(g2);

        drawButton(g2, pauseBtn, currentState == State.PAUSED ? "RESUME" : "PAUSE", Color.ORANGE);
        drawRuntimePanel(g2);

        if (currentState == State.PAUSED) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, 800, 600);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Verdana", Font.BOLD, 50));
            FontMetrics fm = g2.getFontMetrics();
            String txt = "PAUSED";
            g2.drawString(txt, (800 - fm.stringWidth(txt)) / 2, 300);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
            g2.drawString("Press ESC, P, or click PAUSE to resume", 260, 340);
        }
    }

    private void drawGameOver(Graphics2D g2) {
        g2.setColor(Color.RED);
        g2.setFont(new Font("Verdana", Font.BOLD, 50));
        g2.drawString("CRITICAL HEAP LEAK", 130, 250);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g2.drawString("Score: " + score.get() + " | Best: " + highScore, 280, 310);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2.drawString("Press R or click to return to menu", 300, 350);
    }

    private void drawButton(Graphics2D g2, Rectangle r, String text, Color c) {
        g2.setColor(c);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 15, 15);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, r.x + (r.width - fm.stringWidth(text)) / 2, r.y + 30);
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("JVM Architect v5.0");
        GameWindow gw = new GameWindow();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(gw);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        gw.requestFocusInWindow();
    }
}
