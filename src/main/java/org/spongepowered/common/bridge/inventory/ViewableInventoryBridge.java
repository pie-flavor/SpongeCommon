package org.spongepowered.common.bridge.inventory;

import net.minecraft.inventory.Container;

import java.util.List;

public interface ViewableInventoryBridge {

    void bridge$addContainer(Container container);
    void bridge$removeContainer(Container container);
    List<Container> bridge$getContainers();
}
