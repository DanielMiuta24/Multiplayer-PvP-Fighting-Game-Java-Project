package com.codebrawl.net;

import java.io.*; import java.net.Socket; import java.util.function.Consumer;

public class RtClient implements AutoCloseable {
    private Socket sock; private BufferedReader in; private PrintWriter out;
    public void connect(String host, int port) throws IOException {
        sock = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true);
    }
    public void send(String line){ out.println(line); }
    public void startReader(Consumer<String> onLine){
        Thread t = new Thread(() -> {
            try { String line; while((line=in.readLine())!=null) onLine.accept(line); }
            catch(IOException ignored) {}
        }, "ClientReader"); t.setDaemon(true); t.start();
    }
    @Override public void close() throws IOException { if(sock!=null) sock.close(); }
}
