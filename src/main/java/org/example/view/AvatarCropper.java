package org.example.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * 实用工具类，提供头像裁剪和遮罩功能，将用户选择的图片处理为圆形 PNG 资源。
 * <p>
 * 该类包含一个内部对话框 {@link CropDialog}，允许用户通过拖拽和缩放来调整图片显示区域，
 * 并最终生成圆形的头像文件保存到本地。
 * </p>
 */
public final class AvatarCropper {

    private AvatarCropper() {
    }

    /**
     * 打开裁剪对话框，允许用户裁剪指定的图片文件。
     *
     * @param file  用户选择的原始图片文件
     * @param owner 对话框的父窗口
     * @return 包含保存路径和预览图的 {@link Result} 对象；如果用户取消或文件无效，则返回 null
     */
    public static Result crop(File file, Window owner) {
        if (file == null) {
            return null;
        }
        Image source = new Image(file.toURI().toString());
        if (source.isError() || source.getWidth() <= 1 || source.getHeight() <= 1) {
            return null;
        }
        CropDialog dialog = new CropDialog(file, source);
        return dialog.show(owner);
    }

    /**
     * 裁剪结果记录，包含保存后的文件路径和用于显示的预览图。
     *
     * @param savedPath 保存后的头像文件绝对路径
     * @param preview   裁剪后的圆形图像预览
     */
    public record Result(String savedPath, Image preview) {
    }

    /**
     * 内部裁剪对话框类，负责显示裁剪界面并处理用户交互。
     */
    private static final class CropDialog {
        private static final int CROP_SIZE = 260;
        private static final String PRIMARY_BUTTON_STYLE = "-fx-background-color: linear-gradient(to bottom, #f8d57c, #d29a3e);" +
                "-fx-text-fill: #331b07; -fx-background-radius: 18; -fx-cursor: hand;";
        private static final String SECONDARY_BUTTON_STYLE = "-fx-background-color: rgba(217,186,140,0.62);" +
                "-fx-text-fill: #5b3922; -fx-background-radius: 18; -fx-cursor: hand;";

        private final File originalFile;
        private final Image source;
        private final ImageView imageView;
        private final Pane cropPane;
        private final Label errorLabel;

        private double scale;
        private double baseScale;
        private double offsetX;
        private double offsetY;
        private double dragStartX;
        private double dragStartY;
        private double dragOffsetX;
        private double dragOffsetY;

        private CropDialog(File originalFile, Image source) {
            this.originalFile = originalFile;
            this.source = source;
            this.imageView = new ImageView(source);
            this.imageView.setPreserveRatio(true);
            this.imageView.setSmooth(true);
            this.cropPane = new Pane(imageView);
            this.errorLabel = new Label();
        }

        /**
         * 显示裁剪对话框并等待用户操作。
         *
         * @param owner 父窗口
         * @return 裁剪结果，如果取消则返回 null
         */
        private Result show(Window owner) {
            Stage stage = new Stage();
            stage.setTitle("调整头像");
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            }

            cropPane.setPrefSize(CROP_SIZE, CROP_SIZE);
            cropPane.setMinSize(CROP_SIZE, CROP_SIZE);
            cropPane.setMaxSize(CROP_SIZE, CROP_SIZE);
            cropPane.setStyle("-fx-background-color: rgba(44,28,16,0.85); -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(233,203,146,0.45);");
            Rectangle clip = new Rectangle(CROP_SIZE, CROP_SIZE);
            cropPane.setClip(clip);

            Circle guide = new Circle(CROP_SIZE / 2.0);
            guide.setStroke(Color.web("#f2d7a3"));
            guide.setStrokeWidth(2.2);
            guide.setFill(Color.TRANSPARENT);
            guide.setMouseTransparent(true);

            StackPane preview = new StackPane(cropPane, guide);
            preview.setAlignment(Pos.CENTER);
            preview.setPrefSize(CROP_SIZE, CROP_SIZE);

            Label tip = new Label("滚轮缩放，拖拽调整显示区域");
            tip.setFont(Font.font("KaiTi", FontWeight.NORMAL, 16));
            tip.setTextFill(Color.web("#fbe3b0"));
            tip.setAlignment(Pos.CENTER);

            errorLabel.setFont(Font.font("KaiTi", FontWeight.NORMAL, 14));
            errorLabel.setTextFill(Color.web("#ffb4a2"));
            errorLabel.setVisible(false);
            errorLabel.setAlignment(Pos.CENTER);

            Button cancel = new Button("取消");
            cancel.setPrefWidth(96);
            cancel.setStyle(SECONDARY_BUTTON_STYLE);
            Button confirm = new Button("确定");
            confirm.setPrefWidth(96);
            confirm.setStyle(PRIMARY_BUTTON_STYLE);
            confirm.setDefaultButton(true);

            HBox buttonRow = new HBox(12, cancel, confirm);
            buttonRow.setAlignment(Pos.CENTER);

            VBox bottomBox = new VBox(6, tip, errorLabel, buttonRow);
            bottomBox.setAlignment(Pos.CENTER);
            bottomBox.setPadding(new Insets(14, 0, 0, 0));

            BorderPane root = new BorderPane();
            root.setPadding(new Insets(20, 26, 20, 26));
            root.setCenter(preview);
            root.setBottom(bottomBox);
            root.setStyle("-fx-background-color: linear-gradient(to bottom, #3d2615, #241307); -fx-background-radius: 24; -fx-border-radius: 24; -fx-border-color: rgba(237,200,142,0.45);");

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setResizable(false);

            initializeView();
            registerInteractionHandlers();

            final Result[] resultHolder = new Result[1];
            cancel.setOnAction(evt -> stage.close());
            confirm.setOnAction(evt -> {
                WritableImage snapshot = captureSnapshot();
                WritableImage masked = maskToCircle(snapshot);
                try {
                    Path directory = ensureAvatarDirectory();
                    String baseName = originalFile.getName();
                    String fileName = "avatar_" + System.currentTimeMillis() + "_" + Math.abs(baseName.hashCode()) + ".png";
                    Path output = directory.resolve(fileName);
                    saveImage(masked, output);
                    resultHolder[0] = new Result(output.toString(), masked);
                    stage.close();
                } catch (IOException ex) {
                    errorLabel.setText("保存头像失败，请重试");
                    errorLabel.setVisible(true);
                }
            });

            stage.showAndWait();
            return resultHolder[0];
        }

        private void registerInteractionHandlers() {
            cropPane.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handlePress);
            cropPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleDrag);
            cropPane.addEventHandler(ScrollEvent.SCROLL, this::handleScroll);
        }

        private void initializeView() {
            baseScale = Math.max((double) CROP_SIZE / source.getWidth(), (double) CROP_SIZE / source.getHeight());
            scale = baseScale;
            updateImageSize();
            offsetX = (CROP_SIZE - source.getWidth() * scale) / 2.0;
            offsetY = (CROP_SIZE - source.getHeight() * scale) / 2.0;
            applyTransforms();
        }

        private void updateImageSize() {
            imageView.setFitWidth(source.getWidth() * scale);
            imageView.setFitHeight(source.getHeight() * scale);
        }

        private void handlePress(MouseEvent event) {
            dragStartX = event.getX();
            dragStartY = event.getY();
            dragOffsetX = offsetX;
            dragOffsetY = offsetY;
        }

        private void handleDrag(MouseEvent event) {
            offsetX = dragOffsetX + (event.getX() - dragStartX);
            offsetY = dragOffsetY + (event.getY() - dragStartY);
            constrainOffsets();
            applyTransforms();
        }

        private void handleScroll(ScrollEvent event) {
            double delta = event.getDeltaY();
            if (Math.abs(delta) < 1e-4) {
                return;
            }
            double factor = delta > 0 ? 1.12 : 0.9;
            double newScale = clamp(scale * factor, baseScale, baseScale * 4.5);
            if (Math.abs(newScale - scale) < 1e-4) {
                return;
            }

            double pivotX = event.getX();
            double pivotY = event.getY();
            double relativeX = pivotX - offsetX;
            double relativeY = pivotY - offsetY;

            double previousScale = scale;
            scale = newScale;
            updateImageSize();

            double ratio = scale / previousScale;
            offsetX = pivotX - relativeX * ratio;
            offsetY = pivotY - relativeY * ratio;
            constrainOffsets();
            applyTransforms();
        }

        private void applyTransforms() {
            imageView.setTranslateX(offsetX);
            imageView.setTranslateY(offsetY);
        }

        private void constrainOffsets() {
            double width = source.getWidth() * scale;
            double height = source.getHeight() * scale;

            double minX = CROP_SIZE - width;
            double minY = CROP_SIZE - height;

            if (width <= CROP_SIZE) {
                offsetX = (CROP_SIZE - width) / 2.0;
            } else {
                offsetX = clamp(offsetX, minX, 0);
            }

            if (height <= CROP_SIZE) {
                offsetY = (CROP_SIZE - height) / 2.0;
            } else {
                offsetY = clamp(offsetY, minY, 0);
            }
        }

        private WritableImage captureSnapshot() {
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            return cropPane.snapshot(parameters, null);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static Path ensureAvatarDirectory() throws IOException {
        Path dir = Path.of(System.getProperty("user.home"), ".xiangqi", "avatars");
        Files.createDirectories(dir);
        return dir;
    }

    private static void saveImage(WritableImage image, Path path) throws IOException {
        int width = (int) Math.round(image.getWidth());
        int height = (int) Math.round(image.getHeight());
        PixelReader reader = image.getPixelReader();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        ImageIO.write(buffered, "png", path.toFile());
    }

    private static WritableImage maskToCircle(WritableImage source) {
        int size = (int) Math.min(Math.round(source.getWidth()), Math.round(source.getHeight()));
        WritableImage output = new WritableImage(size, size);
        PixelReader reader = source.getPixelReader();
        PixelWriter writer = output.getPixelWriter();
        double radius = size / 2.0;
        double center = radius;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x + 0.5 - center;
                double dy = y + 0.5 - center;
                if (dx * dx + dy * dy <= radius * radius) {
                    writer.setArgb(x, y, reader.getArgb(x, y));
                } else {
                    writer.setArgb(x, y, 0);
                }
            }
        }
        return output;
    }
}
