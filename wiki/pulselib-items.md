# PulseLib Items

Items need a slightly different setup from entities and block entities. Minecraft creates one `Item` object for the item type, but the player can hold many `ItemStack`s of that type. If animation state lived directly on the item object, every stack would share the same animation.

PulseLib handles this by using [`AnimManagerKey`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java) and [`SingletonAnimationManager`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java). The item still implements [`PItemAnimatable`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/animatable/PItemAnimatable.java), but the actual manager is resolved per key.

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
    public IClientItemExtensions registerClientExtension() {
        return new IClientItemExtensions() {
            private final WandRenderer renderer = new WandRenderer(
                    Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                    Minecraft.getInstance().getEntityModels());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        };
    }
}
```

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

PulseLib automatically registers item client extensions for items that implement `PItemAnimatable`, so you do not need a separate client extension event for the common case.

## Stack-specific state

`AnimManagerKey.of(ItemStack)` uses item id, stack count, and component data. If your item animation should differ by custom data, store it in item components so the key changes. If two stacks have identical key data, they can reuse the same cached manager.

Classes used:

* [`PItemAnimatable`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/animatable/PItemAnimatable.java)
* [`PItemRenderer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java)
* [`SingletonAnimationManager`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/animatable/singleton/SingletonAnimationManager.java)
* [`PModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java)
