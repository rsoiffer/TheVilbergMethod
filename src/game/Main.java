package game;

import behaviors.FPSBehavior;
import behaviors._3d.ModelBehavior;
import engine.Core;
import engine.Input;
import static engine.Layer.PREUPDATE;
import static engine.Layer.UPDATE;
import engine.Settings;
import graphics.Camera;
import graphics.opengl.Framebuffer;
import graphics.voxels.VoxelModel;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import util.math.Vec3d;
import util.math.Vec4d;
import world.World;

public class Main {

    public static void main(String[] args) {
        Settings.SHOW_CURSOR = false;
        Core.init();

        new FPSBehavior().create();
        Camera.current = Camera.camera3d;

        PREUPDATE.onStep(() -> {
            Framebuffer.clearWindow(new Vec4d(.4, .7, 1, 1));
        });

        UPDATE.onStep(() -> {
            if (Input.keyJustPressed(GLFW_KEY_ESCAPE)) {
                Core.stopGame(); 
            }
        });

        ModelBehavior m = new ModelBehavior();
        m.model = VoxelModel.load("skelesmalllarge.vox");
        m.position.position = new Vec3d(10, 0, 0);
        m.create();

        World w = new World();
        w.create();

        Player p = new Player();
        p.position.position = new Vec3d(0, 0, 200);
        p.physics.world = w;
        p.create();

        Core.run();
    }
}
