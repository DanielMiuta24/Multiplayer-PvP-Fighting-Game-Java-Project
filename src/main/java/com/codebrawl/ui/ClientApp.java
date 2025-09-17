package com.codebrawl.ui;

import com.codebrawl.net.RtClient;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class ClientApp extends Application {

    private RtClient net;
    private long inputSeq = 0;
    private boolean up, down, left, right, guard;
    private boolean atk1, atk2, atk3, prevAtk1;
    private boolean jump;

    private static class Player {
        String id, name, clazz;
        int x, y, hp, mp;
        boolean facingRight, guarding;
    }
    private final Map<String, Player> players = new HashMap<>();
    private String myId = null;

    private Stage stage;
    private Scene loginScene, selectScene, gameScene;

    private String chosenClass = "samurai";
    private String displayName = "Hero";

    private double camX = 0, camY = 0;

    private Image arenaBG;

    private static final double CHARACTER_SCALE = 2.2;
    private static final long ATTACK1_MS = 240;
    private static final long ATTACK2_MS = 300;
    private static final long ATTACK3_MS = 360;
    private static final long JUMP_MS    = 350;
    private static final long HURT_MS    = 220;

    private static class SpriteAnim {
        private final Image sheet;
        private final int frameW, frameH, frames;
        private double t = 0;

        SpriteAnim(Image sheet) {
            this.sheet = sheet;
            int h = (int)Math.round(sheet.getHeight());
            int w = (int)Math.round(sheet.getWidth());
            int f = Math.max(1, w / Math.max(1, h));
            this.frameH = h;
            this.frameW = h;
            this.frames = f;
        }

        void draw(GraphicsContext g, double x, double y, double dt, boolean playing, boolean flip) {
            if (playing) t = (t + dt * 10) % frames;
            int fi = (int) t;
            double dw = frameW * CHARACTER_SCALE;
            double dh = frameH * CHARACTER_SCALE;
            double dx = Math.round(x - dw / 2.0);
            double dy = Math.round(y - dh / 2.0);
            if (!flip) {
                g.drawImage(sheet, fi * frameW, 0, frameW, frameH, dx, dy, dw, dh);
            } else {
                g.drawImage(sheet, fi * frameW, 0, frameW, frameH, dx + dw, dy, -dw, dh);
            }
        }

        void reset(){ t = 0; }
    }

    private static class CharacterAnimator {
        SpriteAnim idle, walk, run, jump, hurt, dead, attack1, attack2, attack3, shield;
    }

    private final Map<String, CharacterAnimator> anims = new HashMap<>();
    private final Map<String, int[]> lastPos = new HashMap<>();
    private final Map<String, Integer> lastHp = new HashMap<>();
    private final Map<String, Long> hurtUntil = new HashMap<>();

    private long atk1StartMs = -1, atk2StartMs = -1, atk3StartMs = -1, jumpStartMs = -1;

    @Override public void start(Stage stage) {
        this.stage = stage;
        arenaBG = safeLoadImage("/backgrounds/arena.png", 1920, 1080);
        anims.put("samurai", loadAnimsFor("samurai"));
        anims.put("shinobi", loadAnimsFor("shinobi"));
        anims.put("warrior", loadAnimsFor("warrior"));
        loginScene = buildLoginScene();
        stage.setTitle("Code Brawl – Client");
        stage.setScene(loginScene);
        stage.show();
    }

    private Scene buildLoginScene(){
        TextField host = new TextField("127.0.0.1");
        TextField port = new TextField("12345");
        TextField user = new TextField(); user.setPromptText("username");
        PasswordField pass = new PasswordField(); pass.setPromptText("password");
        Button btnRegister = new Button("Register");
        Button btnLogin    = new Button("Login");
        Label status = new Label();

        btnRegister.setOnAction(e -> {
            try {
                ensureConnected(host.getText(), port.getText());
                if (net != null) net.startReader(this::onServer);
                net.send("REGISTER " + user.getText().trim() + " " + pass.getText().trim());
                status.setText("Registering…");
            } catch (Exception ex) { status.setText("Error: "+ex.getMessage()); }
        });

        btnLogin.setOnAction(e -> {
            try {
                ensureConnected(host.getText(), port.getText());
                if (net != null) net.startReader(this::onServer);
                net.send("LOGIN " + user.getText().trim() + " " + pass.getText().trim());
                status.setText("Logging in…");
            } catch (Exception ex) { status.setText("Error: "+ex.getMessage()); }
        });

        VBox root = new VBox(10,
                new Label("Server host/port:"), host, port,
                new Label("Account:"), user, pass,
                new HBox(10, btnRegister, btnLogin),
                status);
        root.setPadding(new Insets(16)); root.setAlignment(Pos.TOP_LEFT);
        return new Scene(root, 380, 280);
    }


    private void ensureConnected(String host, String port) throws Exception {
        if (net != null) return;
        net = new RtClient();
        net.connect(host.trim(), Integer.parseInt(port.trim()));
    }

    private Scene buildSelectScene(){
        TextField name = new TextField("Hero");
        ChoiceBox<String> clazz = new ChoiceBox<>();
        clazz.getItems().addAll("samurai","shinobi","warrior");
        clazz.getSelectionModel().selectFirst();

        Button start = new Button("Enter Arena");
        start.setOnAction(e -> {
            chosenClass = clazz.getValue();
            displayName = name.getText().trim().replace(" ","_");
            net.send("JOIN " + displayName + " " + chosenClass);
            gameScene = buildGameScene();
            stage.setScene(gameScene);
            Platform.runLater(() -> {
                BorderPane bp = (BorderPane) gameScene.getRoot();
                Canvas cv = (Canvas) bp.getCenter();
                if (cv != null) cv.requestFocus();
            });
        });

        VBox root = new VBox(10,
                new Label("Display name:"), name,
                new Label("Choose class:"), clazz,
                start);
        root.setPadding(new Insets(16));
        return new Scene(root, 360, 220);
    }

    private Scene buildGameScene(){
        Canvas canvas = new Canvas(960, 540);
        BorderPane root = new BorderPane(canvas); root.setPadding(new Insets(6));
        Scene scene = new Scene(root, 980, 600);

        canvas.setFocusTraversable(true);
        scene.setOnMouseClicked(e -> canvas.requestFocus());
        Platform.runLater(canvas::requestFocus);

        scene.setOnKeyPressed(e -> {
            switch(e.getCode()){
                case W -> up=true; case S -> down=true; case A -> left=true; case D -> right=true;
                case SPACE -> atk1=true; case DIGIT1 -> atk2=true; case DIGIT2 -> atk3=true;
                case K -> guard=true; case J -> jump=true;
            }
        });
        scene.setOnKeyReleased(e -> {
            switch(e.getCode()){
                case W -> up=false; case S -> down=false; case A -> left=false; case D -> right=false;
                case SPACE -> atk1=false; case DIGIT1 -> atk2=false; case DIGIT2 -> atk3=false;
                case K -> guard=false; case J -> jump=false;
            }
        });

        new AnimationTimer(){
            private long lastFrame=0, lastSend=0;
            @Override public void handle(long now){
                if(lastFrame==0) lastFrame=now;
                double dt = (now-lastFrame)/1e9; lastFrame=now;

                if (atk1 && !prevAtk1) { atk1StartMs = System.currentTimeMillis(); resetAttackAnim(chosenClass, 1); }
                if (atk2) { atk2StartMs = System.currentTimeMillis(); resetAttackAnim(chosenClass, 2); }
                if (atk3) { atk3StartMs = System.currentTimeMillis(); resetAttackAnim(chosenClass, 3); }
                if (jump) { jumpStartMs = System.currentTimeMillis(); resetJumpAnim(chosenClass); }
                prevAtk1 = atk1;

                if(net!=null && now-lastSend>50_000_000){
                    String line = "INPUT " + (inputSeq++) + " " +
                            b(up)+" "+b(down)+" "+b(left)+" "+b(right)+" "+b(atk1)+" "+b(false)+" "+b(false)+" "+b(guard);
                    net.send(line); lastSend=now;
                }
                render(canvas.getGraphicsContext2D(), dt);
            }
        }.start();

        return scene;
    }

    private void onServer(String line){
        Platform.runLater(() -> {
            if (line.startsWith("REGISTER_OK")) {
                new Alert(Alert.AlertType.INFORMATION, "Registration successful. You can now log in.").show();
                return;
            }
            if (line.startsWith("REGISTER_FAIL")) {
                String reason = line.substring("REGISTER_FAIL".length()).trim();
                if (reason.isEmpty()) reason = "unknown_error";
                new Alert(Alert.AlertType.ERROR, "Registration failed: " + reason).show();
                return;
            }
            if (line.startsWith("AUTH_OK")) {
                selectScene = buildSelectScene();
                stage.setScene(selectScene);
                return;
            }
            if (line.startsWith("AUTH_FAIL")) {
                String reason = line.substring("AUTH_FAIL".length()).trim();
                if (reason.isEmpty()) reason = "unknown_error";
                new Alert(Alert.AlertType.ERROR, "Login failed: " + reason).show();
                return;
            }
            if (line.startsWith("WELCOME ")) {
                myId = line.split("\\s+")[1];
                return;
            }
            if (line.startsWith("SNAPSHOT ")) {
                String[] p = line.split("\\s+"); int count = Integer.parseInt(p[2]);
                Map<String,Integer> newHp = new HashMap<>();
                int idx=3;
                players.clear();
                for(int i=0;i<count;i++){
                    Player pl = new Player();
                    pl.id = p[idx++]; pl.name = p[idx++].replace('_',' ');
                    pl.clazz = p[idx++];
                    pl.x = Integer.parseInt(p[idx++]); pl.y = Integer.parseInt(p[idx++]);
                    pl.hp = Integer.parseInt(p[idx++]); pl.mp = Integer.parseInt(p[idx++]);
                    pl.facingRight = "1".equals(p[idx++]); pl.guarding = "1".equals(p[idx++]);
                    players.put(pl.id, pl);
                    Integer prev = lastHp.get(pl.id);
                    newHp.put(pl.id, pl.hp);
                    if (prev != null && pl.hp < prev && pl.hp > 0) {
                        hurtUntil.put(pl.id, System.currentTimeMillis() + HURT_MS);
                    }
                }
                lastHp.clear(); lastHp.putAll(newHp);
            }
        });
    }



    private void render(GraphicsContext g, double dt){
        g.setFill(Color.BLACK); g.fillRect(0,0,960,540);

        Player me = (myId!=null)? players.get(myId) : null;
        if (me != null) {
            camX = me.x - 960 / 2.0;
            camY = me.y - (540 * 0.6);
        }

        drawBackground(g, arenaBG, 960, 540, camX, camY);

        long nowMs = System.currentTimeMillis();

        for (Player p : players.values()){
            double px = p.x - camX, py = p.y - camY;

            int[] prev = lastPos.getOrDefault(p.id, new int[]{p.x, p.y});
            boolean moving = (prev[0]!=p.x) || (prev[1]!=p.y);
            lastPos.put(p.id, new int[]{p.x, p.y});

            CharacterAnimator ca = anims.getOrDefault(p.clazz, anims.get("warrior"));
            SpriteAnim animToDraw = null;

            boolean isMe = (myId!=null && p.id.equals(myId));
            boolean showAtk1 = isMe && atk1StartMs>0 && (nowMs - atk1StartMs) < ATTACK1_MS;
            boolean showAtk2 = isMe && atk2StartMs>0 && (nowMs - atk2StartMs) < ATTACK2_MS;
            boolean showAtk3 = isMe && atk3StartMs>0 && (nowMs - atk3StartMs) < ATTACK3_MS;
            boolean showJump = isMe && jumpStartMs>0 && (nowMs - jumpStartMs) < JUMP_MS;
            boolean showHurt = hurtUntil.getOrDefault(p.id, 0L) > nowMs;
            boolean isDead   = p.hp <= 0;

            if (isDead && ca != null && ca.dead != null) {
                animToDraw = ca.dead;
            } else if (showHurt && ca != null && ca.hurt != null) {
                animToDraw = ca.hurt;
            } else if (p.guarding && ca != null && ca.shield != null) {
                animToDraw = ca.shield;
            } else if (showAtk3 && ca != null && ca.attack3 != null) {
                animToDraw = ca.attack3;
            } else if (showAtk2 && ca != null && ca.attack2 != null) {
                animToDraw = ca.attack2;
            } else if (showAtk1 && ca != null && ca.attack1 != null) {
                animToDraw = ca.attack1;
            } else if (showJump && ca != null && ca.jump != null) {
                animToDraw = ca.jump;
            } else if (moving && ca != null) {
                animToDraw = (ca.run != null ? ca.run : ca.walk != null ? ca.walk : ca.idle);
            } else if (ca != null) {
                animToDraw = ca.idle;
            }

            boolean flip = (ca != null) && !p.facingRight;
            if (animToDraw != null) animToDraw.draw(g, px, py, dt, true, flip);
            else { g.setFill(Color.DARKRED); g.fillRect(px-12, py-12, 24, 24); }

            if (p.guarding && (ca == null || ca.shield == null)) {
                g.setStroke(Color.GOLD);
                g.setLineWidth(2.0);
                g.strokeOval(px - 28, py - 40, 56, 56);
                g.setLineWidth(1.0);
            }

            g.setFill(Color.WHITE);
            g.fillText(p.name + " ("+p.hp+")", px - 24, py - (20 + 24*CHARACTER_SCALE/2.0));
        }
    }

    private void drawBackground(GraphicsContext g, Image bg, double viewW, double viewH, double camX, double camY) {
        if (bg == null) return;
        double worldW = bg.getWidth(), worldH = bg.getHeight();
        double sx = Math.max(0, Math.min(worldW - viewW, camX));
        double sy = Math.max(0, Math.min(worldH - viewH, camY));
        double sw = Math.min(viewW, worldW - sx);
        double sh = Math.min(viewH, worldH - sy);
        g.drawImage(bg, sx, sy, sw, sh, 0, 0, sw, sh);
        if (sw < viewW) g.drawImage(bg, worldW - 1, sy, 1, sh, sw, 0, viewW - sw, sh);
        if (sh < viewH) g.drawImage(bg, sx, worldH - 1, sw, 1, 0, sh, sw, viewH - sh);
    }

    private static String b(boolean v){ return v? "1":"0"; }

    private Image safeLoadImage(String path, int fallbackW, int fallbackH) {
        try { var in = getClass().getResourceAsStream(path); if (in != null) return new Image(in); }
        catch (Exception ignored) {}
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(fallbackW, fallbackH);
        var g = c.getGraphicsContext2D();
        for (int y=0; y<fallbackH; y+=16) for (int x=0; x<fallbackW; x+=16) {
            g.setFill(((x+y)/16)%2==0 ? Color.DARKSLATEGRAY : Color.GRAY);
            g.fillRect(x,y,16,16);
        }
        return c.snapshot(null, null);
    }

    private void resetAttackAnim(String clazz, int which){
        CharacterAnimator ca = anims.get(clazz);
        if (ca == null) return;
        if (which==1 && ca.attack1!=null) ca.attack1.reset();
        if (which==2 && ca.attack2!=null) ca.attack2.reset();
        if (which==3 && ca.attack3!=null) ca.attack3.reset();
    }

    private void resetJumpAnim(String clazz){
        CharacterAnimator ca = anims.get(clazz);
        if (ca != null && ca.jump != null) ca.jump.reset();
    }

    private CharacterAnimator loadAnimsFor(String clazz) {
        CharacterAnimator ca = new CharacterAnimator();
        ca.idle     = loadAnimOrNull("/sprites/"+clazz+"/Idle.png");
        ca.walk     = loadAnimOrNull("/sprites/"+clazz+"/Walk.png");
        ca.run      = loadAnimOrNull("/sprites/"+clazz+"/Run.png");
        ca.jump     = loadAnimOrNull("/sprites/"+clazz+"/Jump.png");
        ca.hurt     = loadAnimOrNull("/sprites/"+clazz+"/Hurt.png");
        ca.dead     = loadAnimOrNull("/sprites/"+clazz+"/Dead.png");
        ca.attack1  = loadAnimOrNull("/sprites/"+clazz+"/Attack_1.png");
        ca.attack2  = loadAnimOrNull("/sprites/"+clazz+"/Attack_2.png");
        ca.attack3  = loadAnimOrNull("/sprites/"+clazz+"/Attack_3.png");
        ca.shield   = loadAnimOrNull("/sprites/"+clazz+"/Shield.png");
        return ca;
    }

    private SpriteAnim loadAnimOrNull(String path) {
        try {
            var in = ClientApp.class.getResourceAsStream(path);
            if (in == null) return null;
            Image img = new Image(in);
            return new SpriteAnim(img);
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args){ launch(args); }
}
