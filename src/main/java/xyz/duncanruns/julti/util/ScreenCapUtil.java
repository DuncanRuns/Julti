package xyz.duncanruns.julti.util;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import xyz.duncanruns.julti.win32.GDI32Extra;
import xyz.duncanruns.julti.win32.User32;
import xyz.duncanruns.julti.win32.WinGDIExtra;

public final class ScreenCapUtil {
    private static WinDef.HBITMAP hBitmap = null;
    private static int width, height;

    private ScreenCapUtil() {

    }

    public static ImageInfo capture(Pointer hwnd) {
        return capture(new WinDef.HWND(hwnd));
    }

    public static ImageInfo capture(WinDef.HWND hWnd) {
        WinDef.HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
        WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

        WinDef.RECT bounds = new WinDef.RECT();
        User32.INSTANCE.GetClientRect(hWnd, bounds);

        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;

        WinDef.HBITMAP hBitmap = getHbitmap(width, height, hdcWindow);

        WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
        GDI32Extra.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, WinGDIExtra.SRCCOPY); // 1,540,200

        GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
        GDI32.INSTANCE.DeleteDC(hdcMemDC);

        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        Memory buffer = new Memory((long) width * height * 4);
        GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS); // 2,111,900


        int[] intArray = buffer.getIntArray(0, width * height); // 1,913,200
        User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

        return new ImageInfo(intArray, width, height);
    }

    private static WinDef.HBITMAP getHbitmap(int width, int height, WinDef.HDC hdcWindow) {
        if (width != ScreenCapUtil.width || height != ScreenCapUtil.height || hBitmap == null) {
            ScreenCapUtil.width = width;
            ScreenCapUtil.height = height;
            if (hBitmap != null) {
                GDI32.INSTANCE.DeleteObject(ScreenCapUtil.hBitmap); //1,229,400
                hBitmap = null;
            }
            ScreenCapUtil.hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height); // 2,360,300
        }
        return hBitmap;
    }

    public static class ImageInfo {
        public final int[] pixels;
        public final int width;
        public final int height;

        public ImageInfo(int[] pixels, int width, int height) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }
    }
}
