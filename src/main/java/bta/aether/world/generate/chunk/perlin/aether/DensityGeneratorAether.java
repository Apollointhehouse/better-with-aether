package bta.aether.world.generate.chunk.perlin.aether;

import net.minecraft.core.world.World;
import net.minecraft.core.world.chunk.Chunk;
import net.minecraft.core.world.generate.chunk.perlin.DensityGenerator;
import net.minecraft.core.world.noise.PerlinNoise;

public class DensityGeneratorAether implements DensityGenerator {
    private final World world;

    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;
    private final PerlinNoise mainNoise;
    private final PerlinNoise scaleNoise;
    private final PerlinNoise depthNoise;

    public DensityGeneratorAether(World world)
    {
        this.world = world;

        minLimitNoise = new PerlinNoise(world.getRandomSeed(), 16, 0);
        maxLimitNoise = new PerlinNoise(world.getRandomSeed(), 16, 16);
        mainNoise = new PerlinNoise(world.getRandomSeed(), 8, 32);
        scaleNoise = new PerlinNoise(world.getRandomSeed(), 10, 48);
        depthNoise = new PerlinNoise(world.getRandomSeed(), 16, 58);
    }

    @Override
    public double[] generateDensityMap(Chunk chunk)
    {
        int terrainHeight = (world.getWorldType().getMaxY() + 1) - world.getWorldType().getMinY();

        int xSize = 4 + 1;
        int ySize = (terrainHeight / 8) + 1;
        int zSize = 4 + 1;
        int x = chunk.xPosition * 4;
        int y = 0;
        int z = chunk.zPosition * 4;

        double[] densityMapArray = new double[xSize * ySize * zSize];

        final double mainNoiseScaleX = 240;
        final double mainNoiseScaleY = 600;
        final double mainNoiseScaleZ = 240;

        final double scaleNoiseScaleX = 1.121;
        final double scaleNoiseScaleZ = 1.121;

        final double depthNoiseScaleX = 200;
        final double depthNoiseScaleZ = 200;

        final double depthBaseSize = (terrainHeight / 16D) + 0.5D;

        final double coordScale = 2000;
        final double heightScale = 2400;

        final double heightStretch = 1;

        final double upperLimitScale = 1024;
        final double lowerLimitScale = 512;

        // uses temp and humidity to alter terrain shape.
        double[] scaleArray = scaleNoise.get(null, x, z, xSize, zSize, scaleNoiseScaleX, scaleNoiseScaleZ);
        double[] depthArray = depthNoise.get(null, x, z, xSize, zSize, depthNoiseScaleX, depthNoiseScaleZ);
        double[] mainNoiseArray = mainNoise.get(null, x, y, z, xSize, ySize, zSize, coordScale / mainNoiseScaleX, heightScale / mainNoiseScaleY, coordScale / mainNoiseScaleZ);
        double[] minLimitArray = minLimitNoise.get(null, x, y, z, xSize, ySize, zSize, coordScale, heightScale, coordScale);
        double[] maxLimitArray = maxLimitNoise.get(null, x, y, z, xSize, ySize, zSize, coordScale, heightScale, coordScale);
        int mainIndex = 0;
        int depthScaleIndex = 0;
        int xSizeScale = 16 / xSize;
        for(int dx = 0; dx < xSize; dx++)
        {
            for(int dz = 0; dz < zSize; dz++)
            {
                // Calculate scale
                double scale = (scaleArray[depthScaleIndex] + 256D) / 512D;
//                scale *= humidity;
                if(scale > 1.0D)
                {
                    scale = 1.0D;
                }
                if(scale < 0.0D)
                {
                    scale = 0.0D;
                }

                scale += 0.5D;

                double offsetY = (double)ySize / 2D;
                depthScaleIndex++;

                for(int dy = 0; dy < ySize; dy++)
                {
                    double density;
                    double densityOffset = (((double)dy - offsetY) * heightStretch) / scale;
                    double minDensity = minLimitArray[mainIndex] / upperLimitScale;
                    double maxDensity = maxLimitArray[mainIndex] / lowerLimitScale;
                    double mainDensity = ((mainNoiseArray[mainIndex] / 10D + 1.0D) / 2D);
                    if(mainDensity < 0.0D)
                    {
                        density = minDensity;
                    }
                    else if(mainDensity > 1.0D)
                    {
                        density = maxDensity;
                    }
                    else
                    {
                        density = minDensity + (maxDensity - minDensity) * mainDensity + 1;
                    }
                    density -= 8D;

                    int upperLowerLimit = 43 - 16;
                    // Upper cutoff
                    if(dy > ySize - upperLowerLimit)
                    {
                        double densityMod = (float)(dy - (ySize - upperLowerLimit)) / ((float)upperLowerLimit - 1.0F);
                        density = density * (1.0D - densityMod) + -30D * densityMod;
                    }
                    upperLowerLimit = 5 + 16;
                    // Lower cutoff
                    if(dy < upperLowerLimit)
                    {
                        double densityMod = (float)(upperLowerLimit - dy) / ((float)upperLowerLimit - 1.0F);
                        density = density * (1.0D - densityMod) + -30D * densityMod;
                    }
                    if (Math.abs(dy - (ySize/2)) < 4){
                        density += 10;
                    }

                    densityMapArray[mainIndex] = density;
                    mainIndex++;
                }

            }
        }

        return densityMapArray;

    }
}