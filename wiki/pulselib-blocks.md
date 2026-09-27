# PulseLib Blocks

Animated blocks in Minecraft are really animated block entities. The block still handles placement, collision, interaction, redstone behavior, and blockstate data. The visible animated model is rendered by the block entity renderer.

That split is important: implement [`PAnimatable`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java) on the `BlockEntity`, not on the `Block`.

## Block class

The block should create a block entity and hide vanilla block rendering. If you forget to hide vanilla rendering, you may see the normal block model fighting with the PulseLib model, or you may simply render the wrong thing.

```java
public class CrusherBlock extends Block implements EntityBlock {
    public CrusherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}
```

`PBlockRenderer` automatically rotates the model from `HORIZONTAL_FACING` or `FACING` when those blockstate properties are present. That means you can keep normal block placement logic in the blockstate and let the renderer follow it.

## BlockEntity

The block entity owns the animation state. Store gameplay flags here, then let a controller translate those flags into animation playback.

```java
public class CrusherBlockEntity extends BlockEntity implements PAnimatable<CrusherBlockEntity> {
    private static final PRawAnimation WORK = PRawAnimation.begin()
            .thenLoop("work")
            .build();

    private final PAnimationManager<CrusherBlockEntity> manager = PLibHelper.createManager(this);
    private boolean working;

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(MyBlockEntities.CRUSHER.get(), pos, state);
    }

    @Override
    public PAnimationManager<CrusherBlockEntity> getAnimationManager(AnimManagerKey key) {
        return this.manager;
    }

    @Override
    public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<CrusherBlockEntity> registrar) {
        registrar.add("work", () -> state -> {
            if (!state.animatable().working) {
                state.controller().stop();
                return ControllerState.STOP;
            }

            state.controller().play(WORK);
            return ControllerState.PLAY;
        });
    }
}
```

## Renderer

The renderer is intentionally small. It only needs model data and a PulseLib render type unless you want extra custom drawing.

```java
public class CrusherRenderer extends PBlockRenderer<CrusherBlockEntity, CrusherRenderState> {
    public CrusherRenderer(BlockEntityRendererProvider.Context context) {
        super(PModelData.block(
                        Identifier.fromNamespaceAndPath("examplemod", "crusher")),
                PRenderTypes.RenderTypeProvider::trianglesSolid);
    }

    @Override
    public CrusherRenderState createRenderState() {
        return new CrusherRenderState();
    }
}

public class CrusherRenderState extends PBlockRenderState.Impl<CrusherBlockEntity> {}
```

## Custom render hooks

Use hooks when the model is not the whole visual. For example, a machine might render the animated body through PulseLib and draw a vanilla fluid surface or item stack in `postSubmit`.

```java
@Override
public void postSubmit(PoseStack poseStack,
                       CrusherRenderState renderState,
                       CameraRenderState cameraRenderState,
                       SubmitNodeCollector submitNodeCollector) {
    // Add PulseLib queue submissions or collector nodes here.
}
```

Classes used:

* [`PBlockRenderer`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/renderer/PBlockRenderer.java)
* [`PModelData.block(...)`](modeldata.md)
* [`PAnimatable`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java)
* [`PLibHelper`](https://github.com/ArcAnc/PulseLib/blob/26.1/src/main/java/com/arcanc/pulselib/util/helpers/PLibHelper.java)
