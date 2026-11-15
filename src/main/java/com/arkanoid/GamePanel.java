package com.arcanoid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;

public class GamePanel extends JPanel implements ActionListener {
    private static final int PANEL_WIDTH = 900;
    private static final int PANEL_HEIGHT = 700;
    private static final int DELAY = 14; // ~70 FPS

    // Платформа — чёткое, прямое управление
    private static final int PADDLE_WIDTH = 130;
    private static final int PADDLE_HEIGHT = 18;
    private static final int PADDLE_SPEED = 8;
    private int paddleX, paddleY;
    private boolean leftPressed, rightPressed;

    // Мяч
    private int ballX, ballY;
    private int ballSize = 14;
    private int ballDX, ballDY;
    private double speedMultiplier = 1.0;
    // Кирпичи
    private static final int BRICK_ROWS = 6;
    private static final int BRICK_COLS = 12;
    private static final int BRICK_WIDTH = 65;
    private static final int BRICK_HEIGHT = 22;
    private static final int BRICK_PADDING = 4;
    private static final int BRICK_OFFSET_TOP = 70;
    private static final int BRICK_OFFSET_LEFT = 25;

    private Brick[] bricks;
    private int score;
    private int highScore;
    private int lives = 3;
    private int level = 1;
    private boolean gameRunning = true;
    private boolean gameOver = false;
    private boolean paused = false;

    private javax.swing.Timer timer;
    private final Random random = new Random();

    // Частицы
    private final java.util.List<Particle> particles = new ArrayList<>();
    private final java.util.List<Spark> sparks = new ArrayList<>();

    // Неоновая палитра кирпичей
    private static final Color[] BRICK_COLORS = {
        new Color(255, 50, 80),
        new Color(255, 120, 40),
        new Color(255, 220, 30),
        new Color(60, 230, 80),
        new Color(40, 200, 220),
        new Color(140, 80, 255),
    };

    // Звёзды фона
    private Star[] stars;

    public GamePanel() {
        this.setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        this.setBackground(Color.decode("#0a0a1a"));
        this.setFocusable(true);

        initStars();
        initGame();
        addKeyListeners();

        timer = new javax.swing.Timer(DELAY, this);
        timer.start();
    }

    private void initStars() {
        stars = new Star[80];
        for (int i = 0; i < stars.length; i++) {
            stars[i] = new Star(
                random.nextInt(PANEL_WIDTH),
                random.nextInt(PANEL_HEIGHT),
                random.nextDouble() * 2 + 0.5
            );
        }
    }

    private void initGame() {
        paddleX = (PANEL_WIDTH - PADDLE_WIDTH) / 2;
        paddleY = PANEL_HEIGHT - PADDLE_HEIGHT - 15;

        ballX = PANEL_WIDTH / 2 - ballSize / 2;
        ballY = PANEL_HEIGHT / 3;
        float baseSpeed = 5f;
        ballDX = Math.round(baseSpeed * (random.nextBoolean() ? 1 : -1));
        ballDY = -Math.round(baseSpeed);

        score = 0;
        lives = 3;
        level = 1;
        speedMultiplier = 1.0;
        gameOver = false;
        paused = false;
        gameRunning = true;
        particles.clear();
        sparks.clear();

        createBricks();
    }

    private void createBricks() {
        int total = BRICK_ROWS * BRICK_COLS;
        bricks = new Brick[total];
        int idx = 0;
        for (int row = 0; row < BRICK_ROWS; row++) {
            for (int col = 0; col < BRICK_COLS; col++) {
                int brickLives = (row < 2 && level >= 2) ? 2 : 1;
                bricks[idx++] = new Brick(
                    BRICK_OFFSET_LEFT + col * (BRICK_WIDTH + BRICK_PADDING),
                    BRICK_OFFSET_TOP + row * (BRICK_HEIGHT + BRICK_PADDING),
                    BRICK_WIDTH,
                    BRICK_HEIGHT,
                    BRICK_COLORS[row % BRICK_COLORS.length],
                    brickLives
                );
            }
        }
    }

    private void addKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        leftPressed = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                        rightPressed = true;
                        break;
                    case KeyEvent.VK_SPACE:
                        if (gameOver || !gameRunning) {
                            restartGame();
                        } else if (paused) {
                            paused = false;
                        }
                        break;
                    case KeyEvent.VK_P:
                    case KeyEvent.VK_ESCAPE:
                        if (!gameOver && gameRunning) {
                            paused = !paused;
                        }
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        leftPressed = false;
                        break;
                    case KeyEvent.VK_RIGHT:
                        rightPressed = false;
                        break;
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameRunning && !gameOver && !paused) {
            movePaddle();
            moveBall();
            checkBrickCollision();
            updateParticles();
            checkWinCondition();
        }
        repaint();
    }

    // Прямое управление платформой — без инерции
    private void movePaddle() {
        if (leftPressed) {
            paddleX = Math.max(0, paddleX - PADDLE_SPEED);
        }
        if (rightPressed) {
            paddleX = Math.min(PANEL_WIDTH - PADDLE_WIDTH, paddleX + PADDLE_SPEED);
        }
    }

    private void moveBall() {
        ballX += ballDX;
        ballY += ballDY;

        // Стены
        if (ballX <= 0) {
            ballX = 0;
            ballDX = Math.abs(ballDX);
            spawnWallParticles(ballX, ballY + ballSize / 2, Color.CYAN);
        }
        if (ballX + ballSize >= PANEL_WIDTH) {
            ballX = PANEL_WIDTH - ballSize;
            ballDX = -Math.abs(ballDX);
            spawnWallParticles(ballX + ballSize, ballY + ballSize / 2, Color.CYAN);
        }
        if (ballY <= 0) {
            ballY = 0;
            ballDY = Math.abs(ballDY);
            spawnWallParticles(ballX + ballSize / 2, ballY, Color.CYAN);
        }

        // Платформа
        if (ballDY > 0 &&
            ballY + ballSize >= paddleY &&
            ballY + ballSize <= paddleY + PADDLE_HEIGHT + 6 &&
            ballX + ballSize >= paddleX &&
            ballX <= paddleX + PADDLE_WIDTH) {

            ballDY = -Math.abs(ballDY);

            double hitPoint = (ballX + ballSize / 2.0) - (paddleX + PADDLE_WIDTH / 2.0);
            double normalizedHit = hitPoint / (PADDLE_WIDTH / 2.0);
            double angle = normalizedHit * (Math.PI / 3);

            double currentSpeed = Math.sqrt(ballDX * ballDX + ballDY * ballDY) * speedMultiplier;
            ballDX = Math.max(-2, Math.min(2, (int) Math.round(Math.sin(angle) * currentSpeed)));
            ballDY = -(int) (currentSpeed * Math.cos(angle));

            if (ballDX == 0) ballDX = 1;

            spawnPaddleSparks(ballX + ballSize / 2, paddleY);
        }

        trail.addPoint(ballX + ballSize / 2, ballY + ballSize / 2);

        // Мяч упал
        if (ballY > PANEL_HEIGHT) {
            lives--;
            spawnExplosion(ballX + ballSize / 2, PANEL_HEIGHT, Color.ORANGE);
            if (lives <= 0) {
                gameOver = true;
                gameRunning = false;
            } else {
                ballX = PANEL_WIDTH / 2 - ballSize / 2;
                ballY = PANEL_HEIGHT / 3;
                float baseSpeed = 5f * (float) speedMultiplier;
                ballDX = Math.round(baseSpeed * (random.nextBoolean() ? 1 : -1));
                ballDY = -Math.round(baseSpeed);
                trail.clear();
            }
        }
    }

    private void checkBrickCollision() {
        for (int i = 0; i < bricks.length; i++) {
            Brick brick = bricks[i];
            if (brick == null || !brick.alive) continue;

            if (ballX + ballSize > brick.x &&
                ballX < brick.x + brick.width &&
                ballY + ballSize > brick.y &&
                ballY < brick.y + brick.height) {

                ballDY = -ballDY;
                brick.lives--;

                if (brick.lives <= 0) {
                    brick.alive = false;
                    score += 10 * level;
                    spawnExplosion(
                        brick.x + brick.width / 2,
                        brick.y + brick.height / 2,
                        brick.color
                    );
                } else {
                    score += 5 * level;
                    spawnSmallImpact(brick.x + brick.width / 2, brick.y + brick.height / 2, brick.color);
                }
                return;
            }
        }
    }

    private void checkWinCondition() {
        boolean anyAlive = false;
        for (Brick b : bricks) {
            if (b != null && b.alive) {
                anyAlive = true;
                break;
            }
        }
        if (!anyAlive) {
            level++;
            speedMultiplier += 0.15;
            score += 100 * level;
            createBricks();
            ballX = PANEL_WIDTH / 2 - ballSize / 2;
            ballY = PANEL_HEIGHT / 3;
            float baseSpeed = 5f * (float) speedMultiplier;
            ballDX = Math.round(baseSpeed * (random.nextBoolean() ? 1 : -1));
            ballDY = -Math.round(baseSpeed);
            trail.clear();
        }
    }

    // --- Частицы ---

    private void spawnExplosion(int x, int y, Color color) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void spawnSmallImpact(int x, int y, Color color) {
        for (int i = 0; i < 6; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void spawnWallParticles(int x, int y, Color color) {
        for (int i = 0; i < 4; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void spawnPaddleSparks(int x, int y) {
        for (int i = 0; i < 8; i++) {
            sparks.add(new Spark(x, y));
        }
    }

    private void updateParticles() {
        particles.removeIf(p -> !p.update());
        sparks.removeIf(s -> !s.update());
    }

    // --- Рисование ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g2d);
        drawBricks(g2d);
        drawTrail(g2d);
        drawPaddle(g2d);
        drawBall(g2d);
        drawParticles(g2d);
        drawHUD(g2d);

        if (gameOver) drawGameOver(g2d);
        else if (!gameRunning) drawWinScreen(g2d);
        else if (paused) drawPaused(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        GradientPaint bg = new GradientPaint(
            0, 0, Color.decode("#0a0a1a"),
            0, PANEL_HEIGHT, Color.decode("#151530")
        );
        g2d.setPaint(bg);
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.WHITE);
        long tick = System.currentTimeMillis() / 1000;
        for (Star star : stars) {
            double alpha = 0.3 + 0.7 * Math.abs(Math.sin(tick * 0.5 + star.x * 0.01));
            g2d.setColor(new Color(255, 255, 255, (int) (alpha * 255)));
            g2d.fillOval(star.x, star.y, (int) star.size, (int) star.size);
        }
    }

    private void drawBricks(Graphics2D g2d) {
        for (Brick brick : bricks) {
            if (brick == null || !brick.alive) continue;

            // Свечение
            g2d.setColor(new Color(brick.color.getRed(), brick.color.getGreen(),
                brick.color.getBlue(), 40));
            g2d.fillRoundRect(brick.x - 2, brick.y - 2,
                brick.width + 4, brick.height + 4, 6, 6);

            // Основной блок с градиентом
            GradientPaint gp = new GradientPaint(
                brick.x, brick.y, brick.color.brighter(),
                brick.x, brick.y + brick.height, brick.color
            );
            g2d.setPaint(gp);
            g2d.fillRoundRect(brick.x, brick.y, brick.width, brick.height, 5, 5);

            // Блик
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillRoundRect(brick.x + 3, brick.y + 2, brick.width - 6, brick.height / 3, 3, 3);

            // Рамка
            g2d.setColor(brick.color.darker());
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(brick.x, brick.y, brick.width, brick.height, 5, 5);

            // Индикатор HP
            if (brick.lives > 1) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                String hpText = String.valueOf(brick.lives);
                FontMetrics fm = g2d.getFontMetrics();
                int tx = brick.x + (brick.width - fm.stringWidth(hpText)) / 2;
                int ty = brick.y + brick.height / 2 + fm.getAscent() / 2 - 2;
                g2d.drawString(hpText, tx, ty);
            }
        }
    }

    private void drawPaddle(Graphics2D g2d) {
        // Свечение под платформой
        g2d.setColor(new Color(0, 200, 255, 50));
        Ellipse2D glow = new Ellipse2D.Double(
            paddleX + PADDLE_WIDTH / 2 - 40,
            paddleY + PADDLE_HEIGHT,
            80, 15
        );
        g2d.fill(glow);

        // Основная платформа
        GradientPaint gradient = new GradientPaint(
            paddleX, paddleY, Color.decode("#00e5ff"),
            paddleX + PADDLE_WIDTH, paddleY, Color.decode("#006aff")
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(paddleX, paddleY, PADDLE_WIDTH, PADDLE_HEIGHT, 12, 12);

        // Верхний блик
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillRoundRect(paddleX + 8, paddleY + 2, PADDLE_WIDTH - 16, 5, 3, 3);
    }

    private void drawBall(Graphics2D g2d) {
        // Внешнее свечение мяча
        RadialGradientPaint outerGlow = new RadialGradientPaint(
            ballX + ballSize / 2f, ballY + ballSize / 2f, ballSize * 2,
            new float[]{0f, 0.6f, 1f},
            new Color[]{new Color(255, 255, 255, 60), new Color(255, 200, 50, 20), new Color(255, 200, 50, 0)}
        );
        g2d.setPaint(outerGlow);
        g2d.fillOval(ballX - ballSize, ballY - ballSize, ballSize * 3, ballSize * 3);

        // Сам мяч
        RadialGradientPaint ballGrad = new RadialGradientPaint(
            ballX + ballSize / 2f - 2, ballY + ballSize / 2f - 2, ballSize * 0.8f,
            new float[]{0f, 1f},
            new Color[]{Color.WHITE, Color.YELLOW}
        );
        g2d.setPaint(ballGrad);
        g2d.fillOval(ballX, ballY, ballSize, ballSize);

        // Блик
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillOval(ballX + 3, ballY + 3, 4, 4);
    }

    private void drawTrail(Graphics2D g2d) {
        if (trail.isEmpty()) return;
        java.util.List<Point> points = trail.getPoints();
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            float alpha = (float) i / points.size() * 150;
            float size = (float) i / points.size() * ballSize;
            g2d.setColor(new Color(255, 200, 50, (int) alpha));
            g2d.fillOval(p.x - (int) size / 2, p.y - (int) size / 2, (int) size, (int) size);
        }
    }

    private void drawParticles(Graphics2D g2d) {
        for (Particle p : particles) {
            g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(),
                p.color.getBlue(), (int) (p.alpha * 255)));
            g2d.fillOval(p.x, p.y, p.size, p.size);
        }
        for (Spark s : sparks) {
            g2d.setColor(new Color(255, 255, 200, (int) (s.alpha * 255)));
            g2d.fillOval(s.x, s.y, s.size, s.size);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));

        g2d.drawString("SCORE: " + score, 15, 28);
        g2d.drawString("LEVEL: " + level, PANEL_WIDTH / 2 - 40, 28);
        g2d.drawString("BEST: " + highScore, PANEL_WIDTH - 130, 28);

        g2d.drawString("LIVES:", PANEL_WIDTH - 250, 28);
        for (int i = 0; i < lives; i++) {
            g2d.setColor(Color.RED);
            g2d.fillOval(PANEL_WIDTH - 165 + i * 22, 14, 14, 14);
            g2d.setColor(new Color(255, 150, 150, 150));
            g2d.fillOval(PANEL_WIDTH - 163 + i * 22, 16, 5, 5);
        }

        double currentSpeed = Math.sqrt(ballDX * ballDX + ballDY * ballDY);
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2d.drawString(String.format("SPEED: %.1f", currentSpeed), 15, 48);
    }

    private void drawGameOver(Graphics2D g2d) {
        if (score > highScore) highScore = score;
        drawOverlay(g2d, "GAME OVER", Color.RED);
    }

    private void drawWinScreen(Graphics2D g2d) {
        drawOverlay(g2d, "LEVEL " + level + " COMPLETE!", Color.GREEN);
    }

    private void drawPaused(Graphics2D g2d) {
        drawOverlay(g2d, "PAUSED", new Color(255, 200, 50));
    }

    private void drawOverlay(Graphics2D g2d, String title, Color color) {
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 52));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (PANEL_WIDTH - fm.stringWidth(title)) / 2 + 3;
        int y = PANEL_HEIGHT / 2 - 25 + 3;
        g2d.drawString(title, x, y);

        g2d.setColor(color);
        g2d.drawString(title, x - 3, y - 3);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        String scoreText = "Score: " + score;
        fm = g2d.getFontMetrics();
        x = (PANEL_WIDTH - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, x, y + 55);

        if (highScore > 0) {
            String hsText = "Best: " + highScore;
            fm = g2d.getFontMetrics();
            x = (PANEL_WIDTH - fm.stringWidth(hsText)) / 2;
            g2d.drawString(hsText, x, y + 85);
        }

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g2d.setColor(new Color(255, 255, 255, 180));
        String hint = "Press SPACE to restart";
        fm = g2d.getFontMetrics();
        x = (PANEL_WIDTH - fm.stringWidth(hint)) / 2;
        g2d.drawString(hint, x, y + 130);
    }

    private void restartGame() {
        gameOver = false;
        gameRunning = true;
        paused = false;
        initGame();
    }

    // --- Внутренние классы ---

    private static class Star {
        int x, y;
        double size;
        Star(int x, int y, double size) { this.x = x; this.y = y; this.size = size; }
    }

    private static class Brick {
        int x, y, width, height;
        Color color;
        boolean alive;
        int lives;
        Brick(int x, int y, int w, int h, Color c, int l) {
            this.x = x; this.y = y; this.width = w; this.height = h;
            this.color = c; this.lives = l; this.alive = true;
        }
    }

    private static class Particle {
        int x, y, dx, dy, size;
        Color color;
        float alpha = 1.0f;
        Particle(int x, int y, Color color) {
            this.x = x; this.y = y; this.color = color;
            this.dx = (new Random().nextInt(6) - 3);
            this.dy = (new Random().nextInt(6) - 3);
            this.size = new Random().nextInt(4) + 2;
        }
        boolean update() {
            x += dx;
            y += dy;
            dy += 0.1;
            alpha -= 0.025f;
            size = Math.max(1, size - 1);
            return alpha > 0;
        }
    }

    private static class Spark {
        int x, y, dx, dy, size;
        float alpha = 1.0f;
        Spark(int x, int y) {
            this.x = x; this.y = y;
            this.dx = (new Random().nextInt(10) - 5);
            this.dy = -new Random().nextInt(6) - 2;
            this.size = new Random().nextInt(3) + 1;
        }
        boolean update() {
            x += dx;
            y += dy;
            dy += 0.15;
            alpha -= 0.04f;
            return alpha > 0;
        }
    }

    private static class Trail {
        private final LinkedList<Point> points = new LinkedList<>();
        private static final int MAX = 12;
        void addPoint(int x, int y) {
            points.addLast(new Point(x, y));
            while (points.size() > MAX) points.removeFirst();
        }
        java.util.List<Point> getPoints() { return Collections.unmodifiableList(points); }
        boolean isEmpty() { return points.isEmpty(); }
        void clear() { points.clear(); }
    }

    private final Trail trail = new Trail();
}
