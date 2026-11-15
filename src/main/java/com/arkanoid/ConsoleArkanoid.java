package com.arcanoid;

import java.awt.*;
import java.util.*;
import java.io.IOException;

/**
 * Консольный Арканоид — запускается без GUI.
 * Управление: A/D или стрелки, P — пауза, R — рестарт, Q — выход.
 */
public class ConsoleArkanoid {
    private static final int WIDTH = 60;
    private static final int HEIGHT = 25;

    private char[][] screen;
    private Random random = new Random();

    // Платформа
    private int paddleX;
    private static final int PADDLE_WIDTH = 10;

    // Мяч
    private int ballX, ballY;
    private int ballDX = 1, ballDY = -1;

    // Кирпичи
    private static final int ROWS = 5;
    private static final int COLS = 12;
    private static final int BRICK_W = 4;
    private static final int BRICK_H = 1;
    private static final int OFFSET_X = 3;
    private static final int OFFSET_Y = 2;
    private Brick[] bricks;

    // Состояние
    private int score = 0;
    private int lives = 3;
    private int level = 1;
    private boolean running = true;
    private boolean paused = false;
    private boolean gameOver = false;

    public static void main(String[] args) throws IOException {
        ConsoleArkanoid game = new ConsoleArkanoid();
        game.run();
    }

    private void run() throws IOException {
        initGame();
        hideCursor();
        printInstructions();
        Thread inputThread = new Thread(() -> { try { readInput(); } catch (IOException e) {} });
        inputThread.setDaemon(true);
        inputThread.start();

        while (running) {
            if (!paused && !gameOver) {
                update();
            }
            render();
            // Очистка экрана для обновления кадра
            System.out.print("\033[H\033[2J");
            sleep(80);
        }
        showCursor();
    }

    private void initGame() {
        paddleX = WIDTH / 2 - PADDLE_WIDTH / 2;
        ballX = WIDTH / 2;
        ballY = HEIGHT - 4;
        ballDX = random.nextBoolean() ? 1 : -1;
        ballDY = -1;
        score = 0;
        lives = 3;
        level = 1;
        gameOver = false;
        createBricks();
    }

    private void createBricks() {
        int total = ROWS * COLS;
        bricks = new Brick[total];
        int idx = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int hp = (row < 2 && level >= 2) ? 2 : 1;
                Color c = getBrickColor(row);
                bricks[idx++] = new Brick(OFFSET_X + col * (BRICK_W + 1),
                    OFFSET_Y + row * (BRICK_H + 1), BRICK_W, BRICK_H, c, hp);
            }
        }
    }

    private Color getBrickColor(int row) {
        return switch (row % 6) {
            case 0 -> new Color(255, 50, 80);
            case 1 -> new Color(255, 120, 40);
            case 2 -> new Color(255, 220, 30);
            case 3 -> new Color(60, 230, 80);
            case 4 -> new Color(40, 200, 220);
            default -> new Color(140, 80, 255);
        };
    }

    private void update() {
        movePaddle();
        moveBall();
        checkBrickCollision();
        checkWinCondition();
    }

    private void movePaddle() {
        if (leftPressed) {
            paddleX = Math.max(0, paddleX - 2);
        }
        if (rightPressed) {
            paddleX = Math.min(WIDTH - PADDLE_WIDTH, paddleX + 2);
        }
    }

    private boolean leftPressed, rightPressed;

    private void moveBall() {
        ballX += ballDX;
        ballY += ballDY;

        // Стены
        if (ballX <= 0 || ballX >= WIDTH - 1) ballDX = -ballDX;
        if (ballY <= 0) ballDY = -ballDY;

        // Платформа
        if (ballDY > 0 && ballY >= HEIGHT - 3 && ballY <= HEIGHT - 2
            && ballX >= paddleX && ballX < paddleX + PADDLE_WIDTH) {
            ballDY = -1;
            double hit = (ballX - (paddleX + PADDLE_WIDTH / 2.0)) / (PADDLE_WIDTH / 2.0);
            ballDX = Math.max(-2, Math.min(2, (int) Math.round(hit * 2)));
            if (ballDX == 0) ballDX = 1;
        }

        // Упал
        if (ballY > HEIGHT) {
            lives--;
            if (lives <= 0) {
                gameOver = true;
                running = false;
            } else {
                ballX = WIDTH / 2;
                ballY = HEIGHT - 4;
                ballDX = random.nextBoolean() ? 1 : -1;
                ballDY = -1;
            }
        }
    }

    private void checkBrickCollision() {
        for (Brick b : bricks) {
            if (b == null || !b.alive) continue;
            if (ballX >= b.x && ballX < b.x + b.w && ballY >= b.y && ballY < b.y + b.h) {
                ballDY = -ballDY;
                b.hp--;
                if (b.hp <= 0) {
                    b.alive = false;
                    score += 10 * level;
                } else {
                    score += 5 * level;
                }
                return;
            }
        }
    }

    private void checkWinCondition() {
        boolean alive = false;
        for (Brick b : bricks) if (b != null && b.alive) { alive = true; break; }
        if (!alive) {
            level++;
            score += 100 * level;
            createBricks();
            ballX = WIDTH / 2;
            ballY = HEIGHT - 4;
            ballDX = random.nextBoolean() ? 1 : -1;
            ballDY = -1;
        }
    }

    private void render() {
        StringBuilder sb = new StringBuilder();

        // Заголовок
        sb.append("┌").append("─".repeat(WIDTH)).append("┐\n");
        sb.append("│ ").append(pad("SCORE: " + score, 18));
        sb.append(" LEVEL: " + level);
        sb.append(" BEST: " + highScore).append(" │\n");
        sb.append("│ ").append(pad("LIVES: ", 9));
        for (int i = 0; i < lives; i++) sb.append("♥ ");
        for (int i = lives; i < 3; i++) sb.append("♡ ");
        sb.append(" │\n");
        sb.append("├").append("─".repeat(WIDTH)).append("┤\n");

        // Игровое поле
        screen = new char[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++)
            Arrays.fill(screen[y], ' ');

        // Кирпичи
        for (Brick b : bricks) {
            if (b == null || !b.alive) continue;
            for (int dx = 0; dx < b.w; dx++) {
                for (int dy = 0; dy < b.h; dy++) {
                    int cx = b.x + dx, cy = b.y + dy;
                    if (cx >= 0 && cx < WIDTH && cy >= 0 && cy < HEIGHT) {
                        char ch = b.hp > 1 ? '#' : '█';
                        screen[cy][cx] = ch;
                    }
                }
            }
        }

        // Платформа
        int py = HEIGHT - 3;
        for (int i = 0; i < PADDLE_WIDTH; i++) {
            int px = paddleX + i;
            if (px >= 0 && px < WIDTH && py >= 0 && py < HEIGHT) {
                screen[py][px] = '=';
            }
        }

        // Мяч
        if (ballX >= 0 && ballX < WIDTH && ballY >= 0 && ballY < HEIGHT) {
            screen[ballY][ballX] = '●';
        }

        // Вывод поля
        for (int y = 0; y < HEIGHT; y++) {
            sb.append("│");
            for (int x = 0; x < WIDTH; x++) {
                sb.append(screen[y][x]);
            }
            sb.append("│\n");
        }

        sb.append("├").append("─".repeat(WIDTH)).append("┤\n");
        sb.append("│ ").append(pad("Controls: A/D=Paddle  P=Pause  R=Restart  Q=Quit", 50)).append(" │\n");
        sb.append("└").append("─".repeat(WIDTH)).append("┘\n");

        if (gameOver) {
            sb.append("\n");
            sb.append("  ╔══════════════════════════╗\n");
            sb.append("  ║       GAME OVER!         ║\n");
            sb.append("  ║   Final Score: ").append(pad(String.valueOf(score), 14)).append("║\n");
            if (highScore > 0) {
                sb.append("  ║   Best Score:  ").append(pad(String.valueOf(highScore), 14)).append("║\n");
            }
            sb.append("  ╚══════════════════════════╝\n\n");
            sb.append("  Press R to restart or Q to quit\n\n");
        } else if (paused) {
            sb.append("\n  *** PAUSED - Press P to resume ***\n\n");
        }

        System.out.print(sb.toString());
    }

    private String pad(String s, int width) {
        if (width <= 0) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }

    private void readInput() throws IOException {
        disableEcho();
        byte[] buf = new byte[1024];
        while (running || gameOver) {
            int n = System.in.read(buf);
            if (n <= 0) continue;
            handleInput(buf[0]);
        }
        enableEcho();
    }

    private void handleInput(byte key) {
        char c = (char) key;
        switch (c) {
            case 'a', 'A' -> leftPressed = true;
            case 'd', 'D' -> rightPressed = true;
            case 'p', 'P' -> {
                if (!gameOver) paused = !paused;
            }
            case 'r', 'R' -> {
                gameOver = false;
                initGame();
            }
            case 'q', 'Q' -> running = false;
        }
    }

    // --- Terminal helpers ---

    private static volatile int highScore = 0;

    private static void hideCursor() {
        System.out.print("\033[?25l");
    }

    private static void showCursor() {
        System.out.print("\033[?25h");
    }

    private static void disableEcho() {
        try {
            Process p = Runtime.getRuntime().exec("stty -echo");
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private static void enableEcho() {
        try {
            Process p = Runtime.getRuntime().exec("stty echo");
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static void printInstructions() {
        System.out.println("\n  ╔═══════════════════════════════════════╗");
        System.out.println("  ║         ARKANOID - CONSOLE EDITION     ║");
        System.out.println("  ╠═══════════════════════════════════════╣");
        System.out.println("  ║  A / ←  : Move left                   ║");
        System.out.println("  ║  D / →  : Move right                  ║");
        System.out.println("  ║  P      : Pause / Resume              ║");
        System.out.println("  ║  R      : Restart                     ║");
        System.out.println("  ║  Q      : Quit                        ║");
        System.out.println("  ╚═══════════════════════════════════════╝\n");
    }

    private static class Brick {
        int x, y, w, h;
        Color color;
        boolean alive;
        int hp;
        Brick(int x, int y, int w, int h, Color c, int hp) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.color = c; this.hp = hp; this.alive = true;
        }
    }
}
