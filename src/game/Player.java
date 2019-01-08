package game;

import behaviors._3d.AccelerationBehavior3d;
import behaviors._3d.PositionBehavior3d;
import behaviors._3d.VelocityBehavior3d;
import engine.Behavior;
import static engine.Core.dt;
import engine.Input;
import engine.Layer;
import static graphics.Camera.camera3d;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static util.math.MathUtils.clamp;
import util.math.Vec3d;

public class Player extends Behavior {

    private static final Layer POSTPHYSICS = new Layer(6);

    public final PositionBehavior3d position = require(PositionBehavior3d.class);
    public final VelocityBehavior3d velocity = require(VelocityBehavior3d.class);
    public final AccelerationBehavior3d acceleration = require(AccelerationBehavior3d.class);
    public final PhysicsBehavior physics = require(PhysicsBehavior.class);

    public boolean flying = false;

    @Override
    public void createInner() {
        acceleration.acceleration = new Vec3d(0, 0, -20);

        physics.canCrouch = true;
        physics.hitboxSize1 = new Vec3d(.6, .6, 1.8);
        physics.hitboxSize2 = new Vec3d(.6, .6, 1.8);
        physics.hitboxSize1Crouch = new Vec3d(.6, .6, 1.8);
        physics.hitboxSize2Crouch = new Vec3d(.6, .6, 1.0);
    }

    @Override
    public Layer layer() {
        return POSTPHYSICS;
    }

    @Override
    public void step() {
        // Look around
        camera3d.horAngle -= Input.mouseDelta().x / 300;
        camera3d.vertAngle += Input.mouseDelta().y / 300;
        camera3d.vertAngle = clamp(camera3d.vertAngle, -1.55, 1.55);
        camera3d.position = position.position.add(new Vec3d(0, 0, physics.crouch ? .8 : 1.4));;

        // System.out.println(position.position);
        // Move
        if (Input.keyJustPressed(GLFW_KEY_LEFT_ALT)) {
            flying = !flying;
        }
        if (flying) {
            double flySpeed = 100;
            if (Input.keyDown(GLFW_KEY_W)) {
                position.position = position.position.add(camera3d.facing().setLength(dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_A)) {
                position.position = position.position.add(camera3d.facing().cross(camera3d.up).setLength(-dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_S)) {
                position.position = position.position.add(camera3d.facing().setLength(-dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_D)) {
                position.position = position.position.add(camera3d.facing().cross(camera3d.up).setLength(dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_SPACE)) {
                position.position = position.position.add(camera3d.up.setLength(dt() * flySpeed));
            }
            if (Input.keyDown(GLFW_KEY_LEFT_SHIFT)) {
                position.position = position.position.add(camera3d.up.setLength(-dt() * flySpeed));
            }
            acceleration.velocity.velocity = new Vec3d(0, 0, 0);
        } else {
            physics.shouldCrouch = Input.keyDown(GLFW_KEY_LEFT_CONTROL);
            boolean shouldSprint = Input.keyDown(GLFW_KEY_LEFT_SHIFT);
            double speed = physics.shouldCrouch ? 4 : shouldSprint ? 12 : 8;

            Vec3d forwards = camera3d.facing().setZ(0).normalize();
            Vec3d sideways = camera3d.up.cross(forwards);

            Vec3d idealVel = new Vec3d(0, 0, 0);
            if (Input.keyDown(GLFW_KEY_W)) {
                idealVel = idealVel.add(forwards);
            }
            if (Input.keyDown(GLFW_KEY_A)) {
                idealVel = idealVel.add(sideways);
            }
            if (Input.keyDown(GLFW_KEY_S)) {
                idealVel = idealVel.sub(forwards);
            }
            if (Input.keyDown(GLFW_KEY_D)) {
                idealVel = idealVel.sub(sideways);
            }
            if (idealVel.lengthSquared() > 0) {
                idealVel = idealVel.setLength(speed);
            }
            velocity.velocity = idealVel.setZ(velocity.velocity.z);

            if (Input.keyDown(GLFW_KEY_SPACE)) {
                if (physics.onGround) {
                    velocity.velocity = velocity.velocity.setZ(12);
                }
            }
        }
    }
}
