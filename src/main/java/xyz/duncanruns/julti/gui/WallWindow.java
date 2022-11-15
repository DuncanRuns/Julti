package xyz.duncanruns.julti.gui;

import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.util.HwndUtil;
import xyz.duncanruns.julti.util.MonitorUtil;
import xyz.duncanruns.julti.util.ResourceUtil;
import xyz.duncanruns.julti.win32.GDI32Extra;
import xyz.duncanruns.julti.win32.Msimg32;
import xyz.duncanruns.julti.win32.User32;
import xyz.duncanruns.julti.win32.WinGDIExtra;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WallWindow extends Frame {
    private static final WinDef.HBRUSH BLACK_BRUSH = new WinDef.HBRUSH(GDI32Extra.INSTANCE.GetStockObject(4));
    private static final WinDef.HBRUSH WHITE_BRUSH = new WinDef.HBRUSH(GDI32Extra.INSTANCE.GetStockObject(0));
    private static final WinDef.UINT WHITE = new WinDef.UINT(0xFFFFFF);

    private WinDef.HDC lockIcon = null;
    private int lockWidth, lockHeight;

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Julti julti;
    private int totalWidth, totalHeight;
    private WinDef.RECT fullRect;
    private boolean closed;
    private WinDef.HWND hwnd;
    private WinDef.HDC bufferHdc;

    public WallWindow(Julti julti) {
        super();
        this.julti = julti;
        closed = false;
        setToPrimaryMonitor();
        executor.scheduleAtFixedRate(this::tick, 50_000_000, 1_000_000_000L / 15, TimeUnit.NANOSECONDS);
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

        fullRect = new WinDef.RECT();
        fullRect.left = mainMonitor.x;
        fullRect.right = mainMonitor.x + mainMonitor.width;
        fullRect.top = mainMonitor.y;
        fullRect.bottom = mainMonitor.y + mainMonitor.height;
    }

    private void tick() {
        if (!(JultiOptions.getInstance().pauseRenderingDuringPlay && julti.getInstanceManager().getSelectedInstance() != null) || JultiOptions.getInstance().resetMode != 1) {
            drawWall();
        }
    }

    private void setupWindow() {
        setBackground(Color.BLACK);
        setUndecorated(true);
        setResizable(false);
        setVisible(true);
        String tempTitle = "Julti Wall - " + new Random().nextInt();
        setTitle(tempTitle);
        this.hwnd = new WinDef.HWND(HwndUtil.waitForWindow(tempTitle));
        this.bufferHdc = null;
        setTitle("Julti Wall");
        try {
            setIconImage(ResourceUtil.getImageResource("/lock.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onClose() {
        closed = true;
        executor.shutdownNow();
        GDI32Extra.INSTANCE.DeleteDC(bufferHdc);
        GDI32Extra.INSTANCE.DeleteDC(lockIcon);
        dispose();
    }

    private void drawWall() {
        WinDef.HDC hdcOut = User32.INSTANCE.GetDC(hwnd);
        if (bufferHdc == null) {
            bufferHdc = GDI32.INSTANCE.CreateCompatibleDC(hdcOut);
            WinDef.HBITMAP bufferBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcOut, totalWidth, totalHeight);
            GDI32.INSTANCE.SelectObject(bufferHdc, bufferBitmap);
        }

        // Fill Black
        User32.INSTANCE.FillRect(bufferHdc, fullRect, BLACK_BRUSH);

        // Draw Instances
        drawAllInstances();

        // Buffer to Window
        GDI32Extra.INSTANCE.BitBlt(hdcOut, 0, 0, 1920, 1080, bufferHdc, 0, 0, GDI32.SRCCOPY);
        // Release Window DC
        User32.INSTANCE.ReleaseDC(hwnd, hdcOut);
    }

    private void drawAllInstances() {
        List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();
        Set<MinecraftInstance> lockedInstances = julti.getResetManager().getLockedInstances();
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

        WinDef.HDC lockHDC = getLockHDC(bufferHdc);
        int n = 0;
        fullLoop:
        for (int y = 0; y < totalRows; y++) {
            for (int x = 0; x < totalColumns; x++) {
                final MinecraftInstance instance = instances.get(n++);
                if (!instance.hasWindowQuick()) return;
                final boolean isLocked = lockedInstances.contains(instance);
                final int prepSet = options.wallDarkenLocked && isLocked ? options.wallDarkenLevel : 0;
                prepStretch(bufferHdc, prepSet);
                drawInstance(instance, bufferHdc, x * iWidth, y * iHeight, iWidth, iHeight);
                if (isLocked && options.wallShowLockIcons) {
                    if (prepSet != 0) {
                        prepStretch(bufferHdc, 0);
                    }
                    Msimg32.INSTANCE.TransparentBlt(bufferHdc, x * iWidth, y * iHeight, lockWidth, lockHeight, lockHDC, 0, 0, lockWidth, lockHeight, WHITE);
                }
                if (n >= instances.size()) {
                    break fullLoop;
                }
            }
        }
    }

    private WinDef.HDC getLockHDC(WinDef.HDC baseHDC) {
        if (lockIcon != null) return lockIcon;

        try {
            File lockFile = JultiOptions.getJultiDir().resolve("lock.png").toFile();
            if (!lockFile.exists()) {
                BufferedImage image = ResourceUtil.getImageResource("/lock.png");
                ImageIO.write(image, "png", lockFile);
            }
            BufferedImage image = ImageIO.read(lockFile);
            lockWidth = image.getWidth();
            lockHeight = image.getHeight();

            lockIcon = GDI32.INSTANCE.CreateCompatibleDC(baseHDC);

            WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(baseHDC, lockWidth, lockHeight);
            GDI32.INSTANCE.SelectObject(lockIcon, hBitmap);

            WritableRaster raster = image.getRaster();
            for (int x = 0; x < lockWidth; x++) {
                for (int y = 0; y < lockHeight; y++) {
                    int[] pixel = raster.getPixel(x, y, (int[]) null);
                    if (pixel[3] > 128) {
                        int color = (pixel[0]) + (pixel[1] << 8) + (pixel[2] << 16);
                        GDI32Extra.INSTANCE.SetPixel(lockIcon, x, y, color);
                    } else {
                        GDI32Extra.INSTANCE.SetPixel(lockIcon, x, y, 0xFFFFFF);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lockIcon;
    }

    private static void prepStretch(WinDef.HDC hdc, int darkLevel) {
        if (darkLevel == 0) {
            GDI32Extra.INSTANCE.SetStretchBltMode(hdc, 3);
            return;
        }
        GDI32Extra.COLORADJUSTMENT ca = new GDI32Extra.COLORADJUSTMENT();
        GDI32Extra.INSTANCE.GetColorAdjustment(hdc, ca);
        GDI32Extra.INSTANCE.SetStretchBltMode(hdc, 4);
        ca.caBrightness = new WinDef.SHORT(-darkLevel);
        ca.caContrast = new WinDef.SHORT(-darkLevel);
        GDI32Extra.INSTANCE.SetColorAdjustment(hdc, ca);
    }

    private void drawInstance(MinecraftInstance instance, WinDef.HDC hdc, int x, int y, int w, int h) {
        WinDef.HWND hwndSrc = new WinDef.HWND(instance.getHwnd());

        WinDef.HDC hdcSrc = User32.INSTANCE.GetDC(hwndSrc);

        WinDef.RECT srcBounds = new WinDef.RECT();
        User32.INSTANCE.GetClientRect(hwndSrc, srcBounds);
        int srcWidth = srcBounds.right - srcBounds.left;
        int srcHeight = srcBounds.bottom - srcBounds.top;

        GDI32Extra.INSTANCE.StretchBlt(hdc, x, y, w, h, hdcSrc, 0, 0, srcWidth, srcHeight, WinGDIExtra.SRCCOPY);

        User32.INSTANCE.ReleaseDC(hwndSrc, hdcSrc);
    }

    public boolean isClosed() {
        return closed;
    }
}
