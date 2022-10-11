package xyz.duncanruns.julti;

import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.ScreenCapUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Wall extends JFrame implements WindowListener {
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
        addWindowListener(this);
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
    }

    private static Image getLockImage() throws IOException {
        File lockFile = new File(JultiOptions.getJultiDir().resolve("lock.png").toUri());
        if (lockFile.isFile()) {
            return ImageIO.read(lockFile);
        } else {
            return ImageIO.read(Wall.class.getResource("/lock.png"));
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        onClose();
    }

    private void onClose() {
        executor.shutdownNow();
        closed = true;
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
                        ignored.printStackTrace();
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
            Graphics imageG = image.getGraphics();
            //imageG.setColor(new Color(0, 0, 0, 128));
            //imageG.fillRect(0, 0, image.getWidth(), image.getHeight());
            imageG.drawImage(LOCK_IMAGE, 0, 0, this);
        }
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
        lockedInstances.add(clickedInstance);
    }

    private MinecraftInstance getSelectedInstance(int screenX, int screenY) {
        Point windowPos = getLocation();

        int x = screenX - windowPos.x;
        int y = screenY - windowPos.y;

        List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();

        final int totalRows = (int) Math.ceil(Math.sqrt(instances.size()));
        final int totalColumns = (int) Math.ceil(instances.size() / (float) totalRows);

        final int iWidth = totalWidth / totalColumns;
        final int iHeight = totalHeight / totalRows;

        int row = y / iHeight;
        int column = x / iWidth;
        int instanceIndex = row * totalColumns + column;

        return instances.get(instanceIndex);
    }

    public void playInstance(int screenX, int screenY) {
        MinecraftInstance clickedInstance = getSelectedInstance(screenX, screenY);
        clickedInstance.activate();
        julti.switchScene(clickedInstance);
        lockedInstances.remove(clickedInstance);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        onClose();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    public List<MinecraftInstance> getLockedInstances() {
        return Collections.unmodifiableList(new ArrayList<>(lockedInstances));
    }

    public void onLeaveInstance(MinecraftInstance selectedInstance, List<MinecraftInstance> instances) {
        // If using One At A Time, just reset all instances
        if (JultiOptions.getInstance().wallOneAtATime) {
            instances.forEach(MinecraftInstance::reset);
            requestFocus();
            // Clear out locked instances since all instances reset.
            lockedInstances.clear();
            return;
        }

        selectedInstance.reset();
        if (lockedInstances.isEmpty()) {
            requestFocus();
        } else {
            MinecraftInstance nextInstance = lockedInstances.iterator().next();
            lockedInstances.remove(nextInstance);
            nextInstance.activate();
            julti.switchScene(nextInstance);
        }
    }

    public void resetInstance(int x, int y) {
        MinecraftInstance instance = getSelectedInstance(x, y);
        lockedInstances.remove(instance);
        instance.reset();
    }

    public boolean isClosed() {
        return closed;
    }
}
