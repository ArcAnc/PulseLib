# PulseLib Items

Items need a slightly different setup from entities and block entities. Minecraft creates one `Item` object for the item type, but the player can hold many `ItemStack`s of that type. If animation state lived directly on the item object, every stack would share the same animation.

PulseLib handles this by using [`AnimManagerKey`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java) and [`SingletonAnimationManager`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java). The item still implements [`PItemAnimatable`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/PItemAnimatable.java), but the actual manager is resolved per key.

## Item model JSON

Create a vanilla item model file. This is not the animated model; it tells Minecraft to use the custom item renderer.

```text
assets/<modid>/models/item/<item>.json
```

```json
{
  "parent": "builtin/entity"
}
```

## Item class

The important part here is `getAnimationManager`. Use the key passed into the method, not a single field shared by every stack.

```java
public class WandItem extends Item implements PItemAnimatable<WandItem> {
    private static final PRawAnimation IDLE = PRawAnimation.begin()
            .thenLoop("idle")
            .build();

    public WandItem(Properties properties) {
        super(properties);
    }

    @Override
    public PAnimationManager<WandItem> getAnimationManager(AnimManagerKey key) {
        return SingletonAnimationManager.getManager(key, this);
    }

    @Override
    public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<WandItem> registrar) {
        registrar.add("idle", () -> state -> {
            state.controller().play(IDLE);
            return ControllerState.PLAY;
        });
    }

    @Override
    public BlockEntityWithoutLevelRenderer getPulseItemRenderer() {
        return WandClient.renderer();
    }

}
```

Keep the cached renderer and its construction in a separate client helper:

```java
public final class WandClient {
    private static BlockEntityWithoutLevelRenderer renderer;
    public static BlockEntityWithoutLevelRenderer renderer() {
        if (renderer == null) {
            Minecraft mc = Minecraft.getInstance();
            renderer = new WandRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return renderer;
    }
}
```

The factory's return type matches the item method. Do not use a covariant client renderer type or construct an anonymous renderer in the shared item class: bytecode verification can otherwise load a client superclass on a dedicated server.

## Renderer

The renderer points at the GLB model and selects a PulseLib render type. It also receives the vanilla dispatcher/model-set objects required by `BlockEntityWithoutLevelRenderer`.

```java
public class WandRenderer extends PItemRenderer<WandItem> {
    public WandRenderer(BlockEntityRenderDispatcher blockEntityRenderDispatcher,
                        EntityModelSet entityModelSet) {
        super(PModelData.item(
                        ResourceLocation.fromNamespaceAndPath("examplemod", "wand")),
                PRenderTypes.RenderTypeProvider::trianglesSolid,
                blockEntityRenderDispatcher,
                entityModelSet);
    }
}
```

Both loader adapters install the renderer for items that implement the common `PItemAnimatable` interface. No PulseLib-specific loader event subscription is needed.

## Stack-specific state

`AnimManagerKey.of(ItemStack)` uses a lazily assigned client runtime ID stored in a transient field of the Java stack object. Independently constructed stacks have independent managers even when their item, count and components match. Changing count, damage or components on the same object retains its ID. Empty and nonanimated stacks do not receive IDs; requesting an animation key for one is unsupported.

`copy()` and `copyWithCount()` inherit the source ID. Copying an animated stack before its first render assigns the source ID and transfers it to the copy without creating a manager. A temporary copy therefore reuses the existing manager and phase. Two separately used copies of the same source deliberately share this identity: a runtime field cannot determine whether a copy is a temporary representation or an independent logical item.

A partial `split()` gives the detached part a new ID and retains the source ID. A full split transfers the old ID and clears the emptied source. Vanilla merging keeps the destination stack's identity; the consumed source's animation does not merge into it. These fields do not participate in component comparison or vanilla stacking.

The ID is neither a Data Component nor serialized NBT/network data. A reconstructed stack gets a new ID, including after reconnecting or receiving a replacement stack from server synchronization. Mods that reconstruct stacks rather than copying them can therefore restart animation. Client-only identity does not guarantee continuity across every inventory/network operation.

The existing manager timeout and world-disconnect cleanup remain unchanged. An ID can outlive its cached manager: displaying the same object after timeout creates a new manager and restarts animation. Factory-generated item keys reserve the negative `long` domain, while other factories return nonnegative keys. Manually constructed `AnimManagerKey(long)` values remain the caller's responsibility; existing hash collisions between nonitem types are outside this change.

Classes used:

* [`PItemAnimatable`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/PItemAnimatable.java)
* [`PItemRenderer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java)
* [`SingletonAnimationManager`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java)
* [`PModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1-multiloader/common/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java)
