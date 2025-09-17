package com.codebrawl.net;

import com.codebrawl.realtime.World;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class RtServer {
    private final int port;
    private final World world = new World();
    private final com.codebrawl.auth.AuthManager auth;
    private final ConcurrentHashMap<String, ClientSession> userSessions = new ConcurrentHashMap<>();

    public RtServer(int port) {
        this.port = port;
        this.auth = new com.codebrawl.auth.AuthManager();
        System.out.println("[Server] CWD=" + System.getProperty("user.dir"));
    }

    public boolean reserveUser(String username, ClientSession sess) {
        return userSessions.putIfAbsent(username, sess) == null;
    }

    public void releaseUser(String username) {
        if (username != null) userSessions.remove(username);
    }

    public void start() throws Exception {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("[Server] Listening on " + port);

            Thread acceptor = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Socket s = ss.accept();
                        ClientSession cs = new ClientSession(s, world, this, auth);
                        world.addSession(cs);
                        new Thread(cs, "client-" + s.getPort()).start();
                    }
                } catch (Exception e) {
                    System.out.println("[Server] accept loop terminated: " + e);
                }
            }, "acceptor");
            acceptor.setDaemon(true);
            acceptor.start();

            long last = System.nanoTime();
            while (true) {
                long now = System.nanoTime();
                double dt = (now - last) / 1e9;
                last = now;
                world.tick(Math.min(dt, 0.05));
                Thread.sleep(16);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 12345;
        if (args.length > 0) try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        new RtServer(port).start();
    }
}
