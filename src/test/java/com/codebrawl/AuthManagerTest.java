package com.codebrawl.auth;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
class AuthManagerTest {

    private static Path tempRoot;
    private static AuthManager auth;

    private static int aliceUid;
    private static int bobUid;
    private static int aliceCid;
    private static int bobCid;

    @BeforeAll
    static void setUp() throws Exception {
        tempRoot = Files.createTempDirectory("codebrawl-test-");
        System.setProperty("user.dir", tempRoot.toAbsolutePath().toString());
        auth = new AuthManager(null);
    }

    @AfterAll
    static void tearDown() throws IOException {
        deleteRecursively(tempRoot);
    }

    @Test
    @Order(1)
    void registerAndLogin() throws Exception {
        assertTrue(auth.register("alice", "pw"));
        assertTrue(auth.register("bob", "pw2"));

        aliceUid = auth.login("alice", "pw");
        bobUid   = auth.login("bob", "pw2");

        assertTrue(aliceUid > 0);
        assertTrue(bobUid > 0);

        assertFalse(auth.register("alice", "anything"));
    }

    @Test
    @Order(2)
    void createCharacters() throws Exception {
        aliceCid = auth.createCharacter(aliceUid, "AliceSam", "samurai");
        bobCid   = auth.createCharacter(bobUid,   "BobWar",   "warrior");

        assertTrue(aliceCid > 0);
        assertTrue(bobCid > 0);

        assertEquals(0, auth.getKills(aliceCid));
        assertEquals(0, auth.getKills(bobCid));

        List<AuthManager.CharacterRow> aliceChars = auth.listCharacters(aliceUid);
        assertTrue(aliceChars.stream().anyMatch(c -> c.id == aliceCid));

        List<AuthManager.CharacterRow> bobChars = auth.listCharacters(bobUid);
        assertTrue(bobChars.stream().anyMatch(c -> c.id == bobCid));
    }

    @Test
    @Order(3)
    void addKillAndDeathUpdatesStats() throws Exception {
        auth.addKill(aliceCid);
        auth.addKill(aliceCid);
        auth.addDeath(bobCid);

        assertEquals(2, auth.getKills(aliceCid));

        var top = auth.topKillers(10);
        assertFalse(top.isEmpty());
        assertEquals("AliceSam", top.get(0).name);
        assertTrue(top.stream().anyMatch(r -> r.name.equals("BobWar")));
    }

    @Test
    @Order(4)
    void persistenceAcrossNewAuthManagerInstance() throws SQLException {
        AuthManager auth2 = new AuthManager(null);
        assertEquals(2, auth2.getKills(aliceCid));

        var top2 = auth2.topKillers(5);
        assertFalse(top2.isEmpty());
        assertEquals("AliceSam", top2.get(0).name);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        Files.walk(root)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
    }
}
