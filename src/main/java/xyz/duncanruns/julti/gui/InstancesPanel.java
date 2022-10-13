package xyz.duncanruns.julti.gui;

import com.formdev.flatlaf.ui.FlatMarginBorder;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class InstancesPanel extends JPanel {
    private final Julti julti;
    private final JPanel mainPanel;
    private final Supplier<Boolean> shouldUpdateSupplier;
    private final Supplier<Boolean> shouldShutdownSupplier;
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ArrayList<SingleInstancePanel> instancePanels = new ArrayList<>();

    public InstancesPanel(Julti julti, Supplier<Boolean> shouldUpdateSupplier, Supplier<Boolean> shouldShutdownSupplier) {
        this.julti = julti;
        this.shouldUpdateSupplier = shouldUpdateSupplier;
        this.shouldShutdownSupplier = shouldShutdownSupplier;

        setLayout(new GridBagLayout());
        setBorder(new FlatMarginBorder(new Insets(5, 5, 5, 5)));

        JLabel instancesLabel = new JLabel("Instances");
        instancesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(instancesLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, 10, 0, new Insets(0, 0, 5, 0), 0, 0));

        mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout());
        add(mainPanel, new GridBagConstraints(0, 1, 1, 1, 0, 0, 10, 0, new Insets(0, 0, 5, 0), 0, 0));

        tick();
        executor.scheduleWithFixedDelay(this::tick, 250, 250, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (shouldShutdownSupplier.get()) {
            executor.shutdownNow();
            return;
        }
        if (!shouldUpdateSupplier.get()) {
            return;
        }
        List<MinecraftInstance> instances = julti.getInstanceManager().getInstances();

        if (instancePanels.size() != instances.size()) {
            while (instancePanels.size() < instances.size()) {
                instancePanels.add((SingleInstancePanel) mainPanel.add(new SingleInstancePanel(julti)));
            }
            while (instancePanels.size() > instances.size()) {
                mainPanel.remove(instancePanels.remove(0));
            }
            revalidate();
        }
        int i = 0;
        for (MinecraftInstance instance : instances) {
            instancePanels.get(i++).setInfo(instance);
        }
    }
}
