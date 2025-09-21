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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;

public class ClientApp extends Application {

    private RtClient net;

    private Stage stage;
    private Scene loginScene, charScene, gameScene;

    private boolean up, down, left, right, guard, atk1, prevAtk1, jump;
    private long seq = 0;
    private String myId;
    private String chosenClass = "samurai";
    private String displayName = "Hero";

    private static class Player { String id, name, clazz; int x,y,hp,mp; boolean facingRight, guarding; }
    private final Map<String, Player> players = new HashMap<>();
    private final Map<String, int[]> lastPos = new HashMap<>();
    private final Map<String, Integer> lastHp = new HashMap<>();
    private final Map<String, Long> hurtUntil = new HashMap<>();

    private Image arenaBG;
    private static final double SCALE = 2.0;

    private static class Anim { Image sheet; int fw, fh, frames; double t; }
    private static class CharAnims { Anim idle, walk, run, jump, hurt, dead, attack1, shield; }
    private final Map<String, CharAnims> anims = new HashMap<>();

    private long atk1StartMs = -1, jumpStartMs = -1;
    private static final long ATTACK1_MS = 260;
    private static final long JUMP_MS = 350;
    private static final long HURT_MS = 220;

    private Button respawnBtn;
    private Label killsLabel = new Label("Kills: 0");

    @Override public void start(Stage stage) {
        this.stage = stage;
        arenaBG = safeImg("/backgrounds/arena.png");
        anims.put("samurai", loadChar("samurai"));
        anims.put("shinobi", loadChar("shinobi"));
        anims.put("warrior", loadChar("warrior"));
        loginScene = buildLoginScene();
        stage.setTitle("Code Brawl — Client");
        stage.setScene(loginScene);
        stage.show();
    }

    private Scene buildLoginScene() {
        TextField host = new TextField("127.0.0.1");
        TextField port = new TextField("12345");
        TextField user = new TextField(); user.setPromptText("username");
        PasswordField pass = new PasswordField(); pass.setPromptText("password");
        Button bReg = new Button("Register");
        Button bLog = new Button("Login");
        Label status = new Label();

        bReg.setOnAction(e -> {
            try {
                ensureConnected(host.getText(), port.getText());
                net.send("REGISTER " + user.getText().trim() + " " + pass.getText().trim());
                status.setText("Registering…");
            } catch (Exception ex) { status.setText(ex.getMessage()); }
        });

        bLog.setOnAction(e -> {
            try {
                ensureConnected(host.getText(), port.getText());
                net.startReader(this::onServerLine);
                net.send("LOGIN " + user.getText().trim() + " " + pass.getText().trim());
                status.setText("Logging in…");
            } catch (Exception ex) { status.setText(ex.getMessage()); }
        });

        VBox root = new VBox(10,
                new Label("Server host/port"), host, port,
                new Label("Account"), user, pass,
                new HBox(10, bReg, bLog),
                status
        );
        root.setPadding(new Insets(16));
        Scene s = new Scene(root, 380, 260);
        installGlobalInputFilters(s);
        return s;
    }

    private static final class CharRow { int id; String name, clazz; }

    private Scene buildCharacterScene(List<CharRow> chars) {
        VBox list = new VBox(8); list.setPadding(new Insets(12));
        list.getChildren().add(new Label("Your characters (max 3):"));
        ToggleGroup group = new ToggleGroup();
        for (CharRow r : chars) {
            RadioButton rb = new RadioButton(r.name + "  [" + r.clazz + "]");
            rb.setUserData(r);
            rb.setToggleGroup(group);
            list.getChildren().add(rb);
        }
        if (!group.getToggles().isEmpty()) group.selectToggle(group.getToggles().get(0));

        Button bJoin = new Button("Enter Arena");
        bJoin.setOnAction(e -> {
            Toggle t = group.getSelectedToggle();
            if (t != null) {
                CharRow r = (CharRow) t.getUserData();
                net.send("JOIN_CHAR " + r.id);
                chosenClass = r.clazz;
                displayName = r.name.replace(' ','_');
            }
        });

        TextField name = new TextField(); name.setPromptText("new character name");
        ChoiceBox<String> cls = new ChoiceBox<>();
        cls.getItems().addAll("samurai","shinobi","warrior");
        cls.getSelectionModel().selectFirst();
        Button bCreate = new Button("Create");
        Label info = new Label();
        bCreate.setOnAction(e -> {
            String n = name.getText().trim().replace(" ", "_");
            if (n.isEmpty()) { info.setText("Enter a name"); return; }
            net.send("CREATE_CHAR " + n + " " + cls.getValue());
            info.setText("Creating…");
        });

        HBox create = new HBox(8, name, cls, bCreate);
        create.setAlignment(Pos.CENTER_LEFT);
        VBox root = new VBox(12, list, bJoin, new Separator(), new Label("Create new:"), create, info);
        root.setPadding(new Insets(16));
        Scene s = new Scene(root, 460, 360);
        installGlobalInputFilters(s);
        return s;
    }

    private Scene buildGameScene() {
        BorderPane root = new BorderPane();
        Canvas canvas = new Canvas(960, 540);
        root.setCenter(canvas);

        VBox hud = new VBox(killsLabel);
        hud.setPadding(new Insets(8));
        hud.setAlignment(Pos.TOP_RIGHT);
        hud.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-background-radius: 8;");
        BorderPane.setAlignment(hud, Pos.TOP_RIGHT);
        root.setRight(hud);

        respawnBtn = new Button("Respawn");
        respawnBtn.setVisible(false);
        respawnBtn.setOnAction(e -> net.send("RESPAWN"));
        StackPane overlay = new StackPane(respawnBtn);
        overlay.setPickOnBounds(false);
        StackPane.setAlignment(respawnBtn, Pos.CENTER);

        StackPane stack = new StackPane(canvas, overlay);
        root.setCenter(stack);

        Scene scene = new Scene(root, 980, 600);
        installGlobalInputFilters(scene);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) showEscapeMenu(scene);
        });

        new AnimationTimer() {
            private long lastFrame = 0, lastSend = 0;
            @Override public void handle(long now) {
                if (lastFrame == 0) lastFrame = now;
                double dt = (now - lastFrame) / 1e9; lastFrame = now;

                if (atk1 && !prevAtk1) { atk1StartMs = System.currentTimeMillis(); }
                if (jump) { jumpStartMs = System.currentTimeMillis(); }
                prevAtk1 = atk1;

                if (net != null && now - lastSend > 50_000_000L) {
                    net.send("INPUT " + (seq++) + " " +
                            b(up) + " " + b(down) + " " + b(left) + " " + b(right) + " " +
                            b(atk1) + " " + b(jump) + " " + b(false) + " " + b(guard));
                    lastSend = now;
                }
                draw(canvas.getGraphicsContext2D(), dt);
            }
        }.start();

        Platform.runLater(root::requestFocus);
        return scene;
    }

    private void onServerLine(String line) {
        Platform.runLater(() -> {
            if (line.startsWith("REGISTER_OK")) return;
            if (line.startsWith("REGISTER_FAIL")) { new Alert(Alert.AlertType.ERROR, line).show(); return; }
            if (line.startsWith("AUTH_OK")) { net.send("LIST_CHARS"); return; }
            if (line.startsWith("AUTH_FAIL")) { new Alert(Alert.AlertType.ERROR, "Login failed").show(); return; }
            if (line.startsWith("CHARS ")) { List<CharRow> rows = parseChars(line); charScene = buildCharacterScene(rows); stage.setScene(charScene); return; }
            if (line.startsWith("CHAR_CREATED ")) { net.send("LIST_CHARS"); return; }
            if (line.startsWith("CHAR_FAIL ")) { new Alert(Alert.AlertType.ERROR, line.substring(10)).show(); return; }
            if (line.startsWith("WELCOME ")) { myId = line.split("\\s+")[1]; gameScene = buildGameScene(); stage.setScene(gameScene); return; }
            if (line.startsWith("SCORES ")) { String[] p = line.split("\\s+"); if (p.length>=2) killsLabel.setText("Kills: " + p[1]); return; }
            if (line.startsWith("RESPAWN_OK")) { return; }
            if (line.startsWith("SNAPSHOT ")) {
                String[] p = line.split("\\s+");
                int count = Integer.parseInt(p[2]);
                Map<String,Integer> newHp = new HashMap<>();
                int idx = 3;
                players.clear();
                for (int i=0;i<count;i++) {
                    Player pl = new Player();
                    pl.id = p[idx++]; pl.name = p[idx++].replace('_',' ');
                    pl.clazz = p[idx++]; pl.x = Integer.parseInt(p[idx++]); pl.y = Integer.parseInt(p[idx++]);
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
                boolean iAmDead = false;
                if (myId != null) {
                    Player me = players.get(myId);
                    iAmDead = (me != null && me.hp <= 0);
                }
                if (respawnBtn != null) respawnBtn.setVisible(iAmDead);
                return;
            }
        });
    }

    private static List<CharRow> parseChars(String line) {
        String[] p = line.split("\\s+");
        int n = Integer.parseInt(p[1]);
        int i = 2;
        List<CharRow> out = new ArrayList<>();
        for (int k=0;k<n;k++) {
            CharRow r = new CharRow();
            r.id = Integer.parseInt(p[i++]);
            r.name = p[i++].replace('_',' ');
            r.clazz = p[i++];
            out.add(r);
        }
        return out;
    }

    private void installGlobalInputFilters(Scene s) {
        s.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.W) up = true;
            else if (c == KeyCode.S) down = true;
            else if (c == KeyCode.A) left = true;
            else if (c == KeyCode.D) right = true;
            else if (c == KeyCode.SPACE) atk1 = true;
            else if (c == KeyCode.K) guard = true;
            else if (c == KeyCode.J) jump = true;
        });
        s.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.W) up = false;
            else if (c == KeyCode.S) down = false;
            else if (c == KeyCode.A) left = false;
            else if (c == KeyCode.D) right = false;
            else if (c == KeyCode.SPACE) atk1 = false;
            else if (c == KeyCode.K) guard = false;
            else if (c == KeyCode.J) jump = false;
        });
    }

    private void draw(GraphicsContext g, double dt) {
        g.setFill(Color.BLACK); g.fillRect(0,0,960,540);
        drawBackground(g);

        long nowMs = System.currentTimeMillis();

        for (Player p : players.values()) {
            double camX = camX(), camY = camY();
            double px = p.x - camX, py = p.y - camY;
            int[] prev = lastPos.getOrDefault(p.id, new int[]{p.x, p.y});
            boolean moving = (prev[0] != p.x) || (prev[1] != p.y);
            lastPos.put(p.id, new int[]{p.x, p.y});

            CharAnims ca = anims.getOrDefault(p.clazz, anims.get("warrior"));
            Anim chosen = null;

            boolean isMe = myId != null && p.id.equals(myId);
            boolean showAtk1 = isMe && atk1StartMs > 0 && (nowMs - atk1StartMs) < ATTACK1_MS;
            boolean showJump = isMe && jumpStartMs > 0 && (nowMs - jumpStartMs) < JUMP_MS;
            boolean showHurt = hurtUntil.getOrDefault(p.id, 0L) > nowMs;
            boolean isDead = p.hp <= 0;

            if (isDead && ca != null && ca.dead != null) chosen = ca.dead;
            else if (showHurt && ca != null && ca.hurt != null) chosen = ca.hurt;
            else if (p.guarding && ca != null && ca.shield != null) chosen = ca.shield;
            else if (showAtk1 && ca != null && ca.attack1 != null) chosen = ca.attack1;
            else if (showJump && ca != null && ca.jump != null) chosen = ca.jump;
            else if (moving && ca != null) chosen = (ca.run != null ? ca.run : ca.walk != null ? ca.walk : ca.idle);
            else if (ca != null) chosen = ca.idle;

            if (chosen != null) {
                chosen.t = (chosen.t + dt * 10) % Math.max(1, chosen.frames);
                int fi = (int)chosen.t;
                double dw = chosen.fw * SCALE, dh = chosen.fh * SCALE;
                double dx = px - dw/2, dy = py - dh/2;
                if (p.facingRight) g.drawImage(chosen.sheet, fi*chosen.fw, 0, chosen.fw, chosen.fh, dx, dy, dw, dh);
                else g.drawImage(chosen.sheet, fi*chosen.fw, 0, chosen.fw, chosen.fh, dx+dw, dy, -dw, dh);
            } else {
                g.setFill(Color.DARKRED); g.fillRect(px-12, py-12, 24, 24);
            }

            g.setFill(Color.WHITE);
            g.fillText(p.name + " (" + p.hp + ")", px - 24, py - 30);
        }
    }

    private double camX() {
        Player m = (myId!=null) ? players.get(myId) : null;
        if (m != null) return m.x - 480;
        if (!players.isEmpty()) return players.values().iterator().next().x - 480;
        return 0;
    }
    private double camY() {
        Player m = (myId!=null) ? players.get(myId) : null;
        if (m != null) return m.y - 540*0.6;
        if (!players.isEmpty()) return players.values().iterator().next().y - 540*0.6;
        return 0;
    }

    private void drawBackground(GraphicsContext g) {
        if (arenaBG == null) return;
        double viewW = 960, viewH = 540;
        double worldW = arenaBG.getWidth(), worldH = arenaBG.getHeight();
        double cx = camX(), cy = camY();
        if (cx < 0) cx = 0; if (cy < 0) cy = 0;
        if (cx > worldW - viewW) cx = worldW - viewW;
        if (cy > worldH - viewH) cy = worldH - viewH;
        g.drawImage(arenaBG, cx, cy, viewW, viewH, 0, 0, viewW, viewH);
    }

    private void showEscapeMenu(Scene scene) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Menu");
        ButtonType back = new ButtonType("Back", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType change = new ButtonType("Change Character", ButtonBar.ButtonData.OTHER);
        ButtonType exit = new ButtonType("Exit", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().setAll(back, change, exit);
        dlg.getDialogPane().setContent(new Label("Paused"));
        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent()) {
            if (res.get() == change) net.send("LIST_CHARS");
            else if (res.get() == exit) Platform.exit();
        }
    }

    private void ensureConnected(String host, String port) throws Exception {
        if (net != null) return;
        net = new RtClient();
        net.connect(host.trim(), Integer.parseInt(port.trim()));
    }

    private static String b(boolean v){ return v ? "1" : "0"; }

    private static Image safeImg(String path) {
        try {
            var in = ClientApp.class.getResourceAsStream(path);
            return (in == null) ? null : new Image(in);
        } catch (Exception e) { return null; }
    }

    private static CharAnims loadChar(String name) {
        CharAnims c = new CharAnims();
        c.idle    = anim("/sprites/"+name+"/Idle.png");
        c.run     = anim("/sprites/"+name+"/Run.png");
        c.walk    = anim("/sprites/"+name+"/Walk.png");
        c.attack1 = anim("/sprites/"+name+"/Attack_1.png");
        c.shield  = anim("/sprites/"+name+"/Shield.png");
        c.dead    = anim("/sprites/"+name+"/Dead.png");
        c.hurt    = anim("/sprites/"+name+"/Hurt.png");
        c.jump    = anim("/sprites/"+name+"/Jump.png");
        return c;
    }

    private static Anim anim(String path) {
        try {
            var in = ClientApp.class.getResourceAsStream(path);
            if (in == null) return null;
            Image sheet = new Image(in);
            Anim a = new Anim();
            a.sheet = sheet;
            a.fh = (int)Math.round(sheet.getHeight());
            a.fw = a.fh;
            a.frames = Math.max(1, (int)Math.round(sheet.getWidth()) / a.fw);
            return a;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args){ launch(args); }
}
