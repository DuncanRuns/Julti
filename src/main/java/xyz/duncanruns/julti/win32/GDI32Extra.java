package xyz.duncanruns.julti.win32;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.win32.W32APIOptions;

/**
 * JNA interface with Window's gdi32.dll
 * <p>
 * <a href="https://github.com/Lxnus/ScreenCapture4J/blob/master/screencapture4j/GDI32Extra.java">(Source)</a>
 *
 * @Author Lxnus
 */
public interface GDI32Extra extends GDI32 {

    GDI32Extra INSTANCE = Native.loadLibrary("gdi32", GDI32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean BitBlt(HDC hObject, int nXDest, int nYDest, int nWidth, int nHeight, HDC hObjectSource, int nXSrc, int nYSrc, DWORD dwRop);

}