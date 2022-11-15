package xyz.duncanruns.julti.gui;

import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.ResourceUtil;
import xyz.duncanruns.julti.util.ScreenCapUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OldWallWindow extends JFrame {
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
    private boolean closed;

    public OldWallWindow(Julti julti) {
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
        MonitorUtil.Monitor mainMonitor = MonitorUtil.getPrimaryMonitor();
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
        if ((!JultiOptions.getInstance().wallResetAllAfterPlaying) || isActive() || JultiOptions.getInstance().resetMode != 1) {
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

            int totalRows;
            int totalColumns;

            JultiOptions options = JultiOptions.getInstance();
            if (!options.autoCalcWallSize) {
                totalRows = options.overrideRowsAmount;
                totalColumns = options.overrideColumnsAmount;
            } else {
                totalRows = (int) Math.ceil(Math.sqrt(instances.size()));
                totalColumns = (int) Math.ceil(instances.size() / (float) totalRows);
            }

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
        if (Collections.unmodifiableList(new ArrayList<>(julti.getResetManager().getLockedInstances())).contains(instance)) {
            // Create Graphics from image
            Graphics imageG = image.getGraphics();
            // Draw Lock
            if (JultiOptions.getInstance().wallDarkenLocked) {
                imageG.setColor(new Color(0, 0, 0, JultiOptions.getInstance().wallDarkenLevel));
                imageG.fillRect(0, 0, image.getWidth(), image.getHeight());
            }
            if (JultiOptions.getInstance().wallShowLockIcons) {
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


    public boolean isClosed() {
        return closed;
    }

}