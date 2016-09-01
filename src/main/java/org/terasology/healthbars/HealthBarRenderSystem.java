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

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.slf4j.LoggerFactory;
import org.terasology.config.Config;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.*;
import org.terasology.logic.health.HealthComponent;
import org.terasology.logic.health.OnDamagedEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.AutoMountCameraComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.logic.players.LocalPlayerSystem;
import org.terasology.math.geom.Vector2f;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.math.geom.Vector4f;
import org.terasology.protobuf.EntityData;
import org.terasology.registry.In;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.logic.NearestSortingList;
import org.terasology.rendering.opengl.OpenGLUtils;
import org.terasology.rendering.world.WorldRenderer;
import org.terasology.utilities.Assets;
import org.terasology.utilities.random.FastRandom;
import org.terasology.utilities.random.Random;
import org.terasology.world.WorldProvider;
import org.terasology.world.biomes.Biome;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.BlockPart;
import org.terasology.world.block.tiles.WorldAtlas;

import java.math.RoundingMode;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;

@RegisterSystem(RegisterMode.CLIENT)
public class HealthBarRenderSystem extends BaseComponentSystem implements UpdateSubscriberSystem, RenderSystem {

    @In
    private EntityManager entityManager;

    @In
    private WorldProvider worldProvider;

    @In
    private WorldAtlas worldAtlas;

    @In
    private LocalPlayer localPlayer;

    private final float HIT_DELAY = 0.09f;
    private Random random = new FastRandom();
//    private NearestSortingList sorter = new NearestSortingList();
    private Texture fillTexture;
    private Texture hitTexture;
    private Texture emptyTexture;
    private Texture barTexture;
    private int displayList;
    private float aspectRatio;
    @In
    private WorldRenderer worldRenderer;

    @Override
    public void initialise() {
        barTexture = Assets.getTexture("healthbars:healthbar").get();
        hitTexture = Assets.getTexture("healthbars:hit").get();
        emptyTexture = Assets.getTexture("healthbars:empty").get();
        fillTexture = Assets.getTexture("healthbars:full").get();
        aspectRatio = (float) barTexture.getWidth() / (float) barTexture.getHeight();
        EntityRef camera = localPlayer.getCameraEntity();
    }

    @Override
    public void shutdown() {
        glDeleteLists(displayList, 1);
    }

    @Override
    public void update(float delta) { }

//    @ReceiveEvent(components = {HealthBarComponent.class, HealthComponent.class, LocationComponent.class})
//    public void onActivated(OnActivatedComponent event, EntityRef entity) {
//        sorter.add(entity);
//    }
//
//    @ReceiveEvent(components = {HealthBarComponent.class, HealthComponent.class, LocationComponent.class})
//    public void onDeactivated(BeforeDeactivateComponent event, EntityRef entity) {
//        sorter.remove(entity);
//    }

    @Override
    public void renderAlphaBlend() {
        render(entityManager.getEntitiesWith(HealthBarComponent.class, HealthComponent.class, LocationComponent.class));
    }

    @ReceiveEvent
    public void updateHitLevel(OnDamagedEvent event, EntityRef victim, HealthBarComponent healthBar, HealthComponent health) {
        healthBar.hitHealth = Math.max(healthBar.hitHealth, health.currentHealth - event.getHealthChange());
        healthBar.lastHit = worldProvider.getTime().getMilliseconds();
        victim.saveComponent(healthBar);
    }

    private void render(Iterable<EntityRef> healthBarEntities) {
        Assets.getMaterial("engine:prog.particle").get().enable();
        glDisable(GL11.GL_CULL_FACE);

        LocationComponent localPlayerLocation = localPlayer.getClientEntity().getComponent(LocationComponent.class);
        Vector3f playerPosition = localPlayerLocation.getWorldPosition();

        for (EntityRef entity : healthBarEntities) {
            LocationComponent location = entity.getComponent(LocationComponent.class);

            if (null == location) {
                continue;
            }

            Vector3f worldPos = location.getWorldPosition();

            if (!worldProvider.isBlockRelevant(worldPos)) {
                continue;
            }

            HealthBarComponent healthBar = entity.getComponent(HealthBarComponent.class);
            HealthComponent health = entity.getComponent(HealthComponent.class);

            float now = worldProvider.getTime().getMilliseconds();
            if(now >= HIT_DELAY * 1000.0f * worldProvider.getTime().getTimeRate() + healthBar.lastHit) {
                float delta = ((float) health.maxHealth / 1000.0f) * (now - healthBar.lastRender) / worldProvider.getTime().getTimeRate();
                healthBar.hitHealth -= delta;
                healthBar.hitHealth = Math.max(health.currentHealth, healthBar.hitHealth);
            }

            healthBar.lastRender = now;
            entity.saveComponent(healthBar);
            float healthFactor = (float) health.currentHealth / (float) health.maxHealth;
            float hitRatio = healthBar.hitHealth / (float) health.maxHealth;

            glPushMatrix();
            glTranslated(worldPos.x - playerPosition.x, worldPos.y - playerPosition.y, worldPos.z - playerPosition.z);

            float light = worldRenderer.getRenderingLightIntensityAt(new Vector3f(worldPos).add(healthBar.offset));

            renderBar(emptyTexture, healthBar, 1.0f, light);
            renderBar(hitTexture, healthBar, hitRatio, light);
            renderBar(fillTexture, healthBar, healthFactor, light);
            renderBar(barTexture, healthBar, 1.0f, light);

            glPopMatrix();
        }

        glEnable(GL11.GL_CULL_FACE);
    }

    protected void renderBar(Texture texture, HealthBarComponent healthBar, float ratio, float light) {
        if (texture.isLoaded()) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            glBindTexture(GL11.GL_TEXTURE_2D, texture.getId());
        } else {
            return;
        }

        glPushMatrix();
        OpenGLUtils.applyBillboardOrientation();
        glTranslatef(healthBar.offset.x, healthBar.offset.y, healthBar.offset.z);
        glScalef(healthBar.scale.x, healthBar.scale.y, healthBar.scale.z);

        Material mat = Assets.getMaterial("engine:prog.particle").get();
        mat.setFloat4("colorOffset", 1.0f, 1.0f, 1.0f, 1.0f, true);
        mat.setFloat2("texOffset", 0, 0, true);
        mat.setFloat2("texScale", 1.0f, 1.0f, true);
        mat.setFloat("light", light, true);

        float halfHeight = 1.0f / aspectRatio / 2.0f;

        glBegin(GL_QUADS);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex3f(-0.5f, halfHeight, 0.0f);

        GL11.glTexCoord2f(ratio, 0.0f);
        GL11.glVertex3f(ratio - 0.5f, halfHeight, 0.0f);

        GL11.glTexCoord2f(ratio, 1.0f);
        GL11.glVertex3f(ratio - 0.5f, -halfHeight, 0.0f);

        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex3f(-0.5f, -halfHeight, 0.0f);
        glEnd();

        glPopMatrix();
    }

    @Override
    public void renderOpaque() {
    }

    @Override
    public void renderOverlay() {
    }

    @Override
    public void renderFirstPerson() {
    }

    @Override
    public void renderShadows() {
    }
}
