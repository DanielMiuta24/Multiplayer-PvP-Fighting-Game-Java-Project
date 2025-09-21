package com.codebrawl.net;

import com.codebrawl.auth.AuthManager;
import com.codebrawl.auth.DatabaseManager;
import com.codebrawl.realtime.World;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RtServer {

    private final int port;
    private World world;
    private AuthManager auth;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    public RtServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            System.out.println("[RtServer] boot: creating AuthManager/DatabaseManager");
            auth = new AuthManager(new DatabaseManager());

            System.out.println("[RtServer] boot: creating World");
            world = new World(auth);

            Thread gameLoop = new Thread(() -> {
                long last = System.nanoTime();
                final double targetDt = 1.0 / 60.0;
                final long stepNs = (long)(targetDt * 1_000_000_000L);
                while (running) {
                    long now = System.nanoTime();
                    double dt = (now - last) / 1e9;
                    if (dt < 0) dt = 0;
                    world.tick(Math.min(dt, 0.05));
                    last = now;
                    try { Thread.sleep(Math.max(1, (stepNs - (System.nanoTime() - now)) / 1_000_000)); }
                    catch (InterruptedException ignored) {}
                }
            }, "GameLoop");
            gameLoop.setDaemon(true);
            gameLoop.start();

            System.out.println("[RtServer] Listening on " + port);
            try (ServerSocket server = new ServerSocket(port)) {
                while (running) {
                    Socket sock = server.accept();
                    System.out.println("[RtServer] accepted " + sock.getRemoteSocketAddress());
                    pool.execute(new ClientSession(sock, world, this, auth));
                }
            }
        } catch (Throwable t) {
            System.err.println("[RtServer] FATAL: " + t);
            t.printStackTrace(System.err);
            throw new RuntimeException(t);
        }
    }

    public static void main(String[] args) {
        try {
            int port = (args.length > 0) ? Integer.parseInt(args[0]) : 12345;
            System.out.println("[RtServer] main(): starting on port " + port);
            new RtServer(port).start();
        } catch (Throwable t) {
            System.err.println("[RtServer] MAIN FATAL: " + t);
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
