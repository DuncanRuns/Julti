package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatMarginBorder;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class InstancesPanel extends JPanel {
    private final JPanel mainPanel;
    private final Supplier<Boolean> shouldUpdateSupplier;
    private final Supplier<Boolean> shouldShutdownSupplier;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ArrayList<SingleInstancePanel> instancePanels = new ArrayList<>();

    private int lastActive = 0;

    public InstancesPanel(Supplier<Boolean> shouldUpdateSupplier, Supplier<Boolean> shouldShutdownSupplier) {
        this.shouldUpdateSupplier = shouldUpdateSupplier;
        this.shouldShutdownSupplier = shouldShutdownSupplier;

        this.setLayout(new GridBagLayout());
        this.setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));

        JLabel instancesLabel = new JLabel("Instances");
        instancesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(instancesLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, 10, 0, new Insets(0, 0, 5, 0), 0, 0));

        this.mainPanel = new JPanel();
        this.mainPanel.setLayout(new GridLayout(0, 5));
        this.add(this.mainPanel, new GridBagConstraints(0, 1, 1, 1, 0, 0, 10, 0, new Insets(0, 0, 5, 0), 0, 0));

        this.tick();
        this.executor.scheduleWithFixedDelay(this::tick, 250, 250, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (this.shouldShutdownSupplier.get()) {
            this.executor.shutdownNow();
            return;
        }
        if (!this.shouldUpdateSupplier.get()) {
            return;
        }

        List<MinecraftInstance> instances = InstanceManager.getInstanceManager().getInstances();

        if (this.instancePanels.size() != instances.size()) {
            while (this.instancePanels.size() < instances.size()) {
                this.instancePanels.add((SingleInstancePanel) this.mainPanel.add(new SingleInstancePanel()));
            }
            while (this.instancePanels.size() > instances.size()) {
                this.mainPanel.remove(this.instancePanels.remove(0));
            }
            this.mainPanel.setLayout(new GridLayout(0, Math.max(1, Math.min(instances.size(), 5))));
            this.revalidate();
        }

        MinecraftInstance selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance();
        this.lastActive = selectedInstance != null ? InstanceManager.getInstanceManager().getInstanceNum(selectedInstance) : this.lastActive;

        int i = 0;
        for (MinecraftInstance instance : instances) {
            SingleInstancePanel panel = this.instancePanels.get(i++);
            panel.setInfo(instance);
            if (i == this.lastActive && instance.hasWindow()) {
                panel.setActive();
            }
        }
    }
}
