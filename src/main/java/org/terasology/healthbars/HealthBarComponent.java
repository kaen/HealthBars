/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.healthbars;

import org.terasology.entitySystem.Component;
import org.terasology.math.geom.Vector3f;

public class HealthBarComponent implements Component {
    // offset from the entity to the center of the health bar
    Vector3f offset = new Vector3f(0.0f, 0.3f, 0.0f);

    // passed directly to glScale
    Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
    public float hitHealth = 0; // TODO
    public float lastRender = 0;
    public float lastHit;
}
