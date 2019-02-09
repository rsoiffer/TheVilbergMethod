package game;

import behaviors.FPSBehavior;
import behaviors._3d.ModelBehavior;
import engine.Core;
import engine.Input;
import static engine.Layer.PREUPDATE;
import static engine.Layer.RENDER3D;
import static engine.Layer.UPDATE;
import graphics.Camera;
import graphics.Color;
import graphics.opengl.Framebuffer;
import graphics.voxels.VoxelModel;
import org.joml.Matrix4d;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import util.math.Transformation;
import util.math.Vec3d;
import util.math.Vec4d;
import vr.Vive;
import vr.ViveInput;
import static vr.ViveInput.MENU;
import world.World;

public class MainVR {

    public static void main(String[] args) {
        Core.init();

        new FPSBehavior().create();
        Camera.current = Camera.camera3d;
        Vec4d clearColor = new Vec4d(.4, .7, 1, 1);
        Vive.SCALE_FACTOR = 2;
        Vive.init();
        Vive.initRender(clearColor);

        PREUPDATE.onStep(() -> {
            Framebuffer.clearWindow(clearColor);
        });

        UPDATE.onStep(() -> {
            if (Input.keyJustPressed(GLFW_KEY_ESCAPE)) {
                Core.stopGame();
            }
            ViveInput.update();
            if (ViveInput.LEFT.buttonDown(MENU) && ViveInput.RIGHT.buttonDown(MENU)) {
                ViveInput.resetRightLeft();
                Vive.resetSeatedZeroPose();
            }
        });

        RENDER3D.onStep(() -> {
            VoxelModel.load("sword.vox").render(new Transformation(new Matrix4d()
                    .translate(Camera.camera3d.position.toJOML())
                    .mul(ViveInput.RIGHT.pose())
                    .translate(-.25, -.25, -.25)
                    .scale(1 / 16.)
            ), Color.WHITE);
            VoxelModel.load("sword.vox").render(new Transformation(new Matrix4d()
                    .translate(Camera.camera3d.position.toJOML())
                    .mul(ViveInput.LEFT.pose())
                    .translate(-.25, -.25, -.25)
                    .scale(1 / 16.)
            ), Color.WHITE);
        });

        ModelBehavior m = new ModelBehavior();
        m.model = VoxelModel.load("skelesmalllarge.vox");
        m.position.position = new Vec3d(10, 0, 0);
        m.create();

        World w = new World();
        w.create();

        PlayerVR p = new PlayerVR();
        p.position.position = new Vec3d(0, 0, 200);
        p.physics.world = w;
        p.create();

        Core.run();
    }
}
