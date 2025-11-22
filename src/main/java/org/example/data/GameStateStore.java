package org.example.data;

import org.example.model.AbstractPiece;
import org.example.model.AdvisorPiece;
import org.example.model.CannonPiece;
import org.example.model.ChariotPiece;
import org.example.model.ChessBoardModel;
import org.example.model.ElephantPiece;
import org.example.model.GeneralPiece;
import org.example.model.HorsePiece;
import org.example.model.SoldierPiece;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * 游戏状态存储类。
 * 负责将游戏状态（棋盘布局、回合信息、获胜者）保存到文件以及从文件加载。
 * 使用 Java Properties 文件格式进行存储。
 */
public final class GameStateStore {

    private static final String DEFAULT_DIRECTORY = System.getProperty("user.home") + "/.xiangqi/saves";

    private final Path storageDirectory;

    public GameStateStore() {
        this(DEFAULT_DIRECTORY);
    }

    public GameStateStore(String directory) {
        Objects.requireNonNull(directory, "directory");
        this.storageDirectory = Paths.get(directory);
        ensureDirectoryExists(storageDirectory);
    }

    /**
     * 保存游戏状态。
     *
     * @param account  账号
     * @param slot     存档槽位 (1-10)
     * @param board    棋盘模型
     * @param saveName 存档名称
     * @throws IOException 如果写入文件失败
     */
    public void saveState(String account, int slot, ChessBoardModel board, String saveName) throws IOException {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(board, "board");
        if (slot < 1 || slot > 10) {
            throw new IllegalArgumentException("Slot must be between 1 and 10");
        }

        Properties properties = new Properties();
        properties.setProperty("redTurn", Boolean.toString(board.isRedTurn()));
        String winner = board.getWinner();
        properties.setProperty("winner", winner == null ? "" : winner);
        properties.setProperty("timestamp", Long.toString(System.currentTimeMillis()));
        properties.setProperty("saveName", saveName == null ? "未命名存档" : saveName);

        List<AbstractPiece> pieces = board.getPieces();
        properties.setProperty("piece.count", Integer.toString(pieces.size()));
        for (int i = 0; i < pieces.size(); i++) {
            AbstractPiece piece = pieces.get(i);
            String serialized = String.join("|",
                    piece.getClass().getSimpleName(),
                    piece.getName(),
                    Integer.toString(piece.getRow()),
                    Integer.toString(piece.getCol()),
                    Boolean.toString(piece.isRed()),
                    Integer.toString(piece.getCaptureCount())
            );
            properties.setProperty("piece." + i, serialized);
        }

        Path target = resolveFile(account, slot);
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            properties.store(writer, "Saved Xiangqi game state");
        }
    }

    /**
     * 加载游戏状态。
     *
     * @param account 账号
     * @param slot    存档槽位 (1-10)
     * @return 包含游戏状态的 Optional 对象，如果存档不存在则返回 empty
     * @throws IOException 如果读取文件失败
     */
    public Optional<GameState> loadState(String account, int slot) throws IOException {
        Objects.requireNonNull(account, "account");
        Path source = resolveFile(account, slot);
        if (!Files.exists(source)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        int pieceCount = Integer.parseInt(properties.getProperty("piece.count", "0"));
        List<AbstractPiece> pieces = new ArrayList<>(pieceCount);
        for (int i = 0; i < pieceCount; i++) {
            String entry = properties.getProperty("piece." + i);
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split("\\|");
            if (parts.length < 6) {
                continue;
            }
            String type = parts[0];
            String name = parts[1];
            int row = parseInt(parts[2], -1);
            int col = parseInt(parts[3], -1);
            boolean red = Boolean.parseBoolean(parts[4]);
            int captures = parseInt(parts[5], 0);
            if (row < 0 || col < 0) {
                continue;
            }
            AbstractPiece piece = instantiatePiece(type, name, row, col, red);
            if (piece == null) {
                continue;
            }
            piece.captureCountProperty().set(captures);
            pieces.add(piece);
        }
        boolean redTurn = Boolean.parseBoolean(properties.getProperty("redTurn", "true"));
        String winner = properties.getProperty("winner", "");
        if (winner != null && winner.isBlank()) {
            winner = null;
        }
        return Optional.of(new GameState(pieces, redTurn, winner));
    }

    /**
     * 删除存档。
     *
     * @param account 账号
     * @param slot    存档槽位 (1-10)
     * @throws IOException 如果删除文件失败
     */
    public void deleteState(String account, int slot) throws IOException {
        Objects.requireNonNull(account, "account");
        Path file = resolveFile(account, slot);
        Files.deleteIfExists(file);
    }

    public boolean hasState(String account, int slot) {
        Objects.requireNonNull(account, "account");
        Path file = resolveFile(account, slot);
        return Files.exists(file);
    }

    /**
     * 获取指定账号的所有存档槽位信息。
     *
     * @param account 账号
     * @return 存档槽位信息列表
     */
    public List<SaveSlotInfo> getSaveSlots(String account) {
        List<SaveSlotInfo> slots = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Path file = resolveFile(account, i);
            if (Files.exists(file)) {
                long timestamp = 0;
                String saveName = "未命名存档";
                try {
                    timestamp = Files.getLastModifiedTime(file).toMillis();
                    // Try to read timestamp from properties if available for better accuracy
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        Properties props = new Properties();
                        props.load(reader);
                        String ts = props.getProperty("timestamp");
                        if (ts != null) {
                            timestamp = Long.parseLong(ts);
                        }
                        String name = props.getProperty("saveName");
                        if (name != null) {
                            saveName = name;
                        }
                    }
                } catch (Exception ignore) {}
                slots.add(new SaveSlotInfo(i, true, timestamp, saveName));
            } else {
                slots.add(new SaveSlotInfo(i, false, 0, null));
            }
        }
        return slots;
    }

    /**
     * 根据类型名称和所有者创建棋子实例。
     *
     * @param type  棋子类型名称
     * @param owner 棋子所有者
     * @return 棋子实例，如果类型未知则返回 null
     */
    private static AbstractPiece instantiatePiece(String type, String name, int row, int col, boolean red) {
        return switch (type) {
            case "AdvisorPiece" -> new AdvisorPiece(name, row, col, red);
            case "CannonPiece" -> new CannonPiece(name, row, col, red);
            case "ChariotPiece" -> new ChariotPiece(name, row, col, red);
            case "ElephantPiece" -> new ElephantPiece(name, row, col, red);
            case "GeneralPiece" -> new GeneralPiece(name, row, col, red);
            case "HorsePiece" -> new HorsePiece(name, row, col, red);
            case "SoldierPiece" -> new SoldierPiece(name, row, col, red);
            default -> null;
        };
    }

    private static void ensureDirectoryExists(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建存档目录: " + directory, ex);
        }
    }

    private Path resolveFile(String account, int slot) {
        String normalized = Normalizer.normalize(account, Normalizer.Form.NFKC);
        String baseName = normalized.replaceAll("[^\\p{Alnum}_-]", "_");
        if (baseName.isBlank()) {
            baseName = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(account.getBytes(StandardCharsets.UTF_8));
        }
        String hashSuffix = Integer.toHexString(account.hashCode());
        String fileName = baseName + "_" + hashSuffix + "_slot" + slot + ".properties";
        return storageDirectory.resolve(fileName);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * 游戏状态记录类。
     */
    public record GameState(List<AbstractPiece> pieces, boolean redTurn, String winner) {}
    /**
     * 存档槽位信息记录类。
     */
    public record SaveSlotInfo(int id, boolean exists, long timestamp, String saveName) {}
}
