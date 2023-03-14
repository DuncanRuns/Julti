package xyz.duncanruns.julti.util;

import java.awt.*;
import java.util.Arrays;

public final class MonitorUtil {
    private MonitorUtil() {}

    public static Monitor getPrimaryMonitor() {
        return new Monitor(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds());
    }

    public static Monitor[] getAllMonitors() {
        final GraphicsDevice[] graphicsDevices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        Monitor[] monitors = new Monitor[graphicsDevices.length];
        for (int i = 0; i < monitors.length; i++) {
            monitors[i] = new Monitor(graphicsDevices[i].getDefaultConfiguration().getBounds());
        }
        return monitors;
    }

    public static class Monitor {
        public final int[] position;
        public final int[] size;
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        public final Rectangle bounds;

        private Monitor(Rectangle bounds) {
            this(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        private Monitor(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.position = new int[] { x, y };
            this.size = new int[] { width, height };
            this.bounds = new Rectangle(x, y, width, height);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            Monitor monitor = (Monitor) o;

            if (this.x != monitor.x) { return false; }
            if (this.y != monitor.y) { return false; }
            if (this.width != monitor.width) { return false; }
            if (this.height != monitor.height) { return false; }
            if (!Arrays.equals(this.position, monitor.position)) { return false; }
            // NOTE: possible behaviour change (bound comparison)
            if (!this.bounds.equals(monitor.bounds)) { return false; }
            return Arrays.equals(this.size, monitor.size);
        }
    }
}
