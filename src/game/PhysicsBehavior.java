package game;

import behaviors._3d.PositionBehavior3d;
import behaviors._3d.PreviousPositionBehavior3d;
import behaviors._3d.VelocityBehavior3d;
import engine.Behavior;
import engine.Layer;
import static util.math.MathUtils.ceil;
import static util.math.MathUtils.floor;
import util.math.Vec3d;
import world.World;

public class PhysicsBehavior extends Behavior {

    private static final Layer PHYSICS = new Layer(5);
    private static final int DETAIL = 10;

    public final PositionBehavior3d position = require(PositionBehavior3d.class);
    public final PreviousPositionBehavior3d prevPos = require(PreviousPositionBehavior3d.class);
    public final VelocityBehavior3d velocity = require(VelocityBehavior3d.class);

    public Vec3d hitboxSize1 = new Vec3d(0, 0, 0);
    public Vec3d hitboxSize2 = new Vec3d(0, 0, 0);
    public boolean onGround;
    public boolean hitWall;
    public double stepUp = 1.05;
    public World world;

    public Vec3d hitboxSize1Crouch = new Vec3d(0, 0, 0);
    public Vec3d hitboxSize2Crouch = new Vec3d(0, 0, 0);
    public boolean canCrouch;
    public boolean crouch;
    public boolean shouldCrouch;

    public boolean containsPoint(Vec3d v) {
        return v.x >= position.position.x - hitboxSize1.x
                && v.y >= position.position.y - hitboxSize1.y
                && v.z >= position.position.z - hitboxSize1.z
                && v.x <= position.position.x + hitboxSize2.x
                && v.y <= position.position.y + hitboxSize2.y
                && v.z <= position.position.z + hitboxSize2.z;
    }

    @Override
    public Layer layer() {
        return PHYSICS;
    }

    private boolean moveToWall(Vec3d del) {
        if (!wouldCollideAt(position.position.add(del))) {
            position.position = position.position.add(del);
            return false;
        }
        double best = 0;
        double check = .5;
        double step = .25;
        for (int i = 0; i < DETAIL; i++) {
            if (wouldCollideAt(del.mul(check).add(position.position))) {
                check -= step;
            } else {
                best = check;
                check += step;
            }
            step /= 2;
        }
        position.position = position.position.add(del.mul(best));
        return true;
    }

    private double potentialMoveDist(Vec3d del) {
        Vec3d oldPos = position.position;
        moveToWall(new Vec3d(del.x, 0, 0));
        moveToWall(new Vec3d(0, del.y, 0));
        moveToWall(new Vec3d(0, 0, del.z));
        double dist = position.position.sub(oldPos).length();
        position.position = oldPos;
        return dist;
    }

    @Override
    public void step() {
        // Useful vars
        boolean wasOnGround = onGround;
        Vec3d del = position.position.sub(prevPos.prevPos);

        // Reset all vars
        onGround = false;
        hitWall = false;
        crouch = canCrouch;

        // Check collision
        if (wouldCollideAt(position.position)) {
            if (wouldCollideAt(prevPos.prevPos)) {
                // Give up
                velocity.velocity = new Vec3d(0, 0, 0);
            } else {
                position.position = prevPos.prevPos;

                // Move in Z dir
                if (moveToWall(new Vec3d(0, 0, del.z))) {
                    velocity.velocity = velocity.velocity.setZ(0);
                    if (del.z < 0) {
                        onGround = true;
                    }
                }
                // Try step up
                boolean steppingUp = false;
                if (wasOnGround || onGround) {
                    double moveDist1 = potentialMoveDist(del.setZ(0));
                    position.position = position.position.add(new Vec3d(0, 0, stepUp));
                    double moveDist2 = potentialMoveDist(del.setZ(0));
                    if (moveDist1 >= moveDist2) {
                        position.position = position.position.sub(new Vec3d(0, 0, stepUp));
                    } else {
                        steppingUp = true;
                    }
                }
                if (moveToWall(new Vec3d(del.x, 0, 0))) {
                    velocity.velocity = velocity.velocity.setX(0);
                    hitWall = true;
                }
                if (moveToWall(new Vec3d(0, del.y, 0))) {
                    velocity.velocity = velocity.velocity.setY(0);
                    hitWall = true;
                }
                if (steppingUp) {
                    moveToWall(new Vec3d(0, 0, -stepUp));
                }
            }
        }

        // Try to stand up
        if (canCrouch && !shouldCrouch) {
            crouch = false;
            if (wouldCollideAt(position.position)) {
                crouch = true;
            }
        }

        // Set onGround
        if (!onGround && wouldCollideAt(position.position.add(new Vec3d(0, 0, -.01)))) {
            onGround = true;
        }
    }

    public boolean wouldCollideAt(Vec3d pos) {
        Vec3d hitboxSize1Real = crouch ? hitboxSize1Crouch : hitboxSize1;
        Vec3d hitboxSize2Real = crouch ? hitboxSize2Crouch : hitboxSize2;
        for (int x = floor(pos.x - hitboxSize1Real.x); x < pos.x + hitboxSize2Real.x; x++) {
            for (int y = floor(pos.y - hitboxSize1Real.y); y < pos.y + hitboxSize2Real.y; y++) {
                if (!world.getBlockRangeEquals(x, y, floor(pos.z - hitboxSize1Real.z), ceil(pos.z + hitboxSize2Real.z) - 1, null)) {
                    return true;
                }
            }
        }
        return false;
    }
}
