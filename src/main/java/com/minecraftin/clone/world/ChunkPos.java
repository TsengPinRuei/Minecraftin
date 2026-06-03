package com.minecraftin.clone.world;

// 表示一個 Chunk 在世界中的座標位置，主要作為 Map key；record 會提供值相等的 equals/hashCode。
public record ChunkPos(int x, int z) {
}
