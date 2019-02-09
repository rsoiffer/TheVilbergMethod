package game;

import behaviors._3d.AccelerationBehavior3d;
import behaviors._3d.PositionBehavior3d;
import behaviors._3d.VelocityBehavior3d;
import engine.Behavior;
import engine.Layer;
import static graphics.Camera.camera3d;
import util.math.Vec3d;
import vr.ViveInput;
import static vr.ViveInput.GRIP;
import static vr.ViveInput.LEFT;
import static vr.ViveInput.RIGHT;
import static vr.ViveInput.TRACKPAD;
import static vr.ViveInput.TRIGGER;

public class PlayerVR extends Behavior {

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
//        camera3d.horAngle -= Input.mouseDelta().x * 16. / 3;
//        camera3d.vertAngle -= Input.mouseDelta().y * 3;
//        camera3d.vertAngle = clamp(camera3d.vertAngle, -1.55, 1.55);
        camera3d.position = position.position.add(new Vec3d(0, 0, physics.crouch ? -1.4 : -1.8));;

        // Move
        if (ViveInput.RIGHT.buttonJustPressed(GRIP)) {
            flying = !flying;
        }
        if (flying) {
            double flySpeed = 30;
            acceleration.acceleration = new Vec3d(0, 0, -20);
            acceleration.acceleration = acceleration.acceleration.add(RIGHT.forwards().mul(flySpeed * ViveInput.RIGHT.trigger()));
            acceleration.acceleration = acceleration.acceleration.add(LEFT.forwards().mul(flySpeed * ViveInput.LEFT.trigger()));
            acceleration.velocity.velocity = acceleration.velocity.velocity.mul(.995);
//            if (Input.keyDown(GLFW_KEY_W)) {
//                position.position = position.position.add(camera3d.facing().setLength(dt() * flySpeed));
//            }
//            if (Input.keyDown(GLFW_KEY_A)) {
//                position.position = position.position.add(camera3d.facing().cross(camera3d.up).setLength(-dt() * flySpeed));
//            }
//            if (Input.keyDown(GLFW_KEY_S)) {
//                position.position = position.position.add(camera3d.facing().setLength(-dt() * flySpeed));
//            }
//            if (Input.keyDown(GLFW_KEY_D)) {
//                position.position = position.position.add(camera3d.facing().cross(camera3d.up).setLength(dt() * flySpeed));
//            }
//            if (Input.keyDown(GLFW_KEY_SPACE)) {
//                position.position = position.position.add(camera3d.up.setLength(dt() * flySpeed));
//            }
//            if (Input.keyDown(GLFW_KEY_LEFT_SHIFT)) {
//                position.position = position.position.add(camera3d.up.setLength(-dt() * flySpeed));
//            }
//            acceleration.velocity.velocity = new Vec3d(0, 0, 0);
        } else {
            physics.shouldCrouch = ViveInput.LEFT.buttonDown(GRIP);
            boolean shouldSprint = ViveInput.LEFT.buttonDown(TRACKPAD);
            double speed = physics.shouldCrouch ? 4 : shouldSprint ? 12 : 8;

            Vec3d forwards = camera3d.facing().setZ(0).normalize();
            Vec3d sideways = camera3d.up.cross(forwards);

            Vec3d idealVel = new Vec3d(0, 0, 0);
            idealVel = idealVel.add(forwards.mul(ViveInput.LEFT.trackpad().y));
            idealVel = idealVel.add(sideways.mul(-1 * ViveInput.LEFT.trackpad().x));
            if (idealVel.lengthSquared() > 0) {
                idealVel = idealVel.setLength(speed);
            }
            velocity.velocity = idealVel.setZ(velocity.velocity.z);

            if (ViveInput.LEFT.buttonDown(TRIGGER)) {
                if (physics.onGround) {
                    velocity.velocity = velocity.velocity.setZ(12);
                }
            }
        }
    }
}
