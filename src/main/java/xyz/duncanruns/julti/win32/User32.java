package xyz.duncanruns.julti.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.platform.win32.WinUser.INPUT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * JNA interface with Window's user32.dll
 *
 * @author Pete S & DuncanRuns & Lxnus
 */
public interface User32 extends StdCallLibrary {
    User32 INSTANCE = Native.load("user32", User32.class);

    Pointer GetForegroundWindow();

    int GetWindowTextA(Pointer hWnd, byte[] lpString, int nMaxCount);

    boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer userData);

    boolean IsWindow(Pointer hWnd);

    LONG GetWindowLongA(Pointer hWnd, int nIndex);

    LONG SetWindowLongA(Pointer hWnd, int nIndex, LONG dwNewLong);

    boolean PostMessageA(Pointer hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    BOOL SendNotifyMessageA(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    boolean ShowWindow(Pointer hWnd, int nCmdShow);

    boolean SetWindowTextA(Pointer hWnd, String lpString);

    int SetForegroundWindow(Pointer hWnd);

    boolean MoveWindow(Pointer hWnd, int x, int y, int nWidth, int nHeight, boolean bRepaint);

    boolean IsIconic(Pointer hWnd);

    UINT MapVirtualKeyA(UINT uCode, UINT uMapType);

    int GetWindowThreadProcessId(Pointer hwnd, IntByReference lpdwProcessId);

    short GetAsyncKeyState(int vKey);

    UINT SendInput(DWORD dword, INPUT[] inputs, int inputSize);

    HDC GetWindowDC(HWND hWnd);

    boolean GetClientRect(HWND hWnd, RECT rect);

    boolean GetWindowRect(HWND hWnd, RECT rect);

    HDC GetDC(HWND hWnd);

    int ReleaseDC(HWND hWnd, HDC hDC);

    int FillRect(HDC hdc, RECT rect, HBRUSH hbrush);

    boolean IsZoomed(Pointer hWnd);

    BOOL GetCursorInfo(CURSORINFO pci);

    interface WNDENUMPROC extends StdCallCallback {
        boolean callback(Pointer hWnd, Pointer arg);
    }

    @Structure.FieldOrder({"cbSize", "flags", "hCursor", "ptScreenPos"})
    class CURSORINFO extends Structure {
        public DWORD cbSize;
        public DWORD flags;
        public HCURSOR hCursor;
        public POINT ptScreenPos;
    }

    // This may somehow cause a system exit, removed for safety.
    // int GetKeyNameTextA(LONG lParam, LPSTR lpString, int cchSize);
}