package xyz.duncanruns.julti.resetting;

import xyz.duncanruns.julti.AffinityManager;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;

import java.util.List;

public class MultiResetManager extends ResetManager {
    public MultiResetManager(Julti julti) {
        super(julti);
    }

    @Override
    public boolean doReset() {
        List<MinecraftInstance> instances = instanceManager.getInstances();

        // Return if no instances
        if (instances.size() == 0) {
            return false;
        }

        // Get selected instance, return if no selected instance,
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return false;
        }

        // if there is only a single instance, reset it and return.
        if (instances.size() == 1) {
            selectedInstance.reset(true);
            return true;
        }

        int nextInstInd = (instances.indexOf(selectedInstance) + 1) % instances.size();
        MinecraftInstance nextInstance = instances.get(nextInstInd);

        nextInstance.activate(instances.indexOf(nextInstance) + 1);
        julti.switchScene(nextInstInd + 1);

        selectedInstance.reset(false);

        super.doReset();
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
    }

    @Override
    public boolean doBGReset() {
        super.doBGReset();
        MinecraftInstance selectedInstance = instanceManager.getSelectedInstance();
        if (selectedInstance == null) {
            return false;
        }
        List<MinecraftInstance> instances = instanceManager.getInstances();
        for (MinecraftInstance instance : instances) {
            if (!instance.equals(selectedInstance)) {
                instance.reset(false);
            }
        }
        if (JultiOptions.getInstance().useAffinity) {
            AffinityManager.ping(julti);
        }
        return true;
    }
}
