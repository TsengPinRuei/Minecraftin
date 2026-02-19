package com.minecraftin.clone.world;

public enum Face {
    NORTH(0, 0, -1, 0.80f),
    SOUTH(0, 0, 1, 0.80f),
    WEST(-1, 0, 0, 0.72f),
    EAST(1, 0, 0, 0.72f),
    UP(0, 1, 0, 1.0f),
    DOWN(0, -1, 0, 0.55f);

    private final int dx;
    private final int dy;
    private final int dz;
    private final float light;

    Face(int dx, int dy, int dz, float light) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.light = light;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public int dz() {
        return dz;
    }

    public float light() {
        return light;
    }
}
