# Animation Events

Animation events are timestamped typed payloads stored in `PAnimation`. Time is expressed in ticks internally; glTF sidecars and Gecko JSON use seconds and are converted with `seconds * 20`.

The public API is `PAnimationEvent<T>` and `PAnimationEventType<T>`. Event types supply a `ResourceLocation`, a `MapCodec<T>`, a side policy, and the execution function. Built-in types live in PulseLib's client-side type registry under their own ids (for example, `pulselib:sound`). Add-ons register a type through `PulseLibEvents.TypeRegistrationEvent.registerAnimationEvent(...)`.

## Built-in types

| Type | Side | Payload |
| --- | --- | --- |
| `sound` | `PRESENTATION_ONLY` | `sound`, optional `locator`, `volume`, `pitch` |
| `particle` | `PRESENTATION_ONLY` | `particle`, optional `locator`, `offset`, `motion` |
| `camera_shake` | `PRESENTATION_ONLY` | `strength`, optional `duration` (ticks), `frequency` |
| `locator_callback` | `BOTH` | `callback`, optional `locator` |
| `animation_parameter` | `BOTH` | optional `controller`, `parameter`, optional `value`, `trigger` |

`PRESENTATION_ONLY` means client-only and non-authoritative. `CLIENT` and `SERVER` run only on that physical side; `BOTH` runs on either side when the controller is ticked there. A server event therefore needs a server-ticked animation manager; the standard singleton/instance managers are client ticked.

`locator_callback` invokes a callback registered with `PAnimationEventCallbacks.register(id, callback)`. The callback receives the event context and the resolved locator position (or `null` when the animatable has no world position). `animation_parameter` targets the current graph controller when `controller` is blank; otherwise it targets a controller by name. `trigger: true` calls `trigger(parameter)`, otherwise `value` is assigned to the parameter.

## glTF sidecar files

For `.glb` and `.gltf` models, PulseLib looks for sidecars in these locations:

```text
glmodels/<model>.events.json
glmodels/<model>.animation_events.json
glmodels/events/<model>.events.json
glmodels/events/<fileName>.events.json
```

The `type` may be a built-in short name or a fully qualified registry id. Event fields are decoded with the selected type's codec.

```json
{
  "animations": {
    "attack": {
      "events": [
        {
          "type": "sound",
          "time": 0.25,
          "sound": "minecraft:entity.player.attack.strong",
          "locator": "hand_right"
        },
        {
          "type": "camera_shake",
          "time": 0.25,
          "strength": 1.5,
          "duration": 5,
          "frequency": 10
        },
        {
          "type": "animation_parameter",
          "time": 0.35,
          "controller": "locomotion",
          "parameter": "attack_done",
          "trigger": true
        }
      ]
    }
  }
}
```

Gecko `sound_effects` and `particle_effects` retain their native format and are converted to the corresponding typed built-ins.

## Delivery rules

- A forward move delivers events in `(previousTime, currentTime]`; the initial move from zero also delivers time-zero events.
- A reverse move delivers `[currentTime, previousTime)` in reverse timestamp order. Negative `PAnimationStagePlayback.speed()` starts a playback at its final frame, so it does not need a one-tick forward setup.
- A cyclic stage is treated as an unwrapped timeline. Every wrap crossed by a tick is traversed in order, including multiple wraps caused by speed changes or a large delta. Events at `0` and at the animation length are distinct and retain their order at the wrap.
- A missed tick and a large positive delta use the same traversal rules: every crossed event is delivered once per crossed loop. Content authors should avoid expensive event handlers in very short loops.
- Graph states and transition source/target layers report their own traversals; events from every contributing clip are therefore delivered. This is intentional—put gameplay effects in an unblended clip when duplicate blended effects are undesirable.
- `seek(model, time)` and `syncCycle(model, phase)` change both previous and current cursors and never replay events. They are the required operations for a seek, late instance creation, and network re-synchronization.

Locator resolution uses the current posed bone. It falls back to the entity origin or block-entity centre if the named locator cannot be found.
