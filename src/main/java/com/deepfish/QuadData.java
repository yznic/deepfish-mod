package com.deepfish;

import java.util.List;

public class QuadData {
    public int[] texture_size;
    public List<Quad> quads;

    public static class Quad {
        public float[][] p;
        public float[][] uv;
        public float[] n;
    }
}
