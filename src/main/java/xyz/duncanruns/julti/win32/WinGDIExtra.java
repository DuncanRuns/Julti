package xyz.duncanruns.julti.win32;

import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinGDI;

/**
 * <a href="https://github.com/Lxnus/ScreenCapture4J/blob/master/screencapture4j/WinGDIExtra.java">(Source)</a>
 * @author Lxnus
 */
@SuppressWarnings("UnusedDeclaration")
public interface WinGDIExtra extends WinGDI {
    DWORD SRCCOPY = new DWORD(0x00CC0020);
}
