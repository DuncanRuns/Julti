package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.ResourceUtil;
import xyz.duncanruns.julti.util.ScreenCapUtil;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Wall extends JFrame {
    private static final Image LOCK_IMAGE;

    static {
        try {
            LOCK_IMAGE = getLockImage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Julti julti;
    private int totalWidth, totalHeight;
    private boolean drewBack = false;
    private final List<Long> frameTimeQueue = new ArrayList<>(21);
    private double fps = 0.0;
    private final Set<MinecraftInstance> lockedInstances = new HashSet<>();
    private boolean closed;

    public Wall(Julti julti) {
        super();
        closed = false;
        this.julti = julti;
        setToPrimaryMonitor();
        executor.scheduleAtFixedRate(this::tick, 50_000_000, 1_000_000_000L / 60, TimeUnit.NANOSECONDS);
        setupWindow();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
    }

    private void setToPrimaryMonitor() {
        MonitorUtil.Monitor mainMonitor = MonitorUtil.getDefaultMonitor();
        setLocation(mainMonitor.x, mainMonitor.y);
        setSize(mainMonitor.width, mainMonitor.height);
        totalWidth = mainMonitor.width;
        totalHeight = mainMonitor.height;
    }

    private void tick() {
        repaint();
    }

    private void setupWindow() {
        setBackground(Color.BLACK);
        setUndecorated(true);
        setResizable(false);
        setVisible(true);
        setTitle("Wall");
        try {
            setIconImage(ResourceUtil.getImageResource("/lock.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onClose() {
        executor.shutdownNow();
        closed = true;
    }

    private static Image getLockImage() throws IOException {
        File lockFile = new File(JultiOptions.getJultiDir().resolve("lock.png").toUri());
        if (lockFile.isFile()) {
            return ImageIO.read(lockFile);
        } else {
            return ResourceUtil.getImageResource("/lock.png");
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        onClose();
    }

    @Override
    public void paint(Graphics g) {
        if ((!JultiOptions.getInstance().wallOneAtATime) || isActive()) {
            drawWall(g);
        }
    }

    private void drawWall(Graphics graphics) {
        try {
            if (!drewBack) {
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0, 0, totalWidth, totalHeight);
                drewBack = true;
            }

            List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();
            if (instances.size() == 0) return;

            final int totalRows = (int) Math.ceil(Math.sqrt(instances.size()));
            final int totalColumns = (int) Math.ceil(instances.size() / (float) totalRows);

            final int iWidth = totalWidth / totalColumns;
            final int iHeight = totalHeight / totalRows;

            //Thread[] threads = new Thread[instances.size()];

            int n = 0;
            fullLoop:
            for (int y = 0; y < totalRows; y++) {
                for (int x = 0; x < totalColumns; x++) {
                    try {
                        final MinecraftInstance instance = instances.get(n);
                        if (instance.hasWindow()) {
                            ScreenCapUtil.ImageInfo imageInfo = instance.captureScreen();
                            BufferedImage image = new BufferedImage(imageInfo.width, imageInfo.height, BufferedImage.TYPE_INT_RGB);
                            setImageRGB(image, imageInfo);
                            //int finalX = x;
                            //int finalY = y;
                            //threads[n] = new Thread(() -> drawInstance(graphics, iWidth, iHeight, image, finalX, finalY, instance));
                            //threads[n].start();
                            drawInstance(graphics, iWidth, iHeight, image, x, y, instance);
                        }
                        n++;
                        if (n >= instances.size()) {
                            break fullLoop;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            //for (Thread thread : threads) {
            //        thread.join();
            //    }
            //measureFrames();
            //drawFPS(graphics);
        } catch (Exception ignored) {
        }
    }

    private static void setImageRGB(BufferedImage image, ScreenCapUtil.ImageInfo imageInfo) {
        WritableRaster raster = image.getRaster();
        int[] ints = imageInfo.pixels;
        int i = 0;
        for (int y = 0; y < imageInfo.height; y++) {
            for (int x = 0; x < imageInfo.width; x++) {
                // Proper way is to use a ColorModel's getDataElements(int, Object), but that
                // always returns an int list with just one element which is the input integer
                raster.setDataElements(x, y, new int[]{ints[i++]});
            }
        }
    }

    private void drawInstance(Graphics graphics, int iWidth, int iHeight, BufferedImage image, int x, int y, MinecraftInstance instance) {
        if (lockedInstances.contains(instance)) {
            // Create Graphics from image
            Graphics imageG = image.getGraphics();
            // Draw Lock
            if (JultiOptions.getInstance().wallDarkenLocked) {
                imageG.setColor(new Color(0, 0, 0, JultiOptions.getInstance().darkenLevel));
                imageG.fillRect(0, 0, image.getWidth(), image.getHeight());
            }
            if (JultiOptions.getInstance().wallShowLockIcon) {
                imageG.drawImage(LOCK_IMAGE, 0, 0, this);
            }
        }
        // Draw image
        graphics.drawImage(image, iWidth * x, iHeight * y, iWidth, iHeight, this);
    }

    private void measureFrames() {
        frameTimeQueue.add(System.nanoTime());
        if (frameTimeQueue.size() <= 21)
            return;
        frameTimeQueue.remove(0);
        int total = 0;
        for (int i = 0; i < 20; i++) {
            total += (int) (frameTimeQueue.get(i + 1) - frameTimeQueue.get(i));
        }
        fps = total / 20_000_000f;
    }

    private void drawFPS(Graphics graphics) {
        graphics.setColor(Color.black);
        graphics.fillRect(0, totalHeight - 10, totalWidth, 10);
        graphics.setColor(Color.white);
        graphics.drawString("FPS: " + fps, 0, totalHeight);
    }

    public void lockInstance(int screenX, int screenY) {
        MinecraftInstance clickedInstance = getSelectedInstance(screenX, screenY);
        if (clickedInstance == null) return;
        lockedInstances.add(clickedInstance);
    }

    @Nullable
    private MinecraftInstance getSelectedInstance(int screenX, int screenY) {
        Point windowPos = getLocation();

        int x = screenX - windowPos.x;
        int y = screenY - windowPos.y;

        if (!getBounds().contains(screenX, screenY)) return null;

        List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();

        final int totalRows = (int) Math.ceil(Math.sqrt(instances.size()));
        final int totalColumns = (int) Math.ceil(instances.size() / (float) totalRows);

        final int iWidth = totalWidth / totalColumns;
        final int iHeight = totalHeight / totalRows;

        int row = y / iHeight;
        int column = x / iWidth;
        int instanceIndex = row * totalColumns + column;

        if (instanceIndex >= instances.size()) return null;

        return instances.get(instanceIndex);
    }

    public void playInstance(int screenX, int screenY) {
        MinecraftInstance clickedInstance = getSelectedInstance(screenX, screenY);
        if (clickedInstance == null) return;
        clickedInstance.activate();
        julti.switchScene(clickedInstance);
        lockedInstances.remove(clickedInstance);
    }

    public void onLeaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        // If using One At A Time, just reset all instances
        if (JultiOptions.getInstance().wallOneAtATime) {
            requestFocus();
            instances.forEach(MinecraftInstance::reset);
            // Clear out locked instances since all instances reset.
            lockedInstances.clear();
            return;
        }

        resetInstance(selectedInstance);
        lockedInstances.remove(selectedInstance);
        if (lockedInstances.isEmpty()) {
            requestFocus();
            julti.switchToWallScene();
        } else {
            MinecraftInstance nextInstance = lockedInstances.iterator().next();
            lockedInstances.remove(nextInstance);
            nextInstance.activate();
            julti.switchScene(nextInstance);
        }
    }

    private void resetInstance(MinecraftInstance instance) {
        lockedInstances.remove(instance);
        if (!instance.hasPreviewEverStarted() || instance.isWorldLoaded() || (instance.isPreviewLoaded() && System.currentTimeMillis() - instance.getLastPreviewStart() > JultiOptions.getInstance().wallResetCooldown))
            instance.reset();
    }

    public void resetInstance(int x, int y) {
        MinecraftInstance instance = getSelectedInstance(x, y);
        if (instance == null) return;
        resetInstance(instance);
    }

    public boolean isClosed() {
        return closed;
    }

    public void fullReset(List<MinecraftInstance> instances) {
        List<MinecraftInstance> lockedInstances = getLockedInstances();
        for (MinecraftInstance instance : instances) {
            if (lockedInstances.contains(instance)) continue;
            resetInstance(instance);
        }
    }

    public List<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableList(new ArrayList<>(lockedInstances));
    }
}
